    private final List<Runnable> presets = List.of(
        () -> transitionTo(0d, 1d, 0d, 1d, 0d),
        () -> transitionTo(.04d, .07d, .23d, .32d, 0d),
        () -> transitionTo(.01, .1d, 0d, .85d, 0d),
        () -> transitionTo(.04512, .03, .71, 0d, 0d),
        () -> transitionTo(0.15662d, .03d, 0.94d, 0d, 0d),
        () -> transitionTo(0.07, 0d, 0.68d, 0d, 0d),
        () -> transitionTo(.01021d, 0.11d, .33d, .06d, 0d)
    );

    private void transitionTo(double gravTo, double airTo, double collisionTo, double wallTo, double wellTo) {
        Transition transition = new Transition() {
            private final double gravFrom = gravConstant.value();
            private final double airFrom = airBrake.value();
            private final double collisionFrom = collisionBrake.value();
            private final double wallFrom = wallBrake.value();
            private final double wellFrom = gravityWell.value();

            {
                setCycleDuration(Duration.seconds(1.5));
            }

            @Override
            protected void interpolate(double frac) {
                gravConstant.value(gravFrom + frac * (gravTo - gravFrom));
                airBrake.value(airFrom + frac * (airTo - airFrom));
                collisionBrake.value(collisionFrom + frac * (collisionTo - collisionFrom));
                wallBrake.value(wallFrom + frac * (wallTo - wallFrom));
                gravityWell.value(wellFrom + frac * (wellTo - wellFrom));
            }
        };
        transition.play();
        slidoubleListener.refresh();
    }
