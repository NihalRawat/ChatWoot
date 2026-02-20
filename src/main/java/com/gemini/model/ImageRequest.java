package com.gemini.model;

import lombok.Data;

@Data
public class ImageRequest {

	private String transactionId;
	
	private String  base64;
	
	private String  mimeType;
	
	private String  originalFileName;

}
