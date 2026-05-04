# VALORA

Sistema de gestão de **Atividades Complementares** para o Senac PE.
**V**alidação · **A**companhamento · **L**ançamento · **O**rganização · **R**econhecimento **A**cadêmico.

Projeto Integrador ADS-3 (2026) — equipe Pedro Valença et al.

---

## Stack

- **Backend:** Spring Boot 3.5 · Java 21 LTS · PostgreSQL 15 · Flyway (migrations versionadas) · JWT em cookie httpOnly · BCrypt cost 12 · Spring Security RBAC
- **Frontend:** React 18 · Vite 5 · JavaScript ES6+ · Tailwind 3 · shadcn/ui · next-themes (dark/light) · react-hook-form + zod · axios · sonner (toasts)
- **Testes:** JUnit 5 · Mockito · Testcontainers (Postgres em container) · ESLint
- **CI:** GitHub Actions

## Estrutura do monorepo

```
valora/
├── valora-backend/        # Spring Boot 3 + Java 21 + Flyway + Postgres
├── valora-frontend/       # React 18 + Vite + JS ES6+ + Tailwind + shadcn/ui
├── docs/                  # Documentos do projeto (ADRs, contexto, handoffs)
└── .github/workflows/     # CI (GitHub Actions)
```

---

## Tutorial — clonar e rodar localmente

### 1. Pré-requisitos

| Ferramenta | Versão mínima | Como verificar |
|---|---|---|
| Git | 2.30+ | `git --version` |
| Java JDK | 21 | `java -version` |
| Maven | 3.9+ | _wrapper `mvnw` incluído — não precisa instalar global_ |
| Node.js | 18+ | `node --version` |
| npm | 9+ | `npm --version` |
| Docker + Docker Compose | 24+ | `docker --version && docker compose version` |

### 2. Clonar o repositório

```bash
git clone https://github.com/pedrohmvalenca/valora.git
cd valora
```

### 3. Subir o banco PostgreSQL (Docker)

```bash
cd valora-backend
docker compose up -d postgres
```

O banco fica disponível em `localhost:5433` (porta 5433 evita conflito com Postgres do host na 5432).

### 4. Configurar variáveis de ambiente do backend

```bash
cp .env.example .env
```

O `.env` já vem com defaults para dev local — não precisa editar.

### 5. Subir o backend Spring Boot

```bash
./mvnw spring-boot:run
```

> No Windows PowerShell use `./mvnw.cmd spring-boot:run`.

API disponível em `http://localhost:8080/api/v1`. Swagger em `http://localhost:8080/swagger-ui.html`.
Para usar outra porta: `SERVER_PORT=8081 ./mvnw spring-boot:run`.

### 6. Subir o frontend (em outro terminal)

```bash
cd valora-frontend
cp .env.example .env.local      # ajuste VITE_API_BASE se backend não estiver em :8080
npm install
npm run dev
```

App disponível em `http://localhost:5173`.

### 7. Login inicial (seed de demonstração)

| Perfil | E-mail | Senha |
|---|---|---|
| Administrador | `admin@valora.local` | `Admin@123` |
| Coord ADS | `ana.coord@valora.local` | `Admin@123` |
| Coord Jogos Digitais | `bia.coord@valora.local` | `Admin@123` |
| Coord Culinária | `renato.coord@valora.local` | `Admin@123` |
| Coord Administração | `sandra.coord@valora.local` | `Admin@123` |
| Coord Estética | `patricia.coord@valora.local` | `Admin@123` |

> Senhas dos coordenadores são iguais à do admin **apenas em ambiente local de demonstração**. Em produção cada conta tem credenciais únicas.

---

## Comandos úteis

### Backend

```bash
cd valora-backend

./mvnw clean compile               # compilar
./mvnw clean verify                # rodar testes (JUnit + Testcontainers)
./mvnw spring-boot:run             # subir servidor

docker compose up -d postgres      # subir Postgres
docker compose down                # parar (mantém volume)
docker compose down -v             # parar e apagar dados
```

### Frontend

```bash
cd valora-frontend

npm install                        # instalar deps
npm run dev                        # dev server (HMR)
npm run lint                       # ESLint
npm run build                      # build de produção
npm run preview                    # preview do build
```

---

## O que está implementado (Entrega 1 — PWA Admin/Coord)

- Autenticação real com JWT em cookie httpOnly + SameSite=Strict + BCrypt cost 12
- RBAC em camadas (filter chain + method-level + service)
- Bootstrap silencioso de sessão (`GET /auth/me`) preserva login em F5
- Logout com revogação server-side do cookie
- CRUD básico de cursos, coordenadores, alunos (multi-bacharel) e categorias
- Validação de submissões com cálculo automático de saldo por (aluno × curso × categoria)
- Aprovação parcial: ajuste de horas reconhecidas quando solicitação excede saldo
- Reprovação com motivo obrigatório
- Trilha de auditoria assíncrona (login, logout, decisões de submissão) consultável via API admin
- Cobertura de testes JUnit + Testcontainers (~50+ testes)

## Pendente (path 2.0 — a caminho)

- Notificações por e-mail (aprovação/reprovação)
- Trocar senha pelo próprio usuário
- Self-service do aluno (listar próprias submissões pelo PWA)
- Submissão pelo aluno via app mobile
- Dashboards de KPIs com dados reais
- OCR de comprovantes
- Edição/exclusão de cursos, coordenadores, alunos e categorias

---

## Licença

Acadêmica — Senac PE, Projeto Integrador ADS-3 2026.
