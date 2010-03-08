//
// Ryan Barrett <countdown_js@ryanb.org>
// shows a countdown timer until the date in count_to.
//

function countdown(count_to) {
    var ms = new Date(count_to) - new Date();

    if (ms < 0) {
      var str = "now"
    } else {
        var date = new Date(ms);
        var days = Math.round(ms / (24 * 60 * 60 * 1000));
        var str = days + ":" + 
                  pad(date.getUTCHours()) + ":" +
                  pad(date.getUTCMinutes()) + ":" +
                  pad(date.getUTCSeconds());
    }

    document.getElementById("counter").innerHTML = str;
    window.setTimeout("countdown(\"" + count_to + "\")", 1000);
}

function pad(x) {
    return (x < 10) ? "0" + x : x;
}
