package com.voc.jira.plugins.jira.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.Query;
import com.atlassian.sal.api.ApplicationProperties;
import com.voc.jira.plugins.jira.components.ConfigurationManager;
import com.voc.jira.plugins.jira.customfield.SelectSeverityField;

/**
 * The purpose of this web panel is to provide guidance for standard guard rails in the organization.
 * @author David Jellison
 *
 */
//@SuppressWarnings("unchecked")
public class IssueGuidanceWebPanel extends AbstractJiraContextProvider {
	//private static final Logger log = LoggerFactory.getLogger(IssueGuidanceWebPanel.class);
	private final CustomFieldManager customFieldManager;
	private final ConfigurationManager configurationManager;
	private final SearchService searchService;
	//private SelectSeverityField<?, ?> selectSeverityField;
	private ArrayList<String> defectTypes = new ArrayList<String>();
	private String baseUrl = "";
	@SuppressWarnings("unused")
	private String isVisible = "yes";
	
	
	public IssueGuidanceWebPanel(CustomFieldManager customFieldManager,
			ApplicationProperties applicationProperties,
			ConfigurationManager configurationManager,
			SearchService searchService){
		this.customFieldManager = customFieldManager;
		this.configurationManager = configurationManager;
		this.searchService = searchService;
		defectTypes.add("Bug");
	    defectTypes.add("Defect");
	    this.baseUrl = applicationProperties.getBaseUrl();
	}
    
	public Map<String, Object> getContextMap(ApplicationUser user, JiraHelper jiraHelper) {
		Issue issue = (Issue) jiraHelper.getContextParams().get("issue");
		String jql = configurationManager.getJQL();
		Map<String, Object> contextMap = new HashMap<String, Object>();
		
		String status = issue.getStatus().getName();
    	if (status != null) {
    		contextMap.put("status", status);
    	}
		
		// Weighted Priority & Resolution Guidance
		if(defectTypes.contains(issue.getIssueType().getName())){
		    //log.info(issue.getIssueType().getName()
		    //    + " IssueType is one of the known defect issue types collection.");
		    contextMap.put("baseUrl", baseUrl);
	    	
		    String issueType = issue.getIssueType().getName();
		    if (issueType != null) {
		    	contextMap.put("issueType", issueType);
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
		    
		    String priority = issue.getPriority().getName();		    		
	    	if (priority != null) {
	    		contextMap.put("priority", priority);
	    	}
	    	
	    	try{
				if(customFieldManager.getCustomFieldObjectByName(SelectSeverityField.getSeverityFieldName()) != null) {
			    	CustomField cfSeverity = customFieldManager.getCustomFieldObjectByName(SelectSeverityField.getSeverityFieldName());
			    	Object severity = issue.getCustomFieldValue(cfSeverity);
			    	if (severity == null) {
			    		severity = "None";
			    	}
			    	contextMap.put("severity", severity);
	    		} else {
	    			contextMap.put("messageError", String.format("Missing custom field \"%s\". Contact your Jira Administrator.",
	    					SelectSeverityField.getSeverityFieldName()));
	    		}
	    	} catch(NullPointerException e) {
	    		//log.error(String.format("Missing custom field \"%s\".",SelectSeverityField.getSeverityFieldName()), e);
	    		contextMap.put("messageError", String.format("Missing custom field \"%s\". Contact your Jira Administrator.",
	    				SelectSeverityField.getSeverityFieldName()));
	    		e.printStackTrace();
	    	}
	    	
	    	if (issue.getResolution() != null){
	    		Object resolution = issue.getResolution().getName();
		    	Object resolutionDate = issue.getResolutionDate();
		    	if (resolution == null) {
		    		resolution = "Unresolved";
		    	}
		    	contextMap.put("resolution", resolution);
		    	contextMap.put("resolutionDate", resolutionDate);
	    	}
		}
		
		// "Due Date" guidance
		// TODO: add that when Closed, the gap is presented historically (Due Date : Closed Date) 
		/*
    	Timestamp dueDate = issue.getDueDate();
    	if (dueDate != null) {
    	    int currentTimeInDays = (int) (System.currentTimeMillis() / MILLIS_IN_DAY);
    	    int dueDateTimeInDays = (int) (dueDate.getTime() / MILLIS_IN_DAY);
    	    int daysAwayFromDueDateCalc = dueDateTimeInDays - currentTimeInDays;
    	    contextMap.put("daysAwayFromDueDate", daysAwayFromDueDateCalc);
    	}
    	*/
    	
    	return contextMap;
    }
	
    private boolean matchesJql(String jql, Issue issue, ApplicationUser caller) {
        SearchService.ParseResult parseResult = searchService.parseQuery(caller, jql);
        if (parseResult.isValid()) {
            Query query = JqlQueryBuilder.newBuilder(parseResult.getQuery())
                    .where()
                    .and()
                    .issue()
                    .eq(issue.getKey())
                    .buildQuery();
            try {
                return searchService.searchCount(caller, query) > 0;
            } catch (SearchException e) {
                //log.error("Error processing JQL: " + e.getMessage(), e);
                return false;
            }
        }

        return false;
    }
}