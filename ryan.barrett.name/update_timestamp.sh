#!//usr/local/bin/bash
TIMEFILE=`dirname "$0"`/timestamp.js

# for freebsd, on pair.com
export TZ=America/Los_Angeles
TIME=`/bin/date +"%b %d %l:%M%p"`

# for linux
#TIME=`/bin/date "+%b %-d %-l:%M%P"`

rm -f $TIMEFILE
echo "function get_timestamp() { return '$TIME'; }" > $TIMEFILE
