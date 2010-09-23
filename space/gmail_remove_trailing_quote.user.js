// ==UserScript==
// @name           GMail Remove Trailing Quote
// @namespace      google
// @description    Removes "trailing" quoted text at the end of the email you're composing.
// @include        http*://mail.google.com/*
// ==/UserScript==

// TODO: get rid of the global vars and use an object that stores the textarea
// reference.

// Use GM_log() to print to the error console (message section).
// Components.utils.reportError() and
// getService(Components.interfaces.nsIConsoleService.logStringMessage()
// need permissions that they don't have when running in the mail.google.com
// and/or greasemonkey context, and i couldn't get window.dump() (should write
// to stderr or stdout) to work.

var remove_trailing_quotes_textarea;
var remove_trailing_quotes_last_value;

function remove_trailing_quotes() {
  textarea = remove_trailing_quotes_textarea;
  remove_trailing_quotes_last_value = textarea.value;

  /* trim whitespace at beginning and end */
  textarea.value = textarea.value.replace(/^\s*/, '').replace(/\s*$/, '');

  var lines = textarea.value.split('\n');
  var first = null;
  var last = null;

  for (i = 0; i < lines.length; ++i) {
    /* trim whitespace at beginning and end */
    line = lines[i].replace(/^\s*/, '').replace(/\s*$/, '');

    /* preserve whole quotes */
    if (line.match(/^[ >]*On .+, .+ wrote:$/) &&
        (i == 0 || lines[i - 1][0] != '>')) {
      first = last = null;
      /* skip ahead to the next non-whole-quote line */
      do { ++i } while (i < lines.length && lines[i][0] == '>');
    } else if (!line || line[0] == '>') {
      last = i;
      if (first == null)
        first = i;
    } else if (line == '--') {
      break;
    } else {
      first = last = null;
    }
  }

  if (first != null && last != null) {
    lines.splice(first, last - first + 1, '' /* replacement element(s) */);
    textarea.value = lines.join('\n');
  }
}

function remove_trailing_quotes_undo() {
  if (remove_trailing_quotes_last_value) {
    remove_trailing_quotes_textarea.value = remove_trailing_quotes_last_value;
  }
}

function on_load(event) {
  canvas_frame = document.getElementById("canvas_frame");
  if (canvas_frame) {
    canvas_doc = canvas_frame.contentDocument;
    canvas_doc.removeEventListener("DOMNodeInserted", node_inserted, false);
    canvas_doc.addEventListener("DOMNodeInserted", node_inserted, false);
  }
}

window.addEventListener("load", on_load, false);

function node_inserted(event) {
  textarea = canvas_doc.getElementsByClassName('Ak')[0];

  if (textarea) {
    remove_trailing_quotes_textarea = textarea;
    remove_trailing_quotes_last_value = null;

    textarea.removeEventListener("blur", remove_trailing_quotes, false);
    textarea.addEventListener("blur", remove_trailing_quotes, false);

    textarea.removeEventListener("focus", remove_trailing_quotes_undo, false);
    textarea.addEventListener("focus", remove_trailing_quotes_undo, false);
  }
}
