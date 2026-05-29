package com.devmatch.service;



import com.devmatch.common.exception.BusinessException;

import com.devmatch.dto.MilestoneRequest;

import com.devmatch.entity.*;

import com.devmatch.mapper.*;

import com.devmatch.security.SecurityUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.BeanUtils;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.io.FileSystemResource;

import org.springframework.core.io.Resource;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;



import java.io.IOException;

import java.nio.file.Files;

import java.nio.file.Path;

import java.nio.file.Paths;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.UUID;



@Service

@RequiredArgsConstructor

public class ProjectService {

    /** 供交付物下载：资源、下载显示文件名、字节长度（便于客户端 Content-Length） */
    public record DeliverableFile(Resource resource, String downloadFilename, long contentLength) {}



    private final ProjectMapper projectMapper;

    private final MilestoneMapper milestoneMapper;

    private final DeliverableMapper deliverableMapper;

    private final UserMapper userMapper;

    private final WalletService walletService;

    private final NotificationService notificationService;

    private final RedisLockService redisLockService;



    @Value("${upload.path:./uploads}")

    private String uploadPath;



    @Value("${upload.url-prefix:/uploads}")

    private String urlPrefix;



    /**
     * 我的项目列表（企业端需展示开发者头像/名称，开发者端需展示企业方，与详情页字段对齐）。
     */
    public List<Map<String, Object>> myProjects(String status) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Project> raw;
        if (SecurityUtil.isDeveloper()) {
            raw = projectMapper.findByDeveloperId(userId, status);
        } else {
            raw = projectMapper.findByEnterpriseId(userId, status);
        }
        List<Map<String, Object>> out = new ArrayList<>(raw.size());
        for (Project p : raw) {
            out.add(projectToListRow(p));
        }
        return out;
    }

    private Map<String, Object> projectToListRow(Project p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("taskId", p.getTaskId());
        m.put("bidId", p.getBidId());
        m.put("name", p.getName());
        m.put("developerId", p.getDeveloperId());
        m.put("enterpriseId", p.getEnterpriseId());
        m.put("amount", p.getAmount());
        m.put("paymentType", p.getPaymentType());
        m.put("milestonePlanBy", p.getMilestonePlanBy());
        m.put("status", p.getStatus());
        m.put("progress", p.getProgress());
        m.put("startDate", p.getStartDate());
        m.put("endDate", p.getEndDate());
        m.put("createdAt", p.getCreatedAt());
        m.put("updatedAt", p.getUpdatedAt());
        if (p.getDeveloperId() != null) {
            m.put("developer", userPartyCard(userMapper.selectById(p.getDeveloperId()), true));
        } else {
            m.put("developer", null);
        }
        if (p.getEnterpriseId() != null) {
            m.put("enterprise", userPartyCard(userMapper.selectById(p.getEnterpriseId()), false));
        } else {
            m.put("enterprise", null);
        }
        return m;
    }

    /** 列表卡片展示：统一提供 name、avatar（User 实体无 name 字段） */
    private Map<String, Object> userPartyCard(User u, boolean developerParty) {
        if (u == null) {
            return null;
        }
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", u.getId());
        card.put("avatar", u.getAvatar());
        String displayName;
        if (developerParty) {
            displayName = (u.getNickname() != null && !u.getNickname().isBlank())
                    ? u.getNickname()
                    : (u.getUsername() != null ? u.getUsername() : "");
        } else {
            if (u.getCompanyName() != null && !u.getCompanyName().isBlank()) {
                displayName = u.getCompanyName();
            } else if (u.getNickname() != null && !u.getNickname().isBlank()) {
                displayName = u.getNickname();
            } else {
                displayName = u.getUsername() != null ? u.getUsername() : "";
            }
        }
        card.put("name", displayName);
        return card;
    }



    public Map<String, Object> detail(Long id) {

        Project project = getProject(id);

        checkMember(project);

        List<Milestone> milestones = milestoneMapper.findByProjectId(id);

        attachDeliverables(milestones);

        User developer = userMapper.selectById(project.getDeveloperId());

        User enterprise = userMapper.selectById(project.getEnterpriseId());

        return Map.of(

                "project", project,

                "milestones", milestones,

                "developer", developer,

                "enterprise", enterprise

        );

    }



    // --- 里程碑 ---

    public List<Milestone> getMilestones(Long projectId) {

        checkMember(getProject(projectId));

        List<Milestone> list = milestoneMapper.findByProjectId(projectId);

        attachDeliverables(list);

        return list;

    }



    private void attachDeliverables(List<Milestone> milestones) {

        for (Milestone m : milestones) {

            m.setDeliverables(deliverableMapper.findByMilestoneId(m.getId()));

        }

    }



    private String effectivePaymentType(Project project) {

        String pt = project.getPaymentType();

        return (pt == null || pt.isBlank()) ? "MILESTONE" : pt;

    }



    private boolean enterprisePlansMilestones(Project project) {

        return "ENTERPRISE".equals(project.getMilestonePlanBy())

                && "MILESTONE".equals(effectivePaymentType(project));

    }



    private void assertMilestoneStructureMutable(Project project) {

        if ("ONCE".equals(effectivePaymentType(project))) {

            throw new BusinessException("一次性付款项目已固定为单一交付节点，不可增删改里程碑");

        }

    }



    /** 里程碑规划：企业规划时仅企业可改结构；开发者拆分则仅开发者可改 */

    private void checkMilestonePlanner(Project project) {

        if (enterprisePlansMilestones(project)) {

            checkEnterprise(project);

        } else {

            checkDeveloper(project);

        }

    }



    @Transactional

    public Milestone createMilestone(Long projectId, MilestoneRequest req) {

        Project project = getProject(projectId);

        assertMilestoneStructureMutable(project);

        checkMilestonePlanner(project);

        Milestone m = new Milestone();

        BeanUtils.copyProperties(req, m);

        m.setProjectId(projectId);

        m.setStatus("PENDING");

        if (m.getSortOrder() == null) {

            m.setSortOrder(0);

        }

        milestoneMapper.insert(m);

        Long notifyId = enterprisePlansMilestones(project) ? project.getDeveloperId() : project.getEnterpriseId();

        notificationService.send(notifyId, "PROJECT",

                "项目「" + project.getName() + "」新增里程碑「" + m.getName() + "」",

                "/projects/" + projectId);

        return m;

    }



    @Transactional

    public Milestone updateMilestone(Long milestoneId, MilestoneRequest req) {

        Milestone m = getMilestone(milestoneId);

        Project project = getProject(m.getProjectId());

        assertMilestoneStructureMutable(project);

        checkMilestonePlanner(project);

        if (!List.of("PENDING", "IN_PROGRESS").contains(m.getStatus())) {

            throw new BusinessException("该状态下不允许编辑");

        }

        BeanUtils.copyProperties(req, m, "id", "projectId", "status");

        milestoneMapper.updateById(m);

        return m;

    }



    @Transactional

    public void deleteMilestone(Long milestoneId) {

        Milestone m = getMilestone(milestoneId);

        Project project = getProject(m.getProjectId());

        assertMilestoneStructureMutable(project);

        checkMilestonePlanner(project);

        if (!"PENDING".equals(m.getStatus())) {

            throw new BusinessException("已开始的里程碑不允许删除");

        }

        milestoneMapper.deleteById(milestoneId);

    }



    /** 开发者将「待开始」里程碑置为进行中，便于按阶段推进 */

    @Transactional

    public void startMilestone(Long milestoneId) {

        Milestone m = getMilestone(milestoneId);

        Project project = getProject(m.getProjectId());

        checkDeveloper(project);

        if (!"PENDING".equals(m.getStatus())) {

            throw new BusinessException("仅待开始的里程碑可开始");

        }

        m.setStatus("IN_PROGRESS");

        milestoneMapper.updateById(m);

    }



    @Transactional

    public void submitMilestone(Long milestoneId) {

        Milestone m = getMilestone(milestoneId);

        Project project = getProject(m.getProjectId());

        checkDeveloper(project);

        if (!"IN_PROGRESS".equals(m.getStatus())) {

            throw new BusinessException("请先开始里程碑并上传交付物后再提交验收");

        }

        int dc = deliverableMapper.findByMilestoneId(milestoneId).size();

        if (dc == 0) {

            throw new BusinessException("请至少上传一个交付物后再提交验收");

        }

        m.setStatus("SUBMITTED");

        milestoneMapper.updateById(m);



        String suffix = "ONCE".equals(effectivePaymentType(project))

                ? "（整单一次性验收）"

                : "";

        notificationService.send(project.getEnterpriseId(), "PROJECT",

                "里程碑「" + m.getName() + "」已提交验收" + suffix + "，请及时审核", "/projects/" + m.getProjectId());

    }



    @Transactional

    public void acceptMilestone(Long milestoneId) {

        String lockKey = "milestone:accept:" + milestoneId;

        String lockToken = redisLockService.tryLockOrThrow(lockKey);

        try {

            doAcceptMilestone(milestoneId);

        } finally {

            redisLockService.unlock(lockKey, lockToken);

        }

    }



    private void doAcceptMilestone(Long milestoneId) {

        Milestone m = getMilestone(milestoneId);

        Project project = getProject(m.getProjectId());

        checkEnterprise(project);

        if (!"SUBMITTED".equals(m.getStatus())) throw new BusinessException("里程碑未提交");



        m.setStatus("ACCEPTED");

        m.setAcceptedAt(LocalDateTime.now());

        milestoneMapper.updateById(m);



        walletService.milestonePay(project, m);



        int accepted = milestoneMapper.countAccepted(project.getId());

        int total = milestoneMapper.countTotal(project.getId());

        int progress = total > 0 ? (int) (accepted * 100.0 / total) : 0;

        projectMapper.updateProgress(project.getId(), progress);



        if (accepted == total && total > 0) {

            project.setStatus("COMPLETED");

            project.setProgress(100);

            projectMapper.updateById(project);

            notificationService.send(project.getDeveloperId(), "PROJECT",

                    "项目「" + project.getName() + "」已完成！", "/projects/" + project.getId());

        }



        String payHint = "ONCE".equals(effectivePaymentType(project))

                ? "整单验收款已到账"

                : "里程碑「" + m.getName() + "」已验收通过，款项已到账";

        notificationService.send(project.getDeveloperId(), "PAYMENT", payHint, "/wallet");

    }



    @Transactional

    public void rejectMilestone(Long milestoneId, String reason) {

        Milestone m = getMilestone(milestoneId);

        Project project = getProject(m.getProjectId());

        checkEnterprise(project);

        if (!"SUBMITTED".equals(m.getStatus())) throw new BusinessException("里程碑未提交");

        m.setStatus("IN_PROGRESS");

        m.setRejectReason(reason);

        milestoneMapper.updateById(m);



        notificationService.send(project.getDeveloperId(), "PROJECT",

                "里程碑「" + m.getName() + "」验收被驳回，原因：" + reason, "/projects/" + m.getProjectId());

    }



    // --- 交付物 ---

    @Transactional

    public Deliverable uploadDeliverable(Long milestoneId, MultipartFile file) throws IOException {

        Milestone m = getMilestone(milestoneId);

        Project project = getProject(m.getProjectId());

        checkDeveloper(project);

        if (!List.of("PENDING", "IN_PROGRESS").contains(m.getStatus())) {

            throw new BusinessException("当前里程碑状态不允许上传交付物");

        }



        Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();

        Path targetDir = baseDir.resolve("deliverables");

        Files.createDirectories(targetDir);

        String filename = UUID.randomUUID() + getExtension(file.getOriginalFilename());

        Path dest = targetDir.resolve(filename);

        file.transferTo(dest);



        Deliverable d = new Deliverable();

        d.setMilestoneId(milestoneId);

        d.setProjectId(m.getProjectId());

        d.setName(file.getOriginalFilename());

        d.setPath(urlPrefix + "/deliverables/" + filename);

        d.setSize(file.getSize());

        d.setUploaderId(SecurityUtil.getCurrentUserId());

        deliverableMapper.insert(d);

        return d;

    }



    public List<Deliverable> getDeliverables(Long milestoneId) {

        Milestone m = getMilestone(milestoneId);

        checkMember(getProject(m.getProjectId()));

        return deliverableMapper.findByMilestoneId(milestoneId);

    }



    /**
     * 项目成员下载交付物（磁盘文件与库中 path 一致：/uploads/deliverables/{uuid}.ext）
     */
    public DeliverableFile resolveDeliverableDownload(Long deliverableId) throws IOException {

        Deliverable d = deliverableMapper.selectById(deliverableId);

        if (d == null) throw new BusinessException("交付物不存在");

        Project project = getProject(d.getProjectId());

        checkMember(project);

        String storedPath = d.getPath();

        if (storedPath == null || !storedPath.contains("/")) {

            throw new BusinessException("文件路径无效");

        }

        String storedName = storedPath.substring(storedPath.lastIndexOf('/') + 1);

        Path base = Paths.get(uploadPath).toAbsolutePath().normalize();

        Path deliverDir = base.resolve("deliverables").normalize();

        Path file = deliverDir.resolve(storedName).normalize();

        if (!file.startsWith(deliverDir)) {

            throw new BusinessException("非法路径");

        }

        if (!Files.exists(file)) {

            throw new BusinessException("文件不存在或已删除");

        }

        long contentLength = Files.size(file);

        Resource resource = new FileSystemResource(file.toFile());

        String downloadName = (d.getName() != null && !d.getName().isBlank()) ? d.getName() : storedName;

        return new DeliverableFile(resource, downloadName, contentLength);

    }



    // --- Helper ---

    private Project getProject(Long id) {

        Project project = projectMapper.selectById(id);

        if (project == null) throw new BusinessException("项目不存在");

        return project;

    }



    private Milestone getMilestone(Long id) {

        Milestone m = milestoneMapper.selectById(id);

        if (m == null) throw new BusinessException("里程碑不存在");

        return m;

    }



    private void checkMember(Project project) {

        Long userId = SecurityUtil.getCurrentUserId();

        if (SecurityUtil.isAdmin()) return;

        if (!project.getDeveloperId().equals(userId) && !project.getEnterpriseId().equals(userId)) {

            throw new BusinessException(403, "无权访问此项目");

        }

    }



    private void checkDeveloper(Project project) {

        if (!project.getDeveloperId().equals(SecurityUtil.getCurrentUserId())) {

            throw new BusinessException(403, "仅项目开发者可操作");

        }

    }



    private void checkEnterprise(Project project) {

        if (!project.getEnterpriseId().equals(SecurityUtil.getCurrentUserId())) {

            throw new BusinessException(403, "仅项目企业方可操作");

        }

    }



    private String getExtension(String filename) {

        if (filename == null || !filename.contains(".")) return ".bin";

        return filename.substring(filename.lastIndexOf("."));

    }

}


