import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuditLog } from '../../../models/component.model';
import { ComponentService } from '../../../core/services/component.service';

@Component({
  selector: 'app-component-history',
  templateUrl: './component-history.component.html',
  styleUrls: ['./component-history.component.css']
})
export class ComponentHistoryComponent implements OnInit {
  componentId!: string;
  historyLogs: AuditLog[] = [];
  loading = false;
  Math = Math;

  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  // Màu avatar cố định per tên
  private avatarColors = ['#0d9488','#3b82f6','#8b5cf6','#f59e0b','#ec4899','#14b8a6','#f97316'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private componentService: ComponentService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.componentId = params['id'];
      this.loadHistory();
    });
  }

  loadHistory(): void {
    this.loading = true;
    this.componentService.getHistory(this.componentId, this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.historyLogs = response.data.content;
          this.totalElements = response.data.totalElements;
          this.totalPages = response.data.totalPages;
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Lỗi khi tải lịch sử:', error);
        this.loading = false;
      }
    });
  }

  onPageChange(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadHistory();
  }

  /** Tạo mảng số trang với dấu "..." khi cần */
  get pageNumbers(): number[] {
    const pages: number[] = [];
    const total = this.totalPages;
    const cur = this.currentPage;
    if (total <= 7) {
      for (let i = 0; i < total; i++) pages.push(i);
    } else {
      pages.push(0);
      if (cur > 2) pages.push(-1); // ellipsis
      for (let i = Math.max(1, cur - 1); i <= Math.min(total - 2, cur + 1); i++) pages.push(i);
      if (cur < total - 3) pages.push(-1); // ellipsis
      pages.push(total - 1);
    }
    return pages;
  }

  getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    return name.slice(0, 2).toUpperCase();
  }

  getAvatarColor(name: string): string {
    let hash = 0;
    for (const c of (name || '')) hash = ((hash << 5) - hash) + c.charCodeAt(0);
    return this.avatarColors[Math.abs(hash) % this.avatarColors.length];
  }

  getActionClass(action: string): string {
    const map: Record<string, string> = {
      'Thêm mới': 'action-create',
      'Sửa': 'action-edit',
      'Xóa': 'action-delete',
      'Gửi duyệt': 'action-submit',
      'Phê duyệt': 'action-approve',
      'Từ chối': 'action-reject',
      'Hủy phê duyệt': 'action-cancel',
      'Xóa bản ghi': 'action-delete',
    };
    return map[action] ?? 'action-default';
  }

  onBack(): void {
    this.router.navigate(['/components']);
  }
}
