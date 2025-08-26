package com.github.kjetilv.gravitypie2;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main extends Application {

    public static final int COUNT = 5;

    public static void main(String[] args) {
        launch(args);
    }

    private final List<Sphere> spheres;

    private final List<Re> res;

    private final List<Vector> positions;

    private final List<Vector> accelerations;

    private final List<Vector> velocities;

    private final List<Vector> blackHolePositions;

    private final List<Re> blackHoles;

    private final SubScene subScene;

    private final String title;

    public Main() {
        this.title = "Stars";

        res = objects(i ->
            new Re(10, 2 * (5 + i), 10)
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

        blackHolePositions = List.of(new Vector(0, 0, 0));
        blackHoles = List.of(new Re(100, 25, 25));

        AmbientLight ambient = new AmbientLight(Color.color(0.3, 0.3, 0.5));

        PointLight pl1 = new PointLight(Color.WHITE);
        pl1.setTranslateX(-400);
        pl1.setTranslateY(400);
        pl1.setTranslateZ(100);

        PointLight pl2 = new PointLight(Color.WHITE);
        pl2.setTranslateX(400);
        pl2.setTranslateY(-4000);
        pl2.setTranslateZ(-100);


        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.01);
        camera.setFarClip(5000);
        camera.setTranslateZ(-1000); // Move the camera back so it can see the origin
        camera.setRotationAxis(Rotate.X_AXIS);
        camera.setRotate(0);

        List<Node> nodes = Stream.concat(
                Stream.of(ambient, pl1, pl2),
                spheres.stream()
            )
            .toList();

        Group world = new Group(nodes);
        subScene = new SubScene(
            world,
            1024,
            1024,
            true,
            SceneAntialiasing.BALANCED
        );
        subScene.setFill(Color.BLACK);
        subScene.setCamera(camera);
    }

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane(subScene);
        Scene scene = new Scene(root, 1024, 768, true);
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
            set(spheres.get(i), pos(i));
        }
        for (int i = 0; i < COUNT; i++) {
            accelerations.set(i, updatePulls(i));
        }
        for (int i = 0; i < COUNT; i++) {
            velocities.set(i, velocities.get(i).plus(accelerations.get(i)));
        }
        for (int i = 0; i < COUNT; i++) {
            for (int j = i + 1; j < COUNT; j++) {
                resolveCollision(i, j);
            }
        }
        for (int i = 0; i < COUNT; i++) {
            positions.set(i, positions.get(i).plus(velocities.get(i)));
        }
    }

    private Vector updatePulls(int i) {
        Vector pos = pos(i);
        Re re = res(i);
        Stream<Vector> otherSpherePulls = positions.stream()
            .filter(p -> p != pos)
            .map(other ->
                other.minus(pos).mul(pullFrom(other, pos, re)));
        Stream<Vector> blackHolePulls = IntStream.range(0, 1)
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
            double impulseMagnitude = 0.01 * (-(1 + Math.E) * vRelN / (1 / iMass + 1 / jMass));

            setVel(i, v -> v.plus(n.mul(impulseMagnitude / iMass)));
            setVel(j, v -> v.minus(n.mul(impulseMagnitude / jMass)));
        }
    }

    private void setVel(int i, UnaryOperator<Vector> vectorUnaryOperator) {
        set(velocities, i, vectorUnaryOperator);
    }

    private void setPos(int i, UnaryOperator<Vector> vectorUnaryOperator) {
        set(positions, i, vectorUnaryOperator);
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
        return 0.01 * re.weight() / (distance * distance);
    }

    private static <T> Stream<T> stream(IntFunction<T> f) {
        return IntStream.range(0, COUNT).mapToObj(f);
    }

    private static void set(Node sphere, Vector position) {
        sphere.setTranslateX(position.x());
        sphere.setTranslateY(position.y());
        sphere.setTranslateZ(position.z());
    }
}
