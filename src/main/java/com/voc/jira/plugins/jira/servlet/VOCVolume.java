package com.voc.jira.plugins.jira.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.webresource.JiraWebResourceManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.voc.jira.plugins.jira.components.ConfigurationManager;
import com.google.common.collect.Maps;

public class VOCVolume extends HttpServlet {
	private VOCVolumeBuilder vocVolumeBuilder;
	private static final long serialVersionUID = -1468563646927779149L;
	private static final Logger log = LoggerFactory.getLogger(VOCVolume.class);
    private SearchService searchService;
    private UserManager userManager;
    private TemplateRenderer renderer;
    private User user;
    private Query query;
    private Integer numIssuesLimit = 10;
    private Integer numResultsLimit = 10;
    private int numIssuesParsed = 0;
    private int numRows = 10;
    private boolean noTotal = true;
    private boolean sortTotal = false;
    private boolean sortVOCWeightedVolume = false;
    private boolean escapingOnly = false;
    private List<String> keys = new ArrayList<String>();
    private List<Map<String, String>> rowMaps = new ArrayList<Map<String, String>>();
    private com.atlassian.jira.user.util.UserManager jiraUserManager;
    private final I18nResolver i18n;
    private JiraWebResourceManager webResourceManager;

    /**
     * Servlet constructor that is auto-wired by Spring to include the following services registered
     * in the params.
     * 
     * @param issueService
     * @param projectService
     * @param customFieldManager
     * @param issueLinkManager
     * @param configurationManager
     * @param issueTypeManager
     * @param searchService
     * @param userManager
     * @param jiraAuthenticationContext
     * @param jiraUserManager
     * @param templateRenderer
     * @param applicationProperties
     * @param i18n
     */
    public VOCVolume( 
    		IssueService issueService, 
    		ProjectService projectService,
    		CustomFieldManager customFieldManager,
			IssueLinkManager issueLinkManager,
			ConfigurationManager configurationManager,
			IssueTypeManager issueTypeManager,
    		SearchService searchService,
            UserManager userManager,
            JiraAuthenticationContext jiraAuthenticationContext,
            com.atlassian.jira.user.util.UserManager jiraUserManager,
            TemplateRenderer templateRenderer,
            ApplicationProperties applicationProperties,
            I18nResolver i18n,
            JiraWebResourceManager webResourceManager) {
        this.searchService = searchService;
        this.userManager = userManager;
        this.renderer = templateRenderer;
        this.jiraUserManager = jiraUserManager;
        this.i18n = i18n;
        this.webResourceManager = webResourceManager;
        ApplicationUser applicationUser = jiraAuthenticationContext.getUser();
        this.user = applicationUser.getDirectoryUser();
        this.query = JqlQueryBuilder.newBuilder().buildQuery();
        this.vocVolumeBuilder = new VOCVolumeBuilder(
        		customFieldManager, issueLinkManager, configurationManager, 
        		issueTypeManager, issueService, jiraAuthenticationContext, user, 
        		searchService, applicationProperties,
        		i18n);
    }

    /**
     * GET method for servlet handles both showing a list of issues, the new issue page, and the edit issue page
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
    						throws ServletException, IOException {
        try {
        	webResourceManager.requireResource("com.voc.jira.plugins.VOC-Utilities:voc-volume-styles");
        	//TODO: Create development test TrustManager for SSL 
        	//    http://javaskeleton.blogspot.com/2010/07/avoiding-peer-not-authenticated-with.html
        	setNumIssuesLimit(req);
        	setNumResultsLimit(req);
        	setQuery(req);
        	setNoTotal(req);
        	setSortTotal(req);
        	setSortVocVolume(req);
        	setEscapingOnly(req);
	        Map<String, Object> context = Maps.newHashMap();
	        List<String> issueRows = getIssuesRows(req);
	        //TODO: sort issueRows by tracked number and weekly average
	        context.put("issueRows", issueRows);
	        context.put("listHeading", 
	        		vocVolumeBuilder.getListHeading(i18n.getText("voc-volume-servlet.subhead")));
	        context.put("listFooter", vocVolumeBuilder.getListFooter(
	        		i18n.getText("voc-volume-servlet.link.label"), 
	        		getQuery().getQueryString(), 
	        		String.valueOf(getNumIssuesLimit()),
	        		String.valueOf(getNumResultsLimit()),
	        		String.valueOf(getNumRows()),
	        		String.valueOf(getNumIssuesParsed()),
	        		String.valueOf(getNoTotal()),
	        		String.valueOf(getSortTotal()),
	        		String.valueOf(getSortVocVolume()),
	        		getKeys()));
	        resp.setContentType("text/html;charset=utf-8");
	        // Pass in the list of issues as the context
	        renderer.render(i18n.getText("voc-volume-servlet.vm.list"), 
	        		context, resp.getWriter());
        } catch (NullPointerException e) {
        	log.error("NullPointerException: ",e);
    		e.printStackTrace();
    	} catch (UnsupportedEncodingException e) {
    		log.error("UnsupportedEncodingException: ",e);
			e.printStackTrace();
		} catch (UnsupportedOperationException e) {
			log.error("UnsupportedOperationException: ",e);
			e.printStackTrace();
		} catch (SSLPeerUnverifiedException e) {
			log.error("SSLPeerUnverifiedException: ", e);
		}
    }
    
    /**
     * Returns a list of issues used to populate the list.vm template in the GET request
     *
     * @param req
     * @return
     * @throws UnsupportedEncodingException 
     */
    private List<String> getIssuesRows(HttpServletRequest req) 
    		throws UnsupportedEncodingException, NullArgumentException {
        // User is required to carry out a search
        User user = getCurrentUser(req);
        List<String> rows = new ArrayList<String>();
        //Map<String, String> rowMap = Maps.newHashMap();
        
        // A page filter is used to provide pagination. Let's use an unlimited filter to
        // to bypass pagination.
		PagerFilter<?> pagerFilter = PagerFilter.getUnlimitedFilter();
        com.atlassian.jira.issue.search.SearchResults searchResults = null;
        try {
            searchResults = searchService.search(user, getQuery(), pagerFilter);
        } catch (SearchException e) {
            e.printStackTrace();
        }
        
        Map<String, String> volValHtmlMap = new HashMap<String, String>();
        int count = 0;
        int rownum = 1;
        clearKeys();
        clearRowMaps();
        for(Issue issue : searchResults.getIssues()){
        	String rowHtml = "";
        	String rowVolValTotal = "0";
        	boolean boolEscaping = false;
        	
        	try{
        		count++;
        		volValHtmlMap = vocVolumeBuilder.getIssueRowHtml(
        				user, issue.getKey(), Integer.toString(count), Integer.toString(rownum));
        		if (volValHtmlMap.get("total") != null) {
        			rowVolValTotal = volValHtmlMap.get("total");
        		}
        		if (volValHtmlMap.get("isEscaping") != null) {
        			boolEscaping = Boolean.valueOf(volValHtmlMap.get("isEscaping"));
        		}
        		if(((getNoTotal() && Integer.valueOf(rowVolValTotal) > 0) 
        				|| !getNoTotal()) && checkEscapingOnly(boolEscaping)) {
        			if (rownum <= getNumResultsLimit()) { 
        				rownum++;
	        			rowHtml = (String) volValHtmlMap.get("html");
	        			setKey(volValHtmlMap.get("key"));
	        			setRowMapsRow(volValHtmlMap);
        			}
        		}
        	} catch (NullPointerException e) {
        		log.error("NullPointerException: ",e);
        		e.printStackTrace();
        		rowHtml = "<div>No results for " + issue.getKey() + "</div>";
        	} catch (UnsupportedEncodingException e) {
        		log.error("UnsupportedEncodingException: ",e);
				e.printStackTrace();
				rowHtml = "<div>No results for " + issue.getKey() + "</div>";
			} catch (URISyntaxException e) {
				log.error("URISuntaxException: ",e);
				e.printStackTrace();
				rowHtml = "<div>No results for " + issue.getKey() + "</div>";
			}
        	if(rowHtml.length() > 0)
        		rows.add(rowHtml);
    		if (count == getNumIssuesLimit()) { break; }
        }
        setNumRows(rownum);
        setNumIssuesParsed(count);
        
        if(req.getParameter("sortTotal") != null && req.getParameter("sortTotal").contains("true")){
        	rows = getSortedIssueRows("total","html");
        }
        
        if(req.getParameter("sortVOCWeightedVolume") != null && req.getParameter("sortVOCWeightedVolume").contains("true")){
        	rows = getSortedFloatIssueRows("VocVolumeWeightedWeeklyAverage","html");
        }
        
        
        
        return rows;
    }

    /**
     * Helper method for getting the current user
     *
     * @param req
     * @return
     */
    private User getCurrentUser(HttpServletRequest req) {
        return jiraUserManager.getUserByName(userManager.getRemoteUsername(req)).getDirectoryUser();
    }
    
    /**
     * Build query from both the plugin global and gadget settings
     * 
     * @param req
     * @return
     */
    private void setQuery(HttpServletRequest req) {
    	Query finalQuery = JqlQueryBuilder.newBuilder().buildQuery();
    	String jqlQuery = "";
        String jqlGadget = "";
        String jqlTotal = "";
        
        // Get gadget settings JQL
        if(req.getParameter("jqlGadget") != null){
        	jqlGadget = StringEscapeUtils.unescapeHtml(req.getParameter("jqlGadget"));
        	SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlGadget);
        	if(!parseResult.isValid()) {
        		jqlGadget = "";
        	}
        }
    	
        // Get append gadget setting JQL to plugin global settings JQL
        if(req.getParameter("jqlQuery") != null){
        	jqlQuery = StringEscapeUtils.unescapeHtml(req.getParameter("jqlQuery"));
        	SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlQuery);
        	if (!parseResult.isValid()) {
        		jqlQuery = "";
        	}
        }
        	
        // Assemble combined query fragment strings into a query object
        if(jqlQuery != null && jqlQuery != "" && jqlGadget != null && jqlGadget != "") {
        	jqlTotal = jqlQuery + " AND " + jqlGadget;
        } else if ((jqlQuery == null || jqlQuery == "") && jqlGadget != null && jqlGadget != "") {
        	jqlTotal = jqlGadget;
        } else if (jqlQuery != null && jqlQuery != "" && (jqlGadget == null || jqlGadget == "")) {
        	jqlTotal = jqlQuery;
        } 
    	SearchService.ParseResult parseTotalResult = searchService.parseQuery(user, jqlTotal);
    	if (parseTotalResult.isValid()) { 
    		finalQuery = parseTotalResult.getQuery(); 
		} 

    	this.query = finalQuery;
    }
    
    public Query getQuery() {
    	return this.query;
    }
    
    /**
     * Limit the number of rows to display
     * 
     * @param req
     */
    private void setNumRows(int rowcount) {
    	if(rowcount > 1){
        	this.numRows = (rowcount-1);
        }
    }
    
    private int getNumRows() {
    	return this.numRows;
    }
       
    private void setNumIssuesLimit(HttpServletRequest req) {
    	if(req.getParameter("numIssuesLimit") != null){
        	this.numIssuesLimit = Integer.valueOf(req.getParameter("numIssuesLimit"));
        }
    }
    
    private int getNumIssuesLimit() {
    	return this.numIssuesLimit;
    }
    
    private void setNumIssuesParsed(int parsed) {
    	if(parsed > 0){
        	this.numIssuesParsed = parsed;
        }
    }
    
    private int getNumIssuesParsed() {
    	return this.numIssuesParsed;
    }
    
    private void setNumResultsLimit(HttpServletRequest req) {
    	if(req.getParameter("numResultsLimit") != null){
        	this.numResultsLimit = Integer.valueOf(req.getParameter("numResultsLimit"));
        }
    }
    
    private int getNumResultsLimit() {
    	return this.numResultsLimit;
    }
    
    private void setNoTotal(HttpServletRequest req) {
    	if(req.getParameter("noTotal") != null){
        	this.noTotal = Boolean.valueOf((req.getParameter("noTotal")));
        }
    }
    
    private boolean getNoTotal() {
    	return this.noTotal;
    }
    
    private void setEscapingOnly(HttpServletRequest req) {
    	if(req.getParameter("escapingOnly") != null){
        	this.escapingOnly = Boolean.valueOf((req.getParameter("escapingOnly")));
        }
    }
    
    private boolean getEscapingOnly() {
    	return this.escapingOnly;
    }
    
    private boolean checkEscapingOnly(boolean issueEscaping) {
    	if(getEscapingOnly() && !issueEscaping) {
    		return false;
    	}
    	return true;
    }
    
    private void setKey(String key) {
    	this.keys.add(key);
    }
    
    private List<String> getKeys() {
    	return this.keys;
    }
    
    private void clearKeys() {
    	this.keys.clear();
    }
    
    private void setSortTotal(HttpServletRequest req) {
    	if(req.getParameter("sortTotal") != null){
        	this.sortTotal = Boolean.valueOf((req.getParameter("sortTotal")));
        }
    }
    
    private boolean getSortTotal() {
    	return this.sortTotal;
    }
    
    private void setSortVocVolume(HttpServletRequest req) {
    	if(req.getParameter("sortVOCWeightedVolume") != null){
        	this.sortVOCWeightedVolume = Boolean.valueOf((req.getParameter("sortVOCWeightedVolume")));
        }
    }
    
    private boolean getSortVocVolume() {
    	return this.sortVOCWeightedVolume;
    }
    
    private List<String> getSortedIssueRows(String sortKey, String fetchKey) {
    	List<String> sortedIssueRows = new ArrayList<String>();
    	sortIssueRows(sortKey);
    	for(Map<String,String> row : this.rowMaps){
    		sortedIssueRows.add(row.get(fetchKey));
    	}
    	return sortedIssueRows;
    }
    
    private List<String> getSortedFloatIssueRows(String sortKey, String fetchKey) {
    	List<String> sortedIssueRows = new ArrayList<String>();
    	sortIssueRowsWithFloat(sortKey);
    	for(Map<String,String> row : this.rowMaps){
    		sortedIssueRows.add(row.get(fetchKey));
    	}
    	return sortedIssueRows;
    }
    
    private void setRowMapsRow(Map<String,String> rowMap) {
    	this.rowMaps.add(rowMap);
    }
    
    private void clearRowMaps() {
    	this.rowMaps.clear();
    }
    
    private void sortIssueRows(final String key) {
    	Collections.sort(this.rowMaps, new Comparator<Map<String, String>>() {
    	    @Override
    	    public int compare(final Map<String, String> o1, final Map<String, String> o2) {
    	        if (Integer.valueOf(o1.get(key)) < Integer.valueOf(o2.get(key))){
    	            return +1;
    	        }else if (Integer.valueOf(o1.get(key)) > Integer.valueOf(o2.get(key))){
    	            return -1;
    	        }else{
    	            return 0;
    	        }
    	    }
    	});
    }
    private void sortIssueRowsWithFloat(final String key) {
    	Collections.sort(this.rowMaps, new Comparator<Map<String, String>>() {
    	    @Override
    	    public int compare(final Map<String, String> o1, final Map<String, String> o2) {
    	        if (Float.parseFloat(o1.get(key)) < Float.parseFloat(o2.get(key))){
    	            return +1;
    	        }else if (Float.parseFloat(o1.get(key)) > Float.parseFloat(o2.get(key))){
    	            return -1;
    	        }else{
    	            return 0;
    	        }
    	    }
    	});
    }
}