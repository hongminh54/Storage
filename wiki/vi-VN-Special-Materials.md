# Tài Nguyên Đặc Biệt (Special Materials)

Special Materials là các vật phẩm rơi hiếm (custom drops) có thể kích hoạt ngẫu nhiên khi người chơi đào mỏ. Bạn có thể tuỳ biến hoàn toàn và dùng chúng như phần thưởng để tạo động lực cho người chơi.

## Cơ Chế Hoạt Động
- Người chơi đào các block khoáng sản.
- Theo % tỉ lệ, một "Special Material" được cấu hình sẽ được rơi ra khi người chơi may mắn đào block đó.
- Khi rơi, hệ thống có thể phát particle và sound để tạo cảm giác phần thưởng.

## Ví Dụ Cấu Hình (`special_material.yml`)

Trong `plugins/Storage/special_material.yml`, bạn có thể định nghĩa các drop:

```yaml
special_materials:
  rare_gem:
    item:
      material: "EMERALD"
      name: "&a&lRare Gem"
      lore:
        - "&7A mysterious gem found"
        - "&7deep in the mines"
      glow: true
      
    # Drop Chance Configuration
    drop_chance: 5.0  # 5% chance
    source_blocks:
      - "DIAMOND_ORE;0"
      - "EMERALD_ORE;0"
      
    # Amount Randomization
    amount:
      min: 1
      max: 2

    # Rewards feedback
    effects:
      sound:
        enabled: true
        name: "ENTITY_EXPERIENCE_ORB_PICKUP"
      particles:
        enabled: true
        type: "VILLAGER_HAPPY"
        count: 15
```

### Kết Hợp Với Custom Enchants
Hai enchant `Vein Miner` và `TNT` có thể tạo rất nhiều lần break block, dẫn tới việc tài nguyên đặc biệt có thể rơi quá nhanh.
- Bạn có thể áp dụng modifier cho drop chance theo enchant (vd: `-50% chance nếu đào bằng TNT enchant`).
