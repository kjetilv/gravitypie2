package com.github.kjetilv.gravitypie2;

record Re(
    double weight,
    double radius,
    double density,
    Color color
) {

    public javafx.scene.paint.Color rgb(double dim, double opa) {
        return color().rgb(dim, opa);
    }

    double mass() {
        return density * 4 / (3 * Math.PI * Math.pow(radius, 3));
    }

    record Color(double r, double g, double b, double opacity) {

        javafx.scene.paint.Color rgb(double dim, double op) {
            return new javafx.scene.paint.Color(
                r * dim,
                g * dim,
                b * dim,
                op < 0 ? opacity() : op
            );
        }
    }
}
