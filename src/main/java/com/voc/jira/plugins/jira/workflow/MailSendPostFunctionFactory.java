package com.voc.jira.plugins.jira.workflow;

import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.plugin.webresource.UrlMode;
//import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.FunctionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import webwork.action.ActionContext;
import java.util.HashMap;
import java.util.Map;

/*
This is the factory class responsible for dealing with the UI for the post-function.
This is typically where you put default values into the velocity context and where you store user input.
 */

public class MailSendPostFunctionFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginFunctionFactory {
    private static final Logger log = LoggerFactory.getLogger(MailSendPostFunction.class);
    public static final String FROM_ADDRESS = "fromAddr";
    public static final String DEFAULT_FROM_ADDRESS = "autobots@constantcontact.com";
    public static final String MAIL_TO_ADDRESS = "mailToAddr";
    public static final String REPLY_TO_ADDRESS = "replyToAddr";
    public static final String MAIL_SUBJECT = "mailSubj";
    public static final String MAIL_BODY = "mailBody";
    public static final String MAIL_BODY_CUSTOM_FIELDS = "mailBodyCustomFields";
    public static final String LINKED_ISSUE_CUSTOM_FIELDS = "linkedIssueCustomFields";
    public static final String EXCLUDE_CLONED_ISSUES = "excludeClonedIssues";
    public static final String LOGO_PATH = "logoPath";
    //private final WorkflowManager workflowManager;
    public static final String JQL_FIELD = "jql";
    private String logoPath = "";

    public MailSendPostFunctionFactory(WorkflowManager workflowManager, WebResourceUrlProvider webResourceUrlProvider) {
        //this.workflowManager = workflowManager;
    	this.logoPath = webResourceUrlProvider.getStaticPluginResourceUrl("com.voc.jira.plugins.VOC-Utilities:resources", 
        		"ConstantContactLogo_300x50.gif", UrlMode.ABSOLUTE);
    }

    @Override
    protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {

		//Map<String, String[]> myParams = ActionContext.getParameters();
        //final JiraWorkflow jiraWorkflow = workflowManager.getWorkflow(myParams.get("workflowName")[0]);

        //the default message
        velocityParams.put(JQL_FIELD, "");
        velocityParams.put(FROM_ADDRESS, "");
        velocityParams.put(MAIL_TO_ADDRESS, "");
        velocityParams.put(REPLY_TO_ADDRESS, "");
        velocityParams.put(MAIL_SUBJECT, "");
        velocityParams.put(MAIL_BODY, "");
        velocityParams.put(MAIL_BODY_CUSTOM_FIELDS, "");
        velocityParams.put(LINKED_ISSUE_CUSTOM_FIELDS, "");
        velocityParams.put(EXCLUDE_CLONED_ISSUES, "yes");
    }

    @Override
    protected void getVelocityParamsForEdit(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        getVelocityParamsForInput(velocityParams);
        getVelocityParamsForView(velocityParams, descriptor);
    }

    @Override
    protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        if (!(descriptor instanceof FunctionDescriptor)) {
        	log.debug("Descriptor must be a FunctionDescriptor.");
            throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
        }
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
        
        velocityParams.put(LOGO_PATH, logoPath);
        
        String jql = (String) functionDescriptor.getArgs().get(JQL_FIELD);
        if (jql == null) {
            jql = "";
        }
        velocityParams.put(JQL_FIELD, jql);
        
        String fromAddr = (String) functionDescriptor.getArgs().get(FROM_ADDRESS);
        if (fromAddr == null) {
            fromAddr = "";
        }
        velocityParams.put(FROM_ADDRESS, fromAddr);
        
        String mailToAddr = (String) functionDescriptor.getArgs().get(MAIL_TO_ADDRESS);
        if (mailToAddr == null) {
            mailToAddr = "";
        }
        velocityParams.put(MAIL_TO_ADDRESS, mailToAddr);
        
        String replyToAddr = (String) functionDescriptor.getArgs().get(REPLY_TO_ADDRESS);
        if (replyToAddr == null) {
            replyToAddr = "";
        }
        velocityParams.put(REPLY_TO_ADDRESS, replyToAddr);
        
        String mailSubj = (String) functionDescriptor.getArgs().get(MAIL_SUBJECT);
        if (mailSubj == null) {
            mailSubj = "";
        }
        velocityParams.put(MAIL_SUBJECT, mailSubj);
        
        String mailBody = (String) functionDescriptor.getArgs().get(MAIL_BODY);
        if (mailBody == null) {
            mailBody = "";
        }
        velocityParams.put(MAIL_BODY, mailBody);
        
        String mailBodyCustomFields = (String) functionDescriptor.getArgs().get(MAIL_BODY_CUSTOM_FIELDS);
        if (mailBodyCustomFields == null) {
            mailBodyCustomFields = "";
        }
        velocityParams.put(MAIL_BODY_CUSTOM_FIELDS, mailBodyCustomFields);
        
        String linkedIssueCustomFields = (String) functionDescriptor.getArgs().get(LINKED_ISSUE_CUSTOM_FIELDS);
        if (linkedIssueCustomFields == null) {
            linkedIssueCustomFields = "";
        }
        velocityParams.put(LINKED_ISSUE_CUSTOM_FIELDS, linkedIssueCustomFields);
        
        //Boolean excludeClonedIssues = Boolean.valueOf((Boolean) functionDescriptor.getArgs().get(EXCLUDE_CLONED_ISSUES));
        String excludeClonedIssues = (String) functionDescriptor.getArgs().get(EXCLUDE_CLONED_ISSUES);
        System.out.println("excludeClonedIssues: " + excludeClonedIssues);
        if (excludeClonedIssues == null) {
        	excludeClonedIssues = "yes";
        }
        velocityParams.put(EXCLUDE_CLONED_ISSUES, excludeClonedIssues);

    }

    public Map<String, Object> getDescriptorParams(Map<String, Object> formParams) {
    	
        Map<String, Object> params = new HashMap<String, Object>();
        String jql = extractSingleParam(formParams, JQL_FIELD);
        params.put(JQL_FIELD, jql);
        String fromAddr = extractSingleParam(formParams, FROM_ADDRESS);
        params.put(FROM_ADDRESS, fromAddr);
        String mailToAddr = extractSingleParam(formParams, MAIL_TO_ADDRESS);
        params.put(MAIL_TO_ADDRESS, mailToAddr);
        String replyToAddr = extractSingleParam(formParams, REPLY_TO_ADDRESS);
        params.put(REPLY_TO_ADDRESS, replyToAddr);
        String mailSubj = extractSingleParam(formParams, MAIL_SUBJECT);
        params.put(MAIL_SUBJECT, mailSubj);
        String mailBody = extractSingleParam(formParams, MAIL_BODY);
        params.put(MAIL_BODY, mailBody);
        String mailBodyCustomFields = extractSingleParam(formParams, MAIL_BODY_CUSTOM_FIELDS);
        params.put(MAIL_BODY_CUSTOM_FIELDS, mailBodyCustomFields);
        String linkedIssueCustomFields = extractSingleParam(formParams, LINKED_ISSUE_CUSTOM_FIELDS);
        params.put(LINKED_ISSUE_CUSTOM_FIELDS, linkedIssueCustomFields);
        try {
	        String excludeClonedIssues = extractSingleParam(formParams, EXCLUDE_CLONED_ISSUES);
	        params.put(EXCLUDE_CLONED_ISSUES, excludeClonedIssues);
    	} catch (IllegalArgumentException e) {
	        params.put(EXCLUDE_CLONED_ISSUES, "no");
    	} catch (NullPointerException e) {
    		params.put(EXCLUDE_CLONED_ISSUES, "no");
    	}
        return params;
    }
    
    public String getLogoPath() {
    	if (logoPath != null) { return logoPath; }
    	return "";
    }
    

}