package com.devmatch.controller;

import com.devmatch.common.R;
import com.devmatch.entity.KycRecord;
import com.devmatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 企业相关接口
 * 前端调用路径: /enterprise/kyc
 */
@Tag(name = "企业接口")
@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final UserService userService;

    // 前端调用: POST /enterprise/kyc  (multipart/form-data)
    @Operation(summary = "提交企业资质认证")
    @PostMapping("/kyc")
    public R<Void> submitEnterpriseKyc(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String creditCode,
            @RequestParam(required = false) String legalPersonName,
            @RequestParam(required = false) String legalPersonId,
            @RequestParam(required = false) MultipartFile licenseFile,
            @RequestParam(required = false) MultipartFile idFrontFile,
            @RequestParam(required = false) MultipartFile idBackFile
    ) throws IOException {
        KycRecord record = new KycRecord();
        record.setType("ENTERPRISE");
        record.setRealName(companyName);
        record.setCreditCode(creditCode);
        // 法人信息存入 idNumber 字段（格式: 法人姓名|身份证号）
        if (legalPersonName != null || legalPersonId != null) {
            record.setIdNumber((legalPersonName != null ? legalPersonName : "") + "|" + (legalPersonId != null ? legalPersonId : ""));
        }
        if (licenseFile != null && !licenseFile.isEmpty()) {
            record.setLicenseUrl(userService.uploadKycFile(licenseFile, "license"));
        }
        if (idFrontFile != null && !idFrontFile.isEmpty()) {
            record.setIdFrontUrl(userService.uploadKycFile(idFrontFile, "id-front"));
        }
        if (idBackFile != null && !idBackFile.isEmpty()) {
            record.setIdBackUrl(userService.uploadKycFile(idBackFile, "id-back"));
        }
        userService.submitKyc(record);
        return R.ok();
    }
}
