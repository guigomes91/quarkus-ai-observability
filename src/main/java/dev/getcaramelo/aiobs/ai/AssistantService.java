package dev.getcaramelo.aiobs.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * AiService declarativo do quarkus-langchain4j.
 *
 * O @MemoryId habilita memoria de conversa por sessao: e proposital,
 * porque na fase 6 (cenario real) vamos mostrar o contexto crescendo
 * a cada turno da conversa e como isso aparece nas metricas de tokens.
 */
@RegisterAiService
public interface AssistantService {

    @SystemMessage("""
            Voce e um assistente tecnico da Coders University.
            Responda de forma objetiva, em portugues do Brasil.
            Quando a pergunta envolver codigo, prefira exemplos em Java 21.
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
