package com.townclinic.homebed.entity;

import jakarta.persistence.*;

/** 巡诊照片证据：换药部位、压疮、导尿、家属在场等 */
@Entity
@Table(name = "visit_photo")
public class VisitPhoto extends BaseEntity {
    @Column(nullable = false)
    private Long visitId;

    /** 文件存储相对路径（/uploads/ 下），也可能为 dataURL */
    @Column(length = 512, nullable = false)
    private String url;

    @Column(length = 128)
    private String category;

    @Column(length = 256)
    private String remark;

    private Long uploaderId;

    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }
}
