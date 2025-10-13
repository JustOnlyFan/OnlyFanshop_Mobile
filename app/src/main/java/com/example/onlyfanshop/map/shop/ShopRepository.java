package com.example.onlyfanshop.map.shop;

import java.util.List;

public class ShopRepository {

    private static volatile ShopRepository INSTANCE;
    private ShopDataSource dataSource;

    private ShopRepository(ShopDataSource ds) {
        this.dataSource = ds != null ? ds : new InMemoryShopDataSource();
    }

    public static ShopRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (ShopRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ShopRepository(new InMemoryShopDataSource());
                }
            }
        }
        return INSTANCE;
    }

    public void setDataSource(ShopDataSource ds) {
        this.dataSource = ds;
    }

    public List<Shop> getAllShops() { return dataSource.getAll(); }
    public void addShop(Shop s) { dataSource.add(s); }
    public void addShops(List<Shop> list) { dataSource.addAll(list); }
    public boolean updateShop(Shop s) { return dataSource.update(s); }
    public boolean removeShop(String id) { return dataSource.remove(id); }
    public Shop findById(String id) { return dataSource.findById(id); }
}