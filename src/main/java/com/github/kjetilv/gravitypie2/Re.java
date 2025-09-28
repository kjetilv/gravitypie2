package com.github.kjetilv.gravitypie2;

record Re(
    double weight,
    double radius,
    double density,
    Color color
) {

    record Color(double r, double g, double b, double opa) {}

    double mass() {
        return density * 4 / (3 * Math.PI * Math.pow(radius, 3));
    }
}
