package com.somdiproy.lambda.detection.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Request model for Nova Lite detection Lambda
 * Receives screened files from Nova Micro for issue detection
 */
public class DetectionRequest {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("analysisId") 
    private String analysisId;
    
    @JsonProperty("repository")
    private String repository;
    
    @JsonProperty("branch")
    private String branch;
    
    @JsonProperty("files")
    private List<FileInput> files;
    
    @JsonProperty("stage")
    private String stage = "detection";
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("scanNumber")
    private Integer scanNumber;
    
    @JsonProperty("options")
    private DetectionOptions options;
    
    // Constructors
    public DetectionRequest() {
        this.timestamp = System.currentTimeMillis();
        this.stage = "detection";
    }
    
    public DetectionRequest(String sessionId, String analysisId, List<FileInput> files) {
        this();
        this.sessionId = sessionId;
        this.analysisId = analysisId;
        this.files = files;
    }
    
    // Validation
    public boolean isValid() {
        return sessionId != null && !sessionId.isEmpty()
            && analysisId != null && !analysisId.isEmpty()
            && files != null && !files.isEmpty();
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getAnalysisId() {
        return analysisId;
    }
    
    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }
    
    public String getRepository() {
        return repository;
    }
    
    public void setRepository(String repository) {
        this.repository = repository;
    }
    
    public String getBranch() {
        return branch;
    }
    
    public void setBranch(String branch) {
        this.branch = branch;
    }
    
    public List<FileInput> getFiles() {
        return files;
    }
    
    public void setFiles(List<FileInput> files) {
        this.files = files;
    }
    
    public String getStage() {
        return stage;
    }
    
    public void setStage(String stage) {
        this.stage = stage;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public Integer getScanNumber() {
        return scanNumber;
    }
    
    public void setScanNumber(Integer scanNumber) {
        this.scanNumber = scanNumber;
    }
    
    public DetectionOptions getOptions() {
        return options;
    }
    
    public void setOptions(DetectionOptions options) {
        this.options = options;
    }
    
    /**
     * File input model - matches output from screening lambda
     */
    public static class FileInput {
        
        @JsonProperty("path")
        private String path;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("content")
        private String content;
        
        @JsonProperty("optimizedContent")
        private String optimizedContent;
        
        @JsonProperty("size")
        private Long size;
        
        @JsonProperty("language")
        private String language;
        
        @JsonProperty("confidence")
        private Double confidence;
        
        @JsonProperty("complexity")
        private String complexity;
        
        @JsonProperty("sha")
        private String sha;
        
        @JsonProperty("mimeType")
        private String mimeType;
        
        @JsonProperty("encoding")
        private String encoding;
        
        // Constructors
        public FileInput() {}
        
        // Getters and Setters
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getOptimizedContent() {
            return optimizedContent != null ? optimizedContent : content;
        }
        
        public void setOptimizedContent(String optimizedContent) {
            this.optimizedContent = optimizedContent;
        }
        
        public Long getSize() {
            return size;
        }
        
        public void setSize(Long size) {
            this.size = size;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public void setLanguage(String language) {
            this.language = language;
        }
        
        public Double getConfidence() {
            return confidence;
        }
        
        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }
        
        public String getComplexity() {
            return complexity;
        }
        
        public void setComplexity(String complexity) {
            this.complexity = complexity;
        }
        
        public String getSha() {
            return sha;
        }
        
        public void setSha(String sha) {
            this.sha = sha;
        }
        
        public String getMimeType() {
            return mimeType;
        }
        
        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }
        
        public String getEncoding() {
            return encoding;
        }
        
        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }
    }
    
    /**
     * Detection options
     */
    public static class DetectionOptions {
        
        @JsonProperty("includeSecurityAnalysis")
        private Boolean includeSecurityAnalysis = true;
        
        @JsonProperty("includePerformanceAnalysis")
        private Boolean includePerformanceAnalysis = true;
        
        @JsonProperty("includeQualityAnalysis")
        private Boolean includeQualityAnalysis = true;
        
        @JsonProperty("includeBestPractices")
        private Boolean includeBestPractices = true;
        
        @JsonProperty("severityThreshold")
        private String severityThreshold = "low";
        
        @JsonProperty("maxIssuesPerFile")
        private Integer maxIssuesPerFile = 20;
        
        // Getters and Setters
        public Boolean getIncludeSecurityAnalysis() {
            return includeSecurityAnalysis;
        }
        
        public void setIncludeSecurityAnalysis(Boolean includeSecurityAnalysis) {
            this.includeSecurityAnalysis = includeSecurityAnalysis;
        }
        
        public Boolean getIncludePerformanceAnalysis() {
            return includePerformanceAnalysis;
        }
        
        public void setIncludePerformanceAnalysis(Boolean includePerformanceAnalysis) {
            this.includePerformanceAnalysis = includePerformanceAnalysis;
        }
        
        public Boolean getIncludeQualityAnalysis() {
            return includeQualityAnalysis;
        }
        
        public void setIncludeQualityAnalysis(Boolean includeQualityAnalysis) {
            this.includeQualityAnalysis = includeQualityAnalysis;
        }
        
        public Boolean getIncludeBestPractices() {
            return includeBestPractices;
        }
        
        public void setIncludeBestPractices(Boolean includeBestPractices) {
            this.includeBestPractices = includeBestPractices;
        }
        
        public String getSeverityThreshold() {
            return severityThreshold;
        }
        
        public void setSeverityThreshold(String severityThreshold) {
            this.severityThreshold = severityThreshold;
        }
        
        public Integer getMaxIssuesPerFile() {
            return maxIssuesPerFile;
        }
        
        public void setMaxIssuesPerFile(Integer maxIssuesPerFile) {
            this.maxIssuesPerFile = maxIssuesPerFile;
        }
    }
}