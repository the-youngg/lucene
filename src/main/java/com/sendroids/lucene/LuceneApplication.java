package com.sendroids.lucene;

import com.sendroids.lucene.config.BipsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(BipsProperties.class)
@SpringBootApplication
public class LuceneApplication {

	public static void main(String[] args) {
		SpringApplication.run(LuceneApplication.class, args);
	}

}
