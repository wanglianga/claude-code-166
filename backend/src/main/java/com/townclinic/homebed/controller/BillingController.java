package com.townclinic.homebed.controller;

import com.townclinic.homebed.common.R;
import com.townclinic.homebed.dto.BillGenerateReq;
import com.townclinic.homebed.dto.ConfirmBillReq;
import com.townclinic.homebed.dto.QuestionReplyReq;
import com.townclinic.homebed.dto.QuestionReq;
import com.townclinic.homebed.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "统一账单与医保结算")
@RestController
@RequestMapping("/api/bills")
public class BillingController {

    private final BillingService service;

    public BillingController(BillingService service) { this.service = service; }

    @PostMapping("/generate")
    @Operation(summary = "按周期生成统一账单（耗材/冷链/护理/医保报销/自费）并做合规校验")
    public R<?> generate(@Valid @RequestBody BillGenerateReq req) {
        return R.ok(service.generate(req));
    }

    @GetMapping("/{id}")
    @Operation(summary = "账单详情（明细、合规标记、质疑）")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(service.billDetail(id));
    }

    @GetMapping("/mine")
    @Operation(summary = "家属查看我的账单")
    public R<?> mine() { return R.ok(service.myBills()); }

    @GetMapping
    @Operation(summary = "账单列表（管理/医保，可按状态过滤）")
    public R<?> list(@RequestParam(required = false) String status) {
        return R.ok(service.allBills(status));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "家属逐项确认并确认账单")
    public R<?> confirm(@PathVariable Long id, @RequestBody(required = false) ConfirmBillReq req) {
        return R.ok(service.confirm(id, req == null ? new ConfirmBillReq(null, null) : req));
    }

    @PostMapping("/{id}/settle")
    @Operation(summary = "医保经办结算（家属确认后）")
    public R<?> settle(@PathVariable Long id) {
        return R.ok(service.settle(id));
    }

    @PostMapping("/questions")
    @Operation(summary = "家属提出收费质疑")
    public R<?> ask(@Valid @RequestBody QuestionReq req) {
        return R.ok(service.ask(req));
    }

    @PostMapping("/questions/reply")
    @Operation(summary = "卫生院/医保答复质疑（可退费）")
    public R<?> reply(@Valid @RequestBody QuestionReplyReq req) {
        return R.ok(service.reply(req));
    }

    @GetMapping("/questions/all")
    @Operation(summary = "全部收费质疑")
    public R<?> questions() { return R.ok(service.allQuestions()); }

    @GetMapping("/compliance/list")
    @Operation(summary = "医保合规复核清单（重复收费/超频次/目录变化/目录外）")
    public R<?> compliance() { return R.ok(service.complianceList()); }
}
