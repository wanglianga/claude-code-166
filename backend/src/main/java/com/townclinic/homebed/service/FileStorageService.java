package com.townclinic.homebed.service;

import com.townclinic.homebed.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + root, e);
        }
    }

    /** 保存图片，返回可访问相对路径 /uploads/xxx.jpg */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiException("上传文件为空");
        String original = file.getOriginalFilename() == null ? "img" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot);
        Set<String> allow = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
        if (!ext.isEmpty() && !allow.contains(ext.toLowerCase())) {
            throw new ApiException("仅支持图片文件 jpg/png/webp/gif");
        }
        String name = UUID.randomUUID().toString().replace("-", "") + ext.toLowerCase();
        try {
            Path target = root.resolve(name);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new ApiException("文件保存失败：" + e.getMessage());
        }
        return "/uploads/" + name;
    }
}
