package com.gtinfinityascension.gtialib.lang;

import com.tterrag.registrate.AbstractRegistrate;

/**
 * For mods whose en_us is written by Registrate: feeds the English text of their {@link Tr} entries to Registrate,
 * while {@link TrLanguageProvider#addAll} (with {@code includeEnglish = false}) writes pt_br and zh_cn.
 */
public final class TrRegistrate {

    private TrRegistrate() {}

    /** Call after every {@link Tr} of the mod is declared (for example at the end of the mod constructor). */
    public static void feedEnglish(AbstractRegistrate<?> registrate, String prefix) {
        for (Tr entry : Tr.entries(prefix)) registrate.addRawLang(entry.key(), entry.text(Tr.EN));
    }
}
