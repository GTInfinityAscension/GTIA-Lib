package com.gtinfinityascension.gtialib.lang;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Writes one lang file of a mod from its {@link Tr} entries. {@link #addAll} registers the three locales at once.
 */
public final class TrLanguageProvider extends LanguageProvider {

    private final String prefix;
    private final String locale;

    public TrLanguageProvider(PackOutput output, String modid, String prefix, String locale) {
        super(output, modid, locale);
        this.prefix = prefix;
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        for (Tr entry : Tr.entries(prefix)) add(entry.key(), entry.text(locale));
    }

    /**
     * Adds en_us, pt_br and zh_cn providers for every {@link Tr} key starting with {@code prefix}.
     * A mod whose en_us is written by Registrate passes {@code includeEnglish = false} and feeds English to
     * Registrate instead, since two providers can't write the same file.
     */
    public static void addAll(GatherDataEvent event, String modid, String prefix, boolean includeEnglish) {
        PackOutput output = event.getGenerator().getPackOutput();
        for (String locale : Tr.LOCALES) {
            if (!includeEnglish && Tr.EN.equals(locale)) continue;
            event.getGenerator().addProvider(event.includeClient(),
                    new TrLanguageProvider(output, modid, prefix, locale));
        }
    }
}
