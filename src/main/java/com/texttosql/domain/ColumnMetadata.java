package com.texttosql.domain;

import groovy.transform.builder.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMetadata {
    private String columnName;
    private String dataType;
    private boolean nullable;
    private boolean isPrimaryKey;
}
