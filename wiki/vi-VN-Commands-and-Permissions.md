# Lệnh và Quyền Hạn (Commands and Permissions)

Dưới đây là một danh sách hoàn chỉnh tất cả các Lệnh và Quyền (Permissions) hiện có trong Storage plugin.

## Các Lệnh

### Lệnh Cơ Bản Cho Người Chơi (User Commands)
| Lệnh | Mô tả                                        | Quyền Yêu Cầu (Permission) |
|---------|----------------------------------------------|------------|
| `/storage` hoặc `/kho` | Mở GUI chính                                 | - |
| `/storage help` | Bảng hướng dẫn rút gọn                       | - |
| `/storage toggle` | Bật/Tắt tự động nhặt                         | `storage.toggle` |
| `/storage groundstore` | Bật/Tắt chế độ nhặt vật phẩm dưới đất        | `storage.groundstore` |
| `/storage autosell` | Bật/Tắt tự động bán theo từng item           | `storage.autosell` |
| `/storage convert` | Mở GUI chuyển đổi khoáng sản (Ingot ↔ Block) | `storage.convert` |
| `/storage craft` | Mở GUI Chế Tạo                               | `storage.craft.use` |
| `/storage view <người_chơi>` | Xem kho của người chơi khác                  | `storage.view` |
| `/storage transfer <người_chơi> <vật_liệu>` | Chuyển tài nguyên cho người khác             | `storage.transfer.use` |
| `/storage transfer multi <người_chơi>` | Chuyển nhiều tài nguyên cho người chơi khác  | `storage.transfer.multi` |
| `/storage transfer log [player] [page]` | Xem lịch sử Chuyển/Nhận                      | `storage.transfer.log` |
| `/storage deposit <item> [amount\|all]` | Gửi vật phẩm từ túi đồ vào Storage           | `storage.deposit` |
| `/storage withdraw <item> [amount\|all]` | Rút vật phẩm từ Storage ra túi đồ            | `storage.withdraw` |
| `/storage sell <item> [amount\|all]` | Bán vật phẩm từ Storage                      | `storage.sell` |

### Lệnh Admin (Admin Commands)
| Lệnh                                             | Mô tả                                           | Quyền Yêu Cầu (Permission) |
|-----------------------------------------------------|-------------------------------------------------|------------|
| `/storage reload`                                   | Tải lại tất cả File Cấu Hình                    | `storage.admin.reload` |
| `/storage max <người_chơi> <số_lượng>`                    | Đặt lại sức chứa tối đa cho Player              | `storage.admin.max` |
| `/storage add <vật_liệu;data> <người_chơi> <số_lượng>`    | Cộng số lượng vật phẩm cho Player               | `storage.admin.add` |
| `/storage remove <vật_liệu;data> <người_chơi> <số_lượng>` | Trừ số vật phẩm từ Storage của Player           | `storage.admin.remove` |
| `/storage set <vật_liệu;data> <người_chơi> <số_lượng>`    | Đặt số lượng vật phẩm chính xác mặc định        | `storage.admin.set` |
| `/storage reset <vật_liệu\|all> [người_chơi]`           | Reset 1 tài nguyên/tất cả tài nguyên của Player | `storage.admin.reset` |
| `/storage autosave`                                 | Xem trạng thái tự động lưu database             | `storage.admin.reload` |
| `/storage save`                                     | Buộc lưu trữ Data                               | `storage.admin.reload` |
| `/storage crafteditor`                              | Mở GUI editor Công thức Chế Tạo                 | `storage.craft.admin` |
| `/storage crafteditor import`                       | Nhập vật phẩm đang cầm trên tay vào CraftEditor | `storage.craft.admin` |

### Lệnh Hệ Thống Sự Kiện (Event Commands)
| Lệnh | Mô tả                            | Quyền Yêu Cầu (Permission) |
|---------|----------------------------------|------------|
| `/storage event list` | Danh sách Event đang kích hoạt   | `storage.event.view` |
| `/storage event start <event>` | Bắt đầu Event bằng thủ công      | `storage.event.admin` |
| `/storage event stop <event>` | Chặn/Ngưng một event bằng thủ công | `storage.event.admin` |

**Sự kiện hỗ trợ hiện tại**: `mining_contest` (Thi đào block), `double_drop` (Nhân đôi đồ rơi), `community_event` (Góp chung điểm cộng đồng)

### Lệnh Phù Phép (Custom Enchant Commands)
| Lệnh | Mô tả                                | Quyền Yêu Cầu |
|---------|--------------------------------------|-------------------------|
| `/storage enchant give <enchant> <level>` | Phù phép vật phẩm đang cầm trên tay  | `storage.admin.enchant` |
| `/storage enchant remove <enchant>` | Gỡ Phù Phép đang cầm trên tay        | `storage.admin.enchant` |
| `/storage enchant list` | Danh sách Phù phép có sẵn            | `storage.admin.enchant` |
| `/storage enchant info <enchant>` | Hiển thị thông tin chi tiết phù phép | `storage.admin.enchant` |
| `/storage enchant setmaxlevel <enchant> <level>` | Giới hạn Cấp bậc cho Phù phép        | `storage.admin.enchant` |

**Phù phép hỗ trợ hiện tại**: `tnt`, `haste`, `multiplier`, `veinminer`

### Lệnh Tài Nguyên Đặc Biệt (Special Material Commands)
| Lệnh | Mô tả                                   | Quyền Yêu Cầu |
|---------|-----------------------------------------|------------|
| `/storage specialmaterial list` | Danh sách toàn bộ Tài Nguyên Đặc Biệt   | `storage.admin.specialmaterial` |
| `/storage specialmaterial info <material>` | Hiển thị thông tin chi tiết             | `storage.admin.specialmaterial` |
| `/storage specialmaterial give <player> <material> <amount>`| Give tài nguyên đặc biệt cho người chơi | `storage.admin.specialmaterial` |

### Lệnh Kho Quái Vật MythicMobs (MythicStorage)
| Lệnh | Mô tả                                                                         | Quyền Yêu Cầu |
|---------|-------------------------------------------------------------------------------|------------|
| `/mythicstorage` | Mở GUI MythicStorage                                                          | `storage.mythicstorage.use` |
| `/mythicstorage toggle` | Bật/Tắt nhặt tự động                                                          | `storage.mythicstorage.toggle` |
| `/mythicstorage groundstore` | Bật/Tắt nhặt đồ dưới đất                                                      | `storage.mythicstorage.groundstore` |
| `/mythicstorage view <player>` | Xem kho mythic của người chơi                                                 | `storage.mythicstorage.view` |
| `/mythicstorage transfer <player> <item>` | Chuyển vật phẩm cho người chơi khác                                           | `storage.mythicstorage.transfer` |
| `/mythicstorage transfer multi <player>` | Chuyển nhiều tài nguyên cho người chơi khác                                   | `storage.mythicstorage.transfer.multi` |
| `/mythicstorage transfer log [page]` | Xem nhật ký chuyển hàng Mythic                                                | `storage.mythicstorage.transfer.log` |
| `/mythicstorage transfer log <player> <page>` | Xem lịch sử Chuyển/Nhận                                                       | `storage.mythicstorage.transfer.log.others` |
| `/mythicstorage deposit <item> [amount\|all]` | Gửi vật phẩm từ túi đồ vào MythicStorage                                      | `storage.mythicstorage.use` |
| `/mythicstorage withdraw <item> [amount\|all]` | Rút vật phẩm từ MythicStorage ra túi đồ                                       | `storage.mythicstorage.use` |
| `/mythicstorage add <player> <item> <amount>` | Thêm vật phẩm cho người chơi                                                  | `storage.mythicstorage.admin` |
| `/mythicstorage remove <player> <item> <amount>`| Trừ vật phẩm người chơi                                                       | `storage.mythicstorage.admin` |
| `/mythicstorage set <player> <item> <amount>` | Đặt vật phẩm người chơi                                                       | `storage.mythicstorage.admin` |
| `/mythicstorage max <player> <amount>` | Đặt sức chứa tối đa                                                           | `storage.mythicstorage.admin` |
| `/mythicstorage resetlimit <player>` | Làm mới lại giới hạn sức chứa của người chơi (trong trường hợp lỗi phát sinh) | `storage.mythicstorage.admin` |
| `/mythicstorage reset <player> [item]` | Làm mới lại số lượng vật phẩm của người chơi                                  | `storage.mythicstorage.admin` |
| `/mythicstorage reload` | Tải lại cấu hình                                                              | `storage.mythicstorage.admin` |
| `/mythicstorage help` | Trợ giúp lệnh MythicStorage                                                   | - |

### Lệnh Kho Nông Sản (CropStorage)
| Lệnh | Mô tả                                                                         | Quyền Yêu Cầu |
|---------|-------------------------------------------------------------------------------|------------|
| `/cropstorage` | Mở GUI CropStorage                                                            | `storage.cropstorage.use` |
| `/cropstorage help` | Trợ giúp lệnh CropStorage                                                     | `storage.cropstorage.use` |
| `/cropstorage toggle` | Bật/Tắt tự động nhặt nông sản                                                 | `storage.cropstorage.toggle` |
| `/cropstorage groundstore` | Bật/Tắt nhặt nông sản dưới đất                                                | `storage.cropstorage.groundstore` |
| `/cropstorage status` | Xem trạng thái CropStorage                                                    | `storage.cropstorage.use` |
| `/cropstorage deposit <item> [amount\|all]` | Gửi nông sản từ túi đồ vào CropStorage                                        | `storage.cropstorage.use` |
| `/cropstorage withdraw <item> [amount\|all]` | Rút nông sản từ CropStorage ra túi đồ                                         | `storage.cropstorage.use` |
| `/cropstorage sell <item> [amount\|all]` | Bán nông sản từ CropStorage                                                   | `storage.cropstorage.use` |
| `/cropstorage autosell` | Bật/Tắt tự động bán theo từng item                                            | `storage.cropstorage.autosell` |
| `/cropstorage view <player>` | Xem kho của người chơi khác                                                   | `storage.cropstorage.view` |
| `/cropstorage add <item> <player> <amount>` | Thêm nông sản người chơi                                                      | `storage.cropstorage.admin` |
| `/cropstorage remove <item> <player> <amount>` | Trừ nông sản người chơi                                                       | `storage.cropstorage.admin` |
| `/cropstorage set <item> <player> <amount>` | Đặt số lượng nông sản người chơi                                              | `storage.cropstorage.admin` |
| `/cropstorage max <player> <amount>` | Đặt sức chứa tối đa cho người chơi                                            | `storage.cropstorage.admin` |
| `/cropstorage resetlimit <player>` | Làm mới lại giới hạn sức chứa của người chơi (trong trường hợp lỗi phát sinh) | `storage.cropstorage.admin` |
| `/cropstorage reset <player> [item]` | Làm mới lại nông sản người chơi                                               | `storage.cropstorage.admin` |
| `/cropstorage reload` | Tải lại cấu hình                                                              | `storage.cropstorage.admin` |

---

## Gán Quyền Hạn (Permissions)

### Quyền Của Người Chơi (User Permissions)
| Permission | Mô Tả                                               | Default (Mặc Định) |
|------------|-----------------------------------------------------|---------|
| `storage.toggle` | Quyền autopickup                                    | `true` (Ai cũng có) |
| `storage.groundstore` | Quyền nhặt vật phẩm dưới đất                        | `true` |
| `storage.deposit` | Quyền gửi vật phẩm vào kho                          | `true` |
| `storage.withdraw` | Quyền rút vật phẩm khỏi kho                         | `true` |
| `storage.sell` | Quyền bán vật phẩm trong kho                        | `true` |
| `storage.autosell` | Quyền bán vật phẩm tự động                          | `op` |
| `storage.convert` | Quyền sử dụng tính năng chuyển đổi khoáng sản       | `true`  |
| `storage.view` | Quyền xem kho của người chơi khác                   | `true`  |
| `storage.transfer.use` | Quyền chuyển tài nguyên                             | `true`  |
| `storage.transfer.multi` | Quyền chuyển nhiều tài nguyên                       | `true`  |
| `storage.transfer.log` | Quyền xem lịch sử chuyển tài nguyên                 | `true`  |
| `storage.transfer.log.others` | Quyền xem lịch sử chuyển tài nguyên người chơi khác | `true`  |
| `storage.event.view` | Quyền xem trạng thái sự kện                         | `true`  |
| `storage.enchant.use` | Quyền sử dụng lệnh phù phép                         | `true`  |
| `storage.craft.use` | Quyền sử dụng tính năng chế tạo                     | `true`  |
| `storage.cropstorage.use` | Quyền sử dụng tính năng CropStorage                 | `true` |
| `storage.cropstorage.toggle` | Quyền autopickup                                    | `true` |
| `storage.cropstorage.groundstore` | Quyền nhặt vật phẩm dưới đất                        | `true` |
| `storage.cropstorage.view` | Xem kho của người chơi khác                         | `true` |
| `storage.cropstorage.autosell` | Quyền bán vật phẩm tự động                          | `op` |
| `storage.mythicstorage.use` | Quyền sử dụng MythicStorage                         | `true`  |
| `storage.mythicstorage.toggle` | Quyền autopickup                                    | `true`  |
| `storage.mythicstorage.groundstore` | Quyền nhặt vật phẩm dưới đất                        | `true` |
| `storage.mythicstorage.view` | Quyền xem kho của người chơi khác                   | `true`  |
| `storage.mythicstorage.transfer` | Quyền chuyển vật phẩm mythicmobs                    | `true`  |
| `storage.mythicstorage.transfer.multi` | Quyền chuyển nhiều vật phẩm                         | `true`  |
| `storage.mythicstorage.transfer.log` | Quyền xem lịch sử chuyển vật phẩm                   | `true`  |
| `storage.mythicstorage.transfer.log.others` | Quyền xem lịch sử chuyển vật phẩm của người Khác    | `true`  |

Lưu ý: Các quyền hạn trên có thể được cấu hình và tùy chỉnh theo nhu cầu của người dùng.

### Quyền Của Quản Trị Viên (Admin Permissions)
| Permission                     | Mô Tả                                                          | Default (Mặc Định) |
|--------------------------------|----------------------------------------------------------------|-|
| `storage.admin`                |                                                                | `op` |
| `storage.admin.add`            | Quyền thêm vật phẩm cho người chơi                             | `op` |
| `storage.admin.remove`         | Quyền Xóa vật phẩm của người chơi                              | `op` |
| `storage.admin.set`            | Quyền Đặt số lượng vật phẩm của người chơi                     | `op` |
| `storage.admin.max`            | Quyền Đặt giới hạn kho lưu trữ của người chơi                  | `op` |
| `storage.admin.resetlimit`     | Quyền Làm mới giới hạn lưu trữ của người chơi                  | `op` |
| `storage.admin.reload`         | Quyền tải lại cấu hình                                         | `op` |
| `storage.admin.reset`          | Quyền làm mới vật phẩm kho lưu trữ người dùng                  | `op` |
| `storage.admin.specialmaterial` | Quyền sử dụng lệnh tài nguyên đặc biệt                         | `op` |
| `storage.transfer.admin`       | Quyền tính năng chuyển vật phẩm (chủ yếu là bypass limit, v.v) | `op` |
| `storage.event.admin`          | Quyền quản lí sự kiện                                          | `op` |
| `storage.admin.enchant`        | Quyền sử dụng lệnh phù phép                                    | `op` |
| `storage.mythicstorage.admin`  | Quyền sử dụng tính năng MythicStorage Admin                    | `op` |
| `storage.cropstorage.admin`    | Quyền sử dụng tính năng CropStorage Admin                      | `op` |
| `storage.craft.admin`          | Quyền sử dụng tính năng CraftEditor                            | `op` |
| `storage.preventrebreak.bypass` | Quyền bỏ qua hệ thống chống gian lận                           | `op` |

### Quyền Hạn Giới Hạn Sức Chứa
Được cấu hình phân cấp Priority thông qua file cấu hình. Hệ thống priority: số càng cao = ưu tiên càng cao (ghi đè priority thấp hơn).

#### Storage
Cấu hình tại `config.yml` (mục `storage_permissions`).

| Permission | Tác Dụng                                                                                          |
|------------|---------------------------------------------------------------------------------------------------|
| `storage.storage.<tier>` | Quyền Cấp Bậc Role (Ví dụ: `storage.storage.vip`)                                                 |
| `storage.storage.max.<n>` | Quyền Giới Hạn Trực Tiếp (Ví dụ: `storage.storage.max.50000` → giới hạn kho lưu trữ sẽ là 50,000) |

#### CropStorage
Cấu hình tại `cropstorage.yml` (mục `crop_storage_permissions`).

| Permission | Tác Dụng                                                                                                      |
|------------|---------------------------------------------------------------------------------------------------------------|
| `storage.cropstorage.storage.<tier>` | Quyền Cấp Bậc Role (Ví dụ: `storage.cropstorage.storage.vip`)                                                 |
| `storage.cropstorage.storage.max.<n>` | Quyền Giới Hạn Trực Tiếp (Ví dụ: `storage.cropstorage.storage.max.10000` → giới hạn kho lưu trữ sẽ là 10,000) |

#### MythicStorage
Cấu hình tại `mythicstorage.yml` (mục `mythic_storage_permissions`).

| Permission | Tác Dụng                                                                                                        |
|------------|-----------------------------------------------------------------------------------------------------------------|
| `storage.mythicstorage.storage.<tier>` | Quyền Cấp Bậc Role (Ví dụ: `storage.mythicstorage.storage.vip`)                                                 |
| `storage.mythicstorage.storage.max.<n>` | Quyền Giới Hạn Trực Tiếp (Ví dụ: `storage.mythicstorage.storage.max.10000` → giới hạn kho lưu trữ sẽ là 10,000) |

**Lưu ý**: Nếu người chơi có nhiều quyền numeric `max.<n>`, hệ thống sẽ lấy giá trị cao nhất.
