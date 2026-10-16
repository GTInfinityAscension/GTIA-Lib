package com.gtinfinityascension.gtialib.test;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtinfinityascension.gtialib.lang.Tr;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Checks that sweep over everything a mod registers, so new content is tested the day it is added. Each returns a
 * {@link SweepResult}; call {@link SweepResult#assertClean} on it.
 */
public final class Sweeps {

    private Sweeps() {}

    /** Every multiblock of {@code namespace} whose preview fits in {@code maxSide} forms from that preview. */
    public static SweepResult multiblocksFormFromPreview(GameTestHelper helper, String namespace, int maxSide) {
        SweepResult result = new SweepResult(namespace + " multiblocks forming from their preview");
        BlockPos origin = new BlockPos(0, 1, 0);
        for (MachineDefinition machine : GTRegistries.MACHINES) {
            if (!(machine instanceof MultiblockMachineDefinition def) ||
                    !namespace.equals(def.getId().getNamespace()))
                continue;
            int[] size = Previews.size(def);
            String id = def.getId().getPath();
            if (size == null) {
                result.fail(id + " (no preview)");
                continue;
            }
            if (size[0] > maxSide || size[1] > maxSide || size[2] > maxSide) continue;
            result.checked();
            try (var placed = Previews.place(helper, def, origin)) {
                if (!placed.forms()) result.fail(id + " (" + placed.error() + ")");
            } catch (RuntimeException e) {
                result.fail(id + " (preview threw " + e + ")");
            }
        }
        return result;
    }

    /** No GT recipe of {@code namespace} has an empty item or fluid input, an empty item output or no duration. */
    public static SweepResult recipesHaveRealContents(GameTestHelper helper, String namespace) {
        SweepResult result = new SweepResult(namespace + " GT recipes");
        for (var recipe : helper.getLevel().getRecipeManager().getRecipes()) {
            if (!(recipe instanceof GTRecipe gt) || !namespace.equals(gt.id.getNamespace())) continue;
            result.checked();
            String id = gt.id.getPath();
            for (var content : gt.getInputContents(ItemRecipeCapability.CAP)) {
                var items = ItemRecipeCapability.CAP.of(content.content).getItems();
                if (items.length == 0 || items[0].isEmpty()) result.fail(id + ": empty item input");
            }
            for (var content : gt.getInputContents(FluidRecipeCapability.CAP)) {
                var fluids = FluidRecipeCapability.CAP.of(content.content).getStacks();
                if (fluids.length == 0 || fluids[0].isEmpty()) result.fail(id + ": empty fluid input");
            }
            for (var content : gt.getOutputContents(ItemRecipeCapability.CAP)) {
                var items = ItemRecipeCapability.CAP.of(content.content).getItems();
                if (items.length == 0 || items[0].isEmpty()) result.fail(id + ": empty item output");
            }
            if (gt.duration <= 0) result.fail(id + ": duration " + gt.duration);
        }
        return result;
    }

    /** Every machine tooltip of {@code namespace} builds without throwing and without showing a raw key. */
    public static SweepResult machineTooltipsBuild(String namespace) {
        SweepResult result = new SweepResult(namespace + " machine tooltips");
        for (MachineDefinition definition : GTRegistries.MACHINES) {
            if (!namespace.equals(definition.getId().getNamespace())) continue;
            BiConsumer<ItemStack, List<Component>> builder = definition.getTooltipBuilder();
            if (builder == null) continue;
            result.checked();
            List<Component> lines = new ArrayList<>();
            try {
                builder.accept(ItemStack.EMPTY, lines);
            } catch (RuntimeException e) {
                result.fail(definition.getId().getPath() + " threw " + e);
                continue;
            }
            for (Component line : lines) {
                String text = line.getString();
                if (text.startsWith(namespace + ".")) result.fail(definition.getId().getPath() + ": raw key " + text);
            }
        }
        return result;
    }

    /**
     * Every item and block of {@code modid} has its name in en_us, pt_br and zh_cn, and every key in en_us also
     * exists in the other two. Reads the lang files from the mod's own jar.
     */
    public static SweepResult namesInAllLanguages(String modid) {
        SweepResult result = new SweepResult(modid + " translations");
        List<JsonObject> langs = new ArrayList<>();
        for (String locale : Tr.LOCALES) {
            JsonObject lang = readLang(modid, locale);
            if (lang == null) {
                result.fail("missing assets/" + modid + "/lang/" + locale + ".json");
                return result;
            }
            langs.add(lang);
        }
        List<String> keys = new ArrayList<>();
        ForgeRegistries.ITEMS.getEntries().forEach(e -> {
            if (modid.equals(e.getKey().location().getNamespace())) keys.add(e.getValue().getDescriptionId());
        });
        ForgeRegistries.BLOCKS.getEntries().forEach(e -> {
            if (modid.equals(e.getKey().location().getNamespace())) keys.add(e.getValue().getDescriptionId());
        });
        keys.addAll(langs.get(0).keySet());
        for (String key : keys.stream().distinct().sorted().toList()) {
            // EN: GTCEu material items share names built at runtime (tagprefix.*); only the mod's own keys count.
            // PT: Itens de material do GTCEu usam nomes montados em runtime (tagprefix.*); só contam as chaves do mod.
            // ZH: GTCEu 材料物品共用运行时生成的名称（tagprefix.*）；只检查本模组自己的键。
            if (!key.contains(modid + ".")) continue;
            result.checked();
            for (int i = 0; i < langs.size(); i++) {
                if (!langs.get(i).has(key)) result.fail(Tr.LOCALES.get(i) + ":" + key);
            }
        }
        return result;
    }

    private static JsonObject readLang(String modid, String locale) {
        var container = ModList.get().getModFileById(modid);
        if (container == null) return null;
        Path path = container.getFile().findResource("assets", modid, "lang", locale + ".json");
        if (!Files.exists(path)) return null;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
}
