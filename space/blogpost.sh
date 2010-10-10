#!/usr/local/bin/bash
#
# blogpost.sh
# http://snarfed.org/blogpost
# Ryan Barrett <blogpost@ryanb.org>
# This script is public domain.
#
# Finds wordpress posts and pages that have been changed from the versions
# in Wordpress and publishes them with blogpost.py.
#
# Configure for your own WordPress instance via the WORKSPACE, BLOGPOST, and
# POST_RE variables. Also, note that it expects the first line of your post
# files to be the title, and the second line to be blank, a la PyBlosxom.
#
# 0.2:
# - delete posts and pages whose .txt files have been deleted
# - update page name and title in the database and .blogpost file to match the
#   post file's filename and title on the first line.
#
# 0.1:
# - initial release

# exit on error
set -e

WORKSPACE=${HOME}/public_html/src/snarfed/space
BLOGPOST="`dirname $0`/blogpost/blogpost.py --doctype=html --no-media"
POST_RE="^[0-9]{4}-[0-9]{2}-[0-9]{2}"

# delete pages that i've deleted locally.
TXTS=`mktemp /tmp/blogpost_txts_XXXXXX`
BLOGPOSTS=`mktemp /tmp/blogpost_blogposts_XXXXXX`
ls ${WORKSPACE}/*.txt | xargs -I %% -n 1 basename %% .txt | sort > ${TXTS}
ls ${WORKSPACE}/*.blogpost | xargs -I %% -n 1 basename %% .blogpost | sort > ${BLOGPOSTS}
for NAME in `join -v 2 ${TXTS} ${BLOGPOSTS}`; do
  ${BLOGPOST} delete ${WORKSPACE}/${NAME}.blogpost
done

# publish files in order of mtime, most recent to least recent. when we hit one
# that blogpost.py says is unchanged from what's in wordpress, stop.
for FILE in `ls -1 -t ${WORKSPACE}/*.txt`; do
  # set -x

  BASE=`basename ${FILE} .txt`
  BPFILE=${WORKSPACE}/${BASE}.blogpost

  # validate filename for non wordpress permalink/post name characters and
  # dash instead of underscore between post date and name.
  if [[ "$BASE" =~ [A-Z.,:\;!?\&+/\\] || "$BASE" =~ ^${POST_RE}- ]]; then
    echo "Invalid filename: $BASE"
    exit 1
  fi

  # extract and strip the title
  TMPFILE=`mktemp ${FILE}_XXXXXX`
  tail -n +2 ${FILE} > ${TMPFILE}

  # infer metadata
  if [[ "$BASE" =~ ^${POST_RE} ]]; then
    NAME=${BASE:11}
    PAGES=""
  else
    NAME=${BASE}
    PAGES="--pages"
  fi

  TITLE=`head -n 1 ${FILE}`
  if [ ! -f ${BPFILE} ]; then
    # new post or page. separate the blogpost.py commands for new vs existing
    # pages because it's hard (impossible?) to quote expand the --title flag in
    # the former and still have it disappear in the latter.
    OUT="`${BLOGPOST} --title=\"${TITLE}\" ${PAGES} post ${TMPFILE}`"

  else
    # existing post or page
    OUT="`${BLOGPOST} post ${TMPFILE}`"

    # fix the post name and title in mysql and the .blogpost file
    ID=`grep -A2 "^sS'id'" ${BPFILE} | tail -n 1 | cut -c2-`
    ALIASES=`mysql $@ --silent --skip-column-names -e \
      "SELECT COUNT(*) from wp_posts WHERE post_name='${NAME}' AND ID != ${ID} AND post_type != 'revision';"`
    if [[ "$ALIASES" != "0" ]]; then
      echo "Existing page with id ${ID} has same name ${NAME}"
      exit
    fi
    # TODO: check to see if the title is actually different and needs updating.
    # If it is, put something into $OUT so that we don't stop on this one.
    mysql $@ -e "UPDATE wp_posts SET post_name='${NAME}', post_title='${TITLE}' WHERE ID=${ID};"
  
    BLOGPOST_TITLE_LINE=`grep -A2 "^sS'title'" ${BPFILE} | tail -n 1`
    sed -i.bak "s/^${BLOGPOST_TITLE_LINE}\$/S'${TITLE}'/" ${BPFILE}
    BLOGPOST_URL_LINE=`grep -A2 "^sS'url'" ${BPFILE} | tail -n 1`
    BLOGPOST_URL_LINE=${BLOGPOST_URL_LINE//\//\\\/}
    sed -i.bak "s/^${BLOGPOST_URL_LINE}\$/S'http:\/\/localhost\/${BASE}'/" ${BPFILE}
    # S'(http:\/\/.+)[^\/]+'
  fi

  rm ${TMPFILE}
  if [[ $OUT =~ ^blogpost:\ skipping\ unmodified: ]]; then
    exit 0
  fi
  echo "$OUT"

done
