# Cơ Chế

Các cơ chế của **Storage** đơn giản hóa sự tương tác của người chơi với thế giới game và tự động hóa hoàn toàn việc sắp xếp kho đồ tẻ nhạt. Dưới đây là phần giải thích về các thức hoạt động.

## Tự Động Nhặt (Auto-Pickup)

Ngay khoảnh khắc người chơi phá vỡ một khối đã được cấu hình trong [config.yml](../src/main/resources/config.yml), khối đó sẽ bỏ qua việc rớt trên mặt đất và thậm chí bỏ qua luôn túi đồ chính của người chơi.
sau đó đi thẳng vào **Kho Khoáng Sản (OreStorage)**. Bạn cũng có thể tuỳ chỉnh hiển thị một thông báo Action Bar (hoặc Subtitle) cho người chơi biết họ vừa đào được bao nhiêu và dung lượng kho còn dư. Ngoài ra **Phù Phép Gia Tài (Fortune)** vẫn áp dụng cho các vật phẩm rớt ra như bình thường!

*Lệnh:* `/storage`

## Tự Động Bán (Auto-Sell)

Tính năng bán tự động dành cho người chơi
- Plugin liên kết trực tiếp tới các plugin kinh tế như **Vault**, **PlayerPoints**, hoặc kích hoạt qua **Lệnh (Commands)**.
- Khi bật Tự Động Bán, vật phẩm sẽ tự động được bán lấy tiền ngay lập tức khi bạn vừa đào xong, không cần lưu vào trong kho nữa.
- Rất thích hợp cho những ai không muốn bản tài nguyên một cách thủ công

**Lưu ý**: Tính năng chỉ khả dụng với OreStorage (Kho Khoáng Sản) và CropStorage (Kho Nông Sản)

*Lệnh:* `/storage autosell`

## Nhặt Vật Phẩm Từ Dưới Đất (Ground-Store)

Đôi khi, các vật phẩm cũng đã rớt sẵn trên mặt đất (Ví dụ: do một vụ nổ, một người chơi khác không bật tích năng autopickup và đào block, hoặc ném từ trong rương ra).
- Nếu tính năng *ground-store* được bật, các vật phẩm rớt trên mặt đất trùng khớp với cấu hình trong Storage sẽ nhảy vào kho chứa ảo của người chơi một cách tức thì

*Lệnh:* `/storage groundstore`

## Hệ Thống Chế Tạo (Crafting System)

Bởi vì các vật phẩm của bạn đã bị trữ hàng loạt trong Kho Ảo, nên chắc bạn sẽ chả muốn người chơi phải lấy đồ ra túi đồ tay chỉ để đi craft bằng tay đâu.
- **Hệ Thống Chế Tạo ** (`/storage craft`) cho phép Server cài đặt các công thức chế tạo tùy thích.
- Những công thức này chủ động lấy trực tiếp nguyên liệu đang nằm gọn lỏn trong Kho ảo (bao gồm cả MythicStorage) hoàn toàn tự động. Thành phẩm sẽ đi thẳng vào túi đồ chính của người chơi.

## Hệ Thống Chuyển Đổi (Convert System)
Những người cày bừa ròng rã hàng triệu khối thì rất cần việc nén khối lại.
- `/storage convert` sẽ mở ra một Menu GUI.
- Bạn có thể chuyển Phôi (Ingots) thành Khối (Blocks) (Ví dụ: 9 Phôi Sắt -> 1 Khối Sắt) và ngược lại.
- Có thể chỉnh cực kì linh hoạt nếu bạn muốn chuyển đổi theo một tỉ lệ nén cụ thể `4:1` (như Thạch Anh - Quartz). Xem tại [config.yml](../src/main/resources/config.yml)

## Bảo Vệ (Protections)
Plugin đi kèm với nhiều lớp bảo vệ cực kì an toàn được tích hợp sẵn:
- **Ngăn Đào Rơi / Đập Lại (Prevent Rebreak)**: Để tránh lạm dụng một số lỗi hoặc gian lận, nếu người chơi đặt một khối (từ túi đồ) xuống và đập lại ngay lập tức, khối đó sẽ KHÔNG vào Kho Lưu Trữ, và họ cũng KHÔNG kiếm điểm Event từ khối đó (Chỉ xảy ra khi có sự kiện).
- **Tích Hợp WorldGuard**: Các quy tắc đào block và giới hạn phá block do Worldguard quản lý được bảo toàn tuyệt đối. (Nói cách khác, người chơi không thể lợi dụng Auto-Pickup để Bypass đào phá các region được).
