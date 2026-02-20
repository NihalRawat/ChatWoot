package com.gemini.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.gemini.service.ChatWootService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class ChatwootWebhookController {

	@Value("${gemini.api.key}")
	private String GeminiKey;
	
	private final WebClient webClient = WebClient.builder().build();

	private final ChatWootService chatWootService;
	private String GEMINI_API_KEY;

	@PostConstruct
	public void init() {
	    this.GEMINI_API_KEY = GeminiKey;
	}

	@PostMapping("/chatwoot")
	public ResponseEntity<Void> handleChatwootMessage(@RequestBody Map<String, Object> payload) {

		chatWootService.processMessageAsync(payload);  // async call
		
		System.out.println("immediate response ============"+ResponseEntity.ok().build());
	    return ResponseEntity.ok().build();
	}
	
	 @PostMapping("/chatwoot/v2")
	    public Map<String, Object> handleChatwootMessageBackUp2(@RequestBody Map<String, Object> payload) {

	        try {	        	
	        return 	chatWootService.handleChatwootMessage(payload);	        	
	        } catch (Exception e) {
	        	e.printStackTrace();
	            return Map.of(
	                    "content", "Sorry, something went wrong. Please try again later."
	            );
	        }
	    }

    @PostMapping("/chatwoot/v1")
    public Map<String, Object> handleChatwootMessageBackup(@RequestBody Map<String, Object> payload) {

        try {
        	System.out.println( " ==================================================");
        	System.out.println( " payload="+payload);
        	System.out.println( " ==================================================");
            // 1️⃣ Extract message content from Chatwoot payload
            Map<String, Object> messageObj = (Map<String, Object>) payload.get("message");
            String userMessage = messageObj.get("content").toString();

            // 2️⃣ Create prompt for Gemini
            String prompt = """
                    You are a professional customer support assistant.
                    Respond politely and clearly.

                    Customer message:
                    %s
                    """.formatted(userMessage);
            System.out.println( " messageObj="+messageObj+" userMessage="+userMessage);
            // 3️⃣ Call Gemini API
            String geminiResponse = webClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                              "contents": [{
                                "parts":[{"text":"%s"}]
                              }]
                            }
                            """.formatted(prompt))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            System.out.println("geminiResponse="+geminiResponse);
            // ⚠️ In production, parse JSON properly.
            // For now returning raw response for simplicity.

            return Map.of(
                    "content", geminiResponse
            );

        } catch (Exception e) {
        	e.printStackTrace();
            return Map.of(
                    "content", "Sorry, something went wrong. Please try again later."
            );
        }
    }
	
}
