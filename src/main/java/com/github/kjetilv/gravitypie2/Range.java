package com.github.kjetilv.gravitypie2;

record Range(double min, double max) {

    double scale(double num, double den) {
        return scale(num / den);
    }

    double scale(double v) {
        return min + (max - min) * v;
    }

    double upper(double v) {
        return min + (1d - min) * v;
    }
}
