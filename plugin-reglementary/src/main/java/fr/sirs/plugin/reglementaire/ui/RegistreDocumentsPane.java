
/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2016, FRANCE-DIGUES,
 * <p>
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.plugin.reglementaire.ui;

import com.giaybac.traprange.PDFTableExtractor;
import com.giaybac.traprange.entity.Table;
import com.giaybac.traprange.entity.TableCell;
import com.giaybac.traprange.entity.TableRow;
import fr.sirs.Injector;
import fr.sirs.PropertiesFileUtilities;
import fr.sirs.SIRS;
import fr.sirs.core.component.PrestationRepository;
import fr.sirs.core.model.HorodatageReference;
import fr.sirs.core.model.Prestation;
import fr.sirs.core.model.Role;
import fr.sirs.plugin.reglementaire.FileTreeItem;
import fr.sirs.plugin.reglementaire.PluginReglementary;
import fr.sirs.plugin.reglementaire.ReglementaryPropertiesFileUtilities;
import fr.sirs.plugin.reglementaire.RegistreTheme;
import fr.sirs.ui.Growl;
import fr.sirs.util.JRXMLUtil;
import fr.sirs.util.SirsStringConverter;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.nio.IOUtilities;

import java.io.*;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import static fr.sirs.plugin.reglementaire.ReglementaryPropertiesFileUtilities.*;

/**
 * Display Registre tree.
 * <ul>
 *     <li>Allows to add folders,</li>
 *     <li><ul>Allows to import timestamped synthese table PDF files :
 *          <li>Input horodatage date</li>
 *          <li>Option to automatically update the table prestations</li>
 *     </ul></li>
 *     <li>Allows to configure the root folder,</li>
 * </ul>
 * Supported external document formats : PDF
 * <p>
 *
 * @author Estelle Idée (Geomatys)
 */
public final class RegistreDocumentsPane extends GridPane {

    @FXML
    private Button importDocButton;

    @FXML
    private Button deleteDocButton;

    @FXML
    private Button setFolderButton;

    @FXML
    private TreeTableView<File> tree;

    @FXML
    private Button addExtractionButton;

    @FXML
    private Button addFolderButton;

    @FXML
    private Button hideShowButton;

    @FXML
    private Button hideFileButton;

    @FXML
    private TreeTableColumn ui_name;
    @FXML
    private TreeTableColumn ui_modifDate;
    @FXML
    private TreeTableColumn ui_timestampDate;
    @FXML
    private TreeTableColumn ui_size;
    @FXML
    private TreeTableColumn ui_open;

    protected static final String BUTTON_STYLE = "buttonbar-button";

    private static final Image ADDF_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/add_folder.png"));
    private static final Image IMP_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/import.png"));
    private static final Image DEL_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/remove.png"));
    private static final Image SET_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/set.png"));
   private static final Image OP_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/ouvrir.png"));
    private static final Image HIDE_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/cocher-decocher.png"));
    private static final Image HI_HISH_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/afficher.png"));
    private static final Image SH_HISH_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/masquer.png"));
    private static final Image PUBLISH_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/publish.png"));

    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy");
    public static final String ROOT_FOLDER      = "symadrem.root.folder";

    // SIRS hidden file properties
    public static final String TIMESTAMP_DATE = "timestamp_date";
    public static final String LIBELLE          = "libelle";
    public static final String HIDDEN           = "hidden";

    private final FileTreeItem root;
    private final int ID_SIRS_IDX = 9;
    private final int START_DATE_IDX = 4;
    private final int END_DATE_IDX = 5;

    /**
     * Synthesis table colomn header that should match the @metaTemplatePrestationSyntheseTable.jrxml
     */
    public static final String[] HEADERS = new String[]{"Désignation", "Libellé", "Tronçon", "Type de prestation", "Date de début", "Date de fin", "Intervenant", "Nom auteur", "Commentaire", "Identifiant SIRS"};

    public RegistreDocumentsPane(final FileTreeItem root) {
        ArgumentChecks.ensureNonNull("root", root);
        SIRS.loadFXML(this);
        Injector.injectDependencies(this);
        this.root = root;

        getStylesheets().add(SIRS.CSS_PATH);

        addFolderButton.setGraphic(new ImageView(ADDF_BUTTON_IMAGE));
        importDocButton.setGraphic(new ImageView(IMP_BUTTON_IMAGE));
        deleteDocButton.setGraphic(new ImageView(DEL_BUTTON_IMAGE));
        setFolderButton.setGraphic(new ImageView(SET_BUTTON_IMAGE));
        addExtractionButton.setGraphic(new ImageView(PUBLISH_BUTTON_IMAGE));
        hideFileButton.setGraphic(new ImageView(HIDE_BUTTON_IMAGE));
        if (root.rootShowHiddenFile) {
            hideShowButton.setGraphic(new ImageView(SH_HISH_BUTTON_IMAGE));
        } else {
            hideShowButton.setGraphic(new ImageView(HI_HISH_BUTTON_IMAGE));
        }


        addFolderButton.setTooltip(new Tooltip("Ajouter un dossier"));
        importDocButton.setTooltip(new Tooltip("Importer un fichier"));
        deleteDocButton.setTooltip(new Tooltip("Supprimer un fichier"));
        setFolderButton.setTooltip(new Tooltip("Configurer le dossier racine"));
        addExtractionButton.setTooltip(new Tooltip("Extraire un registre"));
        hideShowButton.setTooltip(new Tooltip("Cacher/Afficher les fichiers cachés"));
        hideFileButton.setTooltip(new Tooltip("Cacher/Afficher le fichier sélectionné"));

        addFolderButton.getStyleClass().add(BUTTON_STYLE);
        importDocButton.getStyleClass().add(BUTTON_STYLE);
        deleteDocButton.getStyleClass().add(BUTTON_STYLE);
        setFolderButton.getStyleClass().add(BUTTON_STYLE);
        addExtractionButton.getStyleClass().add(BUTTON_STYLE);
        hideShowButton.getStyleClass().add(BUTTON_STYLE);
        hideFileButton.getStyleClass().add(BUTTON_STYLE);

        tree.getColumns().forEach(column -> {
            column.setEditable(false);
            if (column.equals(ui_name)) {
                column.setCellValueFactory(param -> {
                    final TreeItem item = param.getValue();
                    if (item != null) {
                        final File f = (File) item.getValue();
                        return new SimpleObjectProperty(f);
                    }
                    return null;
                });
                column.setCellFactory(param -> new FileNameCell());
            } else if (column.equals(ui_modifDate)) {
                column.setCellValueFactory((Callback) param -> {
                    final TreeItem item = ((CellDataFeatures) param).getValue();
                    if (item != null) {
                        final File f = (File) item.getValue();
                        synchronized (DATE_FORMATTER) {
                            return new SimpleStringProperty(DATE_FORMATTER.format(new Date(f.lastModified())));
                        }
                    }
                    return null;
                });
            } else if (column.equals(ui_timestampDate)) {
                column.setCellValueFactory((Callback) param -> {
                    final TreeItem item = ((CellDataFeatures) param).getValue();
                    if (item != null) {
                        final File f = (File) item.getValue();
                        synchronized (DATE_FORMATTER) {
                            return new SimpleObjectProperty<>(f);
                        }
                    }
                    return null;
                });
                column.setCellFactory(param -> new TimeStampCell());
            } else if (column.equals(ui_size)) {
                column.setCellValueFactory((Callback) param -> {
                    final FileTreeItem f = (FileTreeItem) ((CellDataFeatures) param).getValue();
                    if (f != null) {
                        return new SimpleStringProperty(f.getSize());
                    }
                    return null;
                });
            } else if (column.equals(ui_open)) {
                column.setCellValueFactory(param -> {
                    final FileTreeItem f = (FileTreeItem) param.getValue();
                    return new SimpleObjectProperty(f);
                });
                column.setCellFactory(param -> new OpenCell());
            }
        });

        tree.setShowRoot(false);
        tree.setRoot(root);

        final Preferences prefs = Preferences.userRoot().node(PluginReglementary.NODE_PREFERENCE_NAME);
        final String rootPath   = prefs.get(ROOT_FOLDER, null);

        final File rootDirectory = rootPath != null ? new File(rootPath) : null;

        if (rootDirectory != null && ReglementaryPropertiesFileUtilities.verifyDatabaseVersion(rootDirectory)) {
            root.setValue(rootDirectory);
            root.update(root.rootShowHiddenFile);
            updateDatabaseIdentifier(rootDirectory);

        } else {
            importDocButton.disableProperty().set(true);
            deleteDocButton.disableProperty().set(true);
            addFolderButton.disableProperty().set(true);
        }

        final BooleanBinding guestOrExtern = new BooleanBinding() {

            {
                bind(Injector.getSession().roleBinding());
            }

            @Override
            protected boolean computeValue() {
                final Role userRole = Injector.getSession().roleBinding().get();
                return Role.GUEST.equals(userRole)
                        || Role.EXTERN.equals(userRole);
            }
        };

        setFolderButton.disableProperty().bind(guestOrExtern);
        deleteDocButton.disableProperty().bind(guestOrExtern);
    }

    /**
     * Method used by action on button importDocButton.
     * <p>
     * Allows to import the timestamped PDF files, or other type of files.
     * <p>
     * User must first select a directory in the tree.
     * <p>
     * For timestamped "Tableau de synthèse", the user has the option to automatically update the Prestations' horodatage attributes with the input date and file.
     * @param event
     * @throws IOException if error when copying the files to the selected directory.
     */
    @FXML
    public void createAndShowImportDialog(ActionEvent event) throws IOException {
        final File directory = getSelectedFile();
        if (directory == null || !directory.isDirectory()) {
            showInformationDialog("Veuillez sélectionner un dossier dans la liste des dossiers et fichiers");
            return;
        }
        final Dialog dialog    = new Dialog();
        final DialogPane pane  = new DialogPane();
        final ImportPane ipane = new ImportPane();
        pane.setContent(ipane);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        pane.lookupButton(ButtonType.OK).disableProperty()
                        .bind(Bindings.createBooleanBinding(
                                () -> ipane.isSyntheseTable.isSelected() &&
                                        ipane.horodatageDate.getValue() == null,
                                ipane.isSyntheseTable.selectedProperty(),
                                ipane.horodatageDate.valueProperty()
                        ));
        ipane.horodatageDate.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !ipane.isSyntheseTable.isSelected() ,
                ipane.isSyntheseTable.selectedProperty()
        ));
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setTitle("Import de document");
        showImportDialog(dialog, directory);
    }

    /**
     * Show import dialog and deal with error : <br>
     * If @isSyntheseTable is selected, then the document must be a PDF -> show error message and reshow import dialog.
     * @param dialog the @{@link Dialog} to show.
     * @param directory the directory where to save the file.
     * @throws IOException if error while copying the file.
     */
    private void showImportDialog(final Dialog dialog, final File directory) throws IOException {
        final Optional opt = dialog.showAndWait();
        if (opt.isPresent() && ButtonType.OK.equals(opt.get())) {
            final ImportPane ipane = (ImportPane) dialog.getDialogPane().getContent();
            // Check if it is a Tableau de synthèse. If so, the timestamp date must be set.
            final boolean isSyntheseTable   = ipane.isSyntheseTable.isSelected();
            final LocalDate timeStampDate   = ipane.horodatageDate.getValue();
            final File f                    = new File(ipane.fileField.getText());
            final String fName              = f.getName();

            if (fName.isEmpty()) {
                showWarningDialog("Veuillez sélectionner un fichier.", null, 600, 175);
                showImportDialog(dialog, directory);
                return;
            }

            final File newFile  = new File(directory, fName);

            if (newFile.exists()) {
                showWarningDialog("Il existe déjà un fichier du même nom dans le répertoire sélectionné : \n\n" + newFile.getPath()
                        + "\nVeuillez en sélectionner un autre.", null, 600, 175);
                showImportDialog(dialog, directory);
                return;
            }

            if (isSyntheseTable) {
                if (fName == null || !fName.endsWith(".pdf")) {
                    showWarningDialog("Le tableau de synthèse doit être au format pdf.");
                    showImportDialog(dialog, directory);
                    return;
                }


                final Optional confirmOpt = showConfirmationDialog("Souhaitez-vous mettre automatiquement à jour les prestations du tableau?" +
                        "\n\nLes éléments suivants seront mis à jour pour chaque prestation :" +
                        "\n\n    - la date d'horodatage : " + timeStampDate + ";" +
                        "\n    - le statut d'horodatage : \"" + HorodatageReference.TIME_STAMPED + "\";" +
                        "\n    - le lien vers le Tableau de synthèse : " + newFile + "." +
                        "\n\nSélectionner 'Annuler' pour annuler l'importation du fichier.", null, 600, 300, true);

                boolean updatePrestations = false;
                if (confirmOpt.isPresent()) {
                    if (ButtonType.YES.equals(confirmOpt.get())) {
                        updatePrestations = true;
                    } else if (ButtonType.NO.equals(confirmOpt.get())) {
                        // do nothing
                    } else if (ButtonType.CANCEL.equals(confirmOpt.get())) {
                        // cancel the file import process.
                        return;
                    }
                }

                if (!extractElementsInFile(f, timeStampDate, updatePrestations)) return;
                setProperty(newFile, TIMESTAMP_DATE, timeStampDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }

            Files.copy(f.toPath(), newFile.toPath());

            // refresh tree
            update();
        }
    }


    /**
     * Extract text information from pdf file and update @{@link fr.sirs.core.model.Prestation}.
     *
     * @param pdf_filename pdf file to read.
     * @param timeStampDate timestamp date entered by user and used to update Prestations' horodatageDate if necessary.
     * @param updatePrestations whether to automatically update the Prestations' horodatageDate.
     * @return true if the document has been successfully extracted.<p>
     * false otherwise.
     */
    private boolean extractElementsInFile(final File pdf_filename, final LocalDate timeStampDate, final boolean updatePrestations) {
        ArgumentChecks.ensureNonNull("pdf_filename", pdf_filename);
        final List<Table> tables;
        try (final FileInputStream fileInputStream = new FileInputStream(pdf_filename)) {
            final PDFTableExtractor extractor = (new PDFTableExtractor())
                    .setSource(fileInputStream);

            // two first lines of the doc : title and @SystemeEndiguement libelle
            extractor.exceptLine(0, new int[]{0});
            extractor.exceptLine(0, new int[]{1});
            // exclude last line in each page -> corresponds to "Période : xx/xx/xxxx - xx/xx/xxxx"
            extractor.exceptLine(new int[]{-1});

            //begin parsing pdf file
            tables = extractor.extract();
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("File " + pdf_filename + " not found.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Error creating FileInputStream.", e);
        }

        // Happens when document protected by password and more than 3 attends.
        if (tables == null) {
            return false;
        }

        if (tables.isEmpty()) {
            showErrorDialog("Le document ne contient pas de donnée de tableau de synthèse.");
            return false;
        }

        // Check the first line corresponds to the Tableau de synthèse header.
        // If not -> the document is not a Tableau de synthèse generated by SIRS.
        List<TableCell> firstLineCells = new ArrayList<>();
        int headersLength = 0;
        boolean isSyntheseDoc = false;

        final List<TableRow> firstPageRows = tables.get(0).getRows();
        if (!firstPageRows.isEmpty()) {
            headersLength = HEADERS.length;
            firstLineCells = firstPageRows.get(0).getCells();
            if (headersLength == firstLineCells.size()) {
                boolean isSyntheseDocTmp = true;
                for (int i = 0; i < headersLength; i++) {
                    if (!HEADERS[i].equals(firstLineCells.get(i).getContent())) {
                        isSyntheseDocTmp = false;
                        break;
                    }
                }

                if (isSyntheseDocTmp) {
                    isSyntheseDoc = true;
                }
            }
        }

        if (!isSyntheseDoc) {
            final StringBuilder stgBuilder = new StringBuilder("|");
            for (String header : this.HEADERS) {
                stgBuilder.append(" " + header + " |");
            }
            showErrorDialog("Le document n'est pas reconnu comme un tableau de synthèse. Aucun tableau trouvé avec l'entête :" +
                            "\n" +
                            "\n----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------" +
                            "\n" + stgBuilder +
                            "\n----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------",
                    "Document non reconnu", 1050, 200);
            return false;
        }

        final PrestationRepository prestationRepo = Injector.getBean(PrestationRepository.class);
        // error message if prestations not found
        final StringBuilder errorText = new StringBuilder();
        int countErrorPrestations = 0;

        boolean updateAll = false;
        boolean doNotUpdateNext = false;
        ButtonType yesForAllButton = new ButtonType("Oui et suivants", ButtonBar.ButtonData.APPLY);
        ButtonType noForAllButton = new ButtonType("Non et suivants", ButtonBar.ButtonData.FINISH);
        final SirsStringConverter converter = new SirsStringConverter();
        final StringBuilder textUpdate = new StringBuilder();

        List<Prestation> prestationsToKeep = new ArrayList<>();

        for (Table table : tables) {
            final List<TableRow> rows = table.getRows();
            if (rows.isEmpty()) continue;

            // Removes the header row of each page's table.
            rows.remove(0);

            for (TableRow row : rows) {
                // Check that the last column exists and is not empty.
                // If not -> no identifiant SIRS, which means that the row is part of the previous row.
                final List<TableCell> cells = row.getCells();

                if (cells == null || cells.size() != headersLength) continue;

                final String idSirs = cells.get(ID_SIRS_IDX).getContent();

                if (idSirs.isEmpty()) continue;

                final Prestation prestation;
                try {
                    prestation = prestationRepo.get(idSirs);
                } catch (RuntimeException re) {
                    SIRS.LOGGER.warning("Error while retrieving the Prestation with id " + idSirs);
                    String libelle = cells.get(1).getContent();
                    if (libelle == null || "".equals(libelle.trim()) || "-".equals(libelle.trim())) {
                        libelle = "libellé non renseigné";
                    }
                    errorText.append("\n  -  ").append(libelle).append(" / ").append(idSirs);
                    countErrorPrestations++;
                    continue;
                }
                if (prestation == null) continue;

                if (updatePrestations && !doNotUpdateNext) {
                    // Collect start and end validity dates from the table.
                    final LocalDate startDate = extractedValidityDate(cells, START_DATE_IDX);
                    final LocalDate endDate = extractedValidityDate(cells, END_DATE_IDX);
                    final boolean isStartTimeStamp = endDate == null;

                    final LocalDate horodatageStartDate = prestation.getHorodatageStartDate();
                    if (!updateAll) {
                        // Check whether there is already a timestamp date for start validity.

                        String typeDate = null;
                        LocalDate horoDate = null;
                        LocalDate validityDate = null;
                        if (isStartTimeStamp) {
                            if (horodatageStartDate != null) {
                                typeDate = "début";
                                horoDate = horodatageStartDate;
                                validityDate = prestation.getHorodatageDateDebutStart();
                            }
                        } else {
                            // Check whether there is already a timestamp date for end validity.
                            final LocalDate horodatageEndDate = prestation.getHorodatageEndDate();
                            if (horodatageEndDate != null) {
                                typeDate = "fin";
                                horoDate = horodatageEndDate;
                                validityDate = prestation.getHorodatageDateFinEnd();
                            }
                        }
                        if (typeDate != null) {
                            final String horoDateStg = horoDate == null ? null : horoDate.format(JRXMLUtil.DD_MM_YYYY_FORMATER);
                            final String validityDateStg = validityDate == null ? null : validityDate.format(JRXMLUtil.DD_MM_YYYY_FORMATER);
                            final String message = "La prestation " + converter.toString(prestation) + " / " + prestation.getId() +
                                    " a déjà été horodatée le " + horoDateStg + " avec une date de " + typeDate + " le " + validityDateStg + "." +
                                    "\n\nSouhaitez-vous écraser les données d'horodatage avec les nouvelles informations ?";
                            final Optional opt = PropertiesFileUtilities.createAlert(Alert.AlertType.CONFIRMATION,
                                    "Date d'horodatage de " + typeDate + " existante", message, 700, 200,
                                    ButtonType.YES, ButtonType.NO, yesForAllButton, noForAllButton, ButtonType.CANCEL);

                            if (!opt.isPresent()) return false;

                            final Object result = opt.get();
                            if (ButtonType.NO.equals(result)) {
                                continue;
                            } else if (ButtonType.CANCEL.equals(result)) {
                                return false;
                            } else if (yesForAllButton.equals(result)) {
                                updateAll = true;
                            } else if (noForAllButton.equals(result)) {
                                doNotUpdateNext = true;
                                continue;
                            }
                        }
                    }

                    if (isStartTimeStamp) {
                        prestation.setHorodatageStartDate(timeStampDate);
                        prestation.setSyntheseTablePathStart(pdf_filename.toString());
                        prestation.setHorodatageDateDebutStart(startDate);
                    } else {
                        prestation.setHorodatageEndDate(timeStampDate);
                        prestation.setSyntheseTablePathEnd(pdf_filename.toString());
                        prestation.setHorodatageDateDebutEnd(startDate);
                        prestation.setHorodatageDateFinEnd(endDate);

                        // if the prestation has not been timestamped for its start, then the data are copied into the start timestamp attributs.
                        if (horodatageStartDate == null) {
                            prestation.setHorodatageStartDate(timeStampDate);
                            prestation.setSyntheseTablePathStart(pdf_filename.toString());
                            prestation.setHorodatageDateDebutStart(startDate);
                        }
                    }
                    prestation.setHorodatageStatusId(HorodatageReference.getRefTimeStampedStatus());
                    prestationsToKeep.add(prestation);
                }

            }
        }

        // Update prestations if user's choice.
        if (updatePrestations) {
            // Update prestations outside the above for loop in case of CANCEL or CLOSE button action.
            prestationsToKeep.forEach(prestation -> {
                        prestationRepo.update(prestation);
                        textUpdate.append("\n  -  ").append(converter.toString(prestation)).append(" / ").append(prestation.getId());
                    }
            );
            showInformationDialog(prestationsToKeep.size() + " prestations ont été mises à jour : \n" + textUpdate, "Succès", 800, 600);
        }

        if (countErrorPrestations == 0) return true;

        showWarningDialog("Erreur lors de la récupération de " + countErrorPrestations + " prestations : \n" + errorText, null, 600, 400);

        return true;
    }

    private LocalDate extractedValidityDate(List<TableCell> cells, int dateIdx) {
        final String dateString = cells.get(dateIdx).getContent();
        return dateString == null || dateString.trim().isEmpty() ? null : LocalDate.parse(dateString, JRXMLUtil.DD_MM_YYYY_FORMATER);
    }


    @FXML
    public void showRemoveDialog(ActionEvent event) throws IOException {
        final Optional opt      = showConfirmationDialog("Détruire le fichier/dossier dans le système de fichier ?" +
                "\nDans le cas d'un dossier, les sous-dossiers seront également supprimés." +
                "\nCette action est irréversible.", "Détruire document", 600, 200, false);

        if(opt.isPresent() && ButtonType.OK.equals(opt.get())){
            final File f = getSelectedFile();
            if (f != null) {
                if (f.isDirectory()) {
                    IOUtilities.deleteRecursively(f.toPath());
                    removeProperty(f, TIMESTAMP_DATE);
                } else {
                    f.delete();
                }
                removeProperties(f);

                // refresh tree
                update();
            } else {
                showErrorDialog("Vous devez sélectionner un dossier.");
            }
        }
    }

    @FXML
    public void setMainFolder(ActionEvent event) {
        final Dialog dialog    = new Dialog();
        final DialogPane pane  = new DialogPane();
        final MainFolderPane ipane = new MainFolderPane();
        pane.setContent(ipane);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setTitle("Emplacement du dossier racine");

        final Optional opt = dialog.showAndWait();
        if (opt.isPresent() && ButtonType.OK.equals(opt.get())) {
            File f = new File(ipane.rootFolderField.getText());
            final boolean isDirectory = f.isDirectory();
            final boolean isCorrectDatabaseVersion = verifyDatabaseVersion(f);
            if (isDirectory && isCorrectDatabaseVersion) {
                String rootPath = f.getPath();

                final Preferences prefs = Preferences.userRoot().node(PluginReglementary.NODE_PREFERENCE_NAME);
                prefs.put(ROOT_FOLDER, rootPath);
                importDocButton.disableProperty().set(false);
                deleteDocButton.disableProperty().unbind();
                deleteDocButton.disableProperty().set(false);
                addFolderButton.disableProperty().set(false);
                addExtractionButton.disableProperty().set(false);
                // refresh tree
                final File rootDirectory = new File(rootPath);
                updateFileSystem(rootDirectory);
                root.setValue(rootDirectory);
                root.update(root.rootShowHiddenFile);
                updateDatabaseIdentifier(rootDirectory);
            } else {
                if (!isDirectory)
                    SIRS.LOGGER.log(Level.WARNING, "Selected file is not a directory" );
                if (!isCorrectDatabaseVersion)
                    SIRS.LOGGER.log(Level.WARNING, "Database version is not correct." );
            }
        }
    }

    @FXML
    public void showAddFolderDialog(ActionEvent event) {
        final Dialog dialog    = new Dialog();
        final DialogPane pane  = new DialogPane();
        final NewFolderPane ipane = new NewFolderPane();
        pane.setContent(ipane);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setTitle("Création de dossier");

        final Optional opt = dialog.showAndWait();
        if(opt.isPresent() && ButtonType.OK.equals(opt.get())){
            String folderName  = ipane.folderNameField.getText();
            final File rootDir = root.getValue();

            if (rootDir == null) throw new IllegalStateException("rootDir shall be non null");

            switch (ipane.locCombo.getValue()) {
                case NewFolderPane.IN_CURRENT_FOLDER:
                    addToSelectedFolder(folderName);
                    break;
                case NewFolderPane.IN_SE_FOLDER:
                    addToModelFolder(rootDir, folderName, SE);
                    update();
                    break;
            }
        }
    }

    /**
     * Method used when action on button @addExtractionButton.
     * Opens the Extraction tab.
     * @param event
     */
    @FXML
    public void openExtractionTab(ActionEvent event) {
        final Parent parent = this.getParent().getParent();
        if (!(parent instanceof TabPane)) throw new IllegalStateException("Parent shall be a TabPane.");
        final TabPane result = (TabPane) parent;
        final Tab extraction = result.getTabs().filtered(tab -> RegistreTheme.EXTRACTION_TAB.equals(tab.getText())).stream().findFirst().orElseGet(null);
        if (extraction == null) {
            showErrorDialog("Erreur lors de l'ouverture de l'onglet l'extraction des registres.");
            return;
        }
        result.getSelectionModel().select(extraction);
    }

    @FXML
    public void hideFiles(ActionEvent event) {
        FileTreeItem item = (FileTreeItem) tree.getSelectionModel().getSelectedItem();
        if (item == null) return;
        item.hidden.setValue(!item.hidden.getValue());
        update();
    }

    @FXML
    public void hideShowFiles(ActionEvent event) {
        root.rootShowHiddenFile = !root.rootShowHiddenFile;
        if (root.rootShowHiddenFile) {
            hideShowButton.setGraphic(new ImageView(SH_HISH_BUTTON_IMAGE));
        } else {
            hideShowButton.setGraphic(new ImageView(HI_HISH_BUTTON_IMAGE));
        }
        update();
    }

    private File getSelectedFile() {
        TreeItem<File> item = tree.getSelectionModel().getSelectedItem();
        if (item != null) {
            return item.getValue();
        }
        return null;
    }

    private void update() {
        root.update(root.rootShowHiddenFile);
    }

    private void addToSelectedFolder(final String folderName) {
        File directory = getSelectedFile();
        if (directory != null && directory.isDirectory()) {
            final File newDir = new File(directory, folderName);
            newDir.mkdir();
            update();
        } else {
            showErrorDialog("Vous devez sélectionner un dossier.");
        }
    }


    private void addToModelFolder(final File rootDir, final String folderName, final String model) {
        for (File f : rootDir.listFiles()) {
            if (f.isDirectory()) {
                if (getIsModelFolder(f, model)) {
                    final File newDir = new File(f, folderName);
                    newDir.mkdir();
                    update();
                } else {
                    addToModelFolder(f, folderName, model);
                }
            }
        }
    }

    private static class OpenCell<S> extends TreeTableCell<S, FileTreeItem> {

        private final Button button = new Button();


        public OpenCell() {
            setGraphic(button);
            button.setGraphic(new ImageView(OP_BUTTON_IMAGE));
            button.getStyleClass().add(BUTTON_STYLE);
            button.disableProperty().bind(editingProperty());
            button.setOnAction(this::handle);

        }

        public void handle(ActionEvent event) {
            final FileTreeItem item = getItem();
            if (item != null && item.getValue() != null) {
                File file = item.getValue();

                SIRS.openFile(file).setOnSucceeded(evt -> {
                    if (!Boolean.TRUE.equals(evt.getSource().getValue())) {
                        Platform.runLater(() -> {
                            new Growl(Growl.Type.WARNING, "Impossible de trouver un programme pour ouvrir le document.").showAndFade();
                        });
                    }
                });
            }
        }



        @Override
        public void updateItem(FileTreeItem item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
                final File f = item.getValue();
                if (f != null && !f.isDirectory()) {
                    button.setTooltip(new Tooltip("Ouvrir le fichier"));
                    button.setVisible(true);
                } else {
                    button.setVisible(false);
                }
            } else {
                button.setVisible(false);
            }
        }
    }

    private static class FileNameCell<S> extends TreeTableCell<S, File> {

        private final Label label = new Label();

        public FileNameCell() {
            setGraphic(label);
        }

        @Override
        public void updateItem(File item, boolean empty) {
            super.updateItem(item, empty);
            label.opacityProperty().unbind();
            if (item != null) {
                final String name;
                if (getIsModelFolder(item)) {
                    name = getProperty(item, LIBELLE);
                } else {
                    name = item.getName();
                }
                label.setText(name);
                FileTreeItem fti = (FileTreeItem) getTreeTableRow().getTreeItem();
                if (fti != null) {
                    label.opacityProperty().bind(Bindings.when(fti.hidden).then(0.5).otherwise(1.0));
                }
            } else {
               label.setText("");
            }
        }
    }

    private static class TimeStampCell<S> extends TreeTableCell<S, File> {

        private final Label label = new Label();

        public TimeStampCell() {
            setGraphic(label);
        }

        @Override
        public void updateItem(File item, boolean empty) {
            super.updateItem(item, empty);
            label.opacityProperty().unbind();
            if (item != null) {
                final String name = getProperty(item, TIMESTAMP_DATE);
                label.setText(name);
                FileTreeItem fti = (FileTreeItem) getTreeTableRow().getTreeItem();
                if (fti != null) {
                    label.opacityProperty().bind(Bindings.when(fti.hidden).then(0.5).otherwise(1.0));
                }
            } else {
                label.setText("");
            }
        }
    }
}
