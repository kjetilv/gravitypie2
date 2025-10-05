package com.github.kjetilv.gravitypie2;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.effect.MotionBlur;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;

import java.util.List;
import java.util.function.DoubleFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static javafx.scene.transform.Rotate.X_AXIS;
import static javafx.scene.transform.Rotate.Z_AXIS;

final class Shapes {

    public static final double TWO_THIRDS = 2.0 / 3;

    public static final double THIRD = 1.0 / 3;

    static Group createWireBox(double width, double height, double depth) {
        double x = width / 2;
        double y = height / 2;
        double z = depth / 2;

        return new Group(
            edge(width, 0, -y, -z, Z_AXIS, 90),
            edge(width, 0, -y, z, Z_AXIS, 90),
            edge(width, 0, y, -z, Z_AXIS, 90),
            edge(width, 0, y, z, Z_AXIS, 90),
            edge(height, -x, 0, -z, null, 0),
            edge(height, x, 0, -z, null, 0),
            edge(height, -x, 0, z, null, 0),
            edge(height, x, 0, z, null, 0),
            edge(depth, -x, -y, 0, X_AXIS, 90),
            edge(depth, x, -y, 0, X_AXIS, 90),
            edge(depth, -x, y, 0, X_AXIS, 90),
            edge(depth, x, y, 0, X_AXIS, 90)
        );
    }

    static Stream<Re.Color> colors(int steps) {
        return components(steps, Re.Color::r).flatMap(r ->
            components(steps, Re.Color::b).flatMap(b ->
                components(steps, Re.Color::g).map(g ->
                    r.add(g).add(b))));
    }

    static Re.Color color(int i, int count) {
        double ratio = 1d * i / count;
        double rads = ratio * 3 * Math.PI;
        int phase = ratio < THIRD ? 0
            : ratio < TWO_THIRDS ? 1
                : 2;
        double r = positiveAngle(rads);
        double g = positiveAngle(rads + Math.PI / 2);
        double b = positiveAngle(rads + Math.PI);
        return new Re.Color(
            phase == 0 ? PRIMARY_RANGE.scale(r) : COLOUR_RANGE.scale(r),
            phase == 1 ? PRIMARY_RANGE.scale(g) : COLOUR_RANGE.scale(g),
            phase == 2 ? PRIMARY_RANGE.scale(b) : COLOUR_RANGE.scale(b),
            1d
        );
    }

    static <T> T get(int i, int count, List<T> list) {
        double point = 1d * i / count;
        int index = (int) (point * list.size());
        return list.get(index);
    }

    private Shapes() {
    }

    private static final Range PRIMARY_RANGE = new Range(0.2, 0.95);

    private static final Range COLOUR_RANGE = new Range(0.1, 0.85);

    private static final double WIRE_THICK = 1.2;

    private static final Color EDGE_COLOR = Color.color(.3d, .3d, .32d, .1d);

    private static final PhongMaterial EDGE = new PhongMaterial(EDGE_COLOR);

    private static Stream<Re.Color> components(int steps, DoubleFunction<Re.Color> get) {
        return IntStream.range(0, steps)
            .mapToDouble(Double::valueOf)
            .map(d -> COLOUR_RANGE.scale(d, steps))
            .mapToObj(get);
    }

    private static Cylinder edge(
        double length,
        double x,
        double y,
        double z,
        Point3D axis,
        double angle
    ) {
        Cylinder cyl = new Cylinder(Shapes.WIRE_THICK, length);
        cyl.setMaterial(Shapes.EDGE);
        cyl.setEffect(new MotionBlur(10, 10));
        cyl.setTranslateX(x);
        cyl.setTranslateY(y);
        cyl.setTranslateZ(z);
        if (axis != null) {
            cyl.setRotationAxis(axis);
            cyl.setRotate(angle);
        }
        return cyl;
    }

    private static double positiveAngle(double angle) {
        return Math.max(0, Math.cos(angle));
    }
}
