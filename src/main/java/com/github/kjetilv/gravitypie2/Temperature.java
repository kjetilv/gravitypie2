package com.github.kjetilv.gravitypie2;

record Temperature(double heat) {

    Temperature(double heat) {
        this.heat = Math.max(0, Math.min(1, heat));
    }

    Temperature adjust(double amount) {
        return new Temperature(ranged(heat * amount));
    }

    private static double ranged(double amount) {
        return Math.max(0, Math.min(1d, amount));
    }
}
