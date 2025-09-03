package com.github.kjetilv.gravitypie2;

record Temperature(double heat) {

    Temperature(double heat) {
        this.heat = ranged(heat, 1);
    }

    Temperature heat(double amount) {
        return new Temperature(heat * ranged(amount, 1));
    }

    Temperature cool(double amount) {
        return new Temperature(heat * (1d - ranged(amount, 1)));
    }

    private static double ranged(double amount) {
        return ranged(amount, 1d);
    }

    private static double ranged(double amount, double max) {
        return Math.max(0, Math.min(max, amount));
    }
}
