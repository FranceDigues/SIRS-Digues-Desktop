

package fr.sym.map;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javax.swing.Timer;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.display.canvas.AbstractCanvas;
import org.geotoolkit.display.canvas.control.NeverFailMonitor;
import org.geotoolkit.display2d.canvas.J2DCanvas;
import org.geotoolkit.display2d.canvas.J2DCanvasVolatile;
import org.geotoolkit.display2d.container.ContextContainer2D;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXMap extends BorderPane {

    /**
     * The name of the {@linkplain PropertyChangeEvent property change event} fired when the
     * {@linkplain getHandler canvas handler} changed.
     */
    public static final String HANDLER_PROPERTY = "handler";
    private static final FXMapDecoration[] EMPTY_OVERLAYER_ARRAY = {};
    private static final Logger LOGGER = Logging.getLogger(FXMap.class);

    private final ObjectProperty<FXCanvasHandler> handlerProp = new SimpleObjectProperty<FXCanvasHandler>();
    private final J2DCanvasVolatile canvas;
    private boolean statefull = false;

    private WritableImage image = null;
    private final ImageView view = new ImageView();
    //used to repaint the buffer at regular interval until it is finished
    private final Timer progressTimer = new Timer(150, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            updateImage();
        }
    });
    
    
    private final List<FXMapDecoration> userDecorations = new ArrayList<>();
    private final StackPane mapDecorationPane = new StackPane();
    private final StackPane userDecorationPane = new StackPane();
    private final StackPane mainDecorationPane = new StackPane();
    private int nextMapDecorationIndex = 1;
    private FXInformationDecoration informationDecoration = new DefaultInformationDecoration();
    private FXMapDecoration backDecoration = new FXColorDecoration();
    
    public FXMap(){
        this(false);
    }

    public FXMap(final boolean statefull){
        mapDecorationPane.getChildren().add(0,view);
        mainDecorationPane.getChildren().add(0,backDecoration.getComponent());
        mainDecorationPane.getChildren().add(1,mapDecorationPane);
        mainDecorationPane.getChildren().add(2,userDecorationPane);
        mainDecorationPane.getChildren().add(3,informationDecoration.getComponent());
        informationDecoration.setMap2D(this);
        setCenter(mainDecorationPane);

        
        canvas = new J2DCanvasVolatile(CommonCRS.WGS84.normalizedGeographic(), new Dimension(100, 100));
        canvas.setMonitor(new NeverFailMonitor());
        canvas.setContainer(new ContextContainer2D(canvas, statefull));
        canvas.setAutoRepaint(true);


        canvas.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {

//                if(canvas.isAutoRepaint()){
//                    //dont show the painting icon if the cans is in auto render mode
//                    // since it may repaint dynamic graphic it would show up all the time
//                    return;
//                }

                if(AbstractCanvas.RENDERSTATE_KEY.equals(evt.getPropertyName())){
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            updateImage();
                            
                            final Object state = evt.getNewValue();
                            if(AbstractCanvas.RENDERING.equals(state)){
                                getInformationDecoration().setPaintingIconVisible(true);
                                progressTimer.start();
                            }else{
                                getInformationDecoration().setPaintingIconVisible(false);
                                progressTimer.stop();
                                updateImage();
                            }
                        }
                    });
                    
                }
            }
        });

    }

    private void updateImage() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                final BufferedImage snapshot = (BufferedImage) canvas.getSnapShot();
                if (snapshot != null) {
                    image = SwingFXUtils.toFXImage(snapshot, image);
                    view.setImage(image);
                } else {
                    view.setImage(null);
                }
            }
        };
        
        if(Platform.isFxApplicationThread()){
            r.run();
        }else{
            Platform.runLater(r);
        }
    }
    
    @Override
    public void resize(double width, double height) {
        super.resize(width, height);
        canvas.resize(new Dimension((int)width, (int)height));
    }

    @Override
    public void resizeRelocate(double x, double y, double width, double height) {
        super.resizeRelocate(x, y, width, height);
        canvas.resize(new Dimension((int)width, (int)height));
    }

    public boolean isStatefull() {
        return statefull;
    }

    /**
     * @return the effective Go2 Canvas.
     */
    public J2DCanvas getCanvas() {
        return canvas;
    }

    public ContextContainer2D getContainer(){
        return (ContextContainer2D) canvas.getContainer();
    }

    /**
     * Must be called when the map2d is not used anymore.
     * to avoid memory leak if it uses thread or other resources
     */
    public void dispose() {
        canvas.dispose();
    }

    public ReadOnlyObjectProperty<FXCanvasHandler> getHandlerProperty(){
        return handlerProp;
    }
    
    public FXCanvasHandler getHandler(){
        return handlerProp.getValue();
    }

    public void setHandler(final FXCanvasHandler handler){

        if(getHandler() != handler) {
            //TODO : check for possible vetos

            final FXCanvasHandler old = getHandler();

            if (old != null){
                old.uninstall(this);
            }

            handlerProp.setValue(handler);

            if (handler != null) {
                handler.install(this);
            }

            //firePropertyChange(HANDLER_PROPERTY, old, handler);
        }

    }

    //----------------------Use as extend for subclasses------------------------
    protected void setRendering(final boolean render) {
        informationDecoration.setPaintingIconVisible(render);
    }

    //----------------------Over/Sub/information layers-------------------------
    /**
     * set the top InformationDecoration of the map2d widget
     * @param info , can't be null
     */
    public void setInformationDecoration(final FXInformationDecoration info) {
        ArgumentChecks.ensureNonNull("info decoration", info);

        mainDecorationPane.getChildren().remove(informationDecoration.getComponent());
        informationDecoration = info;
        mainDecorationPane.getChildren().add(3,informationDecoration.getComponent());
    }

    /**
     * get the top InformationDecoration of the map2d widget
     * @return InformationDecoration
     */
    public FXInformationDecoration getInformationDecoration() {
        return informationDecoration;
    }

    /**
     * set the decoration behind the map
     * @param back : MapDecoration, can't be null
     */
    public void setBackgroundDecoration(final FXMapDecoration back) {
        ArgumentChecks.ensureNonNull("background decoration", back);

        mainDecorationPane.getChildren().remove(backDecoration.getComponent());
        backDecoration = back;
        mainDecorationPane.getChildren().add(0, backDecoration.getComponent());
    }

    /**
     * get the decoration behind the map
     * @return MapDecoration : or null if no back decoration
     */
    public FXMapDecoration getBackgroundDecoration() {
        return backDecoration;
    }

    /**
     * add a Decoration between the map and the information top decoration
     * @param deco : MapDecoration to add
     */
    public void addDecoration(final FXMapDecoration deco) {

        if (deco != null && !userDecorations.contains(deco)) {
            deco.setMap2D(this);
            userDecorations.add(deco);
            userDecorationPane.getChildren().add(userDecorations.indexOf(deco), deco.getComponent());
        }
    }

    /**
     * insert a MapDecoration at a specific index
     * @param index : index where to isert the decoration
     * @param deco : MapDecoration to add
     */
    public void addDecoration(final int index, final FXMapDecoration deco) {

        if (deco != null && !userDecorations.contains(deco)) {
            deco.setMap2D(this);
            userDecorations.add(index, deco);
            userDecorationPane.getChildren().add(userDecorations.indexOf(deco), deco.getComponent());
        }
    }

    /**
     * get the index of a MapDecoration
     * @param deco : MapDecoration to find
     * @return index of the MapDecoration
     * @throw ClassCastException or NullPointerException
     */
    public int getDecorationIndex(final FXMapDecoration deco) {
        return userDecorations.indexOf(deco);
    }

    /**
     * remove a MapDecoration
     * @param deco : MapDecoration to remove
     */
    public void removeDecoration(final FXMapDecoration deco) {
        if (deco != null && userDecorations.contains(deco)) {
            deco.setMap2D(null);
            deco.dispose();
            userDecorations.remove(deco);
            userDecorationPane.getChildren().remove(deco.getComponent());
        }
    }

    /**
     * get an array of all MapDecoration
     * @return array of MapDecoration
     */
    public FXMapDecoration[] getDecorations() {
        return userDecorations.toArray(EMPTY_OVERLAYER_ARRAY);
    }

    /**
     * add a MapDecoration between the map and the user MapDecoration
     * those MapDecoration can not be removed because they are important
     * for edition/selection/navigation.
     * @param deco : MapDecoration to add
     */
    protected void addMapDecoration(final FXMapDecoration deco) {
        mapDecorationPane.getChildren().add(nextMapDecorationIndex, deco.getComponent());
        nextMapDecorationIndex++;
    }

//    //-----------------------------MAP2D----------------------------------------
//
//    /**
//     * get the visual component
//     * @return Component
//     */
//    public Component getComponent() {
//        return geoComponent;
//    }
//
//    /**
//     * Can be used to add more components on the side of the map
//     * if needed. in any case, dont remove the central component.
//     * @return JPanel the container in borderlayout mode
//     */
//    public JPanel getUIContainer() {
//        return this;
//    }
    
    
}
