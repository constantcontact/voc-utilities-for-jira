package com.voc.jira.plugins.jira.workflow;

import java.util.Collections;
import java.util.Map;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginValidatorFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;

public class PriorityNotNoneValidatorFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginValidatorFactory {

  public PriorityNotNoneValidatorFactory() {
  }

  @Override
  protected void getVelocityParamsForEdit(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
    //velocityParams.put(FIELD_NAME, getFieldName(descriptor));
    //velocityParams.put(FIELDS, getCFFields());
  }

  @Override
  protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {
    //velocityParams.put(FIELDS, getCFFields());
  }

  @Override
  protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
    //velocityParams.put(FIELD_NAME, getFieldName(descriptor));
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getDescriptorParams(Map<String, Object> conditionParams) {
    return Collections.EMPTY_MAP;
  }

}
