package com.github.kjetilv.gravitypie2;

import module java.base;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

record Vector(double x, double y, double z) {

    Vector() {
        this(0d, 0d, 0d);
    }

    Vector(Range range) {
        this(range, range, range);
    }

    Vector(Range rangeX, Range rangeY, Range rangeZ) {
        this(
            RND.nextDouble(rangeX.min(), rangeX.max()),
            RND.nextDouble(rangeY.min(), rangeY.max()),
            RND.nextDouble(rangeZ.min(), rangeZ.max())
        );
    }

    double length() {
        return sqrt(x * x + y * y + z * z);
    }

    boolean zero() {
        return x == 0d && y == 0d && z == 0d;
    }

    double dot(Vector v) {
        return x * v.x + y * v.y + z * v.z;
    }

    Vector mul(double d) {
        return new Vector(x * d, y * d, z * d);
    }

    Vector div(double d) {
        return new Vector(x / d, y / d, z / d);
    }

    Vector plus(Vector v) {
        return new Vector(x + v.x, y + v.y, z + v.z);
    }

    Vector minus(Vector v) {
        return new Vector(x - v.x, y - v.y, z - v.z);
    }

    /**
     * Calculates the distance between this vector and another vector, when both are seen as points.
     *
     * @param v Other vector
     * @return Distance
     */
    double distanceTo(Vector v) {
        return v.equals(this) ? 0d : sqrt(
            pow(x - v.x, 2) +
            pow(y - v.y, 2) +
            pow(z - v.z, 2)
        );
    }

    private static final Random RND = new Random();
}
