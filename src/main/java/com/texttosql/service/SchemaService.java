package com.texttosql.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.texttosql.domain.ColumnMetadata;
import com.texttosql.domain.DatabaseSchema;
import com.texttosql.domain.ForeignKeyMetadata;
import com.texttosql.domain.TableMetadata;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class SchemaService {

    private final JdbcTemplate jdbcTemplate;

        /**
     * Fetches complete database schema including tables, columns, and relationships
     */
    public DatabaseSchema fetchDatabaseSchema() {
        log.info("Fetching database schema metadata");
        
        DatabaseSchema schema = new DatabaseSchema();
        List<TableMetadata> tables = new ArrayList<>();
        
        // Get all user tables from current database
        String tableQuery = """
            SELECT 
                TABLE_SCHEMA,
                TABLE_NAME
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_TYPE = 'BASE TABLE'
            ORDER BY TABLE_NAME
            """;
        
        jdbcTemplate.query(tableQuery, rs -> {
            String schemaName = rs.getString("TABLE_SCHEMA");
            String tableName = rs.getString("TABLE_NAME");
            
            TableMetadata table = TableMetadata.builder()
                .schemaName(schemaName)
                .tableName(tableName)
                .columns(fetchColumns(schemaName, tableName))
                .foreignKeys(fetchForeignKeys(schemaName, tableName))
                .build();
            
            tables.add(table);
        });
        
        schema.setTables(tables);
        log.info("Fetched schema with {} tables", tables.size());
        
        return schema;
    }

    /**
     * Fetches column metadata for a specific table
     */
    private List<ColumnMetadata> fetchColumns(String schemaName, String tableName) {
        List<ColumnMetadata> columns = new ArrayList<>();
        
        String columnQuery = """
            SELECT 
                c.COLUMN_NAME,
                c.DATA_TYPE,
                c.IS_NULLABLE,
                c.COLUMN_KEY
            FROM INFORMATION_SCHEMA.COLUMNS c
            WHERE c.TABLE_SCHEMA = ?
            AND c.TABLE_NAME = ?
            ORDER BY c.ORDINAL_POSITION
            """;
        
        jdbcTemplate.query(columnQuery, 
            new Object[]{schemaName, tableName},
            rs -> {
                ColumnMetadata column = ColumnMetadata.builder()
                    .columnName(rs.getString("COLUMN_NAME"))
                    .dataType(rs.getString("DATA_TYPE"))
                    .nullable("YES".equals(rs.getString("IS_NULLABLE")))
                    .isPrimaryKey("PRI".equals(rs.getString("COLUMN_KEY")))
                    .build();
                columns.add(column);
            });
        
        return columns;
    }

        /**
     * Fetches foreign key relationships for a specific table
     */
    private List<ForeignKeyMetadata> fetchForeignKeys(String schemaName, String tableName) {
        List<ForeignKeyMetadata> foreignKeys = new ArrayList<>();
        
        String fkQuery = """
            SELECT 
                kcu.CONSTRAINT_NAME,
                kcu.COLUMN_NAME,
                kcu.REFERENCED_TABLE_NAME,
                kcu.REFERENCED_COLUMN_NAME
            FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
            WHERE kcu.TABLE_SCHEMA = ?
            AND kcu.TABLE_NAME = ?
            AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
            ORDER BY kcu.ORDINAL_POSITION
            """;
        
        jdbcTemplate.query(fkQuery,
            new Object[]{schemaName, tableName},
            rs -> {
                ForeignKeyMetadata fk = ForeignKeyMetadata.builder()
                    .constraintName(rs.getString("CONSTRAINT_NAME"))
                    .columnName(rs.getString("COLUMN_NAME"))
                    .referencedTable(rs.getString("REFERENCED_TABLE_NAME"))
                    .referencedColumn(rs.getString("REFERENCED_COLUMN_NAME"))
                    .build();
                foreignKeys.add(fk);
            });
        
        return foreignKeys;
    }

}
