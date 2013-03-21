"""
index.py
http://snarfed.org/space/pyblosxom+index
Ryan Barrett <pyblosxom@ryanb.org>

This plugin displays an alphabetical index of all entries. It uses these
optional config variables from config.py, shown here with their defaults:

py['index_trigger']            = '/site-index'
py['index_num_columns']        = 2
py['index_letters_first']      = True
py['index_title']              = 'index'
py['index_use_story_template'] = True


VERSION:
0.2

TODO:
- use a template instead of hard-coded HTML

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
import math
import os.path
import time
from Pyblosxom import tools
import Pyblosxom.entries

__author__ = 'Ryan Barrett'
__version__ = '0.2'
__url__ = 'http://snarfed.org/space/pyblosxom+index'
__description__ = 'Displays an alphabetical index of all entries.'


def verify_installation(request):
  return 1

def cb_filelist(args):
  request = args['request']
  http = request.getHttp()
  data = request.getData()
  config = request.getConfiguration()

  trigger = config.get('index_trigger', 'site-index')
  if http['PATH_INFO'] != trigger:
    return

  # get the entries
  datadir = config['datadir']
  files = tools.Walk(request, datadir)
  files.sort()

  # sort into sections, one for each letter. the dictionary is 
  # letter => (entry name, path) where path is the relative to datadir.
  sections = {}
  entry_extensions = data['extensions'].keys()

  for file in files:
    assert file.startswith(datadir)
    path, ext = os.path.splitext(file[len(datadir):])
    if ext[1:] in entry_extensions:  # strip the leading period from ext
      entry_name = os.path.basename(path)
      sections.setdefault(entry_name[0].upper(), []).append((entry_name, path))

  # extract the first letters. sort as usual, except that numbers and other
  # non-letters go *after* letters. 
  def letters_before_symbols(a, b):
    if a.isalpha() and not b.isalpha():
      return -1
    elif not a.isalpha() and b.isalpha():
      return 1
    else:
      return cmp(a, b)

  letters = sections.keys()
  if config.get('index_letters_first', 1):
    letters.sort(letters_before_symbols)
  else:
    letters.sort()

  # add the header with links to each section
  body = '<p class="index-header">'
  letter_links = ['<a href="#%s">%s</a>' % (l, l) for l in letters]
  body += ' |\n'.join(letter_links)
  body += '</p>\n<hr class="index"/>\n\n'

  # add the sections themselves, with one link per entry, in a table. the
  # number of columns is taken from the index_num_columns config variable.
  # entries are ordered down each column, in order.
  num_cols = config.get('index_num_columns', 2)

  for l in letters:
    body += '<h3 class="index">%s</h3> <a name="%s"></a n>\n' % (l, l)
    body += '<table class="index">\n'

    entries = sections[l]
    entries.sort()
    num_rows = int(math.ceil(float(len(entries)) / num_cols))

    for row in range(0, num_rows):
      # alternate the <tr> tags' class between index-row-stripe-0 and
      # index-row-stripe-1, so you can use CSS to alternate their color for
      # readability, if you want.
      body += '<tr class="index-row-stripe-%d">\n' % (row % 2)
      for col in range(0, num_cols):
        entry_index = col * num_rows + row
        if entry_index < len(entries):
          entry_name, path = entries[entry_index]
        else:
          entry_name = path = ''
        body += '<td><a href="%s">%s</a></td>\n' % (path, entry_name)
      body += '</tr>\n'

    body += '</table>\n<hr class="index"/>\n\n'

  data = {'title': config.get('index_title', 'index')}
  # use the epoch for mtime. otherwise, pyblosxom uses the current time, which
  # makes other plugins (like weblogsping) think this is a new entry.
  epoch = time.localtime(0)
  fe = Pyblosxom.entries.base.generate_entry(request, data, body, epoch)
  return [fe]

def cb_story(args):
  request = args['request']
  http = request.getHttp()
  config = request.getConfiguration()
  trigger = config.get('index_trigger', 'site-index')

  if (http['PATH_INFO'] == trigger and
      not config.get('index_use_story_template', 1)):
    title = config.get('index_title', 'index')
    args['template'] = '<h1 class="index">%s</h1>\n<hr />\n$body' % title

  return args
