package com.github.kjetilv.gravitypie2;

record Re(
    double weight,
    double radius,
    double density,
    Color color,
    Color specular
) {

    public javafx.scene.paint.Color toRgb(double dim, double opa) {
        return color().rgb(dim, opa);
    }

    double mass() {
        return density * 4 / (3 * Math.PI * Math.pow(radius, 3));
    }

    record Color(double r, double g, double b, double opacity) {

        static Color r(double r) {
            return new Color(r, 0d, 0d);
        }

        static Color g(double g) {
            return new Color(0d, g, 0d);
        }

        static Color b(double b) {
            return new Color(0d, 0d, b);
        }

        Color(double b, double g, double r) {
            this(r, g, b, 1d);
        }

        public Color add(Color c) {
            return new Color(
                Math.min(1d, r + c.r()),
                Math.min(1d, g + c.g()),
                Math.min(1d, b + c.b()),
                Math.min(1d, (opacity + c.opacity()) / 2)
            );
        }

        public Color brighten(double v) {
            return new Color(
                adjust(r, v),
                adjust(g, v),
                adjust(b, v),
                opacity
            );
        }

        javafx.scene.paint.Color rgb(double dim, double op) {
            return new javafx.scene.paint.Color(
                r * dim,
                g * dim,
                b * dim,
                op < 0 ? opacity() : op
            );
        }

        javafx.scene.paint.Color rgb() {
            return new javafx.scene.paint.Color(r, g, b, opacity);
        }

        private static double adjust(double color, double adjust) {
            double headroom = 1 - color;
            return color + Math.min(1d, adjust) * headroom;
        }

        @Override
        public String toString() {
            return String.format("r%.3f g%.3f b%.3f", r, g, b);
        }
    }
}
