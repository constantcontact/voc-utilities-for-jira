package com.voc.jira.plugins.jira.util;

import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.search.SearchResults;
import com.voc.jira.plugins.jira.servlet.IErrorKeeper;

public class IssueCount extends JqlCacheRequest implements ICacheRequest {

	public IssueCount(String jql, Map<String, Object> context,
			final SearchService searchService, final User user, IErrorKeeper err, final String baseUrl, final String keyBase) {
		super(jql,context,searchService,user,err,baseUrl,keyBase);
	}

	@Override
	public Object transform(Object getResult) {
		return ((SearchResults) getResult).getTotal();
	}

	@Override
	public String key() {
		return String.format("%s:%s:count",host,keyBase);
	}
}
