package com.devmatch.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devmatch.common.PageResult;
import com.devmatch.common.exception.BusinessException;
import com.devmatch.entity.*;
import com.devmatch.mapper.*;
import com.devmatch.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletMapper walletMapper;
    private final WalletTransactionMapper transactionMapper;
    private final PayOrderMapper payOrderMapper;
    private final WithdrawRecordMapper withdrawRecordMapper;
    private final RedisLockService redisLockService;

    @Value("${platform.commission-rate:0.05}")
    private BigDecimal commissionRate;

    public Wallet getMyWallet() {
        Long userId = SecurityUtil.getCurrentUserId();
        Wallet w = getOrCreate(userId);
        BigDecimal mi = transactionMapper.getMonthlyIncome(userId);
        w.setMonthlyIncome(mi != null ? mi : BigDecimal.ZERO);
        BigDecimal me = transactionMapper.getMonthlyExpense(userId);
        w.setMonthlyExpense(me != null ? me : BigDecimal.ZERO);
        return w;
    }

    public Wallet getOrCreate(Long userId) {
        Wallet wallet = walletMapper.findByUserId(userId);
        if (wallet == null) {
            wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setFrozen(BigDecimal.ZERO);
            wallet.setTotalIncome(BigDecimal.ZERO);
            wallet.setTotalExpense(BigDecimal.ZERO);
            walletMapper.insert(wallet);
        }
        return wallet;
    }

    @Transactional
    public PayOrder recharge(BigDecimal amount, String payMethod) {
        if (amount.compareTo(BigDecimal.ONE) < 0) throw new BusinessException("充值金额不能小于1元");
        Long userId = SecurityUtil.getCurrentUserId();
        String lockKey = walletLockKey(userId);
        String lockToken = redisLockService.tryLockOrThrow(lockKey);
        try {
            PayOrder order = new PayOrder();
            order.setOrderNo(IdUtil.fastSimpleUUID());
            order.setUserId(userId);
            order.setAmount(amount);
            order.setPayMethod(payMethod);
            order.setStatus("PENDING");
            payOrderMapper.insert(order);

            // 模拟支付：直接成功
            order.setStatus("SUCCESS");
            payOrderMapper.updateById(order);
            doRechargeWithoutLock(userId, amount, "充值-" + payMethod, order.getOrderNo());
            return order;
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    @Transactional
    public void doRecharge(Long userId, BigDecimal amount, String desc, String refId) {
        String lockKey = walletLockKey(userId);
        String lockToken = redisLockService.tryLockOrThrow(lockKey);
        try {
            doRechargeWithoutLock(userId, amount, desc, refId);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private void doRechargeWithoutLock(Long userId, BigDecimal amount, String desc, String refId) {
        int updated = walletMapper.addBalance(userId, amount);
        if (updated == 0) throw new BusinessException("余额更新失败");
        Wallet wallet = walletMapper.findByUserId(userId);
        recordTransaction(userId, "RECHARGE", amount, wallet.getBalance(), desc, refId, "PAY_ORDER");
    }

    @Transactional
    public WithdrawRecord withdraw(BigDecimal amount, String method, String account, String realName) {
        Long userId = SecurityUtil.getCurrentUserId();
        String lockKey = walletLockKey(userId);
        String lockToken = redisLockService.tryLockOrThrow(lockKey);
        try {
            return doWithdraw(userId, amount, method, account, realName);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private WithdrawRecord doWithdraw(Long userId, BigDecimal amount, String method, String account, String realName) {
        Wallet wallet = getOrCreate(userId);
        if (wallet.getBalance().compareTo(amount) < 0) throw new BusinessException("余额不足");

        walletMapper.addBalance(userId, amount.negate());
        wallet = walletMapper.findByUserId(userId);
        recordTransaction(userId, "WITHDRAW", amount.negate(), wallet.getBalance(), "申请提现", null, null);

        WithdrawRecord record = new WithdrawRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setMethod(method);
        record.setAccount(account);
        record.setRealName(realName);
        record.setStatus("PENDING");
        withdrawRecordMapper.insert(record);
        return record;
    }

    @Transactional
    public void milestonePay(Project project, Milestone milestone) {
        String lockKey = "wallet:milestone-pay:" + milestone.getId();
        String lockToken = redisLockService.tryLockOrThrow(lockKey);
        try {
            doMilestonePay(project, milestone);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private void doMilestonePay(Project project, Milestone milestone) {
        BigDecimal totalAmount = milestone.getAmount();
        BigDecimal commission = totalAmount.multiply(commissionRate);
        BigDecimal developerAmount = totalAmount.subtract(commission);

        // 企业冻结金额扣除
        int r = walletMapper.deductFrozen(project.getEnterpriseId(), totalAmount);
        if (r == 0) throw new BusinessException("企业冻结金额不足，无法完成付款");

        // 平台手续费（此处仅记录流水，实际可转到平台账户）
        Wallet entWallet = walletMapper.findByUserId(project.getEnterpriseId());
        recordTransaction(project.getEnterpriseId(), "EXPENSE", totalAmount.negate(),
                entWallet.getBalance(), "里程碑付款-" + milestone.getName(),
                String.valueOf(milestone.getId()), "MILESTONE");

        // 开发者到账
        walletMapper.addBalance(project.getDeveloperId(), developerAmount);
        Wallet devWallet = walletMapper.findByUserId(project.getDeveloperId());
        recordTransaction(project.getDeveloperId(), "INCOME", developerAmount,
                devWallet.getBalance(), "里程碑收款-" + milestone.getName(),
                String.valueOf(milestone.getId()), "MILESTONE");
    }

    @Transactional
    public void freezeForProject(Long enterpriseId, BigDecimal amount, String desc) {
        String lockKey = walletLockKey(enterpriseId);
        String lockToken = redisLockService.tryLockOrThrow(lockKey);
        try {
            doFreezeForProject(enterpriseId, amount, desc);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private void doFreezeForProject(Long enterpriseId, BigDecimal amount, String desc) {
        int r = walletMapper.freezeAmount(enterpriseId, amount);
        if (r == 0) throw new BusinessException("余额不足，无法冻结");
        Wallet wallet = walletMapper.findByUserId(enterpriseId);
        recordTransaction(enterpriseId, "FREEZE", amount.negate(), wallet.getBalance(), desc, null, null);
    }

    /** 从可用余额扣除需求发布押金 */
    @Transactional
    public void deductPublishDeposit(Long userId, BigDecimal amount, Long taskId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        String lockKey = walletLockKey(userId);
        String lockToken = redisLockService.tryLockOrThrow(lockKey);
        try {
            doDeductPublishDeposit(userId, amount, taskId);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private void doDeductPublishDeposit(Long userId, BigDecimal amount, Long taskId) {
        getOrCreate(userId);
        int n = walletMapper.addBalance(userId, amount.negate());
        if (n == 0) {
            throw new BusinessException("钱包可用余额不足，无法支付发布押金（需 ¥" + amount.stripTrailingZeros().toPlainString() + "）");
        }
        Wallet w = walletMapper.findByUserId(userId);
        recordTransaction(userId, "EXPENSE", amount.negate(), w.getBalance(),
                "需求发布押金", String.valueOf(taskId), "TASK_DEPOSIT");
    }

    /** 退还需求发布押金至可用余额 */
    @Transactional
    public void refundPublishDeposit(Long userId, BigDecimal amount, Long taskId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        String lockKey = walletLockKey(userId);
        String lockToken = redisLockService.tryLockOrThrow(lockKey);
        try {
            doRefundPublishDeposit(userId, amount, taskId, description);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private void doRefundPublishDeposit(Long userId, BigDecimal amount, Long taskId, String description) {
        getOrCreate(userId);
        walletMapper.addBalance(userId, amount);
        Wallet w = walletMapper.findByUserId(userId);
        recordTransaction(userId, "INCOME", amount, w.getBalance(),
                description != null ? description : "需求发布押金退还",
                String.valueOf(taskId), "TASK_DEPOSIT_REFUND");
    }

    public PageResult<WithdrawRecord> myWithdrawals(int page, int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        Page<WithdrawRecord> p = new Page<>(page, size);
        return PageResult.of(withdrawRecordMapper.selectPage(p,
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WithdrawRecord>()
                        .eq(WithdrawRecord::getUserId, userId)
                        .orderByDesc(WithdrawRecord::getCreatedAt)));
    }

    public PageResult<WalletTransaction> myTransactions(int page, int size, String type) {
        Page<WalletTransaction> p = new Page<>(page, size);
        return PageResult.of(transactionMapper.findByUserId(p, SecurityUtil.getCurrentUserId(), type));
    }

    public PageResult<WithdrawRecord> adminListWithdraws(int page, int size, String status) {
        Page<WithdrawRecord> p = new Page<>(page, size);
        return PageResult.of(withdrawRecordMapper.findAll(p, status));
    }

    @Transactional
    public void auditWithdraw(Long id, String action, String remark) {
        String lockKey = "withdraw:audit:" + id;
        String lockToken = redisLockService.tryLockOrThrow(lockKey);
        try {
            doAuditWithdraw(id, action, remark);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private void doAuditWithdraw(Long id, String action, String remark) {
        WithdrawRecord record = withdrawRecordMapper.selectById(id);
        if (record == null) throw new BusinessException("提现记录不存在");
        if (!"PENDING".equals(record.getStatus())) throw new BusinessException("该记录已处理");

        record.setAuditorId(SecurityUtil.getCurrentUserId());
        record.setRemark(remark);

        if ("APPROVE".equals(action)) {
            record.setStatus("COMPLETED");
        } else {
            record.setStatus("REJECTED");
            // 退款
            walletMapper.addBalance(record.getUserId(), record.getAmount());
            Wallet wallet = walletMapper.findByUserId(record.getUserId());
            recordTransaction(record.getUserId(), "INCOME", record.getAmount(),
                    wallet.getBalance(), "提现驳回退款", String.valueOf(id), "WITHDRAW");
        }
        withdrawRecordMapper.updateById(record);
    }

    private String walletLockKey(Long userId) {
        return "wallet:" + userId;
    }

    private void recordTransaction(Long userId, String type, BigDecimal amount,
                                   BigDecimal balance, String desc, String refId, String refType) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalance(balance);
        tx.setDescription(desc);
        tx.setRefId(refId);
        tx.setRefType(refType);
        tx.setStatus("SUCCESS");
        transactionMapper.insert(tx);
    }
}
