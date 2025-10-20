package com.texttosql.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedSQL {

    private String sqlQuery;
    private String explanation;
    private RelevantTables relevantTables;
    
}
