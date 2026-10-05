package cn.townhc.homebed.service;

import cn.townhc.homebed.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf");

    @Value("${app.upload.dir}")
    private String uploadDir;

    /** 保存上传文件，返回可访问相对路径（/uploads/...） */
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) throw new BusinessException("上传文件为空");
        String original = file.getOriginalFilename() == null ? "f.bin" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED.contains(ext)) {
            throw new BusinessException("仅支持图片/pdf：" + original);
        }
        try {
            LocalDate today = LocalDate.now();
            Path dir = Paths.get(uploadDir, subDir,
                    String.valueOf(today.getYear()), String.valueOf(today.getMonthValue()));
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            return "/uploads/" + subDir + "/" + today.getYear() + "/" + today.getMonthValue()
                    + "/" + filename;
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage());
        }
    }
}
