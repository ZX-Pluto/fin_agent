ALTER TABLE ai_material ADD COLUMN region VARCHAR(128);

UPDATE ai_material SET region = '默认地区部' WHERE region IS NULL;
