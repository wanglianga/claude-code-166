package cn.townhc.homebed.service;

import cn.townhc.homebed.common.BusinessException;
import cn.townhc.homebed.domain.entity.Patient;
import cn.townhc.homebed.domain.entity.SysUser;
import cn.townhc.homebed.domain.enums.DomainEnums.Role;
import cn.townhc.homebed.repository.PatientRepository;
import cn.townhc.homebed.repository.SysUserRepository;
import cn.townhc.homebed.security.LoginUser;
import cn.townhc.homebed.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final SysUserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Patient> list() {
        LoginUser me = SecurityUtils.current();
        if (me.role() == Role.FAMILY) {
            return patientRepository.findByFamilyUserId(me.id());
        }
        return patientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Patient get(Long id) {
        Patient p = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "患者不存在"));
        LoginUser me = SecurityUtils.current();
        if (me.role() == Role.FAMILY && (p.getFamilyUser() == null || !p.getFamilyUser().getId().equals(me.id()))) {
            throw new BusinessException(403, "只能查看本人绑定的患者");
        }
        return p;
    }

    @Transactional
    public Patient create(Patient patient) {
        LoginUser me = SecurityUtils.current();
        if (me.role() == Role.FAMILY) {
            SysUser family = userRepository.findById(me.id()).orElseThrow();
            patient.setFamilyUser(family);
            patient.setId(null);
        } else if (patient.getFamilyUser() != null && patient.getFamilyUser().getId() != null) {
            patient.setFamilyUser(userRepository.findById(patient.getFamilyUser().getId()).orElse(null));
        }
        Patient saved = patientRepository.save(patient);
        auditService.log(me, "患者", "新建患者档案", saved.getId(), null, saved.getId(),
                "家属/工作人员建立患者档案：" + saved.getName());
        return saved;
    }

    @Transactional
    public Patient update(Long id, Patient input) {
        Patient p = get(id);
        p.setName(input.getName());
        p.setGender(input.getGender());
        p.setAge(input.getAge());
        p.setPhone(input.getPhone());
        p.setIdCard(input.getIdCard());
        p.setAddress(input.getAddress());
        p.setLongitude(input.getLongitude());
        p.setLatitude(input.getLatitude());
        p.setInsuranceType(input.getInsuranceType());
        p.setInsuranceNo(input.getInsuranceNo());
        p.setMobilityLevel(input.getMobilityLevel());
        p.setDiseaseCategory(input.getDiseaseCategory());
        p.setPrimaryCaregiver(input.getPrimaryCaregiver());
        p.setPrimaryContactPhone(input.getPrimaryContactPhone());
        p.setEmergencyContact(input.getEmergencyContact());
        p.setEmergencyPhone(input.getEmergencyPhone());
        return patientRepository.save(p);
    }
}
