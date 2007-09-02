"""
history.py
http://snarfed.org/space/pyblosxom+history
Ryan Barrett <pyblosxom@ryanb.org>

A PyBlosxom plugin that displays change history for entries. The change
history must already exist, either in a version control system, database, or
another form. This plugin just uses that backend to display change history,
previous versions of entries, and textual diffs for changes.


VERSION CONTROL BACKENDS
========================

This plugin is designed to support different version control backends.
Currently the only backend is Subversion, but the backend code has been
abstracted to make it easy to add new backends.

This module requires the 'history_backend' config parameter, which specifies
the version control backend to use. For now, only 'subversion' is supported.
Add this line to your config.py:

    py['history_backend'] = 'subversion'

To use the Subversion backend, the pysvn module must be installed somewhere in
your PYTHONPATH. For more on pysvn, see:

    http://pysvn.tigris.org/

If you use the Subversion backend, it's *highly* recommended that your
repository and web server are on the same machine, and that your working copy
is checked out with a file:// URL. Other methods, such as http://, svn://, and
svn+ssh:// will incur noticeable overhead on each change history request.

To add a new backend, subclass the VersionedEntry class and implement the 
get(), change(), changes(), and diff() methods. Then add an entry to the
BACKENDS dictionary. If your backend uses its own config parameters, you'll
also want to check for them in verify_installation().


NEW URLS
========

There are three new types of pages: past versions, change history, and diffs.
They are accessed by adding these parameters to entry URLs:

    ?history=1

Shows the entry's change history as a series of diffs, one for each change.

    ?version=X

Shows version X of the entry. Depending on your backend, X may be a revision
number, a date, a tag, or something else entirely. The Subversion backend
supports revision numbers.

    ?version=X&raw=1

Shows the raw text of version X of the entry.

    ?diff=1&version1=X&version2=Y

For example, if an entry is located at http://myblog.com/entry, you'd use
http://myblog.com/entry?history=1 to show its change history.

Note that all of the URL parameters require a value. The values for history,
diff, and raw can be anything, but they *must* be present. URLs that omit the
value, e.g. http://myblog.com/entry?history, will not work.


TEMPLATES
=========

The history plugin uses a few new templates, and provides new template
variables for them. Template files for the html flavour are provided with this
plugin. The new templates are:

  * `history-diff`: a diff between two versions of an entry. The text of the
    diff, in unified format, is provided in the $diff template variable.

  * `history-version`: a previous version of an entry. This will usually be
    similar to your story template, with minor modifications. For example, you
    might not provide links to the entry's comments. All template variables
    available in the story template are also available here.

  * `history-head` and `history-foot`: These are displayed around the change
    history page. history-head is displayed at the beginning, history-foot is
    displayed at the end. The body of the page is a series of diffs, one for
    each change to the entry, rendered with the history-diff template.


TEMPLATE VARIABLES
==================

These new template variables are provided in the history-diff and
history-version templates:

    history-diff:
    $diff      The text of the diff, in unified format.
    $version1  The base version of the diff.
    $version2  The ending version of the diff.

    history-version:
    $version   The displayed version of the entry .

    both:
    $author    The person who made the last change.
    $message   The changelog of the last change.
    $date      The date of the last change.

Diffs should usually be displayed in a fixed-width font, so surrounding $diff
with <code> and/or <pre> is strongly suggested.

Also, some lines of the diff will be surrounded with <span class="..."> tags,
depending on the type of change that the line represents. The classes are
diff-added, diff-removed, diff-changed, and diff-line-number.


CHANGELOG:
0.4 update to work with pysvn 1.5.2
0.3 bugfixes: handle change history w/only one version, etc.
0.2 First release.


TODO: caching. Change history data is immutable, so it's a great candidate for
caching. However, a given piece of change history data is retrieved at most
once per request, so caching it within a request wouldn't do much.

However, it would help when running as WSGI, FastCGI, or mod_python, since the
cache will persist across requests. 

Note that the PyBlosxom cache wouldn't be appropriate here. It's designed to
cache entry data, so it considers cache lines stale when their entry is
updated. Change history data is never stale. :P


Copyright 2006 Ryan Barrett

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
"""
__author__ = "Ryan Barrett"
__version__ = "0.4"
__url__ = "http://snarfed.org/space/pyblosxom+history"
__description__ = "Displays change history, past versions, and diffs."

import os
import os.path
import xml.sax.saxutils
import sys
import time
from Pyblosxom import entries, renderers, tools

try:
  import pysvn  # only needed for the subversion backend
except:
  pysvn = None


# Exceptions thrown by backends, in VersionedEntry subclasses
class BadPath(Exception):
  pass

class BadVersion(Exception):
  pass

class VersionedEntry:
  """ An abstract base class for accessing the change history of an entry.
  Provides a list of changes, full text for each past version, and diffs
  between versions.
  """
  def __init__(self, path):
    """ Takes the full path of an entry.

    Raises BadPath if the path is invalid.
    """
    self.path = path

  def get(self, version):
    """ Takes a version identifier, in any format, and returns the full text
    of that version of the entry as a string.

    Raises BadVersion if the version is invalid.
    """
    pass

  def change(self, version):
    """ Returns a dictionary with metadata about the change that created the
    given version. At minimum, the dictionary should include 'version',
    'author', 'date', and 'message'.

    Raises BadVersion if either version is invalid.
    """
    pass

  def changes(self):
    """ Returns a list of changes for each version of this entry. The changes
    are dictionaries equivalent to those returned by the change() method.
    """
    pass

  def diff(self, version1, version2):
    """ Returns a contextual diff between the two versions of the entry's
    text.

    Raises BadVersion if either version is invalid.
    """
    pass


class SubversionEntry(VersionedEntry):
  """ An entry in a subversion repository.
  """
  client = None
  entry = None

  # cache of changes for this entry: {version: change}
  __change_cache = None

  # note the except clauses. they catch pysvn.ClientError (bad pysvn usage), a
  # pysvn._pysvn.ClientError (if internal error, e.g. can't connect to the svn
  # repository), or python language error (if other bug).
  def __init__(self, path):
    VersionedEntry.__init__(self, path)
    self.client = pysvn.Client()
    self.__change_cache = {}

    try:
      self.entry = self.client.info(path)
    except Exception, msg:
      raise BadPath, msg

  def get(self, version):
    try:
      revision = pysvn.Revision(pysvn.opt_revision_kind.number, version)
      return self.client.cat(self.path, revision)
    except Exception, msg:
      raise BadVersion, msg

  def __format_change(self, change):
    """ Fixes the date and version members of the change dictionary. Converts
    date from seconds since the epoch to a string, and populates version (an
    integer) from revision (a pysvn.Revision).
    """
    change['version'] = change['revision'].number
    change['date'] = time.strftime('%Y-%m-%d', time.localtime(change['date']))

  def change(self, version):
    # check the cache
    if not self.__change_cache.has_key(version):
      # it's not there. fetch it from svn....
      try:
        rev = pysvn.Revision(pysvn.opt_revision_kind.number, version)
        changes = self.client.log(self.path, revision_start=rev, limit=1)
      except Exception, msg:
        raise BadVersion, msg

      change = changes[0]
      self.__format_change(change)
      # ...and add it to the cache
      self.__change_cache[version] = change

    return self.__change_cache[version]

  def changes(self):
    changes = self.client.log(self.path)

    for change in changes:
      self.__format_change(change)
      # populate cache
      self.__change_cache[change['version']] = change

    return changes

  def diff(self, version1, version2):
    try:
      rev1 = pysvn.Revision(pysvn.opt_revision_kind.number, version1)
      rev2 = pysvn.Revision(pysvn.opt_revision_kind.number, version2)
      return self.client.diff(
        # this is finicky. mess with it at your own risk!
        tmp_path='/tmp/pyblosxom_history_diff_tmp_file',
        # needs both url_or_path and url_or_path2, otherwise pysvn will break
        url_or_path=self.path, url_or_path2=self.path,
        revision1=rev1, revision2=rev2)
    except Exception, msg:
      raise BadVersion, msg


# if you add a backend, add it here too
BACKENDS = {'subversion': SubversionEntry}


def verify_installation(request):
  config = request.getConfiguration()

  if (not config.has_key('history_backend') or
      config['history_backend'] not in BACKENDS):
    return False

  # if your backend has additional requirements, add them here
  return True


def entry_parser(contents, request):
  """ Run the appropriate entry parser for this file. Return the data dict
  generated by the entry parser.
  """
  # this *should* be just these three lines. however,
  # pyblosxom.blosxom_entry_parser only reads from a file, *not* from a
  # string. so, until that's fixed, we have to duplicate its code below.
#   fn, ext = os.path.splitext(full_path)
#   entry_parser = data['extensions'][ext[1:]]
#   parsed = entry_parser(full_path, request)

  config = request.getConfiguration()
  entryData = {}
  
  lines = [l + '\n' for l in contents.split('\n')]
  title = lines.pop(0).strip()
  entryData['title'] = title

  # absorb meta data lines which begin with a #
  while lines and lines[0].startswith('#'):
      meta = lines.pop(0)
      meta = meta[1:].strip()     # remove the hash
      meta = meta.split(' ', 1)
      entryData[meta[0].strip()] = meta[1].strip()

  # Call the preformat function
  args = {'parser': entryData.get('parser', config.get('parser', 'plain')),
          'story': lines,
          'request': request}
  entryData['body'] = tools.run_callback('preformat', 
                                         args,
                                         donefunc = lambda x:x != None,
                                         defaultfunc = lambda x: ''.join(x['story']))

  # Call the postformat callbacks
  tools.run_callback('postformat',
                    {'request': request,
                     'entry_data': entryData})
      
  return entryData


def format_diff(diff):
  """ Formats a contextual diff for display as HTML. Fixes errant escape
  characters, strips the header, escapes special characters, and adds spans
  with meaningful classes for each line so they can be styled.
  """
  # handle special characters
  diff = diff.replace('\\.', '.')  # i don't know why periods are escaped
  diff = xml.sax.saxutils.escape(diff)

  # remove the first three header lines. they look like this:
  #
  #   ======================
  #   --- /path/to/entry.txt
  #   +++ /path/to/entry.txt
  lines = diff.split('\n')
  lines = lines[4:]

  # add spans with classes so that flavours can style appropriately
  classes = { '@': 'line-number',
              '-': 'removed',
              '+': 'added',
              '!': 'changed',
              ' ': 'context',
              }
  lines = ['<span class="diff-%s">%s</span>' % (classes[l[0]], l)
           for l in lines if l and classes.has_key(l[0])]

  diff = '\n'.join(lines) 
  return diff


def diff(versioned_entry, version1, version2):
  """ Retrieves a diff between the given versions of the requested page,
  formats the diff as HTML, generates an entry for it, and returns it.
  """
  change = versioned_entry.change(version2)

  return {
    'version1': version1,
    'version2': version2,
    'diff': format_diff(versioned_entry.diff(version1, version2)),
    'author': change['author'],
    'message': change['message'].strip(),
    'date': change['date'],
    }


def version(versioned_entry, version, request):
  """ Retrieves the given version of the entry.
  """
  change = versioned_entry.change(version)
  body = versioned_entry.get(version)
  data = entry_parser(body, request)

  data.update({
    'version': version,
    'author': change['author'],
    'message': change['message'].strip(),
    'date': change['date'],
    })

  return data


def history(versioned_entry):
  changes = versioned_entry.changes()
  diffs = []

  if len(changes) == 1:
    version = changes[0]['version']
    diffs = [diff(versioned_entry, version, version)]
    diffs[0]['diff'] = "There is only one version."
  else:
    # a list of (version1, version2) pairs to diff
    to_diff = zip(changes[1:], changes[:-1])

    for change1, change2 in to_diff:
      data = diff(versioned_entry, change1['version'], change2['version'])
      diffs.append(data)

  return {'diffs': diffs}


def generate_history_entry(request, history_fn):
  """ Calls history_fn and handles any exceptions. Then generates an entry,
  merges history_fn 's return value (a dictionary) into the entry's data,
  and returns it.

  history_fn should take an Entry and a request and return a dict.
  """
  config = request.getConfiguration()
  data = request.getData()
  filename = data['root_datadir']

  # run history_fn
  backend_class = BACKENDS[config['history_backend']]
  try:
    versioned_entry = backend_class(filename)
    entryData = history_fn(versioned_entry)
  except (BadPath, BadVersion), msg:
    tools.getLogger().error(msg)
    return  # make pyblosxom 404

  # sadly, pyblosxom seems to pretty much assume that all entries are of type
  # FileEntry. grr! add these attrs to emulate FileEntry. 
  file_basename = os.path.basename(filename)
  fn, ext = os.path.splitext(file_basename)
  absolute_path = os.path.join(*data['path_info'][:-1])
  file_path = '/'.join((absolute_path, fn))

  # construct an entry with the diff
  entryData.update({
    'filename': filename,
    'file_path': file_path,
    'fn': fn,
    'title': fn,
    'absolute_path': absolute_path,
    'nocomments': 1,  # don't show comments, or the comment form!
    })

  # generate the entry
  body = entryData.get('body', '')
  # use the epoch for mtime. otherwise, pyblosxom uses the current time, which
  # makes other plugins (like weblogsping) think this is a new entry.
  epoch = time.localtime(0)
  entry = entries.base.generate_entry(request, entryData, body, epoch)
  entry.update(entryData)
  return entry


def cb_filelist(args):
  """ Handles requests for version history pages.
  """
  request = args['request']
  form = request.getForm()

  history_fns = {
    'version': lambda (entry): version(entry, form.getvalue('version'),
                                       request),
    'diff': lambda (entry): diff(entry, form.getvalue('version1'),
                                 form.getvalue('version2')),
    'history': history,
    }

  # TODO: detect multiple action params and fail with a reasonable error
  for action in history_fns:
    if form.has_key(action):
      return [generate_history_entry(request, history_fns[action])]


def cb_story(args):
  """ Use the history templates.
  """
  request = args['request']
  form = request.getForm()
  entry = args['entry']
  renderer = args['renderer']
  flavour = renderer.flavour

  if form.has_key('version'):
    default = flavour['story']
    args['template'] = flavour.get('history-version', default)
  elif form.has_key('diff'):
    default = u'<code><pre>$diff</pre></code>'
    args['template'] = flavour.get('history-diff', default)
  elif form.has_key('history'):
    default = ''
    head = flavour.get('history-head', default)
    foot = flavour.get('history-foot', default)

    # render diffs
    output = [head]
    for data in entry['diffs']:
      entry.update(data)
      renderer.outputTemplate(output, entry, 'history-diff')

    output.append(foot)
    args['template'] = u'\n'.join(output)

  return args


class RawRenderer(renderers.blosxom.Renderer):
  """ The renderer used when responding to requests for the raw contents of
  older versions.
  """
  def __init__(self, request):
    renderers.blosxom.Renderer.__init__(self, request)

  def render(self, header = 1):
    config = self._request.getConfiguration()
    data = self._request.getData()
    response = self._request.getResponse()
    form = self._request.getForm()

    # set text/plain
    response.addHeader('Content-Type', 'text/plain')

    # pull the version's contents and send them downstream
    backend_class = BACKENDS[config['history_backend']]
    versioned_entry = backend_class(data['root_datadir'])
    contents = versioned_entry.get(form['version'].value)
    response.write(contents)


def cb_renderer(args):
  """ Handle requests for the raw contents of old versions.
  """
  request = args['request']
  form = request.getForm()

  if form.has_key('version') and form.has_key('raw'):
    return RawRenderer(request)
