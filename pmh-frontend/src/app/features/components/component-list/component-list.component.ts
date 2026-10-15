import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { 
  Component as ComponentModel, 
  ComponentSearchDTO, 
  ComponentStatus,
  ComponentStatusLabels,
  ComponentStatusColors,
  SelectOption,
  ComponentActionConfig
} from '../../../models/component.model';
import { ComponentService } from '../../../core/services/component.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-component-list',
  templateUrl: './component-list.component.html',
  styleUrls: ['./component-list.component.css']
})
export class ComponentListComponent implements OnInit {
  components: ComponentModel[] = [];
  selectedComponents: ComponentModel[] = [];
  Math = Math;

  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  
  searchFilter: ComponentSearchDTO = {};
  
  componentCodeOptions: SelectOption[] = [];
  componentNameOptions: SelectOption[] = [];
  statusOptions: SelectOption[] = [
    { value: ComponentStatus.NEW, label: ComponentStatusLabels[ComponentStatus.NEW] },
    { value: ComponentStatus.PENDING, label: ComponentStatusLabels[ComponentStatus.PENDING] },
    { value: ComponentStatus.APPROVED, label: ComponentStatusLabels[ComponentStatus.APPROVED] },
    { value: ComponentStatus.REJECTED, label: ComponentStatusLabels[ComponentStatus.REJECTED] },
    { value: ComponentStatus.CANCELLED, label: ComponentStatusLabels[ComponentStatus.CANCELLED] }
  ];
  isActiveOptions: SelectOption[] = [
    { value: 0, label: 'Không hoạt động' },
    { value: 1, label: 'Hoạt động' }
  ];
  
  loading = false;
  showDetailModal = false;
  modalComponentId?: number;
  currentUserRoles: string[] = [];

  constructor(
    private componentService: ComponentService,
    private router: Router,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadComponents();
    this.loadFilterOptions();
    this.loadCurrentUserRoles();
  }

  /**
   * Load danh sách components
   */
  loadComponents(): void {
    this.loading = true;
    
    this.componentService.search(this.searchFilter, this.currentPage, this.pageSize)
      .subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.components = response.data.content;
            this.totalElements = response.data.totalElements;
            this.totalPages = response.data.totalPages;
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Lỗi khi tải danh sách:', error);
          this.loading = false;
        }
      });
  }


  loadFilterOptions(): void {

    this.componentService.getDistinctCodes().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.componentCodeOptions = response.data.map(code => ({
            value: code,
            label: code
          }));
        }
      },
      error: (error) => {
        console.error('Lỗi khi load component codes:', error);
      }
    });


    this.componentService.getDistinctNames().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.componentNameOptions = response.data.map(name => ({
            value: name,
            label: name
          }));
        }
      },
      error: (error) => {
        console.error('Lỗi khi load component names:', error);
      }
    });
  }

  /**
   * Load roles của user hiện tại từ localStorage
   */
  loadCurrentUserRoles(): void {
    const stored = localStorage.getItem('user_info');
    if (stored) {
      try {
        const user = JSON.parse(stored);
        // Extract role codes from RoleDTO objects
        this.currentUserRoles = user.roles?.map((r: any) => 
          typeof r === 'string' ? r : r.roleCode
        ) || [];
      } catch {
        this.currentUserRoles = [];
      }
    }
  }


  onSearch(): void {
    this.currentPage = 0;
    this.loadComponents();
  }


  onResetFilter(): void {
    this.searchFilter = {};
    this.onSearch();
  }

  /**
   * Chuyển trang
   */
  onPageChange(page: number): void {
    if (page < 0 || (this.totalPages > 0 && page >= this.totalPages)) {
      return;
    }
    this.currentPage = page;
    this.loadComponents();
  }

  /**
   * Thay đổi page size
   */
  onPageSizeChange(size: any): void {
    this.pageSize = Number(size);
    this.currentPage = 0;
    this.loadComponents();
  }

  /**
   * Toggle chọn một component
   */
  toggleSelection(component: ComponentModel): void {
    const index = this.selectedComponents.findIndex(c => c.id === component.id);
    if (index > -1) {
      this.selectedComponents.splice(index, 1);
    } else {
      this.selectedComponents.push(component);
    }
  }

  /**
   * Toggle chọn tất cả
   */
  toggleSelectAll(checked: boolean): void {
    if (checked) {
      this.selectedComponents = [...this.components];
    } else {
      this.selectedComponents = [];
    }
  }

  /**
   * Kiểm tra component có được chọn không
   */
  isSelected(component: ComponentModel): boolean {
    return this.selectedComponents.some(c => c.id === component.id);
  }

  /**
   * Lấy label trạng thái
   */
  getStatusLabel(status: ComponentStatus): string {
    return ComponentStatusLabels[status] || 'Không xác định';
  }

  /**
   * Lấy color badge trạng thái
   */
  getStatusColor(status: ComponentStatus): string {
    return ComponentStatusColors[status] || 'secondary';
  }

  /**
   * Kiểm tra quyền của user (chấp nhận cả "ADMIN" lẫn "ROLE_ADMIN")
   */
  hasRole(role: string): boolean {
    return this.currentUserRoles.some(r =>
      r === role || r === `ROLE_${role}` || r === role.replace('ROLE_', '')
    ) || this.currentUserRoles.some(r => r === 'ADMIN' || r === 'ROLE_ADMIN');
  }

  /**
   * Lấy cấu hình action buttons theo trạng thái
   */
  getActionConfig(component: ComponentModel): ComponentActionConfig {
    const status = component.status;
    const isMaker = this.hasRole('MAKER');
    const isChecker = this.hasRole('CHECKER');
    
    return {
      canClone: true,  // Tất cả trạng thái đều cho phép sao chép
      canEdit: (status === ComponentStatus.NEW || status === ComponentStatus.REJECTED || status === ComponentStatus.CANCELLED) && isMaker,
      canDelete: status === ComponentStatus.NEW && component.isDisplay === 1 && isMaker,
      canSubmit: (status === ComponentStatus.NEW || status === ComponentStatus.REJECTED || status === ComponentStatus.CANCELLED) && isMaker,
      canApprove: status === ComponentStatus.PENDING && isChecker,
      canReject: status === ComponentStatus.PENDING && isChecker,
      canCancel: status === ComponentStatus.APPROVED && isChecker
    };
  }

  /**
   * Kiểm tra có thể thực hiện action hàng loạt không
   */
  canBulkAction(action: string): boolean {
    if (this.selectedComponents.length === 0) return false;
    
    switch (action) {
      case 'submit':
        return this.selectedComponents.every(c => 
          (c.status === ComponentStatus.NEW || c.status === ComponentStatus.REJECTED || c.status === ComponentStatus.CANCELLED)
        ) && this.hasRole('MAKER');
      
      case 'approve':
        return this.selectedComponents.every(c => c.status === ComponentStatus.PENDING) && this.hasRole('CHECKER');
      
      case 'reject':
        return this.selectedComponents.every(c => c.status === ComponentStatus.PENDING) && this.hasRole('CHECKER');
      
      case 'cancel':
        return this.selectedComponents.every(c => c.status === ComponentStatus.APPROVED) && this.hasRole('CHECKER');
      
      default:
        return false;
    }
  }


  onCreate(): void {
    this.router.navigate(['/components/create']);
  }

  onViewDetail(component: ComponentModel): void {
    this.modalComponentId = component.id;
    this.showDetailModal = true;
  }

  /** Đóng modal, reload nếu cần */
  onModalClosed(reload: boolean): void {
    this.showDetailModal = false;
    this.modalComponentId = undefined;
    if (reload) {
      this.loadComponents();
    }
  }


  onEdit(component: ComponentModel): void {
    this.router.navigate(['/components', component.id, 'edit']);
  }

  onClone(component: ComponentModel): void {
    this.router.navigate(['/components', component.id, 'clone']);
  }


  onDelete(component: ComponentModel): void {
    if (!confirm(`Bạn có chắc chắn muốn xóa cấu phần "${component.componentCode}"?`)) {
      return;
    }

    this.componentService.delete(component.id).subscribe({
      next: (response) => {
        if (response.success) {
          this.notify.showSuccess('Xóa thành công!');
          this.loadComponents();
        }
      },
      error: (error) => {
        this.notify.showError('Lỗi khi xóa: ' + (error.error?.message || error.message));
      }
    });
  }


  onSubmit(component: ComponentModel): void {
    this.bulkSubmit([component.id]);
  }


  onViewHistory(component: ComponentModel): void {
    this.router.navigate(['/components', component.id, 'history']);
  }


  bulkSubmit(ids?: number[]): void {
    const targetIds = ids || this.selectedComponents.map(c => c.id);
    
    if (targetIds.length === 0) {
      this.notify.showWarning('Vui lòng chọn ít nhất một bản ghi');
      return;
    }

    this.componentService.submitApproval(targetIds).subscribe({
      next: (response) => {
        if (response.success) {
          this.notify.showSuccess('Gửi duyệt thành công!');
          this.selectedComponents = [];
          this.loadComponents();
        }
      },
      error: (error) => {
        this.notify.showError('Lỗi khi gửi duyệt: ' + (error.error?.message || error.message));
      }
    });
  }


  bulkApprove(): void {
    const ids = this.selectedComponents.map(c => c.id);
    
    if (!confirm(`Bạn có chắc chắn muốn phê duyệt ${ids.length} bản ghi?`)) {
      return;
    }

    this.componentService.approval(ids).subscribe({
      next: (response) => {
        if (response.success) {
          this.notify.showSuccess('Phê duyệt thành công!');
          this.selectedComponents = [];
          this.loadComponents();
        }
      },
      error: (error) => {
        this.notify.showError('Lỗi khi phê duyệt: ' + (error.error?.message || error.message));
      }
    });
  }

 
  bulkReject(): void {
    const ids = this.selectedComponents.map(c => c.id);
    
    if (!confirm(`Bạn có chắc chắn muốn từ chối ${ids.length} bản ghi?`)) {
      return;
    }

    this.componentService.reject(ids).subscribe({
      next: (response) => {
        if (response.success) {
          this.notify.showSuccess('Từ chối thành công!');
          this.selectedComponents = [];
          this.loadComponents();
        }
      },
      error: (error) => {
        this.notify.showError('Lỗi khi từ chối: ' + (error.error?.message || error.message));
      }
    });
  }


  bulkCancel(): void {
    const ids = this.selectedComponents.map(c => c.id);
    
    if (!confirm(`Bạn có chắc chắn muốn hủy duyệt ${ids.length} bản ghi?`)) {
      return;
    }

    this.componentService.cancelApproval(ids).subscribe({
      next: (response) => {
        if (response.success) {
          this.notify.showSuccess('Hủy duyệt thành công!');
          this.selectedComponents = [];
          this.loadComponents();
        }
      },
      error: (error) => {
        this.notify.showError('Lỗi khi hủy duyệt: ' + (error.error?.message || error.message));
      }
    });
  }


  onExportExcel(): void {
    this.notify.showInfo('Chức năng xuất Excel đang được phát triển');
  }
}
