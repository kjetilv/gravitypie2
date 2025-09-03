package com.github.kjetilv.gravitypie2;

public record Rate(int numerator, int denominator) {

    double pointBetween(double min, double max) {
        return min + (max - min) * numerator / denominator;
    }
}
