package com.huawei.fin.ai.material.analysis.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ChapterVO {
    private String name;
    private List<Integer> slideNos = new ArrayList<>();

    public ChapterVO(String name) {
        this.name = name;
    }
}
