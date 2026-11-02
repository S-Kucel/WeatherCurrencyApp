package pl.weathercurrency.ui;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Scale;

/** Small vector illustration; it needs no font, image download or external asset. */
public final class WeatherIcon extends Pane {
    public WeatherIcon(int code, boolean day, double size) {
        Group illustration = new Group();
        Color sunColor = day ? Color.web("#ffcf70") : Color.web("#dbe9ff");
        Circle sun = new Circle(70, 28, 17, sunColor);
        if (code <= 2) {
            illustration.getChildren().add(sun);
            if (day) for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                Line ray = new Line(70 + 24 * Math.cos(radians), 28 + 24 * Math.sin(radians),
                        70 + 29 * Math.cos(radians), 28 + 29 * Math.sin(radians));
                ray.setStroke(sunColor); ray.setStrokeWidth(3); ray.setStrokeLineCap(StrokeLineCap.ROUND);
                illustration.getChildren().add(ray);
            }
        }
        if (code != 0) {
            SVGPath cloud = new SVGPath();
            cloud.setContent("M25 67 C6 67 5 42 23 39 C23 16 57 12 64 35 C87 27 104 42 97 58 C94 65 87 67 79 67 Z");
            cloud.setFill(Color.web("#dceeee"));
            illustration.getChildren().add(cloud);
        }
        if (code >= 51 && code <= 82 && code != 71 && code != 73 && code != 75 && code != 77) {
            for (int x : new int[]{30, 52, 74}) {
                Line drop = new Line(x, 76, x - 4, 86); drop.setStroke(Color.web("#70bfd3"));
                drop.setStrokeWidth(4); drop.setStrokeLineCap(StrokeLineCap.ROUND); illustration.getChildren().add(drop);
            }
        }
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) {
            for (int x : new int[]{30, 52, 74}) illustration.getChildren().add(new Circle(x, 81, 3, Color.web("#bddeed")));
        }
        if (code >= 95) {
            SVGPath bolt = new SVGPath(); bolt.setContent("M56 60 L43 79 L53 79 L45 96 L69 72 L57 72 L65 60 Z");
            bolt.setFill(sunColor); illustration.getChildren().add(bolt);
        }
        if (code == 45 || code == 48) for (int y : new int[]{75, 84}) {
            Line fog = new Line(20, y, 88, y); fog.setStroke(Color.web("#bfd2d4")); fog.setStrokeWidth(4); illustration.getChildren().add(fog);
        }
        illustration.getTransforms().add(new Scale(size / 110, size / 110));
        getChildren().add(illustration);
        setMinSize(size, size * .9); setPrefSize(size, size * .9); setMaxSize(size, size * .9);
        setAccessibleText(pl.weathercurrency.model.WeatherCondition.description(code));
    }
}
