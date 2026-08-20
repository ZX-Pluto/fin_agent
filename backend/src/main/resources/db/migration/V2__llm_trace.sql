CREATE TABLE ai_llm_trace (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT,
    material_id BIGINT,
    agent_name VARCHAR(64),
    skill_name VARCHAR(128),
    model_name VARCHAR(128),
    provider VARCHAR(32),
    prompt TEXT,
    response TEXT,
    input_tokens INT,
    output_tokens INT,
    latency_ms BIGINT,
    status VARCHAR(16) NOT NULL,
    error_message TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_llm_trace_material ON ai_llm_trace(material_id);
CREATE INDEX idx_llm_trace_task ON ai_llm_trace(task_id);
