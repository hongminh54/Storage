# Tuỳ Chỉnh Giao Diện (GUI Customization)

Plugin Storage có hệ thống GUI linh hoạt. Mọi menu, nút bấm, âm thanh, và bố cục đều có thể tuỳ biến bằng cách chỉnh file trong thư mục `plugins/Storage/GUI/`.

## Thư Mục `GUI`
Trong thư mục này có file cho hầu hết các giao diện của plugin, ví dụ:
- `storage.yml` (Main storage menu)
- `mythicstorage.yml` (MythicMobs storage)
- `cropstorage.yml` (Crop storage)
- `convert-ore.yml` (Material conversion menu)
- `transfer.yml` & `transfer-multi.yml`
- `recipe-list.yml` & `recipe-editor.yml` (Crafting interfaces)
- và nhiều file khác

---

## Cấu Trúc Một File GUI

Ví dụ cấu trúc cơ bản của một file GUI:

```yaml
title: "&8Player Storage"
size: 54

items:
  # General filler item
  filler:
    material: "BLACK_STAINED_GLASS_PANE"
    name: " "
    slots:
      - 0, 1, 2, 3, 4, 5, 6, 7, 8
      - 45, 46, 47, 48, 49, 50, 51, 52, 53

  # Close Menu Button
  close:
    material: "BARRIER"
    name: "&cClose Menu"
    slot: 49
    action: "CLOSE"

  # Next Page Button
  next_page:
    material: "ARROW"
    name: "&aNext Page"
    slot: 53
    action: "NEXT_PAGE"
    
  # ... More custom buttons
```

### Các Thành Phần Hỗ Trợ (Components Supported)

1. **`title`**: Tên hiển thị trên đầu khung inventory. Hỗ trợ mã màu và Placeholders.
2. **`size`**: Tổng số slot. Phải là bội số của 9 (vd: 9, 18, 27, 36, 45, 54).
3. **`items`**: Các object có thể click.
   - `material`: Minecraft material ID.
   - `name`: Tên hiển thị.
   - `lore`: Danh sách dòng mô tả. Hỗ trợ Placeholders.
   - `slot` / `slots`: Một slot (0-53) hoặc danh sách slot (tách bằng dấu phẩy).
   - `custom_model_data`: Custom model cho resource pack.
   - `amount`: Số lượng item trong stack.
   - `action`: Action built-in khi click (vd `CLOSE`, `NEXT_PAGE`, `PREVIOUS_PAGE`, `BACK`, ...).

---

## Thêm Âm Thanh Cho GUI (Adding Sounds to GUIs)

Bạn có thể gắn sound khi mở menu hoặc tương tác nút bấm bằng cách thêm section `sounds`:

```yaml
sounds:
  open:
    name: "BLOCK_CHEST_OPEN"
    volume: 1.0
    pitch: 1.2
```

## Hiển Thị Động Item Đã Lưu (Dynamic Display of Saved Items)
Trong các file như `storage.yml` hoặc `mythicstorage.yml`, có khu vực dành riêng để auto-fill item mà người chơi đã lưu. Khi bố trí filler kính nền, hãy chừa vùng trống phù hợp để item có thể generate đúng vị trí.

## Áp Dụng Thay Đổi (Making Changes)
Sau khi chỉnh file GUI, hãy reload bằng `/storage reload` hoặc restart server để áp dụng thay đổi giao diện.
