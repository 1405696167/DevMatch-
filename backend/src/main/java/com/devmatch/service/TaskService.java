package com.devmatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devmatch.common.PageResult;
import com.devmatch.common.exception.BusinessException;
import com.devmatch.dto.BidRequest;
import com.devmatch.dto.TaskCreateRequest;
import com.devmatch.entity.*;
import com.devmatch.mapper.*;
import com.devmatch.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final BidMapper bidMapper;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final ReviewMapper reviewMapper;
    private final MilestoneMapper milestoneMapper;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final SystemConfigMapper systemConfigMapper;
    private final RedisLockService redisLockService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Value("${upload.url-prefix:/uploads}")
    private String urlPrefix;

    public PageResult<Task> list(int page, int size, String keyword, String status,
                                 Long companyId, Double budgetMin, Double budgetMax, String sort, boolean mine) {
        // mine=true 时自动注入当前登录企业的 companyId，用于企业端"我的需求"
        if (mine && SecurityUtil.isEnterprise()) {
            companyId = SecurityUtil.getCurrentUserId();
        }
        Page<Task> p = new Page<>(page, size);
        LambdaQueryWrapper<Task> qw = new LambdaQueryWrapper<Task>().eq(Task::getDeleted, 0);
        // APPROVED_ALL：管理员查看所有曾通过审核的任务（含进行中、已完成等后续状态）
        if ("APPROVED_ALL".equals(status)) {
            qw.in(Task::getStatus, List.of("PUBLISHED", "EXPIRED", "IN_PROGRESS", "CLOSED", "COMPLETED"));
        } else if (status != null && !status.isEmpty()) {
            qw.eq(Task::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(Task::getTitle, keyword);
        }
        if (companyId != null) {
            qw.eq(Task::getCompanyId, companyId);
        }
        if (budgetMin != null) {
            qw.ge(Task::getBudgetMax, budgetMin);
        }
        if (budgetMax != null) {
            qw.le(Task::getBudgetMin, budgetMax);
        }
        if ("BUDGET_DESC".equals(sort)) {
            qw.orderByDesc(Task::getBudgetMax);
        } else if ("BUDGET_ASC".equals(sort)) {
            qw.orderByAsc(Task::getBudgetMin);
        } else {
            qw.orderByDesc(Task::getCreatedAt);
        }
        // 任务市场：不展示已过接单截止日期的招募任务；企业「招募中」Tab 同理（避免与「已过期」重复）
        boolean publishedOnly = status != null && "PUBLISHED".equals(status);
        if (publishedOnly) {
            qw.and(w -> w.isNull(Task::getDeadline).or().ge(Task::getDeadline, LocalDate.now()));
        }
        PageResult<Task> result = PageResult.of(taskMapper.selectPage(p, qw));
        for (Task task : result.getList()) {
            attachCompany(task, false);
        }
        return result;
    }

    public Task detail(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) throw new BusinessException("任务不存在");
        taskMapper.incrementViewCount(id);
        attachCompany(task, true);
        task.setBiddingClosed(!isOpenForBidding(task));
        return task;
    }

    /** 仍在招募且未过接单截止日期则可投标 */
    private boolean isOpenForBidding(Task t) {
        if (t == null || !"PUBLISHED".equals(t.getStatus())) {
            return false;
        }
        if (t.getDeadline() != null && t.getDeadline().isBefore(LocalDate.now())) {
            return false;
        }
        return true;
    }

    /** 企业展示名：公司名 > 昵称 > 用户名 */
    private String enterpriseDisplayName(User u) {
        if (u.getCompanyName() != null && !u.getCompanyName().isBlank()) {
            return u.getCompanyName();
        }
        if (u.getNickname() != null && !u.getNickname().isBlank()) {
            return u.getNickname();
        }
        return u.getUsername() != null ? u.getUsername() : "";
    }

    /**
     * @param extended 任务详情侧栏需要认证状态、项目数、好评率等
     */
    private void attachCompany(Task task, boolean extended) {
        Long cid = task.getCompanyId();
        if (cid == null) {
            task.setCompany(null);
            return;
        }
        User u = userMapper.selectById(cid);
        if (u == null) {
            task.setCompany(null);
            return;
        }
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", u.getId());
        card.put("avatar", u.getAvatar());
        card.put("name", enterpriseDisplayName(u));
        int credit = u.getCreditScore() != null ? u.getCreditScore() : 0;
        card.put("credit", credit);
        if (extended) {
            card.put("verified", "VERIFIED".equals(u.getKycStatus()));
            long projectCount = projectMapper.selectCount(
                    new LambdaQueryWrapper<Project>()
                            .eq(Project::getEnterpriseId, u.getId())
                            .eq(Project::getDeleted, 0));
            card.put("projectCount", projectCount);
            long revTotal = reviewMapper.selectCount(
                    new LambdaQueryWrapper<Review>().eq(Review::getRevieweeId, u.getId()));
            int rateGood = 100;
            if (revTotal > 0) {
                int good = reviewMapper.countGoodReviews(u.getId());
                rateGood = (int) Math.round(100.0 * good / revTotal);
            }
            card.put("rateGood", rateGood);
        }
        task.setCompany(card);
    }

    @Transactional
    public Task create(TaskCreateRequest req) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (!SecurityUtil.isEnterprise()) throw new BusinessException(403, "仅企业用户可发布任务");
        User enterprise = userMapper.selectById(userId);
        if (enterprise == null || !"VERIFIED".equals(enterprise.getKycStatus())) {
            throw new BusinessException(403, "请先完成企业认证后再发布需求");
        }

        Task task = new Task();
        BeanUtils.copyProperties(req, task);
        task.setCompanyId(userId);
        task.setBidCount(0);
        task.setViewCount(0);

        if ("PUBLISH".equals(req.getAction())) {
            task.setStatus("AUDITING");
        } else {
            task.setStatus("DRAFT");
        }
        normalizeTaskDeliverySettings(task);
        taskMapper.insert(task);
        if ("AUDITING".equals(task.getStatus())) {
            maybeChargePublishDeposit(task, "DRAFT");
        }
        attachCompany(task, false);
        return task;
    }

    @Transactional
    public Task update(Long id, TaskCreateRequest req) {
        String lockKey = "task:state:" + id;
        String lockToken = redisLockService.tryLockOrThrow(lockKey);
        try {
            return doUpdate(id, req);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private Task doUpdate(Long id, TaskCreateRequest req) {
        Task task = taskMapper.selectById(id);
        if (task == null) throw new BusinessException("任务不存在");
        checkOwner(task);
        String oldStatus = task.getStatus();
        // 提交审核时校验企业认证
        if ("PUBLISH".equals(req.getAction())) {
            User enterprise = userMapper.selectById(SecurityUtil.getCurrentUserId());
            if (enterprise == null || !"VERIFIED".equals(enterprise.getKycStatus())) {
                throw new BusinessException(403, "请先完成企业认证后再发布需求");
            }
        }
        if (!List.of("DRAFT", "REJECTED", "AUDITING", "PUBLISHED").contains(task.getStatus())) {
            throw new BusinessException("进行中或已完成的任务不允许编辑");
        }
        BeanUtils.copyProperties(req, task, "id", "companyId", "bidCount", "viewCount", "status", "company");
        normalizeTaskDeliverySettings(task);
        if ("PUBLISH".equals(req.getAction())) {
            // 明确发布/重新发布：提交审核
            task.setStatus("AUDITING");
        } else if (List.of("AUDITING", "PUBLISHED").contains(oldStatus)) {
            // 已发布/审核中的任务被编辑后，需重新审核
            task.setStatus("AUDITING");
        }
        taskMapper.updateById(task);
        if ("AUDITING".equals(task.getStatus()) && List.of("DRAFT", "REJECTED").contains(oldStatus)) {
            maybeChargePublishDeposit(task, oldStatus);
        }
        attachCompany(task, false);
        return task;
    }

    @Transactional
    public void delete(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) throw new BusinessException("任务不存在");
        checkOwner(task);
        if (!List.of("DRAFT", "REJECTED").contains(task.getStatus())) {
            throw new BusinessException("仅草稿/驳回状态可删除");
        }
        taskMapper.deleteById(id);
    }

    @Transactional
    public void publish(Long id) {
        String lockKey = "task:state:" + id;
        String lockToken = redisLockService.tryLockOrThrow(lockKey);
        try {
            doPublish(id);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private void doPublish(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) throw new BusinessException("任务不存在");
        checkOwner(task);
        if (!"DRAFT".equals(task.getStatus()) && !"REJECTED".equals(task.getStatus())) {
            throw new BusinessException("只有草稿/驳回状态可提交审核");
        }
        String previousStatus = task.getStatus();
        task.setStatus("AUDITING");
        taskMapper.updateById(task);
        maybeChargePublishDeposit(task, previousStatus);
    }

    @Transactional
    public void close(Long id) {
        String lockKey = "task:state:" + id;
        String lockToken = redisLockService.tryLockOrThrow(lockKey);
        try {
            doClose(id);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private void doClose(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) throw new BusinessException("任务不存在");
        checkOwner(task);
        if (!"PUBLISHED".equals(task.getStatus())) {
            throw new BusinessException("仅招募中的需求可关闭招募");
        }
        refundPublishDepositToWallet(task, "关闭招募，退还发布押金");
        task.setStatus("CLOSED");
        taskMapper.updateById(task);
    }

    // 投标
    @Transactional
    public Bid submitBid(BidRequest req) {
        Long developerId = SecurityUtil.getCurrentUserId();
        if (!SecurityUtil.isDeveloper()) throw new BusinessException(403, "仅开发者可以投标");

        String lockKey = "task:bid:" + req.getTaskId() + ":" + developerId;
        String lockToken = redisLockService.tryLockOrThrow(lockKey, Duration.ofSeconds(10));
        try {
            return doSubmitBid(req, developerId);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private Bid doSubmitBid(BidRequest req, Long developerId) {
        // 必须完成实名认证才能投标
        User developer = userMapper.selectById(developerId);
        if (developer == null || !"VERIFIED".equals(developer.getKycStatus())) {
            throw new BusinessException(403, "投标前需先完成实名认证");
        }

        Task task = taskMapper.selectById(req.getTaskId());
        if (task == null) throw new BusinessException("任务不存在");
        if ("EXPIRED".equals(task.getStatus())) {
            throw new BusinessException("任务已过期，不可投标");
        }
        if (!"PUBLISHED".equals(task.getStatus())) {
            throw new BusinessException("任务未发布，不可投标");
        }
        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDate.now())) {
            throw new BusinessException("任务已过期，不可投标");
        }
        if (bidMapper.countActiveBid(req.getTaskId(), developerId) > 0) {
            throw new BusinessException("您已投标该任务，请勿重复投标");
        }

        Bid bid = new Bid();
        bid.setTaskId(req.getTaskId());
        bid.setDeveloperId(developerId);
        bid.setAmount(req.getAmount());
        bid.setDays(req.getDays());
        bid.setProposal(req.getProposal());
        bid.setStatus("PENDING");
        bidMapper.insert(bid);
        taskMapper.updateBidCount(req.getTaskId(), 1);

        // 通知企业
        notificationService.send(task.getCompanyId(), "PROJECT",
                "您的任务「" + task.getTitle() + "」收到新投标", "/tasks/" + task.getId() + "/bids");
        return bid;
    }

    public List<Map<String, Object>> getBids(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        if (!SecurityUtil.isAdmin()) checkOwner(task);
        List<Bid> bids = bidMapper.findByTaskId(taskId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Bid bid : bids) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", bid.getId());
            item.put("taskId", bid.getTaskId());
            item.put("developerId", bid.getDeveloperId());
            item.put("amount", bid.getAmount());
            item.put("days", bid.getDays());
            item.put("proposal", bid.getProposal());
            item.put("status", bid.getStatus());
            item.put("createdAt", bid.getCreatedAt());
            // 查询开发者基本信息
            User dev = userMapper.selectById(bid.getDeveloperId());
            if (dev != null) {
                Map<String, Object> developer = new HashMap<>();
                developer.put("id", dev.getId());
                developer.put("name", dev.getNickname() != null ? dev.getNickname() : dev.getUsername());
                developer.put("avatar", dev.getAvatar());
                developer.put("creditScore", dev.getCreditScore());
                developer.put("kycVerified", "VERIFIED".equals(dev.getKycStatus()));
                developer.put("bio", dev.getBio());
                developer.put("city", dev.getCity());
                item.put("developer", developer);
            }
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> getMyBids(String status, int page, int size) {
        Long developerId = SecurityUtil.getCurrentUserId();
        List<Bid> bids = bidMapper.findByDeveloperId(developerId, status);
        // 关联任务信息和项目ID
        List<Map<String, Object>> items = new ArrayList<>();
        for (Bid bid : bids) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", bid.getId());
            item.put("taskId", bid.getTaskId());
            item.put("amount", bid.getAmount());
            item.put("days", bid.getDays());
            item.put("proposal", bid.getProposal());
            item.put("status", bid.getStatus());
            item.put("projectId", bid.getProjectId());
            item.put("createdAt", bid.getCreatedAt());
            Task task = taskMapper.selectById(bid.getTaskId());
            if (task != null) {
                item.put("taskTitle", task.getTitle());
                item.put("skills", task.getSkills());
            }
            items.add(item);
        }
        // 简单分页
        int total = items.size();
        int fromIdx = Math.min((page - 1) * size, total);
        int toIdx = Math.min(fromIdx + size, total);
        return Map.of("list", items.subList(fromIdx, toIdx), "total", total);
    }

    @Transactional
    public void cancelBid(Long bidId) {
        Bid bid = bidMapper.selectById(bidId);
        if (bid == null) throw new BusinessException("投标不存在");
        if (!bid.getDeveloperId().equals(SecurityUtil.getCurrentUserId())) {
            throw new BusinessException("无权操作");
        }
        if (!"PENDING".equals(bid.getStatus())) throw new BusinessException("该投标不可撤回");
        bid.setStatus("CANCELLED");
        bidMapper.updateById(bid);
        taskMapper.updateBidCount(bid.getTaskId(), -1);
    }

    @Transactional
    public Project selectBid(Long bidId) {
        Long enterpriseId = SecurityUtil.getCurrentUserId();
        if (!SecurityUtil.isEnterprise()) throw new BusinessException(403, "仅企业用户可选标");

        Bid targetBid = bidMapper.selectById(bidId);
        if (targetBid == null) throw new BusinessException("投标不存在");

        String lockKey = "task:state:" + targetBid.getTaskId();
        String lockToken = redisLockService.tryLockOrThrow(lockKey, Duration.ofSeconds(20));
        try {
            return doSelectBid(bidId, enterpriseId);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private Project doSelectBid(Long bidId, Long enterpriseId) {
        Bid bid = bidMapper.selectById(bidId);
        if (bid == null) throw new BusinessException("投标不存在");

        Task task = taskMapper.selectById(bid.getTaskId());
        if (!task.getCompanyId().equals(enterpriseId)) throw new BusinessException("无权操作");
        if (!"PUBLISHED".equals(task.getStatus())) {
            throw new BusinessException("任务状态不允许选标");
        }
        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDate.now())) {
            throw new BusinessException("任务已过期，不可选标");
        }

        // 创建项目
        Project project = new Project();
        project.setTaskId(task.getId());
        project.setBidId(bid.getId());
        project.setName(task.getTitle());
        project.setDeveloperId(bid.getDeveloperId());
        project.setEnterpriseId(enterpriseId);
        project.setAmount(bid.getAmount());
        String paymentType = task.getPaymentType();
        if (paymentType == null || paymentType.isBlank()) paymentType = "MILESTONE";
        project.setPaymentType(paymentType);
        String planBy = task.getMilestonePlanBy();
        if (planBy == null || planBy.isBlank()) planBy = "DEVELOPER";
        if (!"ENTERPRISE".equals(planBy)) planBy = "DEVELOPER";
        if ("ONCE".equals(paymentType)) {
            planBy = "DEVELOPER";
        }
        project.setMilestonePlanBy(planBy);
        project.setStatus("IN_PROGRESS");
        project.setProgress(0);
        project.setStartDate(LocalDate.now());
        if (bid.getDays() != null && bid.getDays() > 0) {
            project.setEndDate(LocalDate.now().plusDays(bid.getDays()));
        } else if (task.getDurationDays() != null && task.getDurationDays() > 0) {
            project.setEndDate(LocalDate.now().plusDays(task.getDurationDays()));
        }
        projectMapper.insert(project);

        if ("ONCE".equals(paymentType)) {
            Milestone root = new Milestone();
            root.setProjectId(project.getId());
            root.setName("整单交付与验收");
            root.setDescription("本需求为一次性付款：全部成果在本节点统一提交，企业验收通过后一次性结算合同全款。");
            root.setAmount(project.getAmount());
            root.setDeadline(task.getDeadline());
            root.setStatus("IN_PROGRESS");
            root.setSortOrder(0);
            milestoneMapper.insert(root);
        }

        // 更新投标和任务状态
        bid.setStatus("SELECTED");
        bid.setProjectId(project.getId());
        bidMapper.updateById(bid);

        // 拒绝其他投标
        bidMapper.selectList(new LambdaQueryWrapper<Bid>()
                .eq(Bid::getTaskId, task.getId())
                .eq(Bid::getStatus, "PENDING")
                .ne(Bid::getId, bidId)
        ).forEach(b -> {
            b.setStatus("REJECTED");
            bidMapper.updateById(b);
        });

        refundPublishDepositToWallet(task, "选标立项，退还发布押金");

        // 合同金额从可用余额划入冻结资金，供里程碑验收时划转给开发者（与 WalletService.milestonePay 对应）
        walletService.getOrCreate(enterpriseId);
        BigDecimal escrowAmount = bid.getAmount();
        if (escrowAmount != null && escrowAmount.compareTo(BigDecimal.ZERO) > 0) {
            walletService.freezeForProject(enterpriseId, escrowAmount,
                    "选标立项托管-项目#" + project.getId() + "「" + task.getTitle() + "」");
        }

        task.setStatus("IN_PROGRESS");
        taskMapper.updateById(task);

        User developer = userMapper.selectById(bid.getDeveloperId());
        notificationService.send(bid.getDeveloperId(), "PROJECT",
                "恭喜！您的投标已被选中，项目「" + task.getTitle() + "」已开始", "/projects/" + project.getId());
        return project;
    }

    // 管理员审核
    @Transactional
    public void auditTask(Long taskId, String action, String reason) {
        if (!SecurityUtil.isAdmin()) throw new BusinessException(403, "无权限");
        String lockKey = "task:state:" + taskId;
        String lockToken = redisLockService.tryLockOrThrow(lockKey);
        try {
            doAuditTask(taskId, action, reason);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private void doAuditTask(Long taskId, String action, String reason) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        if (!"AUDITING".equals(task.getStatus())) throw new BusinessException("任务不在审核中");

        if ("APPROVE".equals(action)) {
            task.setStatus("PUBLISHED");
            notificationService.send(task.getCompanyId(), "AUDIT", "您的任务「" + task.getTitle() + "」审核通过，已发布", "/tasks/" + taskId);
        } else {
            refundPublishDepositToWallet(task, "审核未通过，退还发布押金");
            task.setStatus("REJECTED");
            task.setRejectReason(reason);
            notificationService.send(task.getCompanyId(), "AUDIT", "您的任务「" + task.getTitle() + "」审核未通过，原因：" + reason, "/tasks/manage");
        }
        taskMapper.updateById(task);
    }

    public PageResult<Task> adminListAuditing(int page, int size) {
        Page<Task> p = new Page<>(page, size);
        return PageResult.of(taskMapper.selectPage(p,
                new LambdaQueryWrapper<Task>().eq(Task::getStatus, "AUDITING").orderByAsc(Task::getCreatedAt)));
    }

    public Map<String, String> uploadAttachment(MultipartFile file) throws IOException {
        if (!SecurityUtil.isEnterprise()) throw new BusinessException(403, "仅企业用户可上传附件");
        java.nio.file.Path baseDir = java.nio.file.Paths.get(uploadPath).toAbsolutePath().normalize();
        java.nio.file.Path targetDir = baseDir.resolve("task-attachments");
        java.nio.file.Files.createDirectories(targetDir);
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : "";
        String filename = UUID.randomUUID() + ext;
        java.nio.file.Path dest = targetDir.resolve(filename);
        file.transferTo(dest.toFile());
        String url = urlPrefix + "/task-attachments/" + filename;
        Map<String, String> result = new HashMap<>();
        result.put("name", originalName);
        result.put("url", url);
        return result;
    }

    private void checkOwner(Task task) {
        if (!task.getCompanyId().equals(SecurityUtil.getCurrentUserId())) {
            throw new BusinessException(403, "无权操作此任务");
        }
    }

    /** 企业端预览：按当前系统配置的押金比例计算金额（基于预算上限） */
    public Map<String, Object> publishDepositPreview(BigDecimal budgetMax) {
        BigDecimal rate = getPublishDepositRate();
        BigDecimal amount = computePublishDepositAmount(budgetMax);
        return Map.of(
                "rate", rate,
                "amount", amount,
                "budgetMax", budgetMax != null ? budgetMax : BigDecimal.ZERO
        );
    }

    public BigDecimal getPublishDepositRate() {
        SystemConfig c = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, "task_publish_deposit_rate"));
        if (c == null || c.getConfigValue() == null || c.getConfigValue().isBlank()) {
            return new BigDecimal("0.05");
        }
        try {
            BigDecimal r = new BigDecimal(c.getConfigValue().trim());
            if (r.compareTo(BigDecimal.ZERO) < 0 || r.compareTo(BigDecimal.ONE) > 0) {
                return new BigDecimal("0.05");
            }
            return r;
        } catch (Exception e) {
            return new BigDecimal("0.05");
        }
    }

    public BigDecimal computePublishDepositAmount(BigDecimal budgetMax) {
        if (budgetMax == null || budgetMax.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal raw = budgetMax.multiply(getPublishDepositRate()).setScale(2, RoundingMode.HALF_UP);
        if (raw.compareTo(BigDecimal.ZERO) > 0 && raw.compareTo(new BigDecimal("0.01")) < 0) {
            return new BigDecimal("0.01");
        }
        return raw.max(BigDecimal.ZERO);
    }

    private BigDecimal computePublishDepositForTask(Task task) {
        return computePublishDepositAmount(task.getBudgetMax());
    }

    /**
     * 首次/再次从草稿或驳回提交审核时，按预算上限与配置比例扣除钱包可用余额作为发布押金。
     */
    private void maybeChargePublishDeposit(Task task, String previousStatus) {
        if (!"AUDITING".equals(task.getStatus())) return;
        if (!List.of("DRAFT", "REJECTED").contains(previousStatus)) return;
        BigDecimal amount = computePublishDepositForTask(task);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            task.setPublishDepositAmount(BigDecimal.ZERO);
            taskMapper.updateById(task);
            return;
        }
        walletService.deductPublishDeposit(task.getCompanyId(), amount, task.getId());
        task.setPublishDepositAmount(amount);
        taskMapper.updateById(task);
    }

    /** 将已记录的发布押金退回企业钱包（不单独写库，由调用方 update 任务） */
    private void refundPublishDepositToWallet(Task task, String reason) {
        BigDecimal held = task.getPublishDepositAmount();
        if (held == null || held.compareTo(BigDecimal.ZERO) <= 0) return;
        walletService.refundPublishDeposit(task.getCompanyId(), held, task.getId(), reason);
        task.setPublishDepositAmount(BigDecimal.ZERO);
    }

    /** 付款方式与里程碑规划方默认值、合法性 */
    private void normalizeTaskDeliverySettings(Task task) {
        if (task.getPaymentType() == null || task.getPaymentType().isBlank()) {
            task.setPaymentType("MILESTONE");
        }
        if ("ONCE".equals(task.getPaymentType())) {
            task.setMilestonePlanBy("DEVELOPER");
            return;
        }
        if (task.getMilestonePlanBy() == null || task.getMilestonePlanBy().isBlank()) {
            task.setMilestonePlanBy("DEVELOPER");
        }
        if (!List.of("DEVELOPER", "ENTERPRISE").contains(task.getMilestonePlanBy())) {
            task.setMilestonePlanBy("DEVELOPER");
        }
    }
}
