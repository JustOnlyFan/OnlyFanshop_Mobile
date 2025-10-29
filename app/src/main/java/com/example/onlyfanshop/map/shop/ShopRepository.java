package com.example.onlyfanshop.map.shop;

import java.util.List;

public class ShopRepository {

    private static volatile ShopRepository INSTANCE;
    private ShopDataSource dataSource;
    private DatabaseShopDataSource databaseSource;

    private ShopRepository(ShopDataSource ds) {
        // Use DatabaseShopDataSource instead of InMemoryShopDataSource
        this.databaseSource = new DatabaseShopDataSource();
        this.dataSource = this.databaseSource;
    }

    public static ShopRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (ShopRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ShopRepository(null);
                }
            }
        }
        return INSTANCE;
    }

    public void setDataSource(ShopDataSource ds) {
        this.dataSource = ds;
    }
    
    public void loadFromDatabase(DatabaseShopDataSource.OnDataLoadedListener listener) {
        if (databaseSource != null) {
            databaseSource.loadFromDatabase(listener);
        }
    }
    
    public boolean isLoaded() {
        return databaseSource != null && databaseSource.isLoaded();
    }

    public List<Shop> getAllShops() { return dataSource.getAll(); }
    public void addShop(Shop s) { dataSource.add(s); }
    public void addShops(List<Shop> list) { dataSource.addAll(list); }
    public boolean updateShop(Shop s) { return dataSource.update(s); }
    public boolean removeShop(String id) { return dataSource.remove(id); }
    public Shop findById(String id) { return dataSource.findById(id); }
}