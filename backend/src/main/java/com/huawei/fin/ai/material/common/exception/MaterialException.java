package com.huawei.fin.ai.material.common.exception;

import lombok.Getter;

@Getter
public class MaterialException extends RuntimeException {

    private final String code;

    public MaterialException(String code, String message) {
        super(message);
        this.code = code;
    }
}
