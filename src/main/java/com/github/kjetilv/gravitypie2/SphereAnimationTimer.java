package com.github.kjetilv.gravitypie2;

import javafx.animation.AnimationTimer;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

final class SphereAnimationTimer extends AnimationTimer {

    private final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(5);

    private Duration total = Duration.ZERO;

    private long frames;

    private long lastSec = start.getEpochSecond();

    private final Runnable update;

    SphereAnimationTimer(Runnable update) {
        this.update = update;
    }

    @Override
    public void handle(long l) {
        Instant before = Instant.now();
        update.run();
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
            update.run();
        }
    }
}
