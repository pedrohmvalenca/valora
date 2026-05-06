#!/bin/sh
# =============================================================================
# Entrypoint do container — converte DATABASE_URL do Render (postgres://) para
# o formato JDBC que o Spring Boot espera (jdbc:postgresql://) e propaga as
# credenciais como SPRING_DATASOURCE_USERNAME/PASSWORD.
#
# Por que isto existe:
#   - Render Postgres expõe DATABASE_URL como `postgres://user:pass@host:5432/db`
#     (formato libpq/PostgreSQL canônico).
#   - Spring DataSource exige `jdbc:postgresql://host:5432/db` + username/password
#     em propriedades separadas.
#   - render.yaml não suporta concatenação de strings entre env vars; converter
#     na hora do boot é a opção mais simples e portável.
#
# Em local/dev, DATABASE_URL já vem com prefixo `jdbc:` (application-local.yml
# ou docker-compose) → este script é noop nesse caso.
# =============================================================================
set -eu

if [ -n "${DATABASE_URL:-}" ]; then
    case "$DATABASE_URL" in
        jdbc:*)
            # Já está no formato JDBC — nada a fazer.
            ;;
        postgres://*|postgresql://*)
            # Strip protocol
            stripped="${DATABASE_URL#postgres://}"
            stripped="${stripped#postgresql://}"

            # Split em user:pass@host_e_resto
            userpass="${stripped%%@*}"
            hostpart="${stripped#*@}"

            # Split user / pass
            db_user="${userpass%%:*}"
            db_pass="${userpass#*:}"

            export SPRING_DATASOURCE_URL="jdbc:postgresql://${hostpart}"
            export SPRING_DATASOURCE_USERNAME="$db_user"
            export SPRING_DATASOURCE_PASSWORD="$db_pass"
            # Limpa DATABASE_URL para o Spring não pegar o formato errado pelo
            # placeholder em application.yml (que tem fallback obrigatório).
            unset DATABASE_URL
            echo "[entrypoint] DATABASE_URL convertida postgres:// -> jdbc:postgresql://"
            ;;
        *)
            echo "[entrypoint] WARN: DATABASE_URL com formato desconhecido — passando direto"
            ;;
    esac
fi

# shellcheck disable=SC2086
exec java $JAVA_OPTS -jar /app/app.jar
