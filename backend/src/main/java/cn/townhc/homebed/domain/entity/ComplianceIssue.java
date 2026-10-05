package cn.townhc.homebed.domain.entity;

import java.util.List;

/**
 * 合规审查单条问题（非持久化，序列化为 JSON 存入 ComplianceRecord.issuesJson）。
 */
public record ComplianceIssue(
        String ruleCode,
        String severity,
        String message,
        List<String> relatedItemNos
) {
}
