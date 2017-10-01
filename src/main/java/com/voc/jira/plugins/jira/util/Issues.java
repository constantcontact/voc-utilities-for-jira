package com.voc.jira.plugins.jira.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchResults;
import com.voc.jira.plugins.jira.components.ConfigurationManager;
import com.voc.jira.plugins.jira.servlet.IErrorKeeper;

public class Issues extends JqlCacheRequest implements ICacheRequest {
	
	public Issues(String jql, Map<String, Object> context, final SearchService searchService,
			final User user, IErrorKeeper err, final String baseUrl, String keyBase, ConfigurationManager configMgr) {
		super(jql,context,searchService,user,err,baseUrl,keyBase, configMgr);
	}

	@Override
	public Object transform(Object getResult) {
		List<Issue> issues = ((SearchResults) getResult).getIssues();
		List<Long> ids = new ArrayList<Long>();
		for (Issue i : issues) {
			ids.add(i.getId());
		}
		HashSet<Long> set = new HashSet<Long>(ids);
		return set;
	}

	@Override
	public String key() {
		return String.format("%s:%s:ids",host,keyBase);
	}
}
