# Quản lý sửa chữa xe máy

Bài tập lớn môn Phát triển ứng dụng trên thiết bị di động, phát triển từ dự án quản lý nhân sự `SQLiteK24DTCN533` được cung cấp. Ứng dụng dùng **Java, layout XML, AppCompatActivity, ListView và SQLiteOpenHelper**; dữ liệu lưu trên thiết bị, không cần máy chủ hoặc tài khoản.

Thông tin sinh viên trên giao diện được giữ từ nguồn gốc: **K24DTCN533 – Nguyễn Hoài Sơn**. Có thể sửa trong `app/src/main/res/values/strings.xml`.

## 1. Mở và chạy bằng Android Studio

1. Giải nén ZIP. Chọn **File → Open**, mở thư mục `QuanLySuaChuaXeMay` chứa `settings.gradle.kts`. Không chỉ mở thư mục `app`.
2. Dùng môi trường Android Studio đã mở được dự án gốc; cho phép Gradle Sync tải dependency. Lần đồng bộ đầu cần Internet.
3. Kiểm tra SDK và JDK theo cấu hình kế thừa bên dưới. Cài SDK còn thiếu qua **Tools → SDK Manager**; kiểm tra Gradle JDK tại **Settings → Build, Execution, Deployment → Build Tools → Gradle**. Tệp daemon trong dự án yêu cầu JDK 25; nếu không tải tự động được, cài/chọn JDK phù hợp với yêu cầu đó.
4. Chọn cấu hình **app**, chọn emulator hoặc điện thoại Android API 26 trở lên, nhấn **Run**.
5. Lần đầu ứng dụng không có dữ liệu. Bấm **Tạo mẫu → Đồng ý** để thêm 3 xe và 3 dịch vụ; hoặc tự thêm dữ liệu.

Giữ nguyên các phiên bản trong mã nguồn đầu vào, không thêm thư viện mới:

| Thành phần | Cấu hình trong dự án |
|---|---|
| Android Gradle Plugin | 9.4.1 |
| Gradle Wrapper | 9.6.0, giữ checksum gốc |
| Gradle daemon toolchain | JDK 25, theo `gradle/gradle-daemon-jvm.properties` |
| Java source/target | Java 11 |
| compileSdk / targetSdk | 37 / 37 |
| minSdk | 26 |
| Namespace mã Java | `com.example.sqlitek24dtcn533` |
| Application ID bản mới | `com.example.sqlitek24dtcn533.motorrepair` |

Đây là các giá trị kế thừa, **không phải bộ phiên bản đã được build xác nhận trong môi trường bàn giao**. Nếu Android Studio báo không hỗ trợ AGP hoặc thiếu SDK, cần kiểm tra lại bộ công cụ của dự án gốc. Không sửa ngẫu nhiên riêng một phiên bản Gradle/JDK/AGP vì chúng phụ thuộc lẫn nhau.

Không đóng gói `local.properties`, thư mục `.idea`, `.gradle` hoặc `build` từ máy người tạo. Android Studio sẽ tạo cấu hình SDK cục bộ khi mở dự án. Tên DB mới là `motorcycle_repair.db`; ứng dụng có ID riêng để có thể cài cạnh ứng dụng nhân sự, không chuyển dữ liệu nhân sự thành dữ liệu xe.

## 2. Chức năng theo đề bài

| Yêu cầu | Vị trí thao tác |
|---|---|
| Thêm xe | Tab **Xe → + Thêm xe** |
| Sửa xe | Chạm xe → **Xem / sửa xe → Lưu xe** |
| Xóa xe | Chạm xe → **Xóa xe**, hoặc nút xóa trong màn hình sửa |
| Thêm/sửa/xóa dịch vụ | Tab **Dịch vụ**; thêm mới hoặc chạm một dịch vụ |
| Gán xe vào dịch vụ | Chạm xe → **Gán dịch vụ**, hoặc tab **Phiếu → + Gán xe vào dịch vụ** |
| Liệt kê xe theo hãng | **Báo cáo / Truy vấn → Hãng xe** |
| Tìm xe cần bảo dưỡng | **Báo cáo / Truy vấn → Chỉ xe cần bảo dưỡng** |
| Kết hợp hai truy vấn | Chọn hãng rồi bật **Chỉ xe cần bảo dưỡng** |

Chức năng bổ sung phục vụ trình diễn: tìm kiếm không dấu, dữ liệu mẫu, xem phiếu theo từng xe, ghi chú phiếu, hoàn thành/xóa phiếu, tổng giá dịch vụ đã gán.

### Quy tắc nghiệp vụ đã lựa chọn

- **“Cần bảo dưỡng” là tình trạng do người dùng xác nhận**, không phải dự đoán từ số km/ngày sử dụng. Đề bài không có các trường này; ứng dụng không tự suy ra lịch bảo dưỡng.
- Xe có ba tình trạng: Hoạt động tốt, Cần bảo dưỡng, Đang sửa chữa. Hoàn thành một phiếu **không tự đổi tình trạng xe**, vì xe có thể còn vấn đề khác.
- Một xe có nhiều phiếu dịch vụ; một dịch vụ được dùng cho nhiều xe. Cùng một xe có thể dùng lại dịch vụ sau khi hoàn thành hoặc xóa phiếu trước. Không cho hai phiếu chưa hoàn thành cùng cặp xe–dịch vụ.
- Phiếu lưu mã, tên và giá dịch vụ tại lúc gán. Sửa danh mục dịch vụ không sửa phiếu cũ. Giá là số nguyên VNĐ từ 0 đến 1.000.000.000.
- Mã xe và mã dịch vụ là duy nhất trong từng loại. Biển số duy nhất sau khi bỏ chấm, gạch ngang, khoảng trắng và chuyển thành chữ hoa. Ví dụ `29B1-123.45` và `29b1 12345` là cùng một biển.
- Kiểm tra biển số chỉ là quy tắc nhập liệu của bài tập, không xác minh biển số hợp pháp/đăng ký thực tế.
- Hãng xe có thể tự nhập; truy vấn nhóm theo tên chuẩn hóa không phân biệt hoa/thường và dấu tiếng Việt. Nên dùng cùng một cách viết thương hiệu để tránh các từ đồng nghĩa khác nhau.
- Không xóa xe/dịch vụ đã có phiếu để tránh mất lịch sử. Nếu cần xóa: xem và xóa các phiếu liên quan trước, rồi xóa xe/dịch vụ. Tất cả thao tác xóa đều có xác nhận. Không xóa liên đới âm thầm.
- “Tổng giá đã gán” là tổng giá các phiếu đang còn lưu, gồm cả chưa hoàn thành; **không phải doanh thu hoặc số tiền đã thanh toán**. Đề bài không có nghiệp vụ thanh toán.
- Dữ liệu mẫu chỉ tạo khi cả ba bảng đều rỗng, không ghi đè dữ liệu đã nhập.

## 3. Kịch bản demo khoảng 5 phút

1. Bấm **Tạo mẫu**. Có XE001 Honda cần bảo dưỡng, XE002 Yamaha hoạt động tốt, XE003 Honda đang sửa chữa; có 3 dịch vụ.
2. Thêm XE004, biển `30G1-456.78`, hãng Suzuki, tình trạng Cần bảo dưỡng. Mở lại để sửa tình trạng; thử nhập trùng mã/biển số và quan sát thông báo.
3. Thêm DV004 “Thay bugi”, giá `90000`; mở lại sửa giá hoặc tên.
4. Chọn XE001 → Gán dịch vụ → DV001, ghi chú “Kiểm tra dầu cũ”. Vào **Phiếu** để xem giá `120.000 đ`.
5. Sửa giá DV001 thành `150000`. Quay lại phiếu cũ: giá vẫn `120.000 đ`. Thử gán trùng khi phiếu chưa hoàn thành: bị từ chối.
6. Hoàn thành phiếu, gán lại cùng dịch vụ: phiếu mới có giá `150.000 đ`. Tình trạng XE001 vẫn Cần bảo dưỡng, chỉ thay đổi khi người dùng lưu lại thông tin xe.
7. Vào **Báo cáo / Truy vấn**, chọn Honda; bật/tắt Chỉ xe cần bảo dưỡng. Kiểm tra danh sách và số kết quả.
8. Thử xóa dịch vụ đã có phiếu: bị chặn. Xóa phiếu thử nghiệm, sau đó xóa dịch vụ hoặc xe chưa được sử dụng.
9. Đóng/mở lại ứng dụng để kiểm tra dữ liệu vẫn còn.

## 4. Cấu trúc mã nguồn

| Tệp/lớp | Trách nhiệm |
|---|---|
| `MainActivity.java` | Ba tab Xe, Dịch vụ, Phiếu; tìm kiếm, menu thao tác và dữ liệu mẫu |
| `VehicleEditActivity.java` | Form thêm/sửa/xóa xe |
| `ServiceEditActivity.java` | Form thêm/sửa/xóa dịch vụ |
| `AssignServiceActivity.java` | Chọn xe, dịch vụ, ghi chú và tạo phiếu |
| `ReportActivity.java` | Lọc theo hãng và tình trạng cần bảo dưỡng |
| `DatabaseHelper.java` | Lược đồ SQLite, CRUD, truy vấn, transaction và khóa ngoại |
| `Vehicle.java`, `ServiceItem.java`, `RepairOrder.java` | Mô hình dữ liệu |
| `InputRules.java` | Chuẩn hóa tiếng Việt, kiểm tra mã/biển số/giá |
| `BaseActivity.java` | Điều hướng trở về, thông báo, lỗi DB, xử lý vùng thanh hệ thống/bàn phím |
| `RowAdapter.java` | Hiển thị các dòng danh sách |
| `res/layout` | Layout XML của từng màn hình |
| `docs/THIET_KE_VA_KIEM_THU.md` | Thiết kế dữ liệu, SQL truy vấn và checklist kiểm thử |
| `tools/check_offline.py` | Kiểm tra độc lập không cần Android SDK |

Chuyển đổi từ dự án gốc: `Staff` → `Vehicle`; luồng `AddStaff`/`StaffDetail` được gộp vào `VehicleEditActivity` theo ID; giữ mô hình `MainActivity` + `ListView` + `SQLiteOpenHelper`, rồi bổ sung dịch vụ, phiếu và báo cáo. Các lớp ContentProvider rỗng, menu, layout và trường nhân sự không dùng đã được loại bỏ. Không yêu cầu ảnh hoặc quyền truy cập tệp vì đề bài xe không có ảnh.

## 5. Kiểm thử và giới hạn xác minh

Đã chạy trong môi trường bàn giao:

- 19 kiểm tra trên SQLite bằng đúng DDL trích từ `DatabaseHelper.java`: ràng buộc, truy vấn, khóa ngoại, giá lưu theo phiếu và gán lại sau hoàn thành.
- 19 kiểm tra Java cho chuẩn hóa, dữ liệu hợp lệ/không hợp lệ và tình trạng xe.
- Parse cú pháp 14 tệp Java; kiểm tra XML, tham chiếu resource và các Activity trong manifest.

**Chưa build APK, chưa chạy emulator/điện thoại và chưa xác minh giao diện bằng Android runtime.** Môi trường bàn giao thiếu Android SDK và không truy cập được máy chủ tải Gradle. Kiểm tra cú pháp không thay thế kiểm tra kiểu/API Android hoặc build hoàn chỉnh.

Chạy lại kiểm tra ngoại tuyến từ thư mục dự án với Python 3 và JDK:

```text
python tools/check_offline.py
```

Trên Windows, sau khi Gradle Sync thành công:

```text
gradlew.bat assembleDebug testDebugUnitTest
```

Khi đã chạy emulator hoặc kết nối điện thoại:

```text
gradlew.bat connectedDebugAndroidTest
```

Trên macOS/Linux thay `gradlew.bat` bằng `./gradlew`. Bộ kiểm thử thiết bị dùng DB `motorcycle_isolated_test.db`, không xóa DB chính. Chi tiết checklist thủ công và các test có trong `docs/THIET_KE_VA_KIEM_THU.md`.

Ứng dụng này phục vụ bài tập với lượng dữ liệu nhỏ. Chưa bao gồm đăng nhập, đồng bộ nhiều thiết bị, lịch bảo dưỡng theo km/ngày, hóa đơn hoặc thanh toán. Các thao tác DB hiện chạy đồng bộ như dự án gốc; với dữ liệu lớn cần chuyển truy vấn khỏi main thread và thêm phân trang.
