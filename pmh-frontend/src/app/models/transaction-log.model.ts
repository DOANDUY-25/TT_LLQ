export interface TransactionLog {
  id: number;
  transactionCode: string;
  accountNo: string;
  amount: number;
  status: string;
  createdAt: string;
  description: string;
  referenceNo: string;
  updatedAt: string;
}

export interface TransactionLogSearchDTO {
  transactionCode?: string;
  accountNo?: string;
  status?: string;
  createdAtFrom?: string;
  createdAtTo?: string;
  referenceNo?: string;
}

export interface TransactionLogSearchResponse {
  content: TransactionLog[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// Status options
export const TransactionStatusOptions = [
  { value: 'SUCCESS', label: 'Thành công' },
  { value: 'PENDING', label: 'Đang xử lý' },
  { value: 'FAILED', label: 'Thất bại' },
  { value: 'CANCELLED', label: 'Đã hủy' }
];
