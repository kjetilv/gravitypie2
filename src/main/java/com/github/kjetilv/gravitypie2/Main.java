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

public class Main extends Application {

    public static final int COUNT = 5;

    public static final int WORLD_SIZE_X = 1024;

    public static final int WORLD_SIZE_Y = 768;

    public static final int WORLD_SIZE_Z = 1024;

    public static void main(String[] args) {
        launch(args);
    }

    private Vector cameraLine = new Vector(0, 0, -2400);

    private int cameraSteps = 3600;

    private int cameraStep = cameraSteps / 2;

    private final List<Sphere> spheres;

    private final List<Re> res;

    private final List<Vector> positions;

    private final List<Vector> accelerations;

    private final List<Vector> velocities;

    private final List<Vector> blackHolePositions;

    private final List<Re> blackHoles;

    private final SubScene subScene;

    private final String title;

    private final PerspectiveCamera camera;

    private final Sphere origo;

    public Main() {
        this.title = "Stars";

        res = objects(_ ->
            new Re(10, 2 * (5 + 5), 5)
        );

        positions = objects(_ ->
            new Vector(new Range(-350, 350)));

        accelerations = zeroes();

        velocities = zeroes();

        spheres = objects(i -> {
            Sphere sphere = new Sphere(res(i).radius());
            Material redMaterial = new PhongMaterial(Color.RED);
            sphere.setMaterial(redMaterial);
            return sphere;
        });

        origo = new Sphere(4);
        Material blueMaterial = new PhongMaterial(Color.BLUE);
        origo.setMaterial(blueMaterial);

        blackHolePositions = List.of(/*new Vector(0, 0, 0)*/);
        blackHoles = List.of(new Re(100, 25, 25));

        AmbientLight ambient = new AmbientLight(Color.color(BOUNCE, BOUNCE, 0.5));

        PointLight pl1 = new PointLight(Color.WHITE);
        pl1.setTranslateX(-400);
        pl1.setTranslateY(400);
        pl1.setTranslateZ(100);

        PointLight pl2 = new PointLight(Color.WHITE);
        pl2.setTranslateX(400);
        pl2.setTranslateY(-4000);
        pl2.setTranslateZ(-100);

        camera = new PerspectiveCamera(true);
        camera.setNearClip(1);
        camera.setFarClip(5000);
        camera.setRotationAxis(Rotate.Y_AXIS);
        camera.setRotate(0);

        // Create a wireframe cube centered at origin
        double half = 500; // half-size of the cube edge length 1000
        Color gridColor = Color.color(0.2, 0.8, 1.0, 0.4);
        List<Node> wire = new ArrayList<>();
        // 12 edges of the cube
        double[] xs = new double[] {-half, half};
        double[] ys = new double[] {-half, half};
        double[] zs = new double[] {-half, half};
        // Edges parallel to X at each combination of y,z
        for (double y : ys) {
            for (double z : zs) {
                Line l = newLine(gridColor);
                start(l, -half, 0);
                end(l, half, 0);
                l.setTranslateY(y);
                l.setTranslateZ(z);
                wire.add(l);
            }
        }
        // Edges parallel to Y at each combination of x,z
        for (double x : xs) {
            for (double z : zs) {
                Line l = newLine(gridColor);
                start(l, 0, -half);
                end(l, 0, half);
                l.setTranslateX(x);
                l.setTranslateZ(z);
                wire.add(l);
            }
        }
        // Edges parallel to Z at each combination of x,y
        for (double x : xs) {
            for (double y : ys) {
                Line l = newLine(gridColor);
                // JavaFX 2D Line has no Z endpoints; use translateZ to position a zero-length projection line
                // Instead, create two small lines to hint depth; but an easier way is to use 3D cylinders.
                // For minimal change, approximate Z edges by slightly offset X-lines rotated 90deg around Y.
                // We'll use a Box as invisible helper is overkill; better draw using SubScene camera facing Z.
                // Workaround: use a 3D Group with a 2D Line rotated around X so length maps to Z visually.
                // Simpler: draw Z edges as Y lines and rotate 90deg around X to align along Z.
                start(l, 0, -half);
                end(l, 0, half);
                l.setTranslateX(x);
                l.setTranslateY(y);
                l.setRotationAxis(Rotate.X_AXIS);
                l.setRotate(90);
                wire.add(l);
            }
        }

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
        subScene.setFill(Color.BLACK);
        subScene.setCamera(camera);
    }

    public static final double G = 0.1;

    public static final double BOUNCE = 1;

    private static Line newLine(Color gridColor) {
        Line l = new Line();
        l.setStroke(gridColor);
        l.setStrokeWidth(5d);
        return l;
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
            updateSphere(spheres.get(i), pos(i));
        }
        for (int i = 0; i < COUNT; i++) {
            setAcc(i, (index, _) -> updatePulls(index));
        }
        for (int i = 0; i < COUNT; i++) {
            setVel(i, (index, v) -> v.plus(accelerations.get(index)));
        }
        for (int i = 0; i < COUNT; i++) {
            for (int j = i + 1; j < COUNT; j++) {
                resolveCollision(i, j);
            }
        }
        for (int i = 0; i < COUNT; i++) {
            setPos(i, (index, v) -> v.plus(vel(index)));
        }

        moveCamera();
    }

    private void moveCamera() {
        double angle = 2 * Math.PI * cameraStep / cameraSteps;
        Vector position = new Vector(
            Math.sin(angle) * cameraLine.length(),
            0,
            Math.cos(angle) * cameraLine.length()
        );

        // Move the camera along the circle around origin
        camera.setTranslateX(position.x());
        camera.setTranslateY(position.y());
        camera.setTranslateZ(position.z());

        // Always point the camera towards the origin (0,0,0)
        // Direction vector from camera to origin

        // Compute yaw (rotation around Y axis) and pitch (rotation around X axis)
        double yaw = Math.toDegrees(Math.atan2(-position.x(), -position.z()));
        camera.setRotate(yaw);

        cameraStep = (cameraStep - 1) % cameraSteps;
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

    private void resolveCollision(int i, int j) {
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
            double impulseMagnitude = BOUNCE * (-(1 + Math.E) * vRelN / (1 / iMass + 1 / jMass));

            setVel(i, v -> v.plus(n.mul(impulseMagnitude / iMass)));
            setVel(j, v -> v.minus(n.mul(impulseMagnitude / jMass)));
        }
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
        return G * re.weight() / (distance * distance);
    }

    private static <T> Stream<T> stream(IntFunction<T> f) {
        return IntStream.range(0, COUNT).mapToObj(f);
    }

    private static void updateSphere(Node sphere, Vector pos) {
        sphere.setTranslateX(pos.x());
        sphere.setTranslateY(pos.y());
        sphere.setTranslateZ(pos.z());
    }
}
