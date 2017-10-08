voc-utilities-for-jira
=======================

## Jira 7.x plugin

VOC Utilities plugin for Jira 6.x is a collection of features specific to the Voice Of Customer (VoC) and Escaping Defect Management. See the VOC Utilities wiki [here](https://github.com/constantcontact/voc-utilities-for-jira/wiki)

## Modules
### Workflow
- "VOC Priority Field Validator" constrains the transition if the issue is one of {Bug, Defect} issue types and requires the Priority field to not be the value "None".
- "VOC Severity Field Validator" constrains the transition if the issue is one of {Bug, Defect} issue types and requires the Severity field to not be null ("None").
- "VOC Mail Send Post Function" enables a transition to send email with custom body and a footer showing the transition and a link back to the issue.
### Jira Query Language Functions
- "VOC Linked Issuetype Created Before" adds JQL function linkedCreatedBefore("","") for escaping defect queries. See also linkedCreatedBeforeQuery("","")
### Web Panels
- "VOC Issue Guidance Web Panel" adds a web panel in the right pane of the issue view screen, if there is a value to render.
- "VOC Volume Web Panel" adds a web panel in the right pane of the issue view screen, if there are linked "VOC Volume" or "Support Request" tickets.
### Dashboard gadgets
- "VOC Volume Gadget" adds a Dashboard Gadget that lists all the Bugs (Defects) 
- "VOC Defects and Counts" adds a Dashboard Gadget that illustrates defect correlation with Support Requests 
- "VOC Created and Resolved" adds a Dashboard Gadget that illustrates found vs. resolved and aging over time horizons

## Build History

### version 4.0.0-Jira7
<ol>
  <li>Refactor for Jira v7.0.0</li>
</ol>

### version 3.0.1
<ol>
  <li>Add memcached configuration for server and port, and toggle its use</li>
</ol>

### version 3.0.0
<ol>
  <li>Initial Open Source Release</li>
</ol>

## Project Setup
- VOC Volume [Install and Run Book](https://github.com/constantcontact/voc-utilities-for-jira/wiki/Install-and-Run-Book)
- VOC Volume [Configuration setup](https://github.com/constantcontact/voc-utilities-for-jira/wiki/VOC-Volume-Configuration) (once installed)
- VOC Volume [Contributor setup](https://github.com/constantcontact/voc-utilities-for-jira/wiki/Contributor-Setup) (if contributing back changes to code base).

