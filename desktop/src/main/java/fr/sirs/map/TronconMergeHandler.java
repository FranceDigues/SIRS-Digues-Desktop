
package fr.sirs.map;

import fr.sirs.CorePlugin;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.model.TronconDigue;
import java.awt.geom.Point2D;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Modality;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.display2d.container.ContextContainer2D;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.filter.identity.DefaultFeatureId;
import org.geotoolkit.gui.javafx.render2d.FXAbstractNavigationHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.gui.javafx.render2d.navigation.AbstractMouseHandler;
import org.geotoolkit.gui.javafx.render2d.navigation.FXPanHandler;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.opengis.filter.identity.Identifier;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class TronconMergeHandler extends FXAbstractNavigationHandler {
    
    private final MouseListen mouseInputListener = new MouseListen();
    private final double zoomFactor = 2;
    
    //edition variables
    private FeatureMapLayer tronconLayer = null;
    private EditionHelper helper;
    
    private final Dialog dialog = new Dialog();
    private final FXTronconMerge editPane;
    private final Session session;
    
    public TronconMergeHandler(final FXMap map) {
        super(map);
        session = Injector.getSession();
        editPane = new FXTronconMerge();
        
        final DialogPane subpane = new DialogPane();
        subpane.setContent(editPane);
        subpane.getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.FINISH);
        dialog.setTitle("Fusionner des tronçons");
        dialog.setResizable(true);
        dialog.initModality(Modality.NONE);
        dialog.initOwner(map.getScene().getWindow());
        dialog.setDialogPane(subpane);
        dialog.setWidth(500);
        dialog.setHeight(700);
        
        dialog.resultProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if(newValue == ButtonType.FINISH){
                    editPane.processMerge();
                }
                dialog.hide();
                if(tronconLayer!=null){
                    tronconLayer.setSelectionFilter(null);
                }
                map.setHandler(new FXPanHandler(map, false));
            }
        });

        editPane.getTroncons().addListener(this::tronconChanged);
        
    }

    private void tronconChanged(ListChangeListener.Change c){
        if(tronconLayer!=null){
            final Set<Identifier> ids = new HashSet<>();
            for(TronconDigue td : editPane.getTroncons()){
                ids.add(new DefaultFeatureId(td.getDocumentId()));
            }
            tronconLayer.setSelectionFilter(GO2Utilities.FILTER_FACTORY.id(ids));
        }
    }
        
    /**
     * {@inheritDoc }
     */
    @Override
    public void install(final FXMap component) {
        super.install(component);
        component.addEventHandler(MouseEvent.ANY, mouseInputListener);
        component.addEventHandler(ScrollEvent.ANY, mouseInputListener);
        map.setCursor(Cursor.CROSSHAIR);
        
        //recuperation du layer de troncon
        tronconLayer = null;
        final ContextContainer2D cc = (ContextContainer2D) map.getCanvas().getContainer();
        final MapContext context = cc.getContext();
        for(MapLayer layer : context.layers()){
            layer.setSelectable(false);
            if(layer.getName().equalsIgnoreCase(CorePlugin.TRONCON_LAYER_NAME)){
                tronconLayer = (FeatureMapLayer) layer;
                try {
                    tronconLayer.setSelectionStyle(CorePlugin.createTronconSelectionStyle(false));
                } catch (URISyntaxException ex) {
                    SIRS.LOGGER.log(Level.FINE, ex.getMessage(), ex);
                }
                layer.setSelectable(true);
            }
        }
        
        helper = new EditionHelper(map, tronconLayer);
        helper.setMousePointerSize(6);
        
        dialog.show();
        
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean uninstall(final FXMap component) {
        if(editPane.getTroncons().isEmpty() || 
                ButtonType.YES.equals(new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la fin du mode édition.", 
                        ButtonType.YES,ButtonType.NO).showAndWait().get())){
            super.uninstall(component);
            component.removeEventHandler(MouseEvent.ANY, mouseInputListener);
            component.removeEventHandler(ScrollEvent.ANY, mouseInputListener);
            return true;
        }
        
        dialog.hide();
        return false;
    }
        
    private class MouseListen extends AbstractMouseHandler {

        private final ContextMenu popup = new ContextMenu();
        private double startX;
        private double startY;
        private MouseButton mousebutton;

        public MouseListen() {
            popup.setAutoHide(true);
        }
        
        private double getMouseX(MouseEvent event){
            final javafx.geometry.Point2D pt = map.localToScreen(0, 0);
            return event.getScreenX()- pt.getX();
        }
        
        private double getMouseY(MouseEvent event){
            final javafx.geometry.Point2D pt = map.localToScreen(0, 0);
            return event.getScreenY() - pt.getY();
        }
        
        @Override
        public void mouseClicked(final MouseEvent e) {            
            if(tronconLayer==null) return;
            
            startX = getMouseX(e);
            startY = getMouseY(e);
            mousebutton = e.getButton();
                
            if(mousebutton == MouseButton.PRIMARY){
                //selection d'un troncon
                final Feature feature = helper.grabFeature(e.getX(), e.getY(), false);
                if(feature !=null){
                    Object bean = feature.getUserData().get(BeanFeature.KEY_BEAN);
                    if(bean instanceof TronconDigue){
                        //on recupere le troncon complet, celui ci n'est qu'une mise a plat
                        bean = session.getTronconDigueRepository().get(((TronconDigue)bean).getDocumentId());
                        if(!editPane.getTroncons().contains(bean)){
                            editPane.getTroncons().add((TronconDigue)bean);
                        }
                    }
                }
            }
            
        }

        @Override
        public void mousePressed(final MouseEvent e) {       
            startX = getMouseX(e);
            startY = getMouseY(e);
            mousebutton = e.getButton();
        }

        @Override
        public void mouseDragged(MouseEvent me) {
            //do not use getX/getY to calculate difference
            //JavaFX Bug : https://javafx-jira.kenai.com/browse/RT-34608
            startX = getMouseX(me);
            startY = getMouseY(me);
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            super.mouseReleased(me);
        }
        
        @Override
        public void mouseExited(final MouseEvent e) {
            decorationPane.setFill(false);
            decorationPane.setCoord(-10, -10,-10, -10, true);
        }

        @Override
        public void mouseMoved(final MouseEvent e){
            startX = getMouseX(e);
            startY = getMouseY(e);
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
