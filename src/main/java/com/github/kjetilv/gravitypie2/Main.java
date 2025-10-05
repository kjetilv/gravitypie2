package com.github.kjetilv.gravitypie2;

import module java.base;
import module javafx.controls;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.awt.*;
import java.util.List;

import static javafx.geometry.Pos.BOTTOM_CENTER;
import static javafx.geometry.Pos.BOTTOM_LEFT;
import static javafx.scene.paint.Color.*;
import static javafx.scene.transform.Rotate.Y_AXIS;

@SuppressWarnings("SameParameterValue")
public class Main extends Application {

    public static final int GRAVITY_WELL_SCALE = 10_000;

    private int cameraStep;

    private final Sphere[] spheres;

    private final Re[] res;

    private final PhongMaterial[] materials;

    private final Vector[] positions;

    private final Vector[] accelerations;

    private final Vector[] velocities;

    private final Vector[] collisionImpulses;

    private final SubScene subScene;

    private final Slidouble gravConstant = new Slidouble("gravConstant");

    private final Slidouble airBrake = new Slidouble("airBrake");

    private final Slidouble collisionBrake = new Slidouble("collisionBrake");

    private final Slidouble wallBrake = new Slidouble("wallBrake");

    private final Slidouble gravityWell = new Slidouble("gravityWell");

    private final AtomicReference<Slidouble> slidableSlidouble = new AtomicReference<>();

    private final List<Slidouble> slidableSlidoubles = List.of(
        gravConstant,
        airBrake,
        collisionBrake,
        wallBrake,
        gravityWell
    );

    private final AtomicBoolean transitioning = new AtomicBoolean();

    private int currentSlidableSlidouble = 0;

    private final Label label = new Label();

    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    private final Slider slider = new Slider(0.000, 1, 0.01);

    private final VBox sliderBox = new VBox(2, label, slider);

    private final SlidoubleListener slidoubleListener = new SlidoubleListener(label, slider, slidableSlidouble);

    private final List<Runnable> presets = List.of(
        () -> transitionTo(
            0d,
            1d,
            0d,
            1d,
            0d
        ),
        () -> transitionTo(
            .04d,
            .07d,
            .23d,
            .32d,
            0d
        ),
        () -> transitionTo(
            .01,
            .1d,
            0d,
            .85d,
            0d
        ),
        () -> transitionTo(
            .04512,
            .03,
            .71,
            0d,
            0d
        ),
        () -> transitionTo(
            0.15662d,
            .03d,
            0.94d,
            0d,
            0d
        ),
        () -> transitionTo(
            0.07,
            0d,
            0.68d,
            0d,
            0d
        ),
        () -> transitionTo(
            .01021d,
            0.11d,
            .33d,
            .06d,
            0d
        )
    );

    private final int worldSizeX;

    private final int worldSizeZ;

    private final int worldSizeY;

    private final Vector cameraLine;

    private final int zBound;

    private final int xBound;

    private final int yBound;

    private int movingLightStep;

    private final double movingLightDistance;

    private final PointLight movingLight1 = new PointLight(WHITE);

    private final PointLight movingLight2 = new PointLight(WHITE);

    private final Rectangle bounds;

    private SphereAnimationTimer sphereAnimationTimer;

    private Stage stage;

    {
        GraphicsDevice device = device();
        bounds = device.getDefaultConfiguration().getBounds();

        worldSizeX = 75 * device.getDisplayMode().getWidth() / 100;
        worldSizeZ = worldSizeX;
        worldSizeY = 75 * device.getDisplayMode().getHeight() / 100;

        zBound = worldSizeZ / 2;
        xBound = worldSizeX / 2;
        yBound = worldSizeY / 2;

        cameraLine = new Vector(0, 0, -2 * worldSizeZ);
    }

    public Main() {
        List<Re.Color> list = Shapes.colors(8)
            .toList();

        res = objects(
            COUNT,
            i -> {
                Re.Color color = Shapes.color(i, COUNT);
                return new Re(
                    10,
                    RE_RANGE.scale(i, COUNT),
                    1L,
                    color,
                    color.brighten(.1d)
                );
            }, Re.class
        );

        positions = objects(
            COUNT,
            _ ->
                new Vector(new Range(-randomRange(), randomRange())), Vector.class
        );

        accelerations = zeroes(COUNT);

        velocities = zeroes(COUNT);

        collisionImpulses = objects(COUNT, _ -> ZERO, Vector.class);

        materials = objects(COUNT, this::material, PhongMaterial.class);

        spheres = objects(COUNT, this::sphere, Sphere.class);

        Sphere origo = new Sphere(4);
        Material blueMaterial = new PhongMaterial(GHOSTWHITE);
        origo.setMaterial(blueMaterial);

        AmbientLight ambient = new AmbientLight(Color.color(.4, .4, .4));

        PointLight pl1 = new PointLight(new Color(.75d, .75d, .75d, 1d));
        pl1.setTranslateX(-(.4 * worldSizeX));
        pl1.setTranslateY(.4 * worldSizeY);
        pl1.setTranslateZ(.5 * worldSizeX);

        PointLight pl2 = new PointLight(new Color(.75d, .75d, .75d, 1d));
        pl2.setTranslateX(.4 * worldSizeX);
        pl2.setTranslateY(-4 * worldSizeY);
        pl2.setTranslateZ(-.5 * worldSizeZ);

        movingLightDistance = worldSizeX + 1.5;

        Group wireBox =
            Shapes.createWireBox(worldSizeX, worldSizeY, worldSizeZ);
        Stream<Node> nodeStream = Stream.concat(
            Stream.of(ambient, pl1, pl2, movingLight1, movingLight2, origo, wireBox),
            Arrays.stream(spheres)
        );
        List<Node> nodes = nodeStream.toList();

        Group world = new Group(nodes);
        subScene = new SubScene(
            world,
            worldSizeX,
            worldSizeY + SLIZER_VERTICALSPACE,
            true,
            SceneAntialiasing.BALANCED
        );
        subScene.setFill(BLACK);
        subScene.setCamera(camera);

        updateSlider();
        preset(1);
    }

    @Override
    public void init() {
        label.setTextFill(BLACK);

        camera.setNearClip(1);
        camera.setFarClip(5 * worldSizeZ);
        camera.setRotationAxis(Y_AXIS);
        camera.setRotate(0);

        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(0.1);
        slider.setMinorTickCount(5);
        slider.setBlockIncrement(0.005);
        slider.setPrefWidth(200);

        sliderBox.setPadding(new javafx.geometry.Insets(2));
        sliderBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0); -fx-background-radius: 5;");
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        slidableSlidouble.set(gravConstant);

        // Update the constant when slider changes
        slider.valueProperty().addListener(slidoubleListener);

        StackPane root = buildRootStackPane(sliderBox);
        Scene scene = setScene(root);
        showStage(this.stage, scene);

        sphereAnimationTimer = new SphereAnimationTimer(this::update);
        sphereAnimationTimer.start();
    }

    private void transitionTo(
        double gravTo,
        double airTo,
        double collisionTo,
        double wallTo,
        double wellTo
    ) {
        if (transitioning.compareAndSet(false, true)) {
            new Transition() {

                private final double gravFrom = gravConstant.value();

                private final double airFrom = airBrake.value();

                private final double collisionFrom = collisionBrake.value();

                private final double wallFrom = wallBrake.value();

                private final double wellFrom = gravityWell.value();

                {
                    setCycleDuration(Duration.millis(250));
                    setInterpolator(Interpolator.EASE_OUT);
                    setOnFinished(_ -> transitioning.set(false));
                }

                @Override
                protected void interpolate(double frac) {
                    gravConstant.value(gravFrom + frac * (gravTo - gravFrom));
                    airBrake.value(airFrom + frac * (airTo - airFrom));
                    collisionBrake.value(collisionFrom + frac * (collisionTo - collisionFrom));
                    wallBrake.value(wallFrom + frac * (wallTo - wallFrom));
                    gravityWell.value(wellFrom + frac * (wellTo - wellFrom));
                    slidoubleListener.refresh();
                }
            }.play();
        }
    }

    private PhongMaterial material(int i) {
        PhongMaterial phong = new PhongMaterial();
        Re.Color color = res[i].color();
        phong.setDiffuseColor(color.rgb());
        phong.setSpecularColor(color.brighten(.1).rgb());
        return phong;
    }

    private Sphere sphere(int i) {
        Sphere sphere = new Sphere(res[i].radius());
        sphere.setMaterial(materials[i]);
        return sphere;
    }

    private StackPane buildRootStackPane(VBox sliderBox) {
        StackPane root = new StackPane(subScene, sliderBox);
        StackPane.setAlignment(sliderBox, BOTTOM_LEFT);
        StackPane.setAlignment(subScene, BOTTOM_CENTER);
        return root;
    }

    private Scene setScene(StackPane root) {
        Scene scene = new Scene(root, worldSizeX, worldSizeY + 120, true);
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (code.isDigitKey()) {
                preset(code.getCode() - '0');
                return;
            }
            switch (code) {
                case UP -> {
                    currentSlidableSlidouble = (currentSlidableSlidouble + 1) % slidableSlidoubles.size();
                    updateSlider();
                }
                case DOWN -> {
                    currentSlidableSlidouble = (currentSlidableSlidouble == 0
                        ? slidableSlidoubles.size()
                        : currentSlidableSlidouble) - 1;
                    updateSlider();
                }
                case S -> System.out.println(slidableSlidoubles.stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining(", ")));
                case Q ->
                    stage.close();
                default -> {
                }
            }
        });
        return scene;
    }

    private void showStage(Stage stage, Scene scene) {
        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.setResizable(true);

        stage.setX(bounds.getX() + (bounds.getWidth() - worldSizeX) / 2);
        stage.setY(bounds.getY() + (bounds.getHeight() - (worldSizeY + SLIZER_VERTICALSPACE)) / 2);

        stage.show();
    }

    private void update() {
        for (int i = 0; i < COUNT; i++) {
            for (int j = i + 1; j < COUNT; j++) {
                handleCollision(i, j);
            }
        }

        for (int i = 0; i < COUNT; i++) {
            velocities[i] = applyCollisionImpulse(i, velocities[i]);
        }

        for (int i = 0; i < COUNT; i++) {
            accelerations[i] = updatePulls(i);
        }

        for (int i = 0; i < COUNT; i++) {
            velocities[i] = updateVelocity(i, velocities[i]);
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

        moveLight();

        moveCamera();
    }

    private void moveLight() {
        double movingLightAngle1 = 2 * Math.PI * movingLightStep / MOVING_LIGHT_STEPS;
        double movingLightAngle2 = movingLightAngle1 + Math.PI;
        movingLight1.setTranslateX(Math.sin(movingLightAngle1) * movingLightDistance);
        movingLight1.setTranslateY(-100);
        movingLight1.setTranslateZ(Math.cos(movingLightAngle1) * movingLightDistance);
        movingLight2.setTranslateX(Math.sin(movingLightAngle2) * movingLightDistance);
        movingLight2.setTranslateY(100);
        movingLight2.setTranslateZ(Math.cos(movingLightAngle2) * movingLightDistance);
        movingLightStep = (movingLightStep + 1) % MOVING_LIGHT_STEPS;
    }

    private void preset(int i) {
        if (i < presets.size()) {
            presets.get(i).run();
            slidoubleListener.refresh();
        }
    }

    private void updateSlider() {
        slidoubleListener.nowDo(slidableSlidoubles.get(currentSlidableSlidouble));
    }

    private Vector updateVelocity(Integer index, Vector v) {
        return v.plus(accelerations[index]).mul(airBrake.mirrorValue());
    }

    private void handleWallBounce(int i) {
        double r = res[i].radius();

        Vector vel = velocities[i];
        Vector pos = positions[i];

        double
            vx = vel.x(),
            vy = vel.y(),
            vz = vel.z();
        double
            px = pos.x(),
            py = pos.y(),
            pz = pos.z();

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

    private void moveSphere(int i) {
        Vector pos = positions[i];
        Sphere sphere = spheres[i];
        sphere.setTranslateX(pos.x());
        sphere.setTranslateY(pos.y());
        sphere.setTranslateZ(pos.z());
    }

    private void setOpacity(int i) {
        double distToOrigo = positions[i].length();
        double dim = 1 - distToOrigo / worldSizeX;
        materials[i].setDiffuseColor(res[i].toRgb(dim, dim));
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

    private Vector applyCollisionImpulse(Integer index, Vector v) {
        Vector collisionImpulse = collisionImpulses[index];
        collisionImpulses[index] = ZERO;
        return v.plus(collisionImpulse);
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
        Vector heightVector = new Vector(0, pos.y() + yBound, 0);
        double height = heightVector.length();
        double groundPull = gravityWell.times(re.weight() / GRAVITY_WELL_SCALE) / height * height;
        return pull.plus(heightVector.mul(groundPull));
    }

    private double pullFrom(Vector sPos, Vector pos, Re re) {
        double distance = pos.distanceTo(sPos);
        return gravConstant.times(re.weight()) / (distance * distance);
    }

    private double randomRange() {
        return worldSizeZ * 0.4;
    }

    static final int COUNT = 300;

    static final Range RE_RANGE = new Range(5, 50);

    static final int CAMERA_STEPS = 21600;

    private static final String TITLE = "Stars";

    private static final Vector ZERO = new Vector();

    private static final int MOVING_LIGHT_STEPS = 3600;

    private static final int SLIZER_VERTICALSPACE = 60;

    private static GraphicsDevice device() {
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        return Arrays.stream(
                devices
            ).max(Comparator.comparing(graphicsDevice ->
                graphicsDevice.getDisplayMode().getWidth()))
            .orElseThrow();
    }

    private static Vector[] zeroes(int count) {
        return objects(count, _ -> ZERO, Vector.class);
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
