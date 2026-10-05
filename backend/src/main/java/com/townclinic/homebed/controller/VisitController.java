package com.townclinic.homebed.controller;

import com.townclinic.homebed.common.R;
import com.townclinic.homebed.dto.ReviewReq;
import com.townclinic.homebed.dto.VisitCompleteReq;
import com.townclinic.homebed.service.FileStorageService;
import com.townclinic.homebed.service.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "护士上门巡诊")
@RestController
@RequestMapping("/api/visits")
public class VisitController {

    private final VisitService service;
    private final FileStorageService storage;

    public VisitController(VisitService service, FileStorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @PostMapping("/start/{planId}")
    @Operation(summary = "护士到达签到，创建/续接巡诊记录")
    public R<?> start(@PathVariable Long planId) {
        return R.ok(service.start(planId));
    }

    @PostMapping("/{visitId}/complete")
    @Operation(summary = "提交巡诊记录：体征/压疮/换药/导尿/血糖血压/用药/耗材/冷链温度/家属签字")
    public R<?> complete(@PathVariable Long visitId, @Valid @RequestBody VisitCompleteReq req) {
        return R.ok(service.complete(visitId, req));
    }

    @PostMapping("/{visitId}/photos")
    @Operation(summary = "上传现场照片（换药部位/压疮/家属在场等）")
    public R<?> photo(@PathVariable Long visitId,
                      @RequestParam(required = false) String category,
                      @RequestParam(required = false) String remark,
                      @RequestParam("file") MultipartFile file) {
        return R.ok(service.uploadPhoto(visitId, category, remark, file, storage));
    }

    @PostMapping("/review")
    @Operation(summary = "医生远程复核巡诊记录")
    public R<?> review(@Valid @RequestBody ReviewReq req) {
        return R.ok(service.review(req));
    }

    @GetMapping("/pending-review")
    @Operation(summary = "待医生远程复核列表")
    public R<?> pending() {
        return R.ok(service.pendingReviews());
    }

    @GetMapping("/nurse/{nurseId}")
    @Operation(summary = "某护士的巡诊记录")
    public R<?> nurseVisits(@PathVariable Long nurseId) {
        return R.ok(service.nurseVisits(nurseId));
    }

    @GetMapping("/{visitId}")
    @Operation(summary = "巡诊记录详情（含领用、照片）")
    public R<?> detail(@PathVariable Long visitId) {
        return R.ok(service.getVisitBundle(visitId));
    }
}
