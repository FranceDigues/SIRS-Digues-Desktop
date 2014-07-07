/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package fr.sym.map.navigation;


import fr.sym.map.FXAbstractNavigationHandler;
import fr.sym.map.FXMap;
import java.awt.geom.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

/**
 * Zoom in Handler for GoMap2D.
 * 
 * @author Johann Sorel (Geomatys)
 */
public class FXZoomInHandler extends FXAbstractNavigationHandler {

    private static  final Cursor CUR_ZOOM_IN = Cursor.CROSSHAIR;
    private final MouseListen mouseInputListener = new MouseListen();
    private double zoomFactor = 2;

    public FXZoomInHandler(final FXMap map) {
        super(map);
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public void install(final FXMap component) {
        super.install(component);
        component.addEventHandler(MouseEvent.ANY, mouseInputListener);
        component.addEventHandler(ScrollEvent.ANY, mouseInputListener);
        map.setCursor(CUR_ZOOM_IN);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void uninstall(final FXMap component) {
        super.uninstall(component);
        component.removeEventHandler(MouseEvent.ANY, mouseInputListener);
        component.removeEventHandler(ScrollEvent.ANY, mouseInputListener);
        map.setCursor(null);
    }

    private class MouseListen extends AbstractMouseHandler {

        private double startX;
        private double startY;
        private double lastX;
        private double lastY;
        private MouseButton mousebutton;

        @Override
        public void mouseClicked(final MouseEvent e) {
            startX = e.getX();
            startY = e.getY();
            lastX = startX;
            lastY = startY;
            mousebutton = e.getButton();

            // left mouse button
            if (e.getButton() == MouseButton.PRIMARY) {
                scale(new Point2D.Double(startX, startY), zoomFactor);
            }

        }

        @Override
        public void mousePressed(final MouseEvent e) {
            startX = e.getX();
            startY = e.getY();
            lastX = 0;
            lastY = 0;

            mousebutton = e.getButton();
            if (mousebutton == MouseButton.PRIMARY) {

            } else if (mousebutton == MouseButton.SECONDARY) {
                if(!isStateFull()){
                    decorationPane.setBuffer(map.getCanvas().getSnapShot());
                    decorationPane.setCoord(0, 0, map.getWidth(), map.getHeight(), true);
                }
            }

        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            double endX = e.getX();
            double endY = e.getY();

            decorationPane.setBuffer(null);

            if (mousebutton == MouseButton.PRIMARY) {

                if(startX != endX && startY != endY){
                    zoom(startX,startY,endX,endY);
                }

                decorationPane.setBuffer(null);
                decorationPane.setFill(false);
                decorationPane.setCoord(-10, -10,-10, -10, false);

            } //right mouse button : pan action
            else if (mousebutton == MouseButton.SECONDARY) {
                
                if(!isStateFull()){
                    decorationPane.setBuffer(null);
                    decorationPane.setFill(false);
                    decorationPane.setCoord(-10, -10,-10, -10, false);
                    processDrag(startX, startY, endX, endY);
                }
            }

            lastX = 0;
            lastY = 0;
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            decorationPane.setFill(false);
            decorationPane.setCoord(-10, -10,-10, -10, true);
        }

        @Override
        public void mouseMoved(final MouseEvent e){
            startX = e.getX();
            startY = e.getY();
            lastX = startX;
            lastY = startY;
        }
        
        @Override
        public void mouseDragged(final MouseEvent e) {
            double x = e.getX();
            double y = e.getY();

            // left mouse button
            if (mousebutton == MouseButton.PRIMARY) {

                if ((lastX > 0) && (lastY > 0)) {
                    drawRectangle(startX,startY,lastX,lastY,true, true);
                }

                // draw new box
                lastX = x;
                lastY = y;
                drawRectangle(startX,startY,lastX,lastY,true, true);

            } //right mouse button : pan action
            else if (mousebutton == MouseButton.SECONDARY) {
                if ((lastX > 0) && (lastY > 0)) {
                    double dx = lastX - startX;
                    double dy = lastY - startY;
                    
                    if(isStateFull()){
                        processDrag(lastX, lastY, x, y);
                    }else{
                        decorationPane.setFill(true);
                        decorationPane.setCoord(dx, dy, map.getWidth()+dx, map.getHeight()+dy, true);
                    }
                }
                lastX = x;
                lastY = y;
            }

        }

        @Override
        public void mouseWheelMoved(final ScrollEvent e) {
            final double rotate = -e.getDeltaY();

            if(rotate<0){
                scale(new Point2D.Double(startX, startY),zoomFactor);
            }else if(rotate>0){
                scale(new Point2D.Double(startX, startY),1d/zoomFactor);
            }

        }
    }

}
