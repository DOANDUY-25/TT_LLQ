import { Component, OnInit } from '@angular/core';
import { TransactionLog, TransactionLogSearchDTO, TransactionStatusOptions } from '../../../models/transaction-log.model';
import { TransactionLogService } from '../../../services/transaction-log.service';

@Component({
  selector: 'app-transaction-log-list',
  templateUrl: './transaction-log-list.component.html',
  styleUrls: ['./transaction-log-list.component.css']
})
export class TransactionLogListComponent implements OnInit {

  // Data
  items: TransactionLog[] = [];
  Math = Math;

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  // Filter
  filter: TransactionLogSearchDTO = {};
  statusOptions = TransactionStatusOptions;

  // State
  loading = false;
  exporting = false;

  constructor(private service: TransactionLogService) {}

  ngOnInit(): void {
    this.loadList();
  }

  // ─── Load Data ─────────────────────────────────────────────────────────────

  loadList(): void {
    this.loading = true;
    this.service.search(this.filter, this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.items = response.data.content || [];
          this.totalElements = response.data.totalElements || 0;
          this.totalPages = response.data.totalPages || 0;
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Lỗi khi tải danh sách:', err);
        this.loading = false;
      }
    });
  }

  // ─── Search ────────────────────────────────────────────────────────────────

  onSearch(): void {
    this.currentPage = 0;
    this.loadList();
  }

  onResetFilter(): void {
    this.filter = {};
    this.currentPage = 0;
    this.loadList();
  }

  // ─── Export ────────────────────────────────────────────────────────────────

  onExport(): void {
    this.exporting = true;
    this.service.exportFile(this.filter).subscribe({
      next: (response) => {
        if (response.success) {
          alert(response.message || 'Đang trong quá trình triển khai');
        }
        this.exporting = false;
      },
      error: (err) => {
        console.error('Lỗi khi xuất file:', err);
        alert('Có lỗi xảy ra khi xuất file');
        this.exporting = false;
      }
    });
  }

  // ─── Pagination ────────────────────────────────────────────────────────────

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadList();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadList();
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    const half = Math.floor(maxVisible / 2);

    let start = Math.max(0, this.currentPage - half);
    let end = Math.min(this.totalPages - 1, start + maxVisible - 1);

    if (end - start < maxVisible - 1) {
      start = Math.max(0, end - maxVisible + 1);
    }

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }

    return pages;
  }

  // ─── Helpers ───────────────────────────────────────────────────────────────

  getStatusLabel(status: string): string {
    const option = this.statusOptions.find(o => o.value === status);
    return option ? option.label : status;
  }

  getStatusClass(status: string): string {
    const map: { [key: string]: string } = {
      'SUCCESS': 'status-approved',
      'PENDING': 'status-pending',
      'FAILED': 'status-rejected',
      'CANCELLED': 'status-cancelled'
    };
    return map[status] || 'status-new';
  }

  formatNumber(value: number | null | undefined): string {
    if (value == null) return '0';
    return new Intl.NumberFormat('vi-VN').format(value);
  }

  formatDateTime(dateStr: string | null | undefined): string {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString('vi-VN');
  }
}
