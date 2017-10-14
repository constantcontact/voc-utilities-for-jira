package com.voc.jira.plugins.jira.workflow;

//import com.voc.jira.plugins.jira.components.ConfigurationManager;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.LinkCollection;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.mail.Email;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.atlassian.mail.MailException;
import com.atlassian.mail.MailFactory;
import com.atlassian.mail.queue.SingleMailQueueItem;
import com.atlassian.mail.server.SMTPMailServer;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.query.Query;
import com.atlassian.sal.api.ApplicationProperties;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;
import com.opensymphony.workflow.loader.WorkflowDescriptor;

/*
 * This class is the logic for the post function.
 */
public class MailSendPostFunction extends AbstractJiraFunctionProvider {
    private static final Logger log = LoggerFactory.getLogger(MailSendPostFunction.class);
    public static final String ISSUE = "issue";
    private static final String MAIL_SMTP = "smtp";
    private CustomFieldManager customFieldManager;
    private LinkCollection linkCollection;
    private IssueLinkManager issueLinkManager;
    private final SearchService searchService;
    private final ApplicationProperties applicationProperties;
    private String logoPath = "";
    private final AvatarService avatarService = ComponentAccessor.getAvatarService();

    public MailSendPostFunction(
            com.atlassian.jira.user.util.UserManager jiraUserManager,
            ComponentAccessor componentAccessor,
            //ConfigurationManager configurationManager,
            CustomFieldManager customFieldManager,
            IssueLinkManager issueLinkManager,
            //LinkCollection linkCollection,
            ApplicationProperties applicationProperties,
            SearchService searchService,
            WebResourceUrlProvider webResourceUrlProvider) {
        this.applicationProperties = applicationProperties;
        this.searchService = searchService;
        this.customFieldManager = ComponentAccessor.getCustomFieldManager();
        this.issueLinkManager = issueLinkManager;
        //this.linkCollection = issueLinkManager.getLinkCollection(issue, user);
        this.logoPath = webResourceUrlProvider.getStaticPluginResourceUrl("com.voc.jira.plugins.VOC-Utilities:resources",
                "ConstantContactLogo_300x50.gif", UrlMode.ABSOLUTE);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public void execute(@SuppressWarnings("rawtypes") Map transientVars, @SuppressWarnings("rawtypes") Map args, PropertySet ps) throws WorkflowException {

        boolean shouldNotify = true;

        Issue issue = getIssue(transientVars);

        WorkflowDescriptor descriptor = (WorkflowDescriptor) transientVars.get("descriptor");
        Integer actionId = (Integer) transientVars.get("actionId");
        ActionDescriptor action = descriptor.getAction(actionId);
        Issue originalIssue = (Issue) transientVars.get("originalissueobject");
        String firstStepName = "";
        if (originalIssue != null) {
            Status status = originalIssue.getStatusObject();
            firstStepName = "<b>" + StringEscapeUtils.escapeHtml(status.getName()) + "</b>&rarr;";
        }

        String actionName = action.getName();
        StepDescriptor endStep = (StepDescriptor) descriptor.getStep(action.getUnconditionalResult().getStep());

        String fromAddr = (String) args.get(MailSendPostFunctionFactory.FROM_ADDRESS);
        String mailAddrToNotify = (String) args.get(MailSendPostFunctionFactory.MAIL_TO_ADDRESS);
        System.out.println("mailToAddr: " + mailAddrToNotify);
        String replyToAddr = (String) args.get(MailSendPostFunctionFactory.REPLY_TO_ADDRESS);
        String mailSubj = (String) args.get(MailSendPostFunctionFactory.MAIL_SUBJECT);
        String mailBody = (String) args.get(MailSendPostFunctionFactory.MAIL_BODY);

        if (fromAddr == null || fromAddr == "") {
            //fromAddr = jiraActionSupport.getText("mail-send-post-function.defaultFromAddress");
            fromAddr = MailSendPostFunctionFactory.DEFAULT_FROM_ADDRESS;
        }

        if (null == mailAddrToNotify) {
            shouldNotify = false; // if no address to mail to, don't notify
        }

        String jql = (String) args.get(MailSendPostFunctionFactory.JQL_FIELD);
        if (shouldNotify && jql != null && !jql.trim().equals("")) {
            shouldNotify = matchesJql(jql, issue, getCaller(transientVars, args)); // if doesn't match JQL, don't notify
        }

        if (shouldNotify) {
            log.debug("[" + issue.getKey() + "] MailSendPostFunction should send mail...");

            ApplicationUser updatedByUser = getCaller(transientVars, args);

            String currentAssigneeLink = "";
            if (issue.getAssignee() != null) {
                currentAssigneeLink = "<a href=\"" + applicationProperties.getBaseUrl() +
                        "/secure/ViewProfile.jspa?name=" + StringEscapeUtils.escapeHtml(issue.getAssignee().getName()) +
                        "\">" + issue.getAssignee().getDisplayName() + "</a>";
            } else {
                currentAssigneeLink = "Unassigned";
            }

            String reporterLink = "";
            if (issue.getReporter() != null) {
                reporterLink = "<a href=\"" + applicationProperties.getBaseUrl() +
                        "/secure/ViewProfile.jspa?name=" + StringEscapeUtils.escapeHtml(issue.getReporter().getName()) +
                        "\">" + issue.getReporter().getDisplayName() + "</a>";
            }

            String mailHeader = "<img id=\"logoImg\" src=\"" + logoPath + "\" width=\"150\" /><br/>";

            StringBuilder fieldTableBuilder = new StringBuilder();
            fieldTableBuilder.append("<hr/>");
            //fieldTableBuilder.append("<img id=\"logoImg\" src=\"" + logoPath +"\" width=\"150\" />");
            fieldTableBuilder.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">");
            fieldTableBuilder.append(createKVRow("Issue Type:", getIssueTypeIconUrl(issue) + issue.getIssueTypeObject().getName()));
            fieldTableBuilder.append(createKVRow("Summary:", getIssueSummaryLink(issue)));
            fieldTableBuilder.append(createKVRow("Assignee:", getAssigneeAvatar(issue) + "&nbsp;" + currentAssigneeLink));
            fieldTableBuilder.append(createKVRow("Priority:", getCurrentIssuePriority(issue)));
            fieldTableBuilder.append(createKVRow("Reporter:", getReporterAvatar(issue) + "&nbsp;" + reporterLink));

            //String cfList = "VOC Severity, Assigned Group, SOX or PCI System, Maintenance Reason, Scope of Impact, Maintenance Process, Verification Testing, Backout Strategy";
            String cfList = (String) args.get(MailSendPostFunctionFactory.MAIL_BODY_CUSTOM_FIELDS);

            if (cfList != null && cfList != "") {
                String[] cfListArray = cfList.split(",");
                for (int i = 0; i < cfListArray.length; i++) {
                    //System.out.println("cfListArray[i].trim(): " + cfListArray[i].trim());
                    if (customFieldManager.getCustomFieldObjectByName(cfListArray[i].trim()) != null) {
                        CustomField cf = customFieldManager.getCustomFieldObjectByName(cfListArray[i].trim());
                        String strCFValue = "None";
                        if (cf.getValue(issue) != null) {
                            if (cf.getCustomFieldType().getName().contains("User Picker")) {
                                User userUPV = (User) cf.getValue(issue);
                                strCFValue = getUserProfileLink(userUPV);
                            } else {
                                strCFValue = cf.getValue(issue).toString();
                            }
                        }
                        fieldTableBuilder.append(createKVRow(cfListArray[i] + ":", strCFValue));
                    }
                }
            }
            fieldTableBuilder.append("</table>");
            String updateLink = updatedByUser == null
                    ? "Anonymous (someone who was not logged into JIRA at the time)"
                    : "<a href=\"" + applicationProperties.getBaseUrl() +
                    "/secure/ViewProfile.jspa?name=" + StringEscapeUtils.escapeHtml(updatedByUser.getName()) + "\">" +
                    StringEscapeUtils.escapeHtml(updatedByUser.getDisplayName()) + "</a>";
            String mailFooter = "<div style=\"font-size:14px;\">" + getIssueTypeIconUrl(issue) + getIssueLink(issue) + "&nbsp;" +
                    firstStepName + "<em>" + StringEscapeUtils.escapeHtml(actionName) +
                    "</em>&rarr;<b>" + StringEscapeUtils.escapeHtml(endStep.getName()) + "</b>" +
                    " by " + updateLink + ". Current assignee is " + currentAssigneeLink + ".</div>";

            String mailTrailer = "<hr/><table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f0f0f0;color:#000000;width:100%;\"><tr valign=\"top\">" +
                    "<td style=\"color:#505050;font-family:Arial,FreeSans,Helvetica,sans-serif;font-size:10px;line-height:14px;padding: 0 16px 16px 16px;text-align:center;\">" +
                    "This message is automatically generated by a JIRA status transition.<br>If you think it was sent incorrectly, please contact your " +
                    "<a style=\"color:#326ca6;\" href=\"" + applicationProperties.getBaseUrl() + "/secure/ContactAdministrators!default.jspa\">JIRA administrators</a>.</td></tr></table>";

            if (fieldTableBuilder.toString() != "" || fieldTableBuilder.toString() != null) {
                //System.out.println("Field Table HTML: " + fieldTableBuilder.toString());
                mailBody = mailHeader + mailBody + fieldTableBuilder.toString() + createLinkedIssuesSection(issue, updatedByUser, args) + "<hr/>" + mailFooter + mailTrailer;
            } else {
                mailBody = mailHeader + mailBody + createLinkedIssuesSection(issue, updatedByUser, args) + "<hr/>" + mailFooter + mailTrailer;
            }

            mailSubj = "(" + issue.getKey() + ") " + mailSubj;

            sendMail(MAIL_SMTP, fromAddr, mailAddrToNotify, replyToAddr, mailSubj, mailBody);
        } else {
            log.debug("[" + issue.getKey() + "] MailSendPostFunction should NOT send mail...");
        }
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

    /**
     * The sendMail method builds an email object and either sends our through the SMTP configuration
     * directly, or submits to the mail queue list to send.
     * SMTP example: sendMail("smtp", fromAddr, mailAddrToNotify, replyToAddr, mailSubj, mailBody);
     * QUEUE example: sendMail("queue", fromAddr, mailAddrToNotify, replyToAddr, mailSubj, mailBody);
     *
     * @param sendType
     * @param fromAddr
     * @param toList
     * @param replyToAddr
     * @param mailSubj
     * @param mailBody
     */
    public void sendMail(String sendType, String fromAddr, String toList, String replyToAddr, String mailSubj, String mailBody) {
        Email email = new Email(toList);
        email.setFrom(fromAddr);
        email.setReplyTo(replyToAddr);
        email.setCc("");  // list of email addresses
        email.setBcc("");
        email.setEncoding(ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_WEBWORK_ENCODING));
        email.setSubject(mailSubj);
        email.setBody(mailBody);
        email.setMimeType("text/html");  // or "text/plain"
        if (sendType == "queue") {
            ComponentAccessor.getMailQueue().addItem(new SingleMailQueueItem(email));
        } else {
            try {
                SMTPMailServer mailServer = MailFactory.getServerManager().getDefaultSMTPMailServer();
                mailServer.send(email);
            } catch (MailException e) {
                log.error("sendMail error: ", e);
                e.printStackTrace();
            }
        }
    }

    private String createKVRow(String key, String value) {
        return "<tr valign=\"top\">"
                + "<td style=\"color:#000000;font-family:Arial,FreeSans,Helvetica,sans-serif;font-size:12px;padding:0 10px 5px 0;white-space:nowrap;\">" + key
                + "</td><td style=\"color:#000000;font-family:Arial,FreeSans,Helvetica,sans-serif;font-size:12px;padding:0 0 5px 0;width:100%;\">" + value
                + "</td></tr>";
    }

    private String getIssueTypeIconUrl(Issue issue) {
    	if(issue.getIssueType() != null) {    		
	        return "<img src=\"" +
	                applicationProperties.getBaseUrl() + issue.getIssueType().getIconUrl() + "\" alt=\"" +
	                issue.getIssueType().getName() + "\" width=16 height=16 />"; 
        }
    	else {
    		return "";
    	}
    }

	private String getIssueLink(Issue issue) {
		if(issue != null){
			String issueKey = issue.getKey();
			if(issue.getResolution() != null){
				if (((Issue) issue.getResolution()) != null) {
					issueKey = "<span style=\"text-decoration:line-through;\">" + issueKey + "</span>";
        		}
			}
        	return "<a href=\"" + applicationProperties.getBaseUrl() + "/browse/" +
                issue.getKey() + "\">" + issueKey + "</a>";
		}else{
			return "";
		}
					
    }

    private String getIssueSummaryLink(Issue issue) {
        return "<a href=\"" + applicationProperties.getBaseUrl() + "/browse/" +
                issue.getKey() + "\">" + issue.getSummary() + "</a>";
    }

    private String getCurrentIssuePriority(Issue issue) {
        return "<img src=\"" + applicationProperties.getBaseUrl() +
                issue.getPriority().getIconUrl() + "\" width=16 height=16 alt=\"" + issue.getPriority().getName() + " " +
                issue.getPriority().getDescription() + "\" />" +
                issue.getPriority().getName();                
    }

    //@SuppressWarnings("deprecation")
	private String getCurrentIssueStatus(Issue issue) {
        return "<img src=\"" + applicationProperties.getBaseUrl() +        		
                issue.getStatus().getIconUrlHtml() + "\" width=16 height=16 alt=\"" + issue.getStatus().getDescription() + "\" />" +
                issue.getStatus().getName();
    }

    private String getUserProfileLink(String name, String displayName) {
        String strUserProfileURL = String.format("%1$s%2$s%3$s", applicationProperties.getBaseUrl(),
                "/secure/ViewProfile.jspa?name=", name);
        return "<a href=\"" + strUserProfileURL + "\">" + displayName + "</a>";
    }

    private String getUserProfileLink(User usr) {
        return getUserProfileLink(usr.getName(), usr.getDisplayName());
    }

    private String getUserProfileLink(CustomField cf, Issue issue) {
        if (cf == null || issue == null) {
            return "None";
        }
        Object v = cf.getValue(issue);
        /* User suppressed to ApplicationUser in 7.x
        if (v instanceof User) {
            User u = (User) v;
            return getUserProfileLink(u.getName(), u.getDisplayName());
        }
        */
        if (v instanceof ApplicationUser) {
            ApplicationUser a = (ApplicationUser) v;
            return getUserProfileLink(a.getName(), a.getDisplayName());
        }
        try {
            return cf.getValue(issue).toString();
        } catch (Exception e) {
            return "None";
        }
    }

    private String getReporterProfileURL(Issue issue) {
        return String.format("%1$s%2$s%3$s", applicationProperties.getBaseUrl()
                , "/secure/ViewProfile.jspa?name=", issue.getReporterId());
    }

    //@SuppressWarnings("deprecation")
	private String getReporterAvatar(Issue issue) {
        URI reporterAvatar = avatarService.getAvatarAbsoluteURL(issue.getReporter(), issue.getReporter(), Avatar.Size.SMALL);
        String reporterAvatarURL = "";
        try {
            reporterAvatarURL = "<a href=\"" + getReporterProfileURL(issue) + "\"><img src=\"" +
                    reporterAvatar.toURL() + "\" height=\"16\" /></a>";
        } catch (MalformedURLException e) {
            log.error("assignee avatar URL error: ", e);
            e.printStackTrace();
        }
        return reporterAvatarURL;
    }

    private int count(String text, String search) {
        int count = 0;
        for (int i = -1; (i = text.indexOf(search, i + 1)) != -1; count++) {
        }
        return count;
    }

    //TODO: Replace the substring for "ViewProfile.jspa" to use a jira URL builder.
    private String getAssigneeProfileURL(Issue issue) {
        return String.format("%1$s%2$s%3$s", applicationProperties.getBaseUrl()
                , "/secure/ViewProfile.jspa?name=", issue.getAssigneeId());
    }

    private String getAssigneeProfileLink(Issue issue) {
    	if(issue != null && issue.getAssignee() != null && issue.getAssignee().getDisplayName() != null && !issue.getAssignee().getDisplayName().isEmpty()){
            return "<a href=\"" + getAssigneeProfileURL(issue) + "\">" + issue.getAssignee().getDisplayName() + "</a>";
        } else {
            return "<a href=\"" + getReporterProfileURL(issue) + "\">" + issue.getReporter().getDisplayName() + "</a>";
        }
    }

    //@SuppressWarnings("deprecation")
	private String getAssigneeAvatar(Issue issue) {
        URI assigneeAvatar = avatarService.getAvatarAbsoluteURL(issue.getAssignee(), issue.getAssignee(), Avatar.Size.SMALL);
        String assigneeAvatarURL = "";
        try {
            assigneeAvatarURL = "<a href=\"" + getAssigneeProfileURL(issue) + "\"><img src=\"" +
                    assigneeAvatar.toURL() + "\" height=\"16\" /></a>";
        } catch (MalformedURLException e) {
            log.error("assignee avatar URL error: ", e);
            e.printStackTrace();
        }
        return assigneeAvatarURL;
    }

    private String getLinkedIssueCustomFields(Issue issue, Map<String, Object> args) {
        StringBuilder linkedIssueCustomFields = new StringBuilder("");
        String cfLinkedIssueList = (String) args.get(MailSendPostFunctionFactory.LINKED_ISSUE_CUSTOM_FIELDS);
        if (cfLinkedIssueList != null && cfLinkedIssueList != "") {
            String[] cfLinkedIssueListArray = cfLinkedIssueList.split(",");
            for (int i = 0; i < cfLinkedIssueListArray.length; i++) {
                if (customFieldManager.getCustomFieldObjectByName(cfLinkedIssueListArray[i].trim()) != null) {
                    CustomField cf = customFieldManager.getCustomFieldObjectByName(cfLinkedIssueListArray[i].trim());
                    String strCFValue = "None";
                    if (cf != null && issue != null && cf.getValue(issue) != null) {
                        if (cf.getCustomFieldType().getName().contains("User Picker")) {
                            strCFValue = getUserProfileLink(cf, issue);
                        } else {
                            strCFValue = cf.getValue(issue).toString();
                        }
                    }
                    linkedIssueCustomFields.append("<div style=\"font-family:Arial;font-size:12px;margin-left:40px;padding:0 10px 5px 0;\">" +
                            cfLinkedIssueListArray[i] + ": " + strCFValue + "</div>");
                }
            }
        }
        return linkedIssueCustomFields.toString();
    }

    /*
     * Create list of linked issues
     */
    private String createLinkedIssuesSection(Issue issue, ApplicationUser updatedByUser, Map<String, Object> args) {
        String linkedIssuesString = "";
        try {
            linkCollection = issueLinkManager.getLinkCollection(issue, updatedByUser);
            StringBuilder linkedIssuesBuilder = new StringBuilder();
            Set<IssueLinkType> linkTypes = linkCollection.getLinkTypes();
            if (linkTypes != null) {
                //System.out.println("Exclude Cloned Issues: " + args.get(MailSendPostFunctionFactory.EXCLUDE_CLONED_ISSUES));
                if (linkTypes.size() > 0) {
                    linkedIssuesBuilder.append("<div style=\"font-family:Arial;font-size:16px;color:gray;border-top-style:dotted;border-top-color:gray;padding:0 10px 5px 0;\">Issue Links</div>");
                }
                String renderLinkType = "true";
                // For each link type
                for (Iterator<IssueLinkType> iterator1 = linkTypes.iterator(); iterator1.hasNext(); ) {
                    IssueLinkType linkType = (IssueLinkType) iterator1.next();
                    renderLinkType = "true";

                    try {
                        if (linkType.getName().contains("Cloners") && args.get(MailSendPostFunctionFactory.EXCLUDE_CLONED_ISSUES).equals("yes")) {
                            //System.out.println("linkType contains \"Cloners\" and excludeClonedIsses is \"yes\"");
                            renderLinkType = "false";
                        }
                    } catch (NullPointerException e) {
                        //System.out.println("linkType contains \"Cloners\" and excludeClonedIsses is missing from ags");
                    }
                    //System.out.println("linkType: " + linkType.getName());

                    if (renderLinkType == "true") {
                        // Get the outward linked issues
                        List<Issue> outwardIssues = linkCollection.getOutwardIssues(linkType.getName());
                        if (outwardIssues != null) {
                            linkedIssuesBuilder.append("<div style=\"font-family:Arial;font-size:12px;font-weight:bold;margin-left:10px;color:gray;\">" +
                                    linkType.getOutward() + "</div>");
                            for (Iterator<Issue> iterator2 = outwardIssues.iterator(); iterator2.hasNext(); ) {
                                Issue outwardIssue = iterator2.next();
                                linkedIssuesBuilder.append("<div style=\"font-family:Arial;font-size:12px;margin-left:20px;padding:0 10px 5px 0;\">" +
                                        getIssueTypeIconUrl(outwardIssue) + "&nbsp;" + getIssueLink(outwardIssue) + ", <em>" +
                                        outwardIssue.getSummary() + "</em>&nbsp;" + getCurrentIssuePriority(outwardIssue) + "&nbsp;" +
                                        getCurrentIssueStatus(outwardIssue) + "&nbsp;&nbsp;" +
                                        getAssigneeAvatar(outwardIssue) + "&nbsp;" + getAssigneeProfileLink(outwardIssue) + "<br/>" +
                                        "</div>");
                                linkedIssuesBuilder.append(getLinkedIssueCustomFields(outwardIssue, args));
                            }
                        }
                        // And the inward linked issues
                        List<Issue> inwardIssues = linkCollection.getInwardIssues(linkType.getName());
                        if (inwardIssues != null) {
                            linkedIssuesBuilder.append("<div style=\"font-family:Arial;font-size:12px;font-weight:bold;margin-left:10px;color:gray;\">" +
                                    linkType.getInward() + "</div>");
                            for (Iterator<Issue> iterator2 = inwardIssues.iterator(); iterator2.hasNext(); ) {
                                Issue inwardIssue = iterator2.next();
                                linkedIssuesBuilder.append("<div style=\"font-family:Arial;font-size:12px;margin-left:20px;padding:0 10px 5px 0;\">" +
                                        getIssueTypeIconUrl(inwardIssue) + "&nbsp;" + getIssueLink(inwardIssue) + ", <em>" +
                                        inwardIssue.getSummary() + "</em>&nbsp;" + getCurrentIssuePriority(inwardIssue) + "&nbsp;" +
                                        getCurrentIssueStatus(inwardIssue) + "&nbsp;&nbsp;" +
                                        getAssigneeAvatar(inwardIssue) + "&nbsp;" + getAssigneeProfileLink(inwardIssue) +
                                        "</div>");
                                linkedIssuesBuilder.append(getLinkedIssueCustomFields(inwardIssue, args));
                            }
                        }
                    }
                }
            }

            if (linkedIssuesBuilder.toString() != "" && linkedIssuesBuilder.toString() != null && count(linkedIssuesBuilder.toString(), "<div ") > 1) {
                linkedIssuesString = linkedIssuesBuilder.toString();
            }
        } catch (NullPointerException e) {
            log.error("NullPointerException: ", e);
            e.printStackTrace();
        }
        return linkedIssuesString;
    }

}