package com.voc.jira.plugins.jira.servlet;

import java.util.Map;

public interface IErrorKeeper {
	void insertErrorMessage(Map<String, Object> context, String message);
}
