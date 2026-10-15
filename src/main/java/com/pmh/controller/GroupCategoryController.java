package com.pmh.controller;

import com.pmh.model.dto.GroupCategoryDetailResponseDTO;
import com.pmh.model.dto.GroupCategoryRequestDTO;
import com.pmh.model.dto.GroupCategorySearchDTO;
import com.pmh.model.entity.ApiResponse;
import com.pmh.model.entity.AuditLog;
import com.pmh.model.entity.GroupCategory;
import com.pmh.service.GroupCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/group-categories")
public class GroupCategoryController {

    private final GroupCategoryService groupCategoryService;

    // ─── Search & Filter ────────────────────────────────────────────────────

    /**
     * Tìm kiếm và phân trang
     * POST /api/group-categories/search
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'VIEWER', 'MAKER', 'CHECKER')")
    @PostMapping("/search")
    public ApiResponse<Page<GroupCategory>> search(
            @RequestBody(required = false) GroupCategorySearchDTO filter,
            Pageable pageable) {
        return ApiResponse.success(
                groupCategoryService.search(filter, pageable),
                "Lấy danh sách tham số danh mục thành công");
    }

    /**
     * Danh sách distinct PARAM_TYPE để làm filter
     * GET /api/group-categories/distinct-param-types
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'VIEWER', 'MAKER', 'CHECKER')")
    @GetMapping("/distinct-param-types")
    public ApiResponse<List<String>> getDistinctParamTypes() {
        return ApiResponse.success(groupCategoryService.getDistinctParamTypes(), "OK");
    }

    /**
     * Danh sách distinct COMPONENT_CODE để làm filter
     * GET /api/group-categories/distinct-component-codes
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'VIEWER', 'MAKER', 'CHECKER')")
    @GetMapping("/distinct-component-codes")
    public ApiResponse<List<String>> getDistinctComponentCodes() {
        return ApiResponse.success(groupCategoryService.getDistinctComponentCodes(), "OK");
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    /**
     * Xem chi tiết — trả về cả currentData và newData (nếu có)
     * GET /api/group-categories/{id}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'VIEWER', 'MAKER', 'CHECKER')")
    @GetMapping("/{id}")
    public ApiResponse<GroupCategoryDetailResponseDTO> getDetail(@PathVariable Long id) {
        return ApiResponse.success(
                groupCategoryService.getDetail(id),
                "Lấy chi tiết tham số danh mục thành công");
    }

    /**
     * Thêm mới (Status = 1: Mới, IsDisplay = 1)
     * POST /api/group-categories
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    @PostMapping
    public ApiResponse<GroupCategory> create(@Valid @RequestBody GroupCategoryRequestDTO dto) {
        return ApiResponse.success(
                groupCategoryService.create(dto),
                "Thêm mới tham số danh mục thành công");
    }

    /**
     * Cập nhật
     * - Status=1 (Mới): cập nhật trực tiếp
     * - Status=5/7 (Từ chối/Hủy duyệt): lưu JSON vào NEW_DATA
     * PUT /api/group-categories/{id}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    @PutMapping("/{id}")
    public ApiResponse<GroupCategory> update(
            @PathVariable Long id,
            @Valid @RequestBody GroupCategoryRequestDTO dto) {
        return ApiResponse.success(
                groupCategoryService.update(id, dto),
                "Cập nhật tham số danh mục thành công");
    }

    /**
     * Xóa (chỉ khi IS_DISPLAY=1 và Status=1)
     * DELETE /api/group-categories/{id}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        groupCategoryService.delete(id);
        return ApiResponse.success(null, "Xóa tham số danh mục thành công");
    }

    /**
     * Sao chép
     * POST /api/group-categories/{id}/clone
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    @PostMapping("/{id}/clone")
    public ApiResponse<GroupCategory> clone(
            @PathVariable Long id,
            @Valid @RequestBody GroupCategoryRequestDTO dto) {
        return ApiResponse.success(
                groupCategoryService.clone(id, dto),
                "Sao chép tham số danh mục thành công");
    }

    // ─── Approval Workflow ────────────────────────────────────────────────────

    /**
     * Gửi duyệt hàng loạt (Status 1/5/7 → 3)
     * POST /api/group-categories/submit-approval
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    @PostMapping("/submit-approval")
    public ApiResponse<Void> submitApproval(@RequestBody List<Long> ids) {
        groupCategoryService.submitApproval(ids);
        return ApiResponse.success(null, "Gửi duyệt thành công");
    }

    /**
     * Phê duyệt hàng loạt (Status 3 → 4, merge NEW_DATA)
     * POST /api/group-categories/approval
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'CHECKER')")
    @PostMapping("/approval")
    public ApiResponse<Void> approval(@RequestBody List<Long> ids) {
        groupCategoryService.approval(ids);
        return ApiResponse.success(null, "Phê duyệt thành công");
    }

    /**
     * Từ chối hàng loạt (Status 3 → 5)
     * Body: { "ids": [...], "reason": "..." }
     * POST /api/group-categories/reject
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'CHECKER')")
    @PostMapping("/reject")
    public ApiResponse<Void> reject(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<?> rawIds = (List<?>) body.get("ids");
        // Jackson deserialises numbers as Integer hoặc Long tuỳ giá trị
        List<Long> longIds = rawIds.stream()
                .map(v -> ((Number) v).longValue())
                .toList();
        String reason = (String) body.getOrDefault("reason", "");
        groupCategoryService.reject(longIds, reason);
        return ApiResponse.success(null, "Từ chối thành công");
    }

    /**
     * Hủy duyệt hàng loạt (Status 4 → 7)
     * POST /api/group-categories/cancel-approval
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'CHECKER')")
    @PostMapping("/cancel-approval")
    public ApiResponse<Void> cancelApproval(@RequestBody List<Long> ids) {
        groupCategoryService.cancelApproval(ids);
        return ApiResponse.success(null, "Hủy duyệt thành công");
    }

    // ─── History ──────────────────────────────────────────────────────────────

    /**
     * Lịch sử thao tác có phân trang
     * GET /api/group-categories/{id}/history
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'VIEWER', 'MAKER', 'CHECKER')")
    @GetMapping("/{id}/history")
    public ApiResponse<Page<AuditLog>> getHistory(
            @PathVariable String id,
            Pageable pageable) {
        return ApiResponse.success(
                groupCategoryService.getHistory(id, pageable),
                "Lấy lịch sử thao tác thành công");
    }
}
