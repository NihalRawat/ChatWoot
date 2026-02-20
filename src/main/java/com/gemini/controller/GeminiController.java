package com.gemini.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gemini.service.GeminiService;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/ocr")
public class GeminiController {
	
    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

	private static final String OPENAI_API_KEY = "AIzaSyBTYfeMuUQaSq4KhvsQwy4KUXyOrlU1fFc";
//	private static final String OPENAI_API_KEY ="AIzaSyDEt7g1v84s4dpY44f9vWou1vLlr18BZ6c";//gitish    	
	
	@PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> extractFromImage(@RequestParam("file") MultipartFile imageFile,
			@RequestParam(value = "isThinking", defaultValue = "0") int isThinking) throws Exception {		
		System.out.println("isThinking="+isThinking+ "files ="+imageFile);
		return geminiService.extractFromImage(imageFile,isThinking);
		
		
	}
	@PostMapping(value = "/extractall", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> extractFromMultipleImage(@RequestParam("file") MultipartFile[] imageFile) throws Exception {
		System.out.println("files"+imageFile);
		return geminiService.extractFromMultipleImage(imageFile);
		
		
	}
	
	@PostMapping(value = "/read-qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> readQrCode(@RequestParam("file") MultipartFile imageFile) throws Exception {

		return geminiService.readQrCode(imageFile);
	}
	
	
	
	
	
	
	
	
	
	
	
	

}
