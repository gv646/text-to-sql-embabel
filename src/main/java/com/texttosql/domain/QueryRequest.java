package com.texttosql.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the user's natural language query combined with database schema
 * This is the input to the "identify relevant tables" action
 */

 @Data
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor
public class QueryRequest {

    private String naturalLanguageQuery;
    private DatabaseSchema schema;
}
