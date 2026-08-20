package com.huawei.fin.ai.material.common.client;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.huawei.fin.ai.material.common.util.FileUtil;

@Component
public class FileServiceClient {

    private final String uploadDir;

    public FileServiceClient(@Value("${app.upload-dir:./data/uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String save(MultipartFile file) {
        return FileUtil.saveUpload(file, uploadDir);
    }

    public Path resolve(String filePath) {
        return Paths.get(filePath);
    }
}
