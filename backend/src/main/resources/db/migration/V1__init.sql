CREATE TABLE ai_material_task (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(255),
    task_type VARCHAR(32) NOT NULL DEFAULT 'MATERIAL_PARSE',
    status VARCHAR(32) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    current_agent VARCHAR(64),
    creator_id VARCHAR(64),
    error_message TEXT,
    params_json TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    start_time TIMESTAMP,
    finish_time TIMESTAMP
);

CREATE TABLE ai_material (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT,
    material_name VARCHAR(255) NOT NULL,
    material_type VARCHAR(16),
    source_type VARCHAR(16) NOT NULL DEFAULT 'UPLOAD',
    source_url TEXT,
    file_path TEXT,
    organization VARCHAR(128),
    report_period VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    confidence NUMERIC(5,4),
    ir_json TEXT,
    error_message TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_material_slide (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    slide_no INT NOT NULL,
    title VARCHAR(512),
    raw_text TEXT,
    structured_content TEXT,
    parse_status VARCHAR(32) NOT NULL DEFAULT 'PARSED'
);

CREATE TABLE ai_business_metric (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    task_id BIGINT,
    slide_id BIGINT,
    metric_name VARCHAR(128) NOT NULL,
    normalized_name VARCHAR(128),
    value NUMERIC(20,4),
    unit VARCHAR(32),
    period VARCHAR(32),
    source_refs TEXT,
    confidence NUMERIC(5,4),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_validation_result (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT,
    material_id BIGINT NOT NULL,
    rule_id BIGINT,
    rule_code VARCHAR(64),
    category VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    metric_name VARCHAR(128),
    actual_value TEXT,
    expected_value TEXT,
    message TEXT NOT NULL,
    suggestion TEXT,
    source_refs TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_rule_config (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(32) NOT NULL,
    description TEXT,
    severity VARCHAR(16) NOT NULL,
    params TEXT,
    builtin BOOLEAN NOT NULL DEFAULT TRUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_model_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    base_url VARCHAR(512) NOT NULL,
    api_key TEXT,
    model_name VARCHAR(128) NOT NULL,
    capabilities VARCHAR(64) NOT NULL DEFAULT 'TEXT',
    temperature NUMERIC(4,2),
    timeout_seconds INT DEFAULT 60,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_knowledge (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    task_id BIGINT,
    knowledge_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    source_refs TEXT,
    confidence NUMERIC(5,4),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_material_task ON ai_material(task_id);
CREATE INDEX idx_slide_material ON ai_material_slide(material_id);
CREATE INDEX idx_metric_material ON ai_business_metric(material_id);
CREATE INDEX idx_validation_material ON ai_validation_result(material_id);
CREATE INDEX idx_knowledge_material ON ai_knowledge(material_id);

INSERT INTO ai_rule_config (rule_code, name, category, description, severity, params, builtin, enabled) VALUES
('R-C01', '必选章节覆盖', 'COMPLETENESS', '检查经营概况、收入、利润、现金流、费用、库存、风险、下一步计划等必选章节是否缺失', 'MEDIUM',
 '{"sections":["经营概况","收入","利润","现金流","费用","库存","风险","下一步计划"]}', TRUE, TRUE),
('R-C02', '必填指标', 'COMPLETENESS', '检查收入、利润、现金流等必填指标是否缺失', 'CRITICAL',
 '{"metrics":["收入","利润","现金流"]}', TRUE, TRUE),
('R-C03', '期间标识', 'COMPLETENESS', '材料必须标注本期，如 2026Q2', 'MEDIUM', '{}', TRUE, TRUE),
('R-C04', '页数异常', 'COMPLETENESS', '材料页数低于阈值时提示', 'LOW', '{"minPages":15}', TRUE, TRUE),
('R-C05', '附件缺失', 'COMPLETENESS', '正文提到附件但未提供时提示', 'MEDIUM', '{}', TRUE, TRUE),
('R-T01', '同比复算', 'CREDIBILITY', '本期值/上年同期-1 与声称增速偏差超阈值即报', 'CRITICAL',
 '{"maxDeviationPct":0.5}', TRUE, TRUE),
('R-T02', '环比复算', 'CREDIBILITY', '本期值/上期-1 与声称增速偏差超阈值即报', 'CRITICAL',
 '{"maxDeviationPct":0.5}', TRUE, TRUE),
('R-T03', '增长率口径一致', 'CREDIBILITY', '同一指标在不同页面声称的增速一致', 'HIGH', '{}', TRUE, TRUE),
('R-T06', '单位量纲归一', 'CREDIBILITY', '亿元/万元混用检测', 'HIGH', '{}', TRUE, TRUE),
('R-R01', '异常波动', 'REASONABLENESS', '环比超阈值且无解释时提示', 'HIGH',
 '{"qoqThreshold":30,"yoyThreshold":50}', TRUE, TRUE),
('R-R03', '矛盾检测', 'REASONABLENESS', '文字结论与数据方向相反时提示', 'HIGH', '{}', TRUE, TRUE),
('R-R04', '风险信号', 'REASONABLENESS', '现金流为负、库存/应收激增等风险信号', 'HIGH',
 '{"inventoryGrowthThreshold":30,"receivableGrowthThreshold":30}', TRUE, TRUE),
('R-R05', '目标达成率', 'REASONABLENESS', '达成率低于 60% 或高于 150% 提示', 'MEDIUM',
 '{"minRate":60,"maxRate":150}', TRUE, TRUE),
('R-S02', '跨页一致性', 'CONSISTENCY', '同一指标在不同页面数值冲突时提示', 'CRITICAL', '{}', TRUE, TRUE);

INSERT INTO ai_model_config (name, provider, base_url, api_key, model_name, capabilities, temperature, timeout_seconds, enabled) VALUES
('默认通义千问文本', 'OPENAI_COMPATIBLE', 'http://127.0.0.1:8088/v1', 'sk-placeholder', 'qwen-v3-32b', 'TEXT', 0.3, 60, FALSE);
