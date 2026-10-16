package com.gtinfinityascension.gtialib.lang;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One translatable text with its English, Portuguese and Chinese versions declared together.
 * {@link TrLanguageProvider} writes the three lang files from these entries, so a text can't exist in one language
 * and be missing in another.
 */
public final class Tr {

    public static final String EN = "en_us";
    public static final String PT = "pt_br";
    public static final String ZH = "zh_cn";
    public static final List<String> LOCALES = List.of(EN, PT, ZH);

    private static final Map<String, Tr> ENTRIES = new ConcurrentHashMap<>();

    private final String key;
    private final String en;
    private final String pt;
    private final String zh;

    private Tr(String key, String en, String pt, String zh) {
        this.key = key;
        this.en = en;
        this.pt = pt;
        this.zh = zh;
    }

    /**
     * Declares a text. Declaring the same key twice is allowed only with the same three texts, so two mods can't
     * silently overwrite each other's strings.
     */
    public static Tr of(String key, String en, String pt, String zh) {
        require(key, "key", key);
        require(key, EN, en);
        require(key, PT, pt);
        require(key, ZH, zh);
        Tr entry = new Tr(key, en, pt, zh);
        Tr previous = ENTRIES.putIfAbsent(key, entry);
        if (previous != null && !previous.sameText(entry)) {
            throw new IllegalStateException("translation key " + key + " declared twice with different text");
        }
        return previous != null ? previous : entry;
    }

    private static void require(String key, String what, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("translation " + key + " has no " + what + " text");
        }
    }

    private boolean sameText(Tr other) {
        return en.equals(other.en) && pt.equals(other.pt) && zh.equals(other.zh);
    }

    public String key() {
        return key;
    }

    public String text(String locale) {
        return switch (locale) {
            case EN -> en;
            case PT -> pt;
            case ZH -> zh;
            default -> throw new IllegalArgumentException("unsupported locale " + locale);
        };
    }

    public MutableComponent component(Object... args) {
        return Component.translatable(key, args);
    }

    /** Every declared entry whose key starts with {@code prefix} (for example {@code "gtia."}), sorted by key. */
    public static List<Tr> entries(String prefix) {
        List<Tr> out = new ArrayList<>();
        for (Tr entry : ENTRIES.values()) {
            if (entry.key.startsWith(prefix)) out.add(entry);
        }
        out.sort((a, b) -> a.key.compareTo(b.key));
        return Collections.unmodifiableList(out);
    }
}
