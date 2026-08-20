package com.huawei.fin.ai.material.material.vo;

import java.util.List;

import lombok.Data;

@Data
public class MaterialElementVO {

    private String type;
    private String text;
    private List<List<String>> rows;
}
