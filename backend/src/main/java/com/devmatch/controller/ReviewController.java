package com.devmatch.controller;

import com.devmatch.common.R;
import com.devmatch.dto.ReviewRequest;
import com.devmatch.entity.Review;
import com.devmatch.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "评价接口")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "提交评价")
    @PostMapping
    public R<Review> submit(@RequestBody @Valid ReviewRequest req) {
        return R.ok(reviewService.submitReview(req));
    }

    @Operation(summary = "获取信用信息")
    @GetMapping("/credit")
    public R<Map<String, Object>> creditInfo(@RequestParam(required = false) Long userId) {
        return R.ok(reviewService.getCreditInfo(userId));
    }
}
