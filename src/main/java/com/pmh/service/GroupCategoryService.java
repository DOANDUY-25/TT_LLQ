package com.pmh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmh.model.dto.GroupCategoryDetailResponseDTO;
import com.pmh.model.dto.GroupCategoryRequestDTO;
import com.pmh.model.dto.GroupCategorySearchDTO;
import com.pmh.model.entity.AuditLog;
import com.pmh.model.entity.GroupCategory;
import com.pmh.model.enums.ComponentStatus;
import com.pmh.repository.GroupCategoryRepository;
import com.pmh.specification.GroupCategorySpecs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupCategoryService {

    private static final String MODULE = "GROUP_CATEGORY";

    private final GroupCategoryRepository groupCategoryRepository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    // ─── Search ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<GroupCategory> search(GroupCategorySearchDTO filter, Pageable pageable) {
        if (filter == null) filter = new GroupCategorySearchDTO();

        int page = (filter.getPage() != null && filter.getPage() >= 0)
                ? filter.getPage() : pageable.getPageNumber();
        int size = (filter.getSize() != null && filter.getSize() > 0)
                ? filter.getSize() : pageable.getPageSize();

        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "updatedDate");

        Pageable finalPageable = PageRequest.of(page, size, sort);

        Specification<GroupCategory> specs = (root, query, cb) -> cb.conjunction();

        if (filter.getParamType() != null && !filter.getParamType().isBlank()) {
            specs = specs.and(GroupCategorySpecs.hasParamType(filter.getParamType()));
        }
        if (filter.getParamValue() != null && !filter.getParamValue().isBlank()) {
            specs = specs.and(GroupCategorySpecs.hasParamValue(filter.getParamValue()));
        }
        if (filter.getParamName() != null && !filter.getParamName().isBlank()) {
            specs = specs.and(GroupCategorySpecs.hasParamName(filter.getParamName()));
        }
        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
            specs = specs.and(GroupCategorySpecs.hasStatus(filter.getStatuses()));
        }
        if (filter.getIsActives() != null && !filter.getIsActives().isEmpty()) {
            specs = specs.and(GroupCategorySpecs.hasIsActive(filter.getIsActives()));
        }

        return groupCategoryRepository.findAll(specs, finalPageable);
    }

    // ─── Detail ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GroupCategoryDetailResponseDTO getDetail(Long id) {
        GroupCategory entity = findById(id);

        GroupCategoryRequestDTO newDataDto = null;
        if (entity.getNewData() != null && !entity.getNewData().isBlank()) {
            try {
                newDataDto = objectMapper.readValue(entity.getNewData(), GroupCategoryRequestDTO.class);
            } catch (JsonProcessingException e) {
                log.error("Lỗi parse NEW_DATA cho GroupCategory ID {}: {}", id, e.getMessage());
            }
        }

        return GroupCategoryDetailResponseDTO.builder()
                .currentData(entity)
                .newData(newDataDto)
                .build();
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public GroupCategory create(GroupCategoryRequestDTO dto) {
        checkDuplicate(dto.getParamName(), dto.getParamType(), dto.getComponentCode(), null);
        validateDates(dto.getEffectiveDate(), dto.getEndEffectiveDate(), true);

        GroupCategory entity = GroupCategory.builder()
                .paramName(dto.getParamName().trim())
                .paramValue(dto.getParamValue().trim())
                .paramType(dto.getParamType().trim())
                .description(dto.getDescription())
                .componentCode(dto.getComponentCode().trim().toUpperCase())
                .effectiveDate(dto.getEffectiveDate())
                .endEffectiveDate(dto.getEndEffectiveDate())
                .newData(null)
                .build();

        entity.setStatus(ComponentStatus.NEW.getCode());
        entity.setIsDisplay(1);
        entity.setIsActive(1);

        GroupCategory saved = groupCategoryRepository.save(entity);
        auditLogService.saveLog(MODULE, saved.getId(), "THÊM MỚI",
                null, saved.getStatus(),
                "Thêm mới tham số danh mục: " + saved.getParamType() + " / " + saved.getParamName());
        return saved;
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    /**
     * Quy tắc cốt lõi:
     * - Status = 1 (Mới)          → cập nhật trực tiếp vào bảng
     * - Status = 5/7 (Từ chối/Hủy duyệt) → lưu JSON vào NEW_DATA, chờ Checker duyệt mới merge
     * - Status = 3/4 (Chờ/Đã duyệt)      → chặn, ném exception
     */
    @Transactional
    public GroupCategory update(Long id, GroupCategoryRequestDTO dto) {
        GroupCategory entity = findById(id);
        ComponentStatus status = ComponentStatus.formCode(entity.getStatus());
        validateDates(dto.getEffectiveDate(), dto.getEndEffectiveDate(), false);
        Integer oldStatus = entity.getStatus();

        switch (status) {
            case NEW -> {
                checkDuplicate(dto.getParamName(), dto.getParamType(), dto.getComponentCode(), id);
                entity.setParamName(dto.getParamName().trim());
                entity.setParamValue(dto.getParamValue().trim());
                entity.setParamType(dto.getParamType().trim());
                entity.setDescription(dto.getDescription());
                entity.setComponentCode(dto.getComponentCode().trim().toUpperCase());
                entity.setEffectiveDate(dto.getEffectiveDate());
                entity.setEndEffectiveDate(dto.getEndEffectiveDate());
                entity.setNewData(null);
            }
            case REJECTED, CANCELLED -> {
                try {
                    entity.setNewData(objectMapper.writeValueAsString(dto));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Lỗi chuyển đổi dữ liệu sang JSON: " + e.getMessage());
                }
            }
            case PENDING ->
                throw new RuntimeException("Bản ghi đang ở trạng thái 'Chờ duyệt', không được phép chỉnh sửa!");
            case APPROVED ->
                throw new RuntimeException("Bản ghi đang ở trạng thái 'Đã duyệt'! Vui lòng Hủy duyệt trước khi sửa.");
            default ->
                throw new RuntimeException("Không được phép sửa bản ghi ở trạng thái hiện tại!");
        }

        GroupCategory saved = groupCategoryRepository.save(entity);
        auditLogService.saveLog(MODULE, saved.getId(), "CẬP NHẬT",
                oldStatus, saved.getStatus(),
                "Cập nhật tham số danh mục: " + saved.getParamType() + " / " + saved.getParamName());
        return saved;
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        GroupCategory entity = findById(id);

        if (entity.getIsDisplay() != null && entity.getIsDisplay() == 2) {
            throw new RuntimeException("Bản ghi đã từng được phê duyệt (IS_DISPLAY=2), không được phép xóa!");
        }
        if (entity.getStatus() == null || entity.getStatus() != ComponentStatus.NEW.getCode()) {
            throw new RuntimeException("Chỉ được phép xóa bản ghi ở trạng thái 'Mới'!");
        }

        auditLogService.saveLog(MODULE, entity.getId(), "XÓA",
                entity.getStatus(), null,
                "Xóa tham số danh mục: " + entity.getParamType() + " / " + entity.getParamName());
        groupCategoryRepository.delete(entity);
    }

    // ─── Clone ────────────────────────────────────────────────────────────────

    @Transactional
    public GroupCategory clone(Long sourceId, GroupCategoryRequestDTO dto) {
        GroupCategory source = findById(sourceId);
        checkDuplicate(dto.getParamName(), dto.getParamType(), dto.getComponentCode(), null);
        validateDates(dto.getEffectiveDate(), dto.getEndEffectiveDate(), true);

        GroupCategory cloned = GroupCategory.builder()
                .paramName(dto.getParamName().trim())
                .paramValue(dto.getParamValue().trim())
                .paramType(dto.getParamType().trim())
                .description(dto.getDescription() != null ? dto.getDescription() : source.getDescription())
                .componentCode(dto.getComponentCode().trim().toUpperCase())
                .effectiveDate(dto.getEffectiveDate())
                .endEffectiveDate(dto.getEndEffectiveDate())
                .newData(null)
                .build();

        cloned.setStatus(ComponentStatus.NEW.getCode());
        cloned.setIsDisplay(1);
        cloned.setIsActive(1);

        GroupCategory saved = groupCategoryRepository.save(cloned);
        auditLogService.saveLog(MODULE, saved.getId(), "SAO CHÉP",
                null, saved.getStatus(),
                "Sao chép từ ID " + sourceId + " → " + saved.getParamType() + " / " + saved.getParamName());
        return saved;
    }

    // ─── Approval Workflow ────────────────────────────────────────────────────

    @Transactional
    public void submitApproval(List<Long> ids) {
        List<GroupCategory> list = groupCategoryRepository.findAllById(ids);
        for (GroupCategory g : list) {
            int s = g.getStatus();
            if (s != ComponentStatus.NEW.getCode()
                    && s != ComponentStatus.REJECTED.getCode()
                    && s != ComponentStatus.CANCELLED.getCode()) {
                throw new RuntimeException(
                        "Trạng thái không hợp lệ để gửi duyệt: " + g.getParamName());
            }
            Integer oldStatus = g.getStatus();
            g.setStatus(ComponentStatus.PENDING.getCode());
            auditLogService.saveLog(MODULE, g.getId(), "GỬI DUYỆT",
                    oldStatus, g.getStatus(),
                    "Gửi duyệt tham số danh mục: " + g.getParamType() + " / " + g.getParamName());
        }
        groupCategoryRepository.saveAll(list);
    }

    @Transactional
    public void approval(List<Long> ids) {
        List<GroupCategory> list = groupCategoryRepository.findAllById(ids);
        for (GroupCategory g : list) {
            if (g.getStatus() != ComponentStatus.PENDING.getCode()) {
                throw new RuntimeException(
                        "Trạng thái không hợp lệ để phê duyệt: " + g.getParamName());
            }
            Integer oldStatus = g.getStatus();
            g.setStatus(ComponentStatus.APPROVED.getCode());
            g.setIsDisplay(2);  // Đã duyệt ít nhất 1 lần → không cho xóa nữa

            // Merge NEW_DATA vào các cột chính nếu có
            if (g.getNewData() != null && !g.getNewData().isBlank()) {
                try {
                    GroupCategoryRequestDTO nd = objectMapper.readValue(
                            g.getNewData(), GroupCategoryRequestDTO.class);
                    if (nd.getParamName()      != null) g.setParamName(nd.getParamName().trim());
                    if (nd.getParamValue()     != null) g.setParamValue(nd.getParamValue().trim());
                    if (nd.getParamType()      != null) g.setParamType(nd.getParamType().trim());
                    if (nd.getDescription()    != null) g.setDescription(nd.getDescription());
                    if (nd.getComponentCode()  != null) g.setComponentCode(nd.getComponentCode().trim().toUpperCase());
                    if (nd.getEffectiveDate()  != null) g.setEffectiveDate(nd.getEffectiveDate());
                    g.setEndEffectiveDate(nd.getEndEffectiveDate());
                    g.setNewData(null);
                } catch (Exception e) {
                    log.error("Lỗi merge NEW_DATA khi duyệt GroupCategory ID {}: {}", g.getId(), e.getMessage());
                }
            }

            auditLogService.saveLog(MODULE, g.getId(), "PHÊ DUYỆT",
                    oldStatus, g.getStatus(),
                    "Phê duyệt tham số danh mục: " + g.getParamType() + " / " + g.getParamName());
        }
        groupCategoryRepository.saveAll(list);
    }

    @Transactional
    public void reject(List<Long> ids, String reason) {
        List<GroupCategory> list = groupCategoryRepository.findAllById(ids);
        for (GroupCategory g : list) {
            if (g.getStatus() != ComponentStatus.PENDING.getCode()) {
                throw new RuntimeException(
                        "Trạng thái không hợp lệ để từ chối: " + g.getParamName());
            }
            Integer oldStatus = g.getStatus();
            g.setStatus(ComponentStatus.REJECTED.getCode());
            String desc = "Từ chối tham số danh mục: " + g.getParamType() + " / " + g.getParamName();
            if (reason != null && !reason.isBlank()) desc += " | Lý do: " + reason;
            auditLogService.saveLog(MODULE, g.getId(), "TỪ CHỐI", oldStatus, g.getStatus(), desc);
        }
        groupCategoryRepository.saveAll(list);
    }

    @Transactional
    public void cancelApproval(List<Long> ids) {
        List<GroupCategory> list = groupCategoryRepository.findAllById(ids);
        for (GroupCategory g : list) {
            if (g.getStatus() != ComponentStatus.APPROVED.getCode()) {
                throw new RuntimeException(
                        "Trạng thái không hợp lệ để hủy duyệt: " + g.getParamName());
            }
            Integer oldStatus = g.getStatus();
            g.setStatus(ComponentStatus.CANCELLED.getCode());
            auditLogService.saveLog(MODULE, g.getId(), "HỦY DUYỆT",
                    oldStatus, g.getStatus(),
                    "Hủy duyệt tham số danh mục: " + g.getParamType() + " / " + g.getParamName());
        }
        groupCategoryRepository.saveAll(list);
    }

    // ─── Distinct values for filter ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<String> getDistinctParamTypes() {
        return groupCategoryRepository.findDistinctParamTypes();
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctComponentCodes() {
        return groupCategoryRepository.findDistinctComponentCodes();
    }

    // ─── History ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AuditLog> getHistory(String id, Pageable pageable) {
        return auditLogService.getHistory(id, pageable);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private GroupCategory findById(Long id) {
        return groupCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy tham số danh mục với ID: " + id));
    }

    /** Kiểm tra trùng (paramName + paramType + componentCode) */
    private void checkDuplicate(String paramName, String paramType, String componentCode, Long excludeId) {
        boolean exists = excludeId == null
                ? groupCategoryRepository.existsByParamNameAndParamTypeAndComponentCode(
                        paramName, paramType, componentCode)
                : groupCategoryRepository.existsByParamNameAndParamTypeAndComponentCodeAndIdNot(
                        paramName, paramType, componentCode, excludeId);
        if (exists) {
            throw new RuntimeException(
                    "Tham số danh mục với Tên '" + paramName
                    + "', Loại '" + paramType
                    + "', Cấu phần '" + componentCode + "' đã tồn tại!");
        }
    }

    private void validateDates(LocalDateTime effectiveDate, LocalDateTime endEffectiveDate, boolean checkPast) {
        if (effectiveDate == null) {
            throw new RuntimeException("Ngày hiệu lực không được để trống!");
        }
        if (checkPast && effectiveDate.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new RuntimeException("Ngày hiệu lực không hợp lệ! Không được chọn thời gian trong quá khứ.");
        }
        if (endEffectiveDate != null && endEffectiveDate.isBefore(effectiveDate)) {
            throw new RuntimeException("Ngày hết hiệu lực phải lớn hơn hoặc bằng ngày hiệu lực!");
        }
    }
}
