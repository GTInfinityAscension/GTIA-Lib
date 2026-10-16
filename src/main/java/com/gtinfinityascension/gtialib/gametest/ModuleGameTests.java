package com.gtinfinityascension.gtialib.gametest;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.gtinfinityascension.gtialib.GTIALib;
import com.gtinfinityascension.gtialib.module.ModuleRegistry;

/** The module registry builds registered structures once, and KubeJS modules can be cleared on reload. */
@PrefixGameTestTemplate(false)
@GameTestHolder(GTIALib.MOD_ID)
public final class ModuleGameTests {

    private ModuleGameTests() {}

    @GameTest(template = "qa_empty_5x5", timeoutTicks = 20)
    public static void registeredModulesAreBuiltOnceAndKubeModulesClear(GameTestHelper h) {
        MultiblockMachineDefinition ebf = (MultiblockMachineDefinition) GTRegistries.MACHINES
                .get(new net.minecraft.resources.ResourceLocation("gtceu", "electric_blast_furnace"));
        h.assertTrue(ebf != null, "the GTCEu EBF exists");
        int before = ModuleRegistry.get(ebf).size();
        int[] built = { 0 };
        ModuleRegistry.registerKubeJS(ebf.getId(), def -> {
            built[0]++;
            return def.getPatternFactory().get();
        }, "gtialib.test.module", 2.0, true);
        var modules = ModuleRegistry.get(ebf);
        h.assertTrue(modules.size() == before + 1, "the module is registered");
        h.assertTrue(ModuleRegistry.get(ebf) == modules && built[0] == 1, "modules are built once and cached");
        var performance = ModuleRegistry.performance(modules.get(modules.size() - 1));
        h.assertTrue(performance != null && performance.speedBonus() == 2.0 && performance.perfectOverclock(),
                "the KubeJS module keeps its performance");
        ModuleRegistry.clearKubeJS();
        h.assertTrue(ModuleRegistry.get(ebf).size() == before, "clearing KubeJS modules removes them");
        h.succeed();
    }
}
