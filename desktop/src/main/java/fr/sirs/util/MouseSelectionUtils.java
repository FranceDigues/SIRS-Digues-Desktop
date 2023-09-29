package fr.sirs.util;

import javafx.event.EventHandler;
import javafx.scene.effect.Light;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Provide Mouse pressed ; dragged and release MouseEventHandler to the set-up of selection rectangle.
 * Used to reduce code duplication.
 */
public final class MouseSelectionUtils {

    public static EventHandler<? super MouseEvent> mousePressed(final Rectangle selection, final Light.Point anchor, final Pane root) {
        return event -> {
            anchor.setX(event.getX());
            anchor.setY(event.getY());
            selection.setX(event.getX());
            selection.setY(event.getY());
            root.getChildren().add(selection);
        };
    }

    public static EventHandler<? super MouseEvent> mouseDragged(final Rectangle selection, final Light.Point anchor) {
        return event -> {
            selection.setWidth(Math.abs(event.getX() - anchor.getX()));
            selection.setHeight(Math.abs(event.getY() - anchor.getY()));
            selection.setX(Math.min(anchor.getX(), event.getX()));
            selection.setY(Math.min(anchor.getY(), event.getY()));
        };
    }

    public static EventHandler<? super MouseEvent> mouseRelease(final Rectangle selection, final Pane root) {
        return event -> root.getChildren().remove(selection);
    }

    public static Rectangle defaultRectangle() {
        final Rectangle selection = new Rectangle();
        selection.setFill(null); // transparent
        selection.setStroke(Color.BLUE); // border
        selection.getStrokeDashArray().add(10.0);
        return selection;
    }
}
