UPDATE ai_model_config
SET timeout_seconds = 300
WHERE timeout_seconds IS NULL OR timeout_seconds < 120;
