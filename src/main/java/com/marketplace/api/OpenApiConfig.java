package com.marketplace.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI marketplaceOpenAPI() {
		String jwtScheme = "Bearer JWT";
		return new OpenAPI()
				.info(new Info()
						.title("Marketplace API")
						.description("Web Marketplace REST API")
						.version("v1")
						.contact(new Contact()
								.name("Marketplace Team")))
				.servers(List.of(
						new Server().url("http://localhost:8080").description("Local development")
				))
				.addSecurityItem(new SecurityRequirement().addList(jwtScheme))
				.components(new Components()
						.addSecuritySchemes(jwtScheme, new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("Paste your JWT access token")));
	}
}
