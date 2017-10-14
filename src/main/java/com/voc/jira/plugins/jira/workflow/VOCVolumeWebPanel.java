package com.voc.jira.plugins.jira.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.LinkCollection;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.Query;
import com.atlassian.sal.api.ApplicationProperties;
import com.voc.jira.plugins.jira.components.ConfigurationManager;

/**
 * The purpose of this web panel is to provide visibility of Tier 3 Support volume field totals.
 * @author David Jellison
 * 
 * Requirements:
 * 1. VOC guidance banner appears in the VOC Volume web panel in the right column of the issue view, 
 *    if there is any value for the target collection of custom fields in linked issues
 *    a. Uservoice
 *    b. Support Tracks
 *    c. Stand-ups
 *    d. Feedback Forums
 *    e. Salesforce
 * 2. If there is no value in one of these fields, then the field is not shown
 * 3. If there is more than one linked issue with the same field, only show the highest value
 * 4. Display the VOC Volume banner for the highest value field in the set of fields displayed
 * 4. This VOC VOlume web panel will apply to all issue types
 * 5. A help button links back to the wiki page describing this feature
 *    https://wiki.roving.com/display/SalesSupport/VOC+Volume+Guidance
 *
 */
//@SuppressWarnings("unchecked")
public class VOCVolumeWebPanel extends AbstractJiraContextProvider {
	private static final Logger log = LoggerFactory.getLogger(VOCVolumeWebPanel.class);
	private static final String CF_LABEL_NAME = "cfLabelName";
	private static final String CF_COUNT_VALUE = "cfCountValue";
	private static final String CF_OTHER_SELECT = "cfOtherSelect";
	private static final String INT_VALUES_TOTAL = "intValuesTotal";
	private static final String STR_HIGHEST_INT_LABEL = "strHighestIntLabel";
	private static final String STR_OTHER_SELECT = "strOtherSelect";
	private final CustomFieldManager customFieldManager;
	private final ConfigurationManager configurationManager;
	private LinkCollection linkCollection;
    private IssueLinkManager issueLinkManager;
    private final SearchService searchService;
	private String baseUrl = "";
	private String isVisible = "yes";
	
	public VOCVolumeWebPanel(CustomFieldManager customFieldManager,
    		IssueLinkManager issueLinkManager,
    		ConfigurationManager configurationManager,
    		IssueTypeManager issueTypeManager,
    		SearchService searchService,
			ApplicationProperties applicationProperties){
		this.customFieldManager = customFieldManager;
		this.issueLinkManager = issueLinkManager;
	    this.configurationManager = configurationManager;
	    this.searchService = searchService;
	    this.baseUrl = applicationProperties.getBaseUrl();
	    System.out.println("INITIALIZED VOCVolumeWebPanel.java");
	}
    
	public Map<String, Object> getContextMap(ApplicationUser user, JiraHelper jiraHelper) {
		Issue issue = (Issue) jiraHelper.getContextParams().get("issue");
		String jql = configurationManager.getJQL();
		Map<String, Object> contextMap = new HashMap<String, Object>();
		
		String status = issue.getStatus().getName();
    	if (status != null) {
    		contextMap.put("status", status);
    	}
    	
    	if(configurationManager.getIsVisible().contains("yes")) {
    		if(jql != null && jql != "") {
        		if(matchesJql(jql, issue, user)){
        			isVisible = "yes";
        			contextMap.put("isVisible", "yes");
        		} else {
        			isVisible = "no";
        			contextMap.put("isVisible", "no");
        		}
        	} else {
        		isVisible = "yes";
        		contextMap.put("isVisible", "yes");
        	}
    	} else {
    		contextMap.put("isVisible", "no");
    	}
    	
    	if(!hasIssuetypeLinks(issue, user, configurationManager.getVOCRequest())
    			&& !hasIssuetypeLinks(issue, user, configurationManager.getSupportRequest())){
    		isVisible = "No";
    		contextMap.put("isVisible", "no");
    	}
    	
    	if(isVisible.contains("yes")) {
		    contextMap.put("baseUrl", baseUrl);
	    	
		    String issueType = issue.getIssueType().getName();
		    if (issueType != null) {
		    	contextMap.put("issueType", issueType);
		    }
		    
		    Map<String, Object> cfUservoiceMap = getAllLinkedCustomFields(issue, user, "Uservoice", "Uservoice Label");
		    if (cfUservoiceMap != null && Integer.valueOf(cfUservoiceMap.get(INT_VALUES_TOTAL).toString()) > 0) {
		    	contextMap.put("Uservoice", cfUservoiceMap.get(INT_VALUES_TOTAL).toString());
		    	contextMap.put("UservoiceLabel", cfUservoiceMap.get(STR_HIGHEST_INT_LABEL).toString());
		    }
		    
		    Map<String, Object> cfSupportTracksMap = getAllLinkedCustomFields(issue, user, "Support Tracks", "Support Tracks Label");
		    if (cfSupportTracksMap != null && Integer.valueOf(String.valueOf(cfSupportTracksMap.get(INT_VALUES_TOTAL))) > 0) {
		    	contextMap.put("SupportTracks", String.valueOf(cfSupportTracksMap.get(INT_VALUES_TOTAL)));
		    	contextMap.put("SupportTracksLabel", String.valueOf(cfSupportTracksMap.get(STR_HIGHEST_INT_LABEL)));
		    }
		    
		    Map<String, Object> cfStandupsMap = getAllLinkedCustomFields(issue, user, "Stand-ups", "Stand-ups Label");
		    if (cfStandupsMap != null && Integer.valueOf(String.valueOf(cfStandupsMap.get(INT_VALUES_TOTAL))) > 0) {
		    	contextMap.put("Stand-ups", String.valueOf(cfStandupsMap.get(INT_VALUES_TOTAL)));
		    	contextMap.put("Stand-upsLabel", String.valueOf(cfStandupsMap.get(STR_HIGHEST_INT_LABEL)));
		    }
		    
		    Map<String, Object> cfSupportTracksWeeklyAvg = getAllLinkedCustomFields(issue, user, "Support Tracks Weekly Avg");
		    if (cfSupportTracksWeeklyAvg != null && Integer.valueOf(String.valueOf(cfSupportTracksWeeklyAvg.get(INT_VALUES_TOTAL))) > 0) {
		    	contextMap.put("Support-Tracks-Weekly-Avg", String.valueOf(cfSupportTracksWeeklyAvg.get(INT_VALUES_TOTAL)));
		    }
		    
		    Map<String, Object> cfFeedbackforumsMap = getAllLinkedCustomFields(issue, user, "Feedback Forums", "Feedback Forums Label");
		    if (cfFeedbackforumsMap != null && Integer.valueOf(String.valueOf(cfFeedbackforumsMap.get(INT_VALUES_TOTAL))) > 0) {
		    	contextMap.put("Feedbackforums", String.valueOf(cfFeedbackforumsMap.get(INT_VALUES_TOTAL)));
		    	contextMap.put("FeedbackforumsLabel", String.valueOf(cfFeedbackforumsMap.get(STR_HIGHEST_INT_LABEL)));
		    }
		    
		    Map<String, Object> cfSalesforceMap = getAllLinkedCustomFields(issue, user, "Salesforce", "Salesforce Label");
		    if (cfSalesforceMap != null && Integer.valueOf(String.valueOf(cfSalesforceMap.get(INT_VALUES_TOTAL))) > 0) {
		    	contextMap.put("Salesforce", String.valueOf(cfSalesforceMap.get(INT_VALUES_TOTAL)));
		    	contextMap.put("SalesforceLabel", String.valueOf(cfSalesforceMap.get(STR_HIGHEST_INT_LABEL)));
		    }
		    
		    Map<String, Object> cfVOCVolumeOtherMap = getAllLinkedCustomFields(issue, user, 
		    		"VOC Volume Other Value", "VOC Volume Other Label", "VOC Volume Other");
		    if (cfVOCVolumeOtherMap != null && Integer.valueOf(String.valueOf(cfVOCVolumeOtherMap.get(INT_VALUES_TOTAL))) > 0) {
		    	contextMap.put("VOCVolumeOtherValue", String.valueOf(cfVOCVolumeOtherMap.get(INT_VALUES_TOTAL)));
		    	contextMap.put("VOCVolumeOtherLabel", String.valueOf(cfVOCVolumeOtherMap.get(STR_HIGHEST_INT_LABEL)));
		    	contextMap.put("VOCVolumeOtherSelect", String.valueOf(cfVOCVolumeOtherMap.get(STR_OTHER_SELECT)));
		    }
		    
		    boolean hasAnyValues = false;
		    if (
	    		Integer.valueOf(cfUservoiceMap.get(INT_VALUES_TOTAL).toString())>0 || 
	    		Integer.valueOf(cfSupportTracksMap.get(INT_VALUES_TOTAL).toString())>0 || 
	    		Integer.valueOf(cfStandupsMap.get(INT_VALUES_TOTAL).toString())>0 || 
	    		Integer.valueOf(cfFeedbackforumsMap.get(INT_VALUES_TOTAL).toString())>0 || 
	    		Integer.valueOf(cfSalesforceMap.get(INT_VALUES_TOTAL).toString())>0 || 
	    		Integer.valueOf(cfVOCVolumeOtherMap.get(INT_VALUES_TOTAL).toString())>0 ||
	    		Integer.valueOf(cfSupportTracksWeeklyAvg.get(INT_VALUES_TOTAL).toString())>0 	    			    			    		
		    ) hasAnyValues = true;
		    contextMap.put("hasAnyValues", hasAnyValues);
		    
		    String cfVOCVolumeBannerValue = getHighestLevelVOCVol(issue, user, "VOC Volume").trim();
		    if (cfVOCVolumeBannerValue != null && cfVOCVolumeBannerValue != "") {
		    	contextMap.put("VOCVolumeBanner", cfVOCVolumeBannerValue);
		    } else {
		    	contextMap.put("VOCVolumeBanner", "None");
		    }
		}
    	contextMap.put("highGuidance", configurationManager.getHighGuidance());
    	contextMap.put("medGuidance", configurationManager.getMedGuidance());
    	contextMap.put("lowGuidance", configurationManager.getLowGuidance());
    	
    	//logContextMap(contextMap);
    	return contextMap;
    }
	
    private boolean matchesJql(String jql, Issue issue, ApplicationUser user) {
        SearchService.ParseResult parseResult = searchService.parseQuery(user, jql);
        if (parseResult.isValid()) {
            Query query = JqlQueryBuilder.newBuilder(parseResult.getQuery())
                    .where()
                    .and()
                    .issue()
                    .eq(issue.getKey())
                    .buildQuery();
            try {
                return searchService.searchCount(user, query) > 0;
            } catch (SearchException e) {
                log.error("Error processing JQL: " + e.getMessage(), e);
                return false;
            }
        }

        return false;
    }
    
	private String getIssueCustomFieldValue(Issue issue, String CustomFieldName) {
		String strCFValue = "None";
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
	
	private boolean hasIssuetypeLinks(Issue issue, ApplicationUser user, String issuetypeName){
	    boolean linkedIssuetypeFound = false;
	    try{
	    	linkCollection = issueLinkManager.getLinkCollection(issue, user);
	        Collection<Issue> linkedIssues = linkCollection.getAllIssues();
	        Iterator<Issue> it = linkedIssues.iterator();
	        while(it.hasNext()) {
	        	Issue linkedIssue = (Issue) it.next();
	        	if(linkedIssue.getIssueType().getName().contains(issuetypeName)){
	        		linkedIssuetypeFound = true;
	        	}
	        }
	    }catch(NullPointerException e){
			log.error("NullPointerException: ",e);
			e.printStackTrace();
		}
	    return linkedIssuetypeFound;
	}
	
	private String getHighestLevelVOCVol(Issue issue, ApplicationUser user, String cfName){
	
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
	                    cfValues.add(getIssueCustomFieldValue(outwardIssue, cfName));
	                }
	            }
	            // And the inward linked issues
	            List<Issue> inwardIssues = linkCollection.getInwardIssues(linkType.getName());
	            if (inwardIssues != null){
	                for (Iterator<Issue> iterator2 = inwardIssues.iterator(); iterator2.hasNext();){
	                    Issue inwardIssue = iterator2.next();
	                    cfValues.add(getIssueCustomFieldValue(inwardIssue, cfName));
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
		    	if(cfHighestValue != "High") {
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
	
	private Map<String,Object> getAllLinkedCustomFields(Issue issue, ApplicationUser user, String cfCountValue) {
		return getAllLinkedCustomFields(issue, user, cfCountValue, "", "");
	}
	
	private Map<String,Object> getAllLinkedCustomFields(Issue issue, ApplicationUser user, String cfCountValue, String cfLabelName) {
		return getAllLinkedCustomFields(issue, user, cfCountValue, cfLabelName, "");
	}

	private Map<String,Object> getAllLinkedCustomFields(Issue issue, ApplicationUser user, String cfCountValue, String cfLabelName, String cfOtherSelect) {
		
		// Get accumulative count for a custom field in all linked issues with this field
	    ArrayList<Map<String,Object>> cfValuesMapList = new ArrayList<Map<String,Object>>();
	    try{
	        linkCollection = issueLinkManager.getLinkCollection(issue, user);
	        //System.out.println("linkCollection size = " + linkCollection.getAllIssues().size());
	        Set<IssueLinkType> linkTypes = linkCollection.getLinkTypes();
	    
		    for (Iterator<IssueLinkType> iterator1 = linkTypes.iterator(); iterator1.hasNext();){
	        	IssueLinkType linkType = (IssueLinkType) iterator1.next();
		    
			    // Get the outward linked issues
	            List<Issue> outwardIssues = linkCollection.getOutwardIssues(linkType.getName());
	            if (outwardIssues != null){
	                for (Iterator<Issue> iterator2 = outwardIssues.iterator(); iterator2.hasNext();){
	                    Issue outwardIssue = iterator2.next();
	                    Map<String, Object> cfValuesMap = new HashMap<String, Object>();
	                    cfValuesMap.put(CF_COUNT_VALUE, getIssueCustomFieldValue(outwardIssue, cfCountValue));
	                    if(cfLabelName != "" && cfLabelName != null) {
	                    	cfValuesMap.put(CF_LABEL_NAME, getIssueCustomFieldValue(outwardIssue, cfLabelName));
	                    }
	                    if(cfOtherSelect != "" && cfOtherSelect != null){
	                    	cfValuesMap.put(CF_OTHER_SELECT, getIssueCustomFieldValue(outwardIssue, cfOtherSelect));
	                    }
	                    cfValuesMapList.add(cfValuesMap);
	                }
	            }
	            // And the inward linked issues
	            List<Issue> inwardIssues = linkCollection.getInwardIssues(linkType.getName());
	            if (inwardIssues != null){
	                for (Iterator<Issue> iterator2 = inwardIssues.iterator(); iterator2.hasNext();){
	                    Issue inwardIssue = iterator2.next();
	                    Map<String, Object> cfValuesMap = new HashMap<String, Object>();
	                    cfValuesMap.put(CF_COUNT_VALUE, getIssueCustomFieldValue(inwardIssue, cfCountValue));
	                    if(cfLabelName != "" && cfLabelName != null) {
	                    	cfValuesMap.put(CF_LABEL_NAME, getIssueCustomFieldValue(inwardIssue, cfLabelName));
	                    }
	                    if(cfOtherSelect != "" && cfOtherSelect != null){
	                    	cfValuesMap.put(CF_OTHER_SELECT, getIssueCustomFieldValue(inwardIssue, cfOtherSelect));
	                    }
	                    cfValuesMapList.add(cfValuesMap);
	                }
	            }
		    }
	    }catch(NullPointerException e){
			log.error("NullPointerException: ",e);
			e.printStackTrace();
		}
	    
		int intValuesTotal = 0;
		int highestInt = 0;
		String strHighestIntLabel = "";
		String strOtherSelectValue = "";
		for(Iterator<Map<String, Object>> listIterator = cfValuesMapList.iterator(); listIterator.hasNext();){
			Map<String, Object> mapValue = listIterator.next();
			if(mapValue != null) {
				int intCount = 0;
				if(mapValue.get(CF_COUNT_VALUE) != null && mapValue.get(CF_COUNT_VALUE) != "None"){
					//intCount = Float.valueOf((String) mapValue.get(CF_COUNT_VALUE)).intValue();
					intCount = Float.valueOf(mapValue.get(CF_COUNT_VALUE).toString()).intValue();
				}
				if(highestInt < intCount) {
					highestInt = intCount;
					if((String) mapValue.get(CF_LABEL_NAME) != "None"){
						strHighestIntLabel = (String) mapValue.get(CF_LABEL_NAME);
						if((String) mapValue.get(CF_OTHER_SELECT) != "None"){	
							strOtherSelectValue = (String) mapValue.get(CF_OTHER_SELECT);
						}
					}
				}
				intValuesTotal += intCount;
			}
		}
		
		Map<String, Object> mapReturn = new HashMap<String,Object>();
		mapReturn.put(INT_VALUES_TOTAL, intValuesTotal);
		mapReturn.put(STR_HIGHEST_INT_LABEL, strHighestIntLabel);
		mapReturn.put(STR_OTHER_SELECT, strOtherSelectValue);
		return mapReturn;
	}

}