-- V105__add_must_change_password_to_users.sql
-- Story 1.11 — Forçar troca de senha no primeiro acesso.
--
-- Adiciona flag must_change_password em users. Alunos cadastrados a partir
-- desta migration nascem com true (POST /students). Usuários antigos (admin
-- seed, coordenadores, alunos pré-1.11) ficam false — não forçamos retroativo.
--
-- Numeração V105 segue a convenção out-of-order do repo (V104 foi renomeada
-- de V4 na Story 3.10 porque V100/101/102/103 já existiam antes).

ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
