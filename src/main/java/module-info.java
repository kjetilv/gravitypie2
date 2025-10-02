module gravitypie {
    requires javafx.controls;
    requires javafx.graphics;

    opens com.github.kjetilv.gravitypie2 to javafx.graphics;
}