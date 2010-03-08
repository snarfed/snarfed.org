#!/bin/bash
#
# writes song (first argument) to snarfed.org:~/www/song.js, and appends it
# (with a timestamp) to snarfed.org:~/www/song.log. also updates the timestamp.

HOST=snarfed.org
JSFILE=~/www/song.js
LOGFILE=~/www/song.log

# change "s to 's
ESCAPED=`echo $* | tr '"' "'"`

# change spaces to pluses for the google search query
GOOGLE="http://google.com/search?q="`echo $ESCAPED | tr ' ' '+'`

# write to song.js and song.log
CONTENT="function get_cur_song() { return '<a href=$GOOGLE>$ESCAPED</a>'; }"
ssh $HOST "unset noclobber; echo \"$CONTENT\" > $JSFILE; echo `date +'%F %H:%M:%S'` '$ESCAPED' >> $LOGFILE"

ssh $HOST ~/www/update_timestamp.sh
