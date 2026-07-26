// Gera trafego pro dashboard nao ficar vazio durante a aula.
// Rodar: k6 run load-test/chat-load.js
//
// Cenario "contexto explodindo" (fase 6): poucas sessoes, muitos turnos
// na MESMA sessao => memoria de conversa acumula e o painel de contexto sobe.
// Rodar: k6 run -e MODE=multiturn load-test/chat-load.js

import http from 'k6/http';
import { check, sleep } from 'k6';

const MODE = __ENV.MODE || 'spread';

export const options = MODE === 'multiturn'
  ? { vus: 3, duration: '25m' }     // 3 conversas longas
  : { vus: 10, duration: '30m' };   // 10 usuarios, sessoes novas

const QUESTIONS = [
  'O que sao Virtual Threads no Java 21?',
  'Explique a diferenca entre Kafka e RabbitMQ em uma frase.',
  'O que e um record em Java?',
  'Como funciona o garbage collector G1?',
  'O que e pattern matching para switch?',
  'Explique idempotencia em APIs REST.',
];

export default function () {
  // spread: cada VU+iteracao vira uma sessao nova (contexto pequeno e estavel)
  // multiturn: sessao fixa por VU (contexto cresce a cada turno)
  const sessionId = MODE === 'multiturn'
    ? `aula-vu-${__VU}`
    : `sessao-${__VU}-${__ITER}`;

  const payload = JSON.stringify({
    sessionId: sessionId,
    message: QUESTIONS[Math.floor(Math.random() * QUESTIONS.length)],
  });

  const res = http.post('http://localhost:8080/chat', payload, {
    headers: { 'Content-Type': 'application/json' },
    timeout: '240s',
  });

  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(MODE === 'multiturn' ? 2 : 1);
}
