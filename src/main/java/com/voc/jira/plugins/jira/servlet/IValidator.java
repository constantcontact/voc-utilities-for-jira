package com.voc.jira.plugins.jira.servlet;

public interface IValidator {
	String message();
	boolean isValid(String value);
}
