package com.somdiproy.lambda.detection.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a detected issue
 * Used by both Detection and Suggestion lambdas
 */
public class Issue {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("severity")
    private String severity;
    
    @JsonProperty("confidence")
    private Double confidence;
    
    @JsonProperty("file")
    private String file;
    
    @JsonProperty("line")
    private Integer line;
    
    @JsonProperty("column")
    private Integer column;
    
    @JsonProperty("endLine")
    private Integer endLine;
    
    @JsonProperty("endColumn")
    private Integer endColumn;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("detailedDescription")
    private String detailedDescription;
    
    @JsonProperty("codeSnippet")
    private String codeSnippet;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("cwe")
    private String cwe;
    
    @JsonProperty("owasp")
    private String owasp;
    
    @JsonProperty("impact")
    private String impact;
    
    @JsonProperty("likelihood")
    private String likelihood;
    
    @JsonProperty("fixComplexity")
    private String fixComplexity;
    
    @JsonProperty("references")
    private Map<String, String> references;
    
    @JsonProperty("tags")
    private Map<String, String> tags;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructors
    public Issue() {
        this.references = new HashMap<>();
        this.tags = new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    // Builder pattern
    public static IssueBuilder builder() {
        return new IssueBuilder();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public String getFile() {
        return file;
    }
    
    public void setFile(String file) {
        this.file = file;
    }
    
    public Integer getLine() {
        return line;
    }
    
    public void setLine(Integer line) {
        this.line = line;
    }
    
    public Integer getColumn() {
        return column;
    }
    
    public void setColumn(Integer column) {
        this.column = column;
    }
    
    public Integer getEndLine() {
        return endLine;
    }
    
    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }
    
    public Integer getEndColumn() {
        return endColumn;
    }
    
    public void setEndColumn(Integer endColumn) {
        this.endColumn = endColumn;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDetailedDescription() {
        return detailedDescription;
    }
    
    public void setDetailedDescription(String detailedDescription) {
        this.detailedDescription = detailedDescription;
    }
    
    public String getCodeSnippet() {
        return codeSnippet;
    }
    
    public void setCodeSnippet(String codeSnippet) {
        this.codeSnippet = codeSnippet;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getCwe() {
        return cwe;
    }
    
    public void setCwe(String cwe) {
        this.cwe = cwe;
    }
    
    public String getOwasp() {
        return owasp;
    }
    
    public void setOwasp(String owasp) {
        this.owasp = owasp;
    }
    
    public String getImpact() {
        return impact;
    }
    
    public void setImpact(String impact) {
        this.impact = impact;
    }
    
    public String getLikelihood() {
        return likelihood;
    }
    
    public void setLikelihood(String likelihood) {
        this.likelihood = likelihood;
    }
    
    public String getFixComplexity() {
        return fixComplexity;
    }
    
    public void setFixComplexity(String fixComplexity) {
        this.fixComplexity = fixComplexity;
    }
    
    public Map<String, String> getReferences() {
        return references;
    }
    
    public void setReferences(Map<String, String> references) {
        this.references = references;
    }
    
    public Map<String, String> getTags() {
        return tags;
    }
    
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Builder class
     */
    public static class IssueBuilder {
        private Issue issue = new Issue();
        
        public IssueBuilder id(String id) {
            issue.setId(id);
            return this;
        }
        
        public IssueBuilder type(String type) {
            issue.setType(type);
            return this;
        }
        
        public IssueBuilder category(String category) {
            issue.setCategory(category);
            return this;
        }
        
        public IssueBuilder severity(String severity) {
            issue.setSeverity(severity);
            return this;
        }
        
        public IssueBuilder confidence(Double confidence) {
            issue.setConfidence(confidence);
            return this;
        }
        
        public IssueBuilder file(String file) {
            issue.setFile(file);
            return this;
        }
        
        public IssueBuilder line(Integer line) {
            issue.setLine(line);
            return this;
        }
        
        public IssueBuilder column(Integer column) {
            issue.setColumn(column);
            return this;
        }
        
        public IssueBuilder endLine(Integer endLine) {
            issue.setEndLine(endLine);
            return this;
        }
        
        public IssueBuilder endColumn(Integer endColumn) {
            issue.setEndColumn(endColumn);
            return this;
        }
        
        public IssueBuilder description(String description) {
            issue.setDescription(description);
            return this;
        }
        
        public IssueBuilder detailedDescription(String detailedDescription) {
            issue.setDetailedDescription(detailedDescription);
            return this;
        }
        
        public IssueBuilder codeSnippet(String codeSnippet) {
            issue.setCodeSnippet(codeSnippet);
            return this;
        }
        
        public IssueBuilder language(String language) {
            issue.setLanguage(language);
            return this;
        }
        
        public IssueBuilder cwe(String cwe) {
            issue.setCwe(cwe);
            return this;
        }
        
        public IssueBuilder owasp(String owasp) {
            issue.setOwasp(owasp);
            return this;
        }
        
        public IssueBuilder impact(String impact) {
            issue.setImpact(impact);
            return this;
        }
        
        public IssueBuilder likelihood(String likelihood) {
            issue.setLikelihood(likelihood);
            return this;
        }
        
        public IssueBuilder fixComplexity(String fixComplexity) {
            issue.setFixComplexity(fixComplexity);
            return this;
        }
        
        public IssueBuilder addReference(String key, String value) {
            issue.getReferences().put(key, value);
            return this;
        }
        
        public IssueBuilder addTag(String key, String value) {
            issue.getTags().put(key, value);
            return this;
        }
        
        public IssueBuilder addMetadata(String key, Object value) {
            issue.getMetadata().put(key, value);
            return this;
        }
        
        public Issue build() {
            return issue;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Issue{type='%s', severity='%s', file='%s', line=%d, description='%s'}", 
                           type, severity, file, line, description);
    }
}