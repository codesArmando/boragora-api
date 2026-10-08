-- Esquema inicial conforme o MER (mer_ajustado_app_van_dark.html) e a Etapa 1 do plano.
-- Enums gravados como texto. Exclusão de turma apaga vínculos, viagens e confirmações;
-- exclusão de grupo com turmas é rejeitada pela chave turma.id_grupo (sem cascata).

CREATE TABLE usuario (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nome_pessoa     VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    senha_hash      VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    admin           BOOLEAN      NOT NULL DEFAULT FALSE,
    expo_push_token VARCHAR(255) NULL,
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE grupo (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    nome                VARCHAR(100) NOT NULL,
    periodo             VARCHAR(10)  NOT NULL,
    horario_saida_ida   TIME         NOT NULL,
    horario_saida_volta TIME         NOT NULL,
    CONSTRAINT pk_grupo PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE turma (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    id_grupo     BIGINT      NOT NULL,
    id_motorista BIGINT      NOT NULL,
    nome         VARCHAR(60) NOT NULL,
    numero_van   INT         NULL,
    capacidade   INT         NULL,
    CONSTRAINT pk_turma PRIMARY KEY (id),
    CONSTRAINT uk_turma_grupo_motorista UNIQUE (id_grupo, id_motorista),
    CONSTRAINT fk_turma_grupo FOREIGN KEY (id_grupo) REFERENCES grupo (id),
    CONSTRAINT fk_turma_motorista FOREIGN KEY (id_motorista) REFERENCES usuario (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE turma_aluno (
    id       BIGINT  NOT NULL AUTO_INCREMENT,
    id_turma BIGINT  NOT NULL,
    id_aluno BIGINT  NOT NULL,
    ativo    BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_turma_aluno PRIMARY KEY (id),
    CONSTRAINT uk_turma_aluno_turma_aluno UNIQUE (id_turma, id_aluno),
    CONSTRAINT fk_turma_aluno_turma FOREIGN KEY (id_turma) REFERENCES turma (id) ON DELETE CASCADE,
    CONSTRAINT fk_turma_aluno_aluno FOREIGN KEY (id_aluno) REFERENCES usuario (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE viagem (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    id_turma                BIGINT NOT NULL,
    data                    DATE   NOT NULL,
    id_viagem_absorvida_por BIGINT NULL,
    CONSTRAINT pk_viagem PRIMARY KEY (id),
    CONSTRAINT uk_viagem_turma_data UNIQUE (id_turma, data),
    CONSTRAINT fk_viagem_turma FOREIGN KEY (id_turma) REFERENCES turma (id) ON DELETE CASCADE,
    CONSTRAINT fk_viagem_absorvida_por FOREIGN KEY (id_viagem_absorvida_por) REFERENCES viagem (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE confirmacao (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    id_viagem           BIGINT      NOT NULL,
    id_turma_aluno      BIGINT      NOT NULL,
    inclui_ida          BOOLEAN     NOT NULL DEFAULT FALSE,
    inclui_volta        BOOLEAN     NOT NULL DEFAULT FALSE,
    status_volta        VARCHAR(10) NOT NULL DEFAULT 'EM_AULA',
    cancelou_apos_saida BOOLEAN     NOT NULL DEFAULT FALSE,
    atualizado_em       DATETIME    NOT NULL,
    CONSTRAINT pk_confirmacao PRIMARY KEY (id),
    CONSTRAINT uk_confirmacao_viagem_turma_aluno UNIQUE (id_viagem, id_turma_aluno),
    CONSTRAINT fk_confirmacao_viagem FOREIGN KEY (id_viagem) REFERENCES viagem (id) ON DELETE CASCADE,
    CONSTRAINT fk_confirmacao_turma_aluno FOREIGN KEY (id_turma_aluno) REFERENCES turma_aluno (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
