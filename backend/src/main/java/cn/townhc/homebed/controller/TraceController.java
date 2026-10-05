package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.domain.entity.AuditLog;
import cn.townhc.homebed.service.TraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "服务全链还原与审计")
@RestController
@RequestMapping("/api/trace")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    @Operation(summary = "按建床申请还原：申请/评估/排班/每次上门/照片/医嘱/签字/账单/合规/风险 全链时间线")
    @GetMapping("/application/{applicationId}")
    public ApiResponse<Map<String, Object>> byApplication(@PathVariable Long applicationId) {
        return ApiResponse.ok(traceService.byApplication(applicationId));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<AuditLog>> auditLogs(@RequestParam(required = false) String module) {
        return ApiResponse.ok(traceService.auditLogs(module));
    }
}
