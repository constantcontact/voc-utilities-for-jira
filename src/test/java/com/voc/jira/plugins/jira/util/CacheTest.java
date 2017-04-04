package com.voc.jira.plugins.jira.util;

import static junit.framework.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import net.spy.memcached.MemcachedClient;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith (MockitoJUnitRunner.class)
public class CacheTest {
	@Test
	public void test_NoConnection_StillReturnValue() throws Exception {
		given_ConnectionAvailable(false);
		given_SlowCalculationIsNormal();
		given_TransformationOfSlowCalculationResultIsNormal();
		when_CacheGet();
		then_ReturnValueSuccessfully();
	}
	@Test
	public void test_ClientSetThrows_StillReturnValue() throws Exception {
		given_CacheClientThrowsOnSet(new NullPointerException(MUMBLE));
		given_ConnectionAvailable(true);
		given_CacheKeyIsNormal();
		given_CallingCodeWantsToCalculateTheLatest(true);
		given_SlowCalculationIsNormal();
		given_TransformationOfSlowCalculationResultIsNormal();
		when_CacheGet();
		then_ReturnValueSuccessfully();
	}
	@Test
	public void test_ClientGetThrows_StillReturnValue() throws Exception {
		given_ConnectionAvailable(true);
		given_CacheKeyIsNormal();
		given_CallingCodeWantsToCalculateTheLatest(false);
		given_SlowCalculationIsNormal();
		given_TransformationOfSlowCalculationResultIsNormal();
		given_CacheClientThrowsOnGet(new NullPointerException(MUMBLE));
		when_CacheGet();
		then_ReturnValueSuccessfully();
	}
	@Test
	public void test_ExceptionOnSlowCalculation_NoConnection_ExceptionThrown() throws Exception {
		given_ConnectionAvailable(false);
		given_SlowCalculationThrows(new IndexOutOfBoundsException(MUMBLE));
		given_TransformationOfSlowCalculationResultIsNormal();
		given_NextLineWillThrow();
		when_CacheGet();
		then_ExceptionThrown();
	}
	@Test
	public void test_ExceptionOnSlowCalculation_Connection_ExceptionThrown() throws Exception {
		given_ConnectionAvailable(true);
		given_CacheKeyIsNormal();
		given_CallingCodeWantsToCalculateTheLatest(true);
		given_SlowCalculationThrows(new NullPointerException(MUMBLE));
		given_TransformationOfSlowCalculationResultIsNormal();
		given_NextLineWillThrow();
		when_CacheGet();
		then_ExceptionThrown();
	}
	@Before
	public void initialize() {
		result = WRONG;
		thrown = null;
		connectionAvailable = true;
	}
	////////////////////////////////////////////////////////
	// given when then helper methods
	////////////////////////////////////////////////////////
	private void given_CacheClientThrowsOnSet(Throwable t) {
		given(memcachedClient.set(anyString(), anyInt(), anyObject())).willThrow(t);
	}
	private void given_CacheClientThrowsOnGet(Throwable t) {
		given(memcachedClient.asyncGet(anyString())).willThrow(t);
	}
	private void given_CallingCodeWantsToCalculateTheLatest(boolean whether) {
		given(request.getLatest()).willReturn(whether);
	}
	private void given_CacheKeyIsNormal() {
		given(request.key()).willReturn(MUMBLE);
	}
	private void given_TransformationOfSlowCalculationResultIsNormal() {
		given(request.transform(anyObject())).willReturn(BINGO);
	}
	private void given_SlowCalculationIsNormal() {
		given(request.get()).willReturn(BINGO);
	}
	private void given_SlowCalculationThrows(Throwable t) {
		given(request.get()).willThrow(t);
		thrown = t;
	}
	private void given_NextLineWillThrow() {
		exception.expect(thrown.getClass());
	}
	private void given_ConnectionAvailable(boolean whether) {
		connectionAvailable = whether;
	}
	private void when_CacheGet() {
		if (connectionAvailable) {
			result = (String)Cache.get(request, memcachedClient);
		} else {
			result = (String)Cache.get(request, null);
		}
	}
	private void then_ExceptionThrown() {
		// nothing to do here since exception.expect() took care of it
	}
	private void then_ReturnValueSuccessfully() {
		assertEquals(BINGO, result);
	}
	////////////////////////////////////////////////////////////////////////
	// vars -- assumption: you'll run JUnit tests serially as is the default
	////////////////////////////////////////////////////////////////////////
	/**
	 * For verifying an expected exception
	 */
	@Rule
	public ExpectedException exception = ExpectedException.none();
	@Mock
	private MemcachedClient memcachedClient;
	@Mock
	private ICacheRequest request;
	/**
	 * Default constant to use for the correct result. 
	 */
	private final String BINGO = "BINGO";
	/**
	 * Default constant to use for the wrong result. 
	 */
	private final String WRONG = "wrong";
	/**
	 * Remind us which stack traces in the build output can be ignored.
	 */
	private final String MUMBLE = "just testing. please ignore.";
	/**
	 * The result value returned by the cache during a test
	 */
	private String result = WRONG;
	/**
	 * For storing any thrown exception during a test.
	 */
	private Throwable thrown;
	/**
	 * Whether a valid connection is returned by the cache pool.
	 */
	private boolean connectionAvailable = true;
}
