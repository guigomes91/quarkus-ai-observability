package dev.getcaramelo.aiobs.observability;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

/**
 * Preco por 1M de tokens vem de configuracao, nunca hardcoded:
 * tabela de preco de provider muda todo mes, e em producao voce
 * quer trocar isso sem redeploy (ConfigMap, por exemplo).
 */
@ConfigMapping(prefix = "llm.pricing")
public interface LlmPricingConfig {

    @WithName("input-per-million")
    double inputPerMillion();

    @WithName("output-per-million")
    double outputPerMillion();

    default double costOf(long inputTokens, long outputTokens) {
        return (inputTokens * inputPerMillion() / 1_000_000.0)
             + (outputTokens * outputPerMillion() / 1_000_000.0);
    }
}
