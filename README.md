voc-utilities-for-jira
=======================

## Jira 6.x plugin

VOC Utilities plugin for Jira 6.x is a collection of features specific to the Voice Of Customer (VoC) and Escaping Defect Management.

### Modules
1. "VOC Priority Field Validator" constrains the transition if the issue is one of {Bug, Defect} issue types and requires the Priority field to not be the value "None".
2. "VOC Severity Field Validator" constrains the transition if the issue is one of {Bug, Defect} issue types and requires the Severity field to not be null ("None").
3. "VOC Issue Guidance Web Panel" adds a web panel in the right pane of the issue view screen, if there is a value to render.
https://wiki.roving.com/display/EngDev/Jira+Defect+Instructions
4. "VOC Mail Send Post Function" enables a transition to send email with custom body and a footer showing the transition and a link back to the issue.
https://wiki.roving.com/display/EngDev/Jira+Email+Send+for+Workflow+Transitions
5. "VOC Volume Web Panel" adds a web panel in the right pane of the issue view screen, if there are linked "VOC Volume" or "Support Request" tickets.
https://wiki.roving.com/display/SalesSupport/VOC+Volume+Guidance
6. "VOC Volume Gadget" adds a Dashboard Gadget that lists
7. "Linked Issuetype Created Before" adds JQL function linkedCreatedBefore("","") for escaping defect queries. See also linkedCreatedBeforeQuery("","")
8. "Charts" D3JS charts for dashboards

## Build History
## version 3.0.0
<ol>
  <li>Initial Open Source Release</li>
</ol>

### Project Setup
Contributor setup [here](https://wiki.roving.com/display/EngDev/Jira+Greenhopper#JiraGreenhopper-CTCTUtilityPlugin). Remote debugging setup instructions here: https://developer.atlassian.com/display/DOCS/Creating+a+Remote+Debug+Target.

