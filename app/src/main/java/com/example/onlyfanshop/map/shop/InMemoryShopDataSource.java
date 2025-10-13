package com.example.onlyfanshop.map.shop;

import java.util.ArrayList;
import java.util.List;

public class InMemoryShopDataSource implements ShopDataSource {

    private final List<Shop> data = new ArrayList<>();

    public InMemoryShopDataSource() {
        seedVNShops();
    }

    private void seedVNShops() {
        // Hà Nội (2)
        data.add(new Shop("HN1","Cửa hàng Quạt CoolAir - Hoàn Kiếm",
                "Chuyên quạt đứng, quạt treo tường, quạt điều hòa.",
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800",
                21.03148, 105.84590,"24 Hàng Bông, Hoàn Kiếm, Hà Nội"));
        data.add(new Shop("HN2","Cửa hàng Quạt Gió Xanh - Cầu Giấy",
                "Quạt cây, quạt điều hoà, dịch vụ giao nhanh nội thành.",
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800",
                21.03686, 105.78350,"165 Xuân Thuỷ, Cầu Giấy, Hà Nội"));

        // Đà Nẵng (1)
        data.add(new Shop("DN1","Cửa hàng Quạt Biển Xanh - Hải Châu",
                "Quạt treo tường, quạt công nghiệp, bảo hành 12 tháng.",
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800",
                16.06696,108.22437,"74 Bạch Đằng, Hải Châu, Đà Nẵng"));

        // Huế (1)
        data.add(new Shop("HUE1","Cửa hàng Quạt Kinh Thành - Phú Hội",
                "Quạt để bàn, quạt trần, lắp đặt tận nơi.",
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800",
                16.46365,107.59093,"15 Lê Lợi, Phú Hội, TP Huế"));

        // TP.HCM (3)
        data.add(new Shop("HCM1","Cửa hàng Quạt Sài Gòn - Quận 1",
                "Đa dạng mẫu mã, giá tốt, nhiều khuyến mãi.",
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800",
                10.77323,106.70115,"42 Lê Lợi, Quận 1, TP.HCM"));
        data.add(new Shop("HCM2","Cửa hàng Quạt Gió Mát - Phú Nhuận",
                "Quạt không cánh, quạt mini USB, giao 2h.",
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800",
                10.80089,106.68799,"70 Phan Xích Long, Phú Nhuận, TP.HCM"));
        data.add(new Shop("HCM3","Cửa hàng Quạt Chợ Lớn - Quận 5",
                "Quạt công nghiệp, quạt trần, lắp đặt tận nơi.",
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800",
                10.75763,106.66335,"50 Hùng Vương, Quận 5, TP.HCM"));
    }

    @Override public List<Shop> getAll() { return new ArrayList<>(data); }
    @Override public void add(Shop shop) { data.add(shop); }
    @Override public void addAll(List<Shop> list) { data.addAll(list); }

    @Override public boolean update(Shop shop) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId().equals(shop.getId())) {
                data.set(i, shop);
                return true;
            }
        }
        return false;
    }

    @Override public boolean remove(String id) {
        return data.removeIf(s -> s.getId().equals(id));
    }

    @Override public Shop findById(String id) {
        for (Shop s : data) if (s.getId().equals(id)) return s;
        return null;
    }
}