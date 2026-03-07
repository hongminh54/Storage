# Placeholders (PlaceholderAPI)

Storage cung cấp một bộ placeholder để sử dụng cùng [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/).
Hãy đảm bảo server của bạn đã cài PlaceholderAPI và đã load expansion của Storage.

## Placeholder Kho Chính (Main Storage)

### Tổng quan & Thống kê

| Placeholder | Mô tả | Ví dụ output |
|-------------|------|--------------|
| `%storage_status%` | Trạng thái auto-pickup | `On`/`Off` |
| `%storage_max_storage%` | Giới hạn tối đa của kho | `100000` |
| `%storage_percentage%` | Phần trăm đã lấp đầy kho | `67.5` |
| `%storage_total_materials%` | Số loại vật liệu khác nhau đang có trong kho | `12` |
| `%storage_total_blocks%` | Tổng tất cả block/vật phẩm trong kho | `50000` |
| `%storage_total_blocks_formatted%` | Tổng (đã format) | `50.0K` |
| `%storage_available_space%` | Sức chứa còn lại | `50000` |
| `%storage_available_space_formatted%` | Sức chứa còn lại (đã format) | `50.0K` |

### Placeholder Theo Từng Tài Nguyên
*(Thay `<MATERIAL>` bằng đúng key trong `config.yml`, ví dụ: `DIAMOND;0`)*

| Placeholder | Mô tả |
|-------------|------|
| `%storage_storage_<MATERIAL>%` | Số lượng trong kho (raw) |
| `%storage_storage_<MATERIAL>_formatted%` | Số lượng (đã format, ví dụ `15.2K`) |
| `%storage_storage_<MATERIAL>_percentage%` | Phần trăm của tài nguyên so với giới hạn tối đa |
| `%storage_sellable%` | Lấy ký hiệu sellable cấu hình sẵn (từ `message.yml` keys `user.sellable.yes/no`) |
| `%storage_sellable_<MATERIAL>%` | Tài nguyên có bán được không (trả về ký hiệu cấu hình từ `message.yml` keys `user.sellable.yes/no`) |
| `%storage_price_<MATERIAL>%` | Giá bán của vật liệu |

### Placeholder Chuyển Giao (Transfer)

| Placeholder | Mô tả |
|-------------|------|
| `%storage_transfer_sent_total%` | Tổng số block bạn đã gửi |
| `%storage_transfer_received_total%` | Tổng số block bạn đã nhận |
| `%storage_transfer_count%` | Tổng số lần giao dịch chuyển đồ |

### Placeholder Trang GUI

| Placeholder | Mô tả |
|-------------|------|
| `%storage_page_storage_current%` | Trang hiện tại của GUI Kho chính |
| `%storage_page_storage_total%` | Tổng số trang của GUI Kho chính |
| `%storage_page_mythic_current%` | Trang hiện tại của GUI MythicStorage |
| `%storage_page_mythic_total%` | Tổng số trang của GUI MythicStorage |

---

## Placeholder Kho MythicMobs (MythicStorage)

*(Thay `<item>` bằng MythicMobs item ID nội bộ, ví dụ `crown`)*

| Placeholder | Mô tả |
|-------------|------|
| `%storage_mythic_<item>%` | Lấy display name của Mythic item |
| `%storage_mythic_<item>_amount%` | Số lượng Mythic item trong kho |
| `%storage_mythic_<item>_amount_formatted%` | Số lượng (đã format) |
| `%storage_mythic_total_items%` | Số loại Mythic item khác nhau |
| `%storage_mythic_total_amount%` | Tổng số lượng tất cả Mythic item |
| `%storage_mythic_total_amount_formatted%` | Tổng (đã format) |
| `%storage_mythic_max_storage%` | Giới hạn tối đa MythicStorage |
| `%storage_mythic_percentage%` | Phần trăm đã lấp đầy MythicStorage |
| `%storage_mythic_available_space%` | Sức chứa còn lại |
| `%storage_mythic_autopickup_status%` | Trạng thái auto-pickup (`Enabled`/`Disabled`) |
| `%storage_mythic_transfer_sent_total%` | Tổng Mythic item bạn đã gửi |
| `%storage_mythic_transfer_received_total%` | Tổng Mythic item bạn đã nhận |
| `%storage_mythic_transfer_count%` | Tổng số lần chuyển Mythic item |

Lưu ý: tên id của vật phẩm mythicmobs có thể xem trong các file yml ở `\plugins\MythicMobs\items`

## Placeholder CropStorage

*(Thay `<MATERIAL>` bằng key nông sản (crop material), ví dụ `WHEAT` hoặc `WHEAT;0` tuỳ config)*

| Placeholder | Mô tả |
|-------------|------|
| `%storage_crop_sellable%` | Lấy ký hiệu sellable cấu hình sẵn cho CropStorage (từ `message.yml` keys `cropstorage.sellable.yes/no`) |
| `%storage_crop_sellable_<MATERIAL>%` | Nông sản này có bán được không (trả về ký hiệu cấu hình từ `message.yml` keys `cropstorage.sellable.yes/no`) |

Lưu ý: PlaceholderAPI expansion hiện tại chỉ public placeholder kiểm tra sellable của CropStorage.

---

## Placeholder Crafting
*(Placeholder crafting dùng prefix `%storagecraft_...%` thay vì `%storage_...%`)*

| Placeholder | Mô tả |
|-------------|------|
| `%storagecraft_total_recipes%` | Tổng số recipe đã đăng ký |
| `%storagecraft_enabled_recipes%` | Tổng số recipe đang bật |
| `%storagecraft_available_recipes%` | Recipe mà player có quyền để nhìn thấy |
| `%storagecraft_craftable_recipes%` | Recipe mà player hiện tại đủ nguyên liệu để craft |
| `%storagecraft_is_crafting%` | Player có đang craft hay không (`true`/`false`) |

**Theo recipe (`<recipeId>`):**

| Placeholder | Mô tả |
|-------------|------|
| `%storagecraft_recipe_<recipeId>_exists%` | Recipe có tồn tại không |
| `%storagecraft_recipe_<recipeId>_enabled%` | Recipe có đang bật không |
| `%storagecraft_recipe_<recipeId>_can_craft%` | Player có craft được recipe này không |
| `%storagecraft_recipe_<recipeId>_max_amount%` | Số lần tối đa player có thể craft recipe này |
| `%storagecraft_recipe_<recipeId>_name%` | Tên hiển thị của item output |

---

## Placeholder Sự Kiện (Events)

### Chung

| Placeholder | Mô tả |
|-------------|------|
| `%storage_event_active%` | Có event nào đang chạy không? (`Active`/`Disabled`) |
| `%storage_event_active_<event_type>%` | Một event cụ thể có đang chạy không? |
| `%storage_event_name%` | Tên event đang chạy |
| `%storage_event_type%` | Loại event đang chạy (display name) |
| `%storage_event_remaining_time%` | Thời gian còn lại (đã format, ví dụ `1h 30m 45s`) |
| `%storage_event_next_time%` | Thời gian tới event tiếp theo (đã format) |
| `%storage_event_remaining_seconds%` | Thời gian còn lại (giây) |
| `%storage_event_duration%` | Tổng thời lượng event (giây) |
| `%storage_event_start_time%` | Thời điểm bắt đầu (timestamp milliseconds) |
| `%storage_event_start_time_formatted%` | Giờ bắt đầu (format `HH:mm:ss`) |
| `%storage_event_start_date%` | Ngày bắt đầu (format `dd/MM/yyyy`) |
| `%storage_event_start_datetime%` | Ngày + giờ bắt đầu (format `dd/MM/yyyy HH:mm:ss`) |
| `%storage_event_next_seconds%` | Số giây tới event tiếp theo |

**Next event (theo loại event):**

| Placeholder | Mô tả |
|-------------|------|
| `%storage_event_next_<event_type>_time%` | Thời gian tới event tiếp theo (theo loại) |
| `%storage_event_next_<event_type>_seconds%` | Số giây tới event tiếp theo (theo loại) |
| `%storage_event_next_<event_type>_datetime%` | Datetime của lần chạy tiếp theo (`dd/MM/yyyy HH:mm:ss`) |
| `%storage_event_next_<event_type>_date%` | Date của lần chạy tiếp theo (`dd/MM/yyyy`) |
| `%storage_event_next_<event_type>_schedule_info%` | Thông tin schedule/interval từ config |

### Mining Contest

| Placeholder | Mô tả |
|-------------|------|
| `%storage_mining_contest_rank%` | Xếp hạng của player |
| `%storage_mining_contest_score%` | Điểm của player (số block đào được) |
| `%storage_mining_contest_participants%` | Tổng số người tham gia |
| `%storage_mining_contest_top_<position>_name%` | Tên player ở vị trí leaderboard tương ứng |
| `%storage_mining_contest_top_<position>_score%` | Điểm ở vị trí leaderboard tương ứng |

### Community Event

| Placeholder | Mô tả |
|-------------|------|
| `%storage_community_progress%` | Tiến độ block toàn server |
| `%storage_community_goal%` | Mục tiêu tổng block |
| `%storage_community_percentage%` | Phần trăm hoàn thành mục tiêu |
| `%storage_community_participants%` | Tổng số người tham gia |
| `%storage_community_player_contribution%` | Đóng góp của player |

### Double Drop

| Placeholder | Mô tả |
|-------------|------|
| `%storage_double_drop_multiplier%` | Hệ số double drop đang áp dụng |
| `%storage_double_drop_player_blocks%` | Số block player đào trong thời gian event |

---

## Leaderboards


| Placeholder | Mô tả |
|-------------|------|
| `%storage_top_<MATERIAL>_<position>_name%` | Tên top player cho một vật liệu |
| `%storage_top_<MATERIAL>_<position>_amount%` | Số lượng của top player cho một vật liệu |
| `%storage_top_all_<position>_name%` | Tên top player cho TỔNG tất cả vật liệu |
| `%storage_top_all_<position>_amount%` | Số lượng top player cho TỔNG tất cả vật liệu |
| `%storage_mythic_top_<item>_<position>_name%` | Tên top player cho một Mythic item |
| `%storage_mythic_top_<item>_<position>_amount%`| Số lượng top player cho một Mythic item |

> *Lưu ý: Leaderboard thường tính top dựa theo người chơi online để tối ưu hiệu năng.*
