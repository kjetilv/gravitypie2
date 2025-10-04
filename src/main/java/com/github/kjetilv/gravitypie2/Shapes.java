package com.github.kjetilv.gravitypie2;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;

import static javafx.scene.transform.Rotate.X_AXIS;
import static javafx.scene.transform.Rotate.Z_AXIS;

final class Shapes {

    public static final double WIRE_THICK = 1.2;

    public static final Color EDGE_COLOR = Color.color(.3d, .3d, .32d, .1d);

    public static final PhongMaterial EDGE = new PhongMaterial(EDGE_COLOR);

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

    private Shapes() {
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
        cyl.setTranslateX(x);
        cyl.setTranslateY(y);
        cyl.setTranslateZ(z);
        if (axis != null) {
            cyl.setRotationAxis(axis);
            cyl.setRotate(angle);
        }
        return cyl;
    }
}
