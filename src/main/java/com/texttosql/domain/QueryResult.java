package com.texttosql.domain;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class QueryResult {
    
    private String originalQuery;
    private String generatedSQL;
    private List<Map<String,Object>> rows;
    private int rowCount;
    private long executionTimeMS;
    private boolean success;
    private String errorMessage;
}
