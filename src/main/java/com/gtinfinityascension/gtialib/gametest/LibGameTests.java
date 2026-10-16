package com.gtinfinityascension.gtialib.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.gtinfinityascension.gtialib.GTIALib;
import com.gtinfinityascension.gtialib.lang.Tr;
import com.gtinfinityascension.gtialib.test.Sweeps;
import com.gtinfinityascension.gtialib.tooltip.MultiblockTooltip;

/** Checks the library against itself and against GTCEu's own content. */
@PrefixGameTestTemplate(false)
@GameTestHolder(GTIALib.MOD_ID)
public final class LibGameTests {

    private LibGameTests() {}

    @GameTest(template = "qa_empty_5x5", timeoutTicks = 20)
    public static void translationsNeedAllThreeLanguages(GameTestHelper h) {
        boolean rejected = false;
        try {
            Tr.of("gtialib.test.missing", "Text", "Texto", " ");
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        h.assertTrue(rejected, "a translation without Chinese text was accepted");
        h.succeed();
    }

    @GameTest(template = "qa_empty_5x5", timeoutTicks = 20)
    public static void aKeyCannotBeRedeclaredWithOtherText(GameTestHelper h) {
        Tr.of("gtialib.test.same", "One", "Um", "一");
        h.assertTrue(Tr.of("gtialib.test.same", "One", "Um", "一").text(Tr.PT).equals("Um"),
                "the same declaration twice should return the first one");
        boolean rejected = false;
        try {
            Tr.of("gtialib.test.same", "Two", "Dois", "二");
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        h.assertTrue(rejected, "a key was redeclared with different text");
        h.succeed();
    }

    @GameTest(template = "qa_empty_5x5", timeoutTicks = 20)
    public static void multiblockTooltipFollowsTheGtnaLayout(GameTestHelper h) {
        Component[] lines = MultiblockTooltip.of("gtialib", "test_machine")
                .fact("A fact", "Um fato", "一个事实")
                .main("Does a thing", "Faz uma coisa", "做一件事")
                .requirement("Needs power", "Precisa de energia", "需要能量")
                .source("GTNL")
                .build();
        h.assertTrue(lines.length == 6, "expected fact + 2 headers + 2 items + source, got " + lines.length);
        h.assertTrue(lines[1].getString().startsWith("- "), "section header should start with '- '");
        h.assertTrue(lines[2].getString().startsWith("∘ "), "section item should start with '∘ '");
        h.succeed();
    }

    @GameTest(template = "qa_empty_5x5", timeoutTicks = 20)
    public static void libraryNamesExistInAllThreeLanguages(GameTestHelper h) {
        Sweeps.namesInAllLanguages(GTIALib.MOD_ID).assertClean(h, 4);
        h.succeed();
    }

    /** The preview sweep is checked on GTCEu's own multiblocks, which must all form. */
    @GameTest(template = "empty_16", timeoutTicks = 6000)
    public static void gtceuMultiblocksFormFromTheirPreview(GameTestHelper h) {
        Sweeps.multiblocksFormFromPreview(h, "gtceu", 40).assertClean(h, 20);
        h.succeed();
    }

    @GameTest(template = "qa_empty_5x5", timeoutTicks = 20)
    public static void gtceuMachineTooltipsBuild(GameTestHelper h) {
        Sweeps.machineTooltipsBuild("gtceu").assertClean(h, 100);
        h.succeed();
    }
}
