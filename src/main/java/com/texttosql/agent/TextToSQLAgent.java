package com.texttosql.agent;

import com.embabel.agent.api.annotation.*;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.common.ai.model.AutoModelSelectionCriteria;
import com.embabel.common.ai.model.LlmOptions;
import com.texttosql.domain.*;
import com.texttosql.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Agent(description = "GenAI agent that converts natural language to SQL")
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

    @AchievesGoal(description = "Convert natural language to SQL and execute")
    public QueryResult processQuery(String naturalLanguageQuery) {
        return null; // Embabel will orchestrate the workflow
    }

    @Action(description = "Fetch database schema")
    public QueryRequest fetchSchema(String naturalLanguageQuery) {
        log.info("📊 Fetching schema");
        DatabaseSchema schema = schemaService.fetchDatabaseSchema();
        return QueryRequest.builder()
            .naturalLanguageQuery(naturalLanguageQuery)
            .schema(schema)
            .build();
    }

    @Action(description = "Identify relevant tables using LLM")
    public RelevantTables identifyRelevantTables(QueryRequest request, OperationContext context) {
        log.info("🧠 LLM: Identifying relevant tables");
        
        String prompt = buildTableIdentificationPrompt(request);
        
        String llmResponse = context.promptRunner()
            .withLlm(LlmOptions.fromCriteria(AutoModelSelectionCriteria.INSTANCE))
            .generateText(prompt);
        
        RelevantTables tables = parseTableIdentificationResponse(llmResponse, request);
        log.info("✅ Identified: {}", tables.getTableNames());
        
        return tables;
    }

    @Action(description = "Generate SQL query using LLM")
    public GeneratedSQL generateSQL(RelevantTables tables, OperationContext context) {
        log.info("🧠 LLM: Generating SQL");
        
        String prompt = buildSQLGenerationPrompt(tables);
        
        String llmResponse = context.promptRunner()
            .withLlm(LlmOptions.fromCriteria(AutoModelSelectionCriteria.INSTANCE))
            .generateText(prompt);
        
        GeneratedSQL sql = parseSQLGenerationResponse(llmResponse, tables);
        log.info("✅ Generated: {}", sql.getSqlQuery());
        
        return sql;
    }

    @Action(description = "Validate SQL")
    public SQLValidation validateSQL(GeneratedSQL generatedSQL) {
        log.info("🔒 Validating SQL");
        ValidationResult result = validationService.validate(generatedSQL.getSqlQuery());
        return SQLValidation.builder()
            .isValid(result.isValid())
            .validationMessage(result.getMessage())
            .generatedSQL(generatedSQL)
            .build();
    }

    @Action(description = "Execute SQL")
    public QueryResult executeSQL(SQLValidation validation) {
        if (!validation.isValid()) {
            log.error("❌ Invalid SQL");
            return QueryResult.builder()
                .originalQuery(validation.getGeneratedSQL().getRelevantTables().getOriginalRequest().getNaturalLanguageQuery())
                .generatedSQL(validation.getGeneratedSQL().getSqlQuery())
                .success(false)
                .errorMessage(validation.getValidationMessage())
                .build();
        }
        
        log.info("⚡ Executing SQL");
        ExecutionResult result = executionService.executeQuery(validation.getGeneratedSQL().getSqlQuery());
        
        return QueryResult.builder()
            .originalQuery(validation.getGeneratedSQL().getRelevantTables().getOriginalRequest().getNaturalLanguageQuery())
            .generatedSQL(validation.getGeneratedSQL().getSqlQuery())
            .rows(result.getRows())
            .rowCount(result.getRows() != null ? result.getRows().size() : 0)
            .executionTimeMS(result.getExecutionTimeMs())
            .success(result.isSuccess())
            .errorMessage(result.getErrorMessage())
            .build();
    }

    @Condition
    public boolean schemaFetched(QueryRequest request) {
        return request != null && request.getSchema() != null;
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

    private String buildTableIdentificationPrompt(QueryRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Identify relevant tables for this query.\n\n");
        prompt.append("Query: \"").append(request.getNaturalLanguageQuery()).append("\"\n\n");
        prompt.append("Tables:\n");
        
        for (TableMetadata table : request.getSchema().getTables()) {
            prompt.append("- ").append(table.getFullTableName()).append(": ");
            prompt.append(table.getColumns().stream()
                .map(ColumnMetadata::getColumnName)
                .collect(Collectors.joining(", ")));
            prompt.append("\n");
        }
        
        prompt.append("\nRespond with:\nTABLES: table1, table2\nREASONING: why\n");
        return prompt.toString();
    }

    private String buildSQLGenerationPrompt(RelevantTables tables) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate MySQL SELECT query.\n\n");
        prompt.append("Query: \"").append(tables.getOriginalRequest().getNaturalLanguageQuery()).append("\"\n\n");
        
        for (TableMetadata table : tables.getOriginalRequest().getSchema().getTables()) {
            if (tables.getTableNames().contains(table.getFullTableName())) {
                prompt.append("Table ").append(table.getFullTableName()).append(":\n");
                for (ColumnMetadata col : table.getColumns()) {
                    prompt.append("  - ").append(col.getColumnName()).append(" (").append(col.getDataType()).append(")\n");
                }
            }
        }
        
        prompt.append("\nRules: SELECT only, LIMIT 500, valid MySQL syntax\n");
        prompt.append("Respond with:\nSQL: your query\nEXPLANATION: what it does\n");
        return prompt.toString();
    }

    private RelevantTables parseTableIdentificationResponse(String llmResponse, QueryRequest request) {
        List<String> tableNames = new ArrayList<>();
        String reasoning = "";
        
        for (String line : llmResponse.split("\n")) {
            if (line.startsWith("TABLES:")) {
                String tablesStr = line.substring(7).trim();
                for (String table : tablesStr.split(",")) {
                    tableNames.add(table.trim());
                }
            } else if (line.startsWith("REASONING:")) {
                reasoning = line.substring(10).trim();
            }
        }
        
        if (tableNames.isEmpty()) {
            tableNames.add(request.getSchema().getTables().get(0).getFullTableName());
            reasoning = "Fallback";
        }
        
        return RelevantTables.builder()
            .tableNames(tableNames)
            .reasoning(reasoning)
            .originalRequest(request)
            .build();
    }

    private GeneratedSQL parseSQLGenerationResponse(String llmResponse, RelevantTables tables) {
        String sql = "";
        String explanation = "";
        
        for (String line : llmResponse.split("\n")) {
            if (line.startsWith("SQL:")) {
                sql = line.substring(4).trim();
            } else if (line.startsWith("EXPLANATION:")) {
                explanation = line.substring(12).trim();
            }
        }
        
        if (sql.isEmpty()) {
            sql = "SELECT * FROM " + tables.getTableNames().get(0) + " LIMIT 500";
            explanation = "Fallback";
        }
        
        return GeneratedSQL.builder()
            .sqlQuery(sql)
            .explanation(explanation)
            .relevantTables(tables)
            .build();
    }
}