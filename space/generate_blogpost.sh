#!/usr/local/bin/bash
#
# generate_blogpost.sh
# http://snarfed.org/generate_blogpost
# Ryan Barrett <generate_blogpost@ryanb.org>
# This script is public domain.
#
# This script generates .blogpost files for use with blogpost.py from a
# Wordpress MySQL database.
#
# More on blogpost.py: http://srackham.wordpress.com/blogpost-readme/
# Tested with version 0.9.3.

# exit on error
set -e

if [[ $# == "0" || $1 == "--help" ]]; then
  echo "Usage: $0 [MYSQL FLAGS ...]"
  echo "  (run in the directory with your post .txt files)"
  exit 1
fi

UNAME=`uname`
if [[ $UNAME == "Linux" ]]; then
  MD5=md5sum
elif [[ $UNAME == "FreeBSD" ]]; then
  MD5="md5 -q"
else
  exit 1
fi

coproc {
  mysql -n --skip-column-names --raw --silent "$@" -e \
    'SELECT ID, post_title, guid, post_type FROM wp_posts 
       WHERE post_type in ("page", "post");'
  echo DONE.
  sleep 999
}

# set the word delimiter to just tab. mysql --raw --silent delimits with tabs,
# and the title may have spaces in it.
IFS=$'\t'

while read -u ${COPROC[0]} id title name type; do
  if [[ ${id} == 'DONE.' ]]; then
    break
  fi

  name=`basename $name`
  if [ ! -f ${name}.txt ]; then
    echo "WARNING: ${name}.txt not found, skipping .blogpost file."
    continue
  fi

  md5sum=`tail -n +2 ${name}.txt | eval ${MD5} | cut -d' ' -f1`

  cat > ${name}.blogpost <<EOF
ccopy_reg
_reconstructor
p0
(c__main__
Cache
p1
c__builtin__
object
p2
Ntp3
Rp4
(dp5
S'status'
p6
S'published'
p7
sS'title'
p8
S'${title}'
p9
sS'url'
p10
S'http://localhost/${name}'
p11
sS'checksum'
p12
S'${md5sum}'
p13
sS'created_at'
p14
I0
sS'doctype'
p15
S'html'
p16
sS'updated_at'
p17
I0
sS'post_type'
p18
S'${type}'
p19
sS'media'
p20
(dp21
sS'id'
p22
I${id}
sS'categories'
p23
(lp24
sb.
EOF
done
