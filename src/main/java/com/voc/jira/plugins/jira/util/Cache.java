package com.voc.jira.plugins.jira.util;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.MemcachedClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cache {
	private static final Logger log = LoggerFactory.getLogger(Cache.class);
	private static final String LOG_PREFIX = "+++++++++++++ Memcached ++++++";

	private static void logExceptionAndEatIt(Exception e,ICacheRequest r) {
		e.printStackTrace();
		final String msg = String.format("%s exception [%s] key [%s]", LOG_PREFIX, e.getMessage(), r.key());
		log.error(msg);
	}
	
	public static Object get(ICacheRequest r) {
		MemcachedClient c = CachePool.getInstance(r.host()).getClient();
		return get(r,c);
	}
	
	static Object get(ICacheRequest r, MemcachedClient c) {
		if (null == c) {
			return r.transform(r.get());
		}
		final String key = r.key().replaceAll(" ", "_");
		try {
			if (r.getLatest()) {
				throw new Exception();
			}
			Future<Object> future = c.asyncGet(key);
			Object result = future.get(1, TimeUnit.SECONDS);
			if (null == result) {
				throw new Exception();
			}
			return result;
		} catch (Exception e) {
			Object val = r.transform(r.get());
			try {
				c.set(key, getTtl(r), val);
			} catch (Exception e1) {
				logExceptionAndEatIt(e1,r);
			}
			return val;
		}
	}
	
	private static int getTtl(ICacheRequest r) {
		return r.ttlSecs() > 0 ? r.ttlSecs() : Time.secondsRemainingTillMidnight();
	}
}
