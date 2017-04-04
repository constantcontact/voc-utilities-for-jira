package com.voc.jira.plugins.jira.servlet;

public class IntegerValidator implements IValidator {

	private final String msg;
	private final int min;
	private final int max;
	
	public IntegerValidator(int min, int max, String msg) {
		this.msg = String.format("%s must be an integer between %s and %s", msg, min, max);
		this.min = min;
		this.max = max;
	}
	@Override
	public String message() {
		return msg;
	}

	@Override
	public boolean isValid(String value) {
		try {
			int val = Integer.parseInt(value);
			return min <= val && val <= max;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
}
