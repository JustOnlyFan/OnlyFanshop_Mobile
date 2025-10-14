package com.example.onlyfanshop.map.shop;

import java.util.List;

public interface ShopDataSource {
    List<Shop> getAll();
    void add(Shop shop);
    void addAll(List<Shop> list);
    boolean update(Shop shop);
    boolean remove(String id);
    Shop findById(String id);
}