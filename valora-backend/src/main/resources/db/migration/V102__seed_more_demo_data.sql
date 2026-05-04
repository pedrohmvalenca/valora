-- V102__seed_more_demo_data.sql
-- Tier extra γ — mais dados de demo para banca
--
-- Conforme decisão PO 2026-05-04: cursos ADS, Culinária, Administração,
-- Jogos Digitais e Estética/Cosmética. ADS+DJD já existem (V100).
-- Adiciona Culinária, Administração e Estética + coordenadores + alunos +
-- categoria "Estágio Não-Obrigatório" + submissões variadas (cobertura de
-- cenários: cabe inteiro, cabe parcial, excede totalmente).

DO $$
DECLARE
    course_cul_id   UUID := '00000000-0000-0000-0000-000000c0c01a';
    course_adm_id   UUID := '00000000-0000-0000-0000-000000c0adb1';
    course_est_id   UUID := '00000000-0000-0000-0000-000000c0e572';
    coord_renato_id UUID := '00000000-0000-0000-0000-000000c00c03';
    coord_sandra_id UUID := '00000000-0000-0000-0000-000000c00d04';
    coord_patricia_id UUID := '00000000-0000-0000-0000-000000c00e05';
    student_eduardo_id  UUID := '00000000-0000-0000-0000-0000000ed5a0';
    student_fernanda_id UUID := '00000000-0000-0000-0000-0000000fe2da';
    student_gabriel_id  UUID := '00000000-0000-0000-0000-0000000ba121';
    student_helena_id   UUID := '00000000-0000-0000-0000-0000000be1ea';
    student_igor_id     UUID := '00000000-0000-0000-0000-0000000196a0';
    student_julia_id    UUID := '00000000-0000-0000-0000-00000003011a';
    student_karen_id    UUID := '00000000-0000-0000-0000-0000000ca7e0';
    cat_estagio_id      UUID := '00000000-0000-0000-0000-00000ca7e404';
    -- IDs de categorias do V100 (referência cruzada)
    cat_monitoria_id    UUID := '00000000-0000-0000-0000-00000ca7e401';
    cat_pesquisa_id     UUID := '00000000-0000-0000-0000-00000ca7e402';
    cat_eventos_id      UUID := '00000000-0000-0000-0000-00000ca7e403';
    -- IDs de cursos do V100 (referência)
    course_ads_id       UUID := '00000000-0000-0000-0000-000000c01ad5';
    course_djd_id       UUID := '00000000-0000-0000-0000-000000c0d1d2';
    coord_password_hash VARCHAR(255) := '$2b$12$9qXNdy6QXExGNlC0Gkecoua59XBL6XrKAr8aFdOnGijDqNBjp28K2';
BEGIN
    -- ============================================================
    -- 3 cursos novos
    -- ============================================================
    INSERT INTO courses (id, name, code, minimum_workload_hours, is_active) VALUES
        (course_cul_id, 'Culinária',              'CUL-2026', 100, TRUE),
        (course_adm_id, 'Administração',          'ADM-2026', 100, TRUE),
        (course_est_id, 'Estética e Cosmética',   'EST-2026', 100, TRUE);

    -- ============================================================
    -- 3 coordenadores novos
    -- ============================================================
    INSERT INTO users (id, email, password_hash, name, profile, is_active) VALUES
        (coord_renato_id,   'renato.coord@valora.local',   coord_password_hash, 'Renato Lopes (Coord Culinária)',          'COORDINATOR', TRUE),
        (coord_sandra_id,   'sandra.coord@valora.local',   coord_password_hash, 'Sandra Mendes (Coord Administração)',     'COORDINATOR', TRUE),
        (coord_patricia_id, 'patricia.coord@valora.local', coord_password_hash, 'Patrícia Dias (Coord Estética)',          'COORDINATOR', TRUE);

    INSERT INTO coordinator_course (coordinator_id, course_id) VALUES
        (coord_renato_id,   course_cul_id),
        (coord_sandra_id,   course_adm_id),
        (coord_patricia_id, course_est_id);

    -- ============================================================
    -- 7 alunos novos (Eduardo multi-bacharel CUL+ADM pra demonstrar EXT-01)
    -- ============================================================
    INSERT INTO students (id, registration_code, name, email, is_active) VALUES
        (student_eduardo_id,  '2026005', 'Eduardo Martins',  'eduardo@aluno.senac.local',  TRUE),
        (student_fernanda_id, '2026006', 'Fernanda Lima',    'fernanda@aluno.senac.local', TRUE),
        (student_gabriel_id,  '2026007', 'Gabriel Castro',   'gabriel@aluno.senac.local',  TRUE),
        (student_helena_id,   '2026008', 'Helena Rocha',     'helena@aluno.senac.local',   TRUE),
        (student_igor_id,     '2026009', 'Igor Pereira',     'igor@aluno.senac.local',     TRUE),
        (student_julia_id,    '2026010', 'Júlia Santos',     'julia@aluno.senac.local',    TRUE),
        (student_karen_id,    '2026011', 'Karen Oliveira',   'karen@aluno.senac.local',    TRUE);

    INSERT INTO student_course (student_id, course_id) VALUES
        (student_eduardo_id,  course_cul_id),
        (student_eduardo_id,  course_adm_id),  -- multi-bacharel CUL+ADM
        (student_fernanda_id, course_cul_id),
        (student_gabriel_id,  course_adm_id),
        (student_helena_id,   course_adm_id),
        (student_igor_id,     course_adm_id),
        (student_julia_id,    course_est_id),
        (student_karen_id,    course_est_id);

    -- ============================================================
    -- 1 categoria nova + estender existentes para os 3 cursos novos
    -- ============================================================
    INSERT INTO categories (id, name, group_type, description) VALUES
        (cat_estagio_id, 'Estágio Não-Obrigatório', 'TEACHING',
         'Estágio realizado fora do componente curricular obrigatório, com supervisão.');

    -- Limites para cursos novos (e categoria nova nos cursos antigos)
    INSERT INTO category_course (category_id, course_id, max_hours) VALUES
        -- Monitoria nos cursos novos
        (cat_monitoria_id, course_cul_id, 50),
        (cat_monitoria_id, course_adm_id, 60),
        (cat_monitoria_id, course_est_id, 40),
        -- Iniciação Científica nos cursos novos (limite menor que ADS/DJD)
        (cat_pesquisa_id,  course_cul_id, 40),
        (cat_pesquisa_id,  course_adm_id, 60),
        (cat_pesquisa_id,  course_est_id, 30),
        -- Eventos nos cursos novos
        (cat_eventos_id,   course_cul_id, 50),
        (cat_eventos_id,   course_adm_id, 40),
        (cat_eventos_id,   course_est_id, 40),
        -- Categoria nova (Estágio) em TODOS os 5 cursos
        (cat_estagio_id,   course_ads_id, 60),
        (cat_estagio_id,   course_djd_id, 60),
        (cat_estagio_id,   course_cul_id, 60),
        (cat_estagio_id,   course_adm_id, 80),
        (cat_estagio_id,   course_est_id, 60);

    -- ============================================================
    -- Submissões — cenários variados pra demo
    -- ============================================================

    -- 1) APPROVED prévias para popular saldos (não aparecem na fila PENDING)
    INSERT INTO submissions
        (id, student_id, course_id, category_id, description, requested_hours,
         recognized_hours, proof_path, status, decided_by, decided_at)
    VALUES
        -- Eduardo CUL/Estágio: 40h aprovadas (max 60h) → resta 20h
        (gen_random_uuid(), student_eduardo_id, course_cul_id, cat_estagio_id,
         'Estágio em restaurante Vila Madalena (40h)', 40, 40,
         'demo/eduardo-estagio-40h.pdf', 'APPROVED', coord_renato_id, NOW() - INTERVAL '15 days'),
        -- Helena ADM/Eventos: 30h aprovadas (max 40h) → resta 10h
        (gen_random_uuid(), student_helena_id, course_adm_id, cat_eventos_id,
         'Congresso Brasileiro de Administração 2025 (30h)', 30, 30,
         'demo/helena-cba.pdf', 'APPROVED', coord_sandra_id, NOW() - INTERVAL '20 days'),
        -- Júlia EST/Monitoria: 20h aprovadas (max 40h) → resta 20h
        (gen_random_uuid(), student_julia_id, course_est_id, cat_monitoria_id,
         'Monitoria de Cosmetologia (20h)', 20, 20,
         'demo/julia-monitoria.pdf', 'APPROVED', coord_patricia_id, NOW() - INTERVAL '12 days');

    -- 2) PENDING — cenário "cabe inteiro" (saldo OK)
    INSERT INTO submissions
        (id, student_id, course_id, category_id, description, requested_hours, proof_path, status)
    VALUES
        -- Fernanda CUL/Eventos 30h (saldo 50h, cabe)
        (gen_random_uuid(), student_fernanda_id, course_cul_id, cat_eventos_id,
         'Curso de Confeitaria Avançada SENAC SP (30h)', 30,
         'demo/fernanda-confeitaria.pdf', 'PENDING'),
        -- Gabriel ADM/Pesquisa 40h (saldo 60h, cabe)
        (gen_random_uuid(), student_gabriel_id, course_adm_id, cat_pesquisa_id,
         'IC em Gestão de Pessoas — semestre 2026.1 (40h)', 40,
         'demo/gabriel-ic-gp.pdf', 'PENDING'),
        -- Karen EST/Eventos 25h (saldo 40h, cabe)
        (gen_random_uuid(), student_karen_id, course_est_id, cat_eventos_id,
         'Workshop Maquiagem Profissional (25h)', 25,
         'demo/karen-workshop.pdf', 'PENDING'),
        -- Igor ADM/Estágio 50h (saldo 80h, cabe)
        (gen_random_uuid(), student_igor_id, course_adm_id, cat_estagio_id,
         'Estágio empresa júnior FEA-USP (50h)', 50,
         'demo/igor-estagio.pdf', 'PENDING');

    -- 3) PENDING — cenário "cabe parcial" (excede saldo, mas saldo > 0)
    INSERT INTO submissions
        (id, student_id, course_id, category_id, description, requested_hours, proof_path, status)
    VALUES
        -- Eduardo CUL/Estágio 35h — saldo 20h, cabe parcial
        (gen_random_uuid(), student_eduardo_id, course_cul_id, cat_estagio_id,
         'Estágio em padaria artesanal Lapa (35h) — DEMO PARCIAL: cabe só 20h', 35,
         'demo/eduardo-padaria.pdf', 'PENDING'),
        -- Helena ADM/Eventos 25h — saldo 10h, cabe parcial
        (gen_random_uuid(), student_helena_id, course_adm_id, cat_eventos_id,
         'Workshop ESG e Compliance (25h) — DEMO PARCIAL: cabe só 10h', 25,
         'demo/helena-esg.pdf', 'PENDING');

    -- 4) PENDING — cenário "excede TOTAL" (categoria esgotada)
    -- Pra demonstrar saldo zerado, vou aprovar uma de Júlia/Eventos esgotando
    INSERT INTO submissions
        (id, student_id, course_id, category_id, description, requested_hours,
         recognized_hours, proof_path, status, decided_by, decided_at)
    VALUES
        -- Júlia EST/Eventos: 40h aprovadas (= max 40h) → ZERA categoria
        (gen_random_uuid(), student_julia_id, course_est_id, cat_eventos_id,
         'Feira Beauty Fair 2025 (40h)', 40, 40,
         'demo/julia-beautyfair.pdf', 'APPROVED', coord_patricia_id, NOW() - INTERVAL '8 days');

    -- Agora a PENDING que vai falhar:
    INSERT INTO submissions
        (id, student_id, course_id, category_id, description, requested_hours, proof_path, status)
    VALUES
        -- Júlia EST/Eventos 15h — saldo 0h, NÃO PODE APROVAR
        (gen_random_uuid(), student_julia_id, course_est_id, cat_eventos_id,
         'Curso Visagismo Senac RJ (15h) — DEMO ESGOTADA: categoria zerada', 15,
         'demo/julia-visagismo.pdf', 'PENDING');

    -- 5) PENDING multi-bacharel (Eduardo) na ADM — saldo independente do CUL
    INSERT INTO submissions
        (id, student_id, course_id, category_id, description, requested_hours, proof_path, status)
    VALUES
        -- Eduardo ADM/Pesquisa 30h — saldo 60h ADM (CUL é independente — EXT-02)
        (gen_random_uuid(), student_eduardo_id, course_adm_id, cat_pesquisa_id,
         'IC em Gestão Gastronômica (30h) — DEMO multi-bacharel: saldo ADM ≠ CUL', 30,
         'demo/eduardo-ic-adm.pdf', 'PENDING');

END $$;
