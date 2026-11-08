package com.shirokai.core.integrations;

import com.shirokai.core.placeholders.PlaceholderService;
import java.util.*;
public interface PlaceholderBridge extends AutoCloseable {
    Optional<String> resolve(String key, PlaceholderService.Context context);
    void export(PlaceholderService service);
    @Override void close();
}
