package com.somdiproy.lambda.detection;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.somdiproy.lambda.detection.model.DetectionRequest;
import com.somdiproy.lambda.detection.model.DetectionResponse;
import com.somdiproy.lambda.detection.model.Issue;
import com.somdiproy.lambda.detection.service.NovaInvokerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.somdiproy.lambda.detection.util.TokenOptimizer;
//import com.somdiproy.smartcodereview.service.SessionService;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Lambda function for Nova Lite issue detection Second tier of the three-tier
 * analysis system Handler:
 * com.somdiproy.lambda.detection.DetectionHandler::handleRequest
 */
public class DetectionHandler implements RequestHandler<DetectionRequest, DetectionResponse> {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DetectionHandler.class);
    
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final NovaInvokerService novaInvoker;

	// Configuration from environment variables
	private static final String MODEL_ID = System.getenv("MODEL_ID"); // amazon.nova-lite-v1:0
	private static final String BEDROCK_REGION = System.getenv("BEDROCK_REGION"); // us-east-1
	private static final int MAX_TOKENS = Integer.parseInt(System.getenv("MAX_TOKENS")); // 4000
	private static final double DETECTION_THRESHOLD = Double.parseDouble(System.getenv("DETECTION_THRESHOLD")); // 0.7

	// Analysis categories
	private static final Set<String> ANALYSIS_CATEGORIES = Set.of("security", "performance", "quality",
			"best-practices");

	private static ExecutorService executorService;
	private static final Object lock = new Object();

	public DetectionHandler() {
		this.novaInvoker = new NovaInvokerService(BEDROCK_REGION);
		// Initialize executor service with proper lifecycle management
		initializeExecutorService();
	}

	private void initializeExecutorService() {
		synchronized (lock) {
			if (executorService == null || executorService.isShutdown()) {
				// Increase thread pool size for parallel processing
				int threadCount = Math.min(4, Runtime.getRuntime().availableProcessors());
				executorService = Executors.newFixedThreadPool(threadCount, r -> {
					Thread t = new Thread(r);
					t.setDaemon(true); // Allow JVM to exit
					t.setName("nova-detection-worker-" + t.getId());
					return t;
				});
			}
		}
	}

	@Override
	public DetectionResponse handleRequest(DetectionRequest request, Context context) {
		LambdaLogger logger = context.getLogger();
		logger.log("🎯 Starting Nova Lite issue detection process");

		// Ensure executor service is available
		initializeExecutorService();

		// Add context listener for cleanup
		context.getRemainingTimeInMillis(); // Trigger context awareness

		DetectionResponse.ProcessingTime processingTime = new DetectionResponse.ProcessingTime();
		long startTime = System.currentTimeMillis();

		try {
			// Validate input
			if (!request.isValid()) {
				return DetectionResponse.error(request.getAnalysisId(), request.getSessionId(),
						"Invalid request: missing required fields");
			}

			List<DetectionRequest.FileInput> files = request.getFiles();
			logger.log(String.format("📝 Analyzing %d files for issues: %s", files.size(), request.getAnalysisId()));

			// Process files in parallel batches
			List<Issue> allIssues = new ArrayList<>();
			List<Future<List<Issue>>> futures = new ArrayList<>();

			// Submit analysis tasks
			for (DetectionRequest.FileInput file : files) {
				Future<List<Issue>> future = executorService.submit(() -> analyzeFile(file, request, logger));
				futures.add(future);
			}

			// Collect results
			int filesAnalyzed = 0;
			for (Future<List<Issue>> future : futures) {
				try {
					// Dynamic timeout based on Lambda context
					int remainingTime = context.getRemainingTimeInMillis();
					int safetyBuffer = 30000; // 30 seconds safety buffer
					int timeoutPerFile = Math.max(10000, (remainingTime - safetyBuffer) / futures.size());
					List<Issue> fileIssues = future.get(timeoutPerFile, TimeUnit.MILLISECONDS);
					allIssues.addAll(fileIssues);
					filesAnalyzed++;
				} catch (TimeoutException e) {
					logger.log("⚠️ File analysis timed out");
				} catch (Exception e) {
					logger.log("⚠️ Error analyzing file: " + e.getMessage());
				}
			}

			// Deduplicate and prioritize issues
			List<Issue> prioritizedIssues = prioritizeIssues(allIssues);

			logger.log(String.format("✅ Detection complete: %d issues found in %d files", prioritizedIssues.size(),
					filesAnalyzed));

			// Calculate metrics
			processingTime.setTotal(System.currentTimeMillis() - startTime);

			Map<String, Integer> categoryCounts = countByCategory(prioritizedIssues);
			Map<String, Integer> severityCounts = countBySeverity(prioritizedIssues);

			// Build response
			return DetectionResponse.builder().analysisId(request.getAnalysisId()).sessionId(request.getSessionId())
					.status("success").filesAnalyzed(filesAnalyzed).totalFiles(files.size())
					.issuesFound(prioritizedIssues.size()).issues(prioritizedIssues)
					.summary(buildSummary(prioritizedIssues, categoryCounts, severityCounts))
					.processingTime(processingTime).metadata(buildMetadata(request)).build();

		} catch (Exception e) {
			logger.log("❌ Detection failed: " + e.getMessage());
			return DetectionResponse.error(request.getAnalysisId(), request.getSessionId(),
					"Detection failed: " + e.getMessage());
		} finally {
			// Cleanup
			executorService.shutdown();
		}
	}

	private void shutdownExecutorGracefully() {
		if (executorService != null && !executorService.isShutdown()) {
			try {
				executorService.shutdown();
				if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
					if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
						log.warn("ExecutorService did not terminate gracefully");
					}
				}
			} catch (InterruptedException e) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Analyze a single file for issues
	 */
	private List<Issue> analyzeFile(DetectionRequest.FileInput file, DetectionRequest request, LambdaLogger logger) {
		List<Issue> issues = new ArrayList<>();

		try {
			logger.log("🔍 Analyzing file: " + file.getPath());

			// Security analysis
			issues.addAll(performSecurityAnalysis(file, logger));

			// Performance analysis
			issues.addAll(performPerformanceAnalysis(file, logger));

			// Code quality analysis
			issues.addAll(performQualityAnalysis(file, logger));

			// Language-specific best practices
			issues.addAll(performBestPracticesAnalysis(file, logger));

			logger.log(String.format("✓ File analysis complete: %d issues found in %s", issues.size(), file.getPath()));

		} catch (Exception e) {
			logger.log("⚠️ Error analyzing file " + file.getPath() + ": " + e.getMessage());
		}

		return issues;
	}

	/**
	 * Perform security vulnerability analysis
	 */
	private List<Issue> performSecurityAnalysis(DetectionRequest.FileInput file, LambdaLogger logger) {
		List<Issue> issues = new ArrayList<>();

		try {
			String prompt = buildSecurityAnalysisPrompt(file);
			NovaInvokerService.NovaResponse response = novaInvoker.invokeNovaLite(prompt, null);

			if (response.isSuccessful()) {
				issues.addAll(parseSecurityIssues(response.getResponseText(), file));
			}
		} catch (Exception e) {
			logger.log("Security analysis failed: " + e.getMessage());
		}

		return issues;
	}

	/**
	 * Perform performance analysis
	 */
	private List<Issue> performPerformanceAnalysis(DetectionRequest.FileInput file, LambdaLogger logger) {
		List<Issue> issues = new ArrayList<>();

		try {
			String prompt = buildPerformanceAnalysisPrompt(file);
			NovaInvokerService.NovaResponse response = novaInvoker.invokeNovaLite(prompt, null);

			if (response.isSuccessful()) {
				issues.addAll(parsePerformanceIssues(response.getResponseText(), file));
			}
		} catch (Exception e) {
			logger.log("Performance analysis failed: " + e.getMessage());
		}

		return issues;
	}

	/**
	 * Perform code quality analysis
	 */
	private List<Issue> performQualityAnalysis(DetectionRequest.FileInput file, LambdaLogger logger) {
		List<Issue> issues = new ArrayList<>();

		try {
			String prompt = buildQualityAnalysisPrompt(file);
			NovaInvokerService.NovaResponse response = novaInvoker.invokeNovaLite(prompt, null);

			if (response.isSuccessful()) {
				issues.addAll(parseQualityIssues(response.getResponseText(), file));
			}
		} catch (Exception e) {
			logger.log("Quality analysis failed: " + e.getMessage());
		}

		return issues;
	}

	/**
	 * Perform best practices analysis
	 */
	private List<Issue> performBestPracticesAnalysis(DetectionRequest.FileInput file, LambdaLogger logger) {
		List<Issue> issues = new ArrayList<>();

		try {
			String prompt = buildBestPracticesPrompt(file);
			NovaInvokerService.NovaResponse response = novaInvoker.invokeNovaLite(prompt, null);

			if (response.isSuccessful()) {
				issues.addAll(parseBestPracticesIssues(response.getResponseText(), file));
			}
		} catch (Exception e) {
			logger.log("Best practices analysis failed: " + e.getMessage());
		}

		return issues;
	}

	/**
	 * Build security analysis prompt
	 */
	private String buildSecurityAnalysisPrompt(DetectionRequest.FileInput file) {
	    return String.format("""
	            Analyze this %s code for security vulnerabilities. Focus on:
	            - SQL Injection vulnerabilities
	            - Cross-Site Scripting (XSS) risks
	            - Insecure deserialization
	            - Hardcoded credentials or secrets
	            - Cryptographic weaknesses
	            - Authentication/authorization flaws
	            - Path traversal vulnerabilities

	            For each issue found, provide:
	            1. Issue type (e.g., "SQL_INJECTION")
	            2. Severity (critical/high/medium/low)
	            3. Line number(s) affected
	            4. Detailed description explaining impact and context (2-3 sentences)
	            5. The vulnerable code snippet

	            DESCRIPTION FORMAT: Start with the vulnerability type, explain the security risk, 
	            describe the potential impact (e.g., "SQL Injection vulnerability allows attackers 
	            to manipulate database queries. This could lead to unauthorized data access, 
	            modification, or deletion. The user input is directly concatenated into the SQL query without validation.")

	            Return results in this exact format:
	            ISSUE_START
	            type: [ISSUE_TYPE]
	            severity: [SEVERITY]
	            line: [LINE_NUMBER]
	            description: [DETAILED_DESCRIPTION_WITH_IMPACT]
	            code: [CODE_SNIPPET]
	            ISSUE_END

	            Code to analyze:
	            %s
	            """, file.getLanguage(), file.getOptimizedContent());
	}

	/**
	 * Build performance analysis prompt
	 */
	private String buildPerformanceAnalysisPrompt(DetectionRequest.FileInput file) {
		return String.format("""
				Analyze this %s code for performance issues. Focus on:
				- Time complexity problems (O(n²) or worse)
				- Memory leaks or excessive allocation
				- Inefficient database queries
				- Blocking I/O operations
				- Unnecessary loops or recursion
				- Missing caching opportunities

				For each issue found, provide:
				1. Issue type (e.g., "INEFFICIENT_LOOP")
				2. Severity (high/medium/low)
				3. Line number(s) affected
				4. Performance impact description
				5. The problematic code snippet

				Return results in this exact format:
				ISSUE_START
				type: [ISSUE_TYPE]
				severity: [SEVERITY]
				line: [LINE_NUMBER]
				description: [DESCRIPTION]
				code: [CODE_SNIPPET]
				ISSUE_END

				Code to analyze:
				%s
				""", file.getLanguage(), file.getOptimizedContent());
	}

	/**
	 * Build code quality analysis prompt
	 */
	private String buildQualityAnalysisPrompt(DetectionRequest.FileInput file) {
		return String.format("""
				Analyze this %s code for quality issues. Focus on:
				- Code duplication (DRY violations)
				- High cyclomatic complexity
				- Poor naming conventions
				- Missing error handling
				- Lack of documentation
				- Code smells and anti-patterns

				For each issue found, provide:
				1. Issue type (e.g., "CODE_DUPLICATION")
				2. Severity (medium/low)
				3. Line number(s) affected
				4. Quality impact description
				5. The problematic code snippet

				Return results in this exact format:
				ISSUE_START
				type: [ISSUE_TYPE]
				severity: [SEVERITY]
				line: [LINE_NUMBER]
				description: [DESCRIPTION]
				code: [CODE_SNIPPET]
				ISSUE_END

				Code to analyze:
				%s
				""", file.getLanguage(), file.getOptimizedContent());
	}

	/**
	 * Build best practices prompt
	 */
	private String buildBestPracticesPrompt(DetectionRequest.FileInput file) {
		return String.format("""
				Analyze this %s code for violations of language-specific best practices. Focus on:
				- Language idioms and conventions
				- Framework-specific patterns
				- Resource management
				- Exception handling patterns
				- Concurrency issues
				- Dependency management

				For each issue found, provide:
				1. Issue type (e.g., "RESOURCE_LEAK")
				2. Severity (medium/low)
				3. Line number(s) affected
				4. Best practice description
				5. The problematic code snippet

				Return results in this exact format:
				ISSUE_START
				type: [ISSUE_TYPE]
				severity: [SEVERITY]
				line: [LINE_NUMBER]
				description: [DESCRIPTION]
				code: [CODE_SNIPPET]
				ISSUE_END

				Code to analyze:
				%s
				""", file.getLanguage(), file.getOptimizedContent());
	}

	/**
	 * Parse security issues from Nova response
	 */
	private List<Issue> parseSecurityIssues(String response, DetectionRequest.FileInput file) {
		return parseIssuesFromResponse(response, file, "security");
	}

	/**
	 * Parse performance issues from Nova response
	 */
	private List<Issue> parsePerformanceIssues(String response, DetectionRequest.FileInput file) {
		return parseIssuesFromResponse(response, file, "performance");
	}

	/**
	 * Parse quality issues from Nova response
	 */
	private List<Issue> parseQualityIssues(String response, DetectionRequest.FileInput file) {
		return parseIssuesFromResponse(response, file, "quality");
	}

	/**
	 * Parse best practices issues from Nova response
	 */
	private List<Issue> parseBestPracticesIssues(String response, DetectionRequest.FileInput file) {
		return parseIssuesFromResponse(response, file, "best-practices");
	}

	/**
	 * Generic issue parser
	 */
	private List<Issue> parseIssuesFromResponse(String response, DetectionRequest.FileInput file, String category) {
		List<Issue> issues = new ArrayList<>();

		String[] issueBlocks = response.split("ISSUE_START");
		for (String block : issueBlocks) {
			if (block.contains("ISSUE_END")) {
				try {
					Issue issue = parseIssueBlock(block, file, category);
					if (issue != null && issue.getConfidence() >= DETECTION_THRESHOLD) {
						issues.add(issue);
					}
				} catch (Exception e) {
					// Skip malformed issue blocks
				}
			}
		}

		return issues;
	}

	/**
	 * Parse individual issue block
	 */
	private Issue parseIssueBlock(String block, DetectionRequest.FileInput file, String category) {
		Map<String, String> fields = new HashMap<>();
		String[] lines = block.split("\n");

		for (String line : lines) {
			if (line.contains(":")) {
				String[] parts = line.split(":", 2);
				if (parts.length == 2) {
					fields.put(parts[0].trim(), parts[1].trim());
				}
			}
		}

		if (!fields.containsKey("type") || !fields.containsKey("severity")) {
			return null;
		}

		int lineNumber = parseLineNumber(fields.get("line"));
		// Only create issue if we have a valid line number (not -1)
		if (lineNumber <= 0) {
		    return null;  // ← Skip issues without valid line numbers
		}

		return Issue.builder()
		    .id(UUID.randomUUID().toString())
		    .type(fields.get("type"))
		    .category(category)
		    .severity(fields.get("severity"))
		    .confidence(calculateConfidence(fields))
		    .file(file.getPath())
		    .line(lineNumber)
		    .column(0)
		    .description(fields.get("description"))
		    .codeSnippet(fields.get("code"))
		    .language(file.getLanguage())
		    .build();
	}

	/**
	 * Calculate confidence score based on issue details
	 */
	private double calculateConfidence(Map<String, String> fields) {
		double confidence = 0.8; // Base confidence

		// Adjust based on severity
		String severity = fields.get("severity");
		if ("critical".equals(severity))
			confidence += 0.15;
		else if ("high".equals(severity))
			confidence += 0.1;
		else if ("medium".equals(severity))
			confidence += 0.05;

		// Adjust based on specificity
		if (fields.containsKey("line") && !fields.get("line").equals("0")) {
			confidence += 0.05;
		}

		return Math.min(confidence, 1.0);
	}

	/**
	 * Parse line number from string
	 */
	private int parseLineNumber(String lineStr) {
	    if (lineStr == null || lineStr.isEmpty() || lineStr.equals("0"))
	        return -1;  // ← Return -1 to indicate invalid/unknown line
	    try {
	        // Handle ranges like "10-15"
	        if (lineStr.contains("-")) {
	            return Integer.parseInt(lineStr.split("-")[0]);
	        }
	        return Integer.parseInt(lineStr);
	    } catch (NumberFormatException e) {
	        return -1;  // ← Return -1 for unparseable line numbers
	    }
	}

	/**
	 * Prioritize and deduplicate issues
	 */
	private List<Issue> prioritizeIssues(List<Issue> allIssues) {
		// Remove duplicates based on file, line, and type
		Map<String, Issue> uniqueIssues = new HashMap<>();

		for (Issue issue : allIssues) {
			String key = issue.getFile() + ":" + issue.getLine() + ":" + issue.getType();
			if (!uniqueIssues.containsKey(key) || issue.getConfidence() > uniqueIssues.get(key).getConfidence()) {
				uniqueIssues.put(key, issue);
			}
		}

		// Sort by priority: severity, confidence, then category
		return uniqueIssues.values().stream().sorted((a, b) -> {
			int severityCompare = compareSeverity(b.getSeverity(), a.getSeverity());
			if (severityCompare != 0)
				return severityCompare;

			int confidenceCompare = Double.compare(b.getConfidence(), a.getConfidence());
			if (confidenceCompare != 0)
				return confidenceCompare;

			return a.getCategory().compareTo(b.getCategory());
		}).collect(Collectors.toList());
	}

	/**
	 * Compare severity levels
	 */
	private int compareSeverity(String a, String b) {
		Map<String, Integer> severityOrder = Map.of("critical", 4, "high", 3, "medium", 2, "low", 1);

		return Integer.compare(severityOrder.getOrDefault(a, 0), severityOrder.getOrDefault(b, 0));
	}

	/**
	 * Count issues by category
	 */
	private Map<String, Integer> countByCategory(List<Issue> issues) {
		return issues.stream().collect(Collectors.groupingBy(Issue::getCategory, Collectors.summingInt(e -> 1)));
	}

	/**
	 * Count issues by severity
	 */
	private Map<String, Integer> countBySeverity(List<Issue> issues) {
		return issues.stream().collect(Collectors.groupingBy(Issue::getSeverity, Collectors.summingInt(e -> 1)));
	}

	/**
	 * Build analysis summary
	 */
	private DetectionResponse.Summary buildSummary(List<Issue> issues, Map<String, Integer> categoryCounts,
			Map<String, Integer> severityCounts) {
		DetectionResponse.Summary summary = new DetectionResponse.Summary();
		summary.setTotalIssues(issues.size());
		summary.setCriticalCount(severityCounts.getOrDefault("critical", 0));
		summary.setHighCount(severityCounts.getOrDefault("high", 0));
		summary.setMediumCount(severityCounts.getOrDefault("medium", 0));
		summary.setLowCount(severityCounts.getOrDefault("low", 0));
		summary.setSecurityCount(categoryCounts.getOrDefault("security", 0));
		summary.setPerformanceCount(categoryCounts.getOrDefault("performance", 0));
		summary.setQualityCount(categoryCounts.getOrDefault("quality", 0));
		summary.setBestPracticesCount(categoryCounts.getOrDefault("best-practices", 0));

		// Top issues
		summary.setTopIssues(
				issues.stream().limit(5)
						.map(issue -> String.format("%s: %s (%s:%d)", issue.getSeverity().toUpperCase(),
								issue.getDescription(), issue.getFile(), issue.getLine()))
						.collect(Collectors.toList()));

		return summary;
	}

	/**
	 * Build response metadata
	 */
	private Map<String, Object> buildMetadata(DetectionRequest request) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("repository", request.getRepository());
		metadata.put("branch", request.getBranch());
		metadata.put("scanNumber", request.getScanNumber());
		metadata.put("detectionThreshold", DETECTION_THRESHOLD);
		metadata.put("modelId", MODEL_ID);
		metadata.put("timestamp", System.currentTimeMillis());
		return metadata;
	}
}