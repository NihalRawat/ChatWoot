package com.gemini.serviceImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gemini.model.ImageRequest;
import com.gemini.service.GeminiService;

@Service
public class GeminiServiceImpl implements GeminiService{

	private static final String OPENAI_API_KEY = "AIzaSyByeb_inVndne8LikcBo672tXBb8BhOlnA";
//	private static final String OPENAI_API_KEY ="AIzaSyB7SuH28FbrqgnRfWw9tODvCQau8_t2qZU";
	
	@Override
	public ResponseEntity<String> extractFromImage(MultipartFile imageFile,int isThinking)  {
		try {
			// 1. Get the bytes
		    byte[] imageBytes = imageFile.getBytes();
		    
		    // 2. Encode to Base64 and ensure no line breaks (\n or \r)
		    String base64Image = Base64.getEncoder().encodeToString(imageBytes).replaceAll("\\s", "");

		    // 3. Get the correct MIME type (e.g., image/png, image/jpeg)
		    String mimeType = imageFile.getContentType();
		    
		    //4.loading prompt
		    String currentDate = LocalDate.now().toString();
		    String prompt=loadPromptFromFile("/prompts/passport.txt");
		    prompt = prompt.replace("{{CURRENT_DATE}}", currentDate);

		    //5.hit google api to process;
		 // Pass mimeType to your helper method
		    System.err.println("calling gemini () ++++");
		    String response = callOpenAIVision(prompt, base64Image, mimeType,isThinking);
		    
		    //6.convert into valid required json
		    System.out.println("cleaning json---------------");
		    System.out.println("response ="+response);
//		    String cleanJson = parseGeminiResponse(response);
		    System.err.println("json Cleaned and returned data+++++++++");
		    
		 // 7. NEW: Split the original PDF and save to local folders
//	        splitAndSavePdfs(imageFile, cleanJson);
	        
	        
//		    return ResponseEntity.ok()
//		            .header("Content-Type", "application/json")
//		            .body(cleanJson);
		    return ResponseEntity.ok()
		            .header("Content-Type", "application/json")
		            .body(response);
		    
		}catch(Exception ex) {
			ex.printStackTrace();
			return ResponseEntity.internalServerError()
	                .body("{\"error\": \"" + ex.getMessage() + "\"}");
		}
		
		 
	}
	public String  loadPromptFromFile(String promptToGemini) {
		 String prompt;
		 String resourcePath;
		 if(!promptToGemini.isEmpty() && !promptToGemini.isBlank()) {
			 resourcePath=promptToGemini;
		 }else {
//			  resourcePath = "/prompts/extractimagedata.txt";
			    resourcePath = "/prompts/findAll.txt";
//			 String resourcePath = "/prompts/qrcodeReader.txt"; //to read qr code 
		 }
		

		    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
		        if (is == null) {
		            throw new RuntimeException("Could not find file at: " + resourcePath);
		        }
		        prompt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		        System.err.println("Loaded prompt and prompt length =="+prompt.length());
		        
		        return prompt;
		    } catch (IOException e) {
		        throw new RuntimeException("Error reading prompt file", e);
		    }
		    
		   
		    
	}

	private String callOpenAIVision(String prompt, String base64Image, String mimeType, int isThinking) throws Exception {

	    // Using Gemini 2.5 Flash which supports thinking_config
	    String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + OPENAI_API_KEY;

	    ObjectMapper objectMapper = new ObjectMapper();

	    // 1. Build Content Parts
	    Map<String, Object> textPart = Map.of("text", prompt);
	    Map<String, Object> inlineData = Map.of("mime_type", mimeType, "data", base64Image);
	    Map<String, Object> imagePart = Map.of("inline_data", inlineData);
	    Map<String, Object> content = Map.of("parts", List.of(textPart, imagePart));

	    // 2. Build thinkingConfig based on isThinking variable
	    Map<String, Object> thinkingConfig;
	    if (isThinking == 1) {
	        // ENABLE: Set a budget and include thoughts in response
	        thinkingConfig = Map.of(
	            "include_thoughts", true,
//	            "thinking_budget", 16000 
	            "thinking_budget", -1 //auto pick
	        );
	    } else {
	        // DISABLE FULLY: Set budget to 0
	        thinkingConfig = Map.of(
	            "include_thoughts", false,
	            "thinking_budget", 0
	        );
	    }

	    // 3. Root Request Body
	    Map<String, Object> requestBody = new HashMap<>();
	    requestBody.put("contents", List.of(content));
	    requestBody.put("generationConfig", Map.of("thinking_config", thinkingConfig));

	    // Convert to JSON
	    String jsonRequest = objectMapper.writeValueAsString(requestBody);

	    // 4. HTTP Call
	    HttpRequest request = HttpRequest.newBuilder()
	            .uri(URI.create(url))
	            .header("Content-Type", "application/json")
	            .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
	            .build();

	    HttpClient client = HttpClient.newHttpClient();
	    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

	    System.out.println("Gemini Response: " + response.body());
	    return response.body();
	}

	private String callOpenAIVisionBackUp20260212(String prompt, String base64Image, String mimeType,int isThinking) throws Exception {
	    // 1. Updated to a stable version (Gemini 2.5 is very new/preview; 2.0 is stable)
	    String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + OPENAI_API_KEY;

	    // 2. IMPORTANT: Escape the prompt to make it JSON-safe
	    // Without this, the newlines in your prompt will break the JSON structure
	    ObjectMapper mapper = new ObjectMapper();
	    String jsonSafePrompt = mapper.writeValueAsString(prompt); 
	    // This adds surrounding quotes, so we strip them for the text block below
	    jsonSafePrompt = jsonSafePrompt.substring(1, jsonSafePrompt.length() - 1);
	 // 3. Determine if thoughts should be enabled (1 = true, 0 = false)
	    boolean enableThoughts = (isThinking == 1);
	    
	    String requestBody = """
				 {
				   "contents": [
				     {
				       "parts": [
				         { "text": "%s" },
				         {
				           "inline_data": {
				             "mime_type": "%s",
				             "data": "%s"
				           }
				         }
				       ]
				     }
				   ],
				  "generationConfig": {
				   "thinking_config": {
				     "include_thoughts": %b
				   }
				 }
				}
	    """.formatted(jsonSafePrompt, mimeType, base64Image,enableThoughts);

	    HttpRequest request = HttpRequest.newBuilder()
	            .uri(URI.create(url))
	            .header("Content-Type", "application/json")
	            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
	            .build();

	    HttpClient client = HttpClient.newHttpClient();
	    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
	    System.out.println("response.body()= "+response.body());
	    return response.body();
	}
	private String parseGeminiResponse(String rawResponse) throws Exception {
	    ObjectMapper mapper = new ObjectMapper();
	    
	    JsonNode root = mapper.readTree(rawResponse);
	    
	    // 1. Get the raw text string from the Gemini response
	    String contentText = root.path("candidates").get(0)
	                             .path("content")
	                             .path("parts").get(0)
	                             .path("text").asText();

	    // 2. Remove potential Markdown formatting if present
	    String cleanJson = contentText.replaceAll("(?s)^```(?:json)?\\n|\\n```$", "").trim();

	    // 3. BEAUTIFY the JSON before returning it
	    Object jsonObject = mapper.readValue(cleanJson, Object.class);
	    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
	}
	
	private void splitAndSavePdfsBackup(MultipartFile imageFile, String jsonResponse) throws Exception {
		System.out.println("Started Splitting document's ");
	    ObjectMapper mapper = new ObjectMapper();
	    JsonNode root = mapper.readTree(jsonResponse);
	    JsonNode documents = root.path("documents");

	    String outputBaseDir = "C:/Users/Nihal/Downloads/sample-OCR-PDF"; // Your local base folder

	    try (PDDocument sourceDoc = PDDocument.load(imageFile.getInputStream())) {
	        for (JsonNode docNode : documents) {
	            String docType = docNode.path("type").asText().replaceAll("[^a-zA-Z0-9.-]", "_");
	            String pageRange = docNode.path("pages").asText();
	            
	            // Create a unique folder or filename using a random ID
	            String randomId = UUID.randomUUID().toString().substring(0, 8);
	            File directory = new File(outputBaseDir + docType);
	            if (!directory.exists()) directory.mkdirs();

	            try (PDDocument newDoc = new PDDocument()) {
	                // Handle ranges like "1", "3-5", or "3-5, 7-8"
	            	System.out.println("Doing for newDoc "+newDoc);
	                String[] parts = pageRange.split(",");
	                for (String part : parts) {
	                    part = part.trim();
	                    if (part.contains("-")) {
	                        String[] range = part.split("-");
	                        int start = Integer.parseInt(range[0]);
	                        int end = Integer.parseInt(range[1]);
	                        for (int i = start; i <= end; i++) {
	                            newDoc.addPage(sourceDoc.getPage(i - 1));
	                        }
	                    } else {
	                        int pageNum = Integer.parseInt(part);
	                        newDoc.addPage(sourceDoc.getPage(pageNum - 1));
	                    }
	                }
	                
	                String fileName = String.format("%s/%s_%s.pdf", directory.getPath(), docType, randomId);
	                newDoc.save(fileName);
	                System.out.println("Saved: " + fileName);
	            }
	        }
	    }
	}
	
	private void splitAndSavePdfs(MultipartFile imageFile, String jsonResponse) throws Exception {
	    System.out.println("Started Splitting documents...");
	    ObjectMapper mapper = new ObjectMapper();
	    JsonNode root = mapper.readTree(jsonResponse);
	    String documentMode = root.path("documentMode").asText();
	    JsonNode documents = root.path("documents");

	    // 1. Path aur Month Setup
	    String outputBaseDir = "C:/Users/Nihal/Downloads/sample-OCR-PDF/";
	    
	    // Current Month Folder (e.g., JANUARY_2026)
	    String currentMonth = java.time.LocalDate.now().getMonth().toString() + "_" + java.time.LocalDate.now().getYear();
	    File monthDirectory = new File(outputBaseDir, currentMonth);
	    if (!monthDirectory.exists()) monthDirectory.mkdirs();

	    // 2. Unique Batch ID aur Original Filename GET
	    String batchId = UUID.randomUUID().toString().substring(0, 8);
	    String originalNameWithExtension = imageFile.getOriginalFilename();
	    // REMOVE Extension  (e.g., "2480_001_merged" instead of "2480_001_merged.pdf")
	    String originalBaseName = originalNameWithExtension != null ? 
	                              originalNameWithExtension.replaceFirst("[.][^.]+$", "") : "UPLOADED_FILE";

	    // 3. FIRST SAVE  INITIAL Full Document  (Original Name + Batch ID)
	    String masterFileName = String.format("%s_MASTER_%s.pdf", originalBaseName, batchId);
	    File masterFileDestination = new File(monthDirectory, masterFileName);
	    imageFile.transferTo(masterFileDestination);
	    System.out.println("Master document saved: " + masterFileDestination.getAbsolutePath());

	 // 4. CHECKING DOCUMENT IF SINGLE THEN NO NEED TO SPLIT
	    if ("single_document".equalsIgnoreCase(documentMode)) {
	        System.err.println("Single document detected. Skipping split process.");
	        return; // don't call split logic further
	    }
	    
	    // 4. Ab PDF split karke save karein
	    try (PDDocument sourceDoc = PDDocument.load(masterFileDestination)) {
	        for (JsonNode docNode : documents) {
	            String docType = docNode.path("type").asText().replaceAll("[^a-zA-Z0-9.-]", "_");
	            String pageRange = docNode.path("pages").asText();

	            try (PDDocument newDoc = new PDDocument()) {
	                // Page extraction logic
	                String[] parts = pageRange.split(",");
	                for (String part : parts) {
	                    part = part.trim();
	                    if (part.contains("-")) {
	                        String[] range = part.split("-");
	                        int start = Integer.parseInt(range[0]);
	                        int end = Integer.parseInt(range[1]);
	                        for (int i = start; i <= end; i++) {
	                            newDoc.addPage(sourceDoc.getPage(i - 1));
	                        }
	                    } else {
	                        int pageNum = Integer.parseInt(part);
	                        newDoc.addPage(sourceDoc.getPage(pageNum - 1));
	                    }
	                }

	                // Split File Naming: DocType_OriginalName_BatchID.pdf
	                String splitFileName = String.format("%s_%s_%s.pdf", docType, originalBaseName, batchId);
	                File finalFilePath = new File(monthDirectory, splitFileName);
	                newDoc.save(finalFilePath);
	                System.out.println("Saved Split File: " + finalFilePath.getAbsolutePath());
	            }
	        }
	    }
	}
	@Override
	public ResponseEntity<String> readQrCode(MultipartFile imageFile) {
		try {
			// 1. Get the bytes
		    byte[] imageBytes = imageFile.getBytes();
		    
		    // 2. Encode to Base64 and ensure no line breaks (\n or \r)
		    String base64Image = Base64.getEncoder().encodeToString(imageBytes).replaceAll("\\s", "");

		    // 3. Get the correct MIME type (e.g., image/png, image/jpeg)
		    String mimeType = imageFile.getContentType();
		    
		    //4.loading prompt
		    String prompt=loadPromptFromFile("/prompts/qrcodeReader.txt");
		    
		    //5.hit google api to process;
		 // Pass mimeType to your helper method
		    System.err.println("calling openAi () ++++");
		    String response = callOpenAIVision(prompt, base64Image, mimeType,0);
		    
		    //6.convert into valid required json
		    System.out.println("cleaning json---------------");
		    System.out.println("response ="+response);
		   
	        
	        
		    return ResponseEntity.ok()
		            .header("Content-Type", "application/json")
		            .body(response);
		    
		}catch(Exception ex) {
			ex.printStackTrace();
			return ResponseEntity.internalServerError()
	                .body("{\"error\": \"" + ex.getMessage() + "\"}");
		}
	}
//	@Override
	public ResponseEntity<String> extractFromMultipleImageBackUpNew(MultipartFile[] files) {

	    if (files == null || files.length == 0) {
	        return ResponseEntity.badRequest()
	                .body("{\"error\":\"No files found\"}");
	    }

	    try {
	    	List<String> base64Images = new ArrayList<>();
	    	List<String> mimeTypes = new ArrayList<>();
	    	List<String> transactionIds = new ArrayList<>();

	    	for (MultipartFile file : files) {
	    		String transactionId = UUID.randomUUID().toString();
	    		System.out.println("converting image's into base-64 ="+file.getOriginalFilename());
	    	    base64Images.add(
	    	        Base64.getEncoder().encodeToString(file.getBytes()).replaceAll("\\s", "")
	    	    );
	    	    mimeTypes.add(file.getContentType());
	    	    transactionIds.add(transactionId);
	    	}
	    	
//	    	String prompt = loadPromptFromFile("/prompts/getAdhar.txt");
	    	String prompt = loadPromptFromFile("/prompts/panCardPrompt.txt");
	    	System.out.println("prompt loaded ........"+prompt.length());
	        // 🔥 SINGLE Gemini call with ALL images
	    	String response =
	    	    callOpenAIVisionWithMultipleImages(prompt, base64Images, mimeTypes);
	 	     System.out.println("response from gemini= "+response);  
	        String cleanJson = parseGeminiResponse(response);
	        System.out.println("response ="+cleanJson);
	        return ResponseEntity.ok()
	                .header("Content-Type", "application/json")
	                .body(cleanJson);

	    } catch (Exception ex) {
	        ex.printStackTrace();
	        return ResponseEntity.internalServerError()
	                .body("{\"error\":\"" + ex.getMessage() + "\"}");
	    }
	}
	private String callOpenAIVisionWithMultipleImages(
	        String prompt,
	        List<String> base64Images,
	        List<String> mimeTypes) throws Exception {
		System.out.println("calling gemini===");
	    String url =
	        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
	        + OPENAI_API_KEY;

	    ObjectMapper mapper = new ObjectMapper();
	    //Your prompt may contain: New lines , Special characters,Quotes
	    //writeValueAsString() escapes everything properly
	    String jsonSafePrompt = mapper.writeValueAsString(prompt);
	    jsonSafePrompt = jsonSafePrompt.substring(1, jsonSafePrompt.length() - 1);

	    StringBuilder partsBuilder = new StringBuilder();

	    // Prompt first
	    partsBuilder.append("""
	        { "text": "%s" }
	    """.formatted(jsonSafePrompt));

	    // Add ALL images
	    for (int i = 0; i < base64Images.size(); i++) {
	        partsBuilder.append("""
	            ,
	            {
	              "inline_data": {
	                "mime_type": "%s",
	                "data": "%s"
	              }
	            }
	        """.formatted(mimeTypes.get(i), base64Images.get(i)));
	    }

	    String requestBody = """
	    {
	      "contents": [
	        {
	          "parts": [
	            %s
	          ]
	        }
	      ]
	    }
	    """.formatted(partsBuilder.toString());

	    HttpRequest request = HttpRequest.newBuilder()
	            .uri(URI.create(url))
	            .header("Content-Type", "application/json")
	            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
	            .build();

	    HttpClient client = HttpClient.newHttpClient();
	    HttpResponse<String> response =
	            client.send(request, HttpResponse.BodyHandlers.ofString());

	    return response.body();
	}
	
	@Override
	public ResponseEntity<String> extractFromMultipleImage(MultipartFile[] files) {

	    if (files == null || files.length == 0) {
	        return ResponseEntity.badRequest()
	                .body("{\"error\":\"No files found\"}");
	    }

	    try {
	        List<ImageRequest> imageRequests = new ArrayList<>();

	        for (MultipartFile file : files) {
	        	System.out.println("Converting into base 64 image name="+file.getOriginalFilename());
	            ImageRequest req = new ImageRequest();
	            req.setTransactionId(UUID.randomUUID().toString());
	            req.setBase64(
	                    Base64.getEncoder()
	                            .encodeToString(file.getBytes())
	                            .replaceAll("\\s", "")
	            );
	            req.setMimeType(file.getContentType());
	            req.setOriginalFileName(file.getOriginalFilename());

	            imageRequests.add(req);

	            System.out.println(
	                    "Prepared image → " +
	                    req.getOriginalFileName() +
	                    " | txn=" + req.getTransactionId()
	            );
	        }
	        
	        String prompt = loadPromptFromFile("/prompts/panCardPrompt.txt");
	        System.out.println("loading prompt..............prompt length="+prompt.length());
	        String response = callGeminiWithMultipleImages(prompt, imageRequests);
	        System.err.println("response received from gemini ="+response);
	        System.out.println("Preaparing a valid json ................");
	        String cleanJson = parseGeminiResponse(response);
	        System.err.println("json formatted ................");
	        return ResponseEntity.ok()
	                .header("Content-Type", "application/json")
	                .body(cleanJson);

	    } catch (Exception ex) {
	        ex.printStackTrace();
	        return ResponseEntity.internalServerError()
	                .body("{\"error\":\"" + ex.getMessage() + "\"}");
	    }
	}
	private String callGeminiWithMultipleImages(
	        String prompt,
	        List<ImageRequest> images
	) throws Exception {

	    String url =
	            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
	                    + OPENAI_API_KEY;

	    List<Map<String, Object>> parts = new ArrayList<>();

	    // 🔥 System / main prompt
	    parts.add(Map.of("text", prompt));

	    // 🔥 STRICT transactionId + image pairing
	    for (ImageRequest img : images) {

	        parts.add(Map.of(
	                "text",
	                "transactionId: " + img.getTransactionId()
	        ));

	        parts.add(Map.of(
	                "inline_data",
	                Map.of(
	                        "mime_type", img.getMimeType(),
	                        "data", img.getBase64()
	                )
	        ));
	    }

	    Map<String, Object> body = Map.of(
	            "contents", List.of(
	                    Map.of(
	                            "role", "user",
	                            "parts", parts
	                    )
	            ),
	            "generationConfig", Map.of(
	                    "temperature", 0,
	                    "responseMimeType", "application/json"
	            )
	    );

	    ObjectMapper mapper = new ObjectMapper();
	    String requestJson = mapper.writeValueAsString(body);

	    HttpRequest request = HttpRequest.newBuilder()
	            .uri(URI.create(url))
	            .header("Content-Type", "application/json")
	            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
	            .build();

	    HttpClient client = HttpClient.newHttpClient();
	    HttpResponse<String> response =
	            client.send(request, HttpResponse.BodyHandlers.ofString());

	    return response.body();
	}




}
