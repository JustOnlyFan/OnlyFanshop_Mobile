package com.example.onlyfanshop.map.shop;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.example.onlyfanshop.adapter.AttractionAdapter;
import com.example.onlyfanshop.model.Attraction;

import java.util.Collections;
import java.util.List;

/**
 * Điều khiển carousel: snap 1 item, gọi callback khi dừng cuộn.
 * ĐÃ SỬA: dùng danh sách data nội bộ thay vì gọi adapter.getItemAt(pos).
 */
public class AttractionCarouselController {

    public interface OnSnapListener {
        void onSnapped(@NonNull Attraction attraction, int position);
    }

    private final RecyclerView recyclerView;
    private final LinearLayoutManager layoutManager;
    private final AttractionAdapter adapter;
    private final SnapHelper snapHelper = new PagerSnapHelper();

    private int lastSnapped = RecyclerView.NO_POSITION;
    private List<Attraction> data = Collections.emptyList();

    public AttractionCarouselController(@NonNull RecyclerView rv,
                                        @NonNull LinearLayoutManager lm,
                                        @NonNull AttractionAdapter ad) {
        this.recyclerView = rv;
        this.layoutManager = lm;
        this.adapter = ad;
    }

    public void attach(List<Attraction> data, OnSnapListener listener) {
        this.data = (data != null) ? data : Collections.emptyList();

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        snapHelper.attachToRecyclerView(recyclerView);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View snapped = snapHelper.findSnapView(layoutManager);
                    if (snapped != null) {
                        int pos = layoutManager.getPosition(snapped);
                        if (pos != RecyclerView.NO_POSITION
                                && pos != lastSnapped
                                && pos < AttractionCarouselController.this.data.size()) {
                            lastSnapped = pos;
                            if (listener != null) {
                                listener.onSnapped(AttractionCarouselController.this.data.get(pos), pos);
                            }
                        }
                    }
                }
            }
        });

        // Focus item đầu tiên
        recyclerView.post(() -> {
            if (!this.data.isEmpty()) {
                lastSnapped = 0;
                if (listener != null) listener.onSnapped(this.data.get(0), 0);
            }
        });
    }

    public void smoothScrollTo(int position) {
        if (position >= 0 && position < adapter.getItemCount()) {
            recyclerView.smoothScrollToPosition(position);
        }
    }
}