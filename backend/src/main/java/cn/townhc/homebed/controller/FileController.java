package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "文件上传（照片/签字/证据）")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传现场照片/签字图片，返回可访问路径")
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(defaultValue = "visit") String type) {
        String path = fileStorageService.store(file, type);
        return ApiResponse.ok(Map.of("path", path, "url", path));
    }
}
