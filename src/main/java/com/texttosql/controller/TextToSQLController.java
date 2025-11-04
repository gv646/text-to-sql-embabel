package com.texttosql.controller;

import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.texttosql.agent.TextToSQLAgent;
import com.texttosql.domain.QueryResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/text-to-sql")
@Tag(name = "Text-to-SQL")
@Slf4j
public class TextToSQLController {

    private final TextToSQLAgent agent;

    public TextToSQLController(TextToSQLAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/query")
    public ResponseEntity<?> executeQuery(@Valid @RequestBody QueryRequest request) {
        try {
            log.info("Query: {}", request.query());
            
            // Use AgentInvocation to properly invoke the agent with OperationContext
            QueryResult result = AgentInvocation
                .create(agent, QueryResult.class)
                .invoke(request.query());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error: ", e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    public record QueryRequest(@NotBlank String query) {}
    public record ErrorResponse(String error) {}
}