package it.cnr.anac.transparency.scheduler;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * Configurazione dei parametri generali della documentazione tramite OpenAPI.
 *
 * @author Cristian Lucchesi
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Task Scheduler Service OpenAPI",
        version = "0.1.0",
        description = "OpenAPI per task-scheduler-service")
    )
@SecuritySchemes(value = {
    @SecurityScheme(
        name = "bearer_authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer")
})
public class OpenApiConfiguration {

  public static final String BEARER_AUTHENTICATION = "Bearer Authentication";


}