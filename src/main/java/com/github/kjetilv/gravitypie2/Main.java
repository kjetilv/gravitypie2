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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static javafx.scene.paint.Color.*;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private final Vector cameraLine = new Vector(0, 0, -2180);

    private int cameraStep;

    private final List<Sphere> spheres;

    private final List<Re> res;

    private final List<Temperature> temperatures;

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

        res = objects(i ->
            new Re(10, R_RANGE.point(new Rate(i, 10)), 1)
        );

        positions = objects(_ ->
            new Vector(new Range(-350, 350)));

        accelerations = zeroes();

        velocities = zeroes();

        temperatures = objects(i ->
            new Temperature(.5));

        collisionImpulses =
            objects(i -> Vector.ZERO);

        spheres = objects(i -> {
            Sphere sphere = new Sphere(res(i).radius());
            Material redMaterial = new PhongMaterial(RED);
            sphere.setMaterial(redMaterial);
            return sphere;
        });

        Sphere origo = new Sphere(4);
        Material blueMaterial = new PhongMaterial(BLUE);
        origo.setMaterial(blueMaterial);

        blackHolePositions = List.of(/*new Vector(0, 0, 0)*/);
        blackHoles = List.of(new Re(100, 25, 25));

        AmbientLight ambient = new AmbientLight(color(.3, .3, 0.5));

        PointLight pl1 = new PointLight(WHITE);
        pl1.setTranslateX(-400);
        pl1.setTranslateY(400);
        pl1.setTranslateZ(100);

        PointLight pl2 = new PointLight(WHITE);
        pl2.setTranslateX(400);
        pl2.setTranslateY(-4000);
        pl2.setTranslateZ(-100);

        camera = new PerspectiveCamera(true);
        camera.setNearClip(1);
        camera.setFarClip(5000);
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

            @Override
            public void handle(long l) {
                update();
            }
        }.start();
    }

    private void update() {
        for (int i = 0; i < COUNT; i++) {
            updateAcceleration(i);
        }
        for (int i = 0; i < COUNT; i++) {
            for (int j = i + 1; j < COUNT; j++) {
                handleCollision(i, j);
            }
        }
        for (int i = 0; i < COUNT; i++) {
            updateVelocity(i);
        }

        for (int i = 0; i < COUNT; i++) {
            updateCollisionImpulse(i);
        }

        for (int i = 0; i < COUNT; i++) {
            updatePosition(i);
        }

        for (int i = 0; i < COUNT; i++) {
            handleWallBounce(i);
        }

        for (int i = 0; i < COUNT; i++) {
            moveSphere(i);
        }

        moveCamera();
    }

    private void updatePosition(int i) {
        setPos(
            i,
            (index, v) ->
                v.plus(vel(index))
        );
    }

    private void updateAcceleration(int i) {
        setAcc(
            i,
            (index, _) ->
                updatePulls(index)
        );
    }

    private void updateVelocity(int i) {
        setVel(
            i,
            (index, v) ->
                updateVelocity(index, v)
        );
    }

    private Vector updateVelocity(Integer index, Vector v) {
        return v.plus(accelerations.get(index)).mul(AIR_BRAKE);
    }

    private void updateCollisionImpulse(int i) {
        setVel(
            i,
            (index, v) ->
                updateCollisionImpulse(index, v)
        );
    }

    private Vector updateCollisionImpulse(Integer index, Vector v) {
        Vector impulse = collisionImpulses.set(index, Vector.ZERO);
        return v.plus(impulse);
    }

    private void moveSphere(int i) {
        Sphere s = spheres.get(i);
        Vector p = pos(i);
        s.setTranslateX(p.x());
        s.setTranslateY(p.y());
        s.setTranslateZ(p.z());
    }

    private void handleWallBounce(int i) {
        double r = res(i).radius();

        Vector vel = vel(i);
        double vx = vel.x(), vy = vel.y(), vz = vel.z();

        Vector pos = pos(i);
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
            velocities.set(i, new Vector(vx, vy, vz).mul(WALL_BRAKE));
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
        Vector pos = pos(i);
        Re re = res(i);
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
        Vector delta = pos(j).minus(pos(i));
        double iR = res(i).radius();
        double jR = res(j).radius();
        double dist = delta.zero() ? Math.min(iR, jR) / 100.0d : delta.length();
        if (dist <= iR + jR) {
            Vector n = delta.div(dist);
            double overlap = iR + jR - dist;

            double iMass = res(i).mass();
            double jMass = res(j).mass();

            double iMove = overlap * iMass / (iMass + jMass);
            double jMove = overlap * jMass / (iMass + jMass);

            setPos(i, v -> v.minus(n.mul(iMove)));
            setPos(j, v -> v.plus(n.mul(jMove)));

            double vRelN = vel(i).minus(vel(j)).dot(n);
            double impulseMagnitude = COLLISION_BRAKE * (-(1 + Math.E) * vRelN / (1 / iMass + 1 / jMass));

            setImp(i, (_, v) -> v.plus(n.mul(impulseMagnitude / iMass)));
            setImp(j, (_, v) -> v.minus(n.mul(impulseMagnitude / jMass)));
        }
    }

    private void setVel(int i, Vector v) {
        velocities.set(i, v);
    }

    private void setVel(int i, UnaryOperator<Vector> op) {
        set(velocities, i, op);
    }

    private void setVel(int i, BiFunction<Integer, Vector, Vector> op) {
        set(velocities, i, v -> op.apply(i, v));
    }

    private void setPos(int i, UnaryOperator<Vector> op) {
        set(positions, i, op);
    }

    private void setPos(int i, BiFunction<Integer, Vector, Vector> op) {
        set(positions, i, v -> op.apply(i, v));
    }

    private void setImp(int i, BiFunction<Integer, Vector, Vector> op) {
        set(collisionImpulses, i, v -> op.apply(i, v));
    }

    private void setAcc(int i, UnaryOperator<Vector> op) {
        set(accelerations, i, op);
    }

    private void setAcc(int i, BiFunction<Integer, Vector, Vector> op) {
        set(accelerations, i, v -> op.apply(i, v));
    }

    private Re res(int i) {
        return res.get(i);
    }

    private Vector pos(int j) {
        return positions.get(j);
    }

    private Vector vel(int i) {
        return velocities.get(i);
    }

    static final int COUNT = 50;

    static final int WORLD_SIZE_X = 1024;

    static final int WORLD_SIZE_Y = 768;

    static final int WORLD_SIZE_Z = 1024;

    static final int X_BOUND = WORLD_SIZE_X / 2;

    static final int Y_BOUND = WORLD_SIZE_Y / 2;

    static final int Z_BOUND = WORLD_SIZE_Z / 2;

    static final double GRAV_CONSTANT = .01;

    static final double COLLISION_BRAKE = .99;

    static final double WALL_BRAKE = .8;

    static final double AIR_BRAKE = .95;

    static final int CAMERA_STEPS = 21600;

    static final Range R_RANGE = new Range(10, 20);

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
        Color gridColor = color(0.2, 0.8, 1.0, 0.4);
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

    private static List<Vector> zeroes() {
        return objects(_ -> new Vector());
    }

    private static <T> List<T> objects(IntFunction<T> intFunction) {
        return new ArrayList<>(stream(intFunction).toList());
    }

    private static <T> void set(List<T> list, int i, UnaryOperator<T> transform) {
        list.set(i, transform.apply(list.get(i)));
    }

    private static double pullFrom(Vector sPos, Vector pos, Re re) {
        double distance = pos.distanceTo(sPos);
        return GRAV_CONSTANT * re.weight() / (distance * distance);
    }

    private static <T> Stream<T> stream(IntFunction<T> f) {
        return IntStream.range(0, COUNT).mapToObj(f);
    }

}
