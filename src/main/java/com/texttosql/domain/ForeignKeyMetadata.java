package com.texttosql.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a foreign key relationship
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForeignKeyMetadata {
    private String constraintName;
    private String columnName;
    private String referencedTable;
    private String referencedColumn;
}