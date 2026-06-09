# Correções e melhorias do PWA — Entrega 1 (documento de handoff)

**Projeto:** VALORA — PI ADS-3, Senac PE
**Repositório:** `github.com/pedrohmvalenca/valora` (backend Spring + frontend PWA)
**Branch deste trabalho:** `feat/correcoes-pwa-e1`
**Escopo:** correções da Plataforma de Gestão e Análise (PWA — perfis Administrador e Coordenador)

---

## 0. Como usar este documento (handoff entre máquinas)

Este documento foi produzido em uma máquina **bloqueada**: sem Maven, sem PostgreSQL e sem Docker. Por isso:

- O **frontend** (`valora-frontend`, Vite) foi compilado e validado normalmente (`npm run build` + `eslint`).
- O **backend** (`valora-backend`, Spring Boot) **não pôde ser compilado nem executado** aqui. As mudanças de backend foram validadas apenas por **revisão de código**, espelhando padrões já existentes no projeto.

A continuação deve ser feita em uma máquina **sem essas restrições**. A outra máquina **não tem acesso aos arquivos locais** — apenas ao que está no GitHub. **O que estiver no branch `feat/correcoes-pwa-e1` é a fonte da verdade.**

As instruções para o agente (BMad) continuar estão na **§9**.

---

## 1. Contexto e necessidade

A Entrega 1 (PWA) foi entregue, mas o uso real e a integração com a Entrega 2 (app mobile do aluno) expuseram lacunas que impedem o loop ponta a ponta:

> O aluno envia atividades pelo **mobile (E2)** e o Coordenador valida no **PWA (E1)**.

Sem gestão completa de aluno e sem visualizar o comprovante no PWA, esse loop não fecha. As correções abaixo não competem com a E2 — são o **outro lado** dela.

Duas dores originais relatadas:

1. **Gestão de aluno incompleta.** Faltava no PWA: localizar um aluno, vinculá-lo a um curso quando ele já existe (outro coordenador), editar e inativar. Consequência concreta: *"o aluno só aparece na lista do primeiro coordenador"*.
2. **Tela de validação de submissão.** (a) Botões Aceitar/Rejeitar exigiam rolar a tela; (b) não era possível **visualizar o comprovante** enviado pelo aluno.

---

## 2. Princípio de identidade (decisão estruturante)

O aluno é uma **identidade global única**: uma linha em `students` + uma linha em `users` (mesmo `id`). O vínculo com cada curso é uma linha em `student_course` (relação N:N). Duas camadas distintas:

- **Existir** — a identidade da pessoa. Chave única: `users.email`. Código acadêmico: `students.registration_code` (matrícula).
- **Estar matriculado** — o vínculo `student_course` com um curso, que decide qual Coordenador enxerga o aluno (RN-0001).

O **unificador de identidade é o e-mail**. A mesma pessoa que cursa dois cursos é **um** aluno com **dois** vínculos — nunca dois perfis. A busca global por e-mail, seguida de vínculo ao curso, é o que impede a duplicação.

---

## 3. Decisões fechadas (pelo PO)

| # | Tema | Decisão |
|---|------|---------|
| 1 | Quem cria a identidade | Coordenador **e** Administrador. Admin também faz importação em lote. |
| 2 | Ativação | Aluno nasce **ativo** e loga direto — sem e-mail de ativação (SMTP indisponível no tier gratuito). |
| 3 | Credencial | Senha provisória gerada no cadastro (já implementado) + **troca forçada no 1º acesso** (a construir). Senha inicial genérica; aluno define a final no 1º acesso. |
| 4 | Campos do aluno | Matrícula (`registration_code`) + nome + e-mail + curso. **Sem CPF** por enquanto (fase 2). |
| 5 | Status da matrícula | Três estados no vínculo `student_course`: **CURSANDO / CONCLUÍDO / ABANDONADO**. |
| 6 | Encerrar × Remover | "Encerrar matrícula" muda o status e preserva o histórico; "Remover" de fato é raro, protegido, só para erro de cadastro. |

---

## 4. Estado do código e o que foi implementado

### 4.1 Já existia antes deste trabalho

- **A0 — Criar aluno + nascer ativo + senha provisória — ✅ FEITO (pré-existente).**
  `POST /api/v1/students` (Coord+Admin) cria `Student` + `User(profile=STUDENT)` com o mesmo id, aluno nasce ativo, gera senha provisória de 8 chars e a **devolve no DTO** a quem cria, vincula a curso(s), valida RN-0001, rejeita e-mail já em uso.
- **B2 — Barra de ação fixa (sticky) + motivo de rejeição — ✅ EM GRANDE PARTE FEITO (pré-existente).**
  A tela `Submissoes.jsx` já tem `ActionBar` sticky com Aprovar/Reprovar, modo de rejeição inline e contador de 20 caracteres (RN-0006). Resta no máximo polimento.
- `GET /api/v1/submissions/{id}/proof` — o backend **já serve** o comprovante (bytes + Content-Type).

### 4.2 Implementado neste branch (`feat/correcoes-pwa-e1`)

**B1 — Preview inline do comprovante na tela de validação** *(frontend — verificado)*
- Carrega o comprovante **de cara** ao abrir a submissão.
- Imagem → preview inline (clicável, abre em tamanho real em nova aba); PDF/outros → cartão "Abrir em nova aba"; erro → fallback honesto ("Não foi possível carregar o comprovante").
- Busca o arquivo como **blob** pela instância `api` (reusa o cookie `withCredentials` — um `<img src>` cru não carregaria a auth do mesmo jeito). Object URL liberado ao trocar/desmontar.

**A2 — Busca global de aluno** *(backend — só revisão)*
- `GET /api/v1/students/search?q=` — busca global (não escopada por curso) por matrícula/nome/e-mail (`ILIKE`, mín. 2 chars, `LIMIT 20`).
- Retorna o **mínimo de identidade** (id, matrícula, nome, e-mail, ativo, **contagem** de cursos — não *quais* cursos, respeitando RN-0001).

**A1 — Vincular aluno existente a um curso** *(backend — só revisão)*
- `POST /api/v1/students/{id}/courses` body `{ courseIds: [...] }` — cria vínculo(s) `student_course` de um aluno que já existe.
- Valida curso existe + coord vinculado (RN-0001); `INSERT … ON CONFLICT (student_id, course_id) DO NOTHING` (idempotente); audita; devolve o `StudentDto` atualizado.
- **Fecha a dor "só aparece na lista do primeiro"**: o 2º coordenador acha o aluno pela busca e cria só o vínculo, sem duplicar identidade.

**Frontend da gestão de aluno** *(verificado)*
- `Alunos.jsx` reconstruída no fluxo **buscar-antes-de-criar**: botão "Adicionar aluno" → busca (debounce 300 ms) → resultados (nome/matrícula/e-mail/"em N curso(s)") → seleciona → escolhe seus cursos → **Vincular**. "Não encontrou? Cadastrar novo aluno" leva ao formulário de criação (pré-preenchido a partir da busca) com "Voltar para busca".

### 4.3 Changelog de arquivos (neste branch)

**Backend (`valora-backend`):**
- `src/main/java/br/com/senac/valora/dtos/StudentSearchResultDto.java` — **novo** (record do resultado da busca).
- `src/main/java/br/com/senac/valora/dtos/LinkStudentCoursesRequest.java` — **novo** (record do body de vínculo).
- `src/main/java/br/com/senac/valora/controllers/StudentController.java` — **modificado**: + endpoints `GET /search` e `POST /{studentId}/courses` (+ 2 imports de DTO).

**Frontend (`valora-frontend`):**
- `src/services/submissions.js` — **modificado**: + `getSubmissionProof(id)` (fetch blob + content-type → object URL).
- `src/pages/Submissoes.jsx` — **modificado**: placeholder do comprovante → componente `ProofViewer`.
- `src/services/admin.js` — **modificado**: + `studentsApi.search` e `studentsApi.linkCourses`.
- `src/pages/Alunos.jsx` — **modificado**: diálogo reconstruído no fluxo buscar-antes-de-criar.

### 4.4 Verificação

| Camada | Verificação possível nesta máquina | Status |
|---|---|---|
| Frontend | `eslint` + `npm run build` | ✅ passou |
| Backend | nenhuma (sem Maven/Postgres/Docker) | ⚠️ **só revisão de código** |

**Pontos do backend a confirmar no primeiro build/run real** (§9): tipagem do native query na busca (`is_active` → `Boolean`, `COUNT(*)` → `Number`); comportamento do `ON CONFLICT` no vínculo. Ambos seguem o padrão de `StudentController.list`, que já roda em produção — mas só rodando se confirma.

---

## 5. Backlog restante (com design acordado)

### A3 · Importação em lote — Admin
Carga de múltiplos alunos; mesmas regras do A0 por linha; relatório de sucesso/erro por linha sem abortar o lote.

### A4 · Coluna de status em `student_course` (3 estados)
Coluna `status` (`CURSANDO` / `CONCLUÍDO` / `ABANDONADO`), default `CURSANDO`. Migration com backfill (vínculos existentes → `CURSANDO`) e índice `(course_id, status)`. **Infra de A5/A6 e das listas filtradas.**

### A5 · Encerrar matrícula no curso (muda status)
Ação que muda o status do vínculo para `CONCLUÍDO` ou `ABANDONADO`, **preservando histórico** (não apaga a linha). Aluno encerrado não conta como ativo no curso nas telas de operação.

### A6 · Remover vínculo de verdade (raro, protegido)
Exclusão real da linha `student_course`, reservada a erro de cadastro. Caminho protegido/confirmado, distinto de "Encerrar".

### A7 · Editar aluno (sincroniza `students` + `users`)
Edição de nome/e-mail. **Atenção:** os dados vivem nas duas tabelas (espelhadas pelo id) — atualizar as duas na mesma transação. Mantém validação de e-mail duplicado.

### A8 · Inativar aluno (RN-0008)
`is_active = false` no nível da identidade (nunca hard delete). Aluno inativo não loga e some da lista de validação do Coordenador *(confirmar — §7)*.

### C1 · Troca de senha forçada no 1º acesso
Flag `mustChangePassword = true` ao criar; 1º login com a provisória força a tela de troca antes de liberar o app (mobile); endpoint de troca grava a nova senha e zera a flag. Logins seguintes entram direto.

### UX de status (A4/A5) — referência
Chip de curso com estado visível (🟢 Cursando / 🔵 Concluído / ⚪ Abandonado); lista filtrada por "Cursando" com toggle "ver concluídos/inativos"; ação "Encerrar matrícula" (comum, segura) separada de "Remover" (rara, protegida em vermelho com confirmação).

---

## 6. Dívidas técnicas a tratar

1. **`students` e `users` espelhados pelo id** — nome/e-mail vivem nas duas tabelas e só sincronizam no create. A edição (A7) precisa escrever nas duas na mesma transação, ou os dados divergem.
2. **`students.email` não é único** — apenas `users.email` é. Alinhar a constraint de unicidade entre as tabelas (após limpar eventuais duplicatas).

---

## 7. Descobertas em aberto (decidir com a instituição)

1. **Como o Senac emite matrícula/identidade** — por pessoa ou por curso/ingresso? Se for por curso, a `registration_code` pertence conceitualmente ao vínculo (`student_course`), não ao aluno. A resposta decide se/como o CPF entra na fase 2. (Por ora, identidade unificada por **e-mail**; matrícula = código acadêmico.)
2. **Aluno inativo na lista de validação** — confirmar que, ao inativar (A8), o aluno some da lista de submissões a validar do Coordenador.

---

## 8. Fora de escopo (Fase 2)

- CPF como atributo/chave de identidade (depende da descoberta §7.1).
- Relatórios por status de matrícula; datas de conclusão do vínculo; re-matrícula (reativar para `CURSANDO`).
- Mascaramento de e-mail/identificadores na busca cross-coordenador (hoje retorna identidade mínima sem dados de curso alheio — suficiente para o MVP).

---

## 9. Instruções para o BMad continuar (máquina sem restrições)

1. **Obter o código:** `git clone github.com/pedrohmvalenca/valora` (ou `git fetch`), depois `git checkout feat/correcoes-pwa-e1`. Tratar o branch como verdade.
2. **Validar o backend que não pôde ser buildado aqui:**
   - `cd valora-backend && ./mvnw -DskipTests compile` (deve compilar sem erro).
   - Subir Postgres (Docker ou local) e rodar a aplicação; conferir os dois endpoints novos:
     - `GET /api/v1/students/search?q=<termo>` → retorna lista mínima (id, matrícula, nome, e-mail, ativo, contagem de cursos).
     - `POST /api/v1/students/{id}/courses` com `{ "courseIds": ["<uuid>"] }` → cria vínculo, idempotente, devolve `StudentDto`.
   - Rodar os testes existentes (`./mvnw test`) e, se houver tempo, adicionar testes para os dois endpoints (busca: ≥2 chars, RN-0001 não vaza cursos alheios; vínculo: idempotência, RN-0001, aluno/curso inexistente).
3. **Validar o frontend (já compila, mas confirmar runtime):** `cd valora-frontend && npm install && npm run build`; com o backend de pé, abrir a tela **Alunos** (fluxo buscar→vincular / buscar→criar) e a tela **Submissões** (preview de comprovante: imagem, PDF, erro).
4. **Seguir o backlog na ordem da §10**, começando por **A4** (coluna de status no vínculo) → A5 → A6 → A7 → A8 → C1 → A3. Cada item já tem design acordado nas §5/§6/§7.
5. **Convenções do projeto:** identificadores em inglês; sem comentários supérfluos e sem qualquer atribuição a ferramentas de IA no código/commits; não comitar artefatos de tooling (`_bmad*`, `.claude`, `.agents`, etc.) nem o repo aninhado `valoraapp-master/`.

---

## 10. Ordem de implementação

1. ✅ **B1** — preview do comprovante *(feito neste branch — verificar runtime)*.
2. ✅ **A1 + A2** — vincular aluno existente + busca global *(feito neste branch — verificar build/runtime do backend)*.
3. **A4 → A5 → A6** — status de matrícula (3 estados), encerrar, remover.
4. **A7 → A8** — editar (sincronizando as duas tabelas), inativar.
5. **C1** — troca de senha no 1º acesso.
6. **A3** — importação em lote.
7. **B2** — apenas polimento (sticky bar já existe).

(A0 já estava entregue antes deste trabalho.)
