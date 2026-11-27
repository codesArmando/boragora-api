-- =====================================================================================
-- Boragora Viagens: banco dos testes manuais
-- =====================================================================================
-- Apaga TODOS os dados do banco e recria o cenário do roteiro "Testes Manuais".
-- O esquema (tabelas) não muda: ele é do Flyway e é criado quando a API sobe pela 1ª vez.
--
-- As confirmações ficam no dia @dia_teste, ou no dia de hoje do MySQL se ele não for
-- informado. Os scripts aplicar-banco.sh e aplicar-banco.ps1 definem @dia_teste quando
-- recebem uma data. Reaplicar o script volta tudo ao estado inicial, sempre com os mesmos IDs.
--
-- Senhas: admin@boragora.local usa admin123; os demais usuários usam senha123.
-- =====================================================================================

SET NAMES utf8mb4;
SET @dia = COALESCE(@dia_teste, CURDATE());

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE confirmacao;
TRUNCATE TABLE viagem;
TRUNCATE TABLE turma_aluno;
TRUNCATE TABLE turma;
TRUNCATE TABLE grupo;
TRUNCATE TABLE usuario;
SET FOREIGN_KEY_CHECKS = 1;

-- Hashes BCrypt gerados pela própria API (BCryptPasswordEncoder).
SET @admin123 = '$2a$10$rb6.i9kN9AuGFQ/iyuP4nea2rqPUNUGS/wGvR3QFMYhU.QsiMzDZK';
SET @senha123 = '$2a$10$xL/6UpqWnJCSFTF8tsUAGetOy4nxOeIEPvKj9MH5q2/mBLl6paDqa';

-- -------------------------------------------------------------------------------------
-- Usuários
-- -------------------------------------------------------------------------------------
INSERT INTO usuario (id, nome_pessoa, email, senha_hash, role, admin) VALUES
    ( 1, 'Motorista-chefe',     'admin@boragora.local',     @admin123, 'MOTORISTA', TRUE),
    ( 2, 'Carlos Menezes',      'carlos@boragora.local',    @senha123, 'MOTORISTA', FALSE),
    ( 3, 'Vanderlei Dias',      'vanderlei@boragora.local', @senha123, 'MOTORISTA', FALSE),
    ( 4, 'João Gabriel',        'joao@boragora.local',      @senha123, 'ALUNO',     FALSE),
    ( 5, 'Beatriz Souza',       'beatriz@boragora.local',   @senha123, 'ALUNO',     FALSE),
    ( 6, 'Camila Rocha',        'camila@boragora.local',    @senha123, 'ALUNO',     FALSE),
    ( 7, 'Larissa Costa',       'larissa@boragora.local',   @senha123, 'ALUNO',     FALSE),
    ( 8, 'Lucas Pereira',       'lucas@boragora.local',     @senha123, 'ALUNO',     FALSE),
    ( 9, 'Mariana Alves',       'mariana@boragora.local',   @senha123, 'ALUNO',     FALSE),
    (10, 'Pedro Henrique Dias', 'pedro@boragora.local',     @senha123, 'ALUNO',     FALSE),
    (11, 'Rafael Lima',         'rafael@boragora.local',    @senha123, 'ALUNO',     FALSE),
    (12, 'Ana Beatriz Moura',   'ana@boragora.local',       @senha123, 'ALUNO',     FALSE),
    (13, 'Diego Farias',        'diego@boragora.local',     @senha123, 'ALUNO',     FALSE),
    (14, 'Vitor Sampaio',       'vitor@boragora.local',     @senha123, 'ALUNO',     FALSE);

-- -------------------------------------------------------------------------------------
-- Grupos e turmas
-- -------------------------------------------------------------------------------------
INSERT INTO grupo (id, nome, periodo, horario_saida_ida, horario_saida_volta) VALUES
    (1, 'Manhã', 'MANHA', '06:20:00', '12:10:00'),
    (2, 'Noite', 'NOITE', '18:10:00', '22:30:00');

INSERT INTO turma (id, id_grupo, id_motorista, nome, numero_van, capacidade) VALUES
    (1, 1, 2, 'Van 1', 1, 15),  -- Manhã · Van 1, Carlos
    (2, 1, 3, 'Van 2', 2, 15),  -- Manhã · Van 2, Vanderlei (sem alunos: tela C5 sem confirmações)
    (3, 2, 2, 'Van 1', 1, 15),  -- Noite · Van 1, Carlos
    (4, 2, 3, 'Van 2', 2, 15);  -- Noite · Van 2, Vanderlei (o cenário das telas C1 e C2)

-- -------------------------------------------------------------------------------------
-- Vínculos. Ana, Diego e Vitor ficam sem turma (telas A3, D6 e D10).
-- -------------------------------------------------------------------------------------
INSERT INTO turma_aluno (id, id_turma, id_aluno) VALUES
    (1, 1,  4),  -- João na Manhã · Van 1
    (2, 4,  5),  -- Noite · Van 2: Beatriz
    (3, 4,  6),  --                Camila
    (4, 4,  4),  --                João
    (5, 4,  7),  --                Larissa
    (6, 4,  8),  --                Lucas
    (7, 4,  9),  --                Mariana
    (8, 4, 10),  --                Pedro
    (9, 4, 11);  --                Rafael (não declara nada)

-- -------------------------------------------------------------------------------------
-- Plano do dia na Noite · Van 2: 5 vão, 7 voltam, 2 liberados, Camila cancelou depois da
-- saída das 18:10. Rafael não tem confirmação.
-- -------------------------------------------------------------------------------------
INSERT INTO viagem (id, id_turma, data) VALUES (1, 4, @dia);

INSERT INTO confirmacao
    (id_viagem, id_turma_aluno, inclui_ida, inclui_volta, status_volta, cancelou_apos_saida, atualizado_em)
VALUES
    (1, 2, TRUE,  TRUE, 'LIBERADO', FALSE, TIMESTAMP(@dia, '15:00:00')),  -- Beatriz
    (1, 3, FALSE, TRUE, 'EM_AULA',  TRUE,  TIMESTAMP(@dia, '18:25:00')),  -- Camila
    (1, 4, TRUE,  TRUE, 'EM_AULA',  FALSE, TIMESTAMP(@dia, '15:05:00')),  -- João
    (1, 5, TRUE,  TRUE, 'LIBERADO', FALSE, TIMESTAMP(@dia, '15:10:00')),  -- Larissa
    (1, 6, TRUE,  TRUE, 'EM_AULA',  FALSE, TIMESTAMP(@dia, '15:15:00')),  -- Lucas
    (1, 7, TRUE,  TRUE, 'EM_AULA',  FALSE, TIMESTAMP(@dia, '15:20:00')),  -- Mariana
    (1, 8, FALSE, TRUE, 'EM_AULA',  FALSE, TIMESTAMP(@dia, '15:25:00'));  -- Pedro

-- Conferência rápida, mostrada no fim da aplicação.
SELECT @dia AS dia_das_confirmacoes,
       (SELECT COUNT(*) FROM usuario)     AS usuarios,
       (SELECT COUNT(*) FROM grupo)       AS grupos,
       (SELECT COUNT(*) FROM turma)       AS turmas,
       (SELECT COUNT(*) FROM turma_aluno) AS vinculos,
       (SELECT COUNT(*) FROM confirmacao) AS confirmacoes;
