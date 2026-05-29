package com.devmatch.service;

import com.devmatch.common.exception.BusinessException;
import com.devmatch.dto.ReviewRequest;
import com.devmatch.entity.Project;
import com.devmatch.entity.Review;
import com.devmatch.entity.User;
import com.devmatch.mapper.ProjectMapper;
import com.devmatch.mapper.ReviewMapper;
import com.devmatch.mapper.UserMapper;
import com.devmatch.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Transactional
    public Review submitReview(ReviewRequest req) {
        Long reviewerId = SecurityUtil.getCurrentUserId();
        Project project = projectMapper.selectById(req.getProjectId());
        if (project == null) throw new BusinessException("项目不存在");

        if (!project.getDeveloperId().equals(reviewerId) && !project.getEnterpriseId().equals(reviewerId)) {
            throw new BusinessException(403, "您不是该项目的参与方");
        }
        if (!"COMPLETED".equals(project.getStatus())) {
            throw new BusinessException("项目未完成，不可评价");
        }
        if (reviewMapper.countByProjectAndReviewer(req.getProjectId(), reviewerId) > 0) {
            throw new BusinessException("您已对该项目提交过评价");
        }

        Long revieweeId = reviewerId.equals(project.getDeveloperId())
                ? project.getEnterpriseId()
                : project.getDeveloperId();

        Review review = new Review();
        review.setProjectId(req.getProjectId());
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setRating(req.getRating());
        review.setContent(req.getContent());
        review.setTags(req.getTags());
        reviewMapper.insert(review);

        // 更新被评价方信用分
        updateCreditScore(revieweeId);

        notificationService.send(revieweeId, "PROJECT",
                "您收到了一条新的评价", "/credit");
        return review;
    }

    public Map<String, Object> getCreditInfo(Long userId) {
        if (userId == null) userId = SecurityUtil.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        BigDecimal avgRating = reviewMapper.getAvgRating(userId);
        int goodCount = reviewMapper.countGoodReviews(userId);
        List<Review> rawReviews = reviewMapper.findReceivedReviews(userId, 50);

        // 关联评价人信息
        List<Map<String, Object>> reviews = new ArrayList<>();
        for (Review r : rawReviews) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("projectId", r.getProjectId());
            item.put("rating", r.getRating());
            item.put("content", r.getContent());
            item.put("tags", r.getTags());
            item.put("createdAt", r.getCreatedAt());
            User reviewer = userMapper.selectById(r.getReviewerId());
            if (reviewer != null) {
                Map<String, Object> reviewerInfo = new HashMap<>();
                reviewerInfo.put("id", reviewer.getId());
                reviewerInfo.put("name", reviewer.getNickname() != null ? reviewer.getNickname() : reviewer.getUsername());
                reviewerInfo.put("avatar", reviewer.getAvatar());
                item.put("reviewer", reviewerInfo);
            }
            reviews.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("creditScore", user.getCreditScore());
        result.put("avgRating", avgRating);
        result.put("goodReviewCount", goodCount);
        result.put("reviews", reviews);
        return result;
    }

    private void updateCreditScore(Long userId) {
        BigDecimal avgRating = reviewMapper.getAvgRating(userId);
        if (avgRating == null) return;

        // 信用分 = 基础60 + 评分转换40分
        int score = 60 + avgRating.multiply(BigDecimal.valueOf(8)).setScale(0, RoundingMode.HALF_UP).intValue();
        score = Math.min(100, Math.max(0, score));

        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setCreditScore(score);
            userMapper.updateById(user);
        }
    }
}
