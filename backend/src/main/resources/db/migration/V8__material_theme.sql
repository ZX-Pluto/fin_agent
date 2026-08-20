ALTER TABLE ai_material ADD COLUMN theme_id BIGINT;

UPDATE ai_material SET theme_id = 1 WHERE theme_id IS NULL;

CREATE INDEX idx_material_theme ON ai_material(theme_id);
