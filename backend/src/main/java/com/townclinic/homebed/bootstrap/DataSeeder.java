package com.townclinic.homebed.bootstrap;

import com.townclinic.homebed.common.DomainEnums.*;
import com.townclinic.homebed.dto.*;
import com.townclinic.homebed.entity.*;
import com.townclinic.homebed.repo.*;
import com.townclinic.homebed.security.CurrentUser;
import com.townclinic.homebed.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 演示数据初始化：五角色账号、患者、目录，以及一条覆盖
 * 申请-评估-建床-排班-上门-签字-复核-医嘱-账单-质疑-退费-结算-风险事件 的完整业务链。
 * 重复启动幂等（已存在账号则跳过）。
 */
@Component
@Order(10)
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository userRepo;
    private final BedApplicationRepository appRepo;
    private final CatalogItemRepository catalogRepo;
    private final VisitPlanRepository planRepo;
    private final VisitRepository visitRepo;
    private final VisitItemRepository visitItemRepo;
    private final VisitPhotoRepository photoRepo;
    private final BillRepository billRepo;
    private final BillItemRepository billItemRepo;
    private final RiskEventRepository riskRepo;
    private final ApplicationService appService;
    private final SchedulingService schedulingService;
    private final VisitService visitService;
    private final CatalogService catalogService;
    private final BillingService billingService;
    private final DoctorOrderService orderService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public DataSeeder(AppUserRepository userRepo, BedApplicationRepository appRepo,
                      CatalogItemRepository catalogRepo, VisitPlanRepository planRepo,
                      VisitRepository visitRepo, VisitItemRepository visitItemRepo,
                      VisitPhotoRepository photoRepo, BillRepository billRepo,
                      BillItemRepository billItemRepo, RiskEventRepository riskRepo,
                      ApplicationService appService, SchedulingService schedulingService,
                      VisitService visitService, CatalogService catalogService,
                      BillingService billingService, DoctorOrderService orderService) {
        this.userRepo = userRepo;
        this.appRepo = appRepo;
        this.catalogRepo = catalogRepo;
        this.planRepo = planRepo;
        this.visitRepo = visitRepo;
        this.visitItemRepo = visitItemRepo;
        this.photoRepo = photoRepo;
        this.billRepo = billRepo;
        this.billItemRepo = billItemRepo;
        this.riskRepo = riskRepo;
        this.appService = appService;
        this.schedulingService = schedulingService;
        this.visitService = visitService;
        this.catalogService = catalogService;
        this.billingService = billingService;
        this.orderService = orderService;
    }

    private static final String SIG =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M8AAAMBAQDJ/pLvAAAAAElFTkSuQmCC";

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.findByUsername("family").isPresent()) return;

        AppUser family = user("family", "123456", "王梅（家属）", Role.FAMILY, "患者家属", "13800000001", 0);
        AppUser nurse1 = user("nurse1", "123456", "李护英", Role.NURSE, "主管护师", "13900000002", 420);
        AppUser nurse2 = user("nurse2", "123456", "赵守兰", Role.NURSE, "护士", "13900000003", 360);
        AppUser doctor = user("doctor", "123456", "陈济民", Role.DOCTOR, "家庭医生/主治医师", "13700000004", 480);
        AppUser admin = user("admin", "123456", "周院长", Role.ADMIN, "卫生院院长", "13600000005", 480);
        AppUser insurance = user("yibao", "123456", "张瑾（医保办）", Role.INSURANCE, "医保经办", "13500000006", 480);

        seedCatalog();

        // ===== 患者 =====
        Patient p1 = as(family, () -> appService.createPatient(new PatientReq(
                "张桂英", "女", 78, "3604********1224", "13800000001",
                "青山村2组3号（距卫生院5.2km）", 116.01, 29.68,
                "城乡居民医保", "JM360420260001", "脑梗死后遗症康复期、骶尾部压疮(II期)", "卧床")));
        Patient p2 = as(family, () -> appService.createPatient(new PatientReq(
                "李德福", "男", 72, "3604********5511", "13800000007",
                "柳湾村6组（距卫生院9.6km）", 116.08, 29.72,
                "城镇职工医保", "ZG360420260002", "2型糖尿病、高血压3级、留置尿管", "轮椅")));
        Patient p3 = as(family, () -> appService.createPatient(new PatientReq(
                "孙玉珍", "女", 84, "3604********8830", "13800000008",
                "石岗村1组（距卫生院4.1km）", 115.97, 29.66,
                "城乡居民医保", "JM360420260003", "股骨颈骨折术后、长期卧床", "卧床")));
        Patient p4 = as(family, () -> appService.createPatient(new PatientReq(
                "周树林", "男", 69, "3604********3342", "13800000009",
                "青山村7组（距卫生院6.8km）", 116.03, 29.71,
                "城乡居民医保", "JM360420260004", "慢性阻塞性肺疾病、行动不便", "可搀扶")));
        Patient p5 = as(family, () -> appService.createPatient(new PatientReq(
                "吴桂兰", "女", 81, "3604********7765", "13800000010",
                "大岭村（距卫生院15.3km）", 116.15, 29.88,
                "异地医保（待核实）", "YD-GD-20260088", "阿尔茨海默病、长期卧床", "卧床")));

        LocalDate today = LocalDate.now();

        // ===== 申请 -> 评估 -> 建床 =====
        Long a1 = as(family, () -> appService.submit(new SubmitApplicationReq(
                p1.getId(), "王梅", "女儿", "13800000001",
                "母亲脑梗后卧床，骶尾部出现压疮，需要护士定期上门换药和康复护理，家属白天可照护。",
                List.of("S001", "S002", "S003", "S005"), "每周3次")).getId());
        as(doctor, () -> appService.assess(new AssessReq(
                a1, 5.2, true, RiskLevel.HIGH,
                List.of("S001", "S002", "S003", "S005", "S006"),
                "每周一、三、五", "患者病情重、压疮II期，在服务半径内，家属照护条件具备，医保资格有效，建议建床并重点跟踪压疮与体温。",
                true, null)));
        as(admin, () -> appService.approve(a1));

        Long a2 = as(family, () -> appService.submit(new SubmitApplicationReq(
                p2.getId(), "李强", "儿子", "13800000007",
                "父亲糖尿病合并留置尿管，行动不便需轮椅，希望护士上门导尿护理、测血糖并指导胰岛素注射。",
                List.of("S001", "S004", "S005"), "每周2次")).getId());
        as(doctor, () -> appService.assess(new AssessReq(
                a2, 9.6, true, RiskLevel.MEDIUM,
                List.of("S001", "S004", "S005", "S006"),
                "每周二、四（上午）", "距卫生院9.6km略超8km标准半径，患者病情稳定、儿子可配合，安排在上午顺路集中上门并控制在护士负荷内。",
                true, null)));
        as(admin, () -> appService.approve(a2));

        Long a3 = as(family, () -> appService.submit(new SubmitApplicationReq(
                p3.getId(), "孙丽", "女儿", "13800000008",
                "老人骨折术后卧床，需要导尿护理和压疮预防。",
                List.of("S001", "S004"), "每周2次")).getId());
        as(doctor, () -> appService.assess(new AssessReq(
                a3, 4.1, true, RiskLevel.MEDIUM,
                List.of("S001", "S004", "S002"), "每周二、五",
                "患者临时住院前已完成建床评估。", true, null)));
        as(admin, () -> appService.approve(a3));
        // 本周计划（随后暂停，计划挂起）
        as(doctor, () -> schedulingService.generate(new GeneratePlanReq(
                a3, today, today.plusDays(4), List.of(1, 2, 3, 4, 5), List.of("S001", "S004"))));
        as(doctor, () -> appService.pause(a3, "患者临时住院（县医院骨科），预计住院2周，住院期间暂停上门，住院期间不计床位护理费"));

        Long a4 = as(family, () -> appService.submit(new SubmitApplicationReq(
                p4.getId(), "周芳", "女儿", "13800000009",
                "慢阻肺活动后气喘，希望建床定期随访、用药指导。",
                List.of("S001", "S007"), "每周1次")).getId());

        Long a5 = as(family, () -> appService.submit(new SubmitApplicationReq(
                p5.getId(), "吴军", "儿子", "13800000010",
                "老人卧床，家住大岭村，希望上门照护。",
                List.of("S001"), "每周1次")).getId());
        as(doctor, () -> appService.assess(new AssessReq(
                a5, 15.3, false, RiskLevel.MEDIUM,
                List.of(), "—",
                "居住距离15.3km明显超出门诊统筹家庭病床8km服务半径，且异地医保资格尚未核实，暂不具备建床条件，建议先核实医保或转就近乡镇卫生院。",
                false, "超服务半径且医保资格待核实")));

        // ===== 排班 =====
        as(doctor, () -> schedulingService.generate(new GeneratePlanReq(
                a1, today, today.plusDays(4), List.of(1, 3, 5), null)));
        as(doctor, () -> schedulingService.generate(new GeneratePlanReq(
                a2, today, today.plusDays(4), List.of(1, 4), null)));

        // 异常场景：家属不在家（取消A1周五计划）、耗材缺货（取消A2周四计划）
        List<VisitPlan> a1Plans = planRepo.findByApplicationIdOrderByPlanDateAsc(a1);
        List<VisitPlan> a2Plans = planRepo.findByApplicationIdOrderByPlanDateAsc(a2);
        as(nurse1, () -> schedulingService.cancelPlan(
                a1Plans.stream().filter(p -> p.getPlanDate().isAfter(today)).reduce((x, y) -> y).orElseThrow().getId(),
                new PlanCancelReq(VisitAbortType.FAMILY_ABSENT, "家属来电临时无人在家，协商改到下周上门")));
        as(nurse2, () -> schedulingService.cancelPlan(
                a2Plans.stream().filter(p -> p.getPlanDate().isAfter(today)).findFirst().orElseThrow().getId(),
                new PlanCancelReq(VisitAbortType.STOCK_OUT, "患者需用的鼻饲固定贴当前库存为0，耗材补货后改约")));

        // ===== 今日两次上门执行 =====
        VisitPlan plan1 = a1Plans.stream().filter(p -> p.getPlanDate().equals(today)
                && p.getStatus() == PlanStatus.ACTIVE).findFirst().orElseThrow();
        VisitPlan plan2 = a2Plans.stream().filter(p -> p.getPlanDate().equals(today)
                && p.getStatus() == PlanStatus.ACTIVE).findFirst().orElseThrow();

        Visit v1Start = as(userRepo.findById(plan1.getNurseId()).orElseThrow(),
                () -> visitService.start(plan1.getId()));
        // 回溯签到时间以体现上门耗时
        v1Start.setStartedAt(LocalDateTime.now().minusMinutes(160));
        visitRepo.save(v1Start);
        final Long v1Id = v1Start.getId();
        Visit v1 = as(userRepo.findById(plan1.getNurseId()).orElseThrow(),
                () -> visitService.complete(v1Id, new VisitCompleteReq(
                        37.9, 148, 92, 94, 8.6, 95,
                        "骶尾部II期压疮约3x4cm，少量黄色渗液，周围皮肤红肿",
                        "骶尾部2处创面换药，渗液较前增多",
                        null,
                        "已提醒家属：重组人胰岛素餐前15分钟皮下注射，注意轮换注射部位",
                        "压疮换药后敷料固定完好；嘱家属每2小时翻身一次、保持皮肤干燥；体温偏高需密切观察。",
                        "压疮渗液较前增多、体温37.9℃，不排除局部感染，建议医生评估是否加用抗感染治疗，必要时转诊。",
                        VisitAbortType.NONE, null, 6.2,
                        SIG, "王梅",
                        List.of(item("S001", "1"), item("S002", "1"), item("S003", "2"),
                                item("S005", "3"), item("C001", "1"), item("DC01", "1")))));
        writeSeedPhoto(v1.getId(), "压疮/换药", "骶尾部创面换药后（示例照片）");

        Visit v2Start = as(userRepo.findById(plan2.getNurseId()).orElseThrow(),
                () -> visitService.start(plan2.getId()));
        v2Start.setStartedAt(LocalDateTime.now().minusMinutes(55));
        visitRepo.save(v2Start);
        final Long v2Id = v2Start.getId();
        Visit v2 = as(userRepo.findById(plan2.getNurseId()).orElseThrow(),
                () -> visitService.complete(v2Id, new VisitCompleteReq(
                        36.6, 156, 98, 82, 13.2, 97,
                        null, null,
                        "留置尿管通畅，尿液清亮，尿道口无红肿，会阴护理完成",
                        "指导儿子胰岛素注射与血糖记录方法",
                        "患者一般情况可，下肢轻度水肿；导尿护理规范，敷贴固定良好。",
                        "空腹血糖13.2mmol/L，较上次8.9明显升高，建议医生调整降糖方案。",
                        VisitAbortType.NONE, null, 9.5,
                        SIG, "李强",
                        List.of(item("S001", "1"), item("S004", "1"), item("S005", "1"),
                                item("C002", "1"), item("C010", "1"), item("DC01", "1")))));
        writeSeedPhoto(v2.getId(), "导尿护理", "留置尿管护理与交接（示例照片）");

        // 额外登记一条压疮风险事件
        RiskEvent ulcer = new RiskEvent();
        ulcer.setApplicationId(a1);
        ulcer.setVisitId(v1.getId());
        ulcer.setPatientId(p1.getId());
        ulcer.setEventType(RiskEventType.PRESSURE_ULCER);
        ulcer.setDescription("骶尾部压疮II期伴渗液增多，已加密巡视频次并要求家属落实翻身记录");
        ulcer.setReporterId(nurse1.getId());
        ulcer.setStatus(EventStatus.HANDLED);
        ulcer.setHandlerId(doctor.getId());
        ulcer.setHandleNote("已加开换药医嘱，明日加访一次，若发热不退转县人民医院外科");
        ulcer.setHandledAt(LocalDateTime.now());
        riskRepo.save(ulcer);

        // ===== 医生远程复核 + 医嘱 =====
        as(doctor, () -> visitService.review(new ReviewReq(v1.getId(),
                "已查看生命体征与换药照片，压疮处理规范；体温偏高，明日加访复查，必要时转诊。", false)));
        as(doctor, () -> orderService.create(new OrderReq(
                v1.getId(), OrderType.REFERRAL,
                "若明日体温≥38℃或创面渗液未减少，转县人民医院外科/伤口门诊评估",
                today.plusDays(2), "县人民医院")));
        as(doctor, () -> orderService.create(new OrderReq(
                v1.getId(), OrderType.REVISIT,
                "明日上午加访一次，复查体温、压疮创面并换药",
                today.plusDays(1), null)));
        as(doctor, () -> orderService.create(new OrderReq(
                v2.getId(), OrderType.PRESCRIPTION_ADJUST,
                "空腹血糖控制不佳，胰岛素调整为早12U、晚10U餐前皮下注射，加测睡前血糖，三天后复诊评估；注意低血糖。",
                today.plusDays(3), null)));
        // v2 保留 PENDING 远程复核，供医生工作台演示

        // ===== 医保目录变化（发生在服务之后、结算之前） =====
        CatalogItem s001 = catalogRepo.findByEnabledTrue().stream()
                .filter(c -> c.getCode().equals("S001")).findFirst().orElseThrow();
        as(insurance, () -> catalogService.update(s001.getId(), new CatalogItemReq(
                s001.getCode(), s001.getName(), s001.getSpec(), s001.getCategory(),
                new BigDecimal("55.00"), s001.getUnit(), true, 85, false,
                s001.getStockQty(), s001.getSafetyStock(), s001.getDurationMin(),
                s001.getFrequencyLimit())));

        // ===== 账单 1：质疑 -> 退费 -> 确认 -> 结算 =====
        Map<String, Object> bill1 = as(admin,
                () -> billingService.generate(new BillGenerateReq(a1, today, today)));
        Long bill1Id = ((Bill) bill1.get("bill")).getId();
        Long s003Item = ((List<BillItem>) bill1.get("items")).stream()
                .filter(i -> i.getItemCode().equals("S003")).findFirst().orElseThrow().getId();
        as(family, () -> billingService.ask(new QuestionReq(bill1Id, s003Item, null,
                "10月5日换药为什么按2次收费？护士只上门了一次。")));
        as(insurance, () -> billingService.reply(new QuestionReplyReq(
                questionLastId(bill1Id),
                "已调取当日巡诊记录与换药照片核对：患者当日有2处创面，但按医保收费标准创面换药限1次/诊次，多收1次30元（医保支付24元、个人自付6元），自付部分6元予以退回，已提醒护士规范计费。",
                true, new BigDecimal("6.00"))));
        List<Long> ackIds = billItemRepo.findByBillId(bill1Id).stream().map(BillItem::getId).toList();
        as(family, () -> billingService.confirm(bill1Id, new ConfirmBillReq("已与照片和护理记录核对，无误", ackIds)));
        as(insurance, () -> billingService.settle(bill1Id));

        // ===== 账单 2：家属对自费项目质疑，等待处理 =====
        Map<String, Object> bill2 = as(admin,
                () -> billingService.generate(new BillGenerateReq(a2, today, today)));
        Long bill2Id = ((Bill) bill2.get("bill")).getId();
        Long c010Item = ((List<BillItem>) bill2.get("items")).stream()
                .filter(i -> i.getItemCode().equals("C010")).findFirst().orElseThrow().getId();
        as(family, () -> billingService.ask(new QuestionReq(bill2Id, c010Item, null,
                "自费的防水敷贴15元用之前没有告诉我们价格，请说明是否必须使用。")));
    }

    private void seedCatalog() {
        // 护理服务
        item0("S001", "家庭病床巡诊费", "次", ItemCategory.SERVICE, "50.00", true, 80, 30, null, 0, 0);
        item0("S002", "压疮护理", "次", ItemCategory.SERVICE, "25.00", true, 80, 25, "1/日", 0, 0);
        item0("S003", "创面换药", "次", ItemCategory.SERVICE, "30.00", true, 80, 20, "1/次", 0, 0);
        item0("S004", "导尿护理", "次", ItemCategory.SERVICE, "28.00", true, 70, 25, "1/日", 0, 0);
        item0("S005", "血糖监测", "次", ItemCategory.SERVICE, "8.00", true, 90, 10, "2/日", 0, 0);
        item0("S006", "血压监测", "次", ItemCategory.SERVICE, "5.00", true, 90, 5, null, 0, 0);
        item0("S007", "用药指导与健康宣教", "次", ItemCategory.SERVICE, "10.00", true, 80, 15, null, 0, 0);
        // 耗材
        item0("C001", "一次性换药包", "个", ItemCategory.CONSUMABLE, "12.00", true, 80, 5, null, 60, 15);
        item0("C002", "无菌导尿包", "个", ItemCategory.CONSUMABLE, "18.00", true, 70, 5, null, 40, 10);
        item0("C003", "一次性注射器(5ml)", "支", ItemCategory.CONSUMABLE, "1.50", true, 90, 5, null, 5, 20);
        item0("C004", "无菌纱布包", "包", ItemCategory.CONSUMABLE, "6.00", true, 90, 5, null, 80, 20);
        item0("C005", "血糖试纸", "片", ItemCategory.CONSUMABLE, "2.50", true, 90, 5, null, 120, 30);
        item0("C006", "鼻饲固定贴", "片", ItemCategory.CONSUMABLE, "4.00", true, 80, 5, null, 0, 20);
        item0("C010", "防水透气敷贴(自费)", "片", ItemCategory.CONSUMABLE, "15.00", false, 0, 5, null, 30, 10);
        // 药品
        item0("D001", "苯磺酸氨氯地平片", "盒", ItemCategory.DRUG, "12.60", true, 80, 0, null, 100, 20);
        item0("D002", "二甲双胍缓释片", "盒", ItemCategory.DRUG, "22.00", true, 80, 0, null, 60, 15);
        item0("DC01", "重组人胰岛素注射液(冷链)", "支", ItemCategory.DRUG_COLD, "58.50", true, 70, 0, null, 20, 5);
        item0("DC02", "低分子肝素钙注射液(冷链)", "支", ItemCategory.DRUG_COLD, "46.00", true, 70, 0, null, 3, 5);
    }

    private void item0(String code, String name, String unit, ItemCategory cat, String price,
                       boolean covered, int ratio, int dur, String freq, int stock, int safety) {
        CatalogItem c = new CatalogItem();
        c.setCode(code);
        c.setName(name);
        c.setUnit(unit);
        c.setCategory(cat);
        c.setPrice(new BigDecimal(price));
        c.setInsuranceCovered(covered);
        c.setReimburseRatio(ratio);
        c.setDurationMin(dur);
        c.setFrequencyLimit(freq);
        c.setStockQty(stock);
        c.setSafetyStock(safety);
        c.setColdChain(cat == ItemCategory.DRUG_COLD);
        c.setEnabled(true);
        catalogRepo.save(c);
    }

    private AppUser user(String username, String pwd, String name, Role role, String title, String phone, int cap) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPassword(pwd);
        u.setName(name);
        u.setRole(role);
        u.setTitle(title);
        u.setPhone(phone);
        u.setEnabled(true);
        u.setDailyCapacityMin(cap == 0 ? 480 : cap);
        return userRepo.save(u);
    }

    private VisitCompleteReq.ItemUseReq item(String code, String qty) {
        Long id = catalogRepo.findByEnabledTrue().stream()
                .filter(c -> c.getCode().equals(code)).findFirst().orElseThrow().getId();
        return new VisitCompleteReq.ItemUseReq(id, new BigDecimal(qty));
    }

    private Long questionLastId(Long billId) {
        return billingService.allQuestions().stream()
                .filter(q -> billId.equals(q.getBillId())).findFirst().orElseThrow().getId();
    }

    private void writeSeedPhoto(Long visitId, String category, String text) {
        try {
            Path dir = Path.of(uploadDir);
            Files.createDirectories(dir);
            BufferedImage img = new BufferedImage(360, 240, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(235, 242, 250));
            g.fillRect(0, 0, 360, 240);
            g.setColor(new Color(60, 96, 130));
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString("Home visit evidence", 24, 70);
            g.setColor(new Color(120, 60, 60));
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString(category, 24, 110);
            g.drawString(LocalDate.now().toString(), 24, 140);
            g.setColor(new Color(200, 120, 90));
            g.fillOval(230, 90, 90, 90);
            g.dispose();
            String fileName = "seed-" + visitId + "-" + category.hashCode() + ".png";
            File f = dir.resolve(fileName).toFile();
            ImageIO.write(img, "png", f);
            VisitPhoto p = new VisitPhoto();
            p.setVisitId(visitId);
            p.setUrl("/uploads/" + fileName);
            p.setCategory(category);
            p.setRemark(text);
            p.setUploaderId(0L);
            photoRepo.save(p);
        } catch (Exception ignored) {
            // 示例照片失败不影响启动
        }
    }

    // ===== 以指定用户身份执行（初始化线程无登录上下文） =====
    private <T> T as(AppUser u, java.util.function.Supplier<T> s) {
        CurrentUser.set(u);
        try { return s.get(); } finally { CurrentUser.clear(); }
    }
}
