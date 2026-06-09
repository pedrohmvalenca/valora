# Correções e melhorias do PWA — Entrega 1

**Projeto:** VALORA — PI ADS-3, Senac PE
**Repositório:** `github.com/pedrohmvalenca/valora` (backend Spring + frontend PWA)
**Escopo:** correções da Plataforma de Gestão e Análise (PWA — perfis Administrador e Coordenador)

> **Documento vivo.** Versão original criada como handoff entre máquinas (uma sem Maven/Docker, outra completa). Reescrito em 2026-06-09 para refletir o estado real após smoke runtime + bateria de planejamento + ciclo de implementação da Amelia (dev). O **tracking operacional** das stories vive em `_bmad-output/implementation-artifacts/sprint-status.yaml`; este documento é a **fonte semântica** do mini-plano PWA-E1.

---

## 0. Status (atualizado em 2026-06-09)

| Dimensão | Estado |
|----------|--------|
| Backend compila e roda | ✅ confirmado (Postgres via Docker, Spring Boot porta 8081, Flyway aplicou V1→V103+V104) |
| Suíte de testes | ✅ 47/47 verde no smoke inicial; 63/63 após Story 1.11 |
| Smoke runtime browser | ✅ Login admin/coord, tela Alunos (busca + vínculo), tela Submissões (preview comprovante) |
| Backlog deste plano | 6 dos 10 itens em `done` / `review`; 4 em `ready-for-dev` com spec formal |
| Descobertas em aberto | §7.1 (Senac) — só uma pergunta bloqueante |

A versão original deste documento foi a base do trabalho. As seções 1–8 abaixo preservam o conteúdo canônico (princípio, decisões, dívidas) com **anotações de status** quando algo virou `done`/`review`/Story X.Y. A seção **§11 (nova)** registra descobertas surgidas durante o uso real, fora do plano original.

---

## 1. Contexto e necessidade

A Entrega 1 (PWA) foi entregue, mas o uso real e a integração com a Entrega 2 (app mobile do aluno) expuseram lacunas que impedem o loop ponta a ponta:

> O aluno envia atividades pelo **mobile (E2)** e o Coordenador valida no **PWA (E1)**.

Sem gestão completa de aluno e sem visualizar o comprovante no PWA, esse loop não fecha. As correções abaixo não competem com a E2 — são o **outro lado** dela.

Duas dores originais relatadas:

1. **Gestão de aluno incompleta.** Faltava no PWA: localizar um aluno, vinculá-lo a um curso quando ele já existe (outro coordenador), editar e inativar. Consequência concreta: *"o aluno só aparece na lista do primeiro coordenador"*. — ✅ **resolvida** (busca global + vincular existente entregues).
2. **Tela de validação de submissão.** (a) Botões Aceitar/Rejeitar exigiam rolar a tela; (b) não era possível **visualizar o comprovante** enviado pelo aluno. — ✅ **resolvida** (sticky ActionBar pré-existia; preview inline implementado em B1).

---

## 2. Princípio de identidade (decisão estruturante)

O aluno é uma **identidade global única**: uma linha em `students` + uma linha em `users` (mesmo `id`). O vínculo com cada curso é uma linha em `student_course` (relação N:N). Duas camadas distintas:

- **Existir** — a identidade da pessoa. Chave única: `users.email`. Código acadêmico: `students.registration_code` (matrícula).
- **Estar matriculado** — o vínculo `student_course` com um curso, que decide qual Coordenador enxerga o aluno (RN-0001).

O **unificador de identidade é o e-mail**. A mesma pessoa que cursa dois cursos é **um** aluno com **dois** vínculos — nunca dois perfis. A busca global por e-mail, seguida de vínculo ao curso, é o que impede a duplicação.

---

## 3. Decisões fechadas (pelo PO)

| # | Tema | Decisão | Status |
|---|------|---------|--------|
| 1 | Quem cria a identidade | Coordenador **e** Administrador. Admin também faz importação em lote. | A3 = Story 3.15 (ready-for-dev) |
| 2 | Ativação | Aluno nasce **ativo** e loga direto — sem e-mail de ativação (SMTP indisponível). | ✅ pré-existente |
| 3 | Credencial | Senha provisória no cadastro + **troca forçada no 1º acesso**. | ✅ **Story 1.11 entregue (`review`)** |
| 4 | Campos do aluno | Matrícula + nome + e-mail + curso. **Sem CPF** (fase 2). | ✅ pré-existente |
| 5 | Status da matrícula | Três estados em `student_course`: **CURSANDO / CONCLUÍDO / ABANDONADO**. | ✅ schema entregue (Story 3.10 `review`); UI = Story 3.11 (ready) |
| 6 | Encerrar × Remover | "Encerrar" muda status; "Remover" é raro/protegido. | Stories 3.11 + 3.12 (ready-for-dev) |

---

## 4. Estado do código e o que foi implementado

### 4.1 Já existia antes deste plano

- **A0 — Criar aluno + nascer ativo + senha provisória — ✅ pré-existente.**
  `POST /api/v1/students` (Coord+Admin) cria `Student` + `User(profile=STUDENT)` com o mesmo id, aluno nasce ativo, gera senha provisória de 8 chars e a **devolve no DTO** a quem cria, vincula a curso(s), valida RN-0001, rejeita e-mail já em uso.
- **B2 — Barra de ação fixa (sticky) + motivo de rejeição — ✅ pré-existente.**
  A tela `Submissoes.jsx` já tinha `ActionBar` sticky com Aprovar/Reprovar, modo de rejeição inline e contador de 20 caracteres (RN-0006).
- `GET /api/v1/submissions/{id}/proof` — o backend **já servia** o comprovante (bytes + Content-Type).

### 4.2 Primeira rodada (branch `feat/correcoes-pwa-e1`, commit `06f65a6`)

**B1 — Preview inline do comprovante** *(frontend — verificado em smoke browser)*
- Carrega o comprovante **de cara** ao abrir a submissão.
- Imagem → preview inline (clicável, abre em tamanho real em nova aba); PDF/outros → cartão "Abrir em nova aba"; erro → fallback honesto ("Não foi possível carregar o comprovante").
- Busca o arquivo como **blob** pela instância `api` (reusa o cookie `withCredentials`). Object URL liberado ao trocar/desmontar.

**A2 — Busca global de aluno** *(backend — verificado em runtime)*
- `GET /api/v1/students/search?q=` — busca global por matrícula/nome/e-mail (`ILIKE`, mín. 2 chars, `LIMIT 20`).
- Retorna o **mínimo de identidade** (id, matrícula, nome, e-mail, ativo, **contagem** de cursos — respeitando RN-0001).

**A1 — Vincular aluno existente a um curso** *(backend — verificado em runtime, idempotência confirmada)*
- `POST /api/v1/students/{id}/courses` body `{ courseIds: [...] }` — cria vínculo(s) `student_course`.
- Valida curso existe + coord vinculado (RN-0001); `INSERT … ON CONFLICT (student_id, course_id) DO NOTHING` (idempotente); audita; devolve o `StudentDto` atualizado.
- **Fecha a dor "só aparece na lista do primeiro"**: o 2º coordenador acha o aluno pela busca e cria só o vínculo, sem duplicar identidade.

**Frontend da gestão de aluno** *(verificado)*
- `Alunos.jsx` reconstruída no fluxo **buscar-antes-de-criar**: botão "Adicionar aluno" → busca (debounce 300 ms) → resultados → selecionar → escolher seus cursos → **Vincular**. "Não encontrou? Cadastrar novo aluno" leva ao formulário de criação com "Voltar para busca".

### 4.3 Changelog da primeira rodada

**Backend:** `StudentSearchResultDto.java` (novo), `LinkStudentCoursesRequest.java` (novo), `StudentController.java` (+ 2 endpoints).
**Frontend:** `submissions.js` (+ `getSubmissionProof`), `Submissoes.jsx` (+ `ProofViewer`), `admin.js` (+ `studentsApi.search` e `linkCourses`), `Alunos.jsx` (diálogo buscar-antes-de-criar).

### 4.4 Verificação completa (smoke runtime 2026-06-09)

| Camada | Resultado |
|--------|-----------|
| Backend compila | ✅ `./mvnw -DskipTests compile` — 73 fontes, 13.9s |
| Suíte de testes | ✅ 47/47 verde (Testcontainers + Postgres real) |
| Postgres | ✅ Docker compose subiu healthy; Flyway aplicou todas as migrations |
| Smoke endpoints | ✅ Login admin, login Coord (Ana ADS), busca global (`q=al` 11 resultados, contagem correta), vínculo Diego→ADS, **idempotência confirmada** (`ON CONFLICT DO NOTHING` em runtime), RN-0001 bloqueia Coord vincular curso alheio (409 BIZ_006) |
| Smoke ProofViewer | ✅ PDF dummy retorna 200 + `application/pdf`; 404 + `RES_001` JSON limpo quando sem comprovante |
| Smoke browser (Chrome) | ✅ Telas Alunos e Submissões com fluxo buscar→vincular e preview de comprovante |
| Frontend lint + build | ✅ ESLint zero warnings, build Vite 1782 módulos / 27s |

### 4.5 Segunda rodada (2026-06-09) — descobertas e bateria PM

**Descoberta UX (Sally)** durante uso real: "buscar aluno" estava escondido atrás do botão "Adicionar aluno". Resultado: **Story 3.9 — Busca/filtro inline na tela Alunos**. ✅ Entregue (`done`).

**Bateria de planejamento (John, PM)** em paralelo com Amelia dev: o backlog §5 (A3–A8 + C1) foi convertido em 7 stories formais via `/bmad-create-story`. Aliado a 2 descobertas extra (§11 abaixo), totalizam:

- **Story 3.10** — A4 schema `status` em `student_course`. ✅ Entregue (`review`, migration V104, 9/9 testes específicos + 56/56 regressão).
- **Story 3.11** — A5 encerrar matrícula. 🟢 `ready-for-dev`.
- **Story 3.12** — A6 remover vínculo protegido. 🟢 `ready-for-dev`.
- **Story 3.13** — A7 editar aluno (sincroniza `students`+`users`). 🟢 `ready-for-dev` (resolve dívida técnica §6.1).
- **Story 3.14** — A8 inativar aluno (RN-0008). 🟢 `ready-for-dev`.
- **Story 3.15** — A3 importação em lote por CSV (Admin). 🟢 `ready-for-dev`.
- **Story 1.11** — C1 forçar troca de senha no 1º acesso. ✅ Entregue (`review`, migration V105, 7 testes específicos + smoke curls e Chrome).

---

## 5. Backlog — estado atual

> Numeração mantém a notação do plano original (A3–A8, B1, B2, C1) com referência à **Story** correspondente no `sprint-status.yaml`. ✅ = entregue (`done` ou `review`). 🟢 = `ready-for-dev` com spec formal.

### A3 · Importação em lote — Admin → 🟢 **Story 3.15**
Carga de múltiplos alunos via CSV UTF-8; mesmas regras do A0 por linha; relatório de sucesso/erro por linha sem abortar o lote; senhas provisórias agrupadas pra entrega.

### A4 · Coluna de status em `student_course` (3 estados) → ✅ **Story 3.10 (`review`)**
Coluna `status` com default `CURSANDO`, CHECK constraint, índice `(course_id, status)`. Migration aplicada como **V104** (convenção out-of-order do repo). Dois endpoints novos: `GET /students/{id}/courses` (rico, com status) e `PATCH /students/{id}/courses/{courseId}/status`.

### A5 · Encerrar matrícula no curso → 🟢 **Story 3.11**
Ação que muda o status do vínculo para `CONCLUIDO` ou `ABANDONADO`, **preservando histórico**. Aluno encerrado some das listas operacionais (validação de submissões e contagem de ativos no curso) por default. Esta story **muda o contrato** do `GET /students` para `linkedCourses: [{courseId, status}]`.

### A6 · Remover vínculo de verdade (raro, protegido) → 🟢 **Story 3.12**
Exclusão real da linha `student_course`, com bloqueio se há submissão registrada (RN-0008). Caminho confirmado por digitação do código do curso (padrão GitHub). Distinto de "Encerrar".

### A7 · Editar aluno (sincroniza `students` + `users`) → 🟢 **Story 3.13**
Edição de nome/e-mail dentro de **uma transação** que atualiza ambas as tabelas. Validação de e-mail duplicado. **Resolve a dívida técnica §6.1.**

### A8 · Inativar aluno (RN-0008) → 🟢 **Story 3.14**
`is_active = false` no nível da identidade (em ambas as tabelas, na mesma transação). Aluno inativo não loga e **some das listas operacionais** por default. Confirmação da §7.2 já feita por leitura de código (ver abaixo).

### C1 · Troca de senha forçada no 1º acesso → ✅ **Story 1.11 (`review`)**
Flag `users.must_change_password` (migration V105); `POST /students` seta a flag ao criar; `LoginResponse` carrega; `POST /auth/change-password` com 401 (current errada), 400 (curta), 204 (sucesso); PWA tem rota `/trocar-senha` com bloqueio de outras rotas via `RoleGuard`. Spec mobile entregue em `_mockup-e2/`.

### UX de status (A4/A5) — referência
Chip de curso com estado visível (🟢 Cursando / 🔵 Concluído / ⚪ Abandonado); lista filtrada por "Cursando" com toggle "ver concluídos/abandonados"; ação "Encerrar matrícula" (comum, segura) separada de "Remover" (rara, protegida em vermelho com confirmação). **Implementado na Story 3.11.**

---

## 6. Dívidas técnicas a tratar

1. **`students` e `users` espelhados pelo id** → 🟢 **coberta pela Story 3.13** — a story aplica `@Transactional` sincronizando as duas tabelas no editar; teste cobre rollback em e-mail duplicado.
2. **`students.email` não é único** (apenas `users.email`) → ✅ **descartada como risco prático em 2026-06-09**. Auditoria via `SELECT email, COUNT(*) FROM students GROUP BY email HAVING COUNT(*) > 1` retornou **zero duplicatas** (11 alunos, 11 e-mails distintos no seed). O constraint pode ser adicionado em fase 2 para defesa em profundidade, mas não bloqueia o MVP.

---

## 7. Descobertas em aberto

### 7.1 Matrícula: por pessoa ou por curso/ingresso? — 🟠 **aguarda Senac**
A `registration_code` hoje pertence ao **aluno-pessoa**. Se o Senac emite uma matrícula por curso/ingresso, ela pertence ao **vínculo** (`student_course`) e o schema muda. Pergunta formal preparada em `docs/academico/perguntas-senac-2026-06.md`. Bloqueia decisões de schema para Fase 2 (CPF), mas **não bloqueia** Stories 3.11–3.15 (que assumem o modelo atual).

### 7.2 Aluno inativo na lista de validação do Coordenador — ✅ **resolvida em 2026-06-09**
Leitura do `SubmissionService.fetchScoped()` (linhas 90–122) confirma que **hoje não há filtro por `student.is_active`**: aluno inativado continuaria aparecendo na fila de validação. Logo, a Story 3.14 implementa esse filtro como default ("aluno inativo some das listas operacionais"). Decisão adotada explicitamente.

---

## 8. Fora de escopo (Fase 2)

- CPF como atributo/chave de identidade (depende da descoberta §7.1).
- Relatórios por status de matrícula; datas de conclusão do vínculo; re-matrícula (reativar para `CURSANDO`).
- Mascaramento de e-mail/identificadores na busca cross-coordenador (hoje retorna identidade mínima sem dados de curso alheio — suficiente para o MVP).
- Constraint UNIQUE em `students.email` (defesa em profundidade; auditoria atual descartou risco prático).

---

## 9. Histórico de execução

A versão original deste documento foi escrita em máquina sem Maven/PostgreSQL/Docker. As validações pendentes foram cumpridas em 2026-06-09 na máquina completa:

1. ✅ `git checkout feat/correcoes-pwa-e1` e tratar o branch como fonte de verdade — feito.
2. ✅ `./mvnw -DskipTests compile` — sucesso (13.9s).
3. ✅ Postgres via `docker-compose -f valora-backend/docker-compose.yml up -d postgres` — sucesso (healthy).
4. ✅ Smoke dos dois endpoints novos (`/students/search` e `/students/{id}/courses`) com cookie de Coord e de Admin — sucesso, incluindo idempotência do `ON CONFLICT` e RN-0001 bloqueando coord não vinculado.
5. ✅ `./mvnw test` — 47/47 verde (depois 63/63 com 1.11).
6. ✅ `npm install && npm run build` no frontend — sucesso.
7. ✅ Smoke runtime no browser (Chrome MCP) das telas Alunos e Submissões — sucesso.

**Convenções do projeto preservadas em todo o trabalho:** identificadores em inglês (ADR-0014); UX em PT-BR; URLs em PT-BR (`/alunos`); sem comentários supérfluos; sem atribuição a ferramentas de IA em código/commits; artefatos de tooling (`_bmad*`, `.claude`, `.agents`, `valoraapp-master/`) deliberadamente fora do versionamento.

---

## 10. Ordem de implementação (atualizada)

1. ✅ **B1 + A1 + A2** — preview comprovante, busca global, vínculo de existente. (Branch `feat/correcoes-pwa-e1`.)
2. ✅ **Story 3.9** — busca/filtro inline na tela Alunos (UX-A da Sally). Descoberta que liderou a segunda rodada.
3. ✅ **A4 / Story 3.10** — schema status em `student_course`.
4. ✅ **C1 / Story 1.11** — forçar troca de senha no 1º acesso.
5. 🟢 **A5 / Story 3.11** — encerrar matrícula. **Próximo lógico após 3.10 done.**
6. 🟢 **A6 / Story 3.12** — remover vínculo protegido. Pode ir em paralelo com 3.11.
7. 🟢 **A7 / Story 3.13** — editar aluno (resolve dívida §6.1). Independente — pode ir em paralelo.
8. 🟢 **A8 / Story 3.14** — inativar aluno. Independente.
9. 🟢 **A3 / Story 3.15** — importação CSV em lote. Independente; valor alto pro início de semestre.
10. 🟢 **Story 3.16** — exibir senha provisória ao criar + reset de senha pelo Coord/Admin (§11 abaixo).
11. 🟢 **Story 3.17** — escopar `GET /courses` e `GET /categories` por perfil (§11 abaixo).
12. **B2** — apenas polimento (sticky bar já existia; preview entregue na B1).

(A0 já estava entregue antes deste plano.)

---

## 11. Descobertas surgidas durante o uso real (fora do plano original)

Três descobertas viraram stories formais durante a segunda rodada:

### 11.1 Bug crítico de UX: senha provisória nunca chega ao Coord → 🟢 **Story 3.16**
Confirmado em runtime: o backend gera a senha, salva o hash BCrypt e devolve em `provisionalPassword` no body do `POST /students`. **O `Alunos.jsx` ignora o campo** — o Coord nunca vê a senha pra entregar ao aluno. Sem isso, **nenhum aluno consegue logar a primeira vez**. A Story 3.16 corrige o bug + adiciona endpoint de **reset de senha** pelo Coord/Admin (caso "aluno perdeu a senha"). Alinha com Story 1.11 (toda senha gerada nasce com `must_change_password=true`).

### 11.2 Bug pré-existente: vazamento de leitura em `GET /courses` e `GET /categories` → 🟢 **Story 3.17**
Descoberto pela Amelia durante a Story 3.9 (AC5-Coord não era implementável) e confirmado por leitura de código: `CourseController.list()` retorna **todos** os cursos para qualquer perfil autenticado (RN-0001 violado por leitura). Varredura PM encontrou o mesmo padrão em `CategoryController.list()` (revela cursos alheios via `courseLimits`). Escrita já está protegida — bug é só leitura, severidade média. Story 3.17 conserta os dois endpoints + adiciona testes.

### 11.3 Convenção de migrations out-of-order (V10x)
A migration V104 da Story 3.10 (renomeada de V4 pela Amelia) revelou que o projeto adota numeração **na faixa V10x** para migrations novas do PWA-E1, não V4/V5 sequenciais. As stories subsequentes (1.11 → V105) seguiram a convenção. **Próximas migrations devem confirmar o próximo número livre** rodando `ls valora-backend/src/main/resources/db/migration/`.

### 11.4 Drift de ADR-0007 no PWA — observado e resolvido pela Story 1.12

> Registrado para rastreabilidade. Drift foi descoberto durante a Story 1.11 (auth) e **fechado pela Story 1.12** (em `review`) na mesma janela. **Não é dor aberta do plano correções-pwa-e1**; entrou aqui apenas para deixar o histórico.

**O que a ADR-0007 prescreve** (28/04/2026, `docs/adrs.md` §ADR-0007): na Entrega 1 o aluno tem acesso ao **PWA com escopo reduzido** — pode autenticar-se, pode visualizar a lista de suas submissões com status, **não pode** criar submissões. O canal de submissão é inaugurado pelo mobile na Entrega 2.

**O drift observado durante a Story 1.11** (pré-1.12):
1. Login STUDENT era aceito no PWA — coerente com ADR-0007 (autenticar-se).
2. Rota `/minhas-submissoes` liberada para STUDENT, mas **a página era stub** ("Path 2.0 — a caminho"); a promessa "visualizar suas submissões com status" da ADR-0007 não estava cumprida.
3. Story 1.11 (troca de senha forçada) também cobria STUDENT no PWA — destoava do espírito mobile-first da ADR-0007: aluno logando no PWA era forçado a trocar senha no PWA e caía num stub.

**O que a Story 1.12 fez** (commit em `feat/correcoes-pwa-e1`, status `review`):
- `Login.jsx` rejeita perfil STUDENT antes do `RoleGuard` da Story 1.11 — toast persistente "Aluno usa o app mobile (Entrega 2)".
- `AuthContext` no bootstrap detecta cookie de STUDENT (cenário pós-F5), faz logout silencioso e seta flag em `sessionStorage` para o `Login` mostrar banner explicativo.
- Rota `/minhas-submissoes` removida do `App.jsx` (comentário marcando a remoção); item de sidebar removido; arquivo `MinhasSubmissoes.jsx` deletado.
- Smoke Chrome MCP verde (3+1 cenários AC9). Lint zero. 64/64 testes backend verdes.

Efetivamente é a **opção (b)** das três que eu havia listado: revisar a leitura prática da ADR-0007 — STUDENT é mobile-only na E1; PWA não tem mais superfície de aluno.

**Status**: ✅ resolvido (aguarda só ratificação `review → done`). Nenhuma ação adicional pendente do nosso lado para fechar o drift.

**Lição preservada**: discoveries de drift surgem em janelas próximas das stories de auth/escopo. Vale auditar coerência ADR × código sempre que mexer em login, RoleGuard ou sidebar.

---

*Documento mantido por John (PM) e atualizado conforme entregas avançam. Tracking detalhado por story: `_bmad-output/implementation-artifacts/sprint-status.yaml`.*
