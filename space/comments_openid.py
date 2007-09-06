"""
OpenID for the PyBlosxom comments plugin.
http://snarfed.org/space/pyblosxom+openid+comments
Copyright 2006-2007 Josh Hoyt, Kevin Turner, Ryan Barrett

Lets users use OpenID to post comments. Allows for enabling comments
for specific users only.

Requirements
============

  - Steven Armstrong's compatibility plugin if not using pyblosxom 1.2+
  - Steven Armstrong's session plugin (http://www.c-area.ch/code/pyblosxom/plugins/ )
  - comments.py (http://pyblosxom.sf.net/blog/registry/input/comments/comments )
  - Python OpenID Libraries

Installation
============

It's a good idea to get your comments working how you want them before
adding OpenID authentication. You can get documentation and code for
the comments plugin from the contrib tarball at:

https://sourceforge.net/project/showfiles.php?group_id=67445&package_id=145140


  1. Put comments_openid.py in your plugin directory. Install the
     JanRain Python OpenID library from openidenabled.com.

  2. Make sure that py['load_plugins'] contains at least:
     ['comments', 'session', 'comments_openid']

  3. Create a directory for putting the OpenID session data and set
     py['openid_store_dir'] = '/path/to/data/directory/'

  4. Add something like this to your comment-form.html template:
    <label for="openid_url">OpenID Identity URL:</label><br />
    <input name="openid_url" id="openid_url" type="text"
           value="$cmt_openid_url" width="50" /><br />
    <br />

    If you are requiring OpenID, it's probably a good idea to get rid
    of the normal comment URL field.

Configuration
=============

This plugin has several configuration options. The only required
configuration is the 'openid_store_dir'. The configuration parameters are:

  - config['openid_store_dir'] ** REQUIRED
    A directory for the OpenID library to use to store information
    about OpenID servers and logins. This directory should be outside
    of your public web space.

  - config['openid_trust_root']
    Trust root for the OpenID Request. Defaults to the base URL of your page.
    (All URLs should fall under this directory)

  - config['openid_required']
    If this is set to True, do not allow comments unless OpenID
    authentication succeeds. Defaults to False.

  - config['openid_reject_identity']
  - config['openid_reject_server']
    This is a list of patterns for identities to always reject. If
    this is not set, no URLs are blacklisted. The patterns follow the
    form of OpenID trust roots, which are basically URLs that allow *
    in the domain name to match any subdomain. For more details, see
    the entry on openid.trust_root in
    http://openid.net/specs.bml#mode-checkid_immediate.

  - config['openid_allow_identity']
  - config['openid_allow_server']
    This is a list of patterns for identities to allow. If this option
    is set, *only* URLs that match a pattern in this list are
    allowed. If this option is not set, all URLs are whitelisted.

Changelog:
  0.2
  - updated to work with janrain's python openid libraries 1.x and 2.x
  - added nickname support using Simple Registration Extension 
  - moved to snarfed.org
  0.1
  - initial release on openidenabled.com
"""

__author__ = 'Josh Hoyt <josh at janrain dot com>'
__author__ = 'Kevin Turner <kevin at janrain dot com>'
__author__ = 'Ryan Barrett <pyblosxom at ryanb dot org>'
__version__ = '0.2'
__url__ = 'http://snarfed.org/space/pyblosxom+openid+comments'
__description__  = 'OpenID commenting support for PyBlosxom'

# Python imports
#import cgitb; cgitb.enable() # for debugging
import time
import cgi
from urlparse import urlparse

# import the openid python librarie from openid_server.py's zip file.
# this assumes that they're in the same directory as this plugin.
import os
import sys
sys.path.append(os.path.join(os.path.dirname(__file__), 'openid_libs.zip'))

# OpenID imports
from openid.server import trustroot
from openid.consumer import consumer as openid
from openid.consumer.discover import DiscoveryFailure
from openid.store import filestore
from openid.oidutil import appendArgs

# Pyblosxom session plugin
try:
    from session import Session
except ImportError:
    Session = None

from Pyblosxom import tools

comment_form_fields = ['title', 'author', 'openid_url', 'email', 'url', 'body']
comment_fields = ['title', 'pubDate', 'link', 'source', 'description']
sid_field = 'cmt_openid_sid'

class OpenIDCommentError(RuntimeError):
    """Specific exception for errors in the OpenID verification process"""

def verify_installation(request):
    config = request.getConfiguration()
    retval = 1
    
    if Session is None:
        print "Missing required plugin session.py."
        retval = 0

    try:
        import comments
    except ImportError:
        print "Missing required plugin comments.py."
        retval = 0

    try:
        import compatibility
    except ImportError:
        print "If you're not running the WSGI version of Pyblosxom"
        print "you'll need the 'compatibility.py' plugin."
    
    return retval

def get_openid_consumer(request, session):
    """Initialize an OpenID store for authenticating comments.

    @param request: Pyblosxom request object

    @param session: session
    @type session: instance of C{Session}

    @return: An instance of OpenIDConsumer
    @rtype: OpenID consumer store or C{None}
    """
    config = request.getConfiguration()

    store_dir = config.get('openid_store_dir')
    if store_dir is None:
        tools.getLogger().error('You must define openid_store_dir in your '
                                ' config to enable OpenID comments')
        return None
    else:
        store = filestore.FileOpenIDStore(store_dir)
        return openid.Consumer(session, store)

def check_url_matches(patterns, url):
    """Check to see if C{url} matches one of C{patterns}. For a URL
    to match, it must:
    
     * Have the same protocol as the pattern (if the pattern has a protocol)

     * Match the domain-name, with *.example.com matching any
       subdomain of example.com

     * The path of the pattern must be a prefix of the path of the URL

    @param patterns: The patterns against which to match the URL
    @type patterns: C{[str]}

    @param url: The URL to check against the patterns
    @type url: C{str}

    @return: Whether any pattern in patterns matches url
    @rtype: C{bool}
    """
    # assign a default protocol 
    default_proto = urlparse(url)[0]
    if not default_proto:
        url = 'http://' + url
        default_proto = 'http'

    for pattern in patterns:
        if default_proto and '://' not in pattern:
            pattern = '%s://%s' % (default_proto, pattern)
        if trustroot.TrustRoot.checkURL(pattern, url):
            return True

    return False

def start_openid_auth(request, openid_url):
    form = request.getForm()
    config = request.getConfiguration()
    data = request.getData()
    session = Session(request)

    # Try to start OpenID verification
    consumer = get_openid_consumer(request, session)
    if consumer is None:
        return

    if check_url_rejected(config, openid_url, 'identity'):
        tools.getLogger().info('Rejected %r' % (openid_url,))
        raise OpenIDCommentError(
            'That identity is not allowed to post to this blog')

    try:
        auth_req = consumer.begin(openid_url)
    except DiscoveryFailure:
        fmt = "Unable to use <q>%s</q> as an identity URL"
        msg = fmt % (cgi.escape(openid_url),)
        raise OpenIDCommentError(msg)

    # Make sure that the server and identity URL are allowed by the config
    server_id = auth_req.endpoint.getServerID()
    server_url = auth_req.endpoint.server_url
    if (check_url_rejected(config, server_id, 'identity') or
        check_url_rejected(config, server_url, 'server')):
        tools.getLogger().info('Rejected %r or %r' % (server_id, server_url))
        raise OpenIDCommentError(
            'That identity is not allowed to post to this blog')

    if 'body' not in form:
        raise OpenIDCommentError('Comment body required')

    if 'openid_trust_root' in config:
        trust_root = config['openid_trust_root']
    else:
        trust_root = config['base_url']

    # Save the data for the return
    populate_comment_session(session, request, auth_req.token, trust_root)
    args = {sid_field: session.id(), 'showcomments': 'yes',}
    return_to = appendArgs(data['url'], args)

    redirect_url = auth_req.redirectURL(trust_root, return_to)

    renderer = data['renderer']
    renderer.addHeader('Status', '302 Found')
    renderer.addHeader("Location", redirect_url)
    renderer.showHeaders()
    renderer.rendered = 1

def populate_comment_session(session, request, token, trust_root):
    cmt_data = {}
    form = request.getForm()
    for k in comment_form_fields:
        if form.has_key(k):
            cmt_data[k] = form[k].value

    session["token"] = token
    session['trust_root'] = trust_root
    session["data"] = cmt_data
    session.save()

def verify_return_to(return_to, trust_root, session_id):
    if not trust_root or not return_to.startswith(trust_root):
        return False

    # Make sure that the session that we loaded actually is the
    # session that went with the return_to
    qs = urlparse(return_to)[4]
    for k, v in cgi.parse_qsl(qs):
        if k == sid_field:
            return v == session_id

    # No sid_field found
    return False

def complete_openid_auth(request, session_id):
    """attempt to resume the session given by the session ID. Check
    the OpenID server response and if it responds that this user is
    who he says he is, then complete the comment posting.

    @param request: Pyblosxom request object

    @param session_id: The session id of the original comment post
    @type session_id: str

    @return: None
    """
    form = request.getForm()
    config = request.getConfiguration()
    data = request.getData()
    session = Session(request, session_id)

    consumer = get_openid_consumer(request, session)
    if consumer is None:
        return

    query = {}
    for k in form:
        query[k] = form.getfirst(k)

    # Get the session and the data that we need out of it in order to
    # complete the request
    sess = Session(request, session_id)

    # Actual user-filled comment data
    cmt_data = sess.get('data', {})

    # Auth token from the OpenID library
    token = sess.get('token')
    trust_root = sess.get('trust_root')

    try:
        # Make sure that the return_to URL that was sent by the server has
        # the same session ID as the current request
        return_to = query.get('openid.return_to', '')
        if (not token or
            not verify_return_to(return_to, trust_root, session_id)):
            raise OpenIDCommentError('Error handling OpenID response')

        # Ask the OpenID library to check the server's response
        response = consumer.complete(query)
        if response.status == openid.SUCCESS:
            if response.identity_url is None:
                raise OpenIDCommentError('OpenID authentication cancelled')
            else:
                cmt_data['openid_url'] = response.identity_url

                # get the user's nickname using Simple Registration Extension
                sreg = response.extensionResponse('sreg')
                if 'nickname' in sreg:
                    cmt_data['author'] = sreg['nickname']

                save_comment(request, cmt_data)

                for key in comment_form_fields:
                    key = "cmt_%s" % key
                    if key in data:
                        del data[key]

                # Now that we completed the authorization transaction,
                # delete the session.
                sess.delete()
        elif response.status == openid.FAILURE:
            fmt = 'Error handling OpenID response for identity <q>%s</q>'
            raise OpenIDCommentError(fmt % (cgi.escape(response.message),))
        else:
            raise OpenIDCommentError('Error handling OpenID response')
    except OpenIDCommentError, why:
        data['comment_message'] = why[0]
        for field_name in comment_form_fields:
            data['cmt_' + field_name] = cmt_data.get(field_name, '')

def save_comment(request, cmt_data):
    """Call the comments plugin to finish handling the comment data.
    
    This code is mostly copied from comments.cb_prepare, but it was
    not exposed in the way it needed to be in order to re-use it.

    @param request: The current request
    @type request: Pyblosxom request object

    @param cmt_data: The comment data
    @type cmt_data: {str:str}
    """
    from comments import add_dont_follow, massage_link, sanitize, writeComment
    config = request.getConfiguration()
    enc = config['blog_encoding']

    for k, v in cmt_data.iteritems():
        if type(v) is type(''):
            cmt_data[k] = v.decode(enc)

    openid_url = cmt_data['openid_url']

    body = add_dont_follow(sanitize(cmt_data['body']), config)

    cmt_time = time.time()
    w3cdate = time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime(cmt_time))
    date = time.strftime('%a %d %b %Y', time.gmtime(cmt_time))
    cdict = {'title': cmt_data['title'],
             'pubDate': str(cmt_time),
             'w3cdate': w3cdate,
             'cmt_date': date,
             'link': massage_link(cmt_data.get('url', openid_url)),
             'source': '',
             'email': cmt_data.get('email', ''),
             'description': body,
             'author': cmt_data.get('author', openid_url),
             'openid_url': openid_url,
            }

    data = request.getData()
    data["comment_message"] = writeComment(request, config, data, cdict, enc)

def check_url_rejected(config, url, suffix):
    """Return whether URL is allowed by the config. This uses
    C{config['openid_allow_' + suffix]} and C{config['openid_reject_'
    + suffix]} to determine if C{url} is allowed to comment.

    @param config: Pyblosxom configuration dictionary
    @type config: C{dict}

    @param url: The URL to check
    @type url: C{str}

    @param suffix: Which variables in the config to use
    @type suffix: C{str}

    @return: Whether to reject comments made with this URL
    @rtype: C{bool}
    """
    allow_only = config.get('openid_allow_' + suffix, None)
    if (allow_only is not None and
        not check_url_matches(allow_only, url)):
        # Did not match allow_only, so reject
        return True

    reject_always = config.get('openid_reject_' + suffix, None)
    if (reject_always is not None and
        check_url_matches(reject_always, url)):
        # Matched reject_always, so reject
        return True

    # Survived allow_only and reject_always
    return False

#******************************
# Callbacks
#******************************

def cb_prepare(args):
    """
    Handle comment-related posts, enabling OpenID

    @param request: pyblosxom request object
    @type request: a Pyblosxom request object
    """
    request = args["request"]
    data = request.getData()
    form = request.getForm()
    config = request.getConfiguration()

    if not config.get('openid_enabled', True):
        return

    # Session id of None generates a new session
    session_id = form.getfirst(sid_field, None)

    if session_id is not None:
        msg = complete_openid_auth(request, session_id)
    elif 'preview' not in form:
        try:
            openid_url = form.getfirst('openid_url')
            if openid_url:
                start_openid_auth(request, openid_url)
            elif config.get('openid_required', False):
                raise OpenIDCommentError(
                    'OpenID authentication is required to post')
        except OpenIDCommentError, why:
            data['comment_message'] = why[0]
            for field_name in comment_form_fields:
                data['cmt_' + field_name] = form.getfirst(field_name, '')

def cb_comment_reject(args):
    """Reject comments that were not generated by this plugin, as a
    way of disabling the comments plugin's native form handlers.

    @param request: pyblosxom request object
    @type request: a Pyblosxom request object

    @param comment: the data that will be saved if we do not reject it
    @type request: {str:str}
    """
    comment = args['comment']
    request = args['request']
    config = request.getConfiguration()

    # Do not reject any comments if OpenID is disabled
    if not config.get('openid_enabled', True):
        return False

    form = request.getForm()
    session_id = form.getfirst(sid_field, None)

    # This is an initial post of an OpenID comment and this was
    # triggered by the regular comments plugin. Reject this comment
    # until we get OpenID auth finished.
    if 'openid_url' in form:
        return True

    if session_id or 'openid_url' in comment:
        # Allow a comment if the auth_method is OpenID and a URL is set
        openid_url = comment.get('openid_url')
        if openid_url:
            del comment['openid_url']
            return check_url_rejected(config, openid_url, 'identity')
        else:
            return True
    else:
        # If OpenID is required, reject any comments that do not have
        # an OpenID URL
        return config.get('openid_required', False)
