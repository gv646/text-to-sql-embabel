package com.texttosql.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
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
