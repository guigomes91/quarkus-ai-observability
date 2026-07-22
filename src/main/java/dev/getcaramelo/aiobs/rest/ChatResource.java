package dev.getcaramelo.aiobs.rest;

import dev.getcaramelo.aiobs.ai.AssistantService;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;

@Path("/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {

    private final AssistantService assistant;

    public ChatResource(AssistantService assistant) {
        this.assistant = assistant;
    }

    /**
     * sessionId no request e proposital: cada sessao acumula memoria
     * de conversa, e e assim que vamos demonstrar o contexto crescendo
     * turno a turno nas metricas (fase 6).
     */
    @POST
    public ChatResponse chat(ChatRequest request) {
        String answer = assistant.chat(request.sessionId(), request.message());
        return new ChatResponse(request.sessionId(), answer);
    }

    public record ChatRequest(String sessionId, String message) {}

    public record ChatResponse(String sessionId, String answer) {}
}
