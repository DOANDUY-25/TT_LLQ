package com.pmh.service;

import com.pmh.model.entity.AuditLog;
import com.pmh.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest request;

    /**
     *  Ghi nhận lịch sử thao tác
     */
    public void saveLog(Long recordId, String action, Integer statusBefore, Integer statusAfter, String description) {
        AuditLog log = AuditLog.builder()
                .module("COMPONENTS")
                .recordId(String.valueOf(recordId))
                .action(action)
                .statusBefore(statusBefore)
                .statusAfter(statusAfter)
                .description(description)
                .performedBy(getCurrentUser())
                .actionDate(LocalDateTime.now())
                .ipAddress(getClientIp())
                .build();

        auditLogRepository.save(log);
    }

    /**
     * Lấy danh sách lịch sử theo ID có phân trang
     */
    public Page<AuditLog> getHistory(String recordId, Pageable pageable) {
        return auditLogRepository.findByRecordIdOrderByActionDateDesc(String.valueOf(recordId), pageable);
    }

    private String getClientIp() {
        return (request != null && request.getRemoteAddr() != null) ? request.getRemoteAddr() : "127.0.0.1";
    }

    private String getCurrentUser() {
        if (request != null && request.getUserPrincipal() != null) {
            return request.getUserPrincipal().getName();
        }
        return "admin"; // Mặc định khi chưa có Security
    }
}

