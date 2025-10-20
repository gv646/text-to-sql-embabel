package com.texttosql.domain;

import java.util.ArrayList;
import java.util.List;

import groovy.transform.builder.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Represents the complete database schema metadata
 * Contains all tables in the database
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseSchema {
    private List<TableMetadata> tables;

    public DatabaseSchema(){
        this.tables = new ArrayList<>();
    }
    
}
