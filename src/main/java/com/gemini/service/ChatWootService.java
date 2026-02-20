package com.gemini.service;

import java.util.Map;

import org.springframework.web.bind.annotation.RequestBody;

public interface ChatWootService {

	public Map<String, Object> handleChatwootMessage(@RequestBody Map<String, Object> payload);

	public void processMessageAsync(Map<String, Object> payload);

	
}
