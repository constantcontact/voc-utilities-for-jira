package com.voc.jira.plugins.jira.util;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.voc.jira.plugins.jira.servlet.IErrorKeeper;

public class RequestError {

	private static final String FORMAT = "<br><div class=\"aui-message error\"><span class=\"aui-icon icon-error\"></span><span style=\"font-size:13px\">%s</span></div>";
	private static final String ERROR = "error happened";
	private static final String ERROR_MSG = "error";
	
	public static void set(Map<String, Object> context, String msg) {
		context.put(ERROR, true);
	}

	public static boolean happened(Map<String, Object> context) {
    	return context.containsKey(ERROR);
    }

	public static String format(String s) {
    	return String.format(FORMAT, s);
    }

	public static void insertErrorMessage(Map<String, Object> context, String key, String message) {
		String msg = format(message);
		context.put(key, context.get(key) + msg);
		set(context,message);
	}

	public static void checkForErrorMessage(HttpServletRequest req,
			Map<String, Object> context, IErrorKeeper keeper) {
		if (req.getParameter(ERROR_MSG) != null) {
			keeper.insertErrorMessage(context, req.getParameter(ERROR_MSG));
		}
	}	
}
