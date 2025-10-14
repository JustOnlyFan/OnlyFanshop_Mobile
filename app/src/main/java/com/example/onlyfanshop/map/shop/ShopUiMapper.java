package com.example.onlyfanshop.map.shop;

import com.example.onlyfanshop.model.Attraction;

import java.util.ArrayList;
import java.util.List;

public final class ShopUiMapper {
    private ShopUiMapper() {}

    public static List<Attraction> toAttractions(List<Shop> shops) {
        List<Attraction> list = new ArrayList<>();
        if (shops == null) return list;
        for (Shop s : shops) {
            list.add(new Attraction(
                    s.getId(),
                    s.getName(),
                    s.getDescription(),
                    s.getImageUrl(),
                    s.getLatitude(),
                    s.getLongitude(),
                    s.getAddress()
            ));
        }
        return list;
    }
}