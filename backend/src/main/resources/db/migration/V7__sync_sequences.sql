SELECT setval(pg_get_serial_sequence('ai_theme', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM ai_theme), 1));
SELECT setval(pg_get_serial_sequence('ai_model', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM ai_model), 1));
SELECT setval(pg_get_serial_sequence('ai_model_field', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM ai_model_field), 1));
SELECT setval(pg_get_serial_sequence('ai_rule_package', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM ai_rule_package), 1));
SELECT setval(pg_get_serial_sequence('ai_rule_item', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM ai_rule_item), 1));
