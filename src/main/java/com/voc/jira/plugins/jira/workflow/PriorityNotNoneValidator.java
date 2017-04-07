package com.voc.jira.plugins.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.Issue;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.InvalidInputException;

import java.util.ArrayList;
import java.util.Map;

public class PriorityNotNoneValidator implements Validator
{
    private static final Logger log = LoggerFactory.getLogger(PriorityNotNoneValidator.class);

    public static final String PRIORITY_DEFAULT="None";
    public static final String PRIORITY="priority";
    public static final String ISSUE="issue";
    private ArrayList<String> defectTypes = new ArrayList<String>();

    public PriorityNotNoneValidator(){
      defectTypes.add("Bug");
      defectTypes.add("Defect");
    }

    @SuppressWarnings({ "rawtypes" })
	public void validate(Map transientVars, Map args, PropertySet ps) throws InvalidInputException {
      Issue issue = (Issue) transientVars.get(ISSUE);
      String errorMsg = "Priority must be changed from the default value '"
          + PRIORITY_DEFAULT + "' for all 'Defect' issues.";
      if(defectTypes.contains(issue.getIssueTypeObject().getName())){
	    log.info(issue.getIssueTypeObject().getName()
	        + " IssueType is one of the known defect issue types collection.");
	    InvalidInputException ex = new InvalidInputException();
	   	    
	    if(null ==  issue.getPriorityObject().getGenericValue() || PRIORITY_DEFAULT.equals( issue.getPriorityObject().getGenericValue().getString("name"))) {
	      ex.addError(errorMsg);
	      ex.addError(PRIORITY,errorMsg);
		  if ( ex.getErrors() != null ) {
			throw ex;
		  }
	    }

	  }
    }
}
