package com.huawei.fin.ai.material.common.modelconfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.huawei.fin.ai.material.common.client.AiGatewayClient;
import com.huawei.fin.ai.material.common.client.LlmCallResult;
import com.huawei.fin.ai.material.common.exception.MaterialErrorCode;
import com.huawei.fin.ai.material.common.exception.MaterialException;

@Service
public class ModelConfigService implements IModelConfigService {

    private static final String MASK_PREFIX = "****";

    private final ModelConfigDao modelConfigDao;
    private final AiGatewayClient aiGatewayClient;

    public ModelConfigService(ModelConfigDao modelConfigDao, AiGatewayClient aiGatewayClient) {
        this.modelConfigDao = modelConfigDao;
        this.aiGatewayClient = aiGatewayClient;
    }

    @Override
    public List<ModelConfigVO> list() {
        List<ModelConfigVO> list = modelConfigDao.selectAll();
        list.forEach(this::mask);
        return list;
    }

    @Override
    public ModelConfigVO get(Long id) {
        ModelConfigVO vo = require(id);
        mask(vo);
        return vo;
    }

    @Override
    @Transactional
    public ModelConfigVO create(ModelConfigVO vo) {
        if (vo.getEnabled() == null) {
            vo.setEnabled(Boolean.TRUE);
        }
        if (vo.getCapabilities() == null || vo.getCapabilities().isBlank()) {
            vo.setCapabilities("TEXT");
        }
        modelConfigDao.insert(vo);
        return vo;
    }

    @Override
    @Transactional
    public ModelConfigVO update(Long id, ModelConfigVO vo) {
        ModelConfigVO old = require(id);
        vo.setId(id);
        if (isMasked(vo.getApiKey())) {
            vo.setApiKey(old.getApiKey());
        }
        modelConfigDao.update(vo);
        return vo;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        require(id);
        modelConfigDao.delete(id);
    }

    @Override
    @Transactional
    public ModelConfigVO toggle(Long id) {
        ModelConfigVO vo = require(id);
        vo.setEnabled(!Boolean.TRUE.equals(vo.getEnabled()));
        modelConfigDao.update(vo);
        return vo;
    }

    @Override
    public Map<String, Object> test(Long id) {
        ModelConfigVO vo = require(id);
        LlmCallResult result = aiGatewayClient.chat(vo,
                "你是连通性测试助手，只回复 ok",
                "请回复 ok");
        if (result.success()) {
            return Map.of(
                    "success", true,
                    "message", "连通成功（" + result.latencyMs() + "ms）: " + result.content());
        }
        return Map.of(
                "success", false,
                "message", "连通失败（" + result.latencyMs() + "ms）: " + result.message());
    }

    @Override
    public Optional<ModelConfigVO> findEnabledTextModel() {
        return Optional.ofNullable(modelConfigDao.selectEnabledByCapability("TEXT"));
    }

    @Override
    public Optional<ModelConfigVO> findEnabledVisionModel() {
        return Optional.ofNullable(modelConfigDao.selectEnabledByCapability("VISION"));
    }

    private ModelConfigVO require(Long id) {
        ModelConfigVO vo = modelConfigDao.selectById(id);
        if (vo == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "模型配置不存在: " + id);
        }
        return vo;
    }

    private void mask(ModelConfigVO vo) {
        if (vo.getApiKey() == null || vo.getApiKey().isBlank()) {
            return;
        }
        String key = vo.getApiKey();
        String tail = key.length() > 4 ? key.substring(key.length() - 4) : key;
        vo.setApiKey(MASK_PREFIX + tail);
    }

    private boolean isMasked(String apiKey) {
        return apiKey != null && apiKey.startsWith(MASK_PREFIX);
    }
}
