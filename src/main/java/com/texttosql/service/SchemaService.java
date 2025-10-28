package com.texttosql.service;

import com.texttosql.domain.ColumnMetadata;
import com.texttosql.domain.DatabaseSchema;
import com.texttosql.domain.ForeignKeyMetadata;
import com.texttosql.domain.TableMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for fetching and managing database schema metadata
 */
@Service
@Slf4j
public class SchemaService {

    private final JdbcTemplate jdbcTemplate;

    public SchemaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Fetches complete database schema including tables, columns, and relationships
     */
    public DatabaseSchema fetchDatabaseSchema() {
        log.info("Fetching database schema metadata");
        
        DatabaseSchema schema = new DatabaseSchema();
        List<TableMetadata> tables = new ArrayList<>();
        
        // Get all user tables
        String tableQuery = """
            SELECT 
                TABLE_SCHEMA,
                TABLE_NAME
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_TYPE = 'BASE TABLE'
            AND TABLE_SCHEMA NOT IN ('sys', 'INFORMATION_SCHEMA')
            ORDER BY TABLE_SCHEMA, TABLE_NAME
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
        
        return jdbcTemplate.query(columnQuery, 
            (rs, rowNum) -> new ColumnMetadata(
                rs.getString("COLUMN_NAME"),
                rs.getString("DATA_TYPE"),
                "YES".equals(rs.getString("IS_NULLABLE")),
                "PRI".equals(rs.getString("COLUMN_KEY"))
            ),
            schemaName, tableName);
    }

    /**
     * Fetches foreign key relationships for a specific table
     */
    private List<ForeignKeyMetadata> fetchForeignKeys(String schemaName, String tableName) {
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
        
        return jdbcTemplate.query(fkQuery,
            (rs, rowNum) -> new ForeignKeyMetadata(
                rs.getString("CONSTRAINT_NAME"),
                rs.getString("COLUMN_NAME"),
                rs.getString("REFERENCED_TABLE_NAME"),
                rs.getString("REFERENCED_COLUMN_NAME")
            ),
            schemaName, tableName);
    }
}