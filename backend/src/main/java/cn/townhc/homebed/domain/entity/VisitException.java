package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.ExceptionStatus;
import cn.townhc.homebed.domain.enums.DomainEnums.ExceptionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "visit_exception")
public class VisitException extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExceptionType type;

    @Column(length = 1024)
    private String description;

    @Column(length = 256)
    private String evidencePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExceptionStatus status = ExceptionStatus.REPORTED;

    /** 管理员/医生处理结论：改期、改派、暂停等 */
    @Column(length = 1024)
    private String handleConclusion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handler_id")
    private SysUser handler;

    private LocalDateTime handledAt;
}
