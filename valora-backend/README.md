# VALORA — Backend

API REST do **VALORA** (Sistema Integrado de Gestão de Atividades Complementares — Senac PE / PI ADS-3).

**Stack:** Spring Boot 3.5.14 · Java 21 LTS · PostgreSQL 15+ · Maven · JWT · Flyway · springdoc-openapi · JaCoCo · Testcontainers.

> Decisões arquiteturais em `../docs/adrs.md`. Especificação completa em `../_bmad-output/planning-artifacts/architecture.md`.

---

## Pré-requisitos

| Ferramenta | Versão | Como instalar (Windows via winget) |
|---|---|---|
| **Java** | 21 LTS | `winget install --id EclipseAdoptium.Temurin.21.JDK -e` (ou Oracle JDK 21) |
| **Maven** | não precisa global — use `./mvnw` | (Maven Wrapper já incluído no projeto) |
| **Docker Desktop** | 27.x ou 28.x | `winget install --id Docker.DockerDesktop -e` |
| **Git** | 2.x | `winget install --id Git.Git -e` |

> Em máquinas onde Docker é bloqueado por política, use Postgres remoto (Supabase / Neon / Render free tier) — ver § Setup local sem Docker.

---

## Setup local (4 passos)

### 1. Subir Postgres via Docker

```bash
docker compose up -d postgres
docker compose ps   # confirme status "healthy"
```

Postgres ficará disponível em `localhost:5433` com banco `valora_local`, usuário `valora_dev`, senha `valora_dev`.

### 2. Configurar variáveis de ambiente

```bash
cp .env.example .env
# editar .env e gerar JWT_SECRET com:
openssl rand -base64 64
```

### 3. Subir o backend

```bash
./mvnw spring-boot:run
```

Aplicação em http://localhost:8080. Flyway aplica V1 automaticamente.

### 4. Validar

```bash
curl http://localhost:8080/actuator/health        # deve retornar {"status":"UP"}
```

Acessar no browser:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

Inspecionar banco:
```bash
docker exec -it valora-postgres psql -U valora_dev -d valora_local
\dt                              -- listar tabelas (espera: usuario, curso, coordenador_curso, log, flyway_schema_history)
SELECT * FROM usuario;           -- 1 row Admin seed
\q
```

---

## Setup local sem Docker

Caso Docker não esteja disponível na sua máquina (política de TI / etc.), usar Postgres remoto free tier:

1. Criar conta gratuita em **Supabase** (https://supabase.com) ou **Neon** (https://neon.tech)
2. Criar novo projeto, copiar `DATABASE_URL` (formato `postgresql://user:pass@host:port/db`)
3. Ajustar `.env`:
   ```bash
   DATABASE_URL=jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:6543/postgres?user=postgres.xxx&password=yyy&sslmode=require
   ```
4. Pular `docker compose up`. Rodar direto `./mvnw spring-boot:run`.

> Cada dev pode ter sua própria instância free tier (cadastrar `valora-pedro`, `valora-joao`, etc.) — sem fricção de coordenação.

---

## Comandos comuns

```bash
./mvnw spring-boot:run           # Subir aplicação em modo dev
./mvnw clean compile             # Apenas compilar
./mvnw clean verify              # Compilar + rodar testes + JaCoCo coverage report
./mvnw test                      # Apenas testes
./mvnw dependency:tree           # Ver árvore de deps
./mvnw spring-boot:build-image   # Build container OCI (Buildpacks)
```

Após `./mvnw clean verify`, abrir relatório de cobertura:
- `target/site/jacoco/index.html`

---

## Operações

### Rotacionar `JWT_SECRET`

1. Gerar novo: `openssl rand -base64 64`
2. Atualizar variável `JWT_SECRET` no provedor (Render dashboard / `.env` local)
3. Restart da aplicação — todos os tokens existentes invalidam imediatamente

### Resetar banco local

```bash
docker compose down -v           # remove container + volume (DADOS PERDIDOS)
docker compose up -d postgres    # recriar do zero
./mvnw spring-boot:run           # Flyway aplica V1 novamente
```

### Restaurar backup Postgres em produção (Render)

Render Dashboard → Database → Backups → Restore (último 24h disponível no free tier).

---

## Erros comuns

| Sintoma | Causa | Solução |
|---|---|---|
| `503 Service Unavailable` no Render | Spin-down do free tier (15min inatividade) | Primeiro request demora 30-60s — ping `/actuator/health` para warm-up antes de demos |
| `Connection refused localhost:5433` | Postgres não subiu | `docker compose ps`; se "unhealthy", `docker compose logs postgres` |
| `Flyway: Validate failed` | Schema diverge das migrations | Verificar se algum dev rodou DDL manual; em dev, `docker compose down -v` reseta |
| `JWT_SECRET cannot be null` | `.env` não carregado ou variável vazia | Confirmar `SPRING_PROFILES_ACTIVE=local` e que `.env` existe |
| `Web server failed to start. Port 8080 was already in use` | Outro processo local na 8080 (ex.: MiniTool ShadowMaker, IIS) | Diagnosticar com `netstat -ano \| findstr :8080` (Windows). Resolver: parar o outro processo OU setar `SERVER_PORT=8081` no seu `.env` (não commitar — override local) |
| Build falha com `cannot find symbol` Lombok | Lombok annotation processor não rodou | `./mvnw clean compile` — primeira vez baixa annotation processors |

---

## Aviso importante — seed Admin

A senha do Admin seed (`Admin@123` para `admin@valora.local`) é **APENAS para desenvolvimento local**. Em homologação e produção:
- O seed `V1__init_identity.sql` ainda é aplicado por design (precisa ter um Admin para começar)
- **Trocar a senha imediatamente após o primeiro login** via endpoint `POST /api/v1/auth/trocar-senha` (Story 1.6)
- Considerar mover o seed para uma migration `db/migration-demo/` separada que só roda em local (decisão pós-Story 1.1)

---

## Estrutura do projeto

```
valora-backend/
├── pom.xml                                  # Maven + 19 deps + 3 plugins
├── mvnw, mvnw.cmd, .mvn/                    # Maven Wrapper (não precisa Maven global)
├── docker-compose.yml                        # Postgres 15-alpine porta 5433
├── .env.example                              # template variáveis de ambiente
├── .gitignore                                # exclui target/, .env, IDEs
├── src/main/java/br/com/senac/valora/
│   └── ValoraApplication.java                # main class
├── src/main/resources/
│   ├── application.yml                       # config default (lê env vars)
│   ├── application-local.yml                 # override dev local
│   └── db/migration/
│       └── V1__init_identity.sql             # schema inicial + seed Admin
└── src/test/java/br/com/senac/valora/
    └── ValoraApplicationTests.java           # smoke test (context loads)
```

---

## Roadmap (próximas stories)

A Story 1.1 (esta) cria a fundação. Próximas:

| Story | O que adiciona |
|---|---|
| **1.2** | Frontend React 18 + Vite + Tailwind (refactor Lovable) |
| **1.3** | Auth backend — login + JWT em cookie httpOnly |
| **1.4** | RBAC base + `GlobalExceptionHandler` |
| **1.5** | Frontend AuthContext + Login Page + RoleGuard |
| **1.6** | Logout + Trocar senha |
| **1.7** | CursoContext + CursoSelector |
| **1.8** | AuditAspect + Log Entity (RF-0032/0033 + EXT-06) |
| **1.9** | Test Foundation — cobertura 3 perfis com Testcontainers |

Ver `_bmad-output/implementation-artifacts/sprint-status.yaml` para status atualizado.
