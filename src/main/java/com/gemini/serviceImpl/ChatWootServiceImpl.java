package com.gemini.serviceImpl;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gemini.service.ChatWootService;

import jakarta.annotation.PostConstruct;

@Service
public class ChatWootServiceImpl implements ChatWootService{

	
	@Value("${gemini.api.key}")
	private String GEMINI_API_KEY;
			
		@Value("${CHATWOOT_API_TOKEN}")
		private String CHATWOOT_API_TOKEN;
	
	private final WebClient webClient = WebClient.builder().build();

//	private String GEMINI_API_KEY;

//	@PostConstruct
//	public void init() {
//	    this.GEMINI_API_KEY = GeminiKey;
//	}
//	
	
	@Override
	@Async
	public void processMessageAsync(Map<String, Object> payload) {

	    try {

	        System.out.println("===================================");
	        System.out.println(LocalDateTime.now() + " payload = " + payload);
	        System.out.println("===================================");

	        // ✅ 1️⃣ Only process new incoming messages
	        String event = (String) payload.get("event");
	        String messageType = (String) payload.get("message_type");

	        if (!"message_created".equals(event)) return;
	        if (!"incoming".equals(messageType)) return;

	        // ✅ 2️⃣ Extract account + conversation id
	     // Extract conversation object
	        Map<String, Object> conversationObj =
	                (Map<String, Object>) payload.get("conversation");
	        
	        Integer conversationId = null;
	        if (conversationObj != null) {
	            conversationId = (Integer) conversationObj.get("id");
	        }

	        Map<String, Object> accountObj =
	                (Map<String, Object>) payload.get("account");

	        Integer accountId = null;
	        if (accountObj != null) {
	            accountId = (Integer) accountObj.get("id");
	        }
	        System.out.println(LocalDateTime.now() +"Extracted accountId = conversationId  ====================");
	        System.out.println(LocalDateTime.now() +"Extracted accountId = " + accountId);
	        System.out.println(LocalDateTime.now() +"Extracted conversationId = " + conversationId);
	        
	        if (accountId == null || conversationId == null) {
	            System.out.println("AccountId or ConversationId is NULL. Skipping...");
	            return;
	        }

	        // ✅ 3️⃣ Extract subject + reply safely
	        Map<String, Object> contentAttributes =
	                (Map<String, Object>) payload.get("content_attributes");

	        String subject = "";
	        String userMessage = "";

	        if (contentAttributes != null) {

	            Map<String, Object> email =
	                    (Map<String, Object>) contentAttributes.get("email");

	            if (email != null) {

	                subject = (String) email.getOrDefault("subject", "");

	                Map<String, Object> textContent =
	                        (Map<String, Object>) email.get("text_content");

	                if (textContent != null) {
	                    userMessage = (String) textContent.get("reply");
	                }
	            }
	        }

	        // 🔁 Fallback for web widget / whatsapp / etc
	        if (userMessage == null || userMessage.isBlank()) {
	            userMessage = (String) payload.get("content");
	        }

	        if (userMessage == null || userMessage.isBlank()) return;

	        userMessage = userMessage.replaceAll("<[^>]*>", "").trim();

	        System.out.println(LocalDateTime.now() +"Subject: ==" + subject);
	        System.out.println(LocalDateTime.now() +"User message: ==" + userMessage);

	        // ✅ 4️⃣ Create AI Prompt
	        String prompt = """
	                You are a professional customer support assistant.
	                Respond politely and clearly.

	                Email Subject: %s

	                Customer Message:
	                %s
	                """.formatted(subject, userMessage);

	        // ✅ 5️⃣ Call Gemini (Blocking here is OK because async thread)
	        String geminiRaw = webClient.post()
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

	        String aiReply = extractTextFromGemini(geminiRaw);
	        System.out.println(LocalDateTime.now() +"==========================================");
	        System.out.println("aiReply="+aiReply);
	        // ✅ 6️⃣ Send Reply Back To Chatwoot
	        System.out.println(LocalDateTime.now() +"==========================================");
	        sendReplyToChatwoot(accountId, conversationId, aiReply);

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	private String extractTextFromGemini(String geminiRaw) {

	    try {
	        ObjectMapper mapper = new ObjectMapper();
	        JsonNode root = mapper.readTree(geminiRaw);

	        return root
	                .path("candidates")
	                .get(0)
	                .path("content")
	                .path("parts")
	                .get(0)
	                .path("text")
	                .asText();

	    } catch (Exception e) {
	        return "Sorry, I am unable to respond at the moment.";
	    }
	}

	private void sendReplyToChatwoot(Integer accountId, Integer conversationId, String message) {
//		http://192.168.167.215:3000/api/v1/accounts/{account_id}/conversations/{conversation_id}/messages
			System.out.println("sending resposne to chatwoot=================");
			Map<String, Object> body = Map.of(
			        "content", message,
			        "message_type", "outgoing",
			        "private", false
			);
			System.out.println(LocalDateTime.now() +"===================================");
			System.out.println(LocalDateTime.now() +"CHATWOOT_API_TOKEN=="+CHATWOOT_API_TOKEN);
			webClient.post()
			        .uri("http://192.168.167.215:3000/api/v1/accounts/"
			                + accountId + "/conversations/"
			                + conversationId + "/messages")
			        .header("api_access_token", CHATWOOT_API_TOKEN)
			        .contentType(MediaType.APPLICATION_JSON)
			        .bodyValue(body)
			        .retrieve()
			        .bodyToMono(String.class)
			        .block();
	}
	
	public Map<String, Object> handleChatwootMessageBackuP(Map<String, Object> payload) {
		try {
	        System.out.println("===================================");
	        System.out.println("payload = " + payload);
	        System.out.println("===================================");

	        // ✅ 1️⃣ Process only incoming new messages
	        String event = (String) payload.get("event");
	        String messageType = (String) payload.get("message_type");

	        if (!"message_created".equals(event) || !"incoming".equals(messageType)) {
	            return Map.of("content", "Ignored");
	        }

	        // ✅ 2️⃣ Extract subject & email reply content safely
	        Map<String, Object> contentAttributes =
	                (Map<String, Object>) payload.get("content_attributes");

	        String subject = "";
	        String userMessage = "";

	        if (contentAttributes != null) {
	            Map<String, Object> email =
	                    (Map<String, Object>) contentAttributes.get("email");

	            if (email != null) {

	                subject = (String) email.get("subject");

	                Map<String, Object> textContent =
	                        (Map<String, Object>) email.get("text_content");

	                if (textContent != null) {
	                    userMessage = (String) textContent.get("reply");  // ✅ Only reply part
	                }
	            }
	        }

	        // 🔁 Fallback (for non-email channels)
	        if (userMessage == null || userMessage.isBlank()) {
	            userMessage = (String) payload.get("content");
	        }

	        if (userMessage == null || userMessage.isBlank()) {
	            return Map.of("content", "Empty message");
	        }

	        System.out.println("Subject: " + subject);
	        System.out.println("User message: " + userMessage);

	        // ✅ 3️⃣ Create better prompt using subject + content
	        String prompt = """
	                You are a professional customer support assistant.
	                Respond politely and clearly.

	                Email Subject: %s

	                Customer Message:
	                %s
	                """.formatted(subject, userMessage);

	        // ✅ 4️⃣ Call Gemini
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

	        return Map.of("content", geminiResponse);

	    } catch (Exception e) {
	        e.printStackTrace();
	        return Map.of("content", "Sorry, something went wrong.");
	    }
	}


	@Override
	public Map<String, Object> handleChatwootMessage(Map<String, Object> payload) {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}
