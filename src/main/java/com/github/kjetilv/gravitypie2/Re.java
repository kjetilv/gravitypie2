package com.github.kjetilv.gravitypie2;

record Re(
    double weight,
    double radius,
    double density
) {

    double mass() {
        return density * 4 / (3 * Math.PI * Math.pow(radius, 3));
    }
}
