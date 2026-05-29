package com.devmatch.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devmatch.entity.Task;
import com.devmatch.mapper.TaskMapper;
import com.devmatch.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 将已过接单截止日且仍在招募中的任务标记为已过期，并通知企业。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExpiryScheduler {

    private final TaskMapper taskMapper;
    private final NotificationService notificationService;

    /** 每小时检查一次，避免跨天后长时间仍显示为「招募中」 */
    @Scheduled(cron = "0 0 * * * ?")
    public void markExpiredPublishedTasks() {
        LocalDate today = LocalDate.now();
        List<Task> list = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, "PUBLISHED")
                        .eq(Task::getDeleted, 0)
                        .isNotNull(Task::getDeadline)
                        .lt(Task::getDeadline, today));
        if (list.isEmpty()) {
            return;
        }
        for (Task t : list) {
            t.setStatus("EXPIRED");
            taskMapper.updateById(t);
            notificationService.send(
                    t.getCompanyId(),
                    "TASK",
                    "您的需求「" + t.getTitle() + "」已超过接单截止日期，任务已过期，开发者将无法继续投标。",
                    "/enterprise/tasks");
        }
        log.info("Marked {} published task(s) as EXPIRED", list.size());
    }
}
