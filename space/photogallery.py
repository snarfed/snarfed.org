# -*- mode: python; indent-tabs-mode: t, tab-width: 8 -*-
"""
This plugin kicks up a photo gallery when accessed with the set trigger.

It REQUIRES either the imagekicker or filekicker plugin.

Directory structure should look like this

<imagefolder>
|
--<trigger>
  |
  --<Gallery name>
    |
    --thumbs (name of the folder is thumbs, yes. contains thumbnails)
    |
    --<images>
 
It also requires some variables in the config.
py['base_url'] is now required.

py['imagedata'] = '<path to map with images>' is required by imagefile, and also here.
py['gallerytrigger'] = '<trigger>' is required.
py['gallery_use_story_template'] = True/False is optional, defaulting to True.

VERSION:
0.5 Added gallery_use_story_template config option
0.4 Added head and foot templates, and config options
0.3 Now works with comments!
0.2 It now can co-exist with comments. It can't use them, but it can co-exist.
0.1 First release. Some issues

TODO:
Make sure it is not so syntax fragile.
Make sure it handles flavours correctly.
Make sure it is correctly commented

LICENCE: GPL

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
"""
import os, string
from Pyblosxom import tools, entries, pyblosxom

__author__ = "Magnus Nordlander magnus (at) nordlander (dot) tk"
__version__ = "0.5 (27 April, 2004)"
__url__ = "http://magnus.nordlander.tk/"
__description__ = "Displays a photo gallery on a given trigger"

def verify_installation(request):
    config = request.getConfiguration()
    import os.path

    goodinstall = 1

    if not config.has_key("base_url"):
	print "Required variable base_url not set"
	goodinstall = 0
    
    if not config.has_key("imagedata"):
	print "Required variable imagedata not set"
	goodinstall = 0
	
    if not config.has_key("gallerytrigger"):
	print "Required variable gallerytrigger not set"
	goodinstall = 0
    return goodinstall

def cb_filelist(args):
    request = args["request"]

    pyhttp = request.getHttp()
    data = request.getData()
    config = request.getConfiguration()

    baseurl = config['base_url']
    trigger = config['gallerytrigger']
    captions = config.get('gallerycaptions', True)
    columns = config.get('gallerycolumns', 4)
    imagepath = config['imagedata'] + trigger
    gallery = pyhttp["PATH_INFO"][len(trigger) + 1:]

    #Check if we have been started with the trigger
    if not pyhttp["PATH_INFO"].startswith(trigger):
        return
    
    #Check whether we are going to access a list of galleries, a gallery, or an image 
    if pyhttp["PATH_INFO"] == trigger or pyhttp["PATH_INFO"] == trigger + "/" :
	#Find out what folders there are
	dirlist = os.listdir(imagepath)
	nicelist = []
	#Generate list of galleries
	for x in dirlist:
	    nicelist.append("<a href=\"" + baseurl + trigger + "/" + x + "\">" + x + "</a><br />")
	contents = string.join(nicelist)
	#Generate entry and return it
	absolute_path = ""
	fn = trigger[1:]
	file_path = trigger[1:]
	fe = entries.base.generate_entry(request, {"title": "Gallery", "absolute_path": absolute_path, "file_path": file_path, "fn": fn }, contents, None)
	return [fe]	
    elif string.find(gallery, "/") == -1:
	#find out what images we have
	abspath = imagepath + "/" + gallery + "/thumbs"
	if not os.path.isdir(abspath):
	    return
	list = os.listdir(abspath)
	i = str(len(list))
	#generate table with thumbnails and 
	stringlist = []
	stringlist.append("<table>")
	j = 1
	stringlist.append("<tr>")
	for x in list:
	    stringlist.append("<td><a href=\"" + baseurl + trigger + "/" + gallery + "/" + x + "\"><img src=\"" + baseurl + trigger + "/" + gallery + "/thumbs/" + x + "\" alt=\"" + x + "\" /></a><br />")
	    if captions:
		stringlist.append(x)
	    stringlist.append("</td>")
	    j = 1 + j
	    if j % columns == 1:
		stringlist.append("</tr>")
		stringlist.append("<tr>")
	stringlist.append("</tr></table>")
	contents = string.join(stringlist)
	absolute_path = trigger[1:]
	fn = gallery
	file_path = absolute_path + "/" + fn
	#generate entry and return it
	data = {"title": gallery, "absolute_path": absolute_path,
		"file_path": file_path, "fn": fn}

	# parse the header and footer
	for filename in ['foot.txt', 'head.txt']:
	    path = os.path.join(config['datadir'], file_path, filename)
	    if os.path.isfile(path):
		data.update(pyblosxom.blosxom_entry_parser(path, request))
		data[filename] = data['body']
	    else:
		data[filename] = ''

	contents = '\n\n'.join([data['head.txt'], contents, data['foot.txt']])

	fe = entries.base.generate_entry(request, data, contents, None)
	return [fe]

    else:
	l = string.find(gallery, "/")
	image = gallery[l+1:]
	#generate entry and return it
	contents = "<br /><img src=\"" + baseurl + trigger + "/" + gallery + "\" alt=\"" + image + "\" /><br />"
	i = string.find(gallery, "/") 
	absolute_path = trigger[1:] + "/" + gallery[:i]
	fn = image
	file_path = absolute_path + "/" + fn
	fe = entries.base.generate_entry(request, {"title": image, "absolute_path": absolute_path, "file_path": file_path, "fn": fn }, contents, None)
	return [fe]

def cb_story(args):
  request = args['request']
  http = request.getHttp()
  config = request.getConfiguration()
  trigger = config['gallerytrigger']

  if (http['PATH_INFO'].startswith(trigger) and
      not config.get('gallery_use_story_template', 1)):
    args['template'] = '<h1 class="photogallery">$title</h1>\n<hr />\n$body'

  return args
