// 协议后台业务；文本内容校验和保存。
package com.campushelp.admin;

import com.campushelp.mapper.AdminAgreementMapper;
import com.campushelp.model.Agreement;
import com.campushelp.web.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAgreementService {
    private final AdminAgreementMapper agreements;

    public AdminAgreementService(AdminAgreementMapper agreements) {
        this.agreements = agreements;
    }

    public List<Agreement> list() {
        return agreements.list();
    }

    @Transactional
    public Agreement save(Agreement agreement) {
        agreement.setCode(trim(agreement.getCode()));
        agreement.setTitle(trim(agreement.getTitle()));
        agreement.setVersion(trim(agreement.getVersion()));
        agreement.setContent(trim(agreement.getContent()));
        if (agreement.getCode() == null || !agreement.getCode().matches("[A-Za-z0-9_-]{2,64}")) {
            throw bad("协议编码只能包含字母、数字、下划线或短横线，长度 2–64");
        }
        if (agreement.getTitle() == null || agreement.getTitle().isBlank() || agreement.getTitle().length() > 120) {
            throw bad("协议标题不能为空且不能超过 120 字");
        }
        if (agreement.getVersion() == null || agreement.getVersion().isBlank()
            || agreement.getVersion().length() > 40) throw bad("版本号不能为空且不能超过 40 字");
        if (agreement.getContent() == null || agreement.getContent().isBlank()) throw bad("协议正文不能为空");
        if (agreement.getIsPublished() == null) agreement.setIsPublished(true);
        long excludeId = agreement.getId() == null ? 0L : agreement.getId();
        if (agreements.codeCount(agreement.getCode(), excludeId) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "协议编码已存在");
        }
        if (agreement.getId() == null) {
            agreements.insert(agreement);
        } else {
            if (agreements.findById(agreement.getId()) == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "协议不存在");
            }
            agreements.update(agreement);
        }
        return agreements.findById(agreement.getId());
    }

    @Transactional
    public void delete(Long id) {
        if (agreements.delete(id) != 1) throw new ApiException(HttpStatus.NOT_FOUND, "协议不存在");
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
