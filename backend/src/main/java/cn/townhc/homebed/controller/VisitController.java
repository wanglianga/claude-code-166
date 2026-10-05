package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.domain.entity.Visit;
import cn.townhc.homebed.domain.entity.VisitException;
import cn.townhc.homebed.domain.enums.DomainEnums.ExceptionStatus;
import cn.townhc.homebed.domain.enums.DomainEnums.ExceptionType;
import cn.townhc.homebed.service.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "上门巡诊执行")
@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @GetMapping
    public ApiResponse<List<Visit>> list(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) Long nurseId,
                                         @RequestParam(required = false) Long patientId) {
        return ApiResponse.ok(visitService.list(status, nurseId, patientId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(visitService.detail(id));
    }

    @Operation(summary = "护士签到开始上门")
    @PostMapping("/{id}/start")
    public ApiResponse<Visit> start(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String loc = body == null ? null : body.get("locationSnapshot");
        return ApiResponse.ok(visitService.start(id, loc));
    }

    @Operation(summary = "提交巡诊记录（体征/压疮/换药/导尿/用药/护理项/耗材/照片/家属签字）")
    @PostMapping("/{id}/complete")
    public ApiResponse<Visit> complete(@PathVariable Long id,
                                       @RequestBody VisitService.SubmitCommand cmd) {
        return ApiResponse.ok(visitService.complete(id, cmd));
    }

    @Operation(summary = "上报巡诊异常（住院/家属不在/缺货/超时/恶化等）")
    @PostMapping("/{id}/exceptions")
    public ApiResponse<VisitException> report(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(visitService.reportException(id,
                ExceptionType.valueOf(body.get("type")),
                body.get("description"),
                body.get("evidencePath")));
    }

    @GetMapping("/exceptions/list")
    public ApiResponse<List<VisitException>> exceptions(@RequestParam(required = false) String status,
                                                        @RequestParam(required = false) Long patientId) {
        return ApiResponse.ok(visitService.exceptions(status, patientId));
    }

    @Operation(summary = "处理异常（改期/改派/恢复）")
    @PostMapping("/exceptions/{exceptionId}/handle")
    public ApiResponse<VisitException> handle(@PathVariable Long exceptionId,
                                              @RequestBody Map<String, Object> body) {
        LocalDateTime newTime = body.get("newTime") == null ? null
                : LocalDateTime.parse(body.get("newTime").toString());
        Long reassignNurseId = body.get("reassignNurseId") == null ? null
                : Long.valueOf(body.get("reassignNurseId").toString());
        ExceptionStatus st = body.get("status") == null ? null
                : ExceptionStatus.valueOf(body.get("status").toString());
        return ApiResponse.ok(visitService.handleException(exceptionId,
                (String) body.getOrDefault("conclusion", ""), st, null, newTime, reassignNurseId));
    }
}
