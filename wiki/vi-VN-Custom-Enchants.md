# Phù Phép Tuỳ Chỉnh

Plugin có sẵn hệ thống Custom Enchant để giúp việc khai thác khoáng sản hiệu quả và đáng giá hơn.

## Danh Sách Phù Phép

### 1. TNT
**Effect:** Tạo một vụ nổ để đào block theo vùng (AoE).
- Tích hợp với virtual storage (tất cả block nổ ra đều được đưa vào kho).
- **Levels 1-5**: Bán kính và sức nổ tăng theo cấp.
- Level 5 có thể đào vùng 7x7 rất nhanh.

### 2. Haste
**Effect:** Tự động cho hiệu ứng Haste chỉ bằng việc cầm cúp có enchant.
- Tăng tốc độ phá block.
- **Levels 1-5**: Tăng cường độ Haste theo cấp.

### 3. Multiplier
**Effect:** Nhân trực tiếp số lượng drop mà hệ thống auto-pickup ghi nhận.
- Nếu 1 quặng kim cương bình thường rơi 1 viên, cúp Multiplier level 3 sẽ ghi nhận 4 viên.
- Kết hợp tốt với Fortune.
- **Levels 1-3**.

### 4. Vein Miner
**Effect:** Phá toàn bộ các block cùng loại được kết nối với nhau.
- Gặp mạch than lớn: phá 1 block và toàn bộ mạch sẽ được đưa vào kho ảo.
- **Levels 1-3**: Tăng giới hạn số block kết nối tối đa có thể phá trong một tick (tối đa 32 ở level 3).

---

## Cấu Hình (`enchants.yml`)

Bạn có thể tuỳ chỉnh chi tiết từng enchant:
```yaml
  tnt:
    name: "TNT"
    max_level: 5
    applicable_items:
      - "DIAMOND_PICKAXE"
      - "NETHERITE_PICKAXE"
```
Bạn cũng có thể chỉnh theo từng level và thêm cooldown để hạn chế lag:
```yaml
    levels:
      1:
        explosion_power: 2.0
        cooldown_ticks: 60
        radius: 3
```

- Bật/tắt `glow_effect`.
- Chỉnh `particles` hoặc `sounds` khi enchant kích hoạt.
- Tuỳ biến `lore_format`.
