package com.texttosql.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.texttosql.config.QueryConfiguration;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;


@Slf4j
@Service
@RequiredArgsConstructor
public class SQLValidationService {
    private final QueryConfiguration config;

    /**
     * Validates that SQL query is safe and read-only
     * 
     * @param sql The SQL query to validate
     * @return ValidationResult with isValid flag and message
     */
    public ValidationResult validate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new ValidationResult(false, "SQL query is empty");
        }

        String upperSql = sql.toUpperCase().trim();

        // Check for blocked keywords
        for (String keyword : config.getBlockedKeywords()) {
            if (containsKeyword(upperSql, keyword)) {
                log.warn("Blocked keyword detected: {}", keyword);
                return new ValidationResult(false, 
                    "Query contains blocked operation: " + keyword + ". Only SELECT queries are allowed.");
            }
        }

        // Ensure query starts with SELECT
        if (!startsWithSelect(upperSql)) {
            log.warn("Query does not start with SELECT");
            return new ValidationResult(false, 
                "Only SELECT queries are allowed. Query must start with SELECT.");
        }

        // Check for command separators (trying to run multiple statements)
        if (sql.contains(";") && !sql.trim().endsWith(";")) {
            log.warn("Multiple statements detected");
            return new ValidationResult(false, 
                "Multiple SQL statements are not allowed");
        }

        // Check for SQL comments that might hide malicious code
        if (containsComments(sql)) {
            log.warn("SQL comments detected");
            return new ValidationResult(false, 
                "SQL comments are not allowed for security reasons");
        }

        log.info("SQL validation passed");
        return new ValidationResult(true, "Query is valid and safe");
    }

    /**
     * Check if SQL contains a specific keyword as a standalone word
     */
    private boolean containsKeyword(String sql, String keyword) {
        // Use word boundaries to match whole words only
        String pattern = "\\b" + Pattern.quote(keyword) + "\\b";
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(sql).find();
    }

    /**
     * Check if SQL starts with SELECT
     */
    private boolean startsWithSelect(String sql) {
        // Remove leading whitespace and check
        return sql.matches("^\\s*SELECT\\b.*");
    }

    /**
     * Check for SQL comments
     */
    private boolean containsComments(String sql) {
        // Check for -- comments
        if (sql.contains("--")) {
            return true;
        }
        
        // Check for /* */ comments
        return sql.contains("/*") || sql.contains("*/");
    }

}
