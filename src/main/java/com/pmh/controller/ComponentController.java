package com.pmh.controller;

import com.pmh.model.dto.ComponentCloneDTO;
import com.pmh.model.dto.ComponentDetailResponseDTO;
import com.pmh.model.dto.ComponentRequestDTO;
import com.pmh.model.dto.ComponentSearchDTO;
import com.pmh.model.entity.ApiResponse;
import com.pmh.model.entity.Components;
import com.pmh.service.ComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/components")
public class ComponentController {

    private final ComponentService componentService;

    /**
     *  Tìm kiếm và phân trang
     */
    @PostMapping("/search")
    public ApiResponse<Page<Components>> search(@RequestBody(required = false) ComponentSearchDTO filter, Pageable pageable) {
        Page<Components> componentsList = componentService.search(filter, pageable);
        return ApiResponse.success(componentsList, "Lấy danh sách cấu phần thành công");
    }

    /**
     * Xem chi tiết cấu phần (hỗ trợ hiển thị so sánh Dữ liệu cũ & Dữ liệu mới từ NEW_DATA)
     */
    @GetMapping("/{id}")
    public ApiResponse<ComponentDetailResponseDTO> getDetail(@PathVariable Long id) {
        ComponentDetailResponseDTO detail = componentService.getDetail(id);
        return ApiResponse.success(detail, "Lấy chi tiết cấu phần thành công");
    }

    /**
     * Thêm mới cấu phần (Status = 1: Mới, IsDisplay = 1)
     */
    @PostMapping
    public ApiResponse<Components> create(@Valid @RequestBody ComponentRequestDTO dto) {
        Components created = componentService.create(dto);
        return ApiResponse.success(created, "Thêm mới cấu phần thành công");
    }

    /**
     * Cập nhật cấu phần
     * - Status = 1 (Mới): Cập nhật trực tiếp
     * - Status = 5 (Từ chối) / 7 (Hủy duyệt): Lưu JSON vào NEW_DATA
     */
    @PutMapping("/{id}")
    public ApiResponse<Components> update(@PathVariable Long id, @Valid @RequestBody ComponentRequestDTO dto) {
        Components updated = componentService.update(id, dto);
        return ApiResponse.success(updated, "Cập nhật cấu phần thành công");
    }

    /**
     *  Xóa cấu phần (Chỉ xóa được khi IS_DISPLAY = 1 và Status = 1)
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        componentService.delete(id);
        return ApiResponse.success(null, "Xóa cấu phần thành công");
    }

    /**
     * Sao chép cấu phần (Clone)
     */
    @PostMapping("/{id}/clone")
    public ApiResponse<Components> clone(@PathVariable Long id, @Valid @RequestBody ComponentCloneDTO cloneDTO) {
        Components cloned = componentService.clone(id, cloneDTO);
        return ApiResponse.success(cloned, "Sao chép cấu phần thành công");
    }
}

