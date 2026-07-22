# quarkus-ai-observability

Observabilidade para aplicacoes de IA com **Quarkus + LangChain4j + Micrometer + OpenTelemetry**.

Projeto da masterclass de pos-graduacao em IA | [coders](https://codersuniversity.com.br/)

> Chamada de LLM nao e uma chamada HTTP qualquer: ela tem custo por request,
> latencia imprevisivel e um contexto que cresce silenciosamente ate a fatura chegar.
> Este projeto mostra como medir tokens, custo, latencia e tamanho de contexto
> antes que virem problema.

## Stack

| Camada | Tecnologia |
|---|---|
| Runtime | Java 21, Quarkus 3.24 |
| IA | quarkus-langchain4j 1.11.2 (Ollama default, OpenAI via profile) |
| Metricas | Micrometer + Prometheus |
| Tracing | OpenTelemetry + Jaeger |
| Dashboards | Grafana (provisionado) |
| Carga | k6 |

## Como rodar em 3 comandos

```bash
docker compose up -d
docker exec -it aiobs-ollama ollama pull llama3.2   # so na primeira vez
./mvnw quarkus:dev
```

Teste rapido:

```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"teste-1","message":"O que sao Virtual Threads?"}'
```

Acessos:

- Aplicacao: http://localhost:8080
- Metricas: http://localhost:8080/q/metrics
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (dashboard "LLM Observability" ja provisionado)
- Jaeger: http://localhost:16686

## Gerando trafego

```bash
k6 run load-test/chat-load.js                 # trafego normal, sessoes novas
k6 run -e MODE=multiturn load-test/chat-load.js  # cenario: contexto explodindo
```

## Fases da masterclass

Cada fase e uma tag. `git checkout <tag>` mostra o estado exato daquele bloco da aula.

| Tag | Conteudo |
|---|---|
| `v1-setup` | Quarkus + LangChain4j, AiService, endpoint /chat |
| `v2-metricas-base` | Micrometer + Prometheus, metricas HTTP padrao |
| `v3-tokens` | ChatModelListener: tokens, custo estimado, tamanho de contexto |
| `v4-tracing` | OpenTelemetry: span do LLM vs span da aplicacao |
| `v5-dashboard` | Grafana provisionado: tokens/min, custo, p95, erros |
| `v6-cenario-real` | Conversa multi-turn, contexto explodindo, deteccao via metrica |

## Rodando com OpenAI (custo real)

```bash
export OPENAI_API_KEY=sk-...
./mvnw quarkus:dev -Dquarkus.profile=openai
```

O preco por 1M de tokens vem de configuracao (`llm.pricing.*`), nunca hardcoded:
tabela de provider muda todo mes.

## Versoes usadas na gravacao

Este material foi gravado com as versoes fixadas no `pom.xml`.
Se voce atualizar as dependencias, a API do `ChatModelListener` pode ter mudado.

## Licenca

MIT
