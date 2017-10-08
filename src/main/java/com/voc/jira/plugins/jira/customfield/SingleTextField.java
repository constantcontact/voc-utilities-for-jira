package com.voc.jira.plugins.jira.customfield;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.ofbiz.core.entity.GenericEntityException;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.issue.context.GlobalIssueContext;
import com.atlassian.jira.issue.context.IssueContext;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.CustomFieldSearcher;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.AbstractSingleFieldType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.sal.api.message.I18nResolver;

public abstract class SingleTextField<T, S> extends AbstractSingleFieldType<Object>{

	//TODO: pull the SEVERITY_FIELD_NAME value from i18n.VOCUtilities.properites file, key: customfield-severity.name
	//Consider making these methods not static, such that the Constructor assigns a private variable
	private final I18nResolver i18n;
	static final String SEVERITY_FIELD_NAME = "VOC Severity"; //pulling from customfield-severity.name doesn't work here
	static final String SEVERITY_FIELD_DESCRIPTION = "Customer Impact";
	static final String SEVERITY_FIELD_OPTIONS = "Unusable,Painful,Annoying,Polish,Enhancement";
	static final String SELECT_CUSTOM_FIELD_TYPE = "com.atlassian.jira.plugin.system.customfieldtypes:select";
	static final String SELECT_SEARCHER_TYPE = "com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher";
	
	static String severityFieldName = SEVERITY_FIELD_NAME;
	static CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager(); 
	private static ConstantsManager constantsManager = ComponentAccessor.getConstantsManager();
	static Collection<IssueType> allIssueTypeObjects = constantsManager.getAllIssueTypeObjects();
	
	private static OptionsManager optionsManager = ComponentAccessor.getOptionsManager();

	static FieldConfig selectSeverityConfig = null;
	static CustomField severityField = null;
	static CustomFieldSearcher fieldTypeSearcher = customFieldManager.getCustomFieldSearcher(SELECT_SEARCHER_TYPE);
	static CustomFieldType<?,?> fieldType = customFieldManager.getCustomFieldType(SELECT_CUSTOM_FIELD_TYPE);

	protected SingleTextField(CustomFieldValuePersister customFieldValuePersister,
			GenericConfigManager genericConfigManager, I18nResolver i18n) {
		super(customFieldValuePersister, genericConfigManager);
		this.i18n = i18n;
		System.out.println("customfield-severity.name == " + this.i18n.getText("customfield-severity.name"));
		//this.severityFieldName = i18n.getText("customfield-severity.name");
		//this.severityFieldName = getPropertyValue("customfield-severity.name");
		SingleTextField.severityFieldName = "VOC Severity";
		System.out.println("customfield-severity.name == " + SingleTextField.severityFieldName);
		if(ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(SEVERITY_FIELD_NAME)
				.getRelevantConfig(IssueContext.GLOBAL) != null) {
			selectSeverityConfig = ComponentAccessor.getCustomFieldManager()
				.getCustomFieldObjectByName(SEVERITY_FIELD_NAME)
				.getRelevantConfig(IssueContext.GLOBAL);
		}
		if(customFieldManager.getCustomFieldObjectByName(SEVERITY_FIELD_NAME) != null) {
			severityField = customFieldManager.getCustomFieldObjectByName(SEVERITY_FIELD_NAME);
		}
	}
	
	public void loadSeverityFieldName() {
		SingleTextField.severityFieldName = i18n.getText("customfield-severity.name");
		System.out.println("SelectSeverityField.severityFieldName == " 
				+ SingleTextField.severityFieldName);
	}
	
	public static Boolean isSeverity() {
		if(severityField == null) {
			return false;
		}
		return true;
	}
	
	public static String getSeverityFieldName() {
		return severityFieldName;
	}
	
	public static CustomField getSeverity() {
		if(customFieldManager.getCustomFieldObjectByName(SEVERITY_FIELD_NAME) != null) {
			severityField = customFieldManager.getCustomFieldObjectByName(SEVERITY_FIELD_NAME);
			return severityField;
		}
		return null;
	}
	
	public static Boolean isSeverityOptions() {
		if(getSeverity() != null && optionsManager.getOptions(selectSeverityConfig) != null) {
			return true;
		}
		return false;
	}
	
	public static CustomField createSeverity() {
		System.out.println("in createSeverity");
		if(getSeverity() != null) {
			System.out.println(SEVERITY_FIELD_NAME + " field already exists, exiting createSeverity without creating.");
			return getSeverity();
		}
		if(!isSeverity()) {
			System.out.println(SEVERITY_FIELD_NAME + " field does not yet exist");
			ArrayList<JiraContextNode> contexts = new ArrayList<JiraContextNode>();
	        contexts.add(GlobalIssueContext.getInstance());
	        List<IssueType> issueTypes = new ArrayList<IssueType>();
			for(IssueConstant currentIssueType : allIssueTypeObjects) {
			    if(currentIssueType.getName().equals("Defect") || currentIssueType.getName().equals("Bug")) {
			    	System.out.println("found issueType 'Defect' or 'Bug'");
			        issueTypes.add((IssueType) ((Option) currentIssueType).getGenericValue());
			    }
			}
	        
	        if(customFieldManager.getCustomFieldType(SELECT_CUSTOM_FIELD_TYPE) != null) {
	        	CustomFieldType<?, ?> selectFieldType = 
	        			customFieldManager.getCustomFieldType(SELECT_CUSTOM_FIELD_TYPE);
				try {
					System.out.println("Attempting to create custom field " + SEVERITY_FIELD_NAME);
					severityField = customFieldManager.createCustomField(
						SEVERITY_FIELD_NAME, 
						SEVERITY_FIELD_DESCRIPTION, 
						selectFieldType, 
						fieldTypeSearcher, 
						contexts, 
						issueTypes);
					if(severityField != null) { System.out.println("createSeverity() successful"); }
				} catch (GenericEntityException e) {
					e.printStackTrace();
				}
				return severityField;
	        }
		}
		return severityField; // return the existing severityField if already exists
	}
	
	public static void setOptions(CustomField customField) {
		System.out.println("In setOptions");
		List<String> listOptions = new ArrayList<String>(Arrays.asList(SEVERITY_FIELD_OPTIONS.split(",")));
		
		if(getOptionsString(customField) != "") {
			System.out.println(SEVERITY_FIELD_NAME + " field options already exist, exiting setOptions without adding new options.");
			return;
		}
		
		System.out.println(SEVERITY_FIELD_NAME + " Options in List: " + listOptions.toString());

			try {
				Iterator<String> itr = listOptions.iterator();
				int i = 0;
				while(itr.hasNext()) {
					System.out.println("in listOptions iterator of setOptions");
					String optionValue = itr.next();
					System.out.println("listOption: " + optionValue);
					addOptionToCustomField(getSeverity(),optionValue, i++);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	/**
	 * Adding a single option to the list of option in a "Select" Custom Field Type
	 * @param customField //the custom field object that the option is added to
	 * @param strOptionLabel //the list item label
	 * @param sequence //the ordered location of the list item
	 * @return //the Option object
	 */
	public static Option addOptionToCustomField(CustomField customField, String strOptionLabel, int sequence) {
		System.out.println("In addOptionToCustomField(" + 
				customField.getFieldName() + "," + 
				strOptionLabel + ")");
		
		Option newOption = null;
	    if (customField != null) {
	        Map<?,?> configs = getConfigs(customField);
            if (configs != null && !configs.isEmpty()) {
            	System.out.println("configs.size(): " + configs.size());
                FieldConfig config = (FieldConfig) configs.keySet().iterator().next();
                newOption = optionsManager.createOption(
	                		config, 			//FieldConfig object to add options into
	                		null, 				//(Long) parent option ID
	                		new Long(sequence), //numeric index position in list of options
	                		strOptionLabel		//(String) value added to the list of options
                		);
	        }
	    }
	    return newOption;
	}
	
	public static Map<?, ?> getConfigs(CustomField customField) {
		Map<?, ?> configs = new HashMap<Object, Object>();
		if (customField != null) {
	        List<FieldConfigScheme> schemes = customField.getConfigurationSchemes();
	        if (schemes != null && !schemes.isEmpty()) {
	            FieldConfigScheme sc = schemes.get(0);
	            configs = sc.getConfigsByConfig();
	        }
		}
		return configs;
	}
	
	public static String getOptionsString(CustomField customField) {
		String strOptions = "";
		Options options = null;
		if(customField != null) {
			Map<?,?> configs = getConfigs(customField);
            if (configs != null && !configs.isEmpty()) {
                FieldConfig config = (FieldConfig) configs.keySet().iterator().next(); 
                options = optionsManager.getOptions(config);
    			for (Iterator<Option> iterator = options.iterator(); 
    					iterator .hasNext();) {
    				Option option = iterator.next();
    				strOptions += option;
    				if (iterator.hasNext()) {
    					strOptions += ",";
    				}
    			}
	        }
		}
		System.out.println(SEVERITY_FIELD_NAME + " Options: " + strOptions);
		return strOptions.trim();
	}

	/*
	private static void setSelectSeverityConfig() {
		if(ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(SEVERITY_FIELD_NAME).getRelevantConfig(IssueContext.GLOBAL) != null) {
			selectSeverityConfig = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(SEVERITY_FIELD_NAME).getRelevantConfig(IssueContext.GLOBAL);
		}
	}
	*/
}
