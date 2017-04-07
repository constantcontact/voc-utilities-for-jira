package com.voc.jira.plugins.jira.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.voc.jira.plugins.jira.servlet.IErrorKeeper;
import com.voc.jira.plugins.jira.servlet.IValidator;
import com.voc.jira.plugins.jira.servlet.NoValidation;

public class Param {

	private static final String REFRESH_URL = "refreshUrl";
	private static final NoValidation NO_VALIDATION = new NoValidation();
	public static final String HC = "hc";
	public static final String CUSTOM_JQL = "customJql";
	public static final String REFRESH = "latest";
	static OptionsManager optionsManager = ComponentAccessor.getOptionsManager();

	/**Make sure the parameter is in the response context, with a default value if it wasn't specified by the user.
	 * @param name of the parameter as it comes in and also as it should be in the response context
	 * @param defaultValue for the parameter if it is not specified by the user
	 * @param req the request from the user
	 * @param context map going back to the client
	 * @return value used
	 */
	public static String pass(String name, String defaultValue, HttpServletRequest req, Map<String, Object> context, IErrorKeeper err) {
		return pass(name, defaultValue, req, context, NO_VALIDATION,err);
	}
	
	/**Make sure the parameter is in the response context, with a default value if it wasn't specified by the user.
	 * @param name of the parameter as it comes in and also as it should be in the response context
	 * @param defaultValue for the parameter if it is not specified by the user
	 * @param req the request from the user
	 * @param context map going back to the client
	 * @return value used
	 */
	public static String pass(String name, String defaultValue, HttpServletRequest req, Map<String, Object> context,IValidator validator, IErrorKeeper err) {
		final String value = req.getParameter(name);
		final String actual = value == null || "".equals(value) ? defaultValue : value;
		if (validator.isValid(actual)) {
			context.put(name,actual);
			return actual;
		} else {
			err.insertErrorMessage(context, validator.message() + ": " + value);
			return defaultValue;
		}
	}
	
	public static String putRefreshUrl(HttpServletRequest req, Map<String, Object> context, IErrorKeeper err,final String baseUrl) {
		final String value = req.getQueryString();
		final String fixed = value == null || "".equals(value) ? "?latest=true" : "?"+value+"&latest=true";
		final String url = baseUrl + fixed;
		context.put(REFRESH_URL, url);
		return url;
	}

	public static String getCustomJql(HttpServletRequest req,Map<String, Object> context, IErrorKeeper keeper, SearchService searchService, User user) {
		String param = req.getParameter(CUSTOM_JQL);
		if (param == null) {
			context.put(CUSTOM_JQL, "");
			return "";
		}
		param = param.trim().replace("&#34;","\"");
		String tmp = param.toUpperCase();
		if ("".equals(param)) {
			context.put(CUSTOM_JQL, "");
			return "";
		}
		String toTest = param;
		if (!tmp.startsWith("AND ") && !tmp.startsWith("OR ") && !tmp.startsWith("ORDER BY ")) {
			param = "AND " + param;
		} else {
			if (tmp.startsWith("AND")) {
				toTest = param.substring(3).trim();
			} else if (tmp.startsWith("ORDER BY")){
				toTest = param.substring(8).trim();
			} else {
				toTest = param.substring(2).trim();
			}
		}
		try {
			toTest = URLDecoder.decode(toTest,"UTF-8");
			param  = URLDecoder.decode(param, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			keeper.insertErrorMessage(context, "URL decode failed for JQL query clause: [" + param +"]");
		}
		if (!Jql.isQueryValid(toTest, searchService, user)) {
			keeper.insertErrorMessage(context, "Invalid JQL query clause: " + toTest);
		}
		context.put(CUSTOM_JQL, param);
		return param;
	}

	public static boolean useHardCodedData(Map<String, Object> context) {
		//return "true".equals(((Option)context.get(HC)).toString());
		return "true".equals(context.get(HC).toString());
	}
		
	public static String getHC(Map<String, Object> context,HttpServletRequest req, IErrorKeeper keeper) {
		return pass(HC,"false",req,context,keeper);
	}
	public static boolean refresh(Map<String, Object> context) {
		//return "true".equals(((Option)context.get(REFRESH)).toString());
		return "true".equals(context.get(REFRESH).toString());
	}
	public static String getLatest(Map<String, Object> context,HttpServletRequest req, IErrorKeeper keeper) {
		return pass(REFRESH,"false",req,context,keeper);
	}
}
