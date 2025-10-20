package com.texttosql.domain;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableMetadata {
    private String tableName;
    private String schemaName;
    private List<ColumnMetadata> columns;
    private List<ForeignKeyMetadata> foreignKeys;


    /**
     * Returns fully qualified table name (schema.table)
     * Example: "dbo.customers" or just "customers"
     */

    public String getFullTableName() {
        return schemaName != null ? schemaName + "." + tableName : tableName;
    }

    
}

