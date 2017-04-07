package com.voc.jira.plugins.jira.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Url {
	private static final Pattern hostPattern = Pattern.compile("//([a-zA-Z0-9-]+)");
	
	public static String getHost(final String url) {
		if (url == null) {
			return "nullhost";
		}
		if ("".equals(url)) {
			return "emptyhost";
		}
		Matcher m = hostPattern.matcher(url);
		if (m.find()) {
			return m.group(1);
		}
		return url;
	}
}
