CREATE TABLE ai_analysis_result (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    theme_id BIGINT,
    package_id BIGINT,
    result_type VARCHAR(32) NOT NULL,
    verdict VARCHAR(32),
    result_json TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 1,
    error_message TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analysis_material ON ai_analysis_result(material_id);
