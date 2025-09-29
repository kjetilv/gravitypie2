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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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

    private final Vector cameraLine = new Vector(0, 0, -2.16 * WORLD_SIZE_Z);

    private int cameraStep;

    private final List<Sphere> spheres;

    private final List<Re> res;

    private final List<Temperature> temperatures;

    private final List<PhongMaterial> materials;

    private final List<Vector> positions;

    private final List<Vector> accelerations;

    private final List<Vector> velocities;

    private final List<Vector> collisionImpulses;

    private final List<Vector> blackHolePositions;

    private final List<Re> blackHoles;

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
                )
        );

        positions = objects(COUNT, _ -> new Vector(new Range(-randomRange(), randomRange())));

        accelerations = zeroes(COUNT);

        velocities = zeroes(COUNT);

        temperatures = objects(COUNT, _ -> new Temperature(ROOM_TEMP));

        collisionImpulses = objects(COUNT, _ -> Vector.ZERO);

        materials = objects(
            COUNT, i -> {
                PhongMaterial phong = new PhongMaterial();
                phong.diffuseColorProperty().set(res.get(i).rgb(0));
                return phong;
            }
        );

        spheres = objects(
            COUNT, i -> {
                Sphere sphere = new Sphere(res.get(i).radius());
                sphere.setMaterial(materials.get(i));
                return sphere;
            }
        );

        Sphere origo = new Sphere(4);
        Material blueMaterial = new PhongMaterial(GHOSTWHITE);
        origo.setMaterial(blueMaterial);

        blackHolePositions = List.of(/*new Vector(0, 0, 0)*/);
        blackHoles = List.of(new Re(100, 25, 25, new Re.Color(0d, 0d, 0d, 1.0d)));

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
                spheres.stream(),
                wire.stream()
            )
            .flatMap(Function.identity());
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

            private final Instant start = Instant.now();

            private Duration total = Duration.ZERO;

            private long frames;

            private long lastSec;

            @Override
            public void handle(long l) {
                Instant before = Instant.now();
                update();
                Instant after = Instant.now();
                total = total.plus(Duration.between(before, after));
                frames++;
                long es = after.getEpochSecond();
                if (es > lastSec) {
                    double micros = .001d * total.toNanosPart() / frames;
                    long seconds = Duration.between(start, after).toSeconds();
                    System.out.printf("%.1fµs fps: %.1f (%d / %s =>)%n", micros, 1d * frames / seconds, frames, total);
                    lastSec = es;
                }
            }
        }.start();
    }

    private void update() {
        for (int i = 0; i < COUNT; i++) {
            accelerations.set(i, updatePulls(i));
        }
        for (int i = 0; i < COUNT; i++) {
            for (int j = i + 1; j < COUNT; j++) {
                handleCollision(i, j);
            }
        }
        for (int i = 0; i < COUNT; i++) {
            velocities.set(i, updateVelocity(i, velocities.get(i)));
        }

        for (int i = 0; i < COUNT; i++) {
            velocities.set(i, updateCollisionImpulse(i, velocities.get(i)));
        }

        for (int i = 0; i < COUNT; i++) {
            positions.set(i, positions.get(i).plus(velocities.get(i)));
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
        double distToOrigo = positions.get(i).length();
        double opa = 1 - distToOrigo / WORLD_SIZE_Z;
        materials.get(i).setDiffuseColor(res.get(i).rgb(opa));
    }

    private Vector updateVelocity(Integer index, Vector v) {
        return v.plus(accelerations.get(index)).mul(AIR_RETAIN);
    }

    private Vector updateCollisionImpulse(Integer index, Vector v) {
        Vector impulse = collisionImpulses.set(index, Vector.ZERO);
        return v.plus(impulse);
    }

    private void moveSphere(int i) {
        Sphere s = spheres.get(i);
        Vector p = positions.get(i);
        s.setTranslateX(p.x());
        s.setTranslateY(p.y());
        s.setTranslateZ(p.z());
    }

    private void handleWallBounce(int i) {
        double r = res.get(i).radius();

        Vector vel = velocities.get(i);
        double vx = vel.x(), vy = vel.y(), vz = vel.z();

        Vector pos = positions.get(i);
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
            velocities.set(i, new Vector(vx, vy, vz).mul(WALL_RETAIN));
            positions.set(i, new Vector(px, py, pz));
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
        Vector pos = positions.get(i);
        Re re = res.get(i);
        Stream<Vector> otherSpherePulls = positions.stream()
            .filter(p -> p != pos)
            .map(other ->
                other.minus(pos).mul(pullFrom(other, pos, re)));
        Stream<Vector> blackHolePulls = IntStream.range(0, blackHolePositions.size())
            .mapToObj(index ->
                blackHolePositions.get(index)
                    .minus(pos)
                    .mul(
                        pullFrom(blackHolePositions.get(index), pos, blackHoles.get(index))
                    ));
        return Stream.concat(otherSpherePulls, blackHolePulls)
            .reduce(
                new Vector(),
                Vector::plus,
                Vector::plus
            );
    }

    private void handleCollision(int i, int j) {
        Vector delta = positions.get(j).minus(positions.get(i));
        double iR = res.get(i).radius();
        double jR = res.get(j).radius();
        double dist = delta.zero() ? Math.min(iR, jR) / 100.0d : delta.length();
        if (dist <= iR + jR) {
            Vector n = delta.div(dist);
            double overlap = iR + jR - dist;

            double iMass = res.get(i).mass();
            double jMass = res.get(j).mass();

            double totalMass = iMass + jMass;
            double iMove = overlap * iMass / totalMass;
            double jMove = overlap * jMass / totalMass;

            positions.set(i, positions.get(i).minus(n.mul(iMove)));
            positions.set(j, positions.get(j).plus(n.mul(jMove)));

            double vRelN = velocities.get(i).minus(velocities.get(j)).dot(n);
            double rawImpulse = -(1 + Math.E) * vRelN / (1 / iMass + 1 / jMass);
            double impulse = COLLISION_RETAIN * rawImpulse;

            collisionImpulses.set(i, collisionImpulses.get(i).plus(n.mul(impulse / jMass)));
            collisionImpulses.set(j, collisionImpulses.get(j).minus(n.mul(impulse / iMass)));
        }
    }

    static final int COUNT = 640;

    static final Range R_RANGE = new Range(10, 45);

    static final int WORLD_SIZE_X = 1400;

    static final int WORLD_SIZE_Y = 1000;

    static final int WORLD_SIZE_Z = WORLD_SIZE_X;

    static final int Z_BOUND = WORLD_SIZE_Z / 2;

    static final int X_BOUND = WORLD_SIZE_X / 2;

    static final int Y_BOUND = WORLD_SIZE_Y / 2;

    static final double GRAV_CONSTANT = .02;

    static final double COLLISION_RETAIN = .8;

    static final double WALL_RETAIN = .9;

    static final double AIR_RETAIN = .9;

    static final int CAMERA_STEPS = 21600;

    static final double COLOUR_RANGE = 0.85;

    static final double ROOM_TEMP = 0.25;

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

    private static List<Vector> zeroes(int count) {
        return objects(count, _ -> new Vector());
    }

    private static <T> List<T> objects(int count, IntFunction<T> intFunction) {
        return new ArrayList<>(stream(intFunction, count).toList());
    }

    private static double pullFrom(Vector sPos, Vector pos, Re re) {
        double distance = pos.distanceTo(sPos);
        return GRAV_CONSTANT * re.weight() / (distance * distance);
    }

    private static <T> Stream<T> stream(IntFunction<T> f, int count) {
        return IntStream.range(0, count).mapToObj(f);
    }
}
