package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.domain.entity.Visit;
import cn.townhc.homebed.domain.entity.VisitPlan;
import cn.townhc.homebed.service.SchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "巡诊计划与可解释排班")
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final SchedulingService schedulingService;

    @GetMapping
    public ApiResponse<List<VisitPlan>> list(@RequestParam(required = false) Long patientId) {
        return ApiResponse.ok(schedulingService.plans(patientId));
    }

    @Operation(summary = "为已批准申请生成巡诊计划：自动评估护士技能/负荷/路线并给出排班解释")
    @PostMapping("/generate")
    public ApiResponse<SchedulingService.ScheduleResult> generate(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(
                schedulingService.generatePlan(Long.valueOf(body.get("applicationId").toString())));
    }

    @Operation(summary = "单次巡诊改派护士")
    @PostMapping("/visits/{visitId}/reassign")
    public ApiResponse<Visit> reassign(@PathVariable Long visitId, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(schedulingService.reassign(visitId,
                Long.valueOf(body.get("nurseId").toString()),
                (String) body.getOrDefault("reason", "异常改派")));
    }

    @Operation(summary = "单次巡诊改期")
    @PostMapping("/visits/{visitId}/reschedule")
    public ApiResponse<Visit> reschedule(@PathVariable Long visitId, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(schedulingService.reschedule(visitId,
                LocalDateTime.parse(body.get("newTime").toString()),
                (String) body.getOrDefault("reason", "异常改期")));
    }
}
