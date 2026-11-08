package com.shirokai.core.customization;

import com.shirokai.core.presentation.Surface;
import java.util.*;

public record StyleChoices(UUID owner, boolean clan, long revision, Map<Surface,String> selections) {
    public StyleChoices { Objects.requireNonNull(owner); selections=Map.copyOf(selections); }
}
