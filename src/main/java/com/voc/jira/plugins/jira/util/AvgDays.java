package com.voc.jira.plugins.jira.util;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Days;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.user.ApplicationUser;
import com.voc.jira.plugins.jira.components.ConfigurationManager;
import com.voc.jira.plugins.jira.servlet.IErrorKeeper;

public class AvgDays extends IssueCount implements ICacheRequest, Serializable {
	private static final long serialVersionUID = -4079430512400349807L;

	public AvgDays(String jql, Map<String, Object> context,
			final SearchService searchService, final ApplicationUser user, IErrorKeeper err,
			final String baseUrl,final String keyBase, ConfigurationManager configMgr) {
		super(jql,context,searchService,user,err,baseUrl,keyBase,configMgr);
	}
	
	@Override
	public Object transform(Object getResult) {
		AvgDaysData d = new AvgDaysData();
		d.issueCount = ((SearchResults) getResult).getTotal();
		d.averageDays = getAvgDays((SearchResults) getResult);
		return d;
	}

	@Override
	public String key() {
		return String.format("%s:%s:AvgDays",host,keyBase);
	}

	private String getAvgDays(SearchResults r) {
		if (r.getTotal()==0) {
			return "-";
		}
    	int totalDays = 0;
    	int count = 0;
    	for (Issue i : r.getIssues()) {
    		Date created = i.getCreated();
    		Date resolved = i.getResolutionDate();
    		if (null==resolved) {
    			err.insertErrorMessage(context,"Encountered a resolved ticket without a resolution date.");
    		} else {
    		    totalDays += Days.daysBetween(new DateTime(created.getTime()),new DateTime(resolved.getTime())).getDays();
    		    count++;
    		}
    	}
    	return count==0 ? "-" : ""+Math.round((double)totalDays/(double)count);
	}
}
