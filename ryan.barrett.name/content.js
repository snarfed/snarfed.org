/*
 * http://ryan.barrett.name/
 * Ryan Barrett <snarfed@ryanb.org>
 *
 * JavaScript/ECMAScript for the content mouse-overs. Provides functions for
 * each content's specific behavior. (usually just showing hidden text.)
 */

var timeout;

function show(name, contents) {
  clearTimeout(timeout);
  if (contents == '')
    contents = (name == "url") ? "<br />" : "<br /><br />";
  document.getElementById(name).innerHTML = contents;
}

function clear_later(name) {
  timeout = setTimeout("show('" + name + "', '')", 2000);
}
