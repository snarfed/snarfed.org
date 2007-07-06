#!/bin/bash
#
# simonitor.sh
# Copyright 2007 Ryan Barrett
# http://snarfed.org/space/simonitor
#
# Fetches the current balance of one or more Simon gift cards, scrapes the
# resulting HTML, and prints the balance to stdout. Simon uses a CAPTCHA,
# so the user is shown the CAPTCHA images and asked to enter its text.
#
# Usage: simonitor.sh CARDNUMBER/CCV [CARDNUMBER/CCV ...]

if [ $# == 0 -o "$1" == "-h" -o "$1" == "--help" ]; then
  echo "Fetches the current balance of one or more Simon gift cards."
  echo 
  echo "Usage: `basename $0` CARDNUMBER/CCV [CARDNUMBER/CCV ...]"
  exit
fi

# simon.com runs on IIS, which requires a valid __VIEWSTATE parameter.
# evidently it's a base64-encoded struct. it doesn't look like it's actually
# used for anything, but it still needs to be there. note that %2B is the url
# encoding for +.
VIEWSTATE=dDwtMTQ3MDgxMDk2Mjt0PDtsPGk8MT47aTwzPjtpPDU%2BO2k8OT47PjtsPHQ8O2w8aTwwPjs%2BO2w8dDxwPGw8VGV4dDs%2BO2w8U2ltb24gTWFsbHMgfCBNb3JlIENob2ljZXMgLSBUaGUgUHJlcGFpZCBWaXNhIGRlYml0IEdpZnQgQ2FyZCBhbmQgVmlzYSBHaWZ0IEFjY291bnQgZnJvbSBTaW1vbi5jb207Pj47Oz47Pj47dDw7bDxpPDA%2BOz47bDx0PDtsPGk8MD47PjtsPHQ8cDxsPFRleHQ7PjtsPC87Pj47Oz47Pj47Pj47dDw7bDxpPDE%2BO2k8Mz47aTw1Pjs%2BO2w8dDw7bDxpPDA%2BO2k8MT47PjtsPHQ8cDxsPFZpc2libGU7PjtsPG88dD47Pj47bDxpPDA%2BOz47bDx0PHA8cDxsPFNlbGVjdGVkTm9kZUlkO0VuYWJsZVZpZXdTdGF0ZTs%2BO2w8cDE5O288dD47Pj47Pjs7Pjs%2BPjt0PHA8bDxWaXNpYmxlOz47bDxvPGY%2BOz4%2BO2w8aTwwPjs%2BO2w8dDxwPHA8bDxTZWxlY3RlZE5vZGVJZDtFbmFibGVWaWV3U3RhdGU7PjtsPHBFO288dD47Pj47Pjs7Pjs%2BPjs%2BPjt0PDtsPGk8MT47aTwzPjtpPDIxPjtpPDI3Pjs%2BO2w8dDxwPGw8VmlzaWJsZTs%2BO2w8bzx0Pjs%2BPjs7Pjt0PHA8bDxWaXNpYmxlOz47bDxvPHQ%2BOz4%2BOzs%2BO3Q8cDw7cDxsPGF1dG9jb21wbGV0ZTs%2BO2w8b2ZmOz4%2BPjs7Pjt0PHA8cDxsPENhcHRjaGFJbWFnZV9DUENvbnRyb2xJRDtJbWFnZVVybDs%2BO2w8dHh0Q2FwdGNoYTtyZWNpcGllbnQvaW1hZ2VzL2NhcHRjaGEvNDExNS5qcGc7Pj47Pjs7Pjs%2BPjt0PHA8cDxsPFZpc2libGU7PjtsPG88Zj47Pj47PjtsPGk8MTU%2BO2k8MTc%2BOz47bDx0PHQ8OztsPGk8MD47Pj47Oz47dDxAMDw7Ozs7Ozs7Ozs7Pjs7Pjs%2BPjs%2BPjt0PDtsPGk8MD47aTwxPjtpPDI%2BOz47bDx0PHA8cDxsPE5hdmlnYXRlVXJsOz47bDwvUHJpdmFjeVBvbGljeS5hc3B4Oz4%2BOz47Oz47dDxwPHA8bDxOYXZpZ2F0ZVVybDs%2BO2w8L2ZpbmRhbWFsbDs%2BPjs%2BOzs%2BO3Q8cDxwPGw8TmF2aWdhdGVVcmw7PjtsPC9hYm91dF9zaW1vbi9jb250YWN0X3NwZzs%2BPjs%2BOzs%2BOz4%2BOz4%2BO2w8SGVhZGVyMTpUb3BOYXZpZ2F0aW9uMTp0b3BNZW51O2J0blN1Ym1pdDs%2BPvMIfy36JAYwb0tY8M0PgAWHOg1W

COOKIE_FILE=/tmp/simonitor_cookie.jar
CURL_ARGS="--cookie-jar $COOKIE_FILE --cookie $COOKIE_FILE --silent --show-error --insecure"

for cc in $*; do
  rm -f $COOKIE_FILE
  CC_ARGS="ccNumberBalance=`echo $cc | sed 's/\//\&ccCid=/'`"

  # GET the form page and the captcha image
  CAPTCHA=`curl $CURL_ARGS https://www.simon.com/giftcard/card_balance.aspx | \
             egrep -o 'recipient/images/captcha/[^.]+\.jpg'`
  CAPTCHAFILE=`mktemp /tmp/simonitor_captcha.XXXXXX` || exit 1
  curl --output $CAPTCHAFILE $CURL_ARGS https://www.simon.com/giftcard/$CAPTCHA
  xloadimage $CAPTCHAFILE &> /dev/null || exit 1 &

  # ask for the captcha
  read -e -p "Enter the captcha string: " captcha

  # erase the prompt w/ANSI escape codes. [1A moves the cursor up a line, [0K
  # erases the line. see http://www.answers.com/topic/ansi-escape-code .
  echo -ne '\e[1A\e[0K'

  # POST the form data
  if ( ! curl $CURL_ARGS --output /dev/null \
         --data "$CC_ARGS&__VIEWSTATE=$VIEWSTATE&btnSubmit.x=0&btnSubmit.y=0&txtCaptcha=$captcha" \
         https://www.simon.com/giftcard/card_balance.aspx ); then
    exit
  fi

  # handle the redirect. iirc, this request sets the auth cookie we need.
  if ( ! curl $CURL_ARGS --output /dev/null \
         https://www.simon.com/giftcard/process.ashx ); then
    exit
  fi

  # GET the balance page and scrape the address, expiration date, and balance
  TMPFILE=`mktemp /tmp/simonitor_out.XXXXXX` || exit 1
  curl $CURL_ARGS -o $TMPFILE https://www.simon.com/giftcard/card_balance.aspx

  BALANCE=`egrep -o 'lblBalance"><b>[^<]+' $TMPFILE | sed 's/lblBalance"><b>//'`
  EXPDATE=`egrep -o 'lblExpDate">[^<]+' $TMPFILE | sed 's/lblExpDate">//'`
  echo "$cc: $BALANCE, expires $EXPDATE"

  CITY_STATE_ZIP=`egrep -o 'lblPersonAddress">[^<]*<br>[^<]+' $TMPFILE \
                  | sed 's/lblPersonAddress">[^<]*<br>//'`
  if [ "$CITY_STATE_ZIP" != "" ]; then
    echo "                      $CITY_STATE_ZIP"
  fi

#   shred -u $TMPFILE
done
