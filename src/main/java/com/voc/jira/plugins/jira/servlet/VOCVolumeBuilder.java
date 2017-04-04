package com.voc.jira.plugins.jira.servlet;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.LinkCollection;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.message.I18nResolver;
import com.voc.jira.plugins.jira.components.ConfigurationManager;
import com.voc.jira.plugins.jira.customfield.SelectSeverityField;

public class VOCVolumeBuilder {
	private static final Logger log = LoggerFactory.getLogger(VOCVolumeBuilder.class);
	private final CustomFieldManager customFieldManager;
	private final ConfigurationManager configurationManager;
	private final IssueService issueService;
	private final ApplicationProperties applicationProperties;
	private ApplicationUser applicationUser;
    private User user;
	private LinkCollection linkCollection;
    private IssueLinkManager issueLinkManager;
    private String baseUrl = "";
    private final AvatarService avatarService = ComponentAccessor.getAvatarService();
    private static final String VOC_VOLUME = "VOC Volume";
    private final I18nResolver i18n;
    private String servletPath = "";
    private SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
	//private SelectSeverityField<?, ?> selectSeverityField;
	
	public VOCVolumeBuilder(
			CustomFieldManager customFieldManager,
			IssueLinkManager issueLinkManager,
			ConfigurationManager configurationManager,
			IssueTypeManager issueTypeManager,
			IssueService issueService,
			JiraAuthenticationContext jiraAuthenticationContext,
			User user,
			SearchService searchService,
			ApplicationProperties applicationProperties,
			I18nResolver i18nResolver){
		this.customFieldManager = customFieldManager;
		this.issueLinkManager = issueLinkManager;
	    this.configurationManager = configurationManager;
	    this.issueService = issueService;
	    this.applicationUser = jiraAuthenticationContext.getUser();
	    this.user = user;
	    this.applicationProperties = applicationProperties;
	    this.baseUrl = applicationProperties.getBaseUrl();
	    this.i18n = i18nResolver;
	    this.servletPath = i18n.getText("voc-volume-servlet.url");
	}
	
	/**
	 * 
	 * @param issue
	 * @return
	 */
	public Map<String, Object> getIssueProps(Issue issue){
		Map<String, Object> issuePropsMap = new HashMap<String, Object>();
		
		String status = issue.getStatusObject().getName();
    	if (status == null) { status = "none"; }
    	issuePropsMap.put("status", status);
    	
    	String cfVOCVolumeBannerValue = getHighestLevelVOCVol(issue, user, VOC_VOLUME);
        if (cfVOCVolumeBannerValue == null || cfVOCVolumeBannerValue == "") {
        	cfVOCVolumeBannerValue = "None";
        }
        issuePropsMap.put(i18n.getText("voc-volume-servlet.banner"), 
        		cfVOCVolumeBannerValue);
		
		return issuePropsMap;
	}
	
	/**
	 * 
	 * @param user
	 * @param issueKey
	 * @return
	 */
	public Issue getIssue(ApplicationUser user, String issueKey){
		IssueService.IssueResult issue = issueService.getIssue(user, issueKey);
		return issue.getIssue();
	}
	
	
	//========== VOC Request & Support Request Data ===============
	
	private String getHighestLevelVOCVol(Issue issue, User user, String cFieldName){
		
		// Get highest level option
	    ArrayList<String> cfValues = new ArrayList<String>();
	    String cfHighestValue = "None";
	    
	    try{
	        linkCollection = issueLinkManager.getLinkCollection(issue, user);
	        Set<IssueLinkType> linkTypes = linkCollection.getLinkTypes();
	    
		    for (Iterator<IssueLinkType> iterator1 = linkTypes.iterator(); iterator1.hasNext();){
	        	IssueLinkType linkType = (IssueLinkType) iterator1.next();
		    
			    // Get the outward linked issues
	            List<Issue> outwardIssues = linkCollection.getOutwardIssues(linkType.getName());
	            if (outwardIssues != null){
	                for (Iterator<Issue> iterator2 = outwardIssues.iterator(); iterator2.hasNext();){
	                    Issue outwardIssue = iterator2.next();
	                    cfValues.add(getIssueCustomFieldValue(outwardIssue, cFieldName));
	                }
	            }
	            // And the inward linked issues
	            List<Issue> inwardIssues = linkCollection.getInwardIssues(linkType.getName());
	            if (inwardIssues != null){
	                for (Iterator<Issue> iterator2 = inwardIssues.iterator(); iterator2.hasNext();){
	                    Issue inwardIssue = iterator2.next();
	                    cfValues.add(getIssueCustomFieldValue(inwardIssue, cFieldName));
	                }
	            }
		    }
	    }catch(NullPointerException e){
			log.error("NullPointerException: ",e);
			e.printStackTrace();
		}
	    
    	for(Iterator<String> iterator = cfValues.iterator(); iterator.hasNext();){
    		String strValue = iterator.next();
		    if(strValue.contains("High")){
		    	cfHighestValue = "High";
		    } else if(strValue.contains("Medium")){
		    	if(cfHighestValue.trim() != "High") {
		    		cfHighestValue = "Medium";
		    	}
		    } else if(strValue.contains("Low")){
		    	if(cfHighestValue.trim() != "High" && cfHighestValue.trim() != "Medium"){
		    		cfHighestValue = "Low";
		    	}
    		}
    	}
	    return cfHighestValue;
	}
	
	private String getIssueCustomFieldValue(Issue issue, String CustomFieldName) {
		String strCFValue = "";
		if(customFieldManager.getCustomFieldObjectByName(CustomFieldName) != null) {
    		CustomField cf = customFieldManager.getCustomFieldObjectByName(CustomFieldName);
    		if (cf.getValue(issue) != null) {
    				strCFValue = cf.getValue(issue).toString();
    		}
		} else {
			log.warn("Missing expected custom field \"" + CustomFieldName + "\" in VOC Volume module.");
		}
        return strCFValue;
	}
	
	//================= HTML ELement Builders =====================

	/**
	 * 
	 * @param issue
	 * @return
	 * @throws UnsupportedEncodingException 
	 * @throws URISyntaxException 
	 */
	public Map<String, String> getIssueRowHtml(User user, String issueKey, String count, String rownum) 
			throws UnsupportedEncodingException, URISyntaxException{
		Map<String, String> volValHtmlMap = new HashMap<String, String>();
		Issue issue = getIssue(applicationUser, issueKey);
		volValHtmlMap = getVolumeValuesHtml(issue);
		String escapingIcon = "";
		if(Boolean.valueOf(volValHtmlMap.get("isEscaping"))) {
			escapingIcon = getEscapingIcon(Boolean.valueOf(volValHtmlMap.get("isBizCritical"))) + 
					"&nbsp;&nbsp;";
		}
		
		String createdDate = getCreatedDate(issue);
			
		Integer totalTracks = Integer.valueOf(volValHtmlMap.get("total"));
		float supportTracksWeeklyAverage = 0;
		if(volValHtmlMap.get("Support Tracks Weekly Avg") != "0" && volValHtmlMap.get("Support Tracks Weekly Avg") != null){
			supportTracksWeeklyAverage = Float.parseFloat(volValHtmlMap.get("Support Tracks Weekly Avg"));
		}				
				
		String strDueDate = getDueDate(issue);
		
		// Compare dates
		DateFormat format = new SimpleDateFormat("yyyy-MM-d", Locale.ENGLISH);
		Date date = null;		
		try {
			date = format.parse(createdDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		float weeksOpened = 0;
		float totalWeeklyAverage = 0;
		if(date != null){
			// Utilize absolute value rather than dealing with negative numbers
			float timeDifference = Math.abs(date.getTime() - new Date().getTime());
			weeksOpened = (timeDifference / (1000*60*60*24*7));
			totalWeeklyAverage = Math.abs(totalTracks / weeksOpened);
		}				
		
		float vocVolumeWeightedWeeklyAverage = (totalWeeklyAverage + (2 * supportTracksWeeklyAverage)) / 3;
		vocVolumeWeightedWeeklyAverage = (float) (Math.round(vocVolumeWeightedWeeklyAverage * 100.0) / 100.0);
		if(strDueDate.length() > 0) { strDueDate = ", Due Date " + strDueDate;}
		
		String strClosedDate = getClosedDate(issue);
		if(strClosedDate.length() > 0) {strClosedDate = ", Closed " + strClosedDate;}
		
		StringBuilder rowHtmlBuilder = new StringBuilder();
		rowHtmlBuilder.append("<div id=\"" + issue.getKey() + "\" class=\"" 
				+ getIssueRowCssClass(issue) + "\" >");
		rowHtmlBuilder.append(
				"<div id=\"" + issue.getKey() + "_row\" class=\"issueline\">" + 
				"<div id=\"rownum_" + rownum + "\" class=\"aui-lozenge aui-lozenge-complete\">" + rownum + "</div>&nbsp;&nbsp;" + 
				getIssueProjectIconLink(issue) + "&nbsp;" + 
				getIssueTypeIconUrl(issue) + "&nbsp;" + getIssueLink(issue) + " <b><em>" + 
				getIssueSummaryLink(issue) + "</em></b>&nbsp;" + getCurrentIssuePriority(issue) + "&nbsp;" + 
        		getCurrentIssueSeverity(issue) + "&nbsp;&nbsp;" + escapingIcon + 
        		getCurrentIssueStatus(issue) + "&nbsp;&nbsp;" +
                getAssigneeAvatar(issue) + "&nbsp;" + getAssigneeProfileLink(issue) + 
                " Created " + createdDate + strDueDate + strClosedDate + "<br/>" + 
        		"</div>");
		rowHtmlBuilder.append(
				"<div class=\"issueline\">" + 
				volValHtmlMap.get("html") + 
				"&nbsp;&nbsp;VOC Weighted Volume: " + 
				"<span class=\"aui-lozenge aui-lozenge-subtle aui-lozenge\">" +
				vocVolumeWeightedWeeklyAverage  + "</span>" + "&nbsp;" + 
				getBanner(getHighestLevelVOCVol(issue,user,VOC_VOLUME)) + 
        		"</div>");
		rowHtmlBuilder.append("</div>");
		volValHtmlMap.put("VocVolumeWeightedWeeklyAverage", String.valueOf(vocVolumeWeightedWeeklyAverage));
		
		volValHtmlMap.put("key", issue.getKey());
		volValHtmlMap.put("html", rowHtmlBuilder.toString());
		return volValHtmlMap;
	}
	
	public Map<String, String> getVolumeValuesHtml(Issue issue) {			
		Map<String, String> volValHtmlMap = new HashMap<String, String>();
		boolean isEscaping = false;
		String strTotalOtherFieldLabel = "";
		
		// totals map of k,v for all custom fields (initialize to 0 for each)
		Map<String, Integer> volFieldsMap = configurationManager.getVOCCustomFields();
		
		// for each linked issue in ("Support Request","VOC Volume")
		//   iterate through map of custom fields
		//     update custom field k,v pairs in custom fields map for totals of each field found
		//     increment grand total value in the totals Int
		try{
	        linkCollection = issueLinkManager.getLinkCollection(issue, user);
	        Set<IssueLinkType> linkTypes = linkCollection.getLinkTypes();
	    
		    for (Iterator<IssueLinkType> iterator1 = linkTypes.iterator(); iterator1.hasNext();){
	        	IssueLinkType linkType = (IssueLinkType) iterator1.next();
		    
			    // Get the outward linked issues
	            List<Issue> outwardIssues = linkCollection.getOutwardIssues(linkType.getName());
	            if (outwardIssues != null){
	                for (Iterator<Issue> iterator2 = outwardIssues.iterator(); iterator2.hasNext();){
	                    Issue outwardIssue = iterator2.next();
	                    if(checkIsEscaping(issue.getCreated(),outwardIssue.getCreated())) isEscaping = true;
	                    Iterator<Entry<String, Integer>> it = volFieldsMap.entrySet().iterator();
	                	while(it.hasNext()) {
	                		Map.Entry<String, Integer> pairs = (Map.Entry<String, Integer>)it.next();
	                		int intTotalFieldVal = 0;
	                		String strTotalFieldName = pairs.getKey();
	                		if (getIssueCustomFieldValue(outwardIssue, strTotalFieldName) != null && 
	                				getIssueCustomFieldValue(outwardIssue, strTotalFieldName) != "") {
	                			intTotalFieldVal = volFieldsMap.get(strTotalFieldName) + 
	                				(int) Double.parseDouble(getIssueCustomFieldValue(outwardIssue, strTotalFieldName));
	                			if (pairs.getKey().contains("Other")) {
		                			if(!strTotalOtherFieldLabel.contains(String.valueOf(getIssueCustomFieldValue(outwardIssue, 
		                					i18n.getText("voc-volume-field.other.label"))))) {
		                				if(strTotalOtherFieldLabel.length()>1) { strTotalOtherFieldLabel += ", "; }
			                			strTotalOtherFieldLabel += String.valueOf(getIssueCustomFieldValue(outwardIssue, 
			                					i18n.getText("voc-volume-field.other.label")));
		                			}
		                		}
	                		} else {
	                			intTotalFieldVal = volFieldsMap.get(strTotalFieldName);
	                		}
	                		volFieldsMap.put(strTotalFieldName, intTotalFieldVal);
	                	}
	                }
	            }
	            // And the inward linked issues
	            List<Issue> inwardIssues = linkCollection.getInwardIssues(linkType.getName());
	            if (inwardIssues != null){
	                for (Iterator<Issue> iterator2 = inwardIssues.iterator(); iterator2.hasNext();){
	                    Issue inwardIssue = iterator2.next();
	                    if(checkIsEscaping(issue.getCreated(),inwardIssue.getCreated())) isEscaping = true;
	                    Iterator<Entry<String, Integer>> it = volFieldsMap.entrySet().iterator();
	                	while(it.hasNext()) {
	                		Map.Entry<String, Integer> pairs = (Map.Entry<String, Integer>)it.next();
	                		int intTotalFieldVal = 0;
	                		String strTotalFieldName = pairs.getKey();
	                		if (getIssueCustomFieldValue(inwardIssue, strTotalFieldName) != null && 
	                				getIssueCustomFieldValue(inwardIssue, strTotalFieldName) != "") {
	                			intTotalFieldVal = volFieldsMap.get(strTotalFieldName) + 
	                				(int) Double.parseDouble(getIssueCustomFieldValue(inwardIssue, strTotalFieldName));
		                		if (pairs.getKey().contains("Other")) {
		                			if(!strTotalOtherFieldLabel.contains(String.valueOf(getIssueCustomFieldValue(inwardIssue, 
		                					i18n.getText("voc-volume-field.other.label"))))) {
		                				if(strTotalOtherFieldLabel.length()>1) { strTotalOtherFieldLabel += ", "; }
			                			strTotalOtherFieldLabel += String.valueOf(getIssueCustomFieldValue(inwardIssue, 
			                					i18n.getText("voc-volume-field.other.label")));
		                			}
		                		}
	                		} else {
	                			intTotalFieldVal = volFieldsMap.get(strTotalFieldName);
	                		}
	                		volFieldsMap.put(strTotalFieldName, intTotalFieldVal);
	                	}
	                }
	            }
		    }
	    }catch(NullPointerException e){
			log.error("NullPointerException: ",e);
			e.printStackTrace();
		}
			
		Integer intVolFieldsTotal = 0;
		for (Object i : volFieldsMap.values().toArray())
			intVolFieldsTotal += Integer.valueOf(i.toString());
		String strVolValsHtml = strCFCountLozengeTotal(intVolFieldsTotal);
		volValHtmlMap.put("total", String.valueOf(intVolFieldsTotal));
		volValHtmlMap.put("totalOtherFieldLabel", strTotalOtherFieldLabel);
		volValHtmlMap.put("isEscaping", String.valueOf(isEscaping));
		volValHtmlMap.put("isBizCritical", String.valueOf(isBizCritical(issue)));
		
		volFieldsMap.put(strTotalOtherFieldLabel, volFieldsMap.get(configurationManager.VOC_VOLUME_OTHER_VALUE));
		volFieldsMap.remove(configurationManager.VOC_VOLUME_OTHER_VALUE);
		Iterator<Entry<String, Integer>> countit = volFieldsMap.entrySet().iterator();
		
		if(countit.hasNext()) {
			strVolValsHtml += "  <span>";
			while(countit.hasNext()) {
				Map.Entry<String, Integer> pairs = (Map.Entry<String, Integer>)countit.next();			
				if(pairs.getValue() != null && (int) Double.parseDouble(pairs.getValue().toString()) != 0){
					// Add the total and support tracks to the map that is returned
					volValHtmlMap.put(pairs.getKey().toString(), String.valueOf(Double.parseDouble(pairs.getValue().toString())));
					strVolValsHtml += strCFCountLozenge(pairs.getKey().toString(), 
							(int) Double.parseDouble(pairs.getValue().toString()));
				}
			}
			strVolValsHtml += "&nbsp;&nbsp;</span>";
		}
		
		
		volValHtmlMap.put("html", strVolValsHtml);
		
		return volValHtmlMap;
	}
	
	public String strCFCountLozengeTotal(Integer count) {
		return strCFCountLozenge("<b>Total</b>", count);
	}
	
	public String strCFCountLozenge(String field, Integer count) {
		String color = "aui-lozenge";
		if(count >= 500) color = "aui-lozenge-error";
		if(500 > count && count > 10) color = "aui-lozenge-current";
		String strHtml = "&nbsp;&nbsp;" + field + ": " + 
				"<span class=\"aui-lozenge aui-lozenge-subtle " + color + "\">" +
				String.valueOf((int) count)  + "</span>";
		return strHtml;
	}
	
	public String getListHeading(String label) {
		if(label == null) {label = "voice of the customer";}
		String html = "<div id=\"listHeading\" class=\"list-heading\">" + getVOCProjectIcon() + "&nbsp;" + 
		getSRQProjectIcon() + "&nbsp;<em>" + label + "</em>&nbsp;&nbsp;" + getHelpIcon() + "</div>";
		return html;
	}
	
	public String getListFooter(String label, String query, String numIssuesLimit, 
			String numResultsLimit, String numRows, String numIssuesParsed, 
			String boolNoTotal, String boolSortTotal, String boolSortVocVolume, List<String> keys) {
		if(label == null) {label = "VOC Volume servlet";}
		if(query == null) {query = "";}
		String html = "<div id=\"listFooter\" class=\"list-footer\">" +
		"Search Limit: <span class=\"aui-lozenge-subtle aui-lozenge\">" + numIssuesLimit + "</span>&nbsp; " + 
		"Results Limit: <span class=\"aui-lozenge-subtle aui-lozenge\">" + numResultsLimit + "</span>&nbsp; " + 
		"Exclude 0 Totals: <span class=\"aui-lozenge-subtle aui-lozenge\">" + 
		((boolNoTotal.contains("true"))?"Yes":"No") + "</span>&nbsp; " + 
		"Sort By Totals: <span class=\"aui-lozenge-subtle aui-lozenge\">" + 
		((boolSortTotal.contains("true"))?"Yes":"No") + "</span>&nbsp; " + 
		"Sort By Voc Volume: <span class=\"aui-lozenge-subtle aui-lozenge\">" + 
		((boolSortVocVolume.contains("true"))?"Yes":"No") + "</span>&nbsp; " + 
		"Rows: <span class=\"aui-lozenge-subtle aui-lozenge\">" + 
		"<a href=\"" + getKeysClause(keys) + "\" title=\"Issue Navigator\">" + numRows + " of " + 
		numIssuesParsed + " linked issues found</a></span> " + 
		"<img id=\"filterQuery\" src=\"" + baseUrl + "/images/icons/ico_filters.png\" title=\"Filter Query: " + 
		StringEscapeUtils.escapeHtml(query) + "\" />" + 
		" <a target=\"_blank\" href=\"" + baseUrl + servletPath + "?jqlQuery=" + 
		StringEscapeUtils.escapeHtml(query) + "\">" +
		"<img id=\"servletVOCVolumeGadget\" src=\"" + baseUrl + "/images/icons/documentation.gif\" title=\"" + 
		label + "\" />" +
		"</a></div>";
		return html;
	}
	
	private String getBanner(String VOCVolumeBanner) {
		String strBannerHtml = "";
		if (VOCVolumeBanner == "High"){
		    strBannerHtml = "<span class=\"banner banner-high\">" +
		    "<img src=\"" + baseUrl + "/images/icons/priority_critical.gif\" alt=\"Very High Impact\" />" +
		    configurationManager.getHighGuidance() + "</span>";
		} else if (VOCVolumeBanner == "Medium"){
		    strBannerHtml = "<span class=\"banner banner-medium\">" +
		    "<img src=\"" + baseUrl + "/images/icons/priority_major.gif\" alt=\"High Impact\" />" +
		    configurationManager.getMedGuidance() + "</span>";
		} else if (VOCVolumeBanner == "Low"){
			strBannerHtml = "<span class=\"banner banner-low\">" +
		    "<img src=\"" + baseUrl + "/images/icons/priority_minor.gif\" alt=\"Low Impact\" />" +
		    configurationManager.getLowGuidance() + "</span>";
		}
		return strBannerHtml;
	}
	
	private String getHelpIcon() {
		String strHelpIcon = "<a title=\"" + i18n.getText("voc-volume-servlet.help.title") + 
		"\" href=\"" + i18n.getText("voc-volume-servlet.help.url") + "\" target=\"_blank\">" +
		"<img src=\"" + baseUrl + "/images/icons/ico_help.png\" alt=\"" + 
		i18n.getText("voc-volume-servlet.help.title") + "\" /></a>";
		return strHelpIcon;
	}
	
	private String getVOCProjectIcon(){
		return "<a target=\"_blank\" title=\"" + i18n.getText("voc-volume-request.project.title") + 
				"\" href=\"" + baseUrl + "/browse/" +  
				i18n.getText("voc-volume-request.project.key") + "\">" +
				"<img src=\"" + baseUrl +  "/images/icons/health.gif\" /></a>";
	}
	
	private String getSRQProjectIcon() {
		return "<a target=\"_blank\" title=\"" + i18n.getText("support-request.project.title") + 
				"\" href=\"" + baseUrl + "/browse/" +  
				i18n.getText("support-request.project.key") + "\">" +
				"<span class=\"aui-icon aui-icon-small aui-iconfont-add-comment\">Add Comment</span></a>";
	}
	
    private String getIssueTypeIconUrl(Issue issue) {
    	return "<img src=\"" +
                applicationProperties.getBaseUrl() + issue.getIssueTypeObject().getIconUrl() + "\" alt=\"" +
                issue.getIssueTypeObject().getName() + "\" width=\"16\" height=\"16\" title=\"" +
                issue.getIssueTypeObject().getName() + "\" />";
    }
    
    private String getIssueLink(Issue issue) {
    	String issueKey = issue.getKey();
    	String issueLabels = "";
    	String issueComponents = "";
    	if (issue.getResolutionObject() != null) {
    			issueKey = "<span class=\"issue-line-through\">" + issueKey + "</span>";
    	}
    	if (issue.getLabels() != null) {
    		issueLabels = "Labels: " + issue.getLabels().toString();
    	}
    	if (issue.getComponentObjects() != null) {
    		List<String> components = new ArrayList<String>();
    		for(ProjectComponent comp : issue.getComponentObjects()){
    			components.add(comp.getName());
    		}
    		issueComponents = "Components: " + components.toString();
    	}
    	return "<a target=\"_blank\" href=\"" + applicationProperties.getBaseUrl() + "/browse/" +
    	issue.getKey() + "\" title=\"" + issueLabels + " " + issueComponents + "\">" + issueKey + "</a>";
    }
    

    private String getIssueProjectIcon(Issue issue) throws URISyntaxException {
    	String path = baseUrl + "/images/icons/icon16-people.png";
		if(issue.getProjectObject().getAvatar() != null) {
			path = baseUrl + "/secure/projectavatar?avatarId=" + 
					issue.getProjectObject().getAvatar().getId().toString() + 
					"&pid=" + issue.getProjectObject().getId() + "&size=small";
		}
    	return "<img src=\"" + path + "\" width=\"16\" alt=\"project avatar\" />";
    }
    
    private String getIssueProjectIconLink(Issue issue) throws URISyntaxException {
    	String strProgDesc = "";
    	if(issue.getProjectObject().getDescription() != null && 
    			issue.getProjectObject().getDescription() != "") {
    		strProgDesc = ": " + issue.getProjectObject().getDescription();
    	}
    	return "<a target=\"_blank\" href=\"" + applicationProperties.getBaseUrl() + "/browse/" + 
    			issue.getProjectObject().getKey() + "\" title=\"" + 
    			issue.getProjectObject().getName() + strProgDesc + "\">" + 
    			getIssueProjectIcon(issue) + "</a>";
    }
    
    private String getIssueSummaryLink(Issue issue) {
    	String issueDesc = "";
    	String issueSummary = "";
    	if (issue.getDescription() != null) {
    		issueDesc = StringEscapeUtils.escapeHtml(issue.getDescription());
		}
    	if (issue.getSummary() != null) {
    		issueSummary = StringEscapeUtils.escapeHtml(issue.getSummary());
		}
    	return "<a target=\"_blank\" href=\"" + applicationProperties.getBaseUrl() + "/browse/" +
                issue.getKey() + "\" title=\"" + issueDesc + "\"\">" + issueSummary + "</a>";
    }
    
    private String getCurrentIssuePriority(Issue issue) {
    	String strPriority = "";
    	if(issue.getPriorityObject() != null) {
    		strPriority =  "<img src=\"" + applicationProperties.getBaseUrl() +
    			issue.getPriorityObject().getIconUrl() + "\" width=\"16\" height=\"16\" alt=\"" + 
    			issue.getPriorityObject().getName() + " " + 
    			issue.getPriorityObject().getDescription() + "\" />" + 
    			issue.getPriorityObject().getName();
    	}
    	return strPriority;
    }
    
    private String getCurrentIssueSeverity(Issue issue) {
    	String strSeverityHtml = "";
    	if(customFieldManager.getCustomFieldObjectByName(SelectSeverityField.getSeverityFieldName())
    			.getValue(issue) != null) {
    		strSeverityHtml = 
    				customFieldManager.getCustomFieldObjectByName(SelectSeverityField.getSeverityFieldName()).getValue(issue).toString();
    	}
    	return strSeverityHtml;
    }
    
    @SuppressWarnings("deprecation")
	private String getCurrentIssueStatus(Issue issue) {
    	String strResolution = "";
    	if(issue.getResolutionObject() != null){
    		strResolution = String.format(" (%1$s)",issue.getResolutionObject().getName());
    	}
    	return "<img src=\"" + applicationProperties.getBaseUrl() +
    			issue.getStatusObject().getIconUrl() + "\" width=\"16\" height=\"16\" alt=\"" + 
    			issue.getStatusObject().getDescription() + "\" />" + 
    			issue.getStatusObject().getName() + 
    			strResolution;
    }
    
    
    private String getReporterProfileURL(Issue issue){
    	return String.format("%1$s%2$s%3$s", applicationProperties.getBaseUrl()
        		, "/secure/ViewProfile.jspa?name=", issue.getReporterId());
    }
    	
	private String getAssigneeProfileURL(Issue issue){
		String strAssigneeProfileURL = "";
		if(issue.getAssignee() != null) {
			strAssigneeProfileURL = String.format("%1$s%2$s%3$s", 
					applicationProperties.getBaseUrl(), 
					"/secure/ViewProfile.jspa?name=", 
					issue.getAssigneeId());
		}
		return strAssigneeProfileURL;
    }
    
    private String getAssigneeProfileLink(Issue issue){
    	if(issue.getAssignee() != null && !issue.getAssignee().getDisplayName().isEmpty()){
    		return "<a target=\"_blank\" href=\"" + getAssigneeProfileURL(issue) + "\">" 
    				+ issue.getAssignee().getDisplayName() + "</a> (Assignee)";
    	} else {
    		String reporterName;
    		try {
				reporterName = issue.getReporter().getDisplayName();
			} catch (Exception e) {
				reporterName = "null";
			}
    		return "<a target=\"_blank\" href=\"" + getReporterProfileURL(issue) + "\">" 
    				+ reporterName + "</a> (Reporter)";
    	}
    }
    
    private String getAssigneeAvatar(Issue issue){
    	String path = baseUrl + "/images/icons/user_12.gif";
    	if(issue.getAssignee() != null) {
	    	try {
	    		URI assigneeAvatar = avatarService.getAvatarAbsoluteURL(applicationUser, 
		    			ApplicationUsers.from(issue.getAssignee()), Avatar.Size.SMALL);
				path = assigneeAvatar.toURL().toString();
			} catch (MalformedURLException e) {
				log.error("assignee avatar URL error: ", e);
				e.printStackTrace();
			}
    	}
    	return "<a href=\"" + getAssigneeProfileURL(issue) + "\"><img src=\"" + 
				path + "\" height=\"16\" alt=\"assignee avatar\" /></a>";
    }
    
    private String getKeysClause(List<String> keys) {
    	String keyClause = "";
    	StringBuilder sb = new StringBuilder();
    	if(keys != null) {
	    	for(String s: keys) { sb.append(s).append(','); }
	    	if(sb.length() > 1){
	    		sb.deleteCharAt(sb.length()-1); //delete last comma
	    	}
	    	keyClause = String.format("%1$s%2$s%3$s%4$s", 
    			applicationProperties.getBaseUrl(), 
				"/issues/?jql=issuekey%20in%20(",
				sb.toString(),
				")");
    	}
    	return keyClause;
    }
    
    private String getIssueRowCssClass(Issue issue) {
    	String strIssueRowColor = "issuerow";
    	if(issue.getStatusObject()!= null && 
    			Integer.parseInt(issue.getStatusObject().getId()) == 
    			IssueFieldConstants.CLOSED_STATUS_ID) {
    		strIssueRowColor += " issuerow-closed";
    	}
    	return strIssueRowColor;
    }
    
    private String getClosedDate(Issue issue) {
    	String strClosedDate = "";
		ChangeHistoryManager changeHistoryManager = ComponentAccessor.getChangeHistoryManager();
		List<ChangeItemBean> statusList = changeHistoryManager.getChangeItemsForField(issue,"status");
		if(statusList != null) {
			for(ChangeItemBean cib : statusList) {
				//System.out.println("history: " + cib.toString());
				//System.out.println("History field " + cib.getField().toString() + " on " + 
				//		formatDate.format(cib.getCreated()) + " changed to " + cib.getToString());
				if(cib.getToString().contains("Closed")) {
					strClosedDate = formatDate.format(cib.getCreated());
				}
			}
		}
		return strClosedDate;
    }
    
    private String getDueDate(Issue issue) {
    	String strDueDate = "";
    	if(issue.getDueDate() != null) {
    		//System.out.println("Due Date: " + issue.getDueDate());
    		strDueDate = formatDate.format(issue.getDueDate());
    	}
    	return strDueDate;
    }
    
    private String getCreatedDate(Issue issue) {
    	String strCreatedDate = "";
    	if(issue.getCreated() != null) {
    		strCreatedDate = formatDate.format(issue.getCreated());
    	}
    	return strCreatedDate;
    }
    
    private boolean checkIsEscaping(Timestamp issueCreated, Timestamp linkedCreated) {
    	return linkedCreated.before(issueCreated);
    }
    
    private String getEscapingIcon(boolean bizCritical) {
    	String strBizCriticalStyle = "";
    	String strBizCritical = "";
    	if(bizCritical) {
    		strBizCriticalStyle = "biz-critical";
    		strBizCritical = "(Business Critical) ";
    	}
		return "<span class=\"" + strBizCriticalStyle + "\"><img src=\"" + 
    		baseUrl + "/images/icons/ic_unlocked.png\"" + 
			"title=\"" + strBizCritical + 
    		i18n.getText("voc-volume-servlet.escaped.title") + "\"></span>";
    }
    
    private boolean isBizCritical(Issue issue){
    	try {
			if (String.valueOf(issue.getPriorityObject().getSequence()).matches(".*[123].*") &&
					getIssueCustomFieldValue(issue,SelectSeverityField.getSeverityFieldName())
					.matches(".*[Unusable|Painful|Annoying].*")) {
				return true;
			}
		} catch (NullPointerException e) {
		}
    	return false;
    }
    
    public String getProperty(String property) {
    	return i18n.getText(property);
    }
}
