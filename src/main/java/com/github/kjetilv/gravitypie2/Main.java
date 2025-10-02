package com.github.kjetilv.gravitypie2;

import module java.base;
import module javafx.controls;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.awt.*;
import java.util.List;

import static javafx.geometry.Pos.BOTTOM_CENTER;
import static javafx.geometry.Pos.BOTTOM_LEFT;
import static javafx.scene.paint.Color.*;

@SuppressWarnings("SameParameterValue")
public class Main extends Application {

    private int cameraStep;

    private final Sphere[] spheres;

    private final Re[] res;

    private final PhongMaterial[] materials;

    private final Vector[] positions;

    private final Vector[] accelerations;

    private final Vector[] velocities;

    private final Vector[] collisionImpulses;

    private final SubScene subScene;

    private final String title;

    private final Slidouble gravConstant = new Slidouble("gravConstant", .1);

    private final Slidouble airBrake = new Slidouble("airBrake", .25);

    private final Slidouble collisionBrake = new Slidouble("collisionBrake", .35d);

    private final Slidouble wallBrake = new Slidouble("wallBrake", .5d);

    private final AtomicReference<Slidouble> slidableSlidouble = new AtomicReference<>();

    private final List<Slidouble> slidableSlidoubles = List.of(
        gravConstant,
        airBrake,
        collisionBrake,
        wallBrake
    );

    private final List<Slidouble> zeroables = List.of(
        airBrake,
        collisionBrake,
        wallBrake
    );

    private int currentSlidableSlidouble = 0;

    private final Label label = new Label();

    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    private final Slider slider = new Slider(0.000, 1, 0.01);

    private final VBox sliderBox = new VBox(2, label, slider);

    private final SlidoubleListener slidoubleListener = new SlidoubleListener(label, slider, slidableSlidouble);

    private final int worldSizeX;

    private final int worldSizeZ;

    private final int worldSizeY;

    {
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        GraphicsDevice device = Arrays.stream(
                devices
            ).max(Comparator.comparing(graphicsDevice ->
                graphicsDevice.getDisplayMode().getWidth()))
            .orElseThrow();

        worldSizeX = 8 * device.getDisplayMode().getWidth() / 10;
        worldSizeZ = worldSizeX;
        worldSizeY = 8 * device.getDisplayMode().getHeight() / 10;
    }

    private final int zBound = worldSizeZ / 2;

    private final Vector cameraLine = new Vector(0, 0, -2 * worldSizeZ);

    private final int xBound = worldSizeX / 2;

    private final int yBound = worldSizeY / 2;

    {
        label.setTextFill(BLACK);

        camera.setNearClip(1);
        camera.setFarClip(5 * worldSizeZ);
        camera.setRotationAxis(Rotate.Y_AXIS);
        camera.setRotate(0);

        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(0.1);
        slider.setMinorTickCount(5);
        slider.setBlockIncrement(0.01);
        slider.setPrefWidth(200);

        sliderBox.setPadding(new javafx.geometry.Insets(2));
        sliderBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0); -fx-background-radius: 5;");
    }

    public Main() {
        this.title = "Stars";

        res = objects(
            COUNT,
            i ->
                new Re(
                    10,
                    RE_RANGE.point(i, COUNT),
                    1L,
                    color(i, COUNT)
                ), Re.class
        );

        positions = objects(
            COUNT,
            _ ->
                new Vector(new Range(-randomRange(), randomRange())), Vector.class
        );

        accelerations = zeroes(COUNT);

        velocities = zeroes(COUNT);

        collisionImpulses = objects(COUNT, _ -> ZERO, Vector.class);

        materials = objects(
            COUNT,
            i -> {
                PhongMaterial phong = new PhongMaterial();
                phong.diffuseColorProperty().set(res[i].rgb(1, 1));
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
        pl1.setTranslateX(-(.4 * worldSizeX));
        pl1.setTranslateY(.4 * worldSizeY);
        pl1.setTranslateZ(.5 * worldSizeX);

        PointLight pl2 = new PointLight(WHITE);
        pl2.setTranslateX(.4 * worldSizeX);
        pl2.setTranslateY(-4 * worldSizeY);
        pl2.setTranslateZ(-.5 * worldSizeZ);

        List<Node> wire = wires(xBound, yBound, zBound);

        Stream<Node> nodeStream = Stream.of(
            Stream.of(ambient, pl1, pl2, origo),
            Arrays.stream(spheres),
            wire.stream()
        ).flatMap(Function.identity());
        List<Node> nodes = nodeStream.toList();

        Group world = new Group(nodes);
        subScene = new SubScene(
            world,
            worldSizeX,
            worldSizeY,
            true,
            SceneAntialiasing.BALANCED
        );
        subScene.setFill(BLACK);
        subScene.setCamera(camera);

        refreshSlider();
    }

    @Override
    public void start(Stage stage) {
        // Create the gravitational constant slider
        ;

        slidableSlidouble.set(gravConstant);

        // Update the constant when slider changes
        slider.valueProperty().addListener(slidoubleListener);

        StackPane root = buildRootStackPane(sliderBox);
        Scene scene = setScene(root);
        showStage(stage, scene);

        new SphereAnimationTimer(this::update).start();
    }

    private StackPane buildRootStackPane(VBox sliderBox) {
        StackPane root = new StackPane(subScene, sliderBox);
        StackPane.setAlignment(sliderBox, BOTTOM_LEFT);
        StackPane.setAlignment(subScene, BOTTOM_CENTER);
        return root;
    }

    private Scene setScene(StackPane root) {
        Scene scene = new Scene(root, worldSizeX, worldSizeY + 60, true);
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case UP -> currentSlidableSlidouble = (currentSlidableSlidouble + 1) % slidableSlidoubles.size();
                case DOWN -> {
                    int newIndex = (currentSlidableSlidouble - 1) % slidableSlidoubles.size();
                    currentSlidableSlidouble = newIndex < 0 ? slidableSlidoubles.size() - 1 : newIndex;
                }
                case Z -> {
                    for (Slidouble zeroable : zeroables) {
                        zeroable.max();
                    }
                }
                case S -> {
                    String summary = slidableSlidoubles.stream()
                        .map(Objects::toString)
                        .collect(Collectors.joining(", "));
                    System.out.println(summary);
                }
                case Q -> System.exit(0);
                default -> {
                }
            }
            refreshSlider();
        });
        return scene;
    }

    private void showStage(Stage stage, Scene scene) {
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    private void refreshSlider() {
        slidoubleListener.nowDo(slidableSlidoubles.get(currentSlidableSlidouble));
    }

    private void update() {
        for (int i = 0; i < COUNT; i++) {
            accelerations[i] = updatePulls(i);
        }
        for (int i = 0; i < COUNT; i++) {
            velocities[i] = updateVelocity(i, velocities[i]);
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

        for (int i = 0; i < COUNT; i++) {
            for (int j = i + 1; j < COUNT; j++) {
                handleCollision(i, j);
            }
        }

        for (int i = 0; i < COUNT; i++) {
            velocities[i] = updateCollisionImpulse(i, velocities[i]);
        }

        moveCamera();
    }

    private void setOpacity(int i) {
        double distToOrigo = positions[i].length();
        double opa = 1 - distToOrigo / worldSizeX;
        materials[i].setDiffuseColor(res[i].rgb(opa, opa));
    }

    private Vector updateVelocity(Integer index, Vector v) {
        return v.plus(accelerations[index]).mul(airBrake.mirrorValue());
    }

    private Vector updateCollisionImpulse(Integer index, Vector v) {
        Vector collisionImpulse = collisionImpulses[index];
        collisionImpulses[index] = ZERO;
        return v.plus(collisionImpulse);
    }

    private void moveSphere(int i) {
        Vector p = positions[i];
        Sphere s = spheres[i];
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

        if (px < -xBound + r) {
            vx = Math.abs(vx);
            px = -xBound + r + 1;
            h = true;
        } else if (px > xBound - r) {
            vx = -Math.abs(vx);
            px = xBound - r - 1;
            h = true;
        }

        if (py < -yBound + r) {
            vy = Math.abs(vy);
            py = -yBound + r + 1;
            h = true;
        } else if (py > yBound - r) {
            vy = -Math.abs(vy);
            py = yBound - r - 1;
            h = true;
        }

        if (pz < -zBound + r) {
            vz = Math.abs(vz);
            pz = -zBound + r + 1;
            h = true;
        } else if (pz > zBound - r) {
            vz = -Math.abs(vz);
            pz = zBound - r - 1;
            h = true;
        }

        if (h) {
            double brake = this.wallBrake.mirrorValue();
            velocities[i] = new Vector(vx * brake, vy * brake, vz * brake);
            positions[i] = new Vector(px, py, pz);
        }
    }

    private void moveCamera() {
        double angle = 2 * Math.PI * cameraStep / CAMERA_STEPS;
        double x = Math.sin(angle) * cameraLine.length();
        double z = Math.cos(angle) * cameraLine.length();

        // Move the camera along the circle around origin
        camera.setTranslateX(x);
        camera.setTranslateY(0);
        camera.setTranslateZ(z);

        // Compute yaw (rotation around Y axis) and pitch (rotation around X axis)
        camera.setRotate(Math.toDegrees(Math.atan2(-x, -z)));

        cameraStep = (cameraStep - 1) % CAMERA_STEPS;
    }

    private Vector updatePulls(int i) {
        Vector pos = positions[i];
        Re re = res[i];
        Vector pull = ZERO;
        for (int j = 0; j < i; j++) {
            double force = pullFrom(positions[j], pos, re);
            pull = pull.plus(positions[j].minus(pos).mul(force));
        }
        for (int j = i + 1; j < COUNT; j++) {
            double force = pullFrom(positions[j], pos, re);
            pull = pull.plus(positions[j].minus(pos).mul(force));
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
            double impulse = collisionBrake.mirrorTimes(rawImpulse);

            collisionImpulses[i] = collisionImpulses[i].plus(n.mul(impulse / jMass));
            collisionImpulses[j] = collisionImpulses[j].minus(n.mul(impulse / iMass));
        }
    }

    private double pullFrom(Vector sPos, Vector pos, Re re) {
        double distance = pos.distanceTo(sPos);
        return gravConstant.times(re.weight()) / (distance * distance);
    }

    private double randomRange() {
        return worldSizeZ * 0.48;
    }

    static final int COUNT = 256;

    static final Range RE_RANGE = new Range(5, 50);

    static final int CAMERA_STEPS = 21600;

    static final double COLOUR_RANGE = 0.85;

    static Vector ZERO = new Vector();

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
}
