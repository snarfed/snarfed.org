/*
 * http://ryan.barrett.name/
 * Ryan Barrett <public@ryanb.org>
 *
 * JavaScript/ECMAScript for browser detection. Right now, it only detects
 * Netscape 4.7, since it's the only browser i know of that partially (read:
 * crappily) supports stylesheets. The "ahem" stylesheet class should detect
 * all other unsupported browsers.
 */

if (navigator.appName.toLowerCase().indexOf("netscape") >= 0 &&
    navigator.appVersion.indexOf("4.") == 0) {

    window.location="badbrowser.html";

} else {
    preload_images();
}


function preload_images() {
//    if (!document.images)
//         return;

    // background
    load_image("background.jpg");

    // purple buttons
    load_image("purple_off.png");
    load_image("purple_on.png");

    // maroon buttons
    load_image("maroon_off.png");
    load_image("maroon_on.png");

    // yellow buttons
    load_image("yellow_off.png");
    load_image("yellow_on.png");

    // green buttons
    load_image("green_off.png");
    load_image("green_on.png");

    // blue buttons
    load_image("blue_off.png");
    load_image("blue_on.png");

    // cyan buttons
    load_image("cyan_off.png");
    load_image("cyan_on.png");
}


function load_image(image_url) {
    img = new Image;
    img.src = image_url;
}
