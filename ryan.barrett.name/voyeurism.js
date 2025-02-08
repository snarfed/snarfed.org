/*
 * http://ryan.barrett.name/
 * Ryan Barrett <snarfed@ryanb.org>
 *
 * JavaScript/ECMAScript for detecting user browser, IP, etc., and displaying
 * my current song, away message, location (from geobytes), etc. The geobytes
 * javascript is lazy-loaded so that the rest of the page doesn't block on it.
 */

function write_dynamic_data() {
    user_snoop();
    ryan_snoop();
}


function user_snoop() {
    document.getElementById("browser").innerHTML = browser_name();
    document.getElementById("platform").innerHTML = platform_name();
    lazy_load_geobytes();
}


function ryan_snoop() {
    // from away.js
    document.getElementById("away").innerHTML = get_away_msg();
    // from song.js
    document.getElementById("song").innerHTML = get_cur_song();
    // from timestamp.js
    document.getElementById("timestamp").innerHTML = get_timestamp();
}


function browser_name() {
    app = navigator.appName;
    ua = navigator.userAgent;

    if (app != "Netscape")
      return app;
    else if (ua.search("Epiphany") > -1)
      return "Epiphany";
    else if (ua.search("Camino") > -1)
      return "Camino";
    else if (ua.search("Konqueror") > -1)
      return "Konqueror";
    else if (ua.search("Galeon") > -1)
      return "Galeon";
    else if (ua.search("Opera") > -1)
      return "Opera";
    else if (ua.search("AOL") > -1)
      return "AOL";
    else if (ua.search("CS 2000") > -1)
      return "CS 2000";
    else if (ua.search("MSN") > -1)
      return "MSN";
    else if (ua.search("Safari") > -1)
      return "Safari";
    else if (ua.search("Firefox") > -1)
      return "Firefox";
    else if (ua.search("Firebird") > -1)
      return "Firebird";
    else if (ua.search("Phoenix") > -1)
      return "Phoenix";
    else if (ua.search("Netscape") > -1)
      return "Netscape";
    else
      // if the app name is Netscape, but the user agent doesn't contain
      // Netscape or any of the above...it must be Mozilla
      return "Mozilla";
}

function platform_name() {
    os = navigator.platform;

    if (os == "Win32")
      return "Windows";
    else if (os == "MacPPC")
      return "Apple";
    else
      return os;
}

function lazy_load_geobytes() {
//   url = "test_geobytes.js";
  url = "http://map.geoup.com/geoup?template=CityName";

  iframe = window.frames["location_iframe"].document;

  iframe.write("<link type='text/css' rel='stylesheet' href='header.css' /> " +
               "<span style='color: silver'> " +
               "<script type='text/javascript' src='" + url + "'></script> ");
               // the span isn't closed because the script's document.write()
               // call writes *after* all of these html tags.
}
