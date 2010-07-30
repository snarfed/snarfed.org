#!/bin/bash
#
# pyblosxom2wxr.sh
# http://snarfed.org/pyblosxom2wxr
# Ryan Barrett <pyblosxom2wxr@ryanb.org>
# Version 0.2. This script is public domain.
#
# This script converts PyBlosxom posts and comments into a WXR (WordPress
# eXtensible RSS) XML file that can be imported into a WordPress blog.
#
# Example usage:
#
# $ ./pyblosxom2wxr.sh post1.txt post2.txt > posts.xml
#
# pyblosxom2wxr has been tested with PyBlosxom 1.4 and WordPress 2.9 and 3.0. It
# should work with other versions too, but your mileage may vary.
#
# TODO: comment ordering

# exit on error
set -e

# check args
if [[ $# = "0" || $1 = "--help" ]]; then
  echo 'Usage: pyblosxom2wxr.sh FILES...'
  exit 1
fi

# comment id sequence number
commentid=1

# output header
now=`date --rfc-3339=seconds`
cat << EOF
<?xml version="1.0" encoding="UTF-8"?> 

<!-- generator="pyblosxom2wxr/1.0" created="${now}" -->
<rss version="2.0"
  xmlns:excerpt="http://wordpress.org/export/1.0/excerpt/"
  xmlns:content="http://purl.org/rss/1.0/modules/content/"
  xmlns:wfw="http://wellformedweb.org/CommentAPI/"
  xmlns:dc="http://purl.org/dc/elements/1.1/"
  xmlns:wp="http://wordpress.org/export/1.0/">

<channel>
  <title></title>
  <link></link>
  <description></description>
  <pubDate></pubDate>
  <generator>http://snarfed.org/pyblosxom2wxr?v=1.0</generator>
  <language>en</language>
  <wp:wxr_version>1.0</wp:wxr_version>
  <wp:base_site_url></wp:base_site_url>
  <wp:base_blog_url></wp:base_blog_url>
  <wp:category></wp:category>

EOF

# convert comments
for file in "$@"; do
  fullname=`basename "$file" .txt`
  dir=`dirname "$file"`
  title=`head -n 1 "$file"`

  # TODO: make this easier to customize
  date_re="[0-9]{4}-[0-9]{2}-[0-9]{2}"
  time_re="([0-9]{2})-([0-9]{2})"

  # my pyblosxom posts have a date prefix, e.g. 2010-03-13. my pages don't.
  if [[ "$fullname" =~ ^${date_re} ]]; then
    type=post
    name=${fullname:11}
    datestr="${fullname::10} 00:00:00 -0800"
  else
    type=page
    name=${fullname}

    timestamp_file=${dir}/../timestamps
    datestr=`grep --max-count=1 -E \
               "^${date_re}-${time_re} (.+/)?${fullname}.txt\$" ${timestamp_file} | \
        cut -f1 -d' ' | \
        sed -r "s/-${time_re}\$/ \1:\2 -0500/"`
  
    if [[ ${datestr} == '' ]]; then
      datestr=`stat --format=%y "$file"`
    fi
  fi

  pubDate=`date -uR -d "$datestr"`
  date=`date -d "$datestr" +'%F %T'`
  dateGmt=`date -u -d "$datestr" +'%F %T'`

  # TODO: category support
  category="uncategorized"

  if grep -q ']]>' "$file"; then
    echo "WARNING: $file contains the string ]]>, which makes its CDATA " \
         "section invalid. WordPress handles this ok, but still, heads up." 1>&2
  fi

  cat << EOF
<item>
  <title>${title}</title>
  <pubDate>${pubDate}</pubDate>
  <category domain="category" nicename="$category">$category</category>
  <guid isPermaLink="true">/${fullname}</guid>
  <description></description>
  <content:encoded><![CDATA[`tail -n +3 "$file"`]]></content:encoded>
  <wp:post_date>${date}</wp:post_date>
  <wp:post_date_gmt>${dateGmt}</wp:post_date_gmt>
  <wp:comment_status>open</wp:comment_status>
  <wp:ping_status>open</wp:ping_status>
  <wp:post_name>${name}</wp:post_name>
  <wp:status>publish</wp:status>
  <wp:post_parent>0</wp:post_parent>
  <wp:menu_order>0</wp:menu_order>
  <wp:post_type>${type}</wp:post_type>
  <wp:post_password></wp:post_password>
  <wp:is_sticky>0</wp:is_sticky>
EOF

  # other possible elements:
#  <link>/${fullname}</link>
#  <wp:post_id></wp:post_id>
#  <excerpt:encoded></excerpt:encoded>
#  <dc:creator>${creator}</dc:creator>


  for cmtfile in ${dir}/"$fullname"-{all,[0-9]*}.cmt; do
    if [[ -e "$cmtfile" ]]; then
      set +e  # because the perl script below uses a non-zero exit code
      tail -q -n +2 "$cmtfile" | \
        sed -r '
          s/^<item>$/<wp:comment>\n<wp:comment_id>X<\/wp:comment_id>/;
          s/^<\/item>$/<wp:comment_approved>1<\/wp:comment_approved>\n<\/wp:comment>/;
          s/<(\/)?author>/<\1wp:comment_author>/g;
          s/<(\/)?link>/<\1wp:comment_author_url>/g;
          s/<(\/)?ipaddress>/<\1wp:comment_author_IP>/g;
          s/<(\/)?description>/<\1wp:comment_content>/g;
          s/^<(ajax|cmt_date|email|openid_url|parent|post|secretToken|source|title|w3cdate)>.+$//;
          s/^<\/?items>$//;
          /^$/d' | \
        perl -pe 'use HTML::Entities; decode_entities($_)' | \
        perl -pe 'use POSIX qw(strftime);
                  s/^<pubDate>(.+)<\/pubDate>$/"<wp:comment_date>" . (strftime "%Y-%m-%d %H:%M:%S", localtime($1)) . "<\/wp:comment_date>"/e;' | \
        perl -e '
          my $id = '${commentid}';
          while (<STDIN>) {
             s/^(<wp:comment_id>)X(<\/wp:comment_id>)$/$1 . $id++ . $2/e;
             print $_;
          }
          exit $id - '${commentid}';'
      # TODO: this is a hack since exit codes are only 8 bits unsigned.
      # this will break on posts with >255 comments.
      let commentid+=$?
      set -e
    fi
  done

  cat << EOF
</item>

EOF
done

# output footer
cat << EOF
</channel>
</rss>
EOF
