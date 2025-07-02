package com.somdiproy.lambda.detection.util;

import java.util.regex.Pattern;

/**
 * Utility class for optimizing code content to minimize token usage
 */
public class TokenOptimizer {
    
    // Patterns for removing comments
    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("//.*$", Pattern.MULTILINE);
    private static final Pattern PYTHON_COMMENT = Pattern.compile("#.*$", Pattern.MULTILINE);
    private static final Pattern EXTRA_WHITESPACE = Pattern.compile("\\s+");
    
    /**
     * Optimize content for Nova Lite detection (30K tokens budget)
     */
    public static String optimizeForDetection(String content, String language) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        String optimized = content;
        
        // Remove comments based on language
        if ("java".equalsIgnoreCase(language) || "javascript".equalsIgnoreCase(language) 
            || "typescript".equalsIgnoreCase(language) || "c".equalsIgnoreCase(language)
            || "cpp".equalsIgnoreCase(language) || "csharp".equalsIgnoreCase(language)) {
            optimized = MULTI_LINE_COMMENT.matcher(optimized).replaceAll("");
            optimized = SINGLE_LINE_COMMENT.matcher(optimized).replaceAll("");
        } else if ("python".equalsIgnoreCase(language) || "ruby".equalsIgnoreCase(language)) {
            optimized = PYTHON_COMMENT.matcher(optimized).replaceAll("");
        }
        
        // Normalize whitespace
        optimized = EXTRA_WHITESPACE.matcher(optimized).replaceAll(" ");
        optimized = optimized.trim();
        
        
        // Limit content size (approx 40K chars for larger files)
        if (optimized.length() > 40000) {
            optimized = optimized.substring(0, 40000);
        }
        
        return optimized;
    }
}