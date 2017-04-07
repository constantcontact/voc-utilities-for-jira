package com.voc.jira.plugins.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.voc.jira.plugins.jira.customfield.SelectSeverityField;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.InvalidInputException;

import java.util.ArrayList;
import java.util.Map;

public class SeverityNotNoneValidator implements Validator
{
    private static final Logger log = LoggerFactory.getLogger(SeverityNotNoneValidator.class);
	//private static SelectSeverityField<?,? > selectSeverityField;
    private final CustomFieldManager customFieldManager;

    public static final String SEVERITY_DEFAULT="None";
    public static final String SEVERITY=SelectSeverityField.getSeverityFieldName();
    public static final String ISSUE="issue";
    private ArrayList<String> defectTypes = new ArrayList<String>();

    public SeverityNotNoneValidator(CustomFieldManager customFieldManager){
      this.customFieldManager = customFieldManager;
      defectTypes.add("Bug");
      defectTypes.add("Defect");
    }

    @SuppressWarnings("rawtypes")
	public void validate(Map transientVars, Map args, PropertySet ps) throws InvalidInputException {
      Issue issue = (Issue) transientVars.get(ISSUE);
      CustomField customField = customFieldManager.getCustomFieldObjectByName(SEVERITY);
      String errorMsg = SEVERITY + " must be changed from the default value '"
          + SEVERITY_DEFAULT + "' for all 'Defect' issues.";
      if(defectTypes.contains(issue.getIssueTypeObject().getName())){
	    log.info(issue.getIssueTypeObject().getName()
	        + " IssueType is one of the known defect issue types collection.");
	    if (customField == null) {  
	        throw new IllegalArgumentException("customField \"" + SEVERITY + "\" (case sensitive) not found");
	    }
	    InvalidInputException ex = new InvalidInputException();
		if(null == issue.getCustomFieldValue(customField)) {
		  ex.addError(errorMsg);
		  ex.addError(SEVERITY,errorMsg);
		  if ( ex.getErrors() != null ) {
			throw ex;
		  }
		}
	  }
    }
}
