package com.gtinfinityascension.gtialib.lang;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Writes one lang file of a mod from its {@link Tr} entries. {@link #addAll} registers the three locales at once.
 */
public final class TrLanguageProvider extends LanguageProvider {

    private final String[] prefixes;
    private final String locale;

    public TrLanguageProvider(PackOutput output, String modid, String locale, String... prefixes) {
        super(output, modid, locale);
        this.prefixes = prefixes;
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        for (String prefix : prefixes) {
            for (Tr entry : Tr.entries(prefix)) add(entry.key(), entry.text(locale));
        }
    }

    /**
     * Adds en_us, pt_br and zh_cn providers for every {@link Tr} key starting with {@code prefix}.
     * A mod whose en_us is written by Registrate passes {@code includeEnglish = false} and feeds English to
     * Registrate instead, since two providers can't write the same file.
     */
    public static void addAll(GatherDataEvent event, String modid, String prefix, boolean includeEnglish) {
        addAll(event, modid, includeEnglish, prefix);
    }

    /**
     * Same as {@link #addAll(GatherDataEvent, String, String, boolean)} for several key prefixes (for example
     * {@code "item.mymod."}, {@code "block.mymod."} and {@code "mymod."}) written into one file per locale.
     */
    public static void addAll(GatherDataEvent event, String modid, boolean includeEnglish, String... prefixes) {
        PackOutput output = event.getGenerator().getPackOutput();
        for (String locale : Tr.LOCALES) {
            if (!includeEnglish && Tr.EN.equals(locale)) continue;
            event.getGenerator().addProvider(event.includeClient(),
                    new TrLanguageProvider(output, modid, locale, prefixes));
        }
    }
}
