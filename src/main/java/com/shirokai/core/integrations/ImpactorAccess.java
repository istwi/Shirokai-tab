package com.shirokai.core.integrations;

import net.impactdev.impactor.api.economy.EconomyService;
import net.kyori.adventure.key.Key;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

/** Optional Impactor 5.3.5 API. No primary-currency fallback, deposits, commands or storage-file access. */
public final class ImpactorAccess {
    private final Executor server;
    public ImpactorAccess(Executor server) { this.server = server; }
    public CompletableFuture<Optional<String>> balance(UUID player, String currencyId) { return balance(player, currencyId, () -> true); }
    public CompletableFuture<Optional<String>> balance(UUID player, String currencyId, java.util.function.BooleanSupplier current) {
        var service = EconomyService.instance();
        var currency = service.currencies().currency(Key.key(currencyId));
        if (currency.isEmpty() || !currency.get().key().asString().equals(currencyId)) return CompletableFuture.completedFuture(Optional.empty());
        // account() can create an account. Do not call it for an absent account.
        return service.hasAccount(currency.get(), player).thenComposeAsync(exists -> {
            if (!current.getAsBoolean() || EconomyService.instance() != service || !exists) return CompletableFuture.completedFuture(Optional.<String>empty());
            return service.account(currency.get(), player).thenApplyAsync(account -> {
                if (!current.getAsBoolean() || EconomyService.instance() != service || account.virtual() || !account.owner().equals(player) || !account.currency().key().asString().equals(currencyId)) return Optional.<String>empty();
                BigDecimal value = account.balance();
                return Optional.of(value.stripTrailingZeros().toPlainString());
            }, server);
        }, server).orTimeout(15, TimeUnit.SECONDS);
    }
}
