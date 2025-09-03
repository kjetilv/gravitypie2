package com.github.kjetilv.gravitypie2;

record Range(double min, double max) {

    Range(double max) {
        this(0d, max);
    }

    Range() {
        this(0d, 0d);
    }

    double point(Rate rate) {
        return rate.pointBetween(min, max);
    }

    double range() {
        return max - min;
    }

    Range value(Range range) {
        return new Range(Math.min(min, range.min), Math.max(max, range.max));
    }

    Range value(double d) {
        return new Range(Math.min(min, d), Math.max(max, d));
    }

    double oscValue(double osc) {
        double zeroToOne = (osc + 1.0d) / 2;
        return zeroToOneValue(zeroToOne);
    }

    double zeroToOneValue(double zeroToOne) {
        return min + zeroToOne * range();
    }

    private static double ranged(double amount) {
        return Math.max(0, Math.min(1, amount));
    }
}
