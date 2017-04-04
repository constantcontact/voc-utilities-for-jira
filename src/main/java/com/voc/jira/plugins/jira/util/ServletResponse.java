package com.voc.jira.plugins.jira.util;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.atlassian.jira.plugin.webresource.JiraWebResourceManager;
import com.atlassian.plugin.webresource.UrlMode;
import com.google.common.collect.Maps;

public class ServletResponse {
	private static final String RESOURCES = "com.voc.jira.plugins.VOC-Utilities:charts-resources";
	private static final String CONTENT_TYPE = "text/html;charset=utf-8";

	@SuppressWarnings("deprecation")
	public static Map<String, Object> setup(HttpServletResponse resp, JiraWebResourceManager webResourceManager) throws IOException {
		webResourceManager.requireResourcesForContext(RESOURCES);
		webResourceManager.includeResources(resp.getWriter(), UrlMode.AUTO);
		resp.setContentType(CONTENT_TYPE);
		return Maps.newHashMap();
	}
}
