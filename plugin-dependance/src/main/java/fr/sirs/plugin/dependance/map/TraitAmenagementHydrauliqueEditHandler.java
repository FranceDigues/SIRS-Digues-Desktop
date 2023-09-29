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
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.component.TraitAmenagementHydrauliqueRepository;
import fr.sirs.core.model.AmenagementHydraulique;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.TraitAmenagementHydraulique;
import fr.sirs.theme.ui.FXTraitAmenagementHydraulique;
import fr.sirs.plugin.dependance.PluginDependance;
import java.awt.geom.Rectangle2D;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.gui.javafx.render2d.navigation.FXPanHandler;
import org.geotoolkit.gui.javafx.render2d.shape.FXGeometryLayer;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.apache.sis.referencing.CRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Copy of the class TraitBergeEditHandler.
 *
 * @author Maxime Gavens (Geomatys)
 */
public class TraitAmenagementHydrauliqueEditHandler extends AbstractNavigationHandler {

    private static final int CROSS_SIZE = 5;

    private final MainMouseListen mouseInputListener = new MainMouseListen();
    private final FXGeometryLayer decoration= new FXGeometryLayer(){
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
    private FeatureMapLayer traitLayer = null;

    public TraitAmenagementHydrauliqueEditHandler(final FXMap map) {

        pane.importProperty().set(false);
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
        map.addDecoration(0,decoration);

        //on rend les couches aménagement hydraulique selectionnables
        final MapContext context = map.getContainer().getContext();
        for(MapLayer layer : context.layers()){
            layer.setSelectable(false);
            if(layer.getName().equalsIgnoreCase(PluginDependance.LAYER_AH_NAME)){
                amenagementHydrauliqueLayer = (FeatureMapLayer) layer;
                amenagementHydrauliqueLayer.setSelectable(true);
            }else if(layer.getName().equalsIgnoreCase(PluginDependance.LAYER_TRAIT_NAME)){
                traitLayer = (FeatureMapLayer) layer;
                traitLayer.setSelectable(true);
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
        component.removeDecoration(decoration);
        dialog.hide();
        return true;
    }

    private class MainMouseListen extends FXPanMouseListen {

        private final ContextMenu popup = new ContextMenu();
        private FXPanMouseListen subHandle = null;

        public MainMouseListen() {
            super(TraitAmenagementHydrauliqueEditHandler.this);
            popup.setAutoHide(true);
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            if(subHandle!=null){
                subHandle.handle(e);
                return;
            }

            final MouseButton button = e.getButton();
            if(button==MouseButton.PRIMARY){
                if(pane.amenagementHydrauliqueProperty().get()==null){
                    //on recupere un aménagement hydraulique
                    final GraphicVisitor visitor = new PickVisitor();
                    final Rectangle2D rect = new Rectangle2D.Double(getMouseX(e)-3, getMouseY(e)-3, 6, 6);
                    map.getCanvas().getGraphicsIn(rect, visitor, VisitFilter.INTERSECTS);
                }else{
                    //mode edition de geometry
                    if(pane.newProperty().get()){
                        //creation d'une geometry
                        final TraitAmenagementHydraulique trait = traitRepo.create();
                        trait.setAmenagementHydrauliqueId(pane.amenagementHydrauliqueProperty().get().getDocumentId());
                        trait.setDate_debut(LocalDate.now());
                        pane.traitProperty().set(trait);

                        final CreateMouseListen handler = new CreateMouseListen();
                        handler.selection.geometry.bindBidirectional(pane.traitProperty().get().geometryProperty());
                        subHandle = handler;
                        handler.refreshDecoration();

                    }else {
                        if(pane.traitProperty().get()==null){
                            //on recupere un trait existant
                            final GraphicVisitor visitor = new PickVisitor();
                            final Rectangle2D rect = new Rectangle2D.Double(getMouseX(e)-3, getMouseY(e)-3, 6, 6);
                            map.getCanvas().getGraphicsIn(rect, visitor, VisitFilter.INTERSECTS);
                        }
                    }
                }
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if(subHandle!=null) subHandle.mouseClicked(e);
            else super.mouseClicked(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if(subHandle!=null) subHandle.mouseDragged(e);
            else super.mouseDragged(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if(subHandle!=null) subHandle.mouseEntered(e);
            else super.mouseEntered(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if(subHandle!=null) subHandle.mouseExited(e);
            else super.mouseExited(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if(subHandle!=null) subHandle.mouseMoved(e);
            else super.mouseMoved(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if(subHandle!=null) subHandle.mouseReleased(e);
            else super.mouseReleased(e);
        }

        @Override
        public void mouseWheelMoved(ScrollEvent e) {
            if(subHandle!=null) subHandle.mouseWheelMoved(e);
            else super.mouseWheelMoved(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if(subHandle!=null) subHandle.keyPressed(e);
            else super.keyPressed(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if(subHandle!=null) subHandle.keyReleased(e);
            else super.keyReleased(e);
        }

        @Override
        public void keyTyped(KeyEvent e) {
            if(subHandle!=null) subHandle.keyTyped(e);
            else super.keyTyped(e);
        }

    }

    private final class PickVisitor extends AbstractGraphicVisitor{

        @Override
        public void visit(ProjectedFeature feature, RenderingContext2D context, SearchAreaJ2D area) {
            final Feature f = feature.getCandidate();
            final Object bean = feature.getCandidate().getUserData().get(BeanFeature.KEY_BEAN);
            if (!(bean instanceof Element) || !Injector.getSession().editionAuthorized((Element)bean))
                return;

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
                    //edition d'une geometry existante
                    final EditMouseListen handler = new EditMouseListen();
                    handler.selection.geometry.bindBidirectional(pane.traitProperty().get().geometryProperty());
                    mouseInputListener.subHandle = handler;
                    handler.refreshDecoration();
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

    private class EditMouseListen extends FXPanMouseListen {

        private final EditionHelper helper;
        private final EditionHelper.EditionGeometry selection = new EditionHelper.EditionGeometry();
        private boolean modified = false;
        private MouseButton pressed = null;

        public EditMouseListen() {
            super(TraitAmenagementHydrauliqueEditHandler.this);
            this.helper = new EditionHelper(map, traitLayer);
        }

        private void refreshDecoration(){
            decoration.getGeometries().setAll(selection.geometry.get());
            decoration.setNodeSelection(selection);
        }

        @Override
        public void mouseClicked(final MouseEvent e) {

            final MouseButton button = e.getButton();

            if(button == MouseButton.PRIMARY){
                if(selection.geometry.get() == null){
                    //nothing
                }else if(e.getClickCount() >= 2){
                    //double click = add a node
                    final Geometry result;
                    if(selection.geometry.get() instanceof LineString){
                        result = helper.insertNode((LineString)selection.geometry.get(), e.getX(), e.getY());
                    }else if(selection.geometry.get() instanceof Polygon){
                        result = helper.insertNode((Polygon)selection.geometry.get(), e.getX(), e.getY());
                    }else if(selection.geometry.get() instanceof GeometryCollection){
                        result = helper.insertNode((GeometryCollection)selection.geometry.get(), e.getX(), e.getY());
                    }else{
                        result = selection.geometry.get();
                    }
                    modified = modified || result != selection.geometry.get();
                    selection.geometry.set( result );
                    decoration.getGeometries().setAll(selection.geometry.get());
                }else if(e.getClickCount() == 1){
                    //single click with a geometry = select a node
                    helper.grabGeometryNode(e.getX(), e.getY(), selection);
                    decoration.setNodeSelection(selection);
                }
            }else if(button == MouseButton.SECONDARY){
                //nothing
            }

        }

        @Override
        public void mousePressed(final MouseEvent e) {
            pressed = e.getButton();

            if(pressed == MouseButton.PRIMARY){
                if(selection.geometry.get() == null){
                    //nothing
                }else if(e.getClickCount() == 1){
                    //single click with a geometry = select a node
                    helper.grabGeometryNode(e.getX(), e.getY(), selection);
                    decoration.setNodeSelection(selection);
                }
            }

            super.mousePressed(e);
        }

        @Override
        public void mouseDragged(final MouseEvent e) {

            if(pressed == MouseButton.PRIMARY && selection != null){
                //dragging node
                selection.moveSelectedNode(helper.toCoord(e.getX(), e.getY()));
                refreshDecoration();
                modified = true;
                return;
            }

            super.mouseDragged(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if(KeyCode.DELETE == e.getCode() && selection != null){
                //delete node
                selection.deleteSelectedNode();
                refreshDecoration();
                modified = true;
            }
        }
    }

    private class CreateMouseListen extends FXPanMouseListen {

        private final EditionHelper helper;
        private final EditionHelper.EditionGeometry selection = new EditionHelper.EditionGeometry();
        private final List<Coordinate> coords = new ArrayList<>();
        private boolean end = false;

        public CreateMouseListen() {
            super(TraitAmenagementHydrauliqueEditHandler.this);
            this.helper = new EditionHelper(map, traitLayer);
        }

        private void refreshDecoration(){
            decoration.getGeometries().setAll(selection.geometry.get());
            decoration.setNodeSelection(selection);
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            if(end) return;

            final double x = getMouseX(e);
            final double y = getMouseY(e);
            mousebutton = e.getButton();

            if(mousebutton == MouseButton.PRIMARY){
                coords.add(helper.toCoord(x,y));
                if(coords.size() == 1){
                    //this is the first point of the geometry we create
                    //add another point that will be used when moving the mouse around
                    coords.add(helper.toCoord(x,y));
                }
                final LineString geometry = EditionHelper.createLine(coords);
                JTS.setCRS(geometry, map.getCanvas().getObjectiveCRS2D());
                selection.geometry.set(geometry);
                refreshDecoration();

                if(e.getClickCount()>=2){
                    end = true;
                }

            }else if(mousebutton == MouseButton.SECONDARY){
                //nothing
            }
        }

        @Override
        public void mouseMoved(MouseEvent me) {
            if(end) return;

            if(coords.size() > 1){
                final double x = getMouseX(me);
                final double y = getMouseY(me);
                coords.remove(coords.size()-1);
                coords.add(helper.toCoord(x,y));
                final LineString geometry = EditionHelper.createLine(coords);
                JTS.setCRS(geometry, map.getCanvas().getObjectiveCRS2D());
                selection.geometry.set(geometry);
                refreshDecoration();
                return;
            }

            super.mouseMoved(me);
        }
    }
}
