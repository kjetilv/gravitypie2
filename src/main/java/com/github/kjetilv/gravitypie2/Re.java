package com.github.kjetilv.gravitypie2;

record Re(
    double weight,
    double radius,
    double density,
    Color color
) {

    public javafx.scene.paint.Color rgb(double dim, double opa) {
        Color cc = color();
        return new javafx.scene.paint.Color(
            cc.r() * dim,
            cc.g() * dim,
            cc.b() * dim,
            opa < 0 ? cc.opa() : opa
        );
    }

    record Color(double r, double g, double b, double opa) {}

    double mass() {
        return density * 4 / (3 * Math.PI * Math.pow(radius, 3));
    }
}
