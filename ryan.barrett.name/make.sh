#!/bin/csh
#

# Script to compile my web pages from their component parts.

unset noclobber

set TOP=part_top.html
set BOTTOM=part_bottom.html


# make component pages
foreach page (about contact voyeurism people projects)
  echo Making $page.
  if ($page == "voyeurism") then
    set file="$page".shtml
  else
    set file=$page.html
  endif
  cat $TOP part_$file $BOTTOM \
    | sed "s/&nbsp;&nbsp;/$page/" \
    | sed 's/<a href="'$file'">//' \
    | sed 's/_off.png" alt="'$page'".*/_on.png" alt="'$page'" \/>/' > $file
end

# make index
# doesn't use $BOTTOM because part_motd.html and part_nomotd.html have their
# own specific endings (without the extra strip of picture)
echo Making index.html.
cat $TOP part_xfn.html part_nomotd.html > index.html
#cat $TOP part_motd.html part_xfn.html $BOTTOM > index.html

# make badbrowser
echo Making badbrowser.html.
cat part_badbrowser.html part_contact.html > badbrowser.html
echo '</body></html>' >> badbrowser.html

echo Done!
