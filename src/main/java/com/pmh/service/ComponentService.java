package com.pmh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmh.model.dto.ComponentCloneDTO;
import com.pmh.model.dto.ComponentDetailResponseDTO;
import com.pmh.model.dto.ComponentRequestDTO;
import com.pmh.model.dto.ComponentSearchDTO;
import com.pmh.model.entity.Components;
import com.pmh.model.enums.ComponentStatus;
import com.pmh.repository.ComponentRepository;
import com.pmh.specification.ComponentSpecs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentService {

    private final ComponentRepository componentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Tìm kiếm & phân trang
     */
    @Transactional
    public Page<Components> search(ComponentSearchDTO filter, Pageable pageable) {
        if (filter == null) {
            filter = new ComponentSearchDTO();
        }

        int page = (filter.getPage() != null && filter.getPage() >= 0) ? filter.getPage() : pageable.getPageNumber();
        int size = (filter.getSize() != null && filter.getSize() > 0) ? filter.getSize() : pageable.getPageSize();

        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "updatedDate");

        Pageable finalPageable = PageRequest.of(page, size, sort);

        Specification<Components> specs = (root, query, cb) -> cb.conjunction();
        if (filter.getComponentCodes() != null && !filter.getComponentCodes().isEmpty()) {
            specs = specs.and(ComponentSpecs.hasComponentCode(filter.getComponentCodes()));
        }
        if (filter.getComponentNames() != null && !filter.getComponentNames().isEmpty()) {
            specs = specs.and(ComponentSpecs.hasComponentName(filter.getComponentNames()));
        }
        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
            specs = specs.and(ComponentSpecs.hasStatus(filter.getStatuses()));
        }
        if (filter.getIsActives() != null && !filter.getIsActives().isEmpty()) {
            specs = specs.and(ComponentSpecs.hasIsActive(filter.getIsActives()));
        }

        return componentRepository.findAll(specs, finalPageable);

    }

    /**
     * Xem chi tiết cấu phần (trả về Dữ liệu cũ và Dữ liệu mới từ NEW_DATA nếu có)
     */
    @Transactional
    public ComponentDetailResponseDTO getDetail(Long id) {
        Components comp = componentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu phần với ID: " + id));

        ComponentRequestDTO newDataDto = null;
        if (comp.getNewData() != null && !comp.getNewData().isBlank()) {
            try {
                newDataDto = objectMapper.readValue(comp.getNewData(), ComponentRequestDTO.class);
            } catch (JsonProcessingException e) {
                log.error("Lỗi parse JSON từ trường NEW_DATA của ID {}: {}", id, e.getMessage());
            }
        }

        return ComponentDetailResponseDTO.builder()
                .currentData(comp)
                .newData(newDataDto)
                .build();
    }

    /**
     * Thêm mới cấu phần (Role: MAKER)
     */
    @Transactional
    public Components create(ComponentRequestDTO dto) {

        if (componentRepository.existsByComponentCode(dto.getComponentCode())) {
            throw new RuntimeException("Mã cấu phần '" + dto.getComponentCode() + "' đã tồn tại trong hệ thống!");
        }

        validateDates(dto.getEffectiveDate(), dto.getEndEffectiveDate(), true);

        Components comp = Components.builder()
                .componentCode(dto.getComponentCode().trim().toUpperCase())
                .componentName(dto.getComponentName().trim())
                .messageType(dto.getMessageType())
                .connectionMethod(dto.getConnectionMethod())
                .checkToken(dto.getCheckToken() != null ? dto.getCheckToken() : "N")
                .effectiveDate(dto.getEffectiveDate())
                .endEffectiveDate(dto.getEndEffectiveDate())
                .newData(null)
                .build();

        // Mặc định khi tạo mới: Status = 1 (Mới), IsDisplay = 1 (Chưa duyệt, cho phép xóa), IsActive = 1
        comp.setStatus(ComponentStatus.NEW.getCode());
        comp.setIsDisplay(1);
        comp.setIsActive(1);

        return componentRepository.save(comp);
    }

    /**
     * Sửa cấu phần (QUY TẮC CỐT LÕI VỀ NEW_DATA)
     * - Status = 1 (Mới): Cập nhật trực tiếp vào dữ liệu hiện tại
     * - Status = 5 (Từ chối) hoặc 7 (Hủy duyệt): KHÔNG sửa trực tiếp, lưu JSON vào NEW_DATA
     * - Status = 3 (Chờ duyệt) hoặc 4 (Đã duyệt): Chặn không cho sửa
     */
    @Transactional
    public Components update(Long id, ComponentRequestDTO dto) {
        Components currentComp = componentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu phần với ID: " + id));

        ComponentStatus status = ComponentStatus.formCode(currentComp.getStatus());

        validateDates(dto.getEffectiveDate(), dto.getEndEffectiveDate(), false);


        dto.setComponentCode(currentComp.getComponentCode());

        if (status == ComponentStatus.NEW) {

            currentComp.setComponentName(dto.getComponentName().trim());
            currentComp.setMessageType(dto.getMessageType());
            currentComp.setConnectionMethod(dto.getConnectionMethod());
            currentComp.setCheckToken(dto.getCheckToken() != null ? dto.getCheckToken().trim().toUpperCase() : "N");
            currentComp.setEffectiveDate(dto.getEffectiveDate());
            currentComp.setEndEffectiveDate(dto.getEndEffectiveDate());
            currentComp.setNewData(null);

        } else if (status == ComponentStatus.REJECTED || status == ComponentStatus.CANCELLED) {
            try {
                String jsonData = objectMapper.writeValueAsString(dto);
                currentComp.setNewData(jsonData);

            } catch (JsonProcessingException e) {
                throw new RuntimeException("Lỗi chuyển đổi dữ liệu sang JSON: " + e.getMessage());
            }

        } else if (status == ComponentStatus.PENDING) {
            throw new RuntimeException("Bản ghi đang ở trạng thái 'Chờ duyệt', không được phép chỉnh sửa!");
        } else if (status == ComponentStatus.APPROVED) {
            throw new RuntimeException("Bản ghi đang ở trạng thái 'Đã duyệt'! Vui lòng thực hiện Hủy duyệt trước khi sửa.");
        } else {
            throw new RuntimeException("Không được phép sửa bản ghi ở trạng thái hiện tại!");
        }

        return componentRepository.save(currentComp);
    }

    /**
     * Xóa cấu phần
     * - Chỉ cho phép xóa khi IS_DISPLAY = 1 (Chưa từng duyệt) và Status = 1 (Mới)
     * - Khi IS_DISPLAY = 2 (Đã duyệt): Tuyệt đối không cho phép xóa
     */
    @Transactional
    public void delete(Long id) {
        Components comp = componentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu phần với ID: " + id));

        if (comp.getIsDisplay() != null && comp.getIsDisplay() == 2) {
            throw new RuntimeException("Bản ghi đã từng được phê duyệt (IS_DISPLAY = 2), không được phép xóa!");
        }

        if (comp.getStatus() != null && comp.getStatus() != ComponentStatus.NEW.getCode()) {
            throw new RuntimeException("Chỉ được phép xóa bản ghi ở trạng thái 'Mới'!");
        }

        componentRepository.delete(comp);
    }

    /**
     * Sao chép cấu phần (Clone)
     * - Kế thừa thông tin cấu hình từ bản ghi nguồn
     * - Tạo mới với mã mới, Status = 1 (Mới), IsDisplay = 1
     */
    @Transactional
    public Components clone(Long sourceId, ComponentCloneDTO cloneDTO) {
        Components source = componentRepository.findById(sourceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu phần gốc với ID: " + sourceId));

        String newCode = cloneDTO.getNewComponentCode().trim().toUpperCase();
        if (componentRepository.existsByComponentCode(newCode)) {
            throw new RuntimeException("Mã cấu phần mới '" + newCode + "' đã tồn tại trong hệ thống!");
        }

        String newName = (cloneDTO.getNewComponentName() != null && !cloneDTO.getNewComponentName().isBlank())
                ? cloneDTO.getNewComponentName().trim()
                : source.getComponentName();

        Components cloned = Components.builder()
                .componentCode(newCode)
                .componentName(newName)
                .messageType(source.getMessageType())
                .connectionMethod(source.getConnectionMethod())
                .checkToken(source.getCheckToken())
                .effectiveDate(LocalDateTime.now())
                .endEffectiveDate(source.getEndEffectiveDate())
                .description(source.getDescription())
                .newData(null)
                .build();

        cloned.setStatus(ComponentStatus.NEW.getCode());
        cloned.setIsDisplay(1);
        cloned.setIsActive(1);

        return componentRepository.save(cloned);
    }

    /**
     * Kiểm tra tính hợp lệ của ngày hiệu lực
     */
    private void validateDates(LocalDateTime effectiveDate, LocalDateTime endEffectiveDate, boolean checkPast) {
        if (effectiveDate == null) {
            throw new RuntimeException("Ngày hiệu lực không được để trống!");
        }

        // RSD trang 14: Không cho phép chọn ngày quá khứ (cho phép sai số 5 phút)
        if (checkPast && effectiveDate.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new RuntimeException("Ngày hiệu lực không hợp lệ! Không được chọn thời gian trong quá khứ.");
        }

        if (checkPast && endEffectiveDate != null && endEffectiveDate.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new RuntimeException("Ngày hết hiệu lực không hợp lệ! Không được chọn thời gian trong quá khứ.");
        }

        if (endEffectiveDate != null && endEffectiveDate.isBefore(effectiveDate)) {
            throw new RuntimeException("Ngày hết hiệu lực phải lớn hơn hoặc bằng ngày hiệu lực!");
        }
    }
}
