package com.voc.jira.plugins.jira.util;

import java.util.ArrayList;
import java.util.Map;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.voc.jira.plugins.jira.components.ConfigurationManager;
import com.voc.jira.plugins.jira.servlet.IErrorKeeper;

public class Jql {
	public static boolean isQueryValid(final String query, final SearchService searchService, final ApplicationUser user) {
		return searchService.parseQuery(user, query).isValid();
	}
	
	public static SearchResults getSearchResults(String query, Map<String, Object> context, final SearchService searchService, final ApplicationUser user, IErrorKeeper err) {
    	if (!isQueryValid(query, searchService, user)) {
    		err.insertErrorMessage(context, "Had an issue executing this JQL query: " + query);
    		return new SearchResults(new ArrayList<Issue>(), 0, 0, 0);
    	}
    	try {
			return searchService.search(user, getQuery(query,searchService,user), PagerFilter.getUnlimitedFilter());
		} catch (SearchException e) {
    		err.insertErrorMessage(context, "Exception executing this JQL query: " + query + "<br>Exception:" + e.getMessage());
			return new SearchResults(new ArrayList<Issue>(), 0, 0, 0);
		}
	}

	public static int getSearchCount(String query,
			Map<String, Object> context, final SearchService searchService,
			final ApplicationUser user, IErrorKeeper err, String baseUrl, String keyBase, ConfigurationManager configMgr) {
		IssueCount c = new IssueCount(query, context, searchService, user, err, baseUrl, keyBase, configMgr);
		return (Integer)Cache.get(c);
	}

	public static Query getQuery(String text, final SearchService searchService, final ApplicationUser user) {
        SearchService.ParseResult parseResult = searchService.parseQuery(user, text);
        if(parseResult.isValid()) {
        	return parseResult.getQuery();
        }
        return JqlQueryBuilder.newBuilder().buildQuery();
    }
	
	public static String getQueryUrl(final String jql, final String baseUrl) {
		return baseUrl + "/secure/IssueNavigator.jspa?mode=hide&reset=true&jqlQuery=" + jql;
	}
}
