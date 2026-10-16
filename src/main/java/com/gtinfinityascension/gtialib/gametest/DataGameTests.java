package com.gtinfinityascension.gtialib.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.gtinfinityascension.gtialib.GTIALib;
import com.gtinfinityascension.gtialib.data.BlockPosHashing;
import com.gtinfinityascension.gtialib.data.BlockPosLongSet;
import com.gtinfinityascension.gtialib.data.Int128;

import java.math.BigInteger;
import java.util.Random;

/** Data structures checked against an oracle (BigInteger, HashSet semantics). */
@PrefixGameTestTemplate(false)
@GameTestHolder(GTIALib.MOD_ID)
public final class DataGameTests {

    private static final BigInteger MOD = BigInteger.ONE.shiftLeft(128);
    private static final BigInteger HALF = BigInteger.ONE.shiftLeft(127);

    private DataGameTests() {}

    /** Wraps a BigInteger to the signed 128-bit range, like Int128 overflow does. */
    private static BigInteger wrap(BigInteger value) {
        BigInteger r = value.mod(MOD);
        return r.compareTo(HALF) >= 0 ? r.subtract(MOD) : r;
    }

    private static Int128 random(Random rng) {
        return switch (rng.nextInt(3)) {
            case 0 -> new Int128(rng.nextLong());
            case 1 -> new Int128(0L, rng.nextLong());
            default -> new Int128(rng.nextLong(), rng.nextLong());
        };
    }

    @GameTest(template = "qa_empty_5x5", timeoutTicks = 200)
    public static void int128MatchesBigInteger(GameTestHelper h) {
        Random rng = new Random(1234);
        for (int i = 0; i < 20_000; i++) {
            Int128 a = random(rng), b = random(rng);
            BigInteger x = a.toBigInteger(), y = b.toBigInteger();
            check(h, "add", x, y, Int128.add(a, b, new Int128()), wrap(x.add(y)));
            check(h, "subtract", x, y, Int128.subtract(a, b, new Int128()), wrap(x.subtract(y)));
            check(h, "multiply", x, y, Int128.multiply(a, b, new Int128()), wrap(x.multiply(y)));
            h.assertTrue(Integer.signum(a.compareTo(b)) == x.compareTo(y), "compare " + x + " " + y);
        }
        h.succeed();
    }

    private static void check(GameTestHelper h, String op, BigInteger x, BigInteger y, Int128 got,
                              BigInteger expected) {
        if (!got.toBigInteger().equals(expected)) {
            h.fail(op + "(" + x + ", " + y + ") = " + got.toBigInteger() + ", expected " + expected);
        }
    }

    @GameTest(template = "qa_empty_5x5", timeoutTicks = 40)
    public static void blockPosSetsBehaveLikeSets(GameTestHelper h) {
        BlockPosLongSet ordered = new BlockPosLongSet();
        var hashed = BlockPosHashing.newSet();
        for (int x = -20; x < 20; x++)
            for (int y = 0; y < 20; y++)
                for (int z = -20; z < 20; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    ordered.add(pos);
                    hashed.add(pos);
                }
        h.assertTrue(ordered.size() == 32_000 && hashed.size() == 32_000, "every position is kept once");
        h.assertTrue(!ordered.add(new BlockPos(0, 0, 0)), "a duplicate is not added again");
        h.assertTrue(ordered.contains(new BlockPos(-20, 19, 19)) && hashed.contains(new BlockPos(-20, 19, 19)),
                "membership is by position, not by object");
        h.assertTrue(ordered.iterator().next().equals(new BlockPos(-20, 0, -20)), "insertion order is kept");
        h.succeed();
    }
}
