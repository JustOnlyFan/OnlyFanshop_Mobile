package com.example.onlyfanshop.map.impl.routing;

import com.example.onlyfanshop.map.core.interfaces.RoutingProvider;
import com.example.onlyfanshop.map.models.RouteResult;

import java.util.List;

/**
 * RoutingProvider thử lần lượt nhiều provider cho đến khi thành công.
 */
public class ChainedRoutingProvider implements RoutingProvider {
    private final RoutingProvider[] providers;

    public ChainedRoutingProvider(RoutingProvider... providers) {
        this.providers = providers;
    }

    @Override
    public void route(double sLat, double sLng, double eLat, double eLng, int alternatives, Callback cb) {
        tryRoute(0, sLat, sLng, eLat, eLng, alternatives, cb);
    }

    private void tryRoute(int idx, double sLat, double sLng, double eLat, double eLng, int alternatives, Callback cb) {
        if (idx >= providers.length) {
            cb.onError(new Exception("All routing providers failed."));
            return;
        }
        providers[idx].route(sLat, sLng, eLat, eLng, alternatives, new Callback() {
            @Override
            public void onSuccess(List<RouteResult> routes) {
                // Nếu có route, trả về luôn. Nếu không, thử provider tiếp theo.
                if (routes != null && !routes.isEmpty()) {
                    cb.onSuccess(routes);
                } else {
                    tryRoute(idx + 1, sLat, sLng, eLat, eLng, alternatives, cb);
                }
            }
            @Override
            public void onError(Throwable t) {
                // Thử provider tiếp theo nếu gặp lỗi
                tryRoute(idx + 1, sLat, sLng, eLat, eLng, alternatives, cb);
            }
        });
    }
}