#!/bin/bash
#
# Dump as much of my data in google services as I can.
# See https://www.google.com/dashboard/ for a full list.

if [ $# -lt 3 ]; then
  echo "Backs up data from Google Contacts, Calendar, Reader, and Tasks."
  echo 
  echo "Usage: `basename $0` backup_google.sh EMAIL PASSWORD DIR [CALENDAR_URLS...]"
  exit 1
fi

BASEDIR=$3
mkdir -p ${BASEDIR}
cd ${BASEDIR}

# use ClientLogin to get a SID cookie. details:
# http://code.google.com/apis/analytics/docs/gdata/1.0/gdataProtocol.html
# http://code.google.com/apis/gdata/faq.html#clientlogin
CLIENTLOGIN=`curl https://www.google.com/accounts/ClientLogin -s \
  -d Email=$1 \
  -d Passwd=$2 \
  -d accountType=GOOGLE \
  -d source=curl-accountFeed-v1`
SID=`echo ${CLIENTLOGIN} | egrep ^SID`

if [ "${SID}" == "" ]; then
  echo ${CLIENTLOGIN}
  exit 1
fi

shift 3
CALENDARS=$@

# short circuit and exit if any command fails
set -e

# include SID cookie for general auth
CURL="curl -s --cookie ${SID}"

# this is one auth alternative, it pulls cookies from firefox, which will work as
# long as i've logged into my google account in firefox recently.
#
# ~/src/misc/export_firefox_cookies.py \
#   ~/.mozilla/firefox/default.???/cookies.sqlite /tmp/cookies.txt
#
# another auth alternative would be just to take everything from android
# phone, since it syncs all of this data from all of these services. see
# etc/android_manual_backup_with_adb.html.


# Calendar. Download the private iCal URLs.
rm -f basic.ics*
wget -q ${CALENDARS}

# I'd rather use this, with the SID cookie, but it needs the CAL and
# S=calendar cookies too, and those have the usual two week expiraton. :/
# ${CURL} http://www.google.com/calendar/exporticalzip > calendars.zip


# Reader. Download the OPML.
${CURL} http://www.google.com/reader/subscriptions/export \
  > reader_subscriptions.opml.xml

# other cookies were:
#
# SID=...
# SSID=...
# LSID=...
# GAUSR=heaven@gmail.com



# Contacts. (Found this endpoint with the firefox Live HTTP Headers plugin.)

${CURL} 'http://www.google.com/contacts/data/export?groupToExport=%5EMine&exportType=ALL&out=VCARD' \
  > contacts.vcf


# Tasks. Annoying, have to reverse engineer and pretend i'm a normal client. :/
#
# POST to https://mail.google.com/tasks/r/d
#
# Needs AT header and GTL cookie.
#
# json input, url-encoded, e.g.:
#
# r={action_list:
#    [{action_type: get_all,
#      action_id: 5,
#      list_id: 04291589652955054844:0:0,
#      get_deleted: false,
#      date_start: 1969-12-31,
#      get_archived: true
#     }],
#    client_version: 1256686
#   }
#
# important notes:
# - the latest_sync_point: 0 to get all tasks
# - the = in r= is *not* url encoded to r%3D
# - the AT: 1 cookie. without it tasks returns 401 Unauthorized.
#
# json output, e.g.:
# {latest_sync_point: 1263000002293000, response_time:1263077227, results:[], tasks:
#   [{id: 04291589652955054844:0:38,
#     last_modified: 1263000002281,
#     name: foo bar
#     notes: ,
#     type: TASK,
#     deleted: false,
#     list_id: [04291589652955054844:0:0],
#     completed: false
#    },
#    {id: 04291589652955054844:0:37,
#     last_modified: 1262929947949,
#     name: baz
#     notes: ,
#     type: TASK,
#     deleted:false,
#     list_id: [04291589652955054844:0:0],
#     completed: false
#    },
#    ...
#

# this is:
# r={"action_list":[{"action_type":"get_all","action_id":"2","get_deleted":true,"date_start":"1969-12-31","get_archived":true}],"client_version":12566865}
CURL='curl -s --cookie SID=...;GTL=...'
DATA=`${CURL} --header "AT: 1" \
  --data 'r=%7B%22action_list%22%3A%5B%7B%22action_type%22%3A%22get_all%22%2C%22action_id%22%3A%222%22%2C%22get_deleted%22%3Atrue%2C%22date_start%22%3A%221969-12-31%22%2C%22get_archived%22%3Atrue%7D%5D%2C%22client_version%22%3A12566865%7D' \
  https://mail.google.com/tasks/r/d`

# extract and url-encode the list ids. they're of the form:
#   04291589652955054844:0:0
#
# need two sed runs to convert both colons.
IDS=`echo ${DATA} | egrep -o '"id":"[0-9]+:[0-9]+:0",' \
                  | egrep -o '[0-9]+:[0-9]+:0' \
                  | sed s/:/%3A/ \
                  | sed s/:/%3A/`

rm -f tasks.json
touch tasks.json

for id in ${IDS}; do
  # this is:
  # r={"action_list":[{"action_type":"get_all","action_id":"10","list_id":"04291589652955054844:4:0","get_deleted":false,"date_start":"1969-12-31","get_archived":true}],"client_version":12566865}
  ${CURL} --header 'AT: 1' --data r=%7B%22action_list%22%3A%5B%7B%22action_type%22%3A%22get_all%22%2C%22action_id%22%3A%2210%22%2C%22list_id%22%3A%22${id}%22%2C%22get_deleted%22%3Afalse%2C%22date_start%22%3A%221969-12-31%22%2C%22get_archived%22%3Atrue%7D%5D%2C%22client_version%22%3A12566865%7D \
    https://mail.google.com/tasks/r/d \
    >> tasks.json
done


# Docs. Complicated HTTP request pattern.

# POST http://docs.google.com/export-status
# archiveId=...&token=...&version=4&tzfp=...&tzo=480&subapp=4&app=2&clientUser=...&hl=en
# ...
