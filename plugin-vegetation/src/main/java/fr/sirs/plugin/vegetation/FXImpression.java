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
package fr.sirs.plugin.vegetation;

import fr.sirs.CorePlugin;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.authentication.SIRSAuthenticator;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.ParcelleVegetation;
import fr.sirs.core.model.PlanVegetation;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.Preview;
import fr.sirs.plugin.vegetation.map.PlanifState;
import static fr.sirs.plugin.vegetation.map.PlanifState.NON_PLANIFIE;
import fr.sirs.util.SirsStringConverter;
import fr.sirs.util.odt.ODTUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import javax.swing.SwingConstants;
import org.apache.sis.measure.Units;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.canvas.J2DCanvas;
import org.geotoolkit.display2d.ext.DefaultBackgroundTemplate;
import org.geotoolkit.display2d.ext.legend.DefaultLegendTemplate;
import org.geotoolkit.display2d.ext.legend.LegendTemplate;
import org.geotoolkit.display2d.ext.scalebar.DefaultScaleBarTemplate;
import org.geotoolkit.display2d.ext.scalebar.GraphicScaleBarJ2D;
import org.geotoolkit.display2d.ext.scalebar.ScaleBarTemplate;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.DefaultPortrayalService;
import org.geotoolkit.display2d.service.PortrayalExtension;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.display2d.service.ViewDef;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.osmtms.OSMTileMapClient;
import org.geotoolkit.security.ApiSecurity;
import org.geotoolkit.storage.coverage.CoverageReference;
import org.geotoolkit.storage.coverage.CoverageStore;
import org.geotoolkit.style.DefaultDescription;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.style.Font;
import org.odftoolkit.simple.style.StyleTypeDefinitions;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Table;
import org.odftoolkit.simple.text.Paragraph;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXImpression extends GridPane{

    private static final LegendTemplate LEGEND_TEMPLATE = new DefaultLegendTemplate(
                new DefaultBackgroundTemplate( //legend background
                    new BasicStroke(0), //stroke
                    Color.WHITE, //stroke paint
                    new Color(1f, 1f, 1f, 0.5f), // fill paint
                    new Insets(10, 10, 10, 10), //border margins
                    0//round border
                    ),
                2, //gap between legend elements
                null, //glyph size, we can let it to null for the legend to use the best size
                new java.awt.Font("Serial",java.awt.Font.PLAIN,10), //Font used for style rules
                false, // show layer names
                new java.awt.Font("Serial",java.awt.Font.BOLD,12) //Font used for layer names
                );

    private static final ScaleBarTemplate SCALEBAR_TEMPLATE = new DefaultScaleBarTemplate(
                            new DefaultBackgroundTemplate( //legend background
                                new BasicStroke(0), //stroke
                                Color.WHITE, //stroke paint
                                new Color(1f, 1f, 1f, 0.5f), // fill paint
                                new Insets(10, 10, 10, 10), //border margins
                                0//round border
                                ),
                            new Dimension(300,30),6,
                            false, 4, NumberFormat.getNumberInstance(),
                            Color.BLACK, Color.BLACK, Color.WHITE,
                            3,true,false, new java.awt.Font("Serial", java.awt.Font.PLAIN, 8),true,Units.METRE);


    @FXML private GridPane uiGrid;
    @FXML private ListView<Preview> uiTroncons;
    @FXML private ComboBox<Integer> uiDateStart;
    @FXML private ComboBox<Integer> uiDateEnd;
    @FXML private CheckBox uiAllTroncon;
    @FXML private Button uiPrint;
    @FXML private ProgressIndicator uiProgress;
    @FXML private Label uiProgressLabel;

    @FXML private CheckBox uiTraiteeNonPlanif;
    @FXML private CheckBox uiTraiteePlanif;
    @FXML private CheckBox uiNonTraiteeNonPlanif;
    @FXML private CheckBox uiNonTraiteePlanif;

    private final Spinner<Double> uiPRStart = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(-Double.MIN_VALUE, Double.MAX_VALUE, 0));
    private final Spinner<Double> uiPREnd = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(-Double.MIN_VALUE, Double.MAX_VALUE, 0));

    private final Session session = Injector.getSession();
    private final VegetationSession vegSession = VegetationSession.INSTANCE;
    private final ObjectProperty<PlanVegetation> planProperty = new SimpleObjectProperty<>();
    private final BooleanProperty running = new SimpleBooleanProperty(false);

    public FXImpression() {
        SIRS.loadFXML(this, Positionable.class);

        uiGrid.add(uiPRStart, 1, 3);
        uiGrid.add(uiPREnd, 3, 3);

        final StringConverter strCvt = new SirsStringConverter();
        uiTroncons.disableProperty().bind(uiAllTroncon.selectedProperty());
        uiTroncons.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        uiTroncons.setCellFactory((ListView<Preview> param) -> new TextFieldListCell<>(strCvt));
        uiPrint.disableProperty().bind(planProperty.isNull().or(running));
        uiProgress.visibleProperty().bind(running);
        uiProgressLabel.visibleProperty().bind(running);

        //on modifie les date de fin en fonction de la date de debut.
        uiDateStart.valueProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
                final ObservableList<Integer> items = FXCollections.observableArrayList();
                final Integer selectedVal = uiDateEnd.getValue();
                final PlanVegetation plan = planProperty.get();
                if(plan!=null){
                    for(int i=uiDateStart.getValue()+1;i<plan.getAnneeFin();i++){
                        items.add(i);
                    }
                }
                uiDateEnd.setItems(items);
                if(selectedVal!=null && items.contains(selectedVal)){
                    uiDateEnd.getSelectionModel().select(selectedVal);
                }else{
                    uiDateEnd.getSelectionModel().selectLast();
                }
            }
        });


        //on liste les troncons du plan actif.
        planProperty.addListener((ObservableValue<? extends PlanVegetation> observable, PlanVegetation oldValue, PlanVegetation newValue) -> update());
        planProperty.bind(vegSession.planProperty());

    }

    private void update(){
        final PlanVegetation plan = planProperty.get();
        if(plan!=null){
            //dates possible
            int debut = plan.getAnneeDebut();
            final ObservableList<Integer> items = FXCollections.observableArrayList();
            for(int i=debut;i<plan.getAnneeFin();i++){
                items.add(i);
            }
            uiDateStart.setItems(items);
            uiDateStart.getSelectionModel().selectFirst();

            //troncons possible
            final Previews previews = session.getPreviews();
            final List<ParcelleVegetation> parcelles = vegSession.getParcelleRepo().getByPlan(plan);
            final Map<String,Preview> troncons = new HashMap<>();
            for(ParcelleVegetation parcelle : parcelles){
                final String tronconId = parcelle.getLinearId();
                if(!troncons.containsKey(tronconId)){
                    final Preview preview = previews.get(tronconId);
                    troncons.put(tronconId, preview);
                }
            }
            final ObservableList<Preview> lst = FXCollections.observableArrayList(troncons.values());
            uiTroncons.setItems(lst);
        }else{
            uiDateStart.setItems(FXCollections.emptyObservableList());
            uiDateStart.getSelectionModel().selectFirst();
            uiTroncons.setItems(FXCollections.emptyObservableList());
        }
    }

    @FXML
    void print(ActionEvent event) {
        final PlanVegetation plan = planProperty.get();

        //liste de toutes les parcelles
        final List<ParcelleVegetation> parcelles = vegSession.getParcelleRepo().getByPlan(plan);


        final FileChooser chooser = new FileChooser();
        final File file = chooser.showSaveDialog(null);
        if(file==null) return;

        uiProgressLabel.setText("");
        running.set(true);

        new Thread(){
            @Override
            public void run() {
                try{
                    final TextDocument doc = TextDocument.newTextDocument();

                    //on recupere les dates
                    final List<Integer> years = new ArrayList<>();
                    for(int i=uiDateStart.getValue();i<=uiDateEnd.getValue();i++){
                        years.add(i);
                    }

                    //on recupere les parcelles a utiliser
                    Platform.runLater(()->uiProgressLabel.setText("Récupération des parcelles"));
                    final List<ParcelleVegetation> parcelles;
                    if(uiAllTroncon.isSelected()){
                        parcelles = vegSession.getParcelleRepo().getByPlan(plan);
                    }else{
                        parcelles = new ArrayList<>();
                        for(Preview p : uiTroncons.getSelectionModel().getSelectedItems()){
                            parcelles.addAll(vegSession.getParcelleRepo().getByLinearId(p.getElementId()));
                        }
                    }

                    //on enleve les parcelles qui n'intersect pas la zone des PR
                    final double prStart = uiPRStart.getValue();
                    final double prEnd = uiPREnd.getValue();
                    if(prStart!=0.0 && prEnd!=0.0){
                        for(int i=parcelles.size()-1;i>=0;i--){
                            final ParcelleVegetation parcelle = parcelles.get(i);
                            if(parcelle.getPrDebut()>prEnd || parcelle.getPrFin()<prStart){
                                parcelles.remove(i);
                            }
                        }
                    }

                    final StringConverter strCvt = new SirsStringConverter();

                    //generation des cartes et table pour chaque année
                    boolean first = true;
                    for(int year : years){
                        Platform.runLater(()->uiProgressLabel.setText("Génération pour l'année "+year));

                        if(first){
                            first = false;
                        }else{
                            final Paragraph breakPara = doc.addParagraph("");
                            doc.addPageBreak(breakPara);
                        }

                        final Paragraph paragraph = doc.addParagraph(""+year);
                        paragraph.setFont(new Font("Serial", StyleTypeDefinitions.FontStyle.BOLD, 18));

                        //generation de la carte
                        final MapContext context = MapBuilder.createContext();
                        final MapContext legendContext = MapBuilder.createContext();
                        context.layers().add(createOSMLayer());

                        for(MapLayer layer : session.getMapContext().layers()){
                            if(layer.getName().equalsIgnoreCase(CorePlugin.TRONCON_LAYER_NAME)){
                                FeatureMapLayer fml = (FeatureMapLayer) layer;
                                fml = MapBuilder.createFeatureLayer(fml.getCollection(), fml.getStyle());
                                context.layers().add(fml);
                            }
//                            else if(layer.getName().equalsIgnoreCase(CorePlugin.BORNE_LAYER_NAME)){
//                                FeatureMapLayer fml = (FeatureMapLayer) layer;
//                                fml = MapBuilder.createFeatureLayer(fml.getCollection(), fml.getStyle());
//                                context.layers().add(fml);
//                            }
                        }
                        final MapLayer parcelleLayer = VegetationSession.parcellePanifState(plan,year,parcelles);
                        context.layers().add(parcelleLayer);
                        legendContext.layers().add(parcelleLayer);

                        final PortrayalExtension ext = new PortrayalExtension() {

                            @Override
                            public void completeCanvas(J2DCanvas jdc) throws PortrayalException {
                                final LegendGraphic legend = new LegendGraphic(jdc, LEGEND_TEMPLATE,legendContext);
                                legend.setPosition(SwingConstants.SOUTH_EAST);
                                jdc.getContainer().getRoot().getChildren().add(legend);

                                final GraphicScaleBarJ2D bar = new GraphicScaleBarJ2D(jdc);
                                bar.setTemplate(SCALEBAR_TEMPLATE);
                                bar.setPosition(SwingConstants.SOUTH_WEST);
                                jdc.getContainer().getRoot().getChildren().add(bar);
                            }
                        };

                        final Hints hints = new Hints();
                        hints.add(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
                        hints.add(new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC));

                        final CanvasDef cdef = new CanvasDef(new Dimension(1000, 600), Color.WHITE, false);
                        final SceneDef sdef = new SceneDef(context,hints,ext);
                        final ViewDef vdef = new ViewDef(parcelleLayer.getBounds());
                        final BufferedImage mapImage = DefaultPortrayalService.portray(cdef, sdef, vdef);

                        ODTUtils.appendImage(doc, mapImage);

                        //on construit les listes
                        final List<ParcelleVegetation> planifieTraitee = uiTraiteePlanif.isSelected() ? new ArrayList<>() : null ;
                        final List<ParcelleVegetation> planifieNonTraitee = uiNonTraiteePlanif.isSelected() ? new ArrayList<>() : null ;
                        final List<ParcelleVegetation> NonPlanifieTraitee = uiTraiteeNonPlanif.isSelected() ? new ArrayList<>() : null ;
                        final List<ParcelleVegetation> NonPlanifieNonTraitee = uiNonTraiteeNonPlanif.isSelected() ? new ArrayList<>() : null ;

                        for(ParcelleVegetation parcelle : parcelles){
                            final boolean traitee = VegetationSession.isParcelleTraitee(parcelle, year);
                            final PlanifState planif = VegetationSession.getParcellePlanifState(plan, parcelle, year);

                            if(traitee){
                                if(planif==NON_PLANIFIE){
                                    if(NonPlanifieTraitee!=null) NonPlanifieTraitee.add(parcelle);
                                }else {
                                    if(planifieTraitee!=null) planifieTraitee.add(parcelle);
                                }
                            }else{
                                if(planif==NON_PLANIFIE){
                                    if(NonPlanifieNonTraitee!=null) NonPlanifieNonTraitee.add(parcelle);
                                }else{
                                    if(planifieNonTraitee!=null) planifieNonTraitee.add(parcelle);
                                }
                            }
                        }

                        final int nbRow = Math.max(
                                        Math.max(planifieTraitee==null       ? 0:planifieTraitee.size(),
                                                 planifieNonTraitee==null    ? 0:planifieNonTraitee.size()),
                                        Math.max(NonPlanifieTraitee==null    ? 0:NonPlanifieTraitee.size(),
                                                 NonPlanifieNonTraitee==null ? 0:NonPlanifieNonTraitee.size())
                                    );
                        final int nbCol = (planifieTraitee==null?0:1)
                                        + (planifieNonTraitee==null?0:1)
                                        + (NonPlanifieTraitee==null?0:1)
                                        + (NonPlanifieNonTraitee==null?0:1);

                        //on fait le tableau
                        final Paragraph paragraph2 = doc.addParagraph("");
                        final Table table = doc.addTable(nbRow+1, nbCol);
                        int col = 0;
                        if(planifieTraitee!=null){
                            Cell cell = table.getCellByPosition(col,0);
                            cell.setStringValue("Planifiée/Traitée");
                            for(int i=0,n=planifieTraitee.size();i<n;i++){
                                cell = table.getCellByPosition(col,i+1);
                                final ParcelleVegetation parcelle = planifieTraitee.get(i);
                                cell.setStringValue(strCvt.toString(parcelle));
                            }
                            col++;
                        }
                        if(planifieNonTraitee!=null){
                            Cell cell = table.getCellByPosition(col,0);
                            cell.setStringValue("Planifiée/Non-traitée");
                            for(int i=0,n=planifieNonTraitee.size();i<n;i++){
                                cell = table.getCellByPosition(col,i+1);
                                final ParcelleVegetation parcelle = planifieNonTraitee.get(i);
                                cell.setStringValue(strCvt.toString(parcelle));
                            }
                            col++;
                        }
                        if(NonPlanifieTraitee!=null){
                            Cell cell = table.getCellByPosition(col,0);
                            cell.setStringValue("Non-planifiée/Traitée");
                            for(int i=0,n=NonPlanifieTraitee.size();i<n;i++){
                                cell = table.getCellByPosition(col,i+1);
                                final ParcelleVegetation parcelle = NonPlanifieTraitee.get(i);
                                cell.setStringValue(strCvt.toString(parcelle));
                            }
                            col++;
                        }
                        if(NonPlanifieNonTraitee!=null){
                            Cell cell = table.getCellByPosition(col,0);
                            cell.setStringValue("Non-planifiée/Non-traitée");
                            for(int i=0,n=NonPlanifieNonTraitee.size();i<n;i++){
                                cell = table.getCellByPosition(col,i+1);
                                final ParcelleVegetation parcelle = NonPlanifieNonTraitee.get(i);
                                cell.setStringValue(strCvt.toString(parcelle));
                            }
                            col++;
                        }


                    }

                    Platform.runLater(()->uiProgressLabel.setText("Sauvegarde du fichier ODT"));
                    doc.save(file);


                    Platform.runLater(()->uiProgressLabel.setText("Génération terminée"));
                    try {sleep(2000);} catch (InterruptedException ex) {}

                }catch(Exception ex){
                    SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                    Platform.runLater(()->GeotkFX.newExceptionDialog("Une erreur est survenue lors de la génération du rapport.", ex).show());
                }finally{
                    Platform.runLater(()->{
                        uiProgressLabel.setText("");
                        running.set(false);
                    });
                }
            }
        }.start();


    }

    private static MapLayer createOSMLayer() throws MalformedURLException, DataStoreException, IOException {
//        final CoverageStore store = new OSMTileMapClient(new URL("http://tile.openstreetmap.org"), null, 18, true);
//        final CoverageStore store = new OSMTileMapClient(new URL("http://c.tile.stamen.com/terrain"), null, 18, true);
//        final CoverageStore store = new OSMTileMapClient(new URL("http://c.tile.stamen.com/toner"), null, 18, true);
        final CoverageStore store = new OSMTileMapClient(new URL("https://tile.thunderforest.com/cycle"), new ApiSecurity(SIRSAuthenticator.getThunderForestApiKey()), 18, true);
        for (GenericName n : store.getNames()) {
            final CoverageReference cr = store.getCoverageReference(n);
            final CoverageMapLayer cml = MapBuilder.createCoverageLayer(cr);
            cml.setName("Thunderforest");
            cml.setDescription(new DefaultDescription(
                    new SimpleInternationalString("Thunderforest"),
                    new SimpleInternationalString("Thunderforest")));
            cml.setOpacity(0.4);
            return cml;
        }
        return null;
    }
}
