package com.voc.jira.plugins.jira.util;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.util.Collection;
import java.util.Collections;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachePool {
	private static final Logger log = LoggerFactory.getLogger(CachePool.class);
	private static final String LOG_PREFIX = "++++ Caught Memcached Exception ++++ ";
	private static final int POOL_SIZE = 20;
	private static int ACTUAL_POOL_SIZE;
	private static MemcachedClient[] m = null;
	private static CachePool instance = null;
	
	private static void err(Exception e) {
		e.printStackTrace();
		log.error(LOG_PREFIX + e.getMessage());
	}
	
	private CachePool(final String baseUrl, final String mport) {
		final String host = Url.getHost(baseUrl);
		final String mhost = getHost(baseUrl, host);
		System.out.println("[MEMCACHED INFO] CachePool() baseUrl == " + baseUrl + ", mhost == " + mhost);
		System.out.println("[MEMCACHED INFO] CachePool() port == " + mport);
		
		m = new MemcachedClient[POOL_SIZE];
		for (int j = 0; j < POOL_SIZE; j++) {
			m[j] = null;
		}
		ACTUAL_POOL_SIZE = 0;
		for (int i = 0; i < POOL_SIZE; i++) {
			try {  //http://programtalk.com/java-api-usage-examples/net.spy.memcached.ConnectionObserver/
				final ConnectionObserver obs = new ConnectionObserver() {
					public void connectionEstablished(SocketAddress sa, int reconnectCount) {
			            System.out.println("*** Memcached Connection Established:  " + sa + " count=" + reconnectCount);
			        }
			        public void connectionLost(SocketAddress sa) {
			            System.out.println("*** Memcached Connection Lost:  " + sa);
			        }
				};
				m[i] = new MemcachedClient(new DefaultConnectionFactory() {
					@Override
			        public Collection<ConnectionObserver> getInitialObservers() {
			            return Collections.singleton(obs);
			        }
			        @Override
			        public boolean isDaemon() {
			            return true;
			        }
			        @Override
			        public FailureMode getFailureMode() {
			        	return FailureMode.Cancel;  //attempting to prevent retries when memcached config wrong
			        }
				}, AddrUtil.getAddresses( String.format("%s:%s", mhost, Integer.parseInt(mport)) ) );				
				ACTUAL_POOL_SIZE++;
			} catch (SecurityException e) {
				err(e);
				break;
			} catch (UnresolvedAddressException e) {
				if(e.getMessage() == null) {
					log.error(String.format("ERROR: Memcached Server Host \'%s\' not reachable (%s).", mhost, e.getMessage()));
				} else {
					err(e);
				}
				break;
			} catch (ConnectException e) {
				log.error(String.format("ERROR: caught ConnectException in CachePool (%s)", e.getMessage()));
				err(e);
				break;
			} catch (IOException e) {
				log.error(String.format("ERROR: caught IOException in CachePool (%s)", e.getMessage()));
				err(e);
				break;
			} catch (Exception e) {
				err(e);
				break;
			}
		}
	}

	private static String getHost(final String baseUrl, final String host) {
		if(baseUrl.contains("roving.com")){
			return "jira".equalsIgnoreCase(host)
				? "10.20.97.188" // production jira
				: "10.20.97.189"; // everyone else
		} else {
			return host;
		}
	}

	public static synchronized CachePool getInstance(final String baseUrl, final String mport) {
		try {
			if (instance == null) {
				instance = new CachePool(baseUrl,mport);
			}
			return instance;
		} catch (Exception e) {
			err(e);
		}
		return null;
	}

	int next = 0;
	public synchronized MemcachedClient getClient() {
		MemcachedClient c = m[next];
		next++;
		if (next >= ACTUAL_POOL_SIZE) {
			next = 0;
		}
		return c;
	}
	
	public void shutdownClient() {
		int next = 0;
		next ++;
		if (next >= ACTUAL_POOL_SIZE) {
			next = 0;
		}
		m[0].shutdown();
	}
}
