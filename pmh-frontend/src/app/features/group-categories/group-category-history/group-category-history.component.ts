import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuditLog } from '../../../models/group-category.model';
import { GroupCategoryService } from '../../../core/services/group-category.service';

@Component({
  selector: 'app-group-category-history',
  templateUrl: './group-category-history.component.html',
  styleUrls: ['./group-category-history.component.scss']
})
export class GroupCategoryHistoryComponent implements OnInit {
  id!: number;
  logs: AuditLog[] = [];
  loading = false;
  Math = Math;

  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  private avatarColors = ['#0d9488','#3b82f6','#8b5cf6','#f59e0b','#ec4899','#f97316','#14b8a6'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private gcService: GroupCategoryService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(p => { this.id = +p['id']; this.loadHistory(); });
  }

  loadHistory(): void {
    this.loading = true;
    this.gcService.getHistory(this.id, this.currentPage, this.pageSize).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.logs = res.data.content;
          this.totalElements = res.data.totalElements;
          this.totalPages = res.data.totalPages;
        }
        this.loading = false;
      },
      error: err => { console.error(err); this.loading = false; }
    });
  }

  onPageChange(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadHistory();
  }

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

  getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.trim().split(/\s+/);
    return parts.length >= 2
      ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
      : name.slice(0, 2).toUpperCase();
  }

  getAvatarColor(name: string): string {
    let hash = 0;
    for (const c of (name || '')) hash = ((hash << 5) - hash) + c.charCodeAt(0);
    return this.avatarColors[Math.abs(hash) % this.avatarColors.length];
  }

  getActionClass(action: string): string {
    const map: Record<string, string> = {
      'THÊM MỚI': 'action-create', 'SAO CHÉP': 'action-create',
      'CẬP NHẬT': 'action-edit',   'SỬA': 'action-edit',
      'XÓA': 'action-delete',
      'GỬI DUYỆT': 'action-submit',
      'PHÊ DUYỆT': 'action-approve',
      'TỪ CHỐI': 'action-reject',
      'HỦY DUYỆT': 'action-cancel'
    };
    return map[action?.toUpperCase()] ?? 'action-default';
  }

  onBack(): void { this.router.navigate(['/group-categories']); }
}
