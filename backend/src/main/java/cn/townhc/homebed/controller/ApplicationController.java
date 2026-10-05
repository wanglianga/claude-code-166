package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.domain.entity.BedApplication;
import cn.townhc.homebed.domain.entity.CareNeed;
import cn.townhc.homebed.domain.enums.DomainEnums.CareItemType;
import cn.townhc.homebed.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "家庭病床申请与评估")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ApiResponse<List<BedApplication>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(applicationService.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<BedApplication> get(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.get(id));
    }

    @GetMapping("/{id}/care-needs")
    public ApiResponse<List<CareNeed>> careNeeds(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.careNeeds(id));
    }

    @Operation(summary = "家属提交家庭病床申请")
    @PostMapping
    public ApiResponse<BedApplication> submit(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> rawItems = (List<String>) body.get("careItems");
        List<CareItemType> items = rawItems == null ? List.of()
                : rawItems.stream().map(CareItemType::valueOf).toList();
        return ApiResponse.ok(applicationService.submit(
                Long.valueOf(body.get("patientId").toString()),
                (String) body.get("reason"),
                (String) body.get("expectedItems"),
                items,
                (String) body.get("attachmentsJson")));
    }

    @Operation(summary = "家庭医生评估（病情/距离/照护人/医保资格/护理项目/风险等级）")
    @PostMapping("/{id}/assess")
    public ApiResponse<BedApplication> assess(@PathVariable Long id,
                                              @RequestBody ApplicationService.AssessmentCommand cmd) {
        return ApiResponse.ok(applicationService.assess(id, cmd));
    }

    @Operation(summary = "家属撤销申请")
    @PostMapping("/{id}/cancel")
    public ApiResponse<BedApplication> cancel(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.cancel(id));
    }
}
