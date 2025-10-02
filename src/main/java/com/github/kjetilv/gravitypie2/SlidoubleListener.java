package com.github.kjetilv.gravitypie2;

import module java.base;
import module javafx.controls;
import javafx.scene.control.Label;

final class SlidoubleListener implements ChangeListener<Number> {

    private final Label label;

    private final Slider slider;

    private final AtomicReference<Slidouble> ref;

    SlidoubleListener(Label label, Slider slider, AtomicReference<Slidouble> ref) {
        this.label = label;
        this.slider = slider;
        this.ref = ref;
        relabel();
    }

    public void nowDo(Slidouble slidouble) {
        ref.set(slidouble);
        this.slider.setValue(slidouble.value());
        relabel();
    }

    @Override
    public void changed(ObservableValue<? extends Number> observableValue, Number oldVal, Number val) {
        ref.get().value(val.doubleValue());
        relabel();
    }

    private void relabel() {
        Slidouble slidouble = ref.get();
        if (slidouble != null) {
            label.setText(slidouble.labelString());
        }
    }
}
