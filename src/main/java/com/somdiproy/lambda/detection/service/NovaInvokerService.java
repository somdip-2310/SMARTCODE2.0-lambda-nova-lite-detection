package com.somdiproy.lambda.detection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

/**
 * Service for invoking Amazon Nova models via Bedrock Converse API
 * Used by Detection Lambda for Nova Lite model invocation
 */
public class NovaInvokerService {
    
    private static final Logger logger = LoggerFactory.getLogger(NovaInvokerService.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String bedrockRegion;
    private final Map<String, Long> lastCallTime = new HashMap<>();
    private final Map<String, Integer> callCount = new HashMap<>();
    
    // Rate limiting: Nova Lite has different limits than Micro
    private static final long MIN_CALL_INTERVAL_MS = 200; // 5 calls per second
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    
    public NovaInvokerService(String bedrockRegion) {
        this.bedrockRegion = bedrockRegion != null ? bedrockRegion : "us-east-1";
    }
    
    /**
     * Invoke Nova Micro model for file screening
     */
    public NovaResponse invokeNovaMicro(String prompt, NovaRequest request) throws NovaInvokerException {
        return invokeNova("amazon.nova-micro-v1:0", prompt, request, 500);
    }
    
    /**
     * Invoke Nova Lite model for issue detection  
     */
    public NovaResponse invokeNovaLite(String prompt, NovaRequest request) throws NovaInvokerException {
        return invokeNova("amazon.nova-lite-v1:0", prompt, request, 4000);
    }
    
    /**
     * Invoke Nova Premier model for suggestion generation
     */
    public NovaResponse invokeNovaPremier(String prompt, NovaRequest request) throws NovaInvokerException {
        return invokeNova("amazon.nova-pro-v1:0", prompt, request, 8000);
    }
    
    /**
     * Generic Nova model invocation with rate limiting and retries
     */
    private NovaResponse invokeNova(String modelId, String prompt, NovaRequest request, int maxTokens) 
            throws NovaInvokerException {
        
        String callKey = modelId + "-" + Thread.currentThread().getId();
        
        try {
            // Rate limiting
            enforceRateLimit(callKey);
            
            // Make API call with retries using Converse API
            NovaResponse response = callBedrockWithRetries(modelId, prompt, request, maxTokens);
            
            // Track metrics
            updateCallMetrics(callKey);
            
            logger.debug("Successfully invoked {} for analysis", modelId);
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to invoke Nova model {}: {}", modelId, e.getMessage());
            throw new NovaInvokerException("Nova invocation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Call Bedrock with retries using Converse API
     */
    private NovaResponse callBedrockWithRetries(String modelId, String prompt, 
                                               NovaRequest request, int maxTokens) 
            throws NovaInvokerException {
        
        Exception lastException = null;
        
        // Initialize AWS SDK client
        BedrockRuntimeClient bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.of(bedrockRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
            	// Build the request
                InferenceConfiguration inferenceConfig = InferenceConfiguration.builder()
                    .maxTokens(maxTokens)
                    .temperature(request != null ? (float) request.getTemperature() : 0.1f)
                    .topP(request != null ? (float) request.getTopP() : 0.9f)
                    .stopSequences(request != null ? request.getStopSequences() : null)
                    .build();
                
                Message userMessage = Message.builder()
                    .role(ConversationRole.USER)
                    .content(ContentBlock.builder()
                        .text(prompt)
                        .build())
                    .build();
                
                ConverseRequest converseRequest = ConverseRequest.builder()
                    .modelId(modelId)
                    .messages(userMessage)
                    .inferenceConfig(inferenceConfig)
                    .build();
                
                // Make the API call
                long startTime = System.currentTimeMillis();
                ConverseResponse converseResponse = bedrockClient.converse(converseRequest);
                long duration = System.currentTimeMillis() - startTime;
                
                // Parse response
                ConverseOutput output = converseResponse.output();
                String responseText = "";
                
                if (output.message() != null && !output.message().content().isEmpty()) {
                    ContentBlock contentBlock = output.message().content().get(0);
                    if (contentBlock.text() != null) {
                        responseText = contentBlock.text();
                    }
                }
                
                // Get token usage
                TokenUsage usage = converseResponse.usage();
                int inputTokens = usage.inputTokens();
                int outputTokens = usage.outputTokens();
                int totalTokens = usage.totalTokens();
                
                // Calculate cost based on model
                double costPerMillion = getCostPerMillion(modelId);
                double estimatedCost = (totalTokens / 1_000_000.0) * costPerMillion;
                
                // Build response
                NovaResponse response = NovaResponse.builder()
                    .responseText(responseText)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .totalTokens(totalTokens)
                    .estimatedCost(estimatedCost)
                    .modelId(modelId)
                    .successful(true)
                    .timestamp(System.currentTimeMillis())
                    .build();
                
                logger.info("Nova {} call successful. Tokens: {}, Cost: ${:.6f}, Duration: {}ms", 
                           modelId, totalTokens, estimatedCost, duration);
                
                return response;
                
            } catch (ThrottlingException e) {
                logger.warn("Rate limit hit on attempt {}: {}", attempt + 1, e.getMessage());
                lastException = e;
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new NovaInvokerException("Interrupted during retry", ie);
                    }
                }
            } catch (ModelTimeoutException e) {
                logger.warn("Model timeout on attempt {}: {}", attempt + 1, e.getMessage());
                lastException = e;
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new NovaInvokerException("Interrupted during retry", ie);
                    }
                }
            } catch (AccessDeniedException e) {
                logger.error("Access denied to Bedrock API: {}", e.getMessage());
                throw new NovaInvokerException("Access denied - check IAM permissions", e);
            } catch (ResourceNotFoundException e) {
                logger.error("Model not found: {}", modelId);
                throw new NovaInvokerException("Model not found: " + modelId, e);
            } catch (ValidationException e) {
                logger.error("Invalid request: {}", e.getMessage());
                throw new NovaInvokerException("Invalid request parameters", e);
            } catch (BedrockRuntimeException e) {
                logger.error("Bedrock error on attempt {}: {}", attempt + 1, e.getMessage());
                lastException = e;
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new NovaInvokerException("Interrupted during retry", ie);
                    }
                }
            } catch (SdkServiceException e) {
                logger.error("AWS SDK service error: {}", e.getMessage());
                lastException = e;
            } catch (SdkClientException e) {
                logger.error("AWS SDK client error: {}", e.getMessage());
                lastException = e;
            }
        }
        
        // All retries failed
        throw new NovaInvokerException(
            String.format("Failed to invoke Nova after %d attempts: %s", 
                         MAX_RETRIES, lastException.getMessage()), 
            lastException
        );
    }
    
    /**
     * Get cost per million tokens for each model
     */
    private double getCostPerMillion(String modelId) {
        switch (modelId) {
            case "amazon.nova-micro-v1:0":
                return 0.0075;
            case "amazon.nova-lite-v1:0":
                return 0.015;
            case "amazon.nova-pro-v1:0":
                return 0.80;
            default:
                return 0.0;
        }
    }
    
    /**
     * Enforce rate limiting
     */
    private void enforceRateLimit(String callKey) throws InterruptedException {
        Long lastCall = lastCallTime.get(callKey);
        if (lastCall != null) {
            long timeSinceLastCall = System.currentTimeMillis() - lastCall;
            if (timeSinceLastCall < MIN_CALL_INTERVAL_MS) {
                Thread.sleep(MIN_CALL_INTERVAL_MS - timeSinceLastCall);
            }
        }
        lastCallTime.put(callKey, System.currentTimeMillis());
    }
    
    /**
     * Update call metrics for monitoring
     */
    private void updateCallMetrics(String callKey) {
        callCount.merge(callKey, 1, Integer::sum);
    }
    
    /**
     * Get call statistics
     */
    public Map<String, Integer> getCallStatistics() {
        return new HashMap<>(callCount);
    }
    
    /**
     * Reset call statistics
     */
    public void resetStatistics() {
        callCount.clear();
        lastCallTime.clear();
    }
    
    /**
     * Request configuration for Nova models
     */
    public static class NovaRequest {
        private double temperature = 0.1;
        private double topP = 0.9;
        private List<String> stopSequences;
        private Map<String, Object> additionalParams = new HashMap<>();
        
        // Constructors
        public NovaRequest() {}
        
        public NovaRequest(double temperature, double topP) {
            this.temperature = temperature;
            this.topP = topP;
        }
        
        // Builder pattern
        public static NovaRequest builder() {
            return new NovaRequest();
        }
        
        public NovaRequest temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public NovaRequest topP(double topP) {
            this.topP = topP;
            return this;
        }
        
        public NovaRequest stopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
            return this;
        }
        
        public NovaRequest addParam(String key, Object value) {
            this.additionalParams.put(key, value);
            return this;
        }
        
        // Getters
        public double getTemperature() { return temperature; }
        public double getTopP() { return topP; }
        public List<String> getStopSequences() { return stopSequences; }
        public Map<String, Object> getAdditionalParams() { return additionalParams; }
    }
    
    /**
     * Response from Nova models
     */
    public static class NovaResponse {
        private String responseText;
        private int inputTokens;
        private int outputTokens;
        private int totalTokens;
        private double estimatedCost;
        private String modelId;
        private boolean successful;
        private long timestamp;
        private String errorMessage;
        private Map<String, Object> metadata = new HashMap<>();
        
        // Constructors
        public NovaResponse() {}
        
        // Builder pattern
        public static NovaResponseBuilder builder() {
            return new NovaResponseBuilder();
        }
        
        // Getters and setters
        public String getResponseText() { return responseText; }
        public void setResponseText(String responseText) { this.responseText = responseText; }
        
        public int getInputTokens() { return inputTokens; }
        public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }
        
        public int getOutputTokens() { return outputTokens; }
        public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }
        
        public int getTotalTokens() { return totalTokens; }
        public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
        
        public double getEstimatedCost() { return estimatedCost; }
        public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
        
        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }
        
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        
        public static class NovaResponseBuilder {
            private NovaResponse response = new NovaResponse();
            
            public NovaResponseBuilder responseText(String responseText) {
                response.setResponseText(responseText);
                return this;
            }
            
            public NovaResponseBuilder inputTokens(int inputTokens) {
                response.setInputTokens(inputTokens);
                return this;
            }
            
            public NovaResponseBuilder outputTokens(int outputTokens) {
                response.setOutputTokens(outputTokens);
                return this;
            }
            
            public NovaResponseBuilder totalTokens(int totalTokens) {
                response.setTotalTokens(totalTokens);
                return this;
            }
            
            public NovaResponseBuilder estimatedCost(double estimatedCost) {
                response.setEstimatedCost(estimatedCost);
                return this;
            }
            
            public NovaResponseBuilder modelId(String modelId) {
                response.setModelId(modelId);
                return this;
            }
            
            public NovaResponseBuilder successful(boolean successful) {
                response.setSuccessful(successful);
                return this;
            }
            
            public NovaResponseBuilder timestamp(long timestamp) {
                response.setTimestamp(timestamp);
                return this;
            }
            
            public NovaResponseBuilder errorMessage(String errorMessage) {
                response.setErrorMessage(errorMessage);
                return this;
            }
            
            public NovaResponse build() {
                return response;
            }
        }
        
        @Override
        public String toString() {
            return String.format("NovaResponse{model='%s', tokens=%d, cost=%.6f, successful=%s}", 
                               modelId, totalTokens, estimatedCost, successful);
        }
    }
    
    /**
     * Custom exception for Nova invocation errors
     */
    public static class NovaInvokerException extends Exception {
        public NovaInvokerException(String message) {
            super(message);
        }
        
        public NovaInvokerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}