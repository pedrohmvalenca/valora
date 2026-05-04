package br.com.senac.valora.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuração CORS para permitir o frontend Vite ({@code http://localhost:5173})
 * fazer chamadas com cookies (credentials).
 *
 * <p>Pontos críticos:
 * <ul>
 *   <li>{@code setAllowCredentials(true)} é obrigatório para que o browser
 *       envie/receba o cookie httpOnly. Por consequência, o spec CORS proíbe
 *       wildcard {@code *} em {@code allowedOrigins} — origens devem ser
 *       listadas explicitamente.</li>
 *   <li>Origens vêm de {@code valora.cors.allowedOrigins} (CSV); default
 *       {@code http://localhost:5173} para dev local.</li>
 * </ul>
 */
@Configuration
public class CorsConfig {

    @Value("${valora.cors.allowedOrigins}")
    private String allowedOriginsCsv;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowCredentials(true);
        cors.setAllowedOrigins(parseOrigins(allowedOriginsCsv));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "Accept", "X-Requested-With"));
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    /**
     * Suporta múltiplas origens via vírgula. Cada entrada tem trim aplicado
     * para tolerar espaçamento na variável de ambiente.
     */
    private List<String> parseOrigins(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
