package com.devmatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devmatch.common.PageResult;
import com.devmatch.common.exception.BusinessException;
import com.devmatch.entity.*;
import com.devmatch.mapper.*;
import com.devmatch.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final WalletTransactionMapper transactionMapper;
    private final ComplaintMapper complaintMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final AnnouncementMapper announcementMapper;

    public Map<String, Object> getDashboard() {
        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0));
        long totalTasks = taskMapper.selectCount(new LambdaQueryWrapper<Task>().eq(Task::getDeleted, 0));
        long totalProjects = projectMapper.selectCount(new LambdaQueryWrapper<Project>().eq(Project::getDeleted, 0));
        long pendingAudit = taskMapper.selectCount(new LambdaQueryWrapper<Task>().eq(Task::getStatus, "AUDITING"));

        return Map.of(
                "totalUsers", totalUsers,
                "totalTasks", totalTasks,
                "totalProjects", totalProjects,
                "pendingAudit", pendingAudit
        );
    }

    public List<Map<String, Object>> getUserGrowthChart() {
        // 近7天用户注册数
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .ge(User::getCreatedAt, start)
                    .lt(User::getCreatedAt, end));
            result.add(Map.of("date", date.toString(), "count", count));
        }
        return result;
    }

    public List<Map<String, Object>> getTransactionChart() {
        // 近7天交易额
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long count = transactionMapper.selectCount(new LambdaQueryWrapper<WalletTransaction>()
                    .eq(WalletTransaction::getType, "INCOME")
                    .ge(WalletTransaction::getCreatedAt, start)
                    .lt(WalletTransaction::getCreatedAt, end));
            result.add(Map.of("date", date.toString(), "count", count));
        }
        return result;
    }

    // 申诉管理
    public PageResult<Complaint> listComplaints(int page, int size, String status) {
        Page<Complaint> p = new Page<>(page, size);
        return PageResult.of(complaintMapper.findAll(p, status));
    }

    @Transactional
    public void handleComplaint(Long id, String result, String remark) {
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null) throw new BusinessException("申诉不存在");
        complaint.setStatus("RESOLVED");
        complaint.setResult(result);
        complaint.setRemark(remark);
        complaint.setHandlerId(SecurityUtil.getCurrentUserId());
        complaintMapper.updateById(complaint);
    }

    // 系统配置
    public List<SystemConfig> listConfigs() {
        return systemConfigMapper.selectList(null);
    }

    @Transactional
    public void updateConfig(String key, String value) {
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        if (config == null) throw new BusinessException("配置项不存在");
        if ("task_publish_deposit_rate".equals(key)) {
            try {
                BigDecimal v = new BigDecimal(value.trim());
                if (v.compareTo(BigDecimal.ZERO) < 0 || v.compareTo(BigDecimal.ONE) > 0) {
                    throw new BusinessException("发布押金比例须在 0～1 之间（如 0.05 表示按预算上限的 5%）");
                }
            } catch (NumberFormatException e) {
                throw new BusinessException("发布押金比例格式无效");
            }
        }
        config.setConfigValue(value);
        systemConfigMapper.updateById(config);
    }

    // 公告管理
    public List<Announcement> listAnnouncements() {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getCreatedAt));
    }

    @Transactional
    public Announcement createAnnouncement(Announcement announcement) {
        announcementMapper.insert(announcement);
        return announcement;
    }

    @Transactional
    public Announcement updateAnnouncement(Long id, Announcement data) {
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) throw new BusinessException("公告不存在");
        data.setId(id);
        announcementMapper.updateById(data);
        return data;
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        announcementMapper.deleteById(id);
    }
}
