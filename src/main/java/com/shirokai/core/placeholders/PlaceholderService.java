package com.shirokai.core.placeholders;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.*;

/** Literal, nonrecursive substitution. This is not a MiniMessage parser or a PAPI adapter. */
public final class PlaceholderService {
    public record Context(UUID playerId, String playerName, int online) { }
    public static final String KEY_PATTERN = "[a-z0-9_.-]+:[a-z0-9_./-]+(?: [a-zA-Z0-9_.:/ -]{1,128})?";
    private static final Pattern TOKEN = Pattern.compile("%(" + KEY_PATTERN + ")%");
    private java.util.function.BiFunction<String, Context, Optional<String>> external = (key, context) -> Optional.empty();
    private final ThreadLocal<Set<String>> resolving = ThreadLocal.withInitial(HashSet::new);
    public void external(java.util.function.BiFunction<String, Context, Optional<String>> resolver) { external = Objects.requireNonNull(resolver); }
    private final Map<String, Function<Context, Optional<String>>> resolvers;
    private final BiConsumer<String, Exception> errors;
    public PlaceholderService(BiConsumer<String, Exception> errors) {
        this.errors = Objects.requireNonNull(errors);
        resolvers = new HashMap<>(Map.of(
            "shirokai:online", context -> Optional.of(Integer.toString(context.online())),
            "shirokai:player", context -> Optional.ofNullable(context.playerName()),
            "shirokai:uuid", context -> Optional.ofNullable(context.playerId()).map(UUID::toString)));
    }
    public void register(String key, Function<Context, Optional<String>> resolver) {
        if (!key.matches("[a-z0-9_]+:[a-z0-9_]+") || resolvers.putIfAbsent(key, Objects.requireNonNull(resolver)) != null)
            throw new IllegalArgumentException("Placeholder inválido ou duplicado: " + key);
    }
    public Set<String> keys() { return Set.copyOf(resolvers.keySet()); }
    public Optional<String> resolve(String key, Context context) {
        Objects.requireNonNull(context);
        var active = resolving.get();
        if (active.size() >= 16 || !active.add(key)) return Optional.empty();
        try {
            var resolver = resolvers.get(key);
            return resolver == null ? external.apply(key, context) : resolver.apply(context);
        } catch (Exception failure) { errors.accept(key, failure); return Optional.empty(); }
        catch (LinkageError failure) { errors.accept(key, new IllegalStateException("API opcional incompatível", failure)); return Optional.empty(); }
        finally { active.remove(key); if (active.isEmpty()) resolving.remove(); }
    }
    public String render(String template, Context context, String fallback) {
        Objects.requireNonNull(context); Objects.requireNonNull(fallback);
        if (template.length() > 8192) throw new IllegalArgumentException("Template excede 8192 caracteres");
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1); String value = fallback;
            value = resolve(key, context).orElse(fallback);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        return matcher.appendTail(result).toString();
    }
}
