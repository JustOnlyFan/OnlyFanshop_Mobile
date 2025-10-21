package com.example.onlyfanshop.map.impl.autocomplete;

import android.util.Log;

import com.example.onlyfanshop.map.core.interfaces.AutocompleteProvider;
import com.example.onlyfanshop.map.models.PlaceSuggestion;

import java.util.Arrays;
import java.util.List;

public class ChainedAutocompleteProvider implements AutocompleteProvider {

    private final List<AutocompleteProvider> providers;

    public ChainedAutocompleteProvider(AutocompleteProvider... providers) {
        this.providers = Arrays.asList(providers);
    }

    @Override
    public void suggest(String query, Callback cb) {
        suggestRecursive(0, query, cb);
    }

    private void suggestRecursive(int idx, String query, Callback cb) {
        if (idx >= providers.size()) {
            cb.onSuccess(List.of()); // Không có provider nào thành công
            return;
        }
        providers.get(idx).suggest(query, new Callback() {
            @Override
            public void onSuccess(List<PlaceSuggestion> suggestions) {
                if (suggestions != null && !suggestions.isEmpty()) {
                    cb.onSuccess(suggestions);
                } else {
                    // Thử tiếp provider tiếp theo
                    suggestRecursive(idx + 1, query, cb);
                }
            }
            @Override
            public void onError(Throwable t) {
                Log.e("AutocompleteProvider", "Autocomplete failed: " + t.getMessage());
                // Thử tiếp provider tiếp theo
                suggestRecursive(idx + 1, query, cb);
            }
        });
    }
}