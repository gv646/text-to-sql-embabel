package com.texttosql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// TODO: Re-enable when Java 21 is available
// import com.embabel.agent.config.annotation.EnableAgents;

@SpringBootApplication
// @EnableAgents
public class TextToSqlAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(TextToSqlAgentApplication.class, args);
	}

}
