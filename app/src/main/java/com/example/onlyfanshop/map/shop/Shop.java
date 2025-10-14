package com.example.onlyfanshop.map.shop;

import android.os.Parcel;
import android.os.Parcelable;

public class Shop implements Parcelable {
    private final String id;
    private final String name;
    private final String description;
    private final String imageUrl;
    private final double latitude;
    private final double longitude;
    private final String address;
    private final String phone;
    private final String openingHours;

    public Shop(String id,
                String name,
                String description,
                String imageUrl,
                double latitude,
                double longitude,
                String address) {
        this(id, name, description, imageUrl, latitude, longitude, address, "", "");
    }

    public Shop(String id,
                String name,
                String description,
                String imageUrl,
                double latitude,
                double longitude,
                String address,
                String phone,
                String openingHours) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.phone = phone;
        this.openingHours = openingHours;
    }

    protected Shop(Parcel in) {
        id = in.readString();
        name = in.readString();
        description = in.readString();
        imageUrl = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        address = in.readString();
        phone = in.readString();
        openingHours = in.readString();
    }

    public static final Creator<Shop> CREATOR = new Creator<Shop>() {
        @Override
        public Shop createFromParcel(Parcel in) {
            return new Shop(in);
        }

        @Override
        public Shop[] newArray(int size) {
            return new Shop[size];
        }
    };

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getOpeningHours() { return openingHours; }

    @Override public int describeContents() { return 0; }
    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(imageUrl);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(address);
        dest.writeString(phone);
        dest.writeString(openingHours);
    }
}