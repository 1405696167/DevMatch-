package com.devmatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devmatch.common.PageResult;
import com.devmatch.common.exception.BusinessException;
import com.devmatch.entity.*;
import com.devmatch.mapper.*;
import com.devmatch.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final KycRecordMapper kycRecordMapper;
    private final ReviewMapper reviewMapper;
    private final UserSkillMapper userSkillMapper;
    private final UserPortfolioMapper userPortfolioMapper;
    private final ProjectMapper projectMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Value("${upload.url-prefix:/uploads}")
    private String urlPrefix;

    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        return user;
    }

    public User getProfile() {
        return getById(SecurityUtil.getCurrentUserId());
    }

    @Transactional
    public User updateProfile(Map<String, Object> updates) {
        User user = getById(SecurityUtil.getCurrentUserId());
        if (updates.containsKey("nickname")) user.setNickname((String) updates.get("nickname"));
        if (updates.containsKey("bio")) user.setBio((String) updates.get("bio"));
        if (updates.containsKey("city")) user.setCity((String) updates.get("city"));
        if (updates.containsKey("homepage")) user.setHomepage((String) updates.get("homepage"));
        if (updates.containsKey("companyName")) user.setCompanyName((String) updates.get("companyName"));
        if (updates.containsKey("email")) user.setEmail((String) updates.get("email"));
        userMapper.updateById(user);
        return user;
    }

    public String uploadKycFile(MultipartFile file, String subDir) throws IOException {
        java.nio.file.Path baseDir = java.nio.file.Paths.get(uploadPath).toAbsolutePath().normalize();
        java.nio.file.Path targetDir = baseDir.resolve("kyc").resolve(subDir);
        java.nio.file.Files.createDirectories(targetDir);
        String filename = UUID.randomUUID() + getExtension(file.getOriginalFilename());
        file.transferTo(targetDir.resolve(filename).toFile());
        return urlPrefix + "/kyc/" + subDir + "/" + filename;
    }

    public String uploadAvatar(MultipartFile file) throws IOException {
        java.nio.file.Path baseDir = java.nio.file.Paths.get(uploadPath).toAbsolutePath().normalize();
        java.nio.file.Path targetDir = baseDir.resolve("avatars");
        java.nio.file.Files.createDirectories(targetDir);
        String filename = UUID.randomUUID() + getExtension(file.getOriginalFilename());
        file.transferTo(targetDir.resolve(filename).toFile());
        String url = urlPrefix + "/avatars/" + filename;

        User user = getById(SecurityUtil.getCurrentUserId());
        user.setAvatar(url);
        userMapper.updateById(user);
        return url;
    }

    public void changePassword(String oldPwd, String newPwd) {
        User user = getById(SecurityUtil.getCurrentUserId());
        if (!passwordEncoder.matches(oldPwd, user.getPasswordHash())) {
            throw new BusinessException("原密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(newPwd));
        userMapper.updateById(user);
    }

    @Transactional
    public void submitKyc(KycRecord record) {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = getById(userId);

        KycRecord existing = kycRecordMapper.findLatestByUserId(userId);
        if (existing != null && "AUDITING".equals(existing.getStatus())) {
            throw new BusinessException("已有待审核的认证申请，请耐心等待");
        }

        record.setUserId(userId);
        record.setStatus("AUDITING");
        kycRecordMapper.insert(record);

        user.setKycStatus("AUDITING");
        userMapper.updateById(user);
    }

    @Transactional
    public void submitDeveloperKyc(String realName, String idNumber,
                                    MultipartFile idFrontFile, MultipartFile idBackFile) throws IOException {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = getById(userId);

        KycRecord existing = kycRecordMapper.findLatestByUserId(userId);
        if (existing != null && "AUDITING".equals(existing.getStatus())) {
            throw new BusinessException("已有待审核的认证申请，请耐心等待");
        }

        KycRecord record = new KycRecord();
        record.setUserId(userId);
        record.setType("PERSONAL");
        record.setRealName(realName);
        record.setIdNumber(idNumber);
        record.setStatus("AUDITING");

        if (idFrontFile == null || idFrontFile.isEmpty()) {
            throw new BusinessException("请上传身份证正面照片");
        }
        if (idBackFile == null || idBackFile.isEmpty()) {
            throw new BusinessException("请上传身份证背面照片");
        }
        record.setIdFrontUrl(uploadKycFile(idFrontFile, "id-front"));
        record.setIdBackUrl(uploadKycFile(idBackFile, "id-back"));

        kycRecordMapper.insert(record);
        user.setKycStatus("AUDITING");
        userMapper.updateById(user);
    }

    public KycRecord getKycStatus() {
        return kycRecordMapper.findLatestByUserId(SecurityUtil.getCurrentUserId());
    }

    public Map<String, Object> getDeveloperProfile(Long developerId) {
        User developer = getById(developerId);
        if (!"DEVELOPER".equals(developer.getRole())) throw new BusinessException("该用户不是开发者");

        Map<String, Object> profile = new HashMap<>();
        profile.put("user", developer);
        profile.put("skills", userSkillMapper.findByUserId(developerId));
        profile.put("portfolios", userPortfolioMapper.findByUserId(developerId));

        java.math.BigDecimal avg = reviewMapper.getAvgRating(developerId);
        profile.put("avgRating", avg != null ? avg.doubleValue() : 5.0);
        profile.put("goodReviewCount", reviewMapper.countGoodReviews(developerId));
        long completedProjects = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getDeveloperId, developerId)
                        .eq(Project::getStatus, "COMPLETED")
                        .eq(Project::getDeleted, 0));
        profile.put("completedProjects", completedProjects);

        List<Review> rawReviews = reviewMapper.findReceivedReviews(developerId, 20);
        List<Map<String, Object>> reviewViews = new ArrayList<>();
        for (Review r : rawReviews) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("rating", r.getRating());
            m.put("content", r.getContent() != null ? r.getContent() : "");
            m.put("createdAt", r.getCreatedAt());
            m.put("tags", r.getTags());
            if (r.getProjectId() != null) {
                Project p = projectMapper.selectById(r.getProjectId());
                m.put("projectName", p != null ? p.getName() : "已完成项目");
            } else {
                m.put("projectName", "已完成项目");
            }
            reviewViews.add(m);
        }
        profile.put("reviews", reviewViews);
        return profile;
    }

    @Transactional
    public void adminToggleStatus(Long userId) {
        User user = getById(userId);
        if ("ACTIVE".equals(user.getStatus())) {
            user.setStatus("DISABLED");
        } else {
            user.setStatus("ACTIVE");
        }
        userMapper.updateById(user);
    }

    public PageResult<User> adminSearch(int page, int size, String keyword, String role, String status) {
        Page<User> p = new Page<>(page, size);
        return PageResult.of(userMapper.searchAdmin(p, keyword, role, status));
    }

    @Transactional
    public void auditKyc(Long kycId, String action, String remark) {
        KycRecord record = kycRecordMapper.selectById(kycId);
        if (record == null) throw new BusinessException("KYC记录不存在");
        record.setAuditorId(SecurityUtil.getCurrentUserId());
        record.setRemark(remark);

        User user = getById(record.getUserId());
        boolean enterpriseKyc = "ENTERPRISE".equals(record.getType());
        String rejectReason = (remark == null || remark.isBlank()) ? "无" : remark;
        if ("APPROVE".equals(action)) {
            record.setStatus("VERIFIED");
            user.setKycStatus("VERIFIED");
            if (enterpriseKyc) {
                notificationService.send(user.getId(), "AUDIT", "您的企业认证审核已通过", "/enterprise/profile?tab=kyc");
            } else {
                notificationService.send(user.getId(), "AUDIT", "您的实名认证已通过审核", "/developer/profile?tab=kyc");
            }
        } else {
            record.setStatus("REJECTED");
            user.setKycStatus("NONE");
            if (enterpriseKyc) {
                notificationService.send(user.getId(), "AUDIT",
                        "您的企业认证审核未通过，原因：" + rejectReason, "/enterprise/profile?tab=kyc");
            } else {
                notificationService.send(user.getId(), "AUDIT",
                        "您的实名认证审核未通过，原因：" + rejectReason, "/developer/profile?tab=kyc");
            }
        }
        kycRecordMapper.updateById(record);
        userMapper.updateById(user);
    }

    public PageResult<KycRecord> getPendingKyc(int page, int size) {
        Page<KycRecord> p = new Page<>(page, size);
        return PageResult.of(kycRecordMapper.selectPage(p,
                new LambdaQueryWrapper<KycRecord>().eq(KycRecord::getStatus, "AUDITING").orderByAsc(KycRecord::getCreatedAt)));
    }

    public PageResult<KycRecord> listKyc(int page, int size, String status) {
        Page<KycRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<KycRecord> qw = new LambdaQueryWrapper<KycRecord>().orderByDesc(KycRecord::getCreatedAt);
        if (status != null && !status.isEmpty()) {
            qw.eq(KycRecord::getStatus, status);
        }
        return PageResult.of(kycRecordMapper.selectPage(p, qw));
    }

    public Map<String, Object> listKycWithUser(int page, int size, String status) {
        Page<KycRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<KycRecord> qw = new LambdaQueryWrapper<KycRecord>().orderByDesc(KycRecord::getCreatedAt);
        if (status != null && !status.isEmpty()) {
            qw.eq(KycRecord::getStatus, status);
        }
        IPage<KycRecord> result = kycRecordMapper.selectPage(p, qw);
        List<Map<String, Object>> items = new ArrayList<>();
        for (KycRecord kyc : result.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", kyc.getId());
            item.put("userId", kyc.getUserId());
            item.put("type", kyc.getType());
            item.put("realName", kyc.getRealName());
            item.put("creditCode", kyc.getCreditCode());
            item.put("idFrontUrl", kyc.getIdFrontUrl());
            item.put("idBackUrl", kyc.getIdBackUrl());
            item.put("licenseUrl", kyc.getLicenseUrl());
            item.put("status", kyc.getStatus());
            item.put("remark", kyc.getRemark());
            item.put("createdAt", kyc.getCreatedAt());
            // 解析法人信息（格式: 法人姓名|身份证号）
            if ("ENTERPRISE".equals(kyc.getType()) && kyc.getIdNumber() != null && kyc.getIdNumber().contains("|")) {
                String[] parts = kyc.getIdNumber().split("\\|", 2);
                item.put("legalPersonName", parts[0]);
                item.put("legalPersonId", parts.length > 1 ? parts[1] : "");
            } else {
                item.put("idNumber", kyc.getIdNumber());
            }
            // 关联用户信息
            User user = userMapper.selectById(kyc.getUserId());
            if (user != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
                userInfo.put("phone", user.getPhone());
                userInfo.put("avatar", user.getAvatar());
                userInfo.put("companyName", user.getCompanyName());
                item.put("user", userInfo);
            }
            items.add(item);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("list", items);
        res.put("total", result.getTotal());
        return res;
    }

    public Map<String, Object> searchDevelopers(int page, int size, String keyword, String skill) {
        Page<User> p = new Page<>(page, size);
        IPage<User> result = userMapper.searchDevelopers(p, keyword, skill);
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (User user : result.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("name", user.getNickname() != null ? user.getNickname() : user.getUsername());
            item.put("avatar", user.getAvatar());
            item.put("bio", user.getBio());
            item.put("city", user.getCity());
            item.put("creditScore", user.getCreditScore() != null ? user.getCreditScore() : 100);
            item.put("kycVerified", "VERIFIED".equals(user.getKycStatus()));
            // 技能列表
            List<UserSkill> skills = userSkillMapper.findByUserId(user.getId());
            item.put("skills", skills.stream().map(UserSkill::getName).collect(java.util.stream.Collectors.toList()));
            // 评分
            java.math.BigDecimal avgRating = reviewMapper.getAvgRating(user.getId());
            item.put("rating", avgRating != null ? avgRating.doubleValue() : 5.0);
            // 好评数
            int goodCount = reviewMapper.countGoodReviews(user.getId());
            item.put("goodReviewCount", goodCount);
            // 完成项目数
            long completedProjects = projectMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Project>()
                    .eq(Project::getDeveloperId, user.getId())
                    .eq(Project::getStatus, "COMPLETED")
                    .eq(Project::getDeleted, 0)
            );
            item.put("completedProjects", completedProjects);
            // 好评率
            long totalProjects = projectMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Project>()
                    .eq(Project::getDeveloperId, user.getId())
                    .eq(Project::getDeleted, 0)
            );
            item.put("rateGood", totalProjects > 0 ? (int)(goodCount * 100 / totalProjects) : 100);
            // 时薪（暂无字段，默认0）
            item.put("hourlyRate", 0);
            item.put("title", user.getBio() != null && user.getBio().length() > 20 ? user.getBio().substring(0, 20) + "..." : user.getBio());
            enriched.add(item);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("list", enriched);
        res.put("total", result.getTotal());
        return res;
    }

    // ==================== 简历/技能/作品集 ====================

    public Map<String, Object> getResume() {
        Long userId = SecurityUtil.getCurrentUserId();
        Map<String, Object> resume = new HashMap<>();
        resume.put("skills", userSkillMapper.findByUserId(userId));
        resume.put("portfolios", userPortfolioMapper.findByUserId(userId));
        resume.put("user", getById(userId));
        long completedProjects = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getDeveloperId, userId)
                        .eq(Project::getStatus, "COMPLETED")
                        .eq(Project::getDeleted, 0));
        resume.put("completedProjects", completedProjects);
        int goodCount = reviewMapper.countGoodReviews(userId);
        long totalProjects = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getDeveloperId, userId)
                        .eq(Project::getDeleted, 0));
        resume.put("rateGood", totalProjects > 0 ? (int) (goodCount * 100 / Math.max(totalProjects, 1)) : 100);
        return resume;
    }

    @Transactional
    public void updateResume(Map<String, Object> data) {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = getById(userId);
        if (data.containsKey("bio")) user.setBio((String) data.get("bio"));
        if (data.containsKey("city")) user.setCity((String) data.get("city"));
        if (data.containsKey("homepage")) user.setHomepage((String) data.get("homepage"));
        userMapper.updateById(user);
    }

    @Transactional
    public void addSkill(Map<String, Object> data) {
        Long userId = SecurityUtil.getCurrentUserId();
        String name = (String) data.get("name");
        if (name == null || name.isBlank()) {
            throw new BusinessException("技能名称不能为空");
        }
        UserSkill skill = new UserSkill();
        skill.setUserId(userId);
        skill.setName(name.trim());
        Object level = data.get("level");
        skill.setLevel(level != null ? Integer.parseInt(level.toString()) : 3);
        userSkillMapper.insert(skill);
    }

    @Transactional
    public void updateSkill(Long skillId, Map<String, Object> data) {
        UserSkill skill = userSkillMapper.selectById(skillId);
        if (skill == null) {
            throw new BusinessException("技能不存在");
        }
        if (!skill.getUserId().equals(SecurityUtil.getCurrentUserId())) {
            throw new BusinessException(403, "无权操作");
        }
        if (data.containsKey("name")) {
            String name = (String) data.get("name");
            if (name == null || name.isBlank()) {
                throw new BusinessException("技能名称不能为空");
            }
            skill.setName(name.trim());
        }
        if (data.containsKey("level")) {
            skill.setLevel(Integer.parseInt(data.get("level").toString()));
        }
        userSkillMapper.updateById(skill);
    }

    @Transactional
    public void deleteSkill(Long skillId) {
        UserSkill skill = userSkillMapper.selectById(skillId);
        if (skill == null) throw new BusinessException("技能不存在");
        if (!skill.getUserId().equals(SecurityUtil.getCurrentUserId())) throw new BusinessException(403, "无权操作");
        userSkillMapper.deleteById(skillId);
    }

    @Transactional
    public void addPortfolio(Map<String, Object> data) {
        Long userId = SecurityUtil.getCurrentUserId();
        String name = (String) data.get("name");
        if (name == null || name.isBlank()) {
            throw new BusinessException("项目名称不能为空");
        }
        UserPortfolio p = new UserPortfolio();
        p.setUserId(userId);
        p.setName(name.trim());
        p.setDescription((String) data.get("description"));
        p.setLink((String) data.get("link"));
        if (data.get("tags") instanceof java.util.List<?> tags) {
            p.setTags(tags.stream().map(Object::toString).collect(java.util.stream.Collectors.toList()));
        }
        userPortfolioMapper.insert(p);
    }

    @Transactional
    public void updatePortfolio(Long id, Map<String, Object> data) {
        UserPortfolio p = userPortfolioMapper.selectById(id);
        if (p == null) throw new BusinessException("作品集不存在");
        if (!p.getUserId().equals(SecurityUtil.getCurrentUserId())) throw new BusinessException(403, "无权操作");
        if (data.containsKey("name")) p.setName((String) data.get("name"));
        if (data.containsKey("description")) p.setDescription((String) data.get("description"));
        if (data.containsKey("link")) p.setLink((String) data.get("link"));
        if (data.get("tags") instanceof java.util.List<?> tags) {
            p.setTags(tags.stream().map(Object::toString).collect(java.util.stream.Collectors.toList()));
        }
        userPortfolioMapper.updateById(p);
    }

    @Transactional
    public void deletePortfolio(Long id) {
        UserPortfolio p = userPortfolioMapper.selectById(id);
        if (p == null) throw new BusinessException("作品集不存在");
        if (!p.getUserId().equals(SecurityUtil.getCurrentUserId())) throw new BusinessException(403, "无权操作");
        userPortfolioMapper.deleteById(id);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }
}
