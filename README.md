# GTIA Lib

Shared library of the [GregTech Infinity Ascension](https://github.com/GTInfinityAscension) mods (GTIACore, GTNA, Agronomica and future addons). Minecraft 1.20.1, Forge 47, GTCEu 7.5.3.

> **Status: 0.1.0, first slice.** The API may still change before 1.0.

## What it has

| Package | What it does |
| --- | --- |
| `lang` | `Tr.of(key, en, pt, zh)` declares a text in English, Portuguese and Chinese at once. `TrLanguageProvider` writes the three lang files in datagen, so a text can't be missing in one language |
| `tooltip` | `MultiblockTooltip`: the GTNA multiblock layout (fact, Main Function, Auxiliary Module, Running Requirements, Source) |
| `test` | GameTest kit: `Previews.place` builds a multiblock from its preview; `Sweeps` checks every multiblock, GT recipe, machine tooltip and translation of a mod; `-Dgtialib.junitReport=<file>` writes JUnit XML for CI |

Planned: data structures (`Int128`, `BlockPos` sets), registration helpers, the multiblock module system, batched field sync, recipe helpers and measured performance work. See the [developer docs](https://gtinfinityascension.github.io/GTIA-Dev-Doc/).

## Use it

```groovy
repositories {
    maven { url = 'https://gtinfinityascension.github.io/maven/' }
}
dependencies {
    modImplementation("com.gtinfinityascension:gtialib:0.1.0") { transitive = false }
}
```

```java
Tr name = Tr.of("mymod.thing", "Thing", "Coisa", "东西");

Component[] tooltip = MultiblockTooltip.of("mymod", "big_furnace")
        .fact("Blast furnaces are older than steel", "Altos-fornos são mais antigos que o aço", "高炉比钢更古老")
        .main("Smelts 16 ores at once", "Funde 16 minérios de uma vez", "一次熔炼16个矿石")
        .requirement("Needs a Maintenance Hatch", "Precisa de um Maintenance Hatch", "需要维护仓")
        .build();

@GameTest(template = "empty_16", timeoutTicks = 6000)
public static void everyMultiblockForms(GameTestHelper h) {
    Sweeps.multiblocksFormFromPreview(h, "mymod", 40).assertClean(h, 1);
    h.succeed();
}
```

In datagen: `TrLanguageProvider.addAll(event, "mymod", "mymod.", true)` (pass `false` when Registrate already writes your `en_us`).

## Building

```sh
./gradlew build runGameTestServer
```

## License

[LGPL-3.0](LICENSE.MD). Written from scratch; no code from decompiled sources.
