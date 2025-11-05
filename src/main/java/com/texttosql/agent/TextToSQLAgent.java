package com.texttosql.agent;

import com.embabel.agent.api.annotation.*;
import com.embabel.agent.api.common.OperationContext;
import com.texttosql.domain.*;
import com.texttosql.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * GenAI-powered agent that converts natural language to SQL using Ollama LLM
 */
@Agent(description = "Converts natural language queries to SQL and executes them")
@Slf4j
@Component
public class TextToSQLAgent {

    private final SchemaService schemaService;
    private final SQLValidationService validationService;
    private final QueryExecutionService executionService;

    public TextToSQLAgent(SchemaService schemaService,
                         SQLValidationService validationService,
                         QueryExecutionService executionService) {
        this.schemaService = schemaService;
        this.validationService = validationService;
        this.executionService = executionService;
    }

    // @AchievesGoal(description = "Convert natural language to SQL and execute it")
    // public QueryResult processQuery(String naturalLanguageQuery) {
    //     log.info("🎯 Goal: Process query: {}", naturalLanguageQuery);
    //     return null;
    // }

    @Action(description = "Fetch database schema metadata")
    public QueryRequest fetchSchema(String naturalLanguageQuery) {
        log.info("📊 Action: Fetching schema");
        DatabaseSchema schema = schemaService.fetchDatabaseSchema();
        log.info("✅ Found {} tables", schema.getTables().size());
        
        return QueryRequest.builder()
            .naturalLanguageQuery(naturalLanguageQuery)
            .schema(schema)
            .build();
    }

    @Action(description = "Use AI to identify relevant database tables")
    public RelevantTables identifyRelevantTables(QueryRequest request, OperationContext context) {
        log.info("🧠 Action: LLM identifying relevant tables");
        
        String prompt = buildTableIdentificationPrompt(request);
        
        String llmResponse = context.ai()
            .withAutoLlm()
            .generateText(prompt);
        
        log.info("📝 LLM Response (tables): {}", llmResponse);
        
        RelevantTables tables = parseTableResponse(llmResponse, request);
        log.info("✅ LLM identified: {}", tables.getTableNames());
        
        return tables;
    }

    @Action(description = "Use AI to generate SQL query")
    public GeneratedSQL generateSQL(RelevantTables tables, OperationContext context) {
        log.info("🧠 Action: LLM generating SQL");
        
        String prompt = buildSQLGenerationPrompt(tables);
        
        String llmResponse = context.ai()
            .withAutoLlm()
            .generateText(prompt);
        
        log.info("📝 LLM Response (SQL): {}", llmResponse);
        
        GeneratedSQL sql = parseSQLResponse(llmResponse, tables);
        log.info("✅ LLM generated: {}", sql.getSqlQuery());
        
        return sql;
    }

    @Action(description = "Validate SQL query for safety")
    public SQLValidation validateSQL(GeneratedSQL sql) {
        log.info("🔒 Action: Validating SQL");
        ValidationResult result = validationService.validate(sql.getSqlQuery());
        
        return SQLValidation.builder()
            .isValid(result.isValid())
            .validationMessage(result.getMessage())
            .generatedSQL(sql)
            .build();
    }

    @AchievesGoal(description = "Convert natural language to SQL and execute it")
    @Action(description = "Execute SQL query against database")
    public QueryResult executeSQL(SQLValidation validation) {
        if (!validation.isValid()) {
            log.error("❌ SQL validation failed");
            return buildErrorResult(validation);
        }
        
        log.info("⚡ Action: Executing SQL");
        ExecutionResult result = executionService.executeQuery(
            validation.getGeneratedSQL().getSqlQuery()
        );
        
        log.info("✅ Returned {} rows in {}ms", 
            result.getRows() != null ? result.getRows().size() : 0,
            result.getExecutionTimeMs());
        
        return buildSuccessResult(validation, result);
    }

    @Condition
    public boolean schemaFetched(QueryRequest request) {
        return request != null && 
               request.getSchema() != null && 
               !request.getSchema().getTables().isEmpty();
    }

    @Condition
    public boolean tablesIdentified(RelevantTables tables) {
        return tables != null && !tables.getTableNames().isEmpty();
    }

    @Condition
    public boolean sqlGenerated(GeneratedSQL sql) {
        return sql != null && sql.getSqlQuery() != null;
    }

    @Condition
    public boolean sqlValidated(SQLValidation validation) {
        return validation != null;
    }

    @Condition
    public boolean sqlValid(SQLValidation validation) {
        return validation != null && validation.isValid();
    }

    // ============================================================
    // IMPROVED PROMPT BUILDING
    // ============================================================

    private String buildTableIdentificationPrompt(QueryRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Identify the relevant table(s) for this query.\n\n");
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

    private String buildSQLGenerationPrompt(RelevantTables tables) {
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
        prompt.append("4. Respond with ONLY the SQL query, nothing else\n");
        prompt.append("\nSQL:");
        
        return prompt.toString();
    }

    // ============================================================
    // IMPROVED RESPONSE PARSING - More flexible!
    // ============================================================

    private RelevantTables parseTableResponse(String llmResponse, QueryRequest request) {
        List<String> tableNames = new ArrayList<>();
        
        // Try to extract table names from response
        for (TableMetadata table : request.getSchema().getTables()) {
            String fullName = table.getFullTableName();
            if (llmResponse.toLowerCase().contains(fullName.toLowerCase())) {
                tableNames.add(fullName);
            }
        }
        
        // Fallback: use all tables
        if (tableNames.isEmpty()) {
            log.warn("⚠️ Could not parse table names, using all tables");
            for (TableMetadata table : request.getSchema().getTables()) {
                tableNames.add(table.getFullTableName());
            }
        }
        
        return RelevantTables.builder()
            .tableNames(tableNames)
            .reasoning("Extracted from LLM response")
            .originalRequest(request)
            .build();
    }

    private GeneratedSQL parseSQLResponse(String llmResponse, RelevantTables tables) {
        String sql = extractSQL(llmResponse);
        
        // Fallback if no valid SQL found
        if (sql == null || sql.trim().isEmpty()) {
            log.warn("⚠️ LLM response parsing failed, using fallback query");
            sql = "SELECT * FROM " + tables.getTableNames().get(0) + " LIMIT 500";
        }
        
        return GeneratedSQL.builder()
            .sqlQuery(sql)
            .explanation("Generated by LLM")
            .relevantTables(tables)
            .build();
    }

    /**
     * Flexible SQL extraction that handles various LLM response formats
     */
    private String extractSQL(String llmResponse) {
        // Method 1: Look for SELECT ... LIMIT pattern
        Pattern selectPattern = Pattern.compile("(SELECT\\s+.+?LIMIT\\s+\\d+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = selectPattern.matcher(llmResponse);
        if (matcher.find()) {
            return cleanSQL(matcher.group(1));
        }
        
        // Method 2: Look for any SELECT statement
        Pattern anySelectPattern = Pattern.compile("(SELECT\\s+.+?(?:;|$))", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        matcher = anySelectPattern.matcher(llmResponse);
        if (matcher.find()) {
            String sql = cleanSQL(matcher.group(1));
            // Add LIMIT if missing
            if (!sql.toUpperCase().contains("LIMIT")) {
                sql += " LIMIT 500";
            }
            return sql;
        }
        
        // Method 3: Look for SQL: prefix
        if (llmResponse.contains("SQL:")) {
            String afterSQL = llmResponse.substring(llmResponse.indexOf("SQL:") + 4).trim();
            String[] lines = afterSQL.split("\n");
            if (lines.length > 0) {
                return cleanSQL(lines[0]);
            }
        }
        
        // Method 4: Just look for "SELECT" anywhere
        if (llmResponse.toUpperCase().contains("SELECT")) {
            int selectIndex = llmResponse.toUpperCase().indexOf("SELECT");
            String fromSelect = llmResponse.substring(selectIndex);
            // Take until semicolon or end
            int endIndex = fromSelect.indexOf(';');
            if (endIndex > 0) {
                fromSelect = fromSelect.substring(0, endIndex);
            }
            return cleanSQL(fromSelect) + " LIMIT 500";
        }
        
        return null;
    }

    private String cleanSQL(String sql) {
        return sql.trim()
            .replaceAll("\\s+", " ")  // Normalize whitespace
            .replaceAll(";$", "")      // Remove trailing semicolon
            .trim();
    }

    private QueryResult buildErrorResult(SQLValidation validation) {
        return QueryResult.builder()
            .originalQuery(validation.getGeneratedSQL().getRelevantTables()
                .getOriginalRequest().getNaturalLanguageQuery())
            .generatedSQL(validation.getGeneratedSQL().getSqlQuery())
            .rows(null)
            .rowCount(0)
            .executionTimeMS(0)
            .success(false)
            .errorMessage(validation.getValidationMessage())
            .build();
    }

    private QueryResult buildSuccessResult(SQLValidation validation, ExecutionResult result) {
        return QueryResult.builder()
            .originalQuery(validation.getGeneratedSQL().getRelevantTables()
                .getOriginalRequest().getNaturalLanguageQuery())
            .generatedSQL(validation.getGeneratedSQL().getSqlQuery())
            .rows(result.getRows())
            .rowCount(result.getRows() != null ? result.getRows().size() : 0)
            .executionTimeMS(result.getExecutionTimeMs())
            .success(result.isSuccess())
            .errorMessage(result.getErrorMessage())
            .build();
    }
}