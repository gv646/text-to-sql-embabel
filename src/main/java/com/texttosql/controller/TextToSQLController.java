package com.texttosql.controller;

import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.texttosql.domain.QueryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Text-to-SQL Agent
 * Uses AgentInvocation with AgentPlatform to invoke the Embabel agent
 */
@RestController
@RequestMapping("/api/v1/text-to-sql")
@Tag(name = "Text-to-SQL API", description = "Convert natural language to SQL and execute")
@Slf4j
public class TextToSQLController {

    private final AgentPlatform agentPlatform;

    public TextToSQLController(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    /**
     * Main endpoint: Convert natural language to SQL and execute
     * 
     * Flow:
     * 1. Natural language query comes in via REST
     * 2. AgentInvocation.create(agentPlatform, QueryResult.class) sets up invocation
     * 3. invoke(query) executes the agent with proper OperationContext
     * 4. Agent orchestrates: Schema → LLM tables → LLM SQL → Validate → Execute
     * 5. Results returned as JSON
     */
    @PostMapping("/query")
    @Operation(summary = "Convert natural language to SQL",
               description = "Uses Ollama LLM to intelligently generate and execute SQL queries")
    public ResponseEntity<?> executeQuery(@Valid @RequestBody QueryRequest request) {
        try {
            log.info("📝 Received query: {}", request.query());
            
            // AgentInvocation.create(agentPlatform, RETURN_TYPE.class)
            // The second parameter is the expected return type (QueryResult), not the agent class
            QueryResult result = AgentInvocation
                .create(agentPlatform, QueryResult.class)
                .invoke(request.query());
            
            if (result == null || !result.isSuccess()) {
                log.error("❌ Query failed: {}", result != null ? result.getErrorMessage() : "null result");
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse(
                        result != null ? result.getErrorMessage() : "Agent returned no result",
                        result != null ? result.getGeneratedSQL() : null
                    ));
            }
            
            log.info("✅ Success: {} rows returned", result.getRowCount());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Error processing query", e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse(e.getMessage(), null));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Text-to-SQL Agent is running");
    }

    // DTOs
    public record QueryRequest(
        @NotBlank(message = "Query cannot be empty")
        String query
    ) {}

    public record ErrorResponse(
        String error,
        String generatedSQL
    ) {}
}