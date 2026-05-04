-- V100__seed_submissions_demo.sql
-- Story 4.5 (consolidada γ) — Seed de demo para protótipo
--
-- ⚠️  APENAS DEV LOCAL/DEMO. Em prod/homol, este arquivo NÃO deve ser carregado
-- (via Flyway location filtrado por profile, ou simplesmente não copiado pra build prod).
--
-- Cria: 2 cursos, 2 coordenadores, 4 alunos, 3 categorias com limites,
-- 8 submissões mistas (PENDING/APPROVED/REJECTED) com cenário de saldo:
-- - Aluno "ALICE" tem 25h APPROVED em Monitoria/ADS (max 60h) → saldo 35h
--   (próxima submissão pendente de 30h Monitoria/ADS pode ser aprovada)
-- - Aluno "BRUNO" tem 50h APPROVED em Monitoria/ADS → saldo 10h
--   (próxima submissão pendente de 25h Monitoria/ADS DEVE FALHAR — excede saldo)
--
-- Senha de coordenadores: Admin@123 (mesmo hash BCrypt do seed Admin V1)
-- — não-realista mas suficiente pra demo. Trocar em prod.

DO $$
DECLARE
    -- IDs determinísticos (mesmo hash em rodadas repetidas — facilita troubleshooting)
    course_ads_id  UUID := '00000000-0000-0000-0000-000000c01ad5';
    course_djd_id  UUID := '00000000-0000-0000-0000-000000c0d1d2';
    coord_ana_id   UUID := '00000000-0000-0000-0000-000000c00a01';
    coord_bia_id   UUID := '00000000-0000-0000-0000-000000c00b02';
    student_alice_id  UUID := '00000000-0000-0000-0000-0000000a11ce';
    student_bruno_id  UUID := '00000000-0000-0000-0000-0000000b2110';
    student_carla_id  UUID := '00000000-0000-0000-0000-0000000ca21a';
    student_diego_id  UUID := '00000000-0000-0000-0000-0000000d1e60';
    cat_monitoria_id  UUID := '00000000-0000-0000-0000-00000ca7e401';
    cat_pesquisa_id   UUID := '00000000-0000-0000-0000-00000ca7e402';
    cat_eventos_id    UUID := '00000000-0000-0000-0000-00000ca7e403';
    -- Hash BCrypt para "Admin@123" (mesmo do seed Admin V1) — apenas dev
    coord_password_hash VARCHAR(255) := '$2b$12$9qXNdy6QXExGNlC0Gkecoua59XBL6XrKAr8aFdOnGijDqNBjp28K2';
BEGIN
    -- ============================================================
    -- Cursos
    -- ============================================================
    INSERT INTO courses (id, name, code, minimum_workload_hours, is_active)
    VALUES
        (course_ads_id, 'Análise e Desenvolvimento de Sistemas', 'ADS-2026', 100, TRUE),
        (course_djd_id, 'Design e Desenvolvimento de Jogos',     'DJD-2026', 100, TRUE);

    -- ============================================================
    -- Coordenadores (User com profile=COORDINATOR)
    -- ============================================================
    INSERT INTO users (id, email, password_hash, name, profile, is_active)
    VALUES
        (coord_ana_id, 'ana.coord@valora.local', coord_password_hash, 'Ana Paula (Coord ADS)', 'COORDINATOR', TRUE),
        (coord_bia_id, 'bia.coord@valora.local', coord_password_hash, 'Bianca Costa (Coord DJD)', 'COORDINATOR', TRUE);

    -- ============================================================
    -- Vínculos coordenador × curso
    -- ============================================================
    INSERT INTO coordinator_course (coordinator_id, course_id)
    VALUES
        (coord_ana_id, course_ads_id),
        (coord_bia_id, course_djd_id);

    -- ============================================================
    -- Alunos (Alice multi-bacharel ADS+DJD; demais em 1 curso)
    -- ============================================================
    INSERT INTO students (id, registration_code, name, email, is_active)
    VALUES
        (student_alice_id, '2026001', 'Alice Silva',     'alice@aluno.senac.local', TRUE),
        (student_bruno_id, '2026002', 'Bruno Oliveira',  'bruno@aluno.senac.local', TRUE),
        (student_carla_id, '2026003', 'Carla Mendes',    'carla@aluno.senac.local', TRUE),
        (student_diego_id, '2026004', 'Diego Ferreira',  'diego@aluno.senac.local', TRUE);

    INSERT INTO student_course (student_id, course_id)
    VALUES
        (student_alice_id, course_ads_id),
        (student_alice_id, course_djd_id),  -- Alice é multi-bacharel
        (student_bruno_id, course_ads_id),
        (student_carla_id, course_ads_id),
        (student_diego_id, course_djd_id);

    -- ============================================================
    -- Categorias com limite por curso
    -- ============================================================
    INSERT INTO categories (id, name, group_type, description)
    VALUES
        (cat_monitoria_id, 'Monitoria',           'TEACHING', 'Atividades de monitoria em disciplinas regulares'),
        (cat_pesquisa_id,  'Iniciação Científica','RESEARCH', 'Participação em projetos de pesquisa orientados'),
        (cat_eventos_id,   'Eventos',             'EXTENSION','Participação em eventos acadêmicos e profissionais');

    INSERT INTO category_course (category_id, course_id, max_hours)
    VALUES
        (cat_monitoria_id, course_ads_id, 60),
        (cat_pesquisa_id,  course_ads_id, 80),
        (cat_eventos_id,   course_ads_id, 40),
        (cat_monitoria_id, course_djd_id, 60),
        (cat_pesquisa_id,  course_djd_id, 60),
        (cat_eventos_id,   course_djd_id, 50);

    -- ============================================================
    -- Submissões — cenário de saldo
    -- ALICE/Monitoria/ADS: 25h APPROVED + 30h PENDING → saldo restante 35h, pode aprovar pendente
    -- BRUNO/Monitoria/ADS: 50h APPROVED + 25h PENDING → saldo restante 10h, NÃO pode aprovar pendente (excede)
    -- ============================================================
    INSERT INTO submissions (id, student_id, course_id, category_id, description, requested_hours, proof_path, status, decided_by, decided_at)
    VALUES
        -- APPROVED (popular saldos)
        (gen_random_uuid(), student_alice_id, course_ads_id, cat_monitoria_id,
         'Monitoria de Algoritmos I — semestre 2025.2 (25h)', 25,
         'demo/alice-monitoria-25h.pdf', 'APPROVED', coord_ana_id, NOW() - INTERVAL '30 days'),
        (gen_random_uuid(), student_bruno_id, course_ads_id, cat_monitoria_id,
         'Monitoria de Programação Web — semestre 2025.2 (50h)', 50,
         'demo/bruno-monitoria-50h.pdf', 'APPROVED', coord_ana_id, NOW() - INTERVAL '20 days');

    INSERT INTO submissions (id, student_id, course_id, category_id, description, requested_hours, proof_path, status, decided_by, decided_at, rejection_reason)
    VALUES
        -- REJECTED (motivo ≥20 chars)
        (gen_random_uuid(), student_carla_id, course_ads_id, cat_eventos_id,
         'Curso Duolingo English Test 30h', 30,
         'demo/carla-duolingo.pdf', 'REJECTED', coord_ana_id, NOW() - INTERVAL '10 days',
         'Duolingo não é reconhecido pelo Manual NDE 2022 do Senac PE como AC válida para a categoria Eventos. Plataforma de gamificação informal não atende ao critério de carga horária estruturada exigido.');

    INSERT INTO submissions (id, student_id, course_id, category_id, description, requested_hours, proof_path, status)
    VALUES
        -- PENDING (5 — fila pra Coord trabalhar)
        (gen_random_uuid(), student_alice_id, course_ads_id, cat_monitoria_id,
         'Monitoria de Algoritmos II — semestre 2026.1 (30h) — pode aprovar (saldo 35h)', 30,
         'demo/alice-monitoria-30h-novo.pdf', 'PENDING'),
        (gen_random_uuid(), student_bruno_id, course_ads_id, cat_monitoria_id,
         'Monitoria de Banco de Dados — semestre 2026.1 (25h) — DEVE FALHAR (saldo 10h)', 25,
         'demo/bruno-monitoria-25h.pdf', 'PENDING'),
        (gen_random_uuid(), student_alice_id, course_djd_id, cat_pesquisa_id,
         'Iniciação Científica em Game Design (40h)', 40,
         'demo/alice-pesquisa-djd.pdf', 'PENDING'),
        (gen_random_uuid(), student_carla_id, course_ads_id, cat_pesquisa_id,
         'Bolsista IC — Pesquisa em Sistemas Distribuídos (60h)', 60,
         'demo/carla-pesquisa.pdf', 'PENDING'),
        (gen_random_uuid(), student_diego_id, course_djd_id, cat_eventos_id,
         'Participação Game Jam Senac 2026 (16h)', 16,
         'demo/diego-gamejam.pdf', 'PENDING');
END $$;
