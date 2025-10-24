package com.texttosql.service;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExecutionResult {

    private boolean success;
    private List<Map<String, Object>> rows;
    private long executionTimeMs;
    private String errorMessage;

}
