#!/usr/bin/python2
#
# cdname.py
# Copyright 2003-2004 Ryan Barrett <cdname.py@ryanb.org>
#
# Homepage: http://snarfed.org/space/cdname.py
# See docstring for usage details.
#
# Thanks to xunker@pyxidis.org for his page on the M3U file format:
#   http://hanna.pyxidis.org/tech/m3u.html
#
# KNOWN ISSUES:
# - the M3U playlist format includes the length of each song, in seconds.
# rather than parse the ID3 tag, cdname cheats and uses 1 second. the resulting
# playlist works fine, but each song will be displayed as 1 second long in most
# MP3 player until it is played, when the MP3 player parses the ID3 tag itself.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.



USAGE = """Usage: cdname.py [OPTION]... MP3FILE...
Names mp3 files according to the artist, album, track, and number in a matching
cddb .inf file (usually retrieved with cdda2wav -L). Each mp3 file should have
a corresponding .inf file with the same filename, minus extension. Can also
optionally generate an M3U playlist.

Options:
  -n        Include track number
  -a        Include album title
  -p        Generate an M3U playlist into "Performer - Album Title.m3u"
  -u        Avoid whitespace; use underscores instead
  -l        Use lower case letters only
  -t        Test run; don't rename, just print what the new names would be
  -d delim  Delimiter used to separate artist, album, track, etc.
            Defaults to " - " (without quotes)
  -v        Display verbose debugging messages
  -V        Print version information and exit
  -h        Display this message

Web: http://snarfed.org/space/cdname.py"""

import sys
import os
import string
import re
import getopt

# constants
VERSION = 'cdname.py 0.3'
SPECIAL_CHARS = {   # characters that aren't allowed in *nix filenames
    '/': '-',
    '\\': '-',
    '"': "'",
    '?': '',
    '<': '(',
    '>': ')',
    ':': ' -' }

# command-line options
NUMBERS   = False
ALBUM     = False
PLAYLIST  = False
TEST_RUN  = False
VERBOSE   = False
DELIMITER = ' - '


def main(mp3s):
    global PLAYLIST
    playlist_file = None

    for mp3 in mp3s:
        # does the input file exist?
        try:
            open(mp3, 'r')
        except:
            log(mp3 + ' does not exist.', error=True)
            continue

        # find and parse inf flie
        inf = find_inf(mp3)
        if not inf:
            log('No inf file exists for %s.' % mp3, error=True)
            continue

        log('Parsing ' + inf.name)
        infdata = inf.read();
        artist = get_inf_value(infdata, 'Performer')
        album  = get_inf_value(infdata, 'Albumtitle')
        track  = get_inf_value(infdata, 'Tracktitle')
        number = get_inf_value(infdata, 'Tracknumber')
        log('Extracted ' + `(number, artist, album, track)`)

        # build new filename for mp3
        newmp3 = track + '.mp3'
        if NUMBERS and number:
            newmp3 = '%02d ' % int(number) + newmp3
        if ALBUM and album:
            newmp3 = album + DELIMITER + newmp3 

        # prepend the artist's name to the mp3 filename, but *only* if it's not
        # a compilation (ie if the track doesn't have an artist name). this is
        # determined by a crude heuristic - whether the track name has a dash.
        #
        # temporarily removed this heuristic, for consistency.
        #
#        if string.find(track, '-') == -1:
        newmp3 = artist + DELIMITER + newmp3

        # sanitize filename to avoid special chars
        dirty = newmp3
        newmp3 = sanitize(newmp3)
        if (newmp3 != dirty):
            log('Sanitized filename to %s' % newmp3)

        # does the output file exist?
        if newmp3 and os.access(newmp3, os.F_OK):
            # yes; don't clobber it!
            print >> sys.stderr, (newmp3 +
                                  ' exists, cowardly refusing to overwrite.')
            continue

        # nope...rename away!
        if TEST_RUN:
            print newmp3
        else:
            os.rename(mp3, newmp3)
            log('Renamed %s to %s' % (mp3, newmp3))

        # add to playlist
        if PLAYLIST and not TEST_RUN:
            # open the playlist file if necessary. (have to do this lazily
            # because we need the artist and album from an inf file.)
            if not playlist_file:
                filename = sanitize(artist + DELIMITER + album + '.m3u')
                if os.access(filename, os.F_OK): 
                    # the playlist file already exists; don't clobber it!
                    print >> sys.stderr, (filename + ' exists, cowardly ' +
                                          'refusing to overwrite.')
                    PLAYLIST = None
                    continue
                else:
                    # write the M3U header
                    playlist_file = open(filename, 'w')
                    log('Writing to playlist file %s' % filename)
                    playlist_file.write('#EXTM3U\n')

            # write this song to the playlist
            path = os.path.abspath(newmp3)
            log('Adding %s to playlist' % path)
            # HACK: this should have the length of the mp3 in seconds, but i
            # don't think it's worth parsing the id3 tag to get
            playlist_file.write('#EXTINF:1,%s\n' % newmp3[0:-4])
            playlist_file.write('%s\n' % path)
    # end for


def find_inf(mp3):
    """ Searches for a .inf file with the same root filename as the given mp3
    file. If the mp3 file has multiple extensions (e.g. foo.wav.192kb.mp3),
    every subset is searched, outside in. (So it would first look for
    foo.wav.192kb.inf, then foo.wav.inf, then foo.inf.)
    If a corresponding inf file is found, opens it and returns the file.
    Otherwise, returns None.
    """
    parts = mp3.split('.')
 
    for i in range(1, len(parts)):
        infname = string.join(parts[:-i], '.') + '.inf'
        try:
            inf = open(infname, 'r')
            return inf
        except:
            pass
    # end for
    return None


def get_inf_value(infdata, key):
    """ Looks up the given key in the given inf file data and returns its
    value, or None if not found.
    """
    result = re.search(r"%s=\s+(.+)\n" % key, infdata)
    if result:
        return result.group(1).strip("'")
    else:
        return None


def sanitize(str):

    """ Returns a sanitized version of str, with special characters (ie
    characters that can't appear in *nis filenames) replaced by usable
    alternatives given in SPECIAL_CHARS.
    """
    for fromstr, tostr in SPECIAL_CHARS.items():
        str = str.replace(fromstr, tostr)
    return str


def log(message, error=False):
    """ Overly simple logging facility. If anyone knows of a more fully
    featured logging facility that *ships with Python*, please let me know!
    """
    message = 'cdname.py: ' + message

    if error:
        print >> sys.stderr, message
    elif VERBOSE:
        print message
        

def parse_args(args):
    """ Parse command-line args. (See doc comment.)
    """
    global VERBOSE, NUMBERS, ALBUM, PLAYLIST, TEST_RUN, DELIMITER, \
           SPECIAL_CHARS

    try:
        options, args = getopt.getopt(args, 'nvVapulthd:', '--help')
    except getopt.GetoptError:
        type, value, traceback = sys.exc_info()
        log(value.msg, error=True)
        sys.exit(2)

    for option, arg in options:
        if option in ('-h', '--help'):
            usage()
            sys.exit(2)
        elif option == '-V':
            version()
            sys.exit(0)
        elif option == '-v':
            VERBOSE = True
        elif option == '-n':
            NUMBERS = True
        elif option == '-a':
            ALBUM = True
        elif option == '-p':
            PLAYLIST = True
        elif option == '-t':
            TEST_RUN = True
        elif option == '-d':
            DELIMITER = sanitize(arg)
            if DELIMITER.lower() != arg.lower():
                log('Delimiter "%s" has illegal character(s)' % arg,
                    error=True)
                sys.exit(2)
        elif option == '-u':
            SPECIAL_CHARS[' '] = '_'
        elif option == '-l':
            for u, l in zip(string.ascii_uppercase, string.ascii_lowercase):
                SPECIAL_CHARS[u] = l
    # end for option, arg

    if not args:
        usage()
        sys.exit(2)

    return args


def usage():
    print USAGE

def version():
    print VERSION

if __name__ == '__main__':
    args = parse_args(sys.argv[1:])
    main(args)
  
