package com.devmatch.controller;

import com.devmatch.common.PageResult;
import com.devmatch.common.R;
import com.devmatch.entity.WithdrawRecord;
import com.devmatch.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 前端调用路径: /admin/withdrawals
 */
@Tag(name = "管理员-钱包管理")
@RestController
@RequestMapping("/api/admin/withdrawals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWalletController {

    private final WalletService walletService;

    @Operation(summary = "提现申请列表")
    @GetMapping
    public R<PageResult<WithdrawRecord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return R.ok(walletService.adminListWithdraws(page, size, status));
    }

    // 前端调用: POST /admin/withdrawals/{id}/audit
    @Operation(summary = "审核提现")
    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id, @RequestBody Map<String, String> body) {
        walletService.auditWithdraw(id, body.get("action"), body.get("remark"));
        return R.ok();
    }
}
