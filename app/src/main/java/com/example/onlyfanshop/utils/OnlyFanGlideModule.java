package com.example.onlyfanshop.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

/**
 * Glide Module để tối ưu image loading cho toàn app
 * Tăng cache size, optimize memory usage, và cải thiện performance
 */
@GlideModule
public class OnlyFanGlideModule extends AppGlideModule {

    // Cache size: 50MB memory, 100MB disk
    private static final int MEMORY_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100MB

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // Tăng memory cache để giảm reload images
        builder.setMemoryCache(new LruResourceCache(MEMORY_CACHE_SIZE));

        // Tăng disk cache để cache nhiều images hơn
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, DISK_CACHE_SIZE));

        // Sử dụng RGB_565 để giảm memory usage (thay vì ARGB_8888)
        // Trade-off: chất lượng ảnh hơi giảm nhưng memory giảm 50%
        builder.setDefaultRequestOptions(
                new RequestOptions()
                        .format(DecodeFormat.PREFER_RGB_565)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .skipMemoryCache(false)
        );
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        super.registerComponents(context, glide, registry);
    }

    // Tắt manifest parsing để tăng performance khi build
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}

