package org.magic.tools;

import java.util.HashMap;
import java.util.Map;

public class RequestBuilder
{
	
	private String url;
	private METHOD method;
	private Map<String,String> headers;
	private Map<String,String> content;
	public enum METHOD { POST, GET}
	
	
	public RequestBuilder() {
		headers = new HashMap<>();
		content= new HashMap<>();
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public METHOD getMethod() {
		return method;
	}

	public void setMethod(METHOD method) {
		this.method = method;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> header) {
		this.headers = header;
	}

	public Map<String, String> getContent() {
		return content;
	}

	public void setContent(Map<String, String> content) {
		this.content = content;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append(method).append(" ").append(url).append("\n");
		
		if(!headers.isEmpty()) {
			builder.append("headers----------\n");
			headers.entrySet().forEach(entry->builder.append(entry.getKey()).append(":").append(entry.getValue()).append("\n"));
		}
		
		
		if(!content.isEmpty()) {
			builder.append("body-------------\n");
			content.entrySet().forEach(entry->builder.append(entry.getKey()).append(":").append(entry.getValue()).append("\n"));
		}
		
		
		return builder.toString();
	}
	
	public static RequestBuilder build()
	{
		return new RequestBuilder();
	}
	
	public RequestBuilder method(METHOD m)
	{
		method=m;
		return this;
	}
	
	public RequestBuilder url(String u)
	{
		url=u;
		return this;
	}
	
	public RequestBuilder clearHeaders()
	{
		headers.clear();
		return this;
	}

	public RequestBuilder clearContents()
	{
		content.clear();
		return this;
	}
	
	public RequestBuilder addHeader(String k, String c)
	{
		headers.put(k, c);
		return this;
	}
	
	public RequestBuilder addContent(String k, String c)
	{
		content.put(k, c);
		return this;
	}
}