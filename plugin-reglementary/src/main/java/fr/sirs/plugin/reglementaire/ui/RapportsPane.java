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
package fr.sirs.plugin.reglementaire.ui;

import com.vividsolutions.jts.geom.LineString;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import static fr.sirs.SIRS.AUTHOR_FIELD;
import static fr.sirs.SIRS.COMMENTAIRE_FIELD;
import static fr.sirs.SIRS.DATE_MAJ_FIELD;
import static fr.sirs.SIRS.FOREIGN_PARENT_ID_FIELD;
import static fr.sirs.SIRS.LATITUDE_MAX_FIELD;
import static fr.sirs.SIRS.LATITUDE_MIN_FIELD;
import static fr.sirs.SIRS.LONGITUDE_MAX_FIELD;
import static fr.sirs.SIRS.LONGITUDE_MIN_FIELD;
import static fr.sirs.SIRS.VALID_FIELD;
import fr.sirs.Session;
import fr.sirs.core.InjectorCore;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.AbstractPositionableRepository;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.BorneDigueRepository;
import fr.sirs.core.component.DigueRepository;
import fr.sirs.core.component.Previews;
import fr.sirs.core.component.SystemeEndiguementRepository;
import fr.sirs.core.component.TronconDigueRepository;
import fr.sirs.core.model.*;
import fr.sirs.core.model.report.ModeleRapport;
import fr.sirs.ui.report.FXModeleRapportsPane;
import fr.sirs.util.DatePickerConverter;
import fr.sirs.util.SirsStringConverter;
import fr.sirs.util.StreamingIterable;
import fr.sirs.util.odt.ODTUtils;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.apache.sis.measure.NumberRange;
import org.ektorp.DocumentNotFoundException;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.referencing.LinearReferencing;
import org.geotoolkit.util.collection.CloseableIterator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Display print configuration for obligation report.
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class RapportsPane extends BorderPane {

    // TODO : check ignored fields
    public static final String[] COLUMNS_TO_IGNORE = new String[] {
        AUTHOR_FIELD, VALID_FIELD, FOREIGN_PARENT_ID_FIELD, LONGITUDE_MIN_FIELD,
        LONGITUDE_MAX_FIELD, LATITUDE_MIN_FIELD, LATITUDE_MAX_FIELD,
        DATE_MAJ_FIELD, COMMENTAIRE_FIELD,
        "prDebut", "prFin", "valid", "positionDebut", "positionFin", "epaisseur"};

    @FXML private ComboBox<Preview> uiSystemEndiguement;
    @FXML private ListView<TronconDigue> uiTroncons;
    private Spinner<Double> uiPrDebut;
    @FXML private DatePicker uiPeriodeFin;
    private Spinner<Double> uiPrFin;
    @FXML private DatePicker uiPeriodeDebut;
    @FXML private CheckBox uiCreateObligation;
    @FXML private TextField uiTitre;
    @FXML private GridPane uiGrid;
    @FXML private ComboBox<RefTypeObligationReglementaire> uiTypeObligation;
    @FXML private ComboBox<RefEtapeObligationReglementaire> uiTypeEtape;
    @FXML private Button uiGenerate;
    @FXML private ProgressBar uiProgress;
    @FXML private Label uiProgressLabel;
    @FXML private BorderPane uiListPane;
    @FXML private BorderPane uiEditorPane;
    @FXML private CheckBox uiPeriod;

    private final BooleanProperty running = new SimpleBooleanProperty(false);

    @Autowired
    private Session session;

    private final SimpleObjectProperty<ModeleRapport> modelProperty = new SimpleObjectProperty<>();

    public RapportsPane() {
        super();
        SIRS.loadFXML(this);
        Injector.injectDependencies(this);

        uiGenerate.disableProperty().bind(
                Bindings.or(running, modelProperty.isNull()));

        // model edition
        final FXModeleRapportsPane rapportEditor = new FXModeleRapportsPane();
        modelProperty.bind(rapportEditor.selectedModelProperty());
        uiListPane.setCenter(rapportEditor);
        uiEditorPane.setCenter(rapportEditor.editor);

        // Filter parameters
        uiPrDebut = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE,0.0));
        uiPrDebut.setEditable(true);
        uiPrFin = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE,0.0));
        uiPrFin.setEditable(true);
        uiGrid.add(uiPrDebut, 1, 3);
        uiGrid.add(uiPrFin, 3, 3);

        uiPeriod.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                final LocalDate date = LocalDate.now();
                if (uiPeriodeDebut.getValue() == null) {
                    uiPeriodeDebut.valueProperty().set(date.minus(10, ChronoUnit.YEARS));
                }
                if (uiPeriodeFin.getValue() == null) {
                    uiPeriodeFin.setValue(date);
                }
            }
        });

        uiPeriodeDebut.disableProperty().bind(uiPeriod.selectedProperty().not());
        uiPeriodeDebut.editableProperty().bind(uiPeriod.selectedProperty());
        uiPeriodeFin.disableProperty().bind(uiPeriod.selectedProperty().not());
        uiPeriodeFin.editableProperty().bind(uiPeriod.selectedProperty());
        DatePickerConverter.register(uiPeriodeDebut);
        DatePickerConverter.register(uiPeriodeFin);

        uiSystemEndiguement.valueProperty().addListener(this::systemeEndiguementChange);
        uiTroncons.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        uiTroncons.getSelectionModel().getSelectedItems().addListener(this::tronconSelectionChange);
        final SirsStringConverter converter = new SirsStringConverter();
        uiTroncons.setCellFactory(param -> {
            return new ListCell() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(converter.toString(item));
                }
            };
        });

        final Previews previewRepository = session.getPreviews();
        SIRS.initCombo(uiSystemEndiguement, SIRS.observableList(previewRepository.getByClass(SystemeEndiguement.class)).sorted(), null);

        SIRS.initCombo(uiTypeObligation, SIRS.observableList(session.getRepositoryForClass(RefTypeObligationReglementaire.class).getAll()), null);
        uiTypeObligation.disableProperty().bind(uiCreateObligation.selectedProperty().not());

        SIRS.initCombo(uiTypeEtape, SIRS.observableList(session.getRepositoryForClass(RefEtapeObligationReglementaire.class).getAll()), null);
        uiTypeEtape.disableProperty().bind(uiCreateObligation.selectedProperty().not());

        // Pour mettre a jour l'etat actif des boutons
        tronconSelectionChange(null);
    }

    private void systemeEndiguementChange(ObservableValue<? extends Preview> observable, Preview oldValue, Preview newValue) {
        if(newValue==null){
            uiTroncons.setItems(FXCollections.emptyObservableList());
        }else{
            final SystemeEndiguementRepository sdRepo = (SystemeEndiguementRepository) session.getRepositoryForClass(SystemeEndiguement.class);
            final DigueRepository digueRepo = (DigueRepository) session.getRepositoryForClass(Digue.class);

            final TronconDigueRepository tronconRepo = Injector.getBean(TronconDigueRepository.class);
            final SystemeEndiguement sd = sdRepo.get(newValue.getElementId());
            final Set<TronconDigue> troncons = new HashSet<>();
            final List<Digue> digues = digueRepo.getBySystemeEndiguement(sd);
            for(Digue digue : digues){
                troncons.addAll(tronconRepo.getByDigue(digue));
            }
            uiTroncons.setItems(FXCollections.observableArrayList(troncons));
        }
    }

    private void tronconSelectionChange(ListChangeListener.Change<? extends TronconDigue> c) {
        final ObservableList<TronconDigue> selectedItems = uiTroncons.getSelectionModel().getSelectedItems();
        if (selectedItems.size() == 1) {
            final TronconDigue troncon = selectedItems.get(0);
            final LineString lineTroncon = LinearReferencing.asLineString(troncon.getGeometry());
            final LinearReferencing.SegmentInfo[] segments = LinearReferencingUtilities.buildSegments(lineTroncon);
            final SystemeReperage sr = Injector.getSession().getRepositoryForClass(SystemeReperage.class).get(troncon.getSystemeRepDefautId());
            final BorneDigueRepository borneRepo = InjectorCore.getBean(BorneDigueRepository.class);

            final float prDebVal = TronconUtils.computePR(segments, sr, lineTroncon.getStartPoint(), borneRepo);
            uiPrDebut.getValueFactory().setValue(new BigDecimal(prDebVal).doubleValue());
            final float prFinVal = TronconUtils.computePR(segments, sr, lineTroncon.getEndPoint(), borneRepo);
            uiPrFin.getValueFactory().setValue(new BigDecimal(prFinVal).doubleValue());

            uiPrDebut.setDisable(false);
            uiPrFin.setDisable(false);
        }else{
            uiPrDebut.getValueFactory().setValue(0d);
            uiPrFin.getValueFactory().setValue(0d);
            uiPrDebut.setDisable(true);
            uiPrFin.setDisable(true);
        }
    }

    /**
     * Méthode de génération du rapport.
     *
     * @param event
     */
    @FXML
    private void generateReport(ActionEvent event) {
        final ModeleRapport report = modelProperty.get();
        if (report == null) return;

        /*
        A- détermination de l'emplacement du fichier de sortie
        ======================================================*/

        final FileChooser chooser = new FileChooser();
        final Path previous = getPreviousPath();
        if (previous != null) {
            chooser.setInitialDirectory(previous.toFile());
            chooser.setInitialFileName(".odt");
        }
        final File file = chooser.showSaveDialog(null);
        if(file==null) return;

        final Path output = file.toPath();
        setPreviousPath(output.getParent());



        /*
        B- détermination des paramètres de création de l'obligation réglementaire, le cas échéant
        =========================================================================================*/

        final RefTypeObligationReglementaire typeObligation = uiTypeObligation.valueProperty().get();
        final RefEtapeObligationReglementaire typeEtape = uiTypeEtape.valueProperty().get();
        final Preview sysEndi = uiSystemEndiguement.valueProperty().get();
        final String titre = uiTitre.getText();


        /*
        C- détermination des paramètres de filtrage des éléments sur le tronçon
        ======================================================================*/

        final LocalDate periodeDebut = uiPeriod.isSelected() ? uiPeriodeDebut.getValue() : null;
        final LocalDate periodeFin = uiPeriod.isSelected() ? uiPeriodeFin.getValue() : null;
        final NumberRange dateRange;
        final AvecBornesTemporelles.IntersectDateRange intersectDateRangePredicate;
        if (periodeDebut == null && periodeFin == null) {
            dateRange = null;
            intersectDateRangePredicate = null;
        } else {
            final long dateDebut = periodeDebut == null ? 0 : periodeDebut.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
            final long dateFin = periodeFin == null ? Long.MAX_VALUE : periodeFin.atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli();
            dateRange = NumberRange.create(dateDebut, true, dateFin, true);
            intersectDateRangePredicate = new AvecBornesTemporelles.IntersectDateRange(dateRange);
        }

        final Double prDebut = uiPrDebut.getValue() == null? -1d : uiPrDebut.getValue();
        final Double prFin = uiPrFin.getValue() == null? -1d : uiPrFin.getValue();
        final NumberRange prRange;
        if (prDebut <= 0d && prFin <= 0d) {
            prRange = null;
        } else {
            prRange = NumberRange.create(Math.min(prDebut, prFin), true, Math.max(prDebut, prFin), true);
        }


        /*
        D- création de la tâche générale de création du rapport
        ======================================================*/

        final Task task;
        task = new Task() {

            @Override
            protected Object call() throws Exception {
                updateTitle("Création d'un rapport");


                /*
                1- détermination de la liste des éléments à inclure dans le rapport
                ------------------------------------------------------------------*/

                // on liste tous les elements a générer
                updateMessage("Recherche des objets du rapport...");
                final ObservableList<TronconDigue> troncons = uiTroncons.getSelectionModel().getSelectedItems();
                final Collection<AbstractPositionableRepository<Objet>> repos = (Collection) session.getRepositoriesForClass(Objet.class);
                final ArrayList<Objet> elements = new ArrayList<>();
                for (TronconDigue troncon : troncons) {
                    if (troncon == null)
                        continue;

                    for (final AbstractPositionableRepository<Objet> repo : repos) {
                        StreamingIterable<Objet> tmpElements = repo.getByLinearIdStreaming(troncon.getId());
                        try (final CloseableIterator<Objet> it = tmpElements.iterator()) {
                            while (it.hasNext()) {
                                Objet next = it.next();
                                if (dateRange != null) {
                                    //on vérifie la date
                                    if (!intersectDateRangePredicate.test(next)) {
                                        continue;
                                    }
                                }

                                //on verifie la position
                                if (prRange != null) {
                                    if (!prRange.intersectsAny(NumberRange.create(next.getPrDebut(), true, next.getPrFin(), true))) {
                                        continue;
                                    }
                                }

                                elements.add(next);
                            }
                        }
                    }
                }


                /*
                2- génération du rapport
                -----------------------*/

                final Task reportGenerator = ODTUtils.generateReport(report, troncons.isEmpty()? null : elements, output, titre);
                Platform.runLater(() -> {
                    reportGenerator.messageProperty().addListener((obs, oldValue, newValue) -> updateMessage(newValue));
                    reportGenerator.workDoneProperty().addListener((obs, oldValue, newValue) -> updateProgress(newValue.doubleValue(), reportGenerator.getTotalWork()));
                });
                reportGenerator.get();


                /*
                3- création de l'obligation réglementaire
                ----------------------------------------*/

                updateProgress(-1, -1);
                if (uiCreateObligation.isSelected()) {
                    updateMessage("Création de l'obligation réglementaire");
                    //on crée une obligation à la date d'aujourdhui
                    final AbstractSIRSRepository<ObligationReglementaire> rep = session.getRepositoryForClass(ObligationReglementaire.class);
                    final AbstractSIRSRepository<EtapeObligationReglementaire> eorr = session.getRepositoryForClass(EtapeObligationReglementaire.class);
                    final ObligationReglementaire obligation = rep.create();
                    final LocalDate date = LocalDate.now();
                    obligation.setAnnee(date.getYear());
                    obligation.setLibelle(titre);
                    if (sysEndi != null){
                        obligation.setSystemeEndiguementId(sysEndi.getElementId());
                    }
                    if (typeObligation != null){
                        obligation.setTypeId(typeObligation.getId());
                    }
                    rep.add(obligation);

                    final EtapeObligationReglementaire etape = eorr.create();
                    etape.setDateRealisation(date);
                    etape.setObligationReglementaireId(obligation.getId());
                    if (typeEtape != null){
                        etape.setTypeEtapeId(typeEtape.getId());
                    }
                    eorr.add(etape);
                }
                return true;
            }
        };

        uiProgress.visibleProperty().bind(task.runningProperty());
        uiProgress.progressProperty().bind(task.progressProperty());
        uiProgressLabel.visibleProperty().bind(task.runningProperty());
        uiProgressLabel.textProperty().bind(task.messageProperty());
        running.bind(task.runningProperty());
        disableProperty().bind(task.runningProperty());

        task.setOnFailed((failEvent) -> {
            SIRS.LOGGER.log(Level.WARNING, "An error happened while creating a report.", task.getException());
            Platform.runLater(() -> GeotkFX.newExceptionDialog("Une erreur est survenue lors de la génération du rapport.", task.getException()).show());
        });

        task.setOnSucceeded((successEvent) -> Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, "La génération du rapport s'est terminée avec succès", ButtonType.OK).show()));

        TaskManager.INSTANCE.submit(task);
    }

    private static interface Printer{

        public Object print(Object candidate) throws Exception;
    }

    private class BorneDebutPrinter implements Printer{

        private final SirsStringConverter cvt = new SirsStringConverter();

        @Override
        public Object print(Object candidate) throws Exception {
            if(!(candidate instanceof Positionable)) return null;

            final Positionable p = (Positionable) candidate;

            final String borneId = p.getBorneDebutId();
            if(borneId==null) return null;

            final boolean borneAval = p.getBorne_debut_aval();
            final double borneDistance = p.getBorne_debut_distance();

            try{
                final Preview preview = Injector.getSession().getPreviews().get(borneId);
                final StringBuilder sb = new StringBuilder(cvt.toString(preview));
                if(borneDistance!=0.0){
                    sb.append( borneAval ? " en aval de " : " en amont de ");
                    sb.append((int)borneDistance);
                    sb.append("m");
                }
                return sb.toString();
            }catch(DocumentNotFoundException ex){
                return null;
            }
        }
    }

    private class BorneFinPrinter implements Printer{

        private final SirsStringConverter cvt = new SirsStringConverter();

        @Override
        public Object print(Object candidate) throws Exception {
            if(!(candidate instanceof Positionable)) return null;

            final Positionable p = (Positionable) candidate;

            final String borneId = p.getBorneFinId();
            if(borneId==null) return null;

            final boolean borneAval = p.getBorne_fin_aval();
            final double borneDistance = p.getBorne_fin_distance();

            try{
                final Preview preview = Injector.getSession().getPreviews().get(borneId);
                final StringBuilder sb = new StringBuilder(cvt.toString(preview));
                if(borneDistance!=0.0){
                    sb.append( borneAval ? " en aval de " : " en amont de ");
                    sb.append((int)borneDistance);
                    sb.append("m");
                }
                return sb.toString();
            }catch(DocumentNotFoundException ex){
                return null;
            }
        }
    }

    /**
     *
     * @return Last chosen path for generation report, or null if we cannot find any.
     */
    private static Path getPreviousPath() {
        final Preferences prefs = Preferences.userNodeForPackage(RapportsPane.class);
        final String str = prefs.get("path", null);
        if (str != null) {
            final Path file = Paths.get(str);
            if (Files.isDirectory(file)) {
                return file;
            }
        }
        return null;
    }

    /**
     * Set value to be retrieved by {@link #getPreviousPath() }.
     * @param path To put as previously chosen path. Should be a directory.
     */
    private static void setPreviousPath(final Path path) {
        final Preferences prefs = Preferences.userNodeForPackage(RapportsPane.class);
        prefs.put("path", path.toAbsolutePath().toString());
    }
}
