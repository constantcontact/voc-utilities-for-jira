package com.voc.jira.plugins.jira.servlet;

public class NoValidation implements IValidator {

	@Override
	public String message() {
		return "";
	}

	@Override
	public boolean isValid(String value) {
		return true;
	}

}
