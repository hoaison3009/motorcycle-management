# Thiết kế dữ liệu và kiểm thử

## 1. Mô hình quan hệ

`vehicles 1 — N repair_orders N — 1 services`

Bảng phiếu giải quyết quan hệ nhiều–nhiều giữa xe và dịch vụ, đồng thời cho phép lưu nhiều lần sử dụng cùng dịch vụ theo thời gian.

| Bảng | Cột | Ý nghĩa |
|---|---|---|
| vehicles | id | Khóa nội bộ tự tăng, không cho người dùng sửa |
| vehicles | code | Mã xe, duy nhất không phân biệt hoa/thường ASCII |
| vehicles | plate / plate_key | Biển số hiển thị / biển số chuẩn hóa duy nhất |
| vehicles | brand / brand_key | Hãng hiển thị / khóa lọc nhóm hãng |
| vehicles | status | NORMAL, MAINTENANCE hoặc REPAIRING |
| vehicles | search_key | Chuỗi chuẩn hóa mã, biển số, hãng để tìm không dấu |
| services | id, code, name | Khóa nội bộ, mã duy nhất, tên dịch vụ |
| services | price | Giá danh mục hiện tại, số nguyên VNĐ |
| services | search_key | Chuỗi tìm kiếm không dấu |
| repair_orders | id | Mã phiếu nội bộ tự tăng |
| repair_orders | vehicle_id / service_id | Khóa ngoại tới xe / dịch vụ |
| repair_orders | service_code / service_name / price | Bản chụp thông tin dịch vụ lúc gán |
| repair_orders | assigned_at / completed_at | Thời gian epoch milliseconds; hoàn thành có thể NULL |
| repair_orders | status | PENDING / COMPLETED |
| repair_orders | note | Ghi chú tùy chọn, giới hạn 500 ký tự ở tầng ứng dụng |

Khóa ngoại được bật ở `onConfigure()` cho mỗi kết nối SQLite. `ON DELETE RESTRICT` ngăn xóa xe/dịch vụ còn được tham chiếu. Unique partial index bảo vệ cặp xe–dịch vụ đang có phiếu PENDING. Ràng buộc CHECK bảo vệ trạng thái và khoảng giá. Nội dung do người dùng nhập được truyền bằng tham số, không nối trực tiếp vào câu SQL.

Thao tác `assign()` dùng transaction: đọc xe/dịch vụ còn tồn tại → lấy giá hiện tại → tạo phiếu → commit. Nếu vi phạm ràng buộc thì rollback. Việc sửa dịch vụ sau đó không sửa bản chụp trên phiếu.

## 2. SQL phục vụ thuyết trình

Giá trị `brand_key` được chuẩn hóa bằng Java trước khi truyền vào truy vấn.

### Liệt kê xe theo hãng

```sql
SELECT * FROM vehicles
WHERE brand_key = ?
ORDER BY id DESC;
```

Ví dụ tham số: `honda`.

### Tìm xe cần bảo dưỡng

```sql
SELECT * FROM vehicles
WHERE status = 'MAINTENANCE'
ORDER BY id DESC;
```

### Kết hợp hãng và tình trạng

```sql
SELECT * FROM vehicles
WHERE brand_key = ? AND status = 'MAINTENANCE'
ORDER BY id DESC;
```

### Xem các phiếu của một xe

```sql
SELECT r.*, v.code AS vehicle_code, v.plate
FROM repair_orders r
JOIN vehicles v ON v.id = r.vehicle_id
WHERE r.vehicle_id = ?
ORDER BY r.id DESC;
```

### Ngăn gán trùng dịch vụ đang làm

```sql
CREATE UNIQUE INDEX idx_pending_pair
ON repair_orders(vehicle_id, service_id)
WHERE status = 'PENDING';
```

Không gắn unique vào toàn bộ cặp xe–dịch vụ, vì như vậy xe sẽ không thể thay dầu hoặc dùng lại dịch vụ lần sau.

## 3. Kiểm thử tự động đã chuẩn bị

- `app/src/test/.../InputRulesTest.java`: 8 ca JUnit về chuẩn hóa, giá và mã/biển số không hợp lệ.
- `app/src/androidTest/.../DatabaseHelperTest.java`: 8 ca kiểm thử có nhiều assertion, chạy với SQLite Android thật; CRUD, truy vấn theo hãng/tình trạng, giá bản chụp, gán trùng, khóa ngoại, khởi động lại DB và dữ liệu mẫu.
- `tools/check_offline.py`: kiểm tra SQLite trên máy tính với DDL trích từ mã Java, cấu trúc XML, tham chiếu resource, parser cú pháp Java và chạy các quy tắc Java thuần. Đây không phải kiểm thử UI hoặc chứng nhận APK đã build.

## 4. Checklist trên emulator/điện thoại

Các mục dưới đây là kế hoạch kiểm thử thủ công, chưa đánh dấu là đã chạy.

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| UI01 | Mở ứng dụng mới | Danh sách rỗng, không lỗi; có nút thêm và tạo mẫu |
| UI02 | Tạo mẫu hai lần | Lần đầu có 3 xe/3 dịch vụ; lần hai bị từ chối, không nhân đôi |
| UI03 | Thêm xe hợp lệ, quay lại | Xe xuất hiện và dữ liệu được lưu |
| UI04 | Bỏ trống mã/biển/hãng hoặc nhập mã có khoảng trắng | Không lưu; có thông báo kiểm tra dữ liệu |
| UI05 | Thêm mã trùng khác hoa/thường | Bị từ chối |
| UI06 | Nhập lại biển số khác cách dùng dấu chấm/gạch | Bị từ chối là cùng biển số |
| UI07 | Sửa hãng và tình trạng xe | Danh sách/báo cáo phản ánh giá trị mới |
| UI08 | Xóa xe chưa có phiếu, chọn Hủy rồi Đồng ý | Hủy giữ nguyên; Đồng ý xóa |
| UI09 | Thêm/sửa/xóa dịch vụ chưa có phiếu | Thành công; danh sách cập nhật |
| UI10 | Giá rỗng, âm, thập phân, quá giới hạn | Không lưu; giá 0 và 120000 được chấp nhận |
| UI11 | Gán khi chưa có xe/dịch vụ | Hiển thị hướng dẫn, nút lưu vô hiệu hóa |
| UI12 | Gán một xe nhiều dịch vụ, một dịch vụ cho nhiều xe | Tạo các phiếu độc lập |
| UI13 | Gán cùng xe/dịch vụ hai lần khi chưa hoàn thành | Lần thứ hai bị từ chối |
| UI14 | Sửa tên/giá danh mục sau khi gán | Phiếu cũ giữ tên/giá; phiếu mới dùng giá mới |
| UI15 | Hoàn thành phiếu rồi gán lại | Cho phép; tình trạng xe không tự đổi |
| UI16 | Xóa xe/dịch vụ đang có phiếu | Bị chặn, hướng dẫn kiểm tra các phiếu liên quan |
| UI17 | Xóa phiếu | Chỉ phiếu bị xóa; xe/dịch vụ còn nguyên |
| UI18 | Lọc Honda, chỉ bảo dưỡng, kết hợp cả hai, bỏ lọc | Kết quả đúng với dữ liệu mẫu và dữ liệu đã sửa |
| UI19 | Tìm “thay dau” hoặc “honda” | Tìm được dữ liệu có dấu/khác hoa thường tương ứng |
| UI20 | Nhập `%`, `_`, dấu nháy vào ô tìm kiếm | Được coi là ký tự tìm kiếm, không làm đổi cấu trúc truy vấn |
| UI21 | Trở về, xoay màn hình khi nhập form | Không lưu ngoài ý muốn; các ô có ID giữ dữ liệu đang nhập |
| UI22 | Mở bàn phím, cuộn form, thiết bị có thanh điều hướng | Nút/ô không bị che; có thể thao tác trở về |
| UI23 | Đóng hẳn rồi mở ứng dụng | Dữ liệu SQLite vẫn còn |
| UI24 | Cài cùng ứng dụng quản lý nhân sự | Hai ứng dụng khác ID; dữ liệu độc lập |
| UI25 | Chọn “Xem phiếu của xe” rồi “Xem phiếu của tất cả xe” | Bộ lọc phiếu chuyển đúng |

## 5. Ghi chú mở rộng

Nếu giảng viên yêu cầu xác định bảo dưỡng theo lịch, cần bổ sung ít nhất số km hiện tại, số km/ngày bảo dưỡng gần nhất và chu kỳ theo từng hạng mục. Không thể suy ra chính xác “đến hạn bảo dưỡng” chỉ từ mã, biển số, hãng và tình trạng tự nhập. Nếu cần thống kê doanh thu, bổ sung trạng thái thanh toán, hóa đơn và thời điểm thu tiền; không dùng tổng giá các phiếu làm doanh thu.
