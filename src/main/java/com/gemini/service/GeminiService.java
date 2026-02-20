package com.gemini.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface GeminiService {

	ResponseEntity<String> extractFromImage(MultipartFile imageFile,int thoughtToken);

	ResponseEntity<String> readQrCode(MultipartFile imageFile);

	ResponseEntity<String> extractFromMultipleImage(MultipartFile[] imageFile);

}
