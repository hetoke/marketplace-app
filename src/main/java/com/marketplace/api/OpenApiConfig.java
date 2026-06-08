package com.marketplace.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI marketplaceOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Marketplace API")
						.description("Web Marketplace REST API")
						.version("v1")
						.contact(new Contact()
								.name("Marketplace Team")))
				.servers(List.of(
						new Server().url("http://localhost:8080").description("Local development")
				));
	}
}
