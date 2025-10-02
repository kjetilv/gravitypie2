module gravitypie {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.desktop;

    opens com.github.kjetilv.gravitypie2 to javafx.graphics;
}