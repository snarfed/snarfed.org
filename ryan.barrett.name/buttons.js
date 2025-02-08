/*
 * http://ryan.barrett.name/
 * Ryan Barrett <snarfed@ryanb.org>
 *
 * JavaScript/ECMAScript for the button mouse-overs. Fills them in, makes
 * them bigger, and shows the name of the page that the button links to.
 */

var curhover = null;   // the button that the mouse is currently over
var selbutton = null;  // the button that is currently selected

function hover(name) {
  // NOTE: this is one possible problem area for old browsers
  // (i.e. Netscape 4.7)
  curhover = document.getElementById(name);
  if (curhover == selbutton)    // if this button is selected, do nothing
    return;

  // enlarge button and show caption
  curhover.src = name + "_on.png";
  document.getElementById("buttonlabel").innerHTML = curhover.alt;
}


function unhover() {
  if (curhover == selbutton)    // if this button is selected, do nothing
    return;

  // revert to smaller button and remove label
  curhover.src = curhover.id + "_off.png";
  document.getElementById("buttonlabel").innerHTML = "";
}
