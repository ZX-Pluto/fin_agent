package com.huawei.fin.ai.material.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.huawei.fin.ai.material.common.exception.MaterialErrorCode;
import com.huawei.fin.ai.material.common.exception.MaterialException;

public final class FileUtil {

    private FileUtil() {
    }

    public static String saveUpload(MultipartFile file, String uploadDir) {
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String ext = "";
            int idx = original.lastIndexOf('.');
            if (idx >= 0) {
                ext = original.substring(idx).toLowerCase(Locale.ROOT);
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target);
            return target.toString();
        } catch (IOException e) {
            throw new MaterialException(MaterialErrorCode.FILE_ERROR, "文件保存失败: " + e.getMessage());
        }
    }

    public static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        return idx < 0 ? "" : filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
