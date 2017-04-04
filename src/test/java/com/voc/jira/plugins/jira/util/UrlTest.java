package com.voc.jira.plugins.jira.util;

import org.junit.Assert;
import org.junit.Test;

public class UrlTest {
	@Test
	public void testGetHost() {
		Assert.assertEquals("host", "answer",  Url.getHost("http://answer.roving.com:0000/blah/blah"));
		Assert.assertEquals("host", "answer1", Url.getHost("https://answer1.roving.com:0000/blah/blah"));
		Assert.assertEquals("host", "answer2", Url.getHost("http://answer2.roving.com/blah/blah"));
		Assert.assertEquals("host", "answer3", Url.getHost("http://answer3.roving.com"));
		Assert.assertEquals("host", "answer4", Url.getHost("http://answer4:0000/blah/blah"));
		Assert.assertEquals("host", "answer5", Url.getHost("http://answer5/blah/blah"));
		Assert.assertEquals("host", "an-swer", Url.getHost("http://an-swer/blah/blah"));
		Assert.assertEquals("host", "ans-wer",  Url.getHost("http://ans-wer.roving.com:0000/blah/blah"));
	}
}
