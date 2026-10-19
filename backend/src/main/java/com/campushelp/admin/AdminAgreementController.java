// 协议后台接口。
package com.campushelp.admin;

import com.campushelp.audit.AdminAuditService;
import com.campushelp.model.Agreement;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/agreements")
public class AdminAgreementController {
    private final AdminAgreementService agreements;
    private final AdminAuditService audit;

    public AdminAgreementController(AdminAgreementService agreements, AdminAuditService audit) {
        this.agreements = agreements;
        this.audit = audit;
    }

    @GetMapping
    public List<Agreement> list() {
        return agreements.list();
    }

    @PostMapping
    public Agreement create(HttpServletRequest request, @RequestBody Agreement agreement) {
        agreement.setId(null);
        Agreement saved = agreements.save(agreement);
        audit.record(request, "AGREEMENT_CREATE", "agreement", saved.getId(), "创建协议：" + saved.getTitle());
        return saved;
    }

    @PutMapping("/{id}")
    public Agreement update(HttpServletRequest request, @PathVariable Long id, @RequestBody Agreement agreement) {
        agreement.setId(id);
        Agreement saved = agreements.save(agreement);
        audit.record(request, "AGREEMENT_UPDATE", "agreement", id, "更新协议：" + saved.getTitle());
        return saved;
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(HttpServletRequest request, @PathVariable Long id) {
        agreements.delete(id);
        audit.record(request, "AGREEMENT_DELETE", "agreement", id, "删除协议 #" + id);
        return Map.of("message", "协议已删除");
    }
}
