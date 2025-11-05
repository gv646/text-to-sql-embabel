package com.texttosql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.embabel.agent.config.annotation.EnableAgents;

@SpringBootApplication
@EnableAgents
public class TextToSqlAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(TextToSqlAgentApplication.class, args);
	}

}
