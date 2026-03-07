# Hướng Dẫn Cấu Hình

Plugin Storage sử dụng nhiều file cấu hình trong thư mục `plugins/Storage/`.

## Danh Sách File
- `config.yml`: Cấu hình chính (Database, Auto-sell, giới hạn kho lưu trữ, mapping block/drops,....).
- `mythicstorage.yml`: Giống với Storage, nhưng khác là tùy chỉnh vật phẩm MythicMobs.
- `cropstorage.yml`: Giống với Storage, nhưng khác là tùy chỉnh các vật phẩm nông sản.
- `crafting.yml`: Cầu hình tùy chỉnh hệ thống chế tạo.
- `message.yml`: Toàn bộ tin nhắn hiển thị của plugin có thể dịch tại đây.
- `events.yml`: Cấu hình và lịch chạy event toàn server.
- `enchants.yml`: Cấu hình custom enchant.
- `special_material.yml`: Cấu hình tài nguyên đặc biệt.

---

## Cấu Hình Chính (`config.yml`)

### Cài Đặt Database (Database Settings)
Bạn có thể chọn loại database (`sqlite`, `yml`, hoặc `mysql`).
```yaml
database:
  type: sqlite
  # Nếu dùng mysql, hãy điền phần mysql
```
*Lưu ý: Cần restart server sau khi thay đổi loại database.*

### Giới Hạn Và Quyền Hạn (Limits and Permissions)
Bạn có thể cấu hình giới hạn sức chứa bằng permission node (theo cơ chế ưu tiên).
```yaml
storage_permissions:
  vip:
    max_storage: 10000
    priority: 1
  premium:
    max_storage: 50000
    priority: 2
```
Nếu người chơi có `storage.storage.premium`, giới hạn sẽ là `50,000`.

### Mapping Blocks & Drops (Quan trọng) (Blocks & Drops Mapping)
Đây là nơi bạn định nghĩa block nào sẽ được đưa vào kho và item nào sẽ được lưu.
```yaml
blocks:
  COBBLESTONE;0:           # Block bị phá
    drop: COBBLESTONE;0    # Item được lưu
  IRON_ORE;0:
    drop: IRON_INGOT;0     # Có thể lưu quặng hoặc ingot tuỳ phiên bản
```
- **Legacy (1.12.2 trở xuống)**: Bắt buộc dùng `MATERIAL;DATA`. Ví dụ: `INK_SACK;4` cho Lapis Lazuli.
- **Modern (1.13+)**: Vẫn cần format với `;0`. Ví dụ: `DIAMOND;0`.

Nếu bạn dùng **MMOItems AutoSmelt**, bạn có thể thêm drop thay thế:
```yaml
  IRON_ORE;0:
    drop: RAW_IRON;0
    drop_autosmelt: IRON_INGOT;0
```

### Auto-Sell & Worth (Bán tự động)
Bạn có thể định giá vật phẩm khi auto-sell hoặc bán thủ công trong GUI.
```yaml
sell_method: vault  # "vault", "playerpoints", hoặc "commands"

worth:
  COBBLESTONE;0: 1
  DIAMOND;0: "7;vault"          # Override cụ thể
  EMERALD;0: "8;playerpoints"   # Override cụ thể
```

---

## MythicStorage (`mythicstorage.yml`)

Định nghĩa các drop từ MythicMobs sẽ được đưa vào MythicStorage (kho riêng).

```yaml
# Các vật phẩm có thể thêm vào kho lưu trữ (MythicMobs/Items/<item>.yml)
# Ví dụ:
# Trong MM phiên bản 5.X.X
#test: <--- ID vật phẩm MythicMobs dùng trong cấu hình items_drop
#  Id: diamond
#  Display: '&dabc'
#  Lore:
#    - '&6A kingly crowl that grants'
#    - '&6the wearer unwavering power!'
#  Enchantments:
#    - PROTECTION_ENVIRONMENTAL:2
#    - PROTECTION_PROJECTILE:2
#    - PROTECTION_FIRE:2
#    - PROTECTION_EXPLOSIONS:2
#  Hide:
#    - ATTRIBUTES
#    - ENCHANTS
#  Attributes:
#    Head:
#      Health: 10
#      KnockbackResistance: 10
items_drop:
  - kingdom
```
Tương tự như Storage, bạn có thể cấu hình giới hạn theo permission tại `mythic_storage_permissions` và nhiều cấu hình khác.

---

## CropStorage (`cropstorage.yml`)

Hoạt động giống kho chính/MythicStorage nhưng được tối ưu cho việc thu hoạch nông sản.
```yaml
crop_block_mapping:
  WHEAT: WHEAT
  CARROTS: CARROT
  POTATOES: POTATO
  BEETROOTS: BEETROOT
  NETHER_WART: NETHER_WART
  SUGAR_CANE: SUGAR_CANE
  COCOA: COCOA_BEANS
  MELON: MELON_SLICE
  PUMPKIN: PUMPKIN
  SWEET_BERRY_BUSH: SWEET_BERRIES
  KELP: KELP
  KELP_PLANT: KELP
  BAMBOO: BAMBOO
  CACTUS: CACTUS
  TORCHFLOWER: TORCHFLOWER
  PITCHER_PLANT: PITCHER_PLANT
  CHORUS_PLANT: CHORUS_FRUIT
  CHORUS_FLOWER: CHORUS_FRUIT

items_drop:
  - WHEAT
  - CARROT
  - POTATO
  - BEETROOT
  - NETHER_WART
  - SUGAR_CANE
  - COCOA_BEANS
  - MELON_SLICE
  - MELON
  - PUMPKIN
  - SWEET_BERRIES
  - KELP
  - BAMBOO
  - CACTUS
  - TORCHFLOWER
  - PITCHER_PLANT
  - CHORUS_FRUIT
```
Bạn cũng có thể cấu hình “tall crops” như Sugar Cane hoặc Cactus để plugin hiểu cách phá và lưu trữ đúng.

---

## Sự Kiện (Events) (`events.yml`)

Bạn có thể lên lịch event tự động như `mining_contest`, `double_drop`, hoặc `community_event`.
```yaml
events:
  mining_contest:
    timing:
      type: "interval"       # hoặc "schedule"
      interval: 7200         # Giây (vd: 2 giờ)
```

Bằng cách tinh chỉnh các cấu hình này, bạn có thể tuỳ biến Storage để phù hợp chính xác và phù hợp với gameplay của server.
