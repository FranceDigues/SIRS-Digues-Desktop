/**
 * This file is part of SIRS-Digues 2.
 * Copyright (C) 2016, FRANCE-DIGUES,
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.plugin.reglementaire.ui;

import fr.sirs.Injector;
import fr.sirs.PropertiesFileUtilities;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.*;
import fr.sirs.core.model.*;
import fr.sirs.plugin.reglementaire.RegistreTheme;
import fr.sirs.util.DatePickerConverter;
import fr.sirs.util.PrinterUtilities;
import fr.sirs.util.SirsStringConverter;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.DocumentNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Display print configuration and generate report for horodatage purpose : prestation synthese table report.
 * <p>
 * Redmine ticket #7782
 * <p>
 * <ul>
 * <li> Allow to select the file destination folder.</li>
 * <li> Modify the title inside the document.</li>
 * <li> "Periode" checkbox allows to filter the prestations by date.</li>
 * <li> Input dates are used inside the report to add a note for the time period.
 *      User can set the dates even though the checkbox is unselected (client's request).</li>
 * <li> Button to select "non horodatée" prestations in one click.</li>
 * </ul>
 * <p>
 * @author Estelle Idee (Geomatys)
 */
public class HorodatageReportPane extends BorderPane {
    @FXML
    private ComboBox<Preview> uiSystemEndiguement;
    @FXML
    private TableView<Prestation> uiPrestationTable;

    /**
     *  When the uiPeriod checkbox is unselected, the uiPeriodeDebut and uiPeriodeFin can still be updated.
     *  The values are used in the generated Tableau de synthèse event though the uiPeriod is unselected.
     */
    @FXML
    private DatePicker uiPeriodeFin;
    @FXML
    private DatePicker uiPeriodeDebut;
    @FXML
    private TextField uiTitre;
    @FXML
    private Button uiSelectNonTimeStamped;
    @FXML
    private Button uiGenerate;
    @FXML
    private CheckBox uiPeriod;
    @FXML
    private TableColumn ui_prestation;
    @FXML
    private TableColumn ui_troncon;
    @FXML
    private TableColumn ui_type;
    @FXML
    private TableColumn ui_statut;

    private Preview selectedSE;
    private List<Prestation> allPrestationsOnSE;

    private final String JRXML_PATH = "/fr/sirs/jrxml/metaTemplatePrestationSyntheseTable.jrxml";
    private final SirsStringConverter converter = new SirsStringConverter();

    static int compareDates(LocalDate dateFin1, LocalDate dateFin2, LocalDate dateDebut1, LocalDate dateDebut2) {
        LocalDate date1     = dateFin1 != null ? dateFin1 : dateDebut1;
        LocalDate date2     = dateFin2 != null ? dateFin2 : dateDebut2;
        return compareDates(date1, date2);
    }

    static int compareDates(LocalDate date1, LocalDate date2) {
        if (date1 == null) {
            if (date2 == null) return 0;
            return -1;
        }
        if (date2 == null) return 1;
        return date1.compareTo(date2);
    }


    @Autowired
    private Session session;

    private static final String TITLE = "Tableau de synthèse prestation pour Registre horodaté";

    public HorodatageReportPane() {
        super();

        SIRS.loadFXML(this);
        Injector.injectDependencies(this);

        uiTitre.setText(TITLE);

        // Date filter checkbox
        // When the checkbox is unselected, the uiPeriodeDebut and uiPeriodeFin can still be updated.
        // The values are used in the generated Tableau de synthèse event though the uiPeriod is unselected.
        // When the checkbox is unselected, the uiPeriodeDebut and uiPeriodeFin can still be updated.
        // The values are used in the generated Tableau de synthèse event though the uiPeriod is unselected.
        uiPeriod.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            if (newValue) {
                final LocalDate date = LocalDate.now();
                if (uiPeriodeDebut.getValue() == null) {
                    uiPeriodeDebut.valueProperty().set(date.minus(10, ChronoUnit.YEARS));
                }
                if (uiPeriodeFin.getValue() == null) {
                    uiPeriodeFin.setValue(date);
                }
            }
            updatePrestationsAndKeepSelection();
        });

        DatePickerConverter.register(uiPeriodeDebut);
        DatePickerConverter.register(uiPeriodeFin);
        uiPeriodeDebut.valueProperty().addListener((observable, oldValue, newValue) -> {
            uiPeriodeFin.setDayCellFactory(RegistreTheme.getUiPeriodFinDayCellFactory(uiPeriodeDebut));
            if (uiPeriod.isSelected()) updatePrestationsAndKeepSelection();
        });

        uiPeriodeFin.valueProperty().addListener((observable, oldValue, newValue) -> {
            uiPeriodeDebut.setDayCellFactory(RegistreTheme.getUiPeriodDebutDayCellFactory(uiPeriodeFin));
            if (uiPeriod.isSelected()) updatePrestationsAndKeepSelection();
        });

        uiSystemEndiguement.valueProperty().addListener((obs, o, n) -> updatePrestationsList(n));

        initSeCombo(uiSystemEndiguement);

        uiSelectNonTimeStamped.setTooltip(new Tooltip("Sélectionne toutes les prestations avec le status \"non horodatée\" de la liste."));

        // TableView to show the prestations, their tronçon, their type and their status.
        uiPrestationTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        uiPrestationTable.getColumns().forEach(column -> {
            column.setEditable(false);
            if (column.equals(ui_prestation)) {
                column.setCellValueFactory((Callback) param -> {
                    final Object item = ((TableColumn.CellDataFeatures)param).getValue();
                    if (item != null) {
                        return new SimpleObjectProperty(item);
                    }
                    return null;
                });
                column.setCellFactory((Callback) param -> new TableCell<Prestation, Prestation>() {
                    @Override
                    protected void updateItem(Prestation item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(converter.toString(item));
                    }
                });
            } else if (column.equals(ui_troncon)) {
                column.setCellValueFactory((Callback) param -> {
                    final Object item = ((TableColumn.CellDataFeatures)param).getValue();
                    if (item != null) {
                        Prestation presta = (Prestation) item;
                        return new SimpleObjectProperty(presta.getLinearId());
                    }
                    return null;
                });
                column.setCellFactory((Callback) param -> new TableCell<Prestation, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        String text = "";
                        if (item != null) {
                            try {
                                final TronconDigue troncon = Injector.getBean(TronconDigueRepository.class).get(item);
                                if (troncon != null)
                                    text = troncon.getLibelle();
                            } catch (DocumentNotFoundException e) {
                                SIRS.LOGGER.warning("Error while getting TronconDigue with id :" + item);
                            }
                        }
                        if ("".equals(text)) text = item;
                        setText(text);
                    }
                });
            } else if (column.equals(ui_type)) {
                column.setCellValueFactory((Callback) param -> {
                    final Object item = ((TableColumn.CellDataFeatures)param).getValue();
                    if (item != null) {
                        Prestation presta = (Prestation) item;
                        return new SimpleObjectProperty(presta.getTypePrestationId());
                    }
                    return null;
                });
                column.setCellFactory((Callback) param -> new TableCell<Prestation, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        String text = "";
                        if (item != null) {
                            try {
                                final RefPrestation typePresta = Injector.getBean(RefPrestationRepository.class).get(item);
                                if (typePresta != null)
                                    text = typePresta.getLibelle();
                            } catch (DocumentNotFoundException e) {
                                SIRS.LOGGER.warning("Error while getting RefPrestation with id :" + item);
                            }
                        }
                        if ("".equals(text)) text = item;
                        setText(text);
                    }
                });
            } else if (column.equals(ui_statut)) {
                column.setStyle("-fx-alignment: CENTER");
                column.setCellValueFactory((Callback) param -> {
                    final Object item = ((TableColumn.CellDataFeatures)param).getValue();
                    if (item != null) {
                        Prestation presta = (Prestation) item;
                        return new SimpleObjectProperty(presta.getHorodatageStatusId());
                    }
                    return null;
                });
                column.setCellFactory((Callback) param -> new TableCell<Prestation, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        String text = "";
                        if (item != null) {
                            try {
                                final RefHorodatageStatus status = Injector.getBean(RefHorodatageStatusRepository.class).get(item);
                                if (status != null) {
                                    text = status.getLibelle();
                                    if (HorodatageReference.getRefNonTimeStampedStatus().equals(item))
                                        setTextFill(Color.BROWN);
                                    else if (HorodatageReference.getRefWaitingStatus().equals(item))
                                        setTextFill(Color.SALMON);
                                    else if (HorodatageReference.getRefTimeStampedStatus().equals(item))
                                        setTextFill(Color.SEAGREEN);
                                    else
                                        setTextFill(Color.BLACK);
                                }
                            } catch (DocumentNotFoundException e) {
                                SIRS.LOGGER.warning("Error while getting RefHorodatageStatus with id :" + item);
                                setTextFill(Color.BLACK);
                            }
                        }
                        if ("".equals(text)) text = item;
                        setText(text);
                    }
                });
            }
        });
    }

    static void initSeCombo(final ComboBox<Preview> comboBox) {
        final Previews previewRepository = Injector.getSession().getPreviews();
        // Create sePreviews first and then add elements otherwise it adds the Preview to the previewRepository.
        final List<Preview> sePreviews = new ArrayList<>(previewRepository.getByClass(SystemeEndiguement.class));
        final Preview noSEPreview = new Preview();
        noSEPreview.setLibelle("Tronçon(s) sans système d'endiguement actuellement");
        sePreviews.add(noSEPreview);
        SIRS.initCombo(comboBox, SIRS.observableList(sePreviews).sorted(), null);
    }

    /**
     * Method to update the list of available prestations for the selected Systeme Endiguement @this.selectedSE after a date filter modification :
     * <ul>
     *     <li>Selection/unselection of the Period checkbox @uiPeriod</li>
     *     <li>Modification of the start date @uiPeriodeDebut</li>
     *     <li>Modification of the end date @uiPeriodeFin</li>
     * </ul>
     * Keeps the previously selected prestations if still available.
     */
    private void updatePrestationsAndKeepSelection() {
        List<Prestation> selectedPresta = new ArrayList<>(uiPrestationTable.getSelectionModel().getSelectedItems());
        updatePrestationsList(this.selectedSE);

        final ObservableList<Prestation> items = uiPrestationTable.getItems();

        if (items == null || items.isEmpty()) return;

        uiPrestationTable.getSelectionModel().clearSelection();
        // Keeps previous selection
        selectedPresta.forEach(presta -> uiPrestationTable.getSelectionModel().select(presta));
    }

    /**
     * Method to update the list of available @{@link Prestation} for the input @{@link SystemeEndiguement}
     * and apply date filter if the checkbox has been selected.
     *
     * <p>If the input @{@link SystemeEndiguement} has changed since the last selection, then the @{@link Prestation} are collected from the data base.<br>
     *  Otherwise the prestations are recovered from the class variable @allPrestationsOnSE.</p>
     *
     * @param newValue the @{@link SystemeEndiguement} for which to collect the available @{@link Prestation} and apply date filter if necessary.
     */
    private void updatePrestationsList(Preview newValue) {
        if (newValue == null) {
            uiPrestationTable.setItems(FXCollections.emptyObservableList());
        } else {
            // the @SystemeEndiguement has changed, the class variables are updated with the new values.
            if (!newValue.equals(this.selectedSE)) {
                this.selectedSE = newValue;
                this.allPrestationsOnSE = getAllPrestationsInSelectedSeRegistre();
            }

            if (this.allPrestationsOnSE == null || this.allPrestationsOnSE.isEmpty()) {
                uiPrestationTable.setItems(FXCollections.emptyObservableList());
                return;
            }

            // copy the list to filter it without modifying the original one.
            List<Prestation> prestations = new ArrayList<>(this.allPrestationsOnSE);
            /*
             * Apply date filter
             */
            if (uiPeriod.isSelected()) {
                prestations = filterPrestationsByDate(prestations);
            }
            uiPrestationTable.setItems(FXCollections.observableArrayList(prestations));
        }
    }

    /**
     * Method to get all the prestations on the selected @{@link SystemeEndiguement} from the @{@link Preview}
     *
     * @return The list of all the @{@link Prestation} available on @this.selectedSE. <br>
     * <ul>
     *     <li>Null if no @{@link Digue} on the SE;</li>
     *     <li>Empty ArrayList if no @{@link TronconDigue} or no @{@link Prestation}.</li>
     * </ul>
     */
    private List<Prestation> getAllPrestationsInSelectedSeRegistre() {
        if (this.selectedSE == null) return new ArrayList<>();
        return getAllPrestationsInSeRegistre(this.selectedSE, this.session);
    }

    /**
     * Method to get all the prestations on the input @{@link SystemeEndiguement} from the @{@link Preview}
     *
     * @return The list of all the @{@link Prestation} available on the @{@link SystemeEndiguement}. <br>
     * <ul>
     *     <li>Null if no @{@link Digue} on the SE;</li>
     *     <li>Empty ArrayList if no @{@link TronconDigue} or no @{@link Prestation}.</li>
     * </ul>
     */
    protected static List<Prestation> getAllPrestationsInSeRegistre(final Preview sePreview, final Session session) {
        final SystemeEndiguementRepository sdRepo   = (SystemeEndiguementRepository) session.getRepositoryForClass(SystemeEndiguement.class);
        final DigueRepository digueRepo             = (DigueRepository) session.getRepositoryForClass(Digue.class);
        final TronconDigueRepository tronconRepo    = Injector.getBean(TronconDigueRepository.class);
        final PrestationRepository prestationRepo   = Injector.getBean(PrestationRepository.class);

        SystemeEndiguement se = null;
        final String elementId = sePreview.getElementId();
        if (elementId != null) {
            se = sdRepo.get(elementId);
            if (se == null) return null;
        }

        final List<TronconDigue> tronconList = new ArrayList<>();

        // new String[] {elementId} in case elementId is null.
        tronconList.addAll(digueRepo.getBySystemeEndiguementIds(new String[] {elementId}).stream().flatMap(digue -> tronconRepo.getByDigue(digue).stream())
                .collect(Collectors.toList()));
        if (se == null) {
            tronconList.addAll(tronconRepo.getByDigueIds(new String[] {null}));
        }

        return tronconList.stream()
                .flatMap(troncon -> prestationRepo.getByLinear(troncon).stream())
                .filter(Prestation::getRegistreAttribution)
                .collect(Collectors.toList());
    }

    /**
     * Filter the list of the prestations according to the date pickers.
     *
     * @param prestations the list of the prestations to filter.
     * @return the list of the prestations after date filtering.
     */
    private List<Prestation> filterPrestationsByDate(List<Prestation> prestations) {
        ArgumentChecks.ensureNonNull("prestations", prestations);
        if (prestations.isEmpty() || !uiPeriod.isSelected()) return prestations;

        final LocalDate periodeDebut    = uiPeriodeDebut.getValue();
        final LocalDate periodeFin      = uiPeriodeFin.getValue();
        final NumberRange dateRange;
        if (periodeDebut == null && periodeFin == null) {
            dateRange = null;
        } else {
            final long dateDebut    = periodeDebut == null ? 0 : periodeDebut.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
            final long dateFin      = periodeFin == null ? Long.MAX_VALUE : periodeFin.atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli();
            dateRange               = NumberRange.create(dateDebut, true, dateFin, true);
        }

        if (dateRange != null) {
            AvecBornesTemporelles.IntersectDateRange dateRangePredicate = new AvecBornesTemporelles.IntersectDateRange(dateRange);
            return prestations.stream().filter(dateRangePredicate).collect(Collectors.toList());
        }
        return prestations;
    }

    /**
     * Method to select all the prestations available in the @uiPrestations.
     * Action directly implemented in the .fxml
     */
    @FXML
    private void selectAll() {
        uiPrestationTable.getSelectionModel().selectAll();
    }

    /**
     * Method to select all "non horodatée" prestations available in the @uiPrestations.
     *
     */
    @FXML
    private void selectNonTimeStamped() {
        List<Prestation> prestations = uiPrestationTable.getItems();
        if (prestations.isEmpty()) return;
        // Keeps all prestations with a status "Non horodaté"
        prestations.stream()
                .filter(Objects::nonNull)
                .filter(prestation -> HorodatageReference.getRefNonTimeStampedStatus().equals(prestation.getHorodatageStatusId()))
                .forEach(presta -> uiPrestationTable.getSelectionModel().select(presta));
    }


    /**
     * <p>Method to generate the Rapport de synthèse for the selected prestations.</p>
     * <ul>
     *     <li>Let the user choose the destination folder and output file's name</li>
     *     <li>Open the created PDF</li>
     *     <li>Uses the JRXML "/fr/sirs/jrxml/metaTemplatePrestationSyntheseTable.jrxml"</li>
     * </ul>
     *
     */
    @FXML
    private void generateReport() {
        List<Prestation> prestations = FXCollections.observableArrayList(uiPrestationTable.getSelectionModel().getSelectedItems());
        if (prestations.isEmpty()) return;

        // Check each prestation :
        // 1. it has never been horodated (no start timestamp nor end timestamp) :
        //     a. status : "Non horodaté" -> no warning
        //     b. status : "En attente" or "horodaté" -> warning
        // 2. it has a start timestamp but no validity end date -> warning message
        // 3. it has an end timestamp -> warning message
        final List<Prestation> prestationsToKeep = new ArrayList<>();
        boolean yesForAll = false;
        ButtonType yesForAllButton = new ButtonType("Oui et suivants", ButtonBar.ButtonData.APPLY);
        ButtonType noForAllButton = new ButtonType("Non et suivants", ButtonBar.ButtonData.FINISH);
        for (Prestation prestation : prestations) {
            if (!yesForAll) {
                final LocalDate horodatageStartDate = prestation.getHorodatageStartDate();
                final LocalDate horodatageEndDate = prestation.getHorodatageEndDate();
                String message = null;
                final String prestaText =  converter.toString(prestation) + " / " + prestation.getId();
                if (horodatageStartDate == null && horodatageEndDate == null) {
                    if (HorodatageReference.getRefWaitingStatus().equals(prestation.getHorodatageStatusId())) {
                        message = "La prestation " + prestaText + " est en cours d'horodatage.";
                    } else if (HorodatageReference.getRefTimeStampedStatus().equals(prestation.getHorodatageStatusId())) {
                        message = "Le statut de la prestation " + prestaText + " est \"Horodaté\" mais aucune date d'horodatage n'est renseignée.";
                    }
                } else if (horodatageEndDate != null) {
                    message = "La prestation " + prestaText + " a déjà été horodatée le " + horodatageEndDate + " pour la date de fin de validité : " + prestation.getHorodatageDateFinEnd() + ".";
                } else if (horodatageStartDate != null && prestation.getDate_fin() == null) {
                    message = "La prestation " + prestaText + " a déjà été horodatée le " + horodatageStartDate + " pour la date de début de validité : " + prestation.getHorodatageDateDebutStart() + ".";
                }

                if (message != null) {
                    message = message.concat("\n\nSouhaitez-vous tout de même relancer le processus d'horodatage pour cette prestation ?" +
                            "\n\nAnnuler : la création du tableau de synthèse sera annulée.");
                    final Optional optional;
                    if (prestations.size() == 1) {
                        optional = PropertiesFileUtilities.showConfirmationDialog(message, "Conflit", 700, 200, true);
                    } else {
                        optional = PropertiesFileUtilities.createAlert(Alert.AlertType.CONFIRMATION, "Conflit", message, 700, 200, ButtonType.YES, ButtonType.NO, yesForAllButton, noForAllButton, ButtonType.CANCEL);
                    }

                    if (optional.isPresent()) {
                        final Object result = optional.get();
                        if (ButtonType.NO.equals(result)) {
                            continue;
                        } else if (ButtonType.CANCEL.equals(result)) {
                            return;
                        } else if (noForAllButton.equals(result)) {
                            break;
                        } else if (yesForAllButton.equals(result)) {
                            yesForAll = true;
                        }
                    }
                }
            }
            prestationsToKeep.add(prestation);
        }

        if (prestationsToKeep.isEmpty()) {
            PropertiesFileUtilities.showInformationDialog("Aucune prestation à ajouter au tableau de synthèse.");
            return;
        }

        // sort prestations by date_fin if available, by date_debut otherwise.
        prestationsToKeep.sort((p1, p2) -> compareDates(p1.getDate_fin(), p2.getDate_fin(), p1.getDate_debut(), p2.getDate_debut()));

        /*
        A- Selection of the output file destination folder.
        ======================================================*/

        final FileChooser chooser   = new FileChooser();
        final Path previous         = RegistreTheme.getPreviousPath(HorodatageReportPane.class);
        if (previous != null) {
            chooser.setInitialDirectory(previous.toFile());
            chooser.setInitialFileName(".pdf");
        }
        final File file = chooser.showSaveDialog(null);
        if (file == null) return;

        final Path output = file.toPath();
        RegistreTheme.setPreviousPath(output.getParent(), HorodatageReportPane.class);

        /*
        A- Creation of the PDF from the jasper template.
        ======================================================*/
        JRBeanCollectionDataSource beanColDataSource    = new JRBeanCollectionDataSource(prestationsToKeep);
        Map<String, Object> parameters                  = new HashMap<>();

        // set report parameters
        parameters.put("title", uiTitre.getText());
        parameters.put("collectionBeanParam", beanColDataSource);
        parameters.put("systemeEndiguement", this.selectedSE.getLibelle());
        parameters.put("dateDebutPicker", uiPeriodeDebut.getValue());
        parameters.put("dateFinPicker", uiPeriodeFin.getValue());

        try (InputStream input = PrinterUtilities.class.getResourceAsStream(JRXML_PATH);
             OutputStream outputStream   = new FileOutputStream(file)){

            JasperDesign jasperDesign   = JRXmlLoader.load(input);
            JasperReport jasperReport   = JasperCompileManager.compileReport(jasperDesign);
            JasperPrint jasperPrint     = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);

            // Change prestations' horodatage status to "En attente".
            PrestationRepository repo   = Injector.getBean(PrestationRepository.class);
            prestationsToKeep.forEach(p -> {
                p.setHorodatageStatusId(HorodatageReference.getRefWaitingStatus());
                repo.update(p);
            });

        } catch (FileNotFoundException e) {
            throw new IllegalStateException("The jrxml file was not found at " + JRXML_PATH, e);
        } catch (JRException e) {
            throw new IllegalStateException("Error while creating the synthese prestation report from jrxml file", e);
        } catch (IOException e) {
            throw new IllegalStateException("Error while creating inputStream for " + JRXML_PATH);
        }
        SIRS.openFile(output);
        // Refresh the table to update timestamp status.
        refresh();
    }

    public void resetPane() {
        uiTitre.setText(TITLE);
        uiPeriod.setSelected(false);
        uiPeriodeDebut.setValue(null);
        uiPeriodeFin.setValue(null);
        uiSystemEndiguement.getSelectionModel().clearSelection();
    }

    public void refresh() {
        uiPrestationTable.refresh();
    }
}
