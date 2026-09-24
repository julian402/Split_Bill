package ue.edu.co.splitbill.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacion de la API en /swagger-ui.html.
 *
 * Declara el esquema "bearer" para que Swagger muestre el boton Authorize: se pega el token que
 * devuelve el login y todas las pruebas desde el navegador lo envian.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI splitBillOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SplitBill API")
                        .version("1.0")
                        .description("Backend de SplitBill: cuentas, grupos y gastos compartidos. "
                                + "Todos los montos van en centavos (60000 pesos = 6000000)."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
