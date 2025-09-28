package com.github.kjetilv.gravitypie2;

record Range(double min, double max) {

    Range(double max) {
        this(0d, max);
    }

    Range() {
        this(0d, 0d);
    }

    double point(double num, double den) {
        return min + (max - min) * num / den;
    }
}
