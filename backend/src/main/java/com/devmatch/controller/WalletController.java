package com.devmatch.controller;

import com.devmatch.common.PageResult;
import com.devmatch.common.R;
import com.devmatch.entity.PayOrder;
import com.devmatch.entity.Wallet;
import com.devmatch.entity.WalletTransaction;
import com.devmatch.entity.WithdrawRecord;
import com.devmatch.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "钱包接口")
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // 前端调用: GET /wallet/balance
    @Operation(summary = "获取钱包余额")
    @GetMapping("/balance")
    public R<Wallet> getBalance() {
        return R.ok(walletService.getMyWallet());
    }

    // 兼容旧路径: GET /wallet
    @GetMapping
    public R<Wallet> getWallet() {
        return R.ok(walletService.getMyWallet());
    }

    @Operation(summary = "充值")
    @PostMapping("/recharge")
    public R<PayOrder> recharge(@RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String payMethod = (String) body.getOrDefault("payMethod", "ALIPAY");
        return R.ok(walletService.recharge(amount, payMethod));
    }

    @Operation(summary = "申请提现")
    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('DEVELOPER')")
    public R<WithdrawRecord> withdraw(@RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        return R.ok(walletService.withdraw(
                amount,
                (String) body.get("method"),
                (String) body.get("account"),
                (String) body.get("realName")
        ));
    }

    // 前端调用: GET /wallet/withdrawals（当前用户的提现记录）
    @Operation(summary = "我的提现记录")
    @GetMapping("/withdrawals")
    public R<PageResult<WithdrawRecord>> myWithdrawals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(walletService.myWithdrawals(page, size));
    }

    // 前端调用: GET /wallet/orders（充值订单）
    @Operation(summary = "充值订单记录")
    @GetMapping("/orders")
    public R<PageResult<WalletTransaction>> orders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(walletService.myTransactions(page, size, "RECHARGE"));
    }

    @Operation(summary = "流水记录")
    @GetMapping("/transactions")
    public R<PageResult<WalletTransaction>> transactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        return R.ok(walletService.myTransactions(page, size, type));
    }

    // ==================== 管理员 ====================

    // 前端调用: GET /admin/withdrawals?status=
    // 由 AdminWalletController 处理，此处保留兼容
    @Operation(summary = "管理员-提现列表")
    @GetMapping("/admin/withdraws")
    @PreAuthorize("hasRole('ADMIN')")
    public R<PageResult<WithdrawRecord>> adminWithdraws(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return R.ok(walletService.adminListWithdraws(page, size, status));
    }

    @Operation(summary = "管理员-审核提现")
    @PostMapping("/admin/withdraws/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> auditWithdraw(@PathVariable Long id, @RequestBody Map<String, String> body) {
        walletService.auditWithdraw(id, body.get("action"), body.get("remark"));
        return R.ok();
    }
}
