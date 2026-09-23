package com.example.commissionquote.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI commissionQuoteOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Commission Quote API")
                .version("v1")
                .description("Generates commission quotes for loans by calling the external commission vendor."));
    }
}
