package com.texttosql.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.texttosql.config.QueryConfiguration;

import lombok.extern.slf4j.Slf4j;

import java.util.*;


@Service
@Slf4j
public class QueryExecutionService {

    private final JdbcTemplate jdbcTemplate;
    private final QueryConfiguration config;

    public QueryExecutionService(JdbcTemplate jdbcTemplate, QueryConfiguration config) {
        this.jdbcTemplate = jdbcTemplate;
        this.config = config;
    }

    public ExecutionResult executeQuery(String sql) {
        log.info("Executing SQL query");
        
        long startTime = System.currentTimeMillis();
        
        try {
            jdbcTemplate.setQueryTimeout(config.getTimeoutSeconds());
            
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            
            if (rows.size() > config.getMaxRows()) {
                log.warn("Query returned {} rows, limiting to {}", rows.size(), config.getMaxRows());
                rows = new ArrayList<>(rows.subList(0, config.getMaxRows()));
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.info("Query executed successfully. Rows: {}, Time: {}ms", 
                rows.size(), executionTime);
            
            return new ExecutionResult(true, rows, executionTime, null);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Query execution failed", e);
            
            String errorMessage = "Query execution failed: " + e.getMessage();
            return new ExecutionResult(false, new ArrayList<>(), executionTime, errorMessage);
        }
    }
}
