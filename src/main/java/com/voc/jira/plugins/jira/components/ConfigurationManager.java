package com.voc.jira.plugins.jira.components;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.voc.jira.plugins.jira.customfield.SelectSeverityField;

public class ConfigurationManager {
	public static final String PLUGIN_STORAGE_KEY = "com.voc.jira.plugins.jira";
    private static final String SMTP_SERVER = "smtp.roving.com";
    private static final String JQL = "jql";
    private static final String ISSUETYPES_JQL = "issuetypesJQL";
    private static final String IS_VISIBLE = "isVisible";
    private static final String HIGH_GUIDANCE = "highGuidance";
    private static final String MED_GUIDANCE = "medGuidance";
    private static final String LOW_GUIDANCE = "lowGuidance";
    private static final String MEMCACHED_SERVER_HOST = "memcachedServerHost";
    private static final String MEMCACHED_SERVER_PORT = "memcachedServerPort";
    private static final String VOC_REQUEST = "VOC Request";
    private static final String SUPPORT_REQUEST = "Support Request";
    private static final String CREATE_SEVERITY = "createSeverity";
    
    // VOC Volume Custom Fields
    private static final String USERVOICE = "Uservoice";
    private static final String SUPPORTTRACKS = "Support Tracks";
    private static final String SUPPORT_TRACKS_WEEKLY_AVERAGE = "Support Tracks Weekly Avg";
    private static final String STAND_UPS = "Stand-ups";
    private static final String FEEDBACKFORUMS = "Feedback Forums";
    private static final String SALESFORCE = "Salesforce";
    public final String VOC_VOLUME_OTHER_VALUE = "VOC Volume Other Value";
    
    // Memcached Server (host:port) and Client enabled
    private static final String IS_MEMCACHED = "isMemcached";

    private static PluginSettingsFactory pluginSettingsFactory;
    
	public ConfigurationManager(PluginSettingsFactory pluginSettingsFactory) {
        ConfigurationManager.pluginSettingsFactory = pluginSettingsFactory;
    }

    public String getSMTPServer() {
        return getValue(SMTP_SERVER);
    }
    
    public String getIsVisible(){
    	return getValue(IS_VISIBLE);
    }
    
    public String getIssuetypesJQL() {
    	return getValue(ISSUETYPES_JQL);
    }
    
    public String getJQL() {
    	return getValue(JQL);
    }
    
    public String getHighGuidance() {
    	return getValue(HIGH_GUIDANCE);
    }
    
    public String getMedGuidance() {
    	return getValue(MED_GUIDANCE);
    }
    
    public String getLowGuidance() {
    	return getValue(LOW_GUIDANCE);
    }
    
    public String getMemcachedServerHost() {
    	return getValue(MEMCACHED_SERVER_HOST);
    }
    
    public String getMemcachedServerPort() {
    	return getValue(MEMCACHED_SERVER_PORT);
    }
    
    public String getVOCRequest() {
    	return getValue(VOC_REQUEST);
    }
    
    public String getSupportRequest() {
    	return getValue(SUPPORT_REQUEST);
    }
    
    public String getCreateSeverity() {
    	return getValue(CREATE_SEVERITY);
    }
    
    public String getIsMemcached(){
    	return getValue(IS_MEMCACHED);
    }
    
    public Map<String, Integer> getVOCCustomFields() {
    	Map<String, Integer> volFieldsMap = new HashMap<String, Integer>();
    	volFieldsMap.put(USERVOICE,0);
    	volFieldsMap.put(SUPPORTTRACKS,0);
    	volFieldsMap.put(STAND_UPS,0);
    	volFieldsMap.put(FEEDBACKFORUMS,0);
    	volFieldsMap.put(SALESFORCE,0);
    	volFieldsMap.put(VOC_VOLUME_OTHER_VALUE,0);
    	volFieldsMap.put(SUPPORT_TRACKS_WEEKLY_AVERAGE,0);
    	return volFieldsMap;
    }
    
    public Map<String, Integer> getWeightedPriorityMap() {
    	Map<String, Integer> weightedPriorityMap = new HashMap<String, Integer>();
    	weightedPriorityMap.put("blocker+Unusable", 1);
    	weightedPriorityMap.put("blocker+Painful", 1);
    	weightedPriorityMap.put("blocker+Annoying", 1);
    	weightedPriorityMap.put("blocker+Polish", 1);
    	weightedPriorityMap.put("blocker+Enhancement", 5);
    	weightedPriorityMap.put("Critical+Unusable", 2);
    	weightedPriorityMap.put("Critical+Painful", 2);
    	weightedPriorityMap.put("Critical+Annoying", 3);
    	weightedPriorityMap.put("Critical+Polish", 4);
    	weightedPriorityMap.put("Critical+Enhancement", 5);
    	weightedPriorityMap.put("High+Unusable", 2);
    	weightedPriorityMap.put("High+Painful", 3);
    	weightedPriorityMap.put("High+Annoying", 3);
    	weightedPriorityMap.put("High+Polish", 4);
    	weightedPriorityMap.put("High+Enhancement", 5);
    	weightedPriorityMap.put("Medium+Unusable", 4);
    	weightedPriorityMap.put("Medium+Painful", 4);
    	weightedPriorityMap.put("Medium+Annoying", 4);
    	weightedPriorityMap.put("Medium+Polish", 4);
    	weightedPriorityMap.put("Medium+Enhancement", 5);
    	weightedPriorityMap.put("Low+Unusable", 4);
    	weightedPriorityMap.put("Low+Painful", 4);
    	weightedPriorityMap.put("Low+Annoying", 4);
    	weightedPriorityMap.put("Low+Polish", 4);
    	weightedPriorityMap.put("Low+Enhancement", 5);
    	return weightedPriorityMap;
    }
    
    public Map<String, String> getWeightedPriorityColorMap() {
    	Map<String, String> weightedPriorityColorMap = new HashMap<String, String>();
    	weightedPriorityColorMap.put("1", "maroon");
    	weightedPriorityColorMap.put("2", "orange");
    	weightedPriorityColorMap.put("3", "gold");
    	weightedPriorityColorMap.put("4", "green");
    	weightedPriorityColorMap.put("5", "darkgreen");
    	return weightedPriorityColorMap;
    }
    
    public String getSeverityOptionsString(CustomField customField) {
		if(!SelectSeverityField.isSeverityOptions()) {
        	System.out.println("ConfigurationManager: Severity field exists without options, calling setOptions");
        	SelectSeverityField.setOptions(customField);
        }
    	return SelectSeverityField.getOptionsString(customField);
    }
       
    private String getValue(String storageKey) {
        PluginSettings settings = pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY);
        Object storedValue = settings.get(storageKey);
        if(storageKey == "smtp-server") {
        	return storedValue == null || storedValue == "" ? SMTP_SERVER : storedValue.toString();
        }
        return storedValue == null ? "" : storedValue.toString();
    }

    public void updateConfiguration(String smtpServer, String issuetypesJQL, String jql, String isVisible, 
    		String createSeverity, String highGuidance, String medGuidance, String lowGuidance, 
    		String isMemcached, String memcachedServerHost, String memcachedServerPort) {
        PluginSettings settings = pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY);
        settings.put(SMTP_SERVER, smtpServer);
        settings.put(ISSUETYPES_JQL, issuetypesJQL);
        settings.put(JQL, jql);
        
        try {
	        if(isVisible.contains("yes")) {
	        	settings.put(IS_VISIBLE, isVisible);
	        } else {
	        	settings.put(IS_VISIBLE, "no");
	        }
        } catch(NullPointerException e) {
        	settings.put(IS_VISIBLE, "no");
        }
        
        try {
	        if(isMemcached.contains("yes")) {
	        	settings.put(IS_MEMCACHED, isMemcached);
	        } else {
	        	settings.put(IS_MEMCACHED, "no");
	        }
        } catch(NullPointerException e) {
        	settings.put(IS_MEMCACHED, "no");
        }
        
        /*
        if(!GenericNumberField.isTextField(USERVOICE)) {
        	try {
				GenericNumberField.createTextField(USERVOICE,"");
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
        }
        */
        
        /*
        if(!GenericTextField.isTextField(USERVOICE_LABEL)) {
        	try {
				GenericTextField.createTextField(USERVOICE_LABEL,"");
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}
        }
        */
        
        try {
        	if(createSeverity != null && createSeverity.contains("yes")) {
	        	settings.put(CREATE_SEVERITY, createSeverity);
	        	if(!SelectSeverityField.isSeverity()) {
	        		System.out.println("calling createSeverity from servlet");
	        		SelectSeverityField.createSeverity();
	        		getSeverityOptionsString(SelectSeverityField.getSeverity());
	        	}
	        } 
	        else {
	        	settings.put(CREATE_SEVERITY, "no");
	        } 
        } catch(NullPointerException e) {
        	e.printStackTrace();
        } finally {
        	settings.put(CREATE_SEVERITY, createSeverity);
        }
        

        settings.put(HIGH_GUIDANCE, highGuidance);
        settings.put(MED_GUIDANCE, medGuidance);
        settings.put(LOW_GUIDANCE, lowGuidance);
        settings.put(MEMCACHED_SERVER_HOST, memcachedServerHost);
        settings.put(MEMCACHED_SERVER_PORT, memcachedServerPort);
    }
    
}
