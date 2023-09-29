/**
 * This file is part of SIRS-Digues 2.
 *
 * Copyright (C) 2016, FRANCE-DIGUES,
 *
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.map;

import com.vividsolutions.jts.geom.Geometry;
import fr.sirs.CorePlugin;
import fr.sirs.FXMainFrame;
import javafx.geometry.Insets;
import org.geotoolkit.display2d.Canvas2DSynchronizer;
import fr.sirs.SIRS;
import fr.sirs.Injector;
import fr.sirs.Plugin;
import fr.sirs.Printable;
import fr.sirs.Session;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.core.model.AvecGeometrie;
import org.geotoolkit.gui.javafx.util.TaskManager;
import fr.sirs.core.model.Element;
import fr.sirs.ui.Growl;
import fr.sirs.util.odt.ODTUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javax.swing.SwingConstants;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.coverage.amended.AmendedCoverageReference;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.display.canvas.control.AbstractCanvasMonitor;
import org.geotoolkit.display2d.GO2Hints;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.display2d.canvas.J2DCanvas;
import org.geotoolkit.display2d.canvas.painter.SolidColorPainter;
import org.geotoolkit.display2d.container.ContextContainer2D;
import org.geotoolkit.display2d.ext.legend.DefaultLegendService;
import org.geotoolkit.display2d.ext.northarrow.GraphicNorthArrowJ2D;
import org.geotoolkit.display2d.ext.scalebar.GraphicScaleBarJ2D;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.DefaultPortrayalService;
import org.geotoolkit.display2d.service.PortrayalExtension;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.display2d.service.ViewDef;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.GeometryDescriptor;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.filter.identity.DefaultFeatureId;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.geometry.jts.JTSEnvelope2D;
import org.geotoolkit.gui.javafx.chooser.FXContextChooser;
import org.geotoolkit.gui.javafx.contexttree.FXMapContextTree;
import org.geotoolkit.gui.javafx.contexttree.MapItemFilterColumn;
import org.geotoolkit.gui.javafx.contexttree.MapItemGlyphColumn;
import org.geotoolkit.gui.javafx.contexttree.MapItemNameColumn;
import org.geotoolkit.gui.javafx.contexttree.MapItemSelectableColumn;
import org.geotoolkit.gui.javafx.contexttree.MapItemVisibleColumn;
import org.geotoolkit.gui.javafx.contexttree.menu.OpacityItem;
import org.geotoolkit.gui.javafx.contexttree.menu.ZoomToItem;
import org.geotoolkit.gui.javafx.render2d.FXContextBar;
import org.geotoolkit.gui.javafx.render2d.FXCoordinateBar;
import org.geotoolkit.gui.javafx.render2d.FXGeoToolBar;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXNavigationBar;
import org.geotoolkit.gui.javafx.render2d.FXSirsScaleBarDecoration;
import org.geotoolkit.gui.javafx.render2d.navigation.FXPanHandler;
import org.geotoolkit.gui.javafx.util.FXUtilities;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.osmtms.OSMTMSCoverageReference;
import org.geotoolkit.owc.xml.OwcXmlIO;
import org.geotoolkit.storage.coverage.CoverageReference;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.draw.FrameStyleHandler;
import org.odftoolkit.simple.style.StyleTypeDefinitions;
import org.opengis.filter.Id;
import org.opengis.geometry.Envelope;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXMapPane extends BorderPane implements Printable {

    public static final Image ICON_SPLIT= SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_COLUMNS,16,FontAwesomeIcons.DEFAULT_COLOR),null);

    private static final Hints MAPHINTS = new Hints();
    static {
        MAPHINTS.put(GO2Hints.KEY_VIEW_TILE, GO2Hints.VIEW_TILE_ON);
        MAPHINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MAPHINTS.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        MAPHINTS.put(GO2Hints.KEY_BEHAVIOR_MODE, GO2Hints.BEHAVIOR_KEEP_TILE);
    }

    private final FXMap uiMap1 = new FXMap(false, MAPHINTS);
    private final FXCoordinateBar uiCoordBar1 = new FXCoordinateBar(uiMap1);
    private final BorderPane paneMap1 = new BorderPane(uiMap1, null, null, uiCoordBar1, null);
    private final FXMap uiMap2 = new FXMap(false, MAPHINTS);
    private final FXCoordinateBar uiCoordBar2 = new FXCoordinateBar(uiMap2);
    private final BorderPane paneMap2 = new BorderPane(uiMap2, null, null, uiCoordBar2, null);
    private final Canvas2DSynchronizer synchronizer = new Canvas2DSynchronizer();

    private final SplitPane mapsplit = new SplitPane();

    private final FXContextBar uiCtxBar;
    private final SIRSAddDataBar uiAddBar;
    private final FXNavigationBar uiNavBar;
    private final FXGeoToolBar uiToolBar;
    private final FXTronconEditBar uiEditBar;
    private final Button splitButton = new Button(null, new ImageView(ICON_SPLIT));
    private final ToolBar uiSplitBar = new ToolBar(splitButton);

    private final FXMapContextTree uiTree;

    //Static growl used to managed AbstractCanvasMonitor exceptions
    private static Growl nullableAlert=null;

    public FXMapPane() {
        setFocusTraversable(true);

        overloadMonitor(uiMap1);

        uiCoordBar2.setCrsButtonVisible(false);
        uiMap1.getCanvas().setBackgroundPainter(new SolidColorPainter(Color.WHITE));
        uiMap2.getCanvas().setBackgroundPainter(new SolidColorPainter(Color.WHITE));
        uiMap1 .addDecoration(new FXSirsScaleBarDecoration());
        uiMap2.addDecoration(new FXSirsScaleBarDecoration());
        uiCoordBar1.setScaleBoxValues(new Long[]{200l,5000l,25000l,50000l});
        uiCoordBar2.setScaleBoxValues(new Long[]{200l,5000l,25000l,50000l});
        synchronizer.addCanvas(uiMap1.getCanvas(),true,true);
        synchronizer.addCanvas(uiMap2.getCanvas(),true,true);

        uiCtxBar = new FXContextBar(uiMap1);
        uiAddBar = new SIRSAddDataBar(uiMap1);
        uiNavBar = new FXNavigationBar(uiMap1);
        uiToolBar = new FXGeoToolBar(uiMap1);
        uiEditBar = new FXTronconEditBar(uiMap1);

        uiTree = new FXMapContextTree();
        uiTree.getTreetable().getColumns().clear();
        uiTree.getTreetable().getColumns().add(new MapItemNameColumn());
        uiTree.getTreetable().getColumns().add(new MapItemGlyphColumn() {
            // Hack : Replace som style panels with overriden ones
            @Override
            protected Pane createEditor(MapLayer candidate) {
                fr.sirs.map.style.FXStyleAggregatedPane pane = new fr.sirs.map.style.FXStyleAggregatedPane();
                pane.init(candidate);
                return pane;
            }
        });
        uiTree.getTreetable().getColumns().add(new MapItemVisibleColumn());

        uiTree.getTreetable().setShowRoot(false);
        uiTree.getMenuItems().add(new OpacityItem());
        uiTree.getMenuItems().add(new SeparatorMenuItem());
        uiTree.getMenuItems().add(new EmptySelectionItem());
        uiTree.getMenuItems().add(new SeparatorMenuItem());
        uiTree.getMenuItems().add(new ZoomToItem(uiMap1));
        uiTree.getMenuItems().add(new StructureFeatureMenuItem());
        uiTree.getMenuItems().add(new StructureCoverageMenuItem());
        uiTree.getMenuItems().add(new ExportMenu());
        uiTree.getMenuItems().add(new DeleteItem());
        uiTree.getTreetable().getColumns().add(2,new MapItemFilterColumn());
        uiTree.getTreetable().getColumns().add(3,new MapItemSelectableColumn());
        uiTree.getTreetable().getColumns().add(4,new MapItemViewRealPositionColumn());
        final Property<MapContext> prop1 = FXUtilities.beanProperty(uiMap1.getContainer(),ContextContainer2D.CONTEXT_PROPERTY, MapContext.class);
        uiTree.mapItemProperty().bind(prop1);
        prop1.addListener(new ChangeListener<MapContext>() {
            @Override
            public void changed(ObservableValue<? extends MapContext> observable, MapContext oldValue, MapContext newValue) {
                uiMap2.getContainer().setContext(newValue);
            }
        });

        splitButton.setOnAction((ActionEvent event) -> {
            if(mapsplit.getItems().contains(paneMap2)){
                mapsplit.getItems().remove(paneMap2);
                splitButton.setTooltip(new Tooltip("Afficher la deuxième carte"));
            } else{
                mapsplit.setDividerPositions(0.5);
                mapsplit.getItems().add(paneMap2);
                splitButton.setTooltip(new Tooltip("Cacher la deuxième carte"));
            }
        });

        uiCtxBar.setMaxHeight(Double.MAX_VALUE);
        uiAddBar.setMaxHeight(Double.MAX_VALUE);
        uiNavBar.setMaxHeight(Double.MAX_VALUE);
        uiToolBar.setMaxHeight(Double.MAX_VALUE);
        uiEditBar.setMaxHeight(Double.MAX_VALUE);
        uiSplitBar.setMaxHeight(Double.MAX_VALUE);

        uiCtxBar.setBackground(Background.EMPTY);
        uiAddBar.setBackground(Background.EMPTY);
        uiNavBar.setBackground(Background.EMPTY);
        uiToolBar.setBackground(Background.EMPTY);
        uiEditBar.setBackground(Background.EMPTY);


        final BorderPane topgrid = new BorderPane();
        topgrid.setPadding(Insets.EMPTY);
        final FlowPane flowPane = new FlowPane(Orientation.HORIZONTAL,uiCtxBar,uiAddBar,uiNavBar,uiToolBar,uiEditBar);
        flowPane.setPadding(Insets.EMPTY);
        flowPane.setHgap(0);
        flowPane.setVgap(0);
        flowPane.setPrefWrapLength(Double.MAX_VALUE);
        flowPane.setMaxWidth(Double.MAX_VALUE);
        flowPane.getStyleClass().add("tool-bar");

        //add plugin toolbars
        final FXMainFrame frame = Injector.getSession().getFrame();
        frame.activePluginProperty().addListener(new ChangeListener<Plugin>() {
            @Override
            public void changed(ObservableValue<? extends Plugin> observable, Plugin oldValue, Plugin newValue) {
                flowPane.getChildren().clear();
                flowPane.getChildren().addAll(uiCtxBar,uiAddBar,uiNavBar,uiToolBar,uiEditBar);
                final List<ToolBar> toolbars = newValue.getMapToolBars(FXMapPane.this);
                if(toolbars!=null){
                    for(ToolBar t : toolbars){
                        t.setMaxHeight(Double.MAX_VALUE);
                        t.setBackground(Background.EMPTY);
                        flowPane.getChildren().add(t);
                    }
                }
            }
        });

        topgrid.setCenter(flowPane);
        topgrid.setRight(uiSplitBar);

        mapsplit.getItems().add(paneMap1);

        final BorderPane border = new BorderPane();
        border.setTop(topgrid);
        border.setCenter(mapsplit);

        final SplitPane split = new SplitPane();
        split.setOrientation(Orientation.HORIZONTAL);
        split.getItems().add(uiTree);
        split.getItems().add(border);
        split.setDividerPositions(0.3);

        setCenter(split);

        uiMap1.setHandler(new FXPanHandler(false));
        uiMap2.setHandler(new FXPanHandler(false));

        //ajout des ecouteurs souris sur click droit
        uiMap1.addEventHandler(MouseEvent.MOUSE_CLICKED, new MapActionHandler(uiMap1));
        uiMap2.addEventHandler(MouseEvent.MOUSE_CLICKED, new MapActionHandler(uiMap2));

        // Force use of amended coverages, to be able to modify their geo-spatial information via SIRS.
        uiMap1.getContainer().addPropertyChangeListener((PropertyChangeEvent evt) -> {
            if (ContextContainer2D.CONTEXT_PROPERTY.equals(evt.getPropertyName())) {
                final Object newContext = evt.getNewValue();
                if (newContext instanceof MapContext) {
                    if (replaceCoverageLayers((MapContext)newContext)) {
                        // HACK : Forced to do it, or else the map still display unmodified layers.
                        uiMap1.getContainer().setContext(null);
                        uiMap1.getContainer().setContext((MapContext)newContext);
                    }

                }
            }
        });

        //Affiche le contexte carto et le déplace à la date du jour
        TaskManager.INSTANCE.submit("Initialisation de la carte", () -> {

            MapContext tmpCtx = null;
            Exception error = null;
            final String previousXmlContext = FXContextChooser.getPreviousFile();
            if (previousXmlContext != null) {
                try {
                    final Path xmlPath = Paths.get(previousXmlContext);
                    if (Files.isRegularFile(xmlPath))
                        tmpCtx = OwcXmlIO.read(xmlPath.toFile());
                } catch (Exception e) {
                    error = e;
                    SIRS.LOGGER.log(Level.WARNING, "Cannot load XML map context from file : ".concat(previousXmlContext), e);
                }
            }

            final MapContext context;
            if (error != null || tmpCtx == null || new GeneralEnvelope(tmpCtx.getBounds()).isEmpty()) {
                context = Injector.getSession().getMapContext();
            } else {
                context = tmpCtx;
            }

            // Do it now to avoid useless map update
            replaceCoverageLayers(context);

            SirsCore.fxRunAndWait(() -> {
                uiMap1.getCanvas().setObjectiveCRS(Injector.getSession().getProjection());
                uiMap1.getCanvas().setVisibleArea(context.getAreaOfInterest());
                setTemporalRange(LocalDate.now(), null);
                // Set context at the end to avoid useless repaint while we parameterize the view.
                uiMap1.getContainer().setContext(context);
                return true;
            });

            if (error != null) {
                throw new SirsCoreRuntimeException("Le contexte cartographique suivant ne peut être ouvert : ".concat(previousXmlContext), error);
            }

            return true;
        });
    }

     /**
     * Static method used to manage the exception produced in geotoolkit.
     *
     * Allows to alert the user of the exception occurences.
     *
     * @param fxMap : FXMap to survey.
     */
    private static void overloadMonitor(FXMap fxMap) {
        ArgumentChecks.ensureNonNull("fxMap, overloadMonitor method's input,", fxMap);
        if (fxMap.getCanvas() != null) {
            fxMap.getCanvas().setMonitor(new AbstractCanvasMonitor() {
                @Override
                public void exceptionOccured(Exception ex, Level level) {
                    Platform.runLater(() -> {
                    if (nullableAlert == null) {
                            nullableAlert = new Growl(Growl.Type.ERROR, "Erreur lors d'une requête CQL, veuillez corriger ou annuler vos modifications.");
                            nullableAlert.show(Duration.seconds(4));

                            // On remet  ensuite l'alert à null pour afficher de nouveau le message d'alerte si aucune correction n'a été apportée.
                            try{
                            Executors.newScheduledThreadPool(1)
                                    .schedule(() -> {nullableAlert = null;}, 4, TimeUnit.SECONDS);
                            }catch (RuntimeException re){
                                nullableAlert=null;
                            }
                        }
                });
                }
            });
        }
    }

    /**
     * Tries to replace all coverage layers to use an {@link AmendedCoverageReference}
     * @param parent The map item containing layers to analyze.
     * @return True if input children have been replaced, false otherwise.
     */
    private boolean replaceCoverageLayers(final MapItem parent) {
        boolean modified = false;
        final List<MapItem> items = parent.items();
        for (int i = 0 ; i < items.size() ; i++) {
            final MapItem item = items.get(i);
            if (item instanceof CoverageMapLayer) {
                final CoverageMapLayer cLayer = (CoverageMapLayer) item;
                final CoverageReference ref = cLayer.getCoverageReference();
                if (ref != null && !(ref instanceof AmendedCoverageReference) && !(ref instanceof OSMTMSCoverageReference)) {
                    final CoverageMapLayer newLayer = MapBuilder.createCoverageLayer(new AmendedCoverageReference(ref, ref.getStore()), cLayer.getStyle());
                    newLayer.setDescription(cLayer.getDescription());
                    newLayer.setElevationModel(cLayer.getElevationModel());
                    newLayer.setName(cLayer.getName());
                    newLayer.setOpacity(cLayer.getOpacity());
                    newLayer.setSelectable(cLayer.isSelectable());
                    newLayer.setSelectionStyle(cLayer.getSelectionStyle());
                    newLayer.setVisible(cLayer.isVisible());
                    newLayer.getUserProperties().putAll(cLayer.getUserProperties());

                    items.set(i, newLayer);
                    modified = true;
                }
            } else if (!item.items().isEmpty()) {
                replaceCoverageLayers(item);
            }
        }

        return modified;
    }

    /**
     * Déplace la temporalité de la carte sélectionnée sur la date demandée.
     * @param ldt La Date pour la carte. Ne doit pas être nulle.
     * @param map La carte à mettre à jour. Nulle pour mettre à jour les deux cartes
     * par défaut.
     */
    public void setTemporalRange(final LocalDate ldt, FXMap map) {
        final Task t = new Task() {
            @Override
            protected Object call() throws Exception {
                if (map == null) {
                    uiCoordBar1.getDateField().valueProperty().setValue(ldt);
                } else {
                    if (uiMap1.equals(map)) {
                        uiCoordBar1.getDateField().valueProperty().setValue(ldt);
                    } else if (uiMap2.equals(map)) {
                        uiCoordBar2.getDateField().valueProperty().setValue(ldt);
                    }
                }
                return null;
            }
        };

        if (Platform.isFxApplicationThread()) {
            t.run();
        } else {
            Platform.runLater(t);
        }
    }

    public FXMap getUiMap() {
        return uiMap1;
    }

    public void focusOnElement(Element target) {
        TaskManager.INSTANCE.submit(new FocusOnMap(target));
    }

    @Override
    public String getPrintTitle() {
        return "Carte";
    }

    @Override
    public boolean print() {
        // Choose output file
        final Path outputFile = SIRS.fxRunAndWait(() -> {
            final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Document OpenOffice", "*.odt");
            final FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(extFilter);
            fileChooser.setSelectedExtensionFilter(extFilter);
            final File result = fileChooser.showSaveDialog(FXMapPane.this.getScene().getWindow());
            if (result == null) {
                return null;
            } else {
                return result.toPath();
            }
        });

        if (outputFile == null)
            return true; // Printing aborted. Return true to avoid another component to print instead of us.

        final Task<Boolean> printTask = new Task() {

            @Override
            protected Object call() throws Exception {
                updateTitle("Impression de la carte");
                final Rectangle2D dispSize = uiMap1.getCanvas().getDisplayBounds();

                final Hints hints = new Hints();
                hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                final PortrayalExtension ext = (J2DCanvas canvas) -> {
                    final GraphicScaleBarJ2D graphicScaleBarJ2D = new GraphicScaleBarJ2D(canvas);
                    graphicScaleBarJ2D.setPosition(SwingConstants.SOUTH_WEST);
                    final GraphicNorthArrowJ2D northArrowJ2D = new GraphicNorthArrowJ2D(canvas, Session.NORTH_ARROW_TEMPLATE);
                    northArrowJ2D.setPosition(SwingConstants.SOUTH_WEST);
                    northArrowJ2D.setOffset(10, 60);

                    try {
                        final double span = canvas.getVisibleEnvelope2D().getSpan(0);
                        if (span > 5000) {
                            graphicScaleBarJ2D.setTemplate(Session.SCALEBAR_KILOMETER_TEMPLATE);
                        } else {
                            graphicScaleBarJ2D.setTemplate(Session.SCALEBAR_METER_TEMPLATE);
                        }
                    } catch (Exception ex) {
                        SIRS.LOGGER.log(Level.INFO, ex.getMessage(), ex);
                    }
                    canvas.getContainer().getRoot().getChildren().add(graphicScaleBarJ2D);
                    canvas.getContainer().getRoot().getChildren().add(northArrowJ2D);
                };

                final CanvasDef cdef = new CanvasDef(new Dimension((int) dispSize.getWidth(), (int) dispSize.getHeight()), new Color(0, 0, 0, 0));
                final SceneDef sdef = new SceneDef(uiMap1.getContainer().getContext(), hints, ext);
                final ViewDef vdef = new ViewDef(uiMap1.getCanvas().getVisibleEnvelope());

                //create ODT
                try (final TextDocument content = TextDocument.newTextDocument()) {
                    content.addParagraph("Carte").applyHeading(true, 1);
                    content.getFooter().appendSection("information").addParagraph("Date de création : " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

                    // Legend
                    content.addParagraph("Légende").applyHeading(true, 2);
                    ODTUtils.appendImage(content, DefaultLegendService.portray(Injector.getSession().getLegendTemplate(), uiMap1.getContainer().getContext(), null))
                            .setHorizontalPosition(StyleTypeDefinitions.FrameHorizontalPosition.LEFT);

                    // Map
                    org.odftoolkit.simple.draw.Image map = ODTUtils.appendImage(content, DefaultPortrayalService.portray(cdef, sdef, vdef), true);
                    final FrameStyleHandler styleHandler = map.getStyleHandler();
                    styleHandler.setAchorType(StyleTypeDefinitions.AnchorType.TO_PAGE);
                    styleHandler.setHorizontalPosition(StyleTypeDefinitions.FrameHorizontalPosition.CENTER);
                    styleHandler.setVerticalPosition(StyleTypeDefinitions.FrameVerticalPosition.MIDDLE);

                    try (OutputStream out = Files.newOutputStream(outputFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                        content.save(out);
                    }
                }

                return true;
            }
        };

        // Events to launch when task finishes
        SIRS.fxRun(false, new TaskManager.MockTask(() -> {
            printTask.setOnFailed(event -> Platform.runLater(() -> GeotkFX.newExceptionDialog("Impossible d'imprimer la carte", printTask.getException()).show()));
            printTask.setOnCancelled(event -> Platform.runLater(() -> new Growl(Growl.Type.WARNING, "L'impression a été annulée").showAndFade()));
            printTask.setOnSucceeded(event -> Platform.runLater(() -> {
                new Growl(Growl.Type.INFO, "L'impression de la carte est terminée").showAndFade();
                SIRS.openFile(outputFile).setOnSucceeded(evt -> {
                    if (!Boolean.TRUE.equals(evt.getSource().getValue())) {
                        Platform.runLater(() -> {
                            new Growl(Growl.Type.WARNING, "Impossible de trouver un programme pour ouvrir la carte").showAndFade();
                        });
                    }
                });
            }));
        }));

        // Print map.
        TaskManager.INSTANCE.submit(printTask);
        return true;
    }

    @Override
    public ObjectProperty getPrintableElements() {
        return new SimpleObjectProperty();
    }

    /**
     * A task which select and zoom on given element on the map.
     * Task returns false if the element cannot be focused on.
     */
    private class FocusOnMap extends Task<Boolean> {

        final Element toFocusOn;

        public FocusOnMap(final Element toFocusOn) {
            ArgumentChecks.ensureNonNull("Element to focus on", toFocusOn);
            this.toFocusOn = toFocusOn;

            updateTitle("Recherche un élément sur la carte");
        }

        @Override
        protected Boolean call() throws Exception {
            final int maxProgress = 3;
            int currentProgress = 0;

            updateProgress(currentProgress++, maxProgress);
            updateMessage("Recherche de la couche correspondante");

            final MapLayer container = CorePlugin.getMapLayerForElement(toFocusOn);
            if (container == null) {
                final Growl growlInfo = new Growl(Growl.Type.INFO, "Aucune couche de données n'a encore été créée pour cet élément.");
                Platform.runLater(growlInfo::showAndFade);
                return false;
            }
            if (!(container instanceof FeatureMapLayer)) {
                if (toFocusOn instanceof AvecGeometrie) {
                    Geometry geom = ((AvecGeometrie) toFocusOn).getGeometry();
                    if (geom != null) {
                        final JTSEnvelope2D env = JTS.toEnvelope(geom);
                        final Envelope selectionEnvelope = SIRS.pseudoBuffer(env);
                        final TaskManager.MockTask displayUpdate = new TaskManager.MockTask(() -> {
                            uiMap1.getCanvas().setVisibleArea(selectionEnvelope);
                            return null;
                        });
                        Platform.runLater(displayUpdate);
                        displayUpdate.get();
                        return true;
                    }
                } else {
                    final Growl growlInfo = new Growl(Growl.Type.WARNING, "L'objet n'a pas de géométrie.");
                    Platform.runLater(growlInfo::showAndFade);
                    return false;
                }
            }

            final FeatureMapLayer fLayer = (FeatureMapLayer) container;

            updateProgress(currentProgress++, maxProgress);
            updateMessage("Filtrage sur l'élément");

            final Id idFilter = GO2Utilities.FILTER_FACTORY.id(
                    Collections.singleton(new DefaultFeatureId(toFocusOn.getId())));
            fLayer.setSelectionFilter(idFilter);
            fLayer.setVisible(true);

            updateProgress(currentProgress++, maxProgress);
            updateMessage("Calcul de la zone à afficher");


            // Envelope spatiale
            final FeatureType fType = fLayer.getCollection().getFeatureType();
            final GenericName typeName = fType.getName();
            QueryBuilder queryBuilder = new QueryBuilder(
                    NamesExt.create(typeName.scope().toString(), typeName.head().toString()));
            queryBuilder.setFilter(idFilter);
            GeometryDescriptor geomDescriptor = fType.getGeometryDescriptor();
            if (geomDescriptor != null) {
                queryBuilder.setProperties(new GenericName[]{geomDescriptor.getName()});
            } else {
                return false; // no zoom possible
            }
            FeatureCollection subCollection =
                    fLayer.getCollection().subCollection(queryBuilder.buildQuery());

            Envelope tmpEnvelope = subCollection.getEnvelope();
            if (tmpEnvelope == null) {
                return false;
            }
            final Envelope selectionEnvelope = SIRS.pseudoBuffer(tmpEnvelope);

            // Envelope temporelle
            final LocalDateTime selectionTime;
            if (toFocusOn instanceof AvecBornesTemporelles) {

                final AvecBornesTemporelles abtToFocusOn = (AvecBornesTemporelles) toFocusOn;
                long minTime = Long.MIN_VALUE;
                long maxTime = Long.MAX_VALUE;

                LocalDateTime tmpTime = abtToFocusOn.getDate_debut()==null ? null : abtToFocusOn.getDate_debut().atTime(LocalTime.MIDNIGHT);
                if (tmpTime != null) {
                    minTime = Timestamp.valueOf(tmpTime).getTime();
                }

                tmpTime = abtToFocusOn.getDate_fin()==null ? null : abtToFocusOn.getDate_fin().atTime(LocalTime.MIDNIGHT);
                if (tmpTime != null) {
                    maxTime = Timestamp.valueOf(tmpTime).getTime();
                }

                final NumberRange<Long> elementRange = NumberRange.create(minTime, true, maxTime, true);

                minTime = Long.MIN_VALUE;
                maxTime = Long.MAX_VALUE;
                Date[] temporalRange = uiMap1.getCanvas().getTemporalRange();
                if (temporalRange != null && temporalRange.length > 0) {
                    minTime = temporalRange[0].getTime();
                    if (temporalRange.length > 1) {
                        maxTime = temporalRange[1].getTime();
                    }
                }

                final NumberRange<Long> mapRange = NumberRange.create(minTime, true, maxTime, true);

                // If map temporal envelope does not intersect our element, we must
                // change it.
                if (!mapRange.intersects(elementRange))
                    selectionTime = new Timestamp(elementRange.getMinValue()).toLocalDateTime();
                else
                    selectionTime = null;
            } else {
                selectionTime = null;
            }

            updateProgress(currentProgress++, maxProgress);
            updateMessage("Mise à jour de l'affichage");

            final TaskManager.MockTask displayUpdate = new TaskManager.MockTask(() -> {
                    uiMap1.getCanvas().setVisibleArea(selectionEnvelope);
                    if (selectionTime != null) {
                        setTemporalRange(selectionTime.toLocalDate(), uiMap1);
                    }
                    return null;
            });

            Platform.runLater(displayUpdate);
            displayUpdate.get();

            return true;
        }
    }

}
