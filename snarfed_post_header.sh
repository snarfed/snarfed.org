#!/bin/bash
#

if [ $# != 3 ]; then
  echo USAGE: $0 IMAGE IMAGE IMAGE
  echo Prepares three images for use as the header of a snarfed.org blog post.
  echo Resizes them to the same height and a total width of 435px.
  exit
fi

# normalize height
HEIGHT1=`identify -format %h "$1" || exit 1`
HEIGHT2=`identify -format %h "$2" || exit 1`
HEIGHT3=`identify -format %h "$3" || exit 1`

NEW_HEIGHT=$HEIGHT1
if (($NEW_HEIGHT > $HEIGHT2)); then
  NEW_HEIGHT=$HEIGHT2
fi
if (($NEW_HEIGHT > $HEIGHT3)); then
  NEW_HEIGHT=$HEIGHT3
fi

convert "$1" -resize x"$NEW_HEIGHT" "$1"
convert "$2" -resize x"$NEW_HEIGHT" "$2"
convert "$3" -resize x"$NEW_HEIGHT" "$3"

# scale width
WIDTH1=`identify -format %w "$1" || exit 1`
WIDTH2=`identify -format %w "$2" || exit 1`
WIDTH3=`identify -format %w "$3" || exit 1`

TOTAL=$(($WIDTH1 + $WIDTH2 + $WIDTH3))

# compensate for bash's lack of floating point
NEW_WIDTH1=$((($WIDTH1 * 435 * 1000 / $TOTAL) / 1000))
NEW_WIDTH2=$((($WIDTH2 * 435 * 1000 / $TOTAL) / 1000))
NEW_WIDTH3=$((($WIDTH3 * 435 * 1000 / $TOTAL) / 1000))

convert "$1" -resize "$NEW_WIDTH1"x "$1"
convert "$2" -resize "$NEW_WIDTH2"x "$2"
convert "$3" -resize "$NEW_WIDTH3"x "$3"

echo "[!![$1](/space/$1)]()"
echo "[!![$2](/space/$2)]()"
echo "[!![$3](/space/$3)]()"
