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
#
#
# I've used the Live HTTP Headers Firefox plugin to help debug and update
# recent versions of this when Simon changes their web site. It's a huge help:
#
# http://livehttpheaders.mozdev.org/

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
#
# when Simon changes their web site, this usually needs to be updated.
VIEWSTATE=%2FwEPDwUJNTc2OTY4MDgyD2QWAmYPZBYCAgMPZBYGAgEPZBYCZg9kFgJmDxYCHgRUZXh0BQEvZAIDDxYCHgxhdXRvY29tcGxldGUFA29mZhYCAgEPZBYGAgEPZBYEZg8WAh4HVmlzaWJsZWcWAmYPDxYCHg9FbmFibGVWaWV3U3RhdGVnZGQCAQ8WAh8CaBYCZg8PFgIfA2dkZAIDD2QWCAIBDxYCHwJnZAIDDxYCHwJnZAIVDw9kFgIfAQUDb2ZmZAIbDw8WBB4YQ2FwdGNoYUltYWdlX0NQQ29udHJvbElEBSRjdGwwMCRDb250ZW50UGxhY2VIb2xkZXIxJHR4dENhcHRjaGEeCEltYWdlVXJsBSVyZWNpcGllbnQvaW1hZ2VzL2NhcHRjaGEvNzg0OTMzNDAuanBnZGQCBQ8PFgIfAmhkFgQCDw8QZGQWAWZkAhEPPCsACwBkAgUPZBYGZg8PFgIeC05hdmlnYXRlVXJsBRMvUHJpdmFjeVBvbGljeS5hc3B4ZGQCAQ8PFgIfBgUKL2ZpbmRhbWFsbGRkAgIPDxYCHwYFGC9hYm91dF9zaW1vbi9jb250YWN0X3NwZ2RkGAEFHl9fQ29udHJvbHNSZXF1aXJlUG9zdEJhY2tLZXlfXxYCBSRjdGwwMCRIZWFkZXIxJFRvcE5hdmlnYXRpb24xJHRvcE1lbnUFI2N0bDAwJENvbnRlbnRQbGFjZUhvbGRlcjEkYnRuU3VibWl0nq6rj%2FdiWrU1I0kobPTuvp1TDV0%3D

COOKIE_FILE=/tmp/simonitor_cookie.jar
CURL_ARGS="--cookie-jar $COOKIE_FILE --cookie $COOKIE_FILE --silent --show-error --insecure"
XLOADIMAGE_PIDS=""

for CC in $*; do
  rm -f $COOKIE_FILE

  # parse credit card number and CCV
  CC_NUM=`echo $CC | grep -o -e '^[0-9]\{16\}'`
  CC_CID=`echo $CC | grep -o -e '[0-9]\{3\}$'`
  if [[ $CC_NUM == "" || $CC_CID == "" ]]; then
    echo "Invalid card number or CCV: $CC"
    continue
  fi


  # GET the form page and the captcha image
  CAPTCHA=`curl $CURL_ARGS https://www.simon.com/giftcard/card_balance.aspx | \
             egrep -o 'recipient/images/captcha/[^.]+\.jpg'`
  CAPTCHAFILE=`mktemp /tmp/simonitor_captcha.XXXXXX` || exit 1
  curl --output $CAPTCHAFILE $CURL_ARGS https://www.simon.com/giftcard/$CAPTCHA
  xloadimage $CAPTCHAFILE > /dev/null &
  XLOADIMAGE_PID="$XLOADIMAGE_PID $!"

  # ask for the captcha.
  #
  # sometimes their captcha generator breaks and only generates AAAAAA. when
  # that happens, i comment out the read command and replace it with this line.
#   CAPTCHA="aaaaaa"
  read -e -p "Enter the captcha string: " CAPTCHA

  # erase the prompt w/ANSI escape codes. [1A moves the cursor up a line, [0K
  # erases the line. see http://www.answers.com/topic/ansi-escape-code .
  echo -ne '\e[1A\e[0K'

  # POST the form data. note that %24 is the url encoding for the $ character.
  if ( ! curl --output /dev/null $CURL_ARGS \
         --data "__VIEWSTATE=$VIEWSTATE&ctl00%24ContentPlaceHolder1%24btnSubmit.x=0&ctl00%24ContentPlaceHolder1%24btnSubmit.y=0&ctl00%24ContentPlaceHolder1%24txtCaptcha=$CAPTCHA&ctl00%24ContentPlaceHolder1%24ccNumberBalance=$CC_NUM&ctl00%24ContentPlaceHolder1%24ccCid=$CC_CID" \
         https://www.simon.com/giftcard/card_balance.aspx ); then
    continue
  fi

  # handle the redirect. iirc, this request sets the auth cookie we need.
  if ( ! curl $CURL_ARGS --output /dev/null \
         https://www.simon.com/giftcard/process.ashx ); then
    continue
  fi

  # GET the balance page and scrape the address, expiration date, and balance
  TMPFILE=`mktemp /tmp/simonitor_out.XXXXXX` || exit 1
  curl $CURL_ARGS -o $TMPFILE https://www.simon.com/giftcard/card_balance.aspx

  BALANCE=`egrep -o 'lblBalance"><b>[^<]+' $TMPFILE | sed 's/lblBalance"><b>//'`
  EXPDATE=`egrep -o 'lblExpDate">[^<]+' $TMPFILE | sed 's/lblExpDate">//'`
  echo "$CC: $BALANCE, expires $EXPDATE"

  NAME=`egrep -o 'lblPersonName">[^<]*' $TMPFILE | sed 's/lblPersonName">//'`
  CITY_STATE_ZIP=`egrep -o 'lblPersonAddress">[^<]*<br>[^<]+' $TMPFILE \
                  | sed 's/lblPersonAddress">//' | sed 's/<br>/ /'`
  if [ "$NAME $CITY_STATE_ZIP" != " " ]; then
    echo "                      $NAME  $CITY_STATE_ZIP"
  fi

  shred -u $TMPFILE $CAPTCHAFILE
done

if [[ $XLOADIMAGE_PID != "" ]]; then
  kill $XLOADIMAGE_PID
fi
