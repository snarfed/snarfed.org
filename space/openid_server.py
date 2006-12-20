#!/usr/bin/python2.3
"""
openid.py
http://snarfed.org/space/pyblosxom+openid+server
Copyright 2006 Ryan Barrett

openid_server.py is a PyBlosxom plugin that implements OpenID 1.1. OpenID is a
distributed authentication protocol, ie a single sign on platform, that uses
URLs as identifiers. It allows you to prove your identity to other sites - to
post comments, for example - by logging into your own site.

This plugin also implements the Simple Registration Extension, which lets you
optionally provide your name, email address, and other information
automatically to sites that you log into with OpenID.

For more on OpenId, see http://openid.net/.

In OpenID terminology,, this plugin acts as an an OpenID Identifer and Identity
Provider. It provides an endpoint URL, handles OpenID requests on that
endpoint, allows associations, and authenticates the user with an HTML form.

To use it, first install the JanRain OpenID, Yadis, UrlJr, and ElementTree
libraries. The easiest way to do this is to download openid_libs.zip from the
page on snarfed.org, and place it in your plugins directory. Of course, if you
don't trust me - and why should you? - feel free to build and install them
yourself. See http://www.openidenabled.com/openid/libraries/python for details.

Then, add this line to your flavour's head template:

  <link rel="openid.server" href="$base_url$openid_trigger">

If you want to use SSL, hard-code your base url, like so:

  <link rel="openid.server" href="https://mysite.com$openid_trigger">

Finally, add the `openid_password` (required) and `openid_trigger` (optional)
config variables to your `config.py`:

  py['openid_password'] = '...'
  py['openid_trigger'] = '/openid'

Et voila! You should be good to go. Try it out on, for example,
http://kveton.com/blog/.

You can also provide your name, email address, and other information so that
they'll be available to sites that you log into with OpenID. Just fill in any
of these config variables in `config.py`:

  py['openid_nickname'] = 'ryan'
  py['openid_email'] = 'ryan'
  py['openid_fullname'] = 'Ryan Barrett'
  py['openid_dob'] = '1901-01-01'                # YYYY-MM-DD
  py['openid_gender'] = 'M'                      # M or F
  py['openid_postcode'] = '90001'
  py['openid_country'] = 'US'                    # an ISO-3166 country code
  py['openid_language'] = 'EN-us'                # an ISO-639 language code
  py['openid_timezone'] = 'America/Los_Angeles'  # from the tz database

Currently, only one user is supported. Also, currently, this plugin does *not*
act as an OpenId client to authenticate comments made on your site. It only
acts as an OpenId Identity Provider, to authenticate you when you comment on
*other* sites.

Default HTML is included for the endpoint, login, and error pages. for your
CSS styling pleasure, it uses divs with the classes `openid-info`,
`openid-login`, and `openid-error`.

You can override the default HTML by adding `openid-info`, `openid-login`, and
`openid-error` templates for your flavour of choice. Example templates for the
html flavour are included in openid_server-0.3.tar.gz.


This program is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
details.
"""

__author__ = 'Ryan Barrett <pyblosxom at ryanb dot org>'
__version__ = '0.3'
__url__ = 'http://snarfed.org/space/pyblosxom+openid+server'
__description__  = 'Implements OpenID 1.1 (http://openid.net/)'

import BaseHTTPServer
import cgi
import Cookie
import os
import sys
import traceback
import urlparse
from Pyblosxom import entries
from Pyblosxom import tools

# the names of the modules we import from the janrain openid libraries.
# they're big, so we import them lazily, only if we're actually handling the
# request, in cb_handle().
FileOpenIDStore = None
OpenIDServer = None

# this is the global openid server instance
oidserver = None


DEFAULT_TRIGGER = '/openid'

DEFAULT_INFO_TEMPLATE = """
<div class="openid-info">
<p>
This is an <a href="http://openid.net/">OpenID</a> server endpoint served by
<a href=""" + __url__ + """>openid_server.py</a>. It's meant for computers,
not people!
</p>
</div>
"""

DEFAULT_LOGIN_TEMPLATE = """
<div class="openid-login">
<h3>OpenID Request</h3>
<p>
You have been asked by
<a href="$openid_trust_root">$openid_trust_root</a>
to log in to OpenID. Please enter the password or click Cancel.
</p>
<form method="post" action="$openid_login_url">
<input type="password" name="password" />
<input type="checkbox" name="remember" value="yes" />
  Remember (requires cookies) <br />
<input type="submit" name="continue" value="Continue" />
<input type="submit" name="cancel" value="Cancel" />
<!-- these are used to recreate the OpenID CheckIDRequest -->
<input type="hidden" name="openid.mode" value="$openid_mode" />
<input type="hidden" name="openid.immediate" value="$openid_immediate" />
<input type="hidden" name="openid.identity" value="$openid_identity" />
<input type="hidden" name="openid.trust_root" value="$openid_trust_root" />
<input type="hidden" name="openid.return_to" value="$openid_return_to" />
<input type="hidden" name="openid.assoc_handle" value="$openid_assoc_handle" />
</form>
</div>
"""

DEFAULT_ERROR_TEMPLATE = """
<div class="openid-error">
<p>
<a href=""" + __url__ + """>openid_server.py</a> encountered an
<a href="http://openid.net/">OpenID</a> error:
<pre><code>$openid_error</code</pre>
</p>
</div>
"""


def FieldStorage_to_dict(form):
  return dict([(key, form[key].value) for key in form.keys()])


def respond(request, oidresponse):
  config = request.getConfiguration()
  response = request.getResponse()
  data = request.getData()

  # add extra fields with Simple Registration Extension:
  # http://www.openidenabled.com/openid/simple-registration-extension/
  if oidresponse.request.mode in ['checkid_immediate', 'checkid_setup']:
    for field in ['nickname', 'email', 'fullname', 'dob', 'gender', 'postcode',
                  'country', 'language', 'timezone']:
      if config.has_key('openid_%s' % field):
        oidresponse.addField('sreg', field, config['openid_%s' % field])

  try:
    webresponse = oidserver.encodeResponse(oidresponse)
  except OpenIDServer.EncodingError, why:
    # cb_filelist() will show an error page
    data['openid_error'] = cgi.escape(why.response.encodeToKVForm())
    return False

  # send the response
  messages = BaseHTTPServer.BaseHTTPRequestHandler.responses
  message = messages.get(int(webresponse.code), '')
  response.setStatus('%s %s' % (webresponse.code, message))

  for header, value in webresponse.headers.iteritems():
    response.addHeader(header, value)

  if webresponse.body:
    response.write(webresponse.body)
  return True

def has_cookie(request):
  cookies = request.getHttp().get('HTTP_COOKIE', None)
  if cookies:
    morsel = Cookie.BaseCookie(cookies).get('openid_remembered')
    if morsel and morsel.value == 'yes':
      return True

  return False

def import_and_initialize(request):
  config = request.getConfiguration()
  data = request.getData()

  for dir in config['plugin_dirs']:
    sys.path.append(os.path.join(dir, 'openid_libs.zip'))

  # this lets us import modules in a function, but still bind their names in
  # the global namespace
  global FileOpenIDStore
  global OpenIDServer
  global server
  try:
    from openid.store.filestore import FileOpenIDStore
    from openid.server import server as OpenIDServer
  except ImportError, e:
    msg = ('Could not find the JanRain OpenId libraries in:\n' +
           '\n'.join(sys.path))
    data['openid_error'] = msg
    return False

  # initialize the OpenID server
  global oidserver
  if not oidserver:
    try:
      store = FileOpenIDStore(config['datadir'])
      oidserver = OpenIDServer.Server(store)
    except Exception:
      exception = traceback.format_exception(*sys.exc_info())
      msg = 'Error initializing OpenID server:\n' + '\n'.join(exception)
      data['openid_error'] = msg
      return False

  return True


def verify_installation(request):
  if not request.getConfiguration().has_key('openid_password'):
    print 'The openid_password config variable is not set.'
    return False

  return True

def cb_handle(args):
  global OpenIDServer
  global oidserver

  request = args['request']
  config = request.getConfiguration()
  data = request.getData()
  http = request.getHttp()
  form = FieldStorage_to_dict(http['form'])
  response = request.getResponse()
  trigger = config.get('openid_trigger', DEFAULT_TRIGGER)

  # the libs are big, and importing them incurs a performance hit, so only
  # import them if we're actually handling the request.
  if http['PATH_INFO'].startswith(trigger):
    if not import_and_initialize(request):
      return False

  if http['PATH_INFO'] == trigger:
    # handle the openid request
    try:
      oidrequest = oidserver.decodeRequest(form)
      if not oidrequest:
        # cb_filelist() will show an info page about this endpoint
        return False
  
      if oidrequest.mode in ['checkid_immediate', 'checkid_setup']:
        if has_cookie(request):
          tools.getLogger().info('Has cookie, confirming identity to ' +
                                 oidrequest.trust_root)
          return respond(request, oidrequest.answer(True))
        elif oidrequest.immediate:
          oidresponse = oidrequest.answer(
            False, server_url=config['base_url'] + trigger)
          return respond(request, oidresponse)
        else:
          data['openid_login_oidrequest'] = oidrequest
          # cb_filelist will show a login page
          return False

      elif oidrequest.mode in ['associate', 'check_authentication']:
        return respond(request, oidserver.handleRequest(oidrequest))
      else:
        # cb_filelist() will show an info page about this endpoint
        return False

    except OpenIDServer.ProtocolError, why:
      return respond(request, why)

  elif http['PATH_INFO'] == trigger + '/login':
    try:
      oidrequest = OpenIDServer.CheckIDRequest.fromQuery(form)
    except:
      exception = traceback.format_exception(*sys.exc_info())
      data['openid_error'] = ('Error decoding login request:\n%s\n%s' %
                              (str(form), '\n'.join(exception)))
      # cb_filelist() will show an error page
      return False

    if form.has_key('continue') and form.has_key('password'):
      if form['password'] == config['openid_password']:
        if form.get('remember', '') == 'yes':
          tools.getLogger().info('Setting cookie to remember openid login')
          response.addHeader('Set-Cookie', 'openid_remembered=yes')
        tools.getLogger().info('Logged in, confirming identity to ' +
                               oidrequest.trust_root)
        return respond(request, oidrequest.answer(True))
      else:
        data['openid_error'] = 'Incorrect password. Click Back to try again.'
        # cb_filelist() will show an error page
        return False

    elif form.has_key('cancel'):
      tools.getLogger().info('Login cancelled, sending cancel to ' +
                             oidrequest.trust_root)
      return respond(request, oidrequest.answer(False))

    else:
      data['openid_error'] = 'Bad login request:' + str(form)
      # cb_filelist() will show an error page
      return False

  return False


def cb_filelist(args):
  request = args['request']
  http = request.getHttp()
  data = request.getData()
  config = request.getConfiguration()
  trigger = config.get('openid_trigger', DEFAULT_TRIGGER)

  if http['PATH_INFO'].startswith(trigger):
    data['openid_login_url'] = trigger + '/login'

    if data.has_key('openid_login_oidrequest'):
      openid_form = vars(data['openid_login_oidrequest'])
      openid_data = dict([('openid_' + key, val)
                          for key, val in openid_form.items()])
      data.update(openid_data)

    fe = entries.base.generate_entry(request, data, '', None)
    return [fe]


def cb_story(args):
  request = args['request']
  http = request.getHttp()
  data = request.getData()
  config = request.getConfiguration()
  flavour =  args['renderer'].flavour
  trigger = config.get('openid_trigger', DEFAULT_TRIGGER)

  if http['PATH_INFO'].startswith(trigger):
    if data.has_key('openid_login_oidrequest'):
      tools.getLogger().info('Login request: ' +
                             str(data['openid_login_oidrequest']))
      args['template'] = flavour.get('openid-login', DEFAULT_LOGIN_TEMPLATE)
    elif data.has_key('openid_error'):
      tools.getLogger().error('error: ' + data['openid_error'])
      args['template'] = flavour.get('openid-error', DEFAULT_ERROR_TEMPLATE)
    else:
      args['template'] = flavour.get('openid-info', DEFAULT_INFO_TEMPLATE)

  return args
