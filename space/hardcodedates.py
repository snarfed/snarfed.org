# -*- mode: python; indent-tabs-mode: t, tab-width: 4 -*-
"""
This allows the user to create a file "timestamps" in their datadir,
that will override the timestamp of any given blog entry. Each line
in this file should be of the form "YYYY-MM-DD-hh-mm file-name".
Then for any entry that one of these lines exist the system will use
that timestamp instead of the actual files modification time.

Note: the filename is relative to your data-dir.
Example of a line for the file /var/data-dir/school/abc.txt
   where the datadir is "/var/data-dir/" and the date is Aug 9, 2004.

2004-08-09-00-00 school/abc.txt
"""
__author__ = 'Nathan Kent Bullock'
__homepage__ = 'http://bullock.moo.com/nathan/'
__email__ = 'nathan_kent_bullock -at- yahoo.ca'
__version__ = '1.2'

from Pyblosxom import tools
import os, os.path, re, time

FILETIME = re.compile('^([0-9]{4})-([0-1][0-9])-([0-3][0-9])-([0-2][0-9])-([0-5][0-9]) +(.*)$')

all_timestamps = None
extensions = None
timestamps_to_save = {}

def get_all_timestamps(datadir):
	f = open(datadir + "/timestamps")
	t = {}
	for str in f.readlines():
		m = FILETIME.search(str.strip())
		if m:
			year = int(m.group(1))
			mo = int(m.group(2))
			day = int(m.group(3))
			hr = int(m.group(4))
			minute = int(m.group(5))
			mtime = time.mktime((year,mo,day,hr,minute,0,0,0,-1))
			
			t[datadir + "/" + m.group(6)] = mtime

	f.close()
	return t

def cb_filestat(args):
	global all_timestamps

	filename = args["filename"]

	if all_timestamps.has_key(filename):
		# we know this file's timestamp
		args['mtime'][8] = all_timestamps[filename]

	elif os.path.splitext(filename)[1] in extensions:
		# we don't know it, but we should. ask the os for it, and remember it.
		args['mtime'] = os.stat(filename)
		all_timestamps[filename] = args['mtime'][8]
		timestamps_to_save[filename] = args['mtime'][8]

	return args

def cb_start(args):
	global all_timestamps, extensions
	request = args['request']

	all_timestamps = get_all_timestamps(args["request"].getConfiguration()['datadir'])

	extensions = request.getData()['extensions'].keys()
	extensions.append(request.getConfiguration().get('comment_ext', ''))

def cb_end(args):
	if timestamps_to_save:
		datadir = args["request"].getConfiguration()['datadir']
		tsfile = file(datadir + "/timestamps", 'a')
		for filename, mtime in timestamps_to_save.items():
			time_str = time.strftime('%Y-%m-%d-%H-%M', time.localtime(mtime))
			tilename = filename[len(datadir) + 1:]
			tsfile.write('%s %s\n' % (time_str, filename))
			tools.getLogger().info('Saved mtime %s for %s' % (time_str, filename))
		tsfile.close()
