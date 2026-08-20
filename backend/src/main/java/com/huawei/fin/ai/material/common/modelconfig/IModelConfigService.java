package com.huawei.fin.ai.material.common.modelconfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IModelConfigService {

    List<ModelConfigVO> list();

    ModelConfigVO get(Long id);

    ModelConfigVO create(ModelConfigVO vo);

    ModelConfigVO update(Long id, ModelConfigVO vo);

    void delete(Long id);

    ModelConfigVO toggle(Long id);

    Map<String, Object> test(Long id);

    Optional<ModelConfigVO> findEnabledTextModel();

    Optional<ModelConfigVO> findEnabledVisionModel();
}
