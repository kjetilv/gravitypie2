package com.github.kjetilv.gravitypie2;

record Range(double min, double max) {
    double point(double num, double den) {
        return min + (max - min) * num / den;
    }
}
