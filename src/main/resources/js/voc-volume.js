// https://developers.google.com/gadgets/docs/remote-content

// Create minimessage factory
//var minimsg = new gadgets.MiniMessage();
// Show a small loading message to the user
//var loadMessage = minimsg.createStaticMessage("loading...");

// Get configured user prefs
//var prefs = new gadgets.Prefs();
//var showDate = prefs.getBool("show_date");
//var showSummary = prefs.getBool("show_summ");
//var numRows = prefs.getInt("num_entries");
//var jqlGadget = prefs.getString("text_jql");

// Fetch issues when the gadget loads
//gadgets.util.registerOnLoadHandler(fetchIssues);

/*
function fetchIssues() {
    // Construct request parameters object
    var params = {};
    // Indicate that the response is XML
    params[gadgets.io.RequestParameters.CONTENT_TYPE] =
    	gadgets.io.ContentType.TEXT;
    //params[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.OAUTH;
    //params[gadgets.io.RequestParameters.OAUTH_USE_TOKEN] = "never";
    //params[gadgets.io.RequestParameters.METHOD] = gadgets.io.MethodType.GET;
    
    // Pass in URL param for gadget jql query
    var jqlGadget = prefs.getString("text_jql");
    if(jqlGadget != "") {
	    var appendUrl = "&jqlGadget=" + encodeURIComponent(jqlGadget);
	    url += appendUrl;
	}
    
    // Pass in URL param for gadget number of results to fetch
    var numRows = prefs.getInt("num_entries");
    if(numRows != "") {
	    var appendUrl = "&numRows=" + encodeURIComponent(numRows);
	    url += appendUrl;
	}
    
    // Proxy the request through the container server
    gadgets.io.makeRequest(url, handleResponse, params);
}
*/

/*
function handleResponse(resp) {
  if(resp.rc == 200){
    renderHTML(resp.data);
  } else {
	renderError(resp);
  }

  minimsg.dismissMessage(loadMessage);
  gadgets.window.adjustHeight();
}

function renderHTML(html) {
	document.getElementById('content_div').innerHTML = html;
}

function renderError(resp) {
	var html = "<div style=\"color:red;font-size:16px;\"><b>Error, response code: " + resp.rc + 
		"</b></div><div>" + resp.errors + "</div>";
	document.getElementById('content_div').innerHTML = html;
}
*/
