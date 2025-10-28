package com.texttosql.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "texttosql")
@Data
public class QueryConfiguration {

    private int maxRows = 500;
    private int timeoutSeconds = 30;
    private List<String> allowedOperations = new ArrayList<>();
    private List<String> blockedKeywords = new ArrayList<>();

    public QueryConfiguration(){
        allowedOperations.add("SELECT");
        blockedKeywords.addAll(List.of(
            "DROP", "DELETE", "INSERT", "UPDATE", "TRUNCATE",
            "ALTER", "CREATE", "GRANT", "REVOKE"
        ));
    }
    
}
