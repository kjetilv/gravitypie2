package com.github.kjetilv.gravitypie2;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Line;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

import java.lang.reflect.Array;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static javafx.scene.paint.Color.*;

public class Main extends Application {

    static void main(String[] args) {
        launch(args);
    }

    private final Vector cameraLine = new Vector(0, 0, -2 * WORLD_SIZE_Z);

    private int cameraStep;

    private final Sphere[] spheres;

    private final Re[] res;

    private final Temperature[] temperatures;

    private final PhongMaterial[] materials;

    private final Vector[] positions;

    private final Vector[] accelerations;

    private final Vector[] velocities;

    private final Vector[] collisionImpulses;

    private final SubScene subScene;

    private final String title;

    private final PerspectiveCamera camera;

    public Main() {
        this.title = "Stars";

        res = objects(
            COUNT, i ->
                new Re(
                    10,
                    R_RANGE.point(i, COUNT),
                    1L,
                    color(i, COUNT)
                ), Re.class
        );

        positions = objects(COUNT, _ -> new Vector(new Range(-randomRange(), randomRange())), Vector.class);

        accelerations = zeroes(COUNT);

        velocities = zeroes(COUNT);

        temperatures = objects(COUNT, _ -> new Temperature(ROOM_TEMP), Temperature.class);

        collisionImpulses = objects(COUNT, _ -> ZERO, Vector.class);

        materials = objects(
            COUNT, i -> {
                PhongMaterial phong = new PhongMaterial();
                phong.diffuseColorProperty().set(res[i].rgb(0));
                return phong;
            }, PhongMaterial.class
        );

        spheres = objects(
            COUNT,
            i -> {
                Sphere sphere = new Sphere(res[i].radius());
                sphere.setMaterial(materials[i]);
                return sphere;
            }, Sphere.class
        );

        Sphere origo = new Sphere(4);
        Material blueMaterial = new PhongMaterial(GHOSTWHITE);
        origo.setMaterial(blueMaterial);

        AmbientLight ambient = new AmbientLight(Color.color(.3, .3, 0.5));

        PointLight pl1 = new PointLight(WHITE);
        pl1.setTranslateX(-(.4 * WORLD_SIZE_X));
        pl1.setTranslateY(.4 * WORLD_SIZE_Y);
        pl1.setTranslateZ(.1 * WORLD_SIZE_X);

        PointLight pl2 = new PointLight(WHITE);
        pl2.setTranslateX(.4 * WORLD_SIZE_X);
        pl2.setTranslateY(-4 * WORLD_SIZE_Y);
        pl2.setTranslateZ(-.1 * WORLD_SIZE_Z);

        camera = new PerspectiveCamera(true);
        camera.setNearClip(1);
        camera.setFarClip(5 * WORLD_SIZE_Z);
        camera.setRotationAxis(Rotate.Y_AXIS);
        camera.setRotate(0);

        List<Node> wire = wires(X_BOUND, Y_BOUND, Z_BOUND);

        Stream<Node> nodeStream = Stream.of(
            Stream.of(ambient, pl1, pl2, origo),
            Arrays.stream(spheres),
            wire.stream()
        ).flatMap(Function.identity());
        List<Node> nodes = nodeStream.toList();

        Group world = new Group(nodes);
        subScene = new SubScene(
            world,
            WORLD_SIZE_X,
            WORLD_SIZE_Y,
            true,
            SceneAntialiasing.BALANCED
        );
        subScene.setFill(BLACK);
        subScene.setCamera(camera);
    }

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane(subScene);
        Scene scene = new Scene(root, WORLD_SIZE_X, WORLD_SIZE_Y, true);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();

        new AnimationTimer() {

            private final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(5);

            private Duration total = Duration.ZERO;

            private long frames;

            private long lastSec = start.getEpochSecond();

            @Override
            public void handle(long l) {
                Instant before = Instant.now();
                update();
                Instant after = Instant.now();
                if (after.isAfter(start)) {
                    total = total.plus(Duration.between(before, after));
                    frames++;
                    long es = after.getEpochSecond();
                    if (es > lastSec) {
                        try {
                            double msTotal = total.getSeconds() * 1_000d + total.getNano() / 1_000_000d;
                            double microsPerFrame = msTotal / frames;
                            long seconds = Duration.between(start, after).toSeconds();
                            double fps = 1d * frames / seconds;
                            double bugdet = 1000d / fps;
                            System.out.printf(
                                "%.1fms/frame %.1ffps [%.1fms]%n",
                                microsPerFrame,
                                fps,
                                bugdet
                            );
                        } finally {
                            lastSec = es;
                        }
                    }
                } else {
                    update();
                }
            }
        }.start();
    }

    private void update() {
        for (int i = 0; i < COUNT; i++) {
            accelerations[i] = updatePulls(i);
        }
        for (int i = 0; i < COUNT; i++) {
            for (int j = i + 1; j < COUNT; j++) {
                handleCollision(i, j);
            }
        }
        for (int i = 0; i < COUNT; i++) {
            velocities[i] = updateVelocity(i, velocities[i]);
        }

        for (int i = 0; i < COUNT; i++) {
            velocities[i] = updateCollisionImpulse(i, velocities[i]);
        }

        for (int i = 0; i < COUNT; i++) {
            positions[i] = positions[i].plus(velocities[i]);
        }

        for (int i = 0; i < COUNT; i++) {
            handleWallBounce(i);
        }

        for (int i = 0; i < COUNT; i++) {
            moveSphere(i);
        }

        for (int i = 0; i < COUNT; i++) {
            setOpacity(i);
        }

        moveCamera();
    }

    private void setOpacity(int i) {
        double distToOrigo = positions[i].length();
        double opa = 1 - distToOrigo / WORLD_SIZE_Z;
        materials[i].setDiffuseColor(res[i].rgb(opa));
    }

    private Vector updateVelocity(Integer index, Vector v) {
        return v.plus(accelerations[index]).mul(AIR_RETAIN);
    }

    private Vector updateCollisionImpulse(Integer index, Vector v) {
        Vector collisionImpulse = collisionImpulses[index];
        collisionImpulses[index] = ZERO;
        return v.plus(collisionImpulse);
    }

    private void moveSphere(int i) {
        Sphere s = spheres[i];
        Vector p = positions[i];
        s.setTranslateX(p.x());
        s.setTranslateY(p.y());
        s.setTranslateZ(p.z());
    }

    private void handleWallBounce(int i) {
        double r = res[i].radius();

        Vector vel = velocities[i];
        double vx = vel.x(), vy = vel.y(), vz = vel.z();

        Vector pos = positions[i];
        double px = pos.x(), py = pos.y(), pz = pos.z();

        boolean h = false;

        if (px < -X_BOUND + r) {
            vx = Math.abs(vx);
            px = -X_BOUND + r + 1;
            h = true;
        } else if (px > X_BOUND - r) {
            vx = -Math.abs(vx);
            px = X_BOUND - r - 1;
            h = true;
        }

        if (py < -Y_BOUND + r) {
            vy = Math.abs(vy);
            py = -Y_BOUND + r + 1;
            h = true;
        } else if (py > Y_BOUND - r) {
            vy = -Math.abs(vy);
            py = Y_BOUND - r - 1;
            h = true;
        }

        if (pz < -Z_BOUND + r) {
            vz = Math.abs(vz);
            pz = -Z_BOUND + r + 1;
            h = true;
        } else if (pz > Z_BOUND - r) {
            vz = -Math.abs(vz);
            pz = Z_BOUND - r - 1;
            h = true;
        }

        if (h) {
            velocities[i] = new Vector(vx, vy, vz).mul(WALL_RETAIN);
            positions[i] = new Vector(px, py, pz);
        }
    }

    private void moveCamera() {
        double angle = 2 * Math.PI * cameraStep / CAMERA_STEPS;
        Vector position = new Vector(
            Math.sin(angle) * cameraLine.length(),
            0,
            Math.cos(angle) * cameraLine.length()
        );

        // Move the camera along the circle around origin
        camera.setTranslateX(position.x());
        camera.setTranslateY(position.y());
        camera.setTranslateZ(position.z());

        // Compute yaw (rotation around Y axis) and pitch (rotation around X axis)
        double yaw = Math.toDegrees(Math.atan2(-position.x(), -position.z()));
        camera.setRotate(yaw);

        cameraStep = (cameraStep - 1) % CAMERA_STEPS;
    }

    private Vector updatePulls(int i) {
        Vector pos = positions[i];
        Re re = res[i];
        Vector pull = ZERO;
        for (int j = 0; j < COUNT; j++) {
            if (i != j) {
                double force = pullFrom(positions[j], pos, re);
                pull = pull.plus(positions[j].minus(pos).mul(force));
            }
        }
        return pull;
    }

    private void handleCollision(int i, int j) {
        assert i != j;
        Vector delta = positions[j].minus(positions[i]);
        double iR = res[i].radius();
        double jR = res[j].radius();
        double dist = delta.zero() ? Math.min(iR, jR) / 100.0d : delta.length();
        if (dist <= iR + jR) {
            Vector n = delta.div(dist);
            double overlap = iR + jR - dist;

            double iMass = res[i].mass();
            double jMass = res[j].mass();

            double totalMass = iMass + jMass;
            double iMove = overlap * iMass / totalMass;
            double jMove = overlap * jMass / totalMass;

            positions[i] = positions[i].minus(n.mul(iMove));
            positions[j] = positions[j].plus(n.mul(jMove));

            double vRelN = velocities[i].minus(velocities[j]).dot(n);
            double rawImpulse = -(1 + Math.E) * vRelN / (1 / iMass + 1 / jMass);
            double impulse = COLLISION_RETAIN * rawImpulse;

            collisionImpulses[i] = collisionImpulses[i].plus(n.mul(impulse / jMass));
            collisionImpulses[j] = collisionImpulses[j].minus(n.mul(impulse / iMass));
        }
    }

    static final int COUNT = 1040;

    static final Range R_RANGE = new Range(5, 40);

    static final int WORLD_SIZE_X = 1440;

    static final int WORLD_SIZE_Y = 900;

    static final int WORLD_SIZE_Z = WORLD_SIZE_X;

    static final int Z_BOUND = WORLD_SIZE_Z / 2;

    static final int X_BOUND = WORLD_SIZE_X / 2;

    static final int Y_BOUND = WORLD_SIZE_Y / 2;

    static final double GRAV_CONSTANT = .01;

    static final double COLLISION_RETAIN = .75;

    static final double WALL_RETAIN = .9;

    static final double AIR_RETAIN = .9;

    static final int CAMERA_STEPS = 21600;

    static final double COLOUR_RANGE = 0.85;

    static final double ROOM_TEMP = 0.25;

    static Vector ZERO = new Vector();

    private static double randomRange() {
        return WORLD_SIZE_Z * 0.48;
    }

    private static Re.Color color(int i, int count) {
        double ratio = 1d * i / count;
        double angle = ratio * 3 * Math.PI;

        double r = Math.sin(angle);
        double g = Math.cos(angle + Math.PI / 2);
        double b = Math.cos(angle + Math.PI);
        return new Re.Color(
            adjust(r),
            adjust(g),
            adjust(b),
            .99d
        );
    }

    private static double adjust(double r) {
        double raw = Math.max(0, r);
        double inRange = raw * COLOUR_RANGE;
        return 1.0 - COLOUR_RANGE + inRange;
    }

    private static List<Node> wires(double halfX, double halfY, double halfZ) {
        List<Node> wire = new ArrayList<>();
        // 12 edges of the box
        double[] xs = new double[] {-halfX, halfX};
        double[] ys = new double[] {-halfY, halfY};
        double[] zs = new double[] {-halfZ, halfZ};
        // Edges parallel to X at each combination of y,z
        for (double y : ys) {
            for (double z : zs) {
                Line l = newLine();
                start(l, -halfX, 0);
                end(l, halfX, 0);
                l.setTranslateY(y);
                l.setTranslateZ(z);
                wire.add(l);
            }
        }
        // Edges parallel to Y at each combination of x,z
        for (double x : xs) {
            for (double z : zs) {
                Line l = newLine();
                start(l, 0, -halfY);
                end(l, 0, halfY);
                l.setTranslateX(x);
                l.setTranslateZ(z);
                wire.add(l);
            }
        }
        // Edges parallel to Z at each combination of x,y
        for (double x : xs) {
            for (double y : ys) {
                Line l = newLine();
                // JavaFX 2D Line has no Z endpoints; approximate Z edges as Y lines rotated around X.
                start(l, 0, -halfZ);
                end(l, 0, halfZ);
                l.setTranslateX(x);
                l.setTranslateY(y);
                l.setRotationAxis(Rotate.X_AXIS);
                l.setRotate(90);
                wire.add(l);
            }
        }
        return wire;
    }

    private static Line newLine() {
        Line l = new Line();
        Color gridColor = Color.color(0.2, 0.8, 1.0, 0.4);
        l.setStroke(gridColor);
        l.setStrokeWidth(5d);
        return l;
    }

    private static void start(Line l, double x, double y) {
        l.setStartX(x);
        l.setStartY(y);
    }

    private static void end(Line l, double x, double y) {
        l.setEndX(x);
        l.setEndY(y);
    }

    private static Vector[] zeroes(int count) {
        return objects(count, _ -> new Vector(), Vector.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] objects(int count, IntFunction<T> intFunction, Class<T> clazz) {
        Object array = Array.newInstance(clazz, count);
        IntStream.range(0, count)
            .forEach(i ->
                Array.set(array, i, intFunction.apply(i))
            );
        return (T[]) array;
    }

    private static double pullFrom(Vector sPos, Vector pos, Re re) {
        double distance = pos.distanceTo(sPos);
        return GRAV_CONSTANT * re.weight() / (distance * distance);
    }
}
