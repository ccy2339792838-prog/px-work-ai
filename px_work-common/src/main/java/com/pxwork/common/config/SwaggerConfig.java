package com.pxwork.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        String schemeName = "satoken";
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name("satoken")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .info(new Info().title("px_work 接口文档")
                        .description("px_work 项目后端 API 接口文档")
                        .version("1.0"));
    }
}
