package com.gtinfinityascension.gtialib.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.gtinfinityascension.gtialib.lang.Tr;

import java.util.ArrayList;
import java.util.List;

/**
 * Multiblock tooltip in the GTNA layout: an optional real-world fact in grey italics, then "Main Function",
 * "Auxiliary Module" (only when the structure has one) and "Running Requirements", each line a yellow "∘" item,
 * and an optional "Source:" line crediting the mod a port came from.
 * <p>
 * Line keys are {@code <namespace>.machine.<id>.tooltip.<section>.<n>}, declared as {@link Tr} entries.
 */
public final class MultiblockTooltip {

    private static final Tr MAIN = Tr.of("gtialib.tooltip.section.main", "Main Function", "Função Principal", "主要功能");
    private static final Tr AUXILIARY = Tr.of("gtialib.tooltip.section.auxiliary", "Auxiliary Module",
            "Módulo Auxiliar", "辅助模块");
    private static final Tr REQUIREMENTS = Tr.of("gtialib.tooltip.section.requirements", "Running Requirements",
            "Requisitos de Funcionamento", "运行要求");
    private static final Tr SOURCE = Tr.of("gtialib.tooltip.source", "Source: %s", "Origem: %s", "来源：%s");

    private final String prefix;
    private Tr fact;
    private final List<Tr> main = new ArrayList<>();
    private final List<Tr> auxiliary = new ArrayList<>();
    private final List<Tr> requirements = new ArrayList<>();
    private String source;

    private MultiblockTooltip(String namespace, String machineId) {
        this.prefix = namespace + ".machine." + machineId + ".tooltip.";
    }

    /** Declares the shared section headers; called by GTIA Lib at startup so datagen sees them. */
    public static void init() {}

    public static MultiblockTooltip of(String namespace, String machineId) {
        return new MultiblockTooltip(namespace, machineId);
    }

    public MultiblockTooltip fact(String en, String pt, String zh) {
        fact = Tr.of(prefix + "fact.0", en, pt, zh);
        return this;
    }

    public MultiblockTooltip main(String en, String pt, String zh) {
        main.add(Tr.of(prefix + "main." + main.size(), en, pt, zh));
        return this;
    }

    public MultiblockTooltip auxiliary(String en, String pt, String zh) {
        auxiliary.add(Tr.of(prefix + "auxiliary." + auxiliary.size(), en, pt, zh));
        return this;
    }

    public MultiblockTooltip requirement(String en, String pt, String zh) {
        requirements.add(Tr.of(prefix + "requirements." + requirements.size(), en, pt, zh));
        return this;
    }

    /** Reuses an existing line (a shared sentence such as "Overclocks like a GregTech machine"). */
    public MultiblockTooltip requirement(Tr line) {
        requirements.add(line);
        return this;
    }

    /** The mod or pack a port came from; shown as is, since it is a name. */
    public MultiblockTooltip source(String modName) {
        source = modName;
        return this;
    }

    public Component[] build() {
        if (main.isEmpty()) throw new IllegalStateException(prefix + " has no Main Function line");
        List<Component> lines = new ArrayList<>();
        if (fact != null) lines.add(fact.component().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        section(lines, MAIN, main);
        section(lines, AUXILIARY, auxiliary);
        section(lines, REQUIREMENTS, requirements);
        if (source != null) lines.add(SOURCE.component(source).withStyle(ChatFormatting.DARK_GRAY));
        return lines.toArray(Component[]::new);
    }

    private static void section(List<Component> lines, Tr header, List<Tr> items) {
        if (items.isEmpty()) return;
        lines.add(Component.literal("- ").append(header.component()).withStyle(ChatFormatting.GOLD));
        for (Tr item : items) {
            lines.add(Component.literal("∘ ").append(item.component()).withStyle(ChatFormatting.YELLOW));
        }
    }
}
