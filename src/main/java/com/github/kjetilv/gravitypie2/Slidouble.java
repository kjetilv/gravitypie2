package com.github.kjetilv.gravitypie2;

final class Slidouble {

    private final String name;

    private double value;

    Slidouble(String name, double value) {
        this.name = name;
        value(value);
    }

    public double times(double d) {
        return value * d;
    }

    public double value() {
        return value;
    }

    public void value(double value) {
        this.value = value;
    }

    public double mirrorValue() {
        return 1 - value;
    }

    public double mirrorTimes(double rawImpulse) {
        return (1 - value) * rawImpulse;
    }

    String labelString() {
        return String.format(name + ": %.4f", value);
    }
}
