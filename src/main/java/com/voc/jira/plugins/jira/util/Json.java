package com.voc.jira.plugins.jira.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class Json {

    public static String get(Object value) {
    	ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
			return e.getMessage();
		} catch (JsonMappingException e) {
			e.printStackTrace();
			return e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		}
    }

    public static void addResponseCodeAndStringToContextForURL(
			Map<String, Object> context, String url, String key)
			throws MalformedURLException, IOException {
    	Response r = getResponse(url);
		context.put(key + "Url", url);
		context.put(key + "ResponseCode", r.code);
		context.put(key, r.text);
	}

	private static String getResponseString(HttpURLConnection con) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(
				con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		return response.toString();
	}

	public static Response getResponse(String url) throws IOException {
		Response r = new Response();
		URL urlObj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
		r.code = con.getResponseCode();
		r.text = getResponseString(con);
		con.disconnect();
		return r;
	}
	public static String getResponseJson(String url) throws IOException {
		URL urlObj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
		String result = getResponseString(con);
		con.disconnect();
		return result;
	}
}
