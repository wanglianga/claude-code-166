package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.domain.entity.Patient;
import cn.townhc.homebed.service.PatientService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "患者档案")
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    public ApiResponse<List<Patient>> list() {
        return ApiResponse.ok(patientService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<Patient> get(@PathVariable Long id) {
        return ApiResponse.ok(patientService.get(id));
    }

    @PostMapping
    public ApiResponse<Patient> create(@RequestBody Patient patient) {
        return ApiResponse.ok(patientService.create(patient));
    }

    @PutMapping("/{id}")
    public ApiResponse<Patient> update(@PathVariable Long id, @RequestBody Patient patient) {
        return ApiResponse.ok(patientService.update(id, patient));
    }
}
