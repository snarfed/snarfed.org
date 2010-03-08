#!/bin/csh
set LOGFILE=~/www/away.log
set MESSAGE=`tail -n 1 $LOGFILE | cut -d' ' -f3-`
set AWAYFILE=~/www/away.js

# backup old log message, and write new one
mv -f $AWAYFILE /tmp
echo "function get_away_msg() { return '$MESSAGE'; }" > $AWAYFILE

# if new, update timestamp
diff $AWAYFILE /tmp/ > /dev/null
if ($? != 0) then
  ~/www/update_timestamp.sh
endif
