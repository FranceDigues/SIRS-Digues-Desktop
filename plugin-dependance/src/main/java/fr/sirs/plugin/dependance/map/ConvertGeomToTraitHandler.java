/*
 * This file is part of SIRS-Digues 2.
 *
 *  Copyright (C) 2021, FRANCE-DIGUES,
 *
 *  SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any
 *  later version.
 *
 *  SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.plugin.dependance.map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.component.TraitAmenagementHydrauliqueRepository;
import fr.sirs.core.model.AmenagementHydraulique;
import fr.sirs.core.model.TraitAmenagementHydraulique;
import fr.sirs.plugin.dependance.PluginDependance;
import fr.sirs.theme.ui.FXTraitAmenagementHydraulique;
import java.awt.geom.Rectangle2D;
import java.time.LocalDate;
import java.util.logging.Level;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.display.VisitFilter;
import org.geotoolkit.display2d.GraphicVisitor;
import org.geotoolkit.display2d.canvas.AbstractGraphicVisitor;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.ProjectedFeature;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.render2d.AbstractNavigationHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.navigation.FXPanHandler;
import org.geotoolkit.gui.javafx.render2d.shape.FXGeometryLayer;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.apache.sis.referencing.CRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Maxime Gavens (Geomatys)
 */
public class ConvertGeomToTraitHandler extends AbstractNavigationHandler {

    private static final int CROSS_SIZE = 5;

    private final MouseListen mouseInputListener = new MouseListen();
    private final FXGeometryLayer geomlayer= new FXGeometryLayer(){
        @Override
        protected Node createVerticeNode(Coordinate c, boolean selected){
            final Line h = new Line(c.x-CROSS_SIZE, c.y, c.x+CROSS_SIZE, c.y);
            final Line v = new Line(c.x, c.y-CROSS_SIZE, c.x, c.y+CROSS_SIZE);
            h.setStroke(Color.RED);
            v.setStroke(Color.RED);
            return new Group(h,v);
        }
    };

    private final TraitAmenagementHydrauliqueRepository traitRepo = (TraitAmenagementHydrauliqueRepository)Injector.getSession().getRepositoryForClass(TraitAmenagementHydraulique.class);
    private final FXTraitAmenagementHydraulique pane = new FXTraitAmenagementHydraulique();
    private final Stage dialog = new Stage();

    private FeatureMapLayer amenagementHydrauliqueLayer = null;

    public ConvertGeomToTraitHandler(final FXMap map) {
        super();

        pane.importProperty().set(true);
        dialog.setScene(new Scene(pane));
        dialog.setAlwaysOnTop(true);
        dialog.initModality(Modality.NONE);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setResizable(true);
        dialog.setWidth(360);
        dialog.setHeight(300);
        dialog.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                map.setHandler(new FXPanHandler(true));
            }
        });

    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void install(final FXMap map) {
        super.install(map);
        map.addEventHandler(MouseEvent.ANY, mouseInputListener);
        map.addEventHandler(ScrollEvent.ANY, mouseInputListener);
        map.setCursor(Cursor.CROSSHAIR);
        map.addDecoration(0,geomlayer);

        //on rend les couches aménagement hydraulique selectionnables
        final MapContext context = map.getContainer().getContext();
        for(MapLayer layer : context.layers()){
            layer.setSelectable(false);
            if(layer.getName().equalsIgnoreCase(PluginDependance.LAYER_AH_NAME)){
                amenagementHydrauliqueLayer = (FeatureMapLayer) layer;
                amenagementHydrauliqueLayer.setSelectable(true);
            }
        }

        pane.mapProperty().set(map);
        dialog.show();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean uninstall(final FXMap component) {
        super.uninstall(component);
        component.removeEventHandler(MouseEvent.ANY, mouseInputListener);
        component.removeEventHandler(ScrollEvent.ANY, mouseInputListener);
        component.removeDecoration(geomlayer);
        dialog.hide();
        return true;
    }

    private class MouseListen extends FXPanMouseListen {

        private final ContextMenu popup = new ContextMenu();

        public MouseListen() {
            super(ConvertGeomToTraitHandler.this);
            popup.setAutoHide(true);
        }

        @Override
        public void mousePressed(final MouseEvent e) {

            final MouseButton button = e.getButton();
            if(button==MouseButton.PRIMARY){
                final GraphicVisitor visitor = new PickVisitor();
                final Rectangle2D rect = new Rectangle2D.Double(getMouseX(e)-3, getMouseY(e)-3, 6, 6);
                map.getCanvas().getGraphicsIn(rect, visitor, VisitFilter.INTERSECTS);
            }

        }
    }

    private final class PickVisitor extends AbstractGraphicVisitor{

        @Override
        public void visit(ProjectedFeature feature, RenderingContext2D context, SearchAreaJ2D area) {
            final Feature f = feature.getCandidate();
            final Object bean = feature.getCandidate().getUserData().get(BeanFeature.KEY_BEAN);

            if(pane.amenagementHydrauliqueProperty().get()==null){
                //on selectionne uniquement un object de type aménagement hydraulique.
                if(bean instanceof AmenagementHydraulique){
                    pane.amenagementHydrauliqueProperty().set((AmenagementHydraulique)bean);
                }
            }else if(pane.importProperty().get()){
                //on selectionne n'importe quelle geometry pour en faire un trait d'aménagement hydraulique.
                Geometry geom = (Geometry) f.getDefaultGeometryProperty().getValue();
                if (geom != null) {
                    geom = LinearReferencingUtilities.asLineString(geom);
                }

                if(geom !=null) {
                    try{
                        final Session session = Injector.getSession();
                        //convertion from data crs to base crs
                        geom = JTS.transform(geom, CRS.findOperation(
                                f.getDefaultGeometryProperty().getType().getCoordinateReferenceSystem(),
                                session.getProjection(), null).getMathTransform());
                        JTS.setCRS(geom, session.getProjection());


                        final TraitAmenagementHydraulique trait = traitRepo.create();
                        trait.setAmenagementHydrauliqueId(pane.amenagementHydrauliqueProperty().get().getDocumentId());
                        trait.setDate_debut(LocalDate.now());
                        trait.setGeometry(geom);
                        pane.traitProperty().set(trait);

                        //save trait d'aménagement hydraulique
                        traitRepo.add(trait);

                        map.getCanvas().repaint();
                    }catch(TransformException | FactoryException ex){
                        SIRS.LOGGER.log(Level.WARNING, ex.getMessage(),ex);
                    }
                }
            }else if(pane.traitProperty().get()==null){
                //on selectionne uniquement un object de type trait d'aménagement hydraulique.
                if(bean instanceof TraitAmenagementHydraulique){
                    pane.traitProperty().set((TraitAmenagementHydraulique)bean);
                }
            }
        }

        @Override
        public boolean isStopRequested() {
            return pane.traitProperty().get()!=null;
        }

        @Override
        public void visit(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D area) {}
    }

}
