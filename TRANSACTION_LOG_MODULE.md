# Transaction Log Module - Lịch sử giao dịch

## 📋 Tổng quan

Module quản lý và xuất file lịch sử giao dịch - Chỉ có tính năng **Tìm kiếm** và **Xuất file** (không có CRUD).

## 🗄️ Database Schema

### Bảng: TRANSACTION_LOG

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| ID | NUMBER | No | SEQ | Primary Key |
| TRANSACTION_CODE | VARCHAR2(255) | Yes | - | Mã giao dịch |
| ACCOUNT_NO | VARCHAR2(255) | Yes | - | Số tài khoản |
| AMOUNT | NUMBER(38,2) | Yes | - | Số tiền |
| STATUS | VARCHAR2(255) | Yes | - | Trạng thái (SUCCESS/PENDING/FAILED/CANCELLED) |
| CREATED_AT | TIMESTAMP(6) | No | CURRENT_TIMESTAMP | Ngày tạo |
| DESCRIPTION | VARCHAR2(255) | Yes | - | Mô tả |
| REFERENCE_NO | VARCHAR2(255) | Yes | - | Số tham chiếu |
| UPDATED_AT | TIMESTAMP(6) | Yes | - | Ngày cập nhật |

## 🔧 Backend Structure

### 1. Entity
**File:** `TransactionLog.java`
```java
@Entity
@Table(name = "TRANSACTION_LOG")
public class TransactionLog {
    @Id
    private Long id;
    private String transactionCode;
    private String accountNo;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    private String description;
    private String referenceNo;
    private LocalDateTime updatedAt;
}
```

### 2. DTOs
- **TransactionLogSearchDTO.java** - Search criteria
  - `transactionCode`: Mã giao dịch
  - `accountNo`: Số tài khoản  
  - `status`: Trạng thái
  - `createdAtFrom`: Từ ngày
  - `createdAtTo`: Đến ngày
  - `referenceNo`: Số tham chiếu

### 3. Repository
**File:** `TransactionLogRepository.java`
```java
public interface TransactionLogRepository 
    extends JpaRepository<TransactionLog, Long>, 
            JpaSpecificationExecutor<TransactionLog> {
}
```

### 4. Specification
**File:** `TransactionLogSpecification.java`
- LIKE search: `transactionCode`, `accountNo`, `referenceNo`
- Equal search: `status`
- Date range: `createdAtFrom` - `createdAtTo`

### 5. Service
**File:** `TransactionLogService.java`
```java
// Tìm kiếm với phân trang
Page<TransactionLog> search(TransactionLogSearchDTO, Pageable)

// Xuất file (chưa implement)
String exportToFile(TransactionLogSearchDTO)
```

### 6. Controller
**File:** `TransactionLogController.java`
```
POST /api/transaction-logs/search
  - Tìm kiếm với phân trang
  - Roles: ADMIN, MAKER, CHECKER, VIEWER

POST /api/transaction-logs/export
  - Xuất file (trả về message "Đang trong quá trình triển khai")
  - Roles: ADMIN, MAKER, CHECKER
```

## 🎨 Frontend Structure

### 1. Component
**Path:** `features/transaction-logs/transaction-log-list/`

**Files:**
- `transaction-log-list.component.html` - Template
- `transaction-log-list.component.ts` - Logic
- `transaction-log-list.component.css` - Styles

### 2. Model
**File:** `models/transaction-log.model.ts`
```typescript
interface TransactionLog {
  id, transactionCode, accountNo, amount, 
  status, createdAt, description, referenceNo, updatedAt
}

interface TransactionLogSearchDTO {
  transactionCode?, accountNo?, status?,
  createdAtFrom?, createdAtTo?, referenceNo?
}
```

### 3. Service
**File:** `services/transaction-log.service.ts`
```typescript
search(searchDTO, page, size, sortBy, sortDir)
exportFile(searchDTO)
```

### 4. Routing
**Route:** `/transaction-logs`
- Có AuthGuard
- Menu item: "Lịch sử giao dịch"

## 🎯 Tính năng

### ✅ Đã hoàn thành

1. **Tìm kiếm (Search)**
   - ✅ Mã giao dịch (text input)
   - ✅ Số tài khoản (text input)
   - ✅ Số tham chiếu (text input)
   - ✅ Trạng thái (dropdown: SUCCESS/PENDING/FAILED/CANCELLED)
   - ✅ Từ ngày - Đến ngày (datetime picker)
   - ✅ Nút "Làm mới" và "Tìm kiếm"

2. **Hiển thị dữ liệu**
   - ✅ Table với 8 columns:
     - STT
     - Mã giao dịch (monospace font, teal color)
     - Số tài khoản
     - Số tiền (right-aligned, formatted)
     - Trạng thái (badge với màu)
     - Ngày tạo (datetime format)
     - Số tham chiếu
     - Mô tả
   - ✅ Pagination (10/20/50/100 per page)
   - ✅ Loading state
   - ✅ Empty state

3. **Xuất file**
   - ✅ Nút "Xuất file" màu cam (gradient orange)
   - ✅ Icon download
   - ✅ Disabled state khi đang xuất
   - ✅ Message: "Đang trong quá trình triển khai"

### ⏳ Chưa implement

- ❌ Logic xuất file thực tế (Excel/CSV)
- ❌ Progress indicator khi xuất
- ❌ Download file tự động

## 🎨 UI Design

### Color Scheme
- **Export button:** Orange gradient `#f59e0b` → `#f97316`
- **Status badges:**
  - SUCCESS: Green `#d1fae5` / `#065f46`
  - PENDING: Yellow `#fef3c7` / `#92400e`
  - FAILED: Red `#fee2e2` / `#991b1b`
  - CANCELLED: Gray `#f3f4f6` / `#374151`

### Layout
```
┌─────────────────────────────────────────┐
│ Lịch sử giao dịch      [Xuất file 🔶]  │
├─────────────────────────────────────────┤
│ 🔍 Bộ lọc tìm kiếm                      │
│                                          │
│ [Mã GD] [Số TK] [Số TC] [Trạng thái]   │
│ [Từ ngày] [Đến ngày]                    │
│                [Làm mới] [Tìm kiếm]    │
├─────────────────────────────────────────┤
│ 📄 Danh sách giao dịch   Tổng: 150     │
│                                          │
│ STT | Mã GD | Số TK | ... | Mô tả      │
│  1  | TX001 | 12345 | ... | Payment    │
│  2  | TX002 | 67890 | ... | Transfer   │
│                                          │
│ Hiển thị 1-10 trong 150  [10/page] [...│
└─────────────────────────────────────────┘
```

### Font & Sizing
- Input height: **32px** (compact design)
- Font sizes: **12-13px**
- Table padding: **10px 14px**
- Border radius: **6-8px**

## 📦 Files Created

### Backend (6 files)
```
src/main/java/com/pmh/
├── model/
│   ├── entity/TransactionLog.java
│   └── dto/TransactionLogSearchDTO.java
├── repository/TransactionLogRepository.java
├── specification/TransactionLogSpecification.java
├── service/TransactionLogService.java
└── controller/TransactionLogController.java
```

### Frontend (4 files)
```
src/app/
├── features/transaction-logs/transaction-log-list/
│   ├── transaction-log-list.component.html
│   ├── transaction-log-list.component.ts
│   └── transaction-log-list.component.css
├── models/transaction-log.model.ts
└── services/transaction-log.service.ts
```

### Configuration (2 files)
```
src/app/
├── app.module.ts (updated - added component)
├── app-routing.module.ts (updated - added route)
└── app.component.html (updated - added menu item)
```

## ✅ Build Status

### Backend
```bash
.\mvnw compile
✓ SUCCESS
Exit Code: 0
```

### Frontend
```bash
ng build
✓ SUCCESS
Exit Code: 0
```

## 🚀 Next Steps (Future Implementation)

1. **Excel Export Logic**
   ```java
   // Sử dụng Apache POI
   public ByteArrayInputStream exportToExcel(List<TransactionLog> data) {
     // Create workbook
     // Write headers & data
     // Return ByteArrayInputStream
   }
   ```

2. **CSV Export**
   ```java
   public String exportToCsv(List<TransactionLog> data) {
     // CSV header
     // CSV rows
     // Return CSV string
   }
   ```

3. **Download Handler (Frontend)**
   ```typescript
   exportFile() {
     this.service.exportFile(filter).subscribe(blob => {
       const url = window.URL.createObjectURL(blob);
       const a = document.createElement('a');
       a.href = url;
       a.download = 'transactions.xlsx';
       a.click();
     });
   }
   ```

## 📝 Notes

- ✅ Module chỉ **READ-ONLY** - không có Create/Update/Delete
- ✅ Có permission check: Viewer cũng có thể xem
- ✅ Export button chỉ cho ADMIN/MAKER/CHECKER
- ✅ UI compact design nhất quán với các module khác
- ⏳ Export logic sẽ được implement trong sprint tiếp theo
