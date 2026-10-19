// 用户反馈后台接口。
package com.campushelp.admin;

import com.campushelp.audit.AdminAuditService;
import com.campushelp.model.Admin;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/feedback")
public class AdminFeedbackController {
    private final AdminFeedbackService feedback;
    private final AdminAuditService audit;

    public AdminFeedbackController(AdminFeedbackService feedback, AdminAuditService audit) {
        this.feedback = feedback;
        this.audit = audit;
    }

    public record HandleRequest(String status, String reply) {}

    @GetMapping
    public AdminFeedbackService.PageResult list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "all") String status,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return feedback.list(page, size, status, keyword);
    }

    @GetMapping("/{id}")
    public AdminFeedbackRow detail(@PathVariable Long id) {
        return feedback.detail(id);
    }

    @PutMapping("/{id}")
    public AdminFeedbackRow handle(HttpServletRequest request, @PathVariable Long id,
        @RequestBody HandleRequest body) {
        Admin admin = AdminAuthInterceptor.current(request);
        AdminFeedbackRow saved = feedback.handle(id, admin, body.status(), body.reply());
        audit.record(request, "FEEDBACK_HANDLE", "feedback", id, "反馈处理为：" + saved.getStatus());
        return saved;
    }
}
