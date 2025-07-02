package com.somdiproy.lambda.detection.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response model for Nova Lite detection Lambda
 * Contains all detected issues ready for suggestion generation
 */
public class DetectionResponse {
    
    @JsonProperty("analysisId")
    private String analysisId;
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("filesAnalyzed")
    private Integer filesAnalyzed;
    
    @JsonProperty("totalFiles")
    private Integer totalFiles;
    
    @JsonProperty("issuesFound")
    private Integer issuesFound;
    
    @JsonProperty("issues")
    private List<Issue> issues;
    
    @JsonProperty("summary")
    private Summary summary;
    
    @JsonProperty("processingTime")
    private ProcessingTime processingTime;
    
    @JsonProperty("errors")
    private List<String> errors;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructors
    public DetectionResponse() {
        this.issues = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    // Builder pattern
    public static DetectionResponseBuilder builder() {
        return new DetectionResponseBuilder();
    }
    
    // Factory method for error response
    public static DetectionResponse error(String analysisId, String sessionId, String errorMessage) {
        DetectionResponse response = new DetectionResponse();
        response.setAnalysisId(analysisId);
        response.setSessionId(sessionId);
        response.setStatus("error");
        response.getErrors().add(errorMessage);
        return response;
    }
    
    // Getters and Setters
    public String getAnalysisId() {
        return analysisId;
    }
    
    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getFilesAnalyzed() {
        return filesAnalyzed;
    }
    
    public void setFilesAnalyzed(Integer filesAnalyzed) {
        this.filesAnalyzed = filesAnalyzed;
    }
    
    public Integer getTotalFiles() {
        return totalFiles;
    }
    
    public void setTotalFiles(Integer totalFiles) {
        this.totalFiles = totalFiles;
    }
    
    public Integer getIssuesFound() {
        return issuesFound;
    }
    
    public void setIssuesFound(Integer issuesFound) {
        this.issuesFound = issuesFound;
    }
    
    public List<Issue> getIssues() {
        return issues;
    }
    
    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }
    
    public Summary getSummary() {
        return summary;
    }
    
    public void setSummary(Summary summary) {
        this.summary = summary;
    }
    
    public ProcessingTime getProcessingTime() {
        return processingTime;
    }
    
    public void setProcessingTime(ProcessingTime processingTime) {
        this.processingTime = processingTime;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Processing time breakdown
     */
    public static class ProcessingTime {
        
        @JsonProperty("total")
        private Long total;
        
        @JsonProperty("securityAnalysis")
        private Long securityAnalysis;
        
        @JsonProperty("performanceAnalysis")
        private Long performanceAnalysis;
        
        @JsonProperty("qualityAnalysis")
        private Long qualityAnalysis;
        
        @JsonProperty("bestPracticesAnalysis")
        private Long bestPracticesAnalysis;
        
        // Getters and Setters
        public Long getTotal() {
            return total;
        }
        
        public void setTotal(Long total) {
            this.total = total;
        }
        
        public Long getSecurityAnalysis() {
            return securityAnalysis;
        }
        
        public void setSecurityAnalysis(Long securityAnalysis) {
            this.securityAnalysis = securityAnalysis;
        }
        
        public Long getPerformanceAnalysis() {
            return performanceAnalysis;
        }
        
        public void setPerformanceAnalysis(Long performanceAnalysis) {
            this.performanceAnalysis = performanceAnalysis;
        }
        
        public Long getQualityAnalysis() {
            return qualityAnalysis;
        }
        
        public void setQualityAnalysis(Long qualityAnalysis) {
            this.qualityAnalysis = qualityAnalysis;
        }
        
        public Long getBestPracticesAnalysis() {
            return bestPracticesAnalysis;
        }
        
        public void setBestPracticesAnalysis(Long bestPracticesAnalysis) {
            this.bestPracticesAnalysis = bestPracticesAnalysis;
        }
    }
    
    /**
     * Analysis summary
     */
    public static class Summary {
        
        @JsonProperty("totalIssues")
        private Integer totalIssues;
        
        @JsonProperty("criticalCount")
        private Integer criticalCount;
        
        @JsonProperty("highCount")
        private Integer highCount;
        
        @JsonProperty("mediumCount")
        private Integer mediumCount;
        
        @JsonProperty("lowCount")
        private Integer lowCount;
        
        @JsonProperty("securityCount")
        private Integer securityCount;
        
        @JsonProperty("performanceCount")
        private Integer performanceCount;
        
        @JsonProperty("qualityCount")
        private Integer qualityCount;
        
        @JsonProperty("bestPracticesCount")
        private Integer bestPracticesCount;
        
        @JsonProperty("topIssues")
        private List<String> topIssues;
        
        // Constructor
        public Summary() {
            this.topIssues = new ArrayList<>();
        }
        
        // Getters and Setters
        public Integer getTotalIssues() {
            return totalIssues;
        }
        
        public void setTotalIssues(Integer totalIssues) {
            this.totalIssues = totalIssues;
        }
        
        public Integer getCriticalCount() {
            return criticalCount;
        }
        
        public void setCriticalCount(Integer criticalCount) {
            this.criticalCount = criticalCount;
        }
        
        public Integer getHighCount() {
            return highCount;
        }
        
        public void setHighCount(Integer highCount) {
            this.highCount = highCount;
        }
        
        public Integer getMediumCount() {
            return mediumCount;
        }
        
        public void setMediumCount(Integer mediumCount) {
            this.mediumCount = mediumCount;
        }
        
        public Integer getLowCount() {
            return lowCount;
        }
        
        public void setLowCount(Integer lowCount) {
            this.lowCount = lowCount;
        }
        
        public Integer getSecurityCount() {
            return securityCount;
        }
        
        public void setSecurityCount(Integer securityCount) {
            this.securityCount = securityCount;
        }
        
        public Integer getPerformanceCount() {
            return performanceCount;
        }
        
        public void setPerformanceCount(Integer performanceCount) {
            this.performanceCount = performanceCount;
        }
        
        public Integer getQualityCount() {
            return qualityCount;
        }
        
        public void setQualityCount(Integer qualityCount) {
            this.qualityCount = qualityCount;
        }
        
        public Integer getBestPracticesCount() {
            return bestPracticesCount;
        }
        
        public void setBestPracticesCount(Integer bestPracticesCount) {
            this.bestPracticesCount = bestPracticesCount;
        }
        
        public List<String> getTopIssues() {
            return topIssues;
        }
        
        public void setTopIssues(List<String> topIssues) {
            this.topIssues = topIssues;
        }
    }
    
    /**
     * Builder class
     */
    public static class DetectionResponseBuilder {
        private DetectionResponse response = new DetectionResponse();
        
        public DetectionResponseBuilder analysisId(String analysisId) {
            response.setAnalysisId(analysisId);
            return this;
        }
        
        public DetectionResponseBuilder sessionId(String sessionId) {
            response.setSessionId(sessionId);
            return this;
        }
        
        public DetectionResponseBuilder status(String status) {
            response.setStatus(status);
            return this;
        }
        
        public DetectionResponseBuilder filesAnalyzed(Integer filesAnalyzed) {
            response.setFilesAnalyzed(filesAnalyzed);
            return this;
        }
        
        public DetectionResponseBuilder totalFiles(Integer totalFiles) {
            response.setTotalFiles(totalFiles);
            return this;
        }
        
        public DetectionResponseBuilder issuesFound(Integer issuesFound) {
            response.setIssuesFound(issuesFound);
            return this;
        }
        
        public DetectionResponseBuilder issues(List<Issue> issues) {
            response.setIssues(issues);
            return this;
        }
        
        public DetectionResponseBuilder summary(Summary summary) {
            response.setSummary(summary);
            return this;
        }
        
        public DetectionResponseBuilder processingTime(ProcessingTime processingTime) {
            response.setProcessingTime(processingTime);
            return this;
        }
        
        public DetectionResponseBuilder metadata(Map<String, Object> metadata) {
            response.setMetadata(metadata);
            return this;
        }
        
        public DetectionResponse build() {
            return response;
        }
    }
}