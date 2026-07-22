package dev.getcaramelo.aiobs.observability;

import java.util.concurrent.TimeUnit;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * O coracao da masterclass.
 *
 * Beans CDI que implementam ChatModelListener sao registrados
 * automaticamente pelo quarkus-langchain4j em todos os chat models.
 *
 * Metricas expostas (nome Micrometer -> nome Prometheus):
 *   llm.tokens        -> llm_tokens_total{type, model}
 *   llm.cost.usd      -> llm_cost_usd_total{model}
 *   llm.request.duration -> llm_request_duration_seconds{model, outcome}
 *   llm.context.size  -> llm_context_size_tokens{model} (summary)
 *
 * ATENCAO (verificar no primeiro quarkus:dev): a API do listener mudou
 * entre versoes do langchain4j. Na linha 1.x o contexto expoe
 * chatRequest()/chatResponse(); se a versao fixada divergir, o compilador
 * aponta na hora e o ajuste e pontual.
 */
@ApplicationScoped
public class TokenMetricsListener implements ChatModelListener {

    private static final Logger LOG = Logger.getLogger(TokenMetricsListener.class);
    private static final String START_NANOS = "metrics.start.nanos";

    private final MeterRegistry registry;
    private final LlmPricingConfig pricing;
    private final long contextWarnThreshold;

    public TokenMetricsListener(MeterRegistry registry,
                                LlmPricingConfig pricing,
                                @org.eclipse.microprofile.config.inject.ConfigProperty(
                                        name = "llm.context.warn-threshold-tokens",
                                        defaultValue = "4000")
                                long contextWarnThreshold) {
        this.registry = registry;
        this.pricing = pricing;
        this.contextWarnThreshold = contextWarnThreshold;
    }

    @Override
    public void onRequest(ChatModelRequestContext ctx) {
        // Guardamos o instante da requisicao no mapa de atributos do contexto,
        // que viaja ate o onResponse/onError da MESMA chamada.
        ctx.attributes().put(START_NANOS, System.nanoTime());
    }

    @Override
    public void onResponse(ChatModelResponseContext ctx) {
        String model = ctx.chatRequest().parameters().modelName();
        TokenUsage usage = ctx.chatResponse().metadata().tokenUsage();

        if (usage != null) {
            long input = usage.inputTokenCount();
            long output = usage.outputTokenCount();

            registry.counter("llm.tokens", "type", "input", "model", model)
                    .increment(input);
            registry.counter("llm.tokens", "type", "output", "model", model)
                    .increment(output);
            registry.counter("llm.cost.usd", "model", model)
                    .increment(pricing.costOf(input, output));

            // Tamanho do contexto enviado: e ISSO que explode em conversa
            // multi-turn sem controle (fase 6 da masterclass).
            DistributionSummary.builder("llm.context.size")
                    .baseUnit("tokens")
                    .tag("model", model)
                    .register(registry)
                    .record(input);

            if (input > contextWarnThreshold) {
                LOG.warnf("Contexto acima do limite: %d tokens (limite %d, modelo %s)",
                        input, contextWarnThreshold, model);
            }
        }

        recordDuration(ctx.attributes().get(START_NANOS), model, "success");
    }

    @Override
    public void onError(ChatModelErrorContext ctx) {
        String model = ctx.chatRequest().parameters().modelName();
        registry.counter("llm.errors", "model", model,
                        "exception", ctx.error().getClass().getSimpleName())
                .increment();
        recordDuration(ctx.attributes().get(START_NANOS), model, "error");
    }

    private void recordDuration(Object startNanos, String model, String outcome) {
        if (startNanos instanceof Long start) {
            // publishPercentileHistogram: sem isso o Prometheus nao recebe buckets
            // e o painel de p95/p99 do Grafana fica vazio. Pegadinha classica.
            io.micrometer.core.instrument.Timer.builder("llm.request.duration")
                    .tag("model", model)
                    .tag("outcome", outcome)
                    .publishPercentileHistogram()
                    .register(registry)
                    .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }
}
