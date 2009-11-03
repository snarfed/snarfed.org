#!/bin/bash
#
# simonitor.sh
# Copyright 2007-2009 Ryan Barrett
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
# used for anything, but it still needs to be there. %2B is the url encoding
# for +, and %2F for /. see http://www.w3schools.com/tags/ref_urlencode.asp.
#
# when Simon changes their web site, this usually needs to be updated.
VIEWSTATE=%2FwEPDwULLTIxMjAxNTM0MjcPZBYCZg9kFgJmD2QWBAIBD2QWAgIIDxYCHgdjb250ZW50BQhuby1jYWNoZWQCAw9kFgICAQ9kFgoCAw8PFgIeBk1vZGVJZGZkZAIFD2QWAgIDDw8WAh4HVmlzaWJsZWdkZAIHD2QWAmYPFgIfAmcWAgIBDw8WAh4LTmF2aWdhdGVVcmwFNmh0dHBzOi8vd3d3LnNpbW9uLmNvbS9naWZ0Y2FyZC9jYXJkX2JhbGFuY2UuYXNweD9yc2M9ZmRkAgkPZBYGAgEPZBYCAgEPZBYCAgEPZBYCAgEPFgIfAmdkAgMPZBYCAgEPFgIeC18hSXRlbUNvdW50AgMWBgIBD2QWAgIBDw8WCB8DBQ0vZGVmYXVsdC5hc3B4HgRUZXh0BQRIb21lHghDc3NDbGFzcwUFZmlyc3QeBF8hU0ICAmRkAgIPZBYCAgEPDxYEHwMFFi9naWZ0Y2FyZC9kZWZhdWx0LmFzcHgfBQUeU2ltb24gR2lmdGNhcmRzICYgR2lmdGFjY291bnRzZGQCAw9kFgICAQ8PFggfAwUMamF2YXNjcmlwdDo7HwUFDENhcmQgQmFsYW5jZR8GBQdjdXJyZW50HwcCAmRkAgUPZBYEAgMPFgIfAmdkAgQPFgIfAmgWAgINDxBkZBYBZmQCCw9kFgICAg8PFgIfAmdkZBgCBR5fX0NvbnRyb2xzUmVxdWlyZVBvc3RCYWNrS2V5X18WAQU2Y3RsMDAkY3RsMDAkRnVsbENvbnRlbnQkTWFpbkNvbnRlbnQkY2hlY2tCYWxhbmNlU3VibWl0BS5jdGwwMCRjdGwwMCRGdWxsQ29udGVudCRjdGwwMCRsZWZ0TmF2TXVsdGlWaWV3Dw9kZmQCdHgcubATw8JFZ94KihB%2BHOIhmg%3D%3D

COOKIE_FILE=/tmp/simonitor_cookie.jar
CURL_ARGS="--cookie-jar $COOKIE_FILE --cookie $COOKIE_FILE --silent --show-error --insecure"

for CC in $*; do
  rm -f $COOKIE_FILE

  # parse credit card number and CCV
  CC_NUM=`echo $CC | grep -o -e '^[0-9]\{16\}'`
  CC_CID=`echo $CC | grep -o -e '[0-9]\{3\}$'`
  if [[ $CC_NUM == "" || $CC_CID == "" ]]; then
    echo "Invalid card number or CCV: $CC"
    continue
  fi


  # GET the form page and the recaptcha image
  RECAPTCHA_K=`curl $CURL_ARGS https://www.simon.com/giftcard/card_balance.aspx | \
                 egrep -o "https://api-secure.recaptcha.net/challenge\?k=[^&']+"`
  RECAPTCHA_C=`curl $RECAPTCHA_K | \
                 egrep -o "challenge : '[^']+" | \
                 cut -c 14-`
  CAPTCHAFILE=`mktemp /tmp/simonitor_captcha.XXXXXX` || exit 1
  curl --output $CAPTCHAFILE $CURL_ARGS https://api-secure.recaptcha.net/image?c=${RECAPTCHA_C}
  xloadimage $CAPTCHAFILE > /dev/null &
  XLOADIMAGE_PID="$!"

  # ask for the captcha
  read -e -p "Enter the captcha string: " CAPTCHA
  CAPTCHA=`echo $CAPTCHA | tr ' ' +`

  # erase the prompt w/ANSI escape codes. [1A moves the cursor up a line, [0K
  # erases the line. see http://www.answers.com/topic/ansi-escape-code .
  echo -ne '\e[1A\e[0K'

  # POST to the form. note that %24 is the url encoding for the $ character.
  TMPFILE=`mktemp /tmp/simonitor_out.XXXXXX` || exit 1
  if ( ! curl --output $TMPFILE $CURL_ARGS \
         --data "__VIEWSTATE=${VIEWSTATE}&returnUrl=https%3A%2F%2Fwww.simon.com%3A443%2Fgiftcard%2Fcard_balance.aspx&ctl00%24ctl00%24FullContent%24MainContent%24tbNumber=${CC_NUM}&ctl00%24ctl00%24FullContent%24MainContent%24tbCid=${CC_CID}&recaptcha_challenge_field=${RECAPTCHA_C}&recaptcha_response_field=${CAPTCHA}&ctl00%24ctl00%24FullContent%24MainContent%24checkBalanceSubmit.x=0&ctl00%24ctl00%24FullContent%24MainContent%24checkBalanceSubmit.y=0" \
         https://www.simon.com/giftcard/card_balance.aspx ); then
    continue
  fi

  # scrape the address, expiration date, and balance
  BALANCE=`egrep -o 'lblBalance">[^<]+' $TMPFILE | sed 's/lblBalance">//'`
  EXPDATE=`egrep -o 'lblExpDate">[^<]+' $TMPFILE | sed 's/lblExpDate">//'`
  echo "$CC: $BALANCE, expires $EXPDATE"

  NAME=`egrep -o 'lblPersonName">[^<]*' $TMPFILE | sed 's/lblPersonName">//'`
  CITY_STATE_ZIP=`egrep -o 'lblPersonAddress">[^<]*(<br>[^<]*)*' $TMPFILE \
                  | sed 's/lblPersonAddress">//' \
                  | sed 's/<br>/ /' | sed 's/<br>/ /' | sed 's/<br>/ /'`
  if [ "$NAME $CITY_STATE_ZIP" != " " ]; then
    echo "                      $NAME  $CITY_STATE_ZIP"
  fi

  shred -u $TMPFILE $CAPTCHAFILE
  if [[ $XLOADIMAGE_PID != "" ]]; then
    kill $XLOADIMAGE_PID
  fi
done
