package br.com.senac.valora.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuração de execução assíncrona — Story 1.8 (audit log).
 *
 * <p>{@code AuditService.recordXxx(...)} é {@code @Async("auditExecutor")}, garantindo
 * que persistência de log NUNCA bloqueia o request principal. Pool pequeno e
 * fixo (3 threads) — operação é I/O leve (insert simples no audit_log).
 *
 * <p>Falha do executor (queue cheia, etc.) cai no SLF4J via WARN dentro do
 * AuditService — request principal segue normal.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("audit-");
        executor.initialize();
        return executor;
    }
}
