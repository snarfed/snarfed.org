#!/bin/bash
#
# sync_wordpress.sh
# http://snarfed.org/sync_wordpress
# Ryan Barrett <sync_wordpress@ryanb.org>
#
# This script syncs wordpress, plugins, themes, and settings stored in the
# database, but *not* content (posts, pages, comments), from one WordPress
# database to another.
#
# This script is public domain.

# User-configurable options:
SOURCE_DUMP=${HOME}/src/wordpress/snarfed.sql
SOURCE_DIR=${HOME}/public_html/w/
SOURCE_MYSQL_OPTS='-h localhost -u snarfed snarfed'

DEST_DIR=public_html/w
DEST_HOST=snarfed.org
DEST_MYSQL_OPTS=''


# exit on error
set -e

DIFF=tkdiff

if [ "$DIFF" == "" ]; then
  DIFF='diff --ignore-case --ignore-all-space'
fi

# WARNING: this is duplicated down below. make changes there too! (i really want
# to unify them, but i can't get all the nested levels of quoting to work.)
MYSQLDUMP_WHERE='option_name not like "_transient%" and
           option_name not like "_site_transient%" and
           option_name not like "%_gc_time" and
           option_name not like "supercache_%" and
           option_name not in ("akismet_available_servers",
                               "akismet_connectivity_time",
                               "akismet_spam_count",
                               "ngg_next_update",
                               "openid_associations",
                               "openid_nonces",
                               "recently_activated",
                               "recently_edited",
                               "rewrite_rules",
                               "supercache_stats"
           ) order by option_name'
MYSQLDUMP='mysqldump --add-drop-table=false
  --single-transaction=true
  --skip-dump-date
  --skip-comments'

# order matters here!
MYSQLDUMP_SED1='
  s/^INSERT INTO `wp_options` VALUES /REPLACE INTO `wp_options` VALUES\n/;
  s/\),\(/),\n(/g;
  s/^CREATE TABLE/CREATE TABLE IF NOT EXISTS/;
  s/NOT NULL default/NOT NULL DEFAULT/;
  s/NOT NULL auto_increment/NOT NULL AUTO_INCREMENT/;'
MYSQLDUMP_SED2="
  /^\([0-9]+,[0-9]+,'(siteurl|admin_email|home|dashboard_widget_options)',[^)]+\),\$/d"

# copy files
RSYNC='rsync -r --size-only --delete --links -e ssh --bwlimit=20
  --exclude=/wp-content/cache --exclude=wp-config.php --exclude=TAGS
  --exclude=/wp-content/uploads'
${RSYNC} --dry-run --itemize-changes ${SOURCE_DIR} ${DEST_HOST}:${DEST_DIR}

read -p 'Sync files (y/n)? ' SYNC
if [ "${SYNC}" == 'y' ]; then
  ${RSYNC} -v ${SOURCE_DIR} ${DEST_HOST}:${DEST_DIR}
fi


# copy settings in database
echo 'BEGIN;' > $SOURCE_DUMP
${MYSQLDUMP} ${SOURCE_MYSQL_OPTS} --where="${MYSQLDUMP_WHERE}" \
  wp_options | sed -r "$MYSQLDUMP_SED1" | sed -r "$MYSQLDUMP_SED2" \
  >> $SOURCE_DUMP
echo 'COMMIT;' >> $SOURCE_DUMP


# diff against svn head, then check in
echo Diffing against SVN...
if ! $DIFF $SOURCE_DUMP; then
  read -p 'Check in (y/n)? ' CHECKIN
  if [ "${CHECKIN}" == 'y' ]; then
    svn ci $SOURCE_DUMP
  fi
fi

# diff against dest
DEST_DUMP=`mktemp /tmp/sync_wordpress_remote.XXXXXX` || exit 1
echo 'BEGIN;' > $DEST_DUMP
ssh ${DEST_HOST} ${MYSQLDUMP} ${DEST_MYSQL_OPTS} wp_options \
  "--where='option_name not like \"_transient%\" and \
           option_name not like \"_site_transient%\" and \
           option_name not like \"%_gc_time\" and \
           option_name not like \"supercache_%\" and \
           option_name not in (\"akismet_available_servers\", \
                               \"akismet_connectivity_time\", \
                               \"akismet_spam_count\", \
                               \"ngg_next_update\", \
                               \"openid_associations\", \
                               \"openid_nonces\", \
                               \"recently_activated\", \
                               \"recently_edited\", \
                               \"rewrite_rules\", \
                               \"supercache_stats\" \
           ) order by option_name'" \
  | sed -r "$MYSQLDUMP_SED1" | sed -r "$MYSQLDUMP_SED2" \
  >> $DEST_DUMP
echo 'COMMIT;' >> $DEST_DUMP

echo Diffing against destination...
if ! $DIFF $DEST_DUMP $SOURCE_DUMP; then
  read -p 'Apply (y/n)? ' APPLY
  if [ "${APPLY}" == 'y' ]; then
    ssh ${DEST_HOST} mysql ${DEST_MYSQL_OPTS} < $SOURCE_DUMP
  fi
fi

rm $DEST_DUMP
