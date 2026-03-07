# Hệ Thống Chế Tạo (Crafting System)

Hệ thống **Crafting** cho phép người chơi có thể tận dụng nguồn tài nguyên trong kho của mình để chế tạo các vật phẩm custom có sẵn trong hệ thống chế tạo của plugin.

Tất cả có thể truy cập trực tiếp bằng lệnh `/storage craft`.

## Giao Diện Chế Tạo (Crafting GUI)

Giao diện tự động phân loại công thức để dễ tìm:
- **Tools & Weapons**
- **Armor & Protection**
- **Blocks & Building**
- **Food & Consumables**
- **Materials & Resources**
- **Miscellaneous**
- **Special Items**
- **Custom**

Các công thức mà người chơi đủ nguyên liệu sẽ được highlight. Nếu thiếu nguyên liệu, người chơi có thể xem chính xác phần đang thiếu.

---

## Tạo Công Thức Tuỳ Chỉnh

Quản trị viên có thể tạo công thức mới bằng `/storage crafteditor` hoặc chỉnh thủ công `plugins/Storage/crafting.yml`.

Ngoài ra bạn cũng có thể import vật phẩm đang cầm trên tay làm công thức chế tạo mới `/storage crafteditor import`

### Ví Dụ Công Thức (Example Recipe)

Ví dụ cấu hình cho một công thức kiếm custom:

```yaml
recipes:
  example_diamond_sword:
    name: "&b&lEnhanced Diamond Sword"
    category: "tools"
    enabled: true

    # Result Item - This is what the player gets!
    result:
      material: "DIAMOND_SWORD"
      name: "&b&lEnhanced Diamond Sword"
      lore:
        - "&7A powerful sword forged"
        - "&7from rare materials"
        - ""
        - "&6&lLEGENDARY WEAPON"
      amount: 1
      custom_model_data: 0
      unbreakable: true
      enchantments:
        DAMAGE_ALL: 5
        DURABILITY: 3
        FIRE_ASPECT: 2
      flags:
        - "HIDE_ENCHANTS"
        - "HIDE_ATTRIBUTES"

    # Requirements - Items consumed from Virtual Storage
    requirements:
      materials:
        'DIAMOND;0': 10
        'IRON_INGOT;0': 5
        'GOLD_INGOT;0': 3
      permissions:
        - "storage.craft.enhanced"
```

### Giải Thích Các Trường
- `category`: Map tới một trong các menu GUI được định nghĩa sẵn.
- `result`: Định nghĩa item output (Material, name, lore, enchants, amount).
- `requirements.materials`: Nguyên liệu bắt buộc trong **Virtual Storage (OreStorage)**. Format `MATERIAL;DATA`.
- `requirements.permissions`: Danh sách permission node để nhìn thấy/craft item.

---

## Tính Năng

- **Delay & Animations**: Có thể đặt delay xử lý khi người chơi bấm "Craft", kèm sound/particle khi thành công hoặc thất bại.
- **MythicStorage Support**: Có thể yêu cầu nguyên liệu từ MythicMobs (MythicStorage) trong danh sách nguyên liệu.