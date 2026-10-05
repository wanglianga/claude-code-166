package com.townclinic.homebed.entity;

import com.townclinic.homebed.common.DomainEnums.QuestionStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 家属对收费/服务的质疑及卫生院答复（医保经办可介入） */
@Entity
@Table(name = "charge_question")
public class ChargeQuestion extends BaseEntity {
    @Column(nullable = false)
    private Long applicationId;

    private Long billId;

    /** 可指向具体账单明细 */
    private Long billItemId;

    @Column(nullable = false)
    private Long familyUserId;

    @Column(nullable = false, length = 1024)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuestionStatus status = QuestionStatus.OPEN;

    private Long replyUserId;

    @Column(length = 1024)
    private String reply;

    private LocalDateTime repliedAt;

    /** 若涉及退费，记录金额 */
    private java.math.BigDecimal refundAmount = java.math.BigDecimal.ZERO;

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getBillItemId() { return billItemId; }
    public void setBillItemId(Long billItemId) { this.billItemId = billItemId; }
    public Long getFamilyUserId() { return familyUserId; }
    public void setFamilyUserId(Long familyUserId) { this.familyUserId = familyUserId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public QuestionStatus getStatus() { return status; }
    public void setStatus(QuestionStatus status) { this.status = status; }
    public Long getReplyUserId() { return replyUserId; }
    public void setReplyUserId(Long replyUserId) { this.replyUserId = replyUserId; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public LocalDateTime getRepliedAt() { return repliedAt; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }
    public java.math.BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(java.math.BigDecimal refundAmount) { this.refundAmount = refundAmount; }
}
