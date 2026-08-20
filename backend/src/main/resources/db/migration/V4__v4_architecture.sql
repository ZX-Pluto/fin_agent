CREATE TABLE ai_theme (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_model (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    theme_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    current_version BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_model_field (
    id BIGSERIAL PRIMARY KEY,
    model_id BIGINT NOT NULL,
    field_code VARCHAR(64) NOT NULL,
    field_name VARCHAR(128) NOT NULL,
    field_type VARCHAR(32) NOT NULL,
    unit VARCHAR(32),
    comment TEXT,
    seq_no INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_rule_package (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    theme_id BIGINT NOT NULL,
    package_type VARCHAR(32) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_rule_item (
    id BIGSERIAL PRIMARY KEY,
    package_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    rule_type VARCHAR(32) NOT NULL,
    scope VARCHAR(128),
    input_fields TEXT,
    execution_strategy VARCHAR(32) NOT NULL DEFAULT 'AI',
    description TEXT,
    severity VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_fact_source (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    organization VARCHAR(128),
    period VARCHAR(32),
    chapter VARCHAR(128) NOT NULL,
    slide_range TEXT,
    structured_facts TEXT,
    parse_json TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PARSED',
    version INT NOT NULL DEFAULT 1,
    error_message TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_model_data (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    organization VARCHAR(128),
    period VARCHAR(32),
    model_id BIGINT NOT NULL,
    model_version INT NOT NULL,
    fact_source_id BIGINT,
    field_code VARCHAR(64) NOT NULL,
    field_value NUMERIC(20,4),
    unit VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fact_source_material ON ai_fact_source(material_id);
CREATE INDEX idx_model_data_material ON ai_model_data(material_id);
CREATE INDEX idx_model_field_model ON ai_model_field(model_id);
CREATE INDEX idx_rule_item_package ON ai_rule_item(package_id);

INSERT INTO ai_theme (id, code, name, description) VALUES
(1, 'OPERATION', '经营情况', '代表处经营材料预审与经营质量分析');

INSERT INTO ai_model (id, code, name, theme_id, version, current_version) VALUES
(1, 'OPERATION_MODEL', '经营情况模型', 1, 1, TRUE);

INSERT INTO ai_model_field (model_id, field_code, field_name, field_type, unit, comment, seq_no) VALUES
(1, 'revenue', '收入', 'number', '亿元', '经营收入', 1),
(1, 'revenue_growth', '收入同比', 'number', '%', '收入同比增长率', 2),
(1, 'profit', '利润', 'number', '亿元', '经营利润', 3),
(1, 'profit_growth', '利润同比', 'number', '%', '利润同比增长率', 4),
(1, 'collection', '回款', 'number', '亿元', '期间回款金额', 5),
(1, 'collection_growth', '回款同比', 'number', '%', '回款同比增长率', 6),
(1, 'dso', 'DSO', 'number', '天', '应收账款周转天数', 7),
(1, 'inventory', '存货', 'number', '亿元', '期末存货', 8),
(1, 'inventory_growth', '存货同比', 'number', '%', '存货同比变化', 9),
(1, 'gross_margin', '毛利率', 'number', '%', '综合毛利率', 10),
(1, 'revenue_target', '收入目标', 'number', '亿元', '期间收入目标', 11),
(1, 'revenue_target_rate', '收入目标达成率', 'number', '%', '收入/目标', 12),
(1, 'profit_target', '利润目标', 'number', '亿元', '期间利润目标', 13),
(1, 'profit_target_rate', '利润目标达成率', 'number', '%', '利润/目标', 14);

INSERT INTO ai_rule_package (id, code, name, theme_id, package_type, description) VALUES
(1, 'PRE_AUDIT_OPERATION', '经营材料预审规则包', 1, 'PRE_AUDIT', '判断 PPT 能否作为进一步分析的可靠输入'),
(2, 'OPERATION_QUALITY', '经营质量分析规则包', 1, 'EXPERT', '模拟专家进行经营质量分析');

INSERT INTO ai_rule_item (package_id, rule_code, name, rule_type, scope, input_fields, execution_strategy, description, severity) VALUES
(1, 'PRE_AUDIT_R01', '核心指标完整性', 'PRE_AUDIT', '经营情况', 'revenue,profit,collection,dso,inventory', 'AI + Deterministic', '收入、利润、回款、DSO、存货是否在材料中存在', 'MEDIUM'),
(1, 'PRE_AUDIT_R02', '页内数据一致性', 'PRE_AUDIT', '经营情况', 'metric,value,growth,target,achievement', 'AI + Deterministic', '同一页面指标数值与同比/目标/达成率是否一致', 'HIGH'),
(1, 'PRE_AUDIT_R03', '跨页数据一致性', 'PRE_AUDIT', '经营情况', 'metric,value,slide', 'AI', '同一指标跨页数值是否一致，口径/期间/版本差异需判断', 'HIGH'),
(1, 'PRE_AUDIT_R04', '指标计算一致性', 'PRE_AUDIT', '经营情况', 'value,target,rate', 'AI + Deterministic', '同比、达成率、增长率、利润率等可推导指标是否合理', 'HIGH'),
(1, 'PRE_AUDIT_R05', '单位/口径一致性', 'PRE_AUDIT', '经营情况', 'metric,unit,period', 'AI', '亿元/万元、含税/不含税、累计/当期、同比/环比等口径检查', 'HIGH'),
(1, 'PRE_AUDIT_R06', '关键异常数据识别', 'PRE_AUDIT', '经营情况', 'revenue_growth,profit_growth,dso,inventory_growth', 'AI', '标记需要重点关注的数据，为专家分析提供入口', 'MEDIUM'),
(1, 'PRE_AUDIT_R07', '结论与数据一致性', 'PRE_AUDIT', '经营情况', 'conclusion,metric,value', 'AI', 'PPT 关键结论是否与经营数据存在潜在矛盾', 'HIGH'),
(2, 'QUALITY_R01', '收入增长质量', 'EXPERT', '经营情况', 'revenue_growth,profit_growth,gross_margin', 'AI', '收入增长与利润、毛利率、回款的匹配度', 'HIGH'),
(2, 'QUALITY_R02', '收入利润增长背离', 'EXPERT', '经营情况', 'revenue_growth,profit_growth', 'AI + Deterministic', '收入增长明显高于利润增长时关注增长质量', 'HIGH'),
(2, 'QUALITY_R03', '盈利质量', 'EXPERT', '经营情况', 'revenue_growth,profit_growth,gross_margin', 'AI', '收入增长但利润不增长/毛利率下降等信号', 'HIGH'),
(2, 'QUALITY_R04', '回款风险', 'EXPERT', '经营情况', 'collection,collection_growth,dso', 'AI', '回款增长低于收入增长、DSO 恶化等风险', 'HIGH'),
(2, 'QUALITY_R05', '库存风险', 'EXPERT', '经营情况', 'inventory,inventory_growth,revenue_growth', 'AI', '库存增长超过收入增长、收入下降库存增长等风险', 'HIGH'),
(2, 'QUALITY_R06', '目标达成风险', 'EXPERT', '经营情况', 'revenue_target_rate,profit_target_rate', 'AI + Deterministic', '收入/利润目标未达成或达成结构异常', 'HIGH'),
(2, 'QUALITY_R07', '经营趋势', 'EXPERT', '经营情况', 'period,revenue,profit', 'AI + Tool', '单代表处当前材料内趋势判断', 'MEDIUM'),
(2, 'QUALITY_R08', '风险解释充分性', 'EXPERT', '经营情况', 'risk,explanation,plan', 'AI', '风险是否有原因解释与后续计划，判断是否只需持续跟踪', 'MEDIUM');
