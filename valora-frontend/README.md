# valora-frontend

PWA do **VALORA** — sistema de gestão de atividades complementares do Senac PE
(Validação · Acompanhamento · Lançamento · Organização · Reconhecimento Acadêmico).

Stack: **React 18 + JavaScript ES6+ + Vite + Tailwind + shadcn/ui** (refactor Lovable — ADR-0002).

---

## Pré-requisitos

| Ferramenta | Versão alvo |
|---|---|
| **Node.js** | 20 LTS (testado em 24.x também) |
| **npm**     | 10+ |

Instalação na máquina (se faltar) — ver `docs/HANDOFF-MAQUINA-2.md` § 3:

```powershell
winget install --id OpenJS.NodeJS.LTS -e --accept-source-agreements --accept-package-agreements
```

## Setup local em 3 passos

```bash
cd valora-frontend

# 1. Dependências
npm install

# 2. Variáveis de ambiente
cp .env.example .env

# 3. Subir
npm run dev   # http://localhost:5173
```

## Scripts

| Comando | O que faz |
|---|---|
| `npm run dev` | Sobe o Vite dev server em `:5173` com HMR |
| `npm run build` | Build de produção em `dist/` |
| `npm run build:dev` | Build sem otimizações (debug) |
| `npm run lint` | ESLint sobre todo o `src/` |
| `npm run preview` | Preview do build de produção |

## Estrutura

```
src/
├── main.jsx
├── App.jsx                       # Provider tree: Theme > Router > Auth > Curso > Tooltip + Routes
├── index.css                     # Tailwind + tokens VALORA Alt 3 oficial + Inter/JetBrains Mono
├── components/
│   ├── ui/                       # shadcn (NÃO modificar diretamente)
│   ├── AppShell.jsx              # Wrapper composable
│   ├── AppShellSidebar.jsx       # 240px desktop / drawer mobile
│   ├── AppShellHeader.jsx        # Sticky 56px (slot p/ CursoSelector — Story 1.7)
│   ├── AppShellContent.jsx       # <main> + <Outlet/>
│   ├── ThemeToggle.jsx           # Sun/Moon via next-themes
│   └── RoleGuard.jsx             # placeholder — Story 1.5 estende com checagem de papel
├── contexts/
│   ├── ThemeProvider.jsx         # next-themes wrapper, dark default + sync data-theme
│   ├── AuthContext.jsx           # 3 perfis (ADMIN/COORD/ALUNO) + cursosVinculados; estado React only
│   └── CursoContext.jsx          # STUB — Story 1.7 implementa lógica completa
├── hooks/                        # use-mobile (shadcn helpers)
├── lib/
│   ├── utils.js                  # cn() helper
│   └── constants.js              # Papel enum
└── pages/
    ├── Login.jsx                 # placeholder visual — Story 1.5 conecta API
    ├── Index.jsx                 # placeholder dentro do AppShell
    └── NotFound.jsx
```

## Paleta VALORA

- **Alt 3 OFICIAL (dark)** — rose-coral suave em outline+ícone para REPROVADA/destrutivo;
  vermelho-cheio (`--destructive-strong`) reservado para confirmação irreversível RARA.
- **Alt 1 BACKUP (dark)** — fill rose-coral em badges. **Aplicar SOMENTE** se Alt 3 ficar austera
  na validação visual com muitos badges REPROVADA juntos. Custo de troca ~1h
  (apenas tokens CSS, sem rework de componentes).
- **Light mode** — único; vermelho convencional saturado.

Tokens completos em `src/index.css` (`:root` para light, `.dark` para Alt 3).

## Decisões herdadas (não re-decidir nesta entrega)

- **ADR-0002** — JavaScript ES6+ (não TypeScript)
- **F1 (Architecture)** — Context API global (sem Redux/Zustand, sem react-query Provider)
- **F2 (Architecture)** — Axios HTTP client com `withCredentials: true` (uso em Story 1.5)
- **F3 (Architecture)** — `next-themes` para dark/light
- **ADR-0010** — Sem suíte de testes automatizada no frontend; validação visual pelo agente Murat
  via Playwright CLI exploration

## shadcn CLI

`components.json` está configurado com `tsx: true` (default Lovable). Após
`npx shadcn-ui@latest add <componente>`, o arquivo gerado vem `.tsx` —
**renomear manualmente para `.jsx`** e remover annotations TS antes de commitar.

## Migração

Esta pasta foi renomeada de `senac-hour-tracker-main/` para `valora-frontend/` na
**Story 1.2** (01/05/2026). Histórico do scaffold Lovable original disponível
no zip de handoff (não preservado em git da raiz `valora/`, que ainda não foi
inicializado — ver `docs/HANDOFF-MAQUINA-2.md`).
