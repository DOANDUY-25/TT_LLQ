import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import {
  GroupCategory,
  GroupCategorySearchDTO,
  GroupCategoryStatus,
  GroupCategoryStatusLabels,
  GroupCategoryStatusColors,
  GroupCategoryActionConfig,
  SelectOption
} from '../../../models/group-category.model';
import { GroupCategoryService } from '../../../core/services/group-category.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-group-category-list',
  templateUrl: './group-category-list.component.html',
  styleUrls: ['./group-category-list.component.css']
})
export class GroupCategoryListComponent implements OnInit {
  items: GroupCategory[] = [];
  selected: GroupCategory[] = [];
  Math = Math;

  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  filter: GroupCategorySearchDTO = {};

  statusOptions: SelectOption[] = [
    { value: GroupCategoryStatus.NEW,       label: GroupCategoryStatusLabels[GroupCategoryStatus.NEW] },
    { value: GroupCategoryStatus.PENDING,   label: GroupCategoryStatusLabels[GroupCategoryStatus.PENDING] },
    { value: GroupCategoryStatus.APPROVED,  label: GroupCategoryStatusLabels[GroupCategoryStatus.APPROVED] },
    { value: GroupCategoryStatus.REJECTED,  label: GroupCategoryStatusLabels[GroupCategoryStatus.REJECTED] },
    { value: GroupCategoryStatus.CANCELLED, label: GroupCategoryStatusLabels[GroupCategoryStatus.CANCELLED] }
  ];
  isActiveOptions: SelectOption[] = [
    { value: 0, label: 'Không hoạt động' },
    { value: 1, label: 'Hoạt động' }
  ];
  paramTypeOptions: string[] = [];

  loading = false;
  currentUserRoles: string[] = [];

  showModal = false;
  modalId?: number;

  showBulkRejectDialog = false;
  bulkRejectReason = '';
  bulkRejectLoading = false;

  showBulkApproveConfirm = false;
  bulkApproveLoading = false;

  showBulkCancelConfirm = false;
  bulkCancelLoading = false;

  constructor(
    private gcService: GroupCategoryService,
    private router: Router,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadRoles();
    this.loadList();
    this.loadParamTypes();
  }

  // ─── Load ──────────────────────────────────────────────────────────────────

  loadList(): void {
    this.loading = true;
    this.gcService.search(this.filter, this.currentPage, this.pageSize).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.items = res.data.content;
          this.totalElements = res.data.totalElements;
          this.totalPages = res.data.totalPages;
        }
        this.loading = false;
      },
      error: err => {
        console.error('Lỗi tải danh sách:', err);
        this.loading = false;
      }
    });
  }

  loadParamTypes(): void {
    this.gcService.getDistinctParamTypes().subscribe({
      next: res => { if (res.success) this.paramTypeOptions = res.data ?? []; }
    });
  }

  loadRoles(): void {
    try {
      const stored = localStorage.getItem('user_info');
      if (stored) {
        const userInfo = JSON.parse(stored);
        // Extract role codes from RoleDTO objects
        this.currentUserRoles = userInfo.roles?.map((r: any) => 
          typeof r === 'string' ? r : r.roleCode
        ) ?? [];
      }
    } catch { 
      this.currentUserRoles = []; 
    }
  }

  // ─── Search ────────────────────────────────────────────────────────────────

  onSearch(): void {
    this.currentPage = 0;
    this.loadList();
  }

  onResetFilter(): void {
    this.filter = {};
    this.onSearch();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadList();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = Number(size);
    this.currentPage = 0;
    this.loadList();
  }

  // ─── Selection ─────────────────────────────────────────────────────────────

  toggleSelect(item: GroupCategory): void {
    const idx = this.selected.findIndex(s => s.id === item.id);
    idx > -1 ? this.selected.splice(idx, 1) : this.selected.push(item);
  }

  toggleSelectAll(checked: boolean): void {
    this.selected = checked ? [...this.items] : [];
  }

  isSelected(item: GroupCategory): boolean {
    return this.selected.some(s => s.id === item.id);
  }

  get allSelected(): boolean {
    return this.items.length > 0 && this.selected.length === this.items.length;
  }

  // ─── Role helpers ──────────────────────────────────────────────────────────

  hasRole(role: string): boolean {
    return this.currentUserRoles.some(r =>
      r === role || r === `ROLE_${role}` || r.replace('ROLE_', '') === role
    ) || this.currentUserRoles.some(r => r === 'ADMIN' || r === 'ROLE_ADMIN');
  }

  // ─── Status helpers ────────────────────────────────────────────────────────

  getStatusLabel(s: GroupCategoryStatus): string {
    return GroupCategoryStatusLabels[s] ?? 'Không xác định';
  }

  getStatusColor(s: GroupCategoryStatus): string {
    return GroupCategoryStatusColors[s] ?? 'secondary';
  }

  // ─── Action config per row ─────────────────────────────────────────────────

  getActionConfig(item: GroupCategory): GroupCategoryActionConfig {
    const s = item.status;
    const isMaker   = this.hasRole('MAKER');
    const isChecker = this.hasRole('CHECKER');
    const editable  = s === GroupCategoryStatus.NEW
                   || s === GroupCategoryStatus.REJECTED
                   || s === GroupCategoryStatus.CANCELLED;
    return {
      canView:    true,
      canClone:   true,
      canEdit:    editable && isMaker,
      canDelete:  s === GroupCategoryStatus.NEW && item.isDisplay === 1 && isMaker,
      canSubmit:  editable && isMaker,
      canApprove: s === GroupCategoryStatus.PENDING && isChecker,
      canReject:  s === GroupCategoryStatus.PENDING && isChecker,
      canCancel:  s === GroupCategoryStatus.APPROVED && isChecker,
      canHistory: true
    };
  }

  // ─── Bulk action eligibility ───────────────────────────────────────────────

  canBulkSubmit(): boolean {
    return this.selected.length > 0 && this.hasRole('MAKER') &&
      this.selected.every(c =>
        c.status === GroupCategoryStatus.NEW ||
        c.status === GroupCategoryStatus.REJECTED ||
        c.status === GroupCategoryStatus.CANCELLED
      );
  }

  canBulkApprove(): boolean {
    return this.selected.length > 0 && this.hasRole('CHECKER') &&
      this.selected.every(c => c.status === GroupCategoryStatus.PENDING);
  }

  canBulkReject(): boolean {
    return this.selected.length > 0 && this.hasRole('CHECKER') &&
      this.selected.every(c => c.status === GroupCategoryStatus.PENDING);
  }

  canBulkCancel(): boolean {
    return this.selected.length > 0 && this.hasRole('CHECKER') &&
      this.selected.every(c => c.status === GroupCategoryStatus.APPROVED);
  }

  // ─── Row navigation ────────────────────────────────────────────────────────

  onCreate(): void { this.router.navigate(['/group-categories/create']); }

  onViewDetail(item: GroupCategory): void {
    this.modalId = item.id;
    this.showModal = true;
  }

  onEdit(item: GroupCategory): void {
    this.router.navigate(['/group-categories', item.id, 'edit']);
  }

  onClone(item: GroupCategory): void {
    this.router.navigate(['/group-categories', item.id, 'clone']);
  }

  onHistory(item: GroupCategory): void {
    this.router.navigate(['/group-categories', item.id, 'history']);
  }

  onModalClosed(reload: boolean): void {
    this.showModal = false;
    this.modalId = undefined;
    if (reload) { this.selected = []; this.loadList(); }
  }

  onRowDelete(item: GroupCategory): void {
    if (!confirm(`Xóa "${item.paramName}"?`)) return;
    this.gcService.delete(item.id).subscribe({
      next: res => { if (res.success) { this.notify.showSuccess('Xóa thành công!'); this.loadList(); } },
      error: err => this.notify.showError('Lỗi: ' + (err.error?.message || err.message))
    });
  }

  onRowSubmit(item: GroupCategory): void {
    this.gcService.submitApproval([item.id]).subscribe({
      next: res => { if (res.success) { this.notify.showSuccess('Gửi duyệt thành công!'); this.loadList(); } },
      error: err => this.notify.showError('Lỗi: ' + (err.error?.message || err.message))
    });
  }

  bulkSubmit(): void {
    const ids = this.selected.map(c => c.id);
    this.gcService.submitApproval(ids).subscribe({
      next: res => {
        if (res.success) { this.notify.showSuccess(`Gửi duyệt ${ids.length} bản ghi thành công!`); this.selected = []; this.loadList(); }
      },
      error: err => this.notify.showError('Lỗi gửi duyệt: ' + (err.error?.message || err.message))
    });
  }

  openBulkApproveConfirm(): void  { this.showBulkApproveConfirm = true; }
  closeBulkApproveConfirm(): void { this.showBulkApproveConfirm = false; }

  confirmBulkApprove(): void {
    this.bulkApproveLoading = true;
    const ids = this.selected.map(c => c.id);
    this.gcService.approval(ids).subscribe({
      next: () => {
        this.notify.showSuccess(`Phê duyệt ${ids.length} bản ghi thành công!`);
        this.selected = []; this.showBulkApproveConfirm = false;
        this.bulkApproveLoading = false; this.loadList();
      },
      error: err => {
        this.notify.showError('Lỗi: ' + (err.error?.message || err.message));
        this.bulkApproveLoading = false;
      }
    });
  }

  openBulkRejectDialog(): void  { this.showBulkRejectDialog = true; this.bulkRejectReason = ''; }
  closeBulkRejectDialog(): void { this.showBulkRejectDialog = false; this.bulkRejectReason = ''; }

  confirmBulkReject(): void {
    if (!this.bulkRejectReason.trim()) { this.notify.showWarning('Vui lòng nhập lý do từ chối.'); return; }
    this.bulkRejectLoading = true;
    const ids = this.selected.map(c => c.id);
    this.gcService.reject(ids, this.bulkRejectReason).subscribe({
      next: () => {
        this.notify.showSuccess(`Từ chối ${ids.length} bản ghi thành công!`);
        this.selected = []; this.showBulkRejectDialog = false;
        this.bulkRejectLoading = false; this.loadList();
      },
      error: err => {
        this.notify.showError('Lỗi: ' + (err.error?.message || err.message));
        this.bulkRejectLoading = false;
      }
    });
  }

  openBulkCancelConfirm(): void  { this.showBulkCancelConfirm = true; }
  closeBulkCancelConfirm(): void { this.showBulkCancelConfirm = false; }

  confirmBulkCancel(): void {
    this.bulkCancelLoading = true;
    const ids = this.selected.map(c => c.id);
    this.gcService.cancelApproval(ids).subscribe({
      next: () => {
        this.notify.showSuccess(`Hủy duyệt ${ids.length} bản ghi thành công!`);
        this.selected = []; this.showBulkCancelConfirm = false;
        this.bulkCancelLoading = false; this.loadList();
      },
      error: err => {
        this.notify.showError('Lỗi: ' + (err.error?.message || err.message));
        this.bulkCancelLoading = false;
      }
    });
  }

  // ─── Pagination helper ─────────────────────────────────────────────────────

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const total = this.totalPages, cur = this.currentPage;
    if (total <= 7) {
      for (let i = 0; i < total; i++) pages.push(i);
    } else {
      pages.push(0);
      if (cur > 2) pages.push(-1);
      for (let i = Math.max(1, cur - 1); i <= Math.min(total - 2, cur + 1); i++) pages.push(i);
      if (cur < total - 3) pages.push(-1);
      pages.push(total - 1);
    }
    return pages;
  }
}
