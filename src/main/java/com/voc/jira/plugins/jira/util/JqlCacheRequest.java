package com.voc.jira.plugins.jira.util;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.voc.jira.plugins.jira.components.ConfigurationManager;
import com.voc.jira.plugins.jira.servlet.IErrorKeeper;

public class JqlCacheRequest implements ICacheRequest {

	final String jql;
	final String keyBase;
	final Map<String, Object> context;
	final SearchService searchService;
	final User user;
	final IErrorKeeper err;
	final String host;
	final String memcachedPort;
	final String isMemcached;
	final ConfigurationManager configMgr;
	private static final Logger log = LoggerFactory.getLogger(JqlCacheRequest.class);
    
	public JqlCacheRequest(String jql, Map<String, Object> context,
			final SearchService searchService, final User user, IErrorKeeper err,
			final String baseUrl,final String keyBase, ConfigurationManager configMgr) {
		this.jql = jql;
		this.keyBase = keyBase;
		this.context = context;
		this.searchService = searchService;
		this.user = user;
		this.err = err;
		this.configMgr = configMgr;
		this.host = configMgr.getMemcachedServerHost();   // this.host = Url.getHost(baseUrl);
		this.memcachedPort = configMgr.getMemcachedServerPort();
		this.isMemcached = configMgr.getIsMemcached();
	}

	@Override
	public Object get() {
		log.info("running jql:"+jql);
		return Jql.getSearchResults(jql, context, searchService, user, err);
	}

	@Override
	public Object transform(Object getResult) {
		return getResult;
	}

	@Override
	public String key() {
		return String.format("%s:%s",host,keyBase);
	}

	@Override
	public int ttlSecs() {
		return -1;
	}

	@Override
	public boolean getLatest() {
		return Param.refresh(context);
	}

	@Override
	public String host() {
		return this.host;
	}
	
	@Override
	public String port() {
		return this.memcachedPort;
	}
	
	@Override
	public boolean isMemcached() {
		return this.isMemcached.toLowerCase().contains("yes");
	}
}
