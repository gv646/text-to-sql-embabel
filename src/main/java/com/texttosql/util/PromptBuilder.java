package com.texttosql.util;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.texttosql.domain.ColumnMetadata;
import com.texttosql.domain.QueryRequest;
import com.texttosql.domain.RelevantTables;
import com.texttosql.domain.TableMetadata;

/**
     * Utility class for building the LLM prompts
     */

@Component
public class PromptBuilder {
    /**
     * Build table identifiaction prompt
     */

    public String buildTableIdentificationPrompt(QueryRequest request){
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a database expert. Identify the most relevant tables for this query.\n\n");
        prompt.append("User Query: ").append(request.getNaturalLanguageQuery()).append("\n\n");
        prompt.append("Available Tables:\n");
        
        for (TableMetadata table : request.getSchema().getTables()) {
            prompt.append("- ").append(table.getFullTableName()).append(": ");
            prompt.append(table.getColumns().stream()
                .map(ColumnMetadata::getColumnName)
                .collect(Collectors.joining(", ")));
            prompt.append("\n");
        }

        prompt.append("\nRespond with ONLY the table names, comma-separated.\n");
        prompt.append("Example: testdb.users, testdb.orders\n");


        return prompt.toString();


    }

        /**
     * Build prompt for SQL generation
     */
    public String buildSQLGenerationPrompt(RelevantTables tables) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Generate a MySQL SELECT query.\n\n");
        prompt.append("User wants: ").append(tables.getOriginalRequest().getNaturalLanguageQuery()).append("\n\n");
        prompt.append("Tables to use:\n");
        
        for (TableMetadata table : tables.getOriginalRequest().getSchema().getTables()) {
            if (tables.getTableNames().contains(table.getFullTableName())) {
                prompt.append("\nTable: ").append(table.getFullTableName()).append("\n");
                prompt.append("Columns:\n");
                for (ColumnMetadata col : table.getColumns()) {
                    prompt.append("  - ").append(col.getColumnName())
                        .append(" (").append(col.getDataType()).append(")");
                    if (col.isPrimaryKey()) prompt.append(" [PRIMARY KEY]");
                    prompt.append("\n");
                }
            }
        }
        
        prompt.append("\nRULES:\n");
        prompt.append("1. ONLY SELECT statements\n");
        prompt.append("2. Add LIMIT 500\n");
        prompt.append("3. Use proper MySQL syntax\n");
        prompt.append("4. Use JOINs if multiple tables are needed\n");
        prompt.append("5. Respond with ONLY the SQL query, nothing else\n");
        prompt.append("\nSQL:");
        
        return prompt.toString();
    }
}
