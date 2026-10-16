package com.gtinfinityascension.gtialib.test;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.ArrayList;
import java.util.List;

/** What a {@link Sweeps} check looked at and what it found wrong. */
public final class SweepResult {

    private static final int MAX_LISTED = 40;

    private final String what;
    private int checked;
    private final List<String> failures = new ArrayList<>();

    SweepResult(String what) {
        this.what = what;
    }

    void checked() {
        checked++;
    }

    void fail(String failure) {
        failures.add(failure);
    }

    public int checkedCount() {
        return checked;
    }

    public List<String> failures() {
        return List.copyOf(failures);
    }

    /**
     * Fails the test when anything was wrong, or when fewer than {@code minChecked} things were checked (a sweep
     * that silently checks nothing is a broken test, not a passing one).
     */
    public void assertClean(GameTestHelper helper, int minChecked) {
        helper.assertTrue(checked >= minChecked,
                what + ": checked " + checked + ", expected at least " + minChecked);
        if (!failures.isEmpty()) {
            List<String> shown = failures.subList(0, Math.min(MAX_LISTED, failures.size()));
            String more = failures.size() > MAX_LISTED ? " (+" + (failures.size() - MAX_LISTED) + " more)" : "";
            helper.fail(what + ": " + failures.size() + " of " + checked + " failed: " + String.join("; ", shown) +
                    more);
        }
    }
}
