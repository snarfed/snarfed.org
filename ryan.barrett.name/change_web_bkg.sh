#!/usr/local/bin/bash

DIR=~/www/backgrounds
BACKGROUND=~/www/background.jpg

case `expr $RANDOM % 10` in
  0) NEW=arctic.jpg;;
  1) NEW=bloodcount.jpg;;
  2) NEW=earth.jpg;;
  3) NEW=id_closer.jpg;;
  4) NEW=iraq.jpg;;
  5) NEW=junkle.jpg;;
  6) NEW=ojblue.jpg;;
  7) NEW=subway.jpg;;
  8) NEW=fear.jpg;;
  9) NEW=tree.jpg;;
esac

cd $DIR
cp -f $NEW $BACKGROUND

