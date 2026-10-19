// 用户反馈后台业务：分页、详情和处理状态保存。
package com.campushelp.admin;

import com.campushelp.mapper.AdminFeedbackMapper;
import com.campushelp.model.Admin;
import com.campushelp.web.ApiException;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// 类/接口职责：后台查看用户反馈、标记处理并保存回复；不直接修改用户提交的原文。
public class AdminFeedbackService {
    private static final Set<String> STATUSES = Set.of("待处理", "已处理");
    private final AdminFeedbackMapper feedback;

    public AdminFeedbackService(AdminFeedbackMapper feedback) {
        this.feedback = feedback;
    }

    public record PageResult(int page, int size, long total, List<AdminFeedbackRow> items) {}

    public PageResult list(int page, int size, String status, String keyword) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        String normalizedStatus = status == null || status.isBlank() || "all".equals(status) ? null : status;
        if (normalizedStatus != null && !STATUSES.contains(normalizedStatus)) {
            throw bad("反馈状态筛选无效");
        }
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        long total = feedback.count(normalizedStatus, normalizedKeyword);
        List<AdminFeedbackRow> items = feedback.page(normalizedStatus, normalizedKeyword,
            (safePage - 1) * safeSize, safeSize);
        return new PageResult(safePage, safeSize, total, items);
    }

    public AdminFeedbackRow detail(Long id) {
        AdminFeedbackRow row = feedback.findById(id);
        if (row == null) throw new ApiException(HttpStatus.NOT_FOUND, "反馈不存在");
        return row;
    }

    @Transactional
    public AdminFeedbackRow handle(Long id, Admin admin, String status, String reply) {
        AdminFeedbackRow existing = detail(id);
        String normalizedStatus = status == null ? "待处理" : status.trim();
        if (!STATUSES.contains(normalizedStatus)) throw bad("反馈状态无效");
        String normalizedReply = reply == null ? null : reply.trim();
        if (normalizedReply != null && normalizedReply.length() > 2000) throw bad("回复不能超过 2000 字");
        feedback.saveHandling(id, normalizedStatus, normalizedReply == null || normalizedReply.isEmpty()
            ? null : normalizedReply, admin.getId());
        return detail(existing.getId());
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
