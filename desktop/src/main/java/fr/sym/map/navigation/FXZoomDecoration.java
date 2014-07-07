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

import fr.sym.map.FXMap;
import fr.sym.map.FXMapDecoration;
import java.awt.Image;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Zoom pan decoration
 *
 * @author Johann Sorel
 * @module pending
 */
public class FXZoomDecoration extends FlowPane implements FXMapDecoration{

    private final Color TRS = new Color(1, 1, 1, 0.1);
    
    private final Rectangle rectangle = new Rectangle();
    
    private Image buffer = null;
    private FXMap map = null;
    private boolean draw = false;
    private boolean fill = false;

    public FXZoomDecoration(){
        final AnchorPane ap = new AnchorPane(rectangle);
        getChildren().add(ap);
        rectangle.setVisible(false);
        rectangle.setFill(TRS);
        rectangle.setStroke(Color.DARKGREY);
        rectangle.setStrokeWidth(2);
        ap.setPrefSize(10, 10);
        ap.setMinSize(10, 10);
        ap.setMaxSize(10, 10);
    }

    public void setFill(final boolean fill){
        this.fill = fill;
        rectangle.setFill(fill?TRS:null);
    }

    public void setCoord(double sx, double sy, double ex, double ey, final boolean draw){
        this.draw = draw;  
        
        if(ex<sx){
            double d = ex;
            ex = sx;
            sx = d;
        }
        if(ey<sy){
            double d = ey;
            ey = sy;
            sy = d;
        }
        
        rectangle.setX(sx);
        rectangle.setY(sy);
        rectangle.setWidth(ex-sx);
        rectangle.setHeight(ey-sy);
        rectangle.setVisible(draw);
    }

    @Override
    public void refresh() {
    }

    @Override
    public Node getComponent() {
        return this;
    }

    @Override
    public void setMap2D(final FXMap map) {
        this.map = map;
    }

    @Override
    public FXMap getMap2D() {
        return map;
    }

    @Override
    public void dispose() {
        map = null;
        setBuffer(null);
    }

    /**
     * @return the buffer
     */
    public Image getBuffer() {
        return buffer;
    }

    /**
     * @param buffer the buffer to set
     */
    public void setBuffer(final Image buffer) {
        this.buffer = buffer;
    }
}
