#!/bin/bash
#
# simonitor.sh
# Copyright 2007 Ryan Barrett
# http://snarfed.org/space/simonitor
#
# Fetches the current balance of one or more Simon gift cards, scrapes the
# resulting HTML, and prints the balance to stdout.
#
# Usage: simonitor.sh CARDNUMBER/CCV [CARDNUMBER/CCV ...]

if [ $# == 0 -o "$1" == "-h" -o "$1" == "--help" ]; then
  echo "Fetches the current balance of one or more Simon gift cards."
  echo 
  echo "Usage: `basename $0` CARDNUMBER/CCV [CARDNUMBER/CCV ...]"
  exit
fi

# simon.com runs on IIS, which requires a valid __VIEWSTATE parameter.
# (evidently it's a base64-encoded struct.) it doesn't look like it's actually
# used for anything, but it still needs to be there.
VIEWSTATE=dDwxMjA4MDA1ODI3O3Q8O2w8aTwxPjtpPDM%2BO2k8NT47aTw5Pjs%2BO2w8dDw7bDxpPDA%2BOz47bDx0PHA8bDxUZXh0Oz47bDxTaW1vbiBNYWxscyB8IE1vcmUgQ2hvaWNlcyAtIFRoZSBQcmVwYWlkIFZpc2EgZGViaXQgR2lmdCBDYXJkIGFuZCBWaXNhIEdpZnQgQWNjb3VudCBmcm9tIFNpbW9uLmNvbTs%2BPjs7Pjs%2BPjt0PDtsPGk8MD47PjtsPHQ8O2w8aTwwPjs%2BO2w8dDxwPGw8VGV4dDs%2BO2w8Lzs%2BPjs7Pjs%2BPjs%2BPjt0PDtsPGk8MT47aTwzPjtpPDU%2BOz47bDx0PDtsPGk8MD47aTwxPjs%2BO2w8dDxwPGw8VmlzaWJsZTs%2BO2w8bzx0Pjs%2BPjtsPGk8MD47PjtsPHQ8cDxwPGw8RW5hYmxlVmlld1N0YXRlOz47bDxvPHQ%2BOz4%2BOz47Oz47Pj47dDxwPGw8VmlzaWJsZTs%2BO2w8bzxmPjs%2BPjtsPGk8MD47PjtsPHQ8cDxwPGw8RW5hYmxlVmlld1N0YXRlOz47bDxvPHQ%2BOz4%2BOz47Oz47Pj47Pj47dDw7bDxpPDE%2BO2k8Mz47PjtsPHQ8cDxsPFZpc2libGU7PjtsPG88dD47Pj47Oz47dDxwPGw8VmlzaWJsZTs%2BO2w8bzx0Pjs%2BPjs7Pjs%2BPjt0PHA8cDxsPFZpc2libGU7PjtsPG88Zj47Pj47PjtsPGk8MTU%2BO2k8MTc%2BOz47bDx0PHQ8OztsPGk8MD47Pj47Oz47dDxAMDw7Ozs7Ozs7Ozs7Pjs7Pjs%2BPjs%2BPjt0PDtsPGk8MD47aTwxPjtpPDI%2BOz47bDx0PHA8cDxsPE5hdmlnYXRlVXJsOz47bDwvUHJpdmFjeVBvbGljeS5hc3B4Oz4%2BOz47Oz47dDxwPHA8bDxOYXZpZ2F0ZVVybDs%2BO2w8L2ZpbmRhbWFsbDs%2BPjs%2BOzs%2BO3Q8cDxwPGw8TmF2aWdhdGVVcmw7PjtsPC9hYm91dF9zaW1vbi9jb250YWN0X3NwZzs%2BPjs%2BOzs%2BOz4%2BOz4%2BO2w8YnRuU3VibWl0Oz4%2BHNmMyjt7bv%2BcJv3cGrLm7%2FpFGAc%3D

COOKIE_FILE=/tmp/simonitor_cookie.jar
CURL_ARGS="--cookie-jar $COOKIE_FILE --cookie $COOKIE_FILE --silent --show-error"

for cc in $*; do
  rm -f $COOKIE_FILE
  CC_ARGS="ccNumberBalance=`echo $cc | sed 's/\//\&ccCid=/'`"

  if ( ! curl $CURL_ARGS --output /dev/null \
         --data "$CC_ARGS&__VIEWSTATE=$VIEWSTATE&btnSubmit.x=0&btnSubmit.y=0" \
         https://www.simon.com/giftcard/card_balance.aspx ); then
    exit
  fi

  if ( ! curl $CURL_ARGS --output /dev/null \
         https://www.simon.com/giftcard/process.ashx ); then
    exit
  fi

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

  shred -u $TMPFILE
done
