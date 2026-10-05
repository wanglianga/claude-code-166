package com.townclinic.homebed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "patient")
public class Patient extends BaseEntity {
    @Column(nullable = false, length = 64)
    private String name;

    /** 男 / 女 */
    @Column(length = 4)
    private String gender;

    private Integer age;

    @Column(length = 18)
    private String idCard;

    @Column(length = 32)
    private String phone;

    @Column(length = 256)
    private String address;

    /** 经度/纬度（演示用，可空） */
    private Double lng;
    private Double lat;

    /** 医保类型：职工医保 / 居民医保 / 异地医保 / 无医保 */
    @Column(length = 32)
    private String insuranceType;

    @Column(length = 64)
    private String insuranceNo;

    /** 主要诊断 */
    @Column(length = 256)
    private String diagnosis;

    /** 行动能力：可搀扶 / 卧床 / 轮椅 等 */
    @Column(length = 32)
    private String mobility;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public String getInsuranceType() { return insuranceType; }
    public void setInsuranceType(String insuranceType) { this.insuranceType = insuranceType; }
    public String getInsuranceNo() { return insuranceNo; }
    public void setInsuranceNo(String insuranceNo) { this.insuranceNo = insuranceNo; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getMobility() { return mobility; }
    public void setMobility(String mobility) { this.mobility = mobility; }
}
