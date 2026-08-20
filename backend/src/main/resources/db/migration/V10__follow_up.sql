CREATE TABLE ai_follow_up (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    suggestion TEXT,
    source_label VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'TODO',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_follow_up_material ON ai_follow_up(material_id);
CREATE UNIQUE INDEX uk_follow_up_material_title ON ai_follow_up(material_id, title);
