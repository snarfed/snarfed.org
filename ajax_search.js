/* Site search with the Google AJAX Search API.
 *
 * http://snarfed.org/space/site+search+with+the+Google+AJAX+Search+API
 * Copyright 2006-2010 Ryan Barrett <pyblosxom@ryanb.org>.
 * Licensed under GPL v2.
 */

var results_div_id = 'page-content';
var results_div_class = 'ajax_search_results';
var searching_message = '<span class="ajax-message">Searching...</span>';

function site_search(domain, query) {
  // must not be empty
  if (!query.match('\\S')) {
    alert('Please enter a query.');
    return;
  }
  this.query = query;

  // save the current page
  if (document.getElementById('old' + results_div_id))
    site_search_back();

  old_page = document.getElementById(results_div_id);
  old_page.id = 'old' + results_div_id
  old_page.style.display = 'none';

  // tell the user we're searching
  this.page = document.createElement('div');
  this.page.id = results_div_id;
  this.page.setAttribute('class', results_div_class);
  this.page.innerHTML += searching_message;
  old_page.parentNode.insertBefore(this.page, old_page);

  // set up the search
  this.gwebsearch = new GwebSearch();
  this.gwebsearch.setResultSetSize(GSearch.LARGE_RESULTSET);
  this.gwebsearch.setUserDefinedLabel('');
  this.gwebsearch.setLinkTarget(GSearch.LINK_TARGET_SELF);
  this.gwebsearch.setSiteRestriction(domain);

  this.gwebsearch.clearResults();
  this.gwebsearch.setSearchCompleteCallback(this, site_search.prototype.done);

  // set up the page
  this.page.innerHTML =
    '<div class="gsc-back"> [<a href="#" onclick="site_search_back(); return false;">' +
    '<< back to ' + document.title + '</a>] </div>' +
    '<div class="gsc-title">Results for <b>' + this.query + '</b>:</div>';
  this.branding = GSearch.getBranding();
  this.page.appendChild(this.branding);

  // go!
  this.gwebsearch.execute(query);
}

site_search.prototype.done = function() {
  results = this.gwebsearch.results;
  for (i = 0; i < results.length; i++) {
    this.page.insertBefore(results[i].html.cloneNode(true), this.branding);
  }

  cursor = this.gwebsearch.cursor;
  if (!cursor && results.length == 0) {
    this.page.innerHTML += '<p>No results.</p>';
  } else if (cursor.currentPageIndex < cursor.pages.length - 1) {
    // this is recursive. GWebSearch will re-call its callback, ie this
    // function, when it gets the next page of result.
    this.gwebsearch.gotoPage(cursor.currentPageIndex + 1);
  } else {
//     this.page.innerHTML += '<a href="' + cursor.moreResultsUrl +
//                            '">More results...</a>';
  }
}

function site_search_back() {
  results = document.getElementById(results_div_id);

  old_page = document.getElementById('old' + results_div_id);
  old_page.id = results_div_id;
  old_page.style.display = '';

  results.parentNode.removeChild(results);
}
