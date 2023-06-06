
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
import com.giaybac.traprange.entity.TableRow;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.component.PrestationRepository;
import fr.sirs.core.model.Prestation;
import fr.sirs.core.model.Role;
import fr.sirs.plugin.reglementaire.FileTreeItem;
import fr.sirs.plugin.reglementaire.PropertiesFileUtilities;
import fr.sirs.plugin.reglementaire.RegistreTheme;
import fr.sirs.ui.Growl;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.nio.IOUtilities;

import java.io.*;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static fr.sirs.plugin.reglementaire.PropertiesFileUtilities.*;

/**
 *
 * @author guilhem
 */
public class DocumentsPane extends GridPane {

    @FXML
    private Button importDocButton;

    @FXML
    private Button deleteDocButton;

    @FXML
    private Button setFolderButton;

    @FXML
    private TreeTableView<File> tree1;

    @FXML
    private Button addDocButton;

    @FXML
    private Button addFolderButton;

    @FXML
    private Button listButton;

    @FXML
    private Button hideShowButton;

    @FXML
    private Button hideFileButton;

    protected static final String BUTTON_STYLE = "buttonbar-button";

    private static final Image ADDF_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/add_folder.png"));
    private static final Image ADDD_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/add_doc.png"));
    private static final Image IMP_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/import.png"));
    private static final Image DEL_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/remove.png"));
    private static final Image SET_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/set.png"));
    private static final Image LIST_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/list.png"));
    private static final Image PUB_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/publish.png"), 17, 20, false, false);
    private static final Image OP_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/ouvrir.png"));
    private static final Image HIDE_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/cocher-decocher.png"));
    private static final Image HI_HISH_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/afficher.png"));
    private static final Image SH_HISH_BUTTON_IMAGE = new Image(RegistreTheme.class.getResourceAsStream("images/masquer.png"));

    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy");

    public static final String UNCLASSIFIED     = "Non classés";
    public static final String SAVE_FOLDER      = "Sauvegarde";
    public static final String DOCUMENT_FOLDER  = "Dossier d'ouvrage";
    public static final String ROOT_FOLDER      = "symadrem.root.folder";

    // SIRS hidden file properties
    public static final String TIMESTAMP_DATE = "timestamp_date";
    public static final String DO_INTEGRATED    = "do_integrated";
    public static final String LIBELLE          = "libelle";
    public static final String DYNAMIC          = "dynamic";
    public static final String MODELE           = "modele";
    public static final String HIDDEN           = "hidden";
    public static final String DATE_RANGE_MIN   = "dateRangeMin";
    public static final String DATE_RANGE_MAX   = "dateRangeMax";

    public static final String SE = "se";
    public static final String TR = "tr";
    public static final String DG = "dg";


    private static final Logger LOGGER = Logging.getLogger("fr.sirs");

    private final FileTreeItem root;

//    private final DynamicDocumentTheme dynDcTheme;

    public DocumentsPane(final FileTreeItem root) {
        //   , final DynamicDocumentTheme dynDcTheme) {
        SIRS.loadFXML(this);
        Injector.injectDependencies(this);
        this.root = root;
//        this.dynDcTheme = dynDcTheme;

        getStylesheets().add(SIRS.CSS_PATH);

        addFolderButton.setGraphic(new ImageView(ADDF_BUTTON_IMAGE));
        importDocButton.setGraphic(new ImageView(IMP_BUTTON_IMAGE));
        deleteDocButton.setGraphic(new ImageView(DEL_BUTTON_IMAGE));
        setFolderButton.setGraphic(new ImageView(SET_BUTTON_IMAGE));
        addDocButton.setGraphic(new ImageView(ADDD_BUTTON_IMAGE));
        listButton.setGraphic(new ImageView(LIST_BUTTON_IMAGE));
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
        addDocButton.setTooltip(new Tooltip("Ajouter un dossier dynamique"));
        listButton.setTooltip(new Tooltip("Exporter le sommaire"));
        hideShowButton.setTooltip(new Tooltip("Cacher/Afficher les fichiers cachés"));
        hideFileButton.setTooltip(new Tooltip("Cacher/Afficher le fichier sélectionné"));

        addFolderButton.getStyleClass().add(BUTTON_STYLE);
        importDocButton.getStyleClass().add(BUTTON_STYLE);
        deleteDocButton.getStyleClass().add(BUTTON_STYLE);
        setFolderButton.getStyleClass().add(BUTTON_STYLE);
        addDocButton.getStyleClass().add(BUTTON_STYLE);
        listButton.getStyleClass().add(BUTTON_STYLE);
        hideShowButton.getStyleClass().add(BUTTON_STYLE);
        hideFileButton.getStyleClass().add(BUTTON_STYLE);

        // Name column
        tree1.getColumns().get(0).setEditable(false);
        tree1.getColumns().get(0).setCellValueFactory((Callback) param -> {
            final TreeItem item = ((CellDataFeatures)param).getValue();
            if (item != null) {
                final File f = (File) item.getValue();
                return new SimpleObjectProperty(f);
            }
            return null;
        });
        tree1.getColumns().get(0).setCellFactory((Callback) param -> new FileNameCell());

        // Date column
        tree1.getColumns().get(1).setEditable(false);
        tree1.getColumns().get(1).setCellValueFactory((Callback) param -> {
            final TreeItem item = ((CellDataFeatures)param).getValue();
            if (item != null) {
                final File f = (File) item.getValue();
                synchronized (DATE_FORMATTER) {
                    return new SimpleStringProperty(DATE_FORMATTER.format(new Date(f.lastModified())));
                }
            }
            return null;
        });

        // Timestamp Date column
        tree1.getColumns().get(2).setEditable(false);
        tree1.getColumns().get(2).setCellValueFactory((Callback) param -> {
            final TreeItem item = ((CellDataFeatures)param).getValue();
            if (item != null) {
                final File f = (File) item.getValue();
                synchronized (DATE_FORMATTER) {
                    return new SimpleObjectProperty<>(f);
                }
            }
            return null;
        });
        tree1.getColumns().get(2).setCellFactory((Callback) param -> new TimeStampCell());

        // Size column
        tree1.getColumns().get(3).setEditable(false);
        tree1.getColumns().get(3).setCellValueFactory((Callback) param -> {
            final FileTreeItem f = (FileTreeItem) ((CellDataFeatures)param).getValue();
            if (f != null) {
                return new SimpleStringProperty(f.getSize());
            }
            return null;
        });


        // do integrated column
        tree1.getColumns().get(4).setCellValueFactory((Callback) param -> {
            final TreeItem item = ((CellDataFeatures)param).getValue();
            if (item != null) {
                final File f = (File) item.getValue();
                return new SimpleObjectProperty(f);
            }
            return null;
        });
        tree1.getColumns().get(4).setCellFactory((Callback) param -> new DOIntegatedCell());

        // publish column
        tree1.getColumns().get(5).setCellValueFactory((Callback) param -> {
            final FileTreeItem f = (FileTreeItem) ((CellDataFeatures)param).getValue();
            return new SimpleObjectProperty(f);
        });
        tree1.getColumns().get(5).setCellFactory((Callback) param -> new PublicationCell(root));

        // open column
        tree1.getColumns().get(6).setCellValueFactory((Callback) param -> {
            final FileTreeItem f = (FileTreeItem) ((CellDataFeatures)param).getValue();
            return new SimpleObjectProperty(f);
        });
        tree1.getColumns().get(6).setCellFactory((Callback) param -> new OpenCell());


        tree1.setShowRoot(false);
        tree1.setRoot(root);

        final Preferences prefs = Preferences.userRoot().node("ReglementaryPlugin");
        final String rootPath   = prefs.get(ROOT_FOLDER, null);

        if (rootPath != null && PropertiesFileUtilities.verifyDatabaseVersion(new File(rootPath))) {
            final File rootDirectory = new File(rootPath);
            root.setValue(rootDirectory);
            root.update(root.rootShowHiddenFile);
            updateDatabaseIdentifier(rootDirectory);

        } else {
            importDocButton.disableProperty().set(true);
            deleteDocButton.disableProperty().set(true);
            addDocButton.disableProperty().set(true);
            addFolderButton.disableProperty().set(true);
            listButton .disableProperty().set(true);
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

    @FXML
    public void createAndShowImportDialog(ActionEvent event) throws IOException {
        final File directory = getSelectedFile();
        if (directory == null || !directory.isDirectory()) {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText("Veuillez sélectionner un dossier dans la liste des dossiers et fichiers");
            alert.showAndWait();
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

            final File newFile              = new File(directory, f.getName());

            if (newFile.exists()) {
                final Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("Il existe déjà un fichier du même nom dans le répertoire sélectionné : \n\n" + newFile.getPath());
                alert.setHeaderText(null);
                alert.getDialogPane().setPrefWidth(600);
                alert.getDialogPane().setPrefHeight(175);
                alert.showAndWait();
                showImportDialog(dialog, directory);
                return;
            }

            if (isSyntheseTable) {
                final String tip = fName.substring(fName.lastIndexOf(".") + 1);
                if (!"pdf".equals(tip)) {
                    final Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setContentText("Le tableau de synthèse doit être au format pdf.");
                    alert.setHeaderText(null);
                    alert.showAndWait();
                    showImportDialog(dialog, directory);
                    return;
                }

                final Alert al = new Alert(Alert.AlertType.CONFIRMATION,
                        "Souhaitez-vous mettre automatiquement à jour la date d'horodatage des prestations ?" +
                                "\n\n" +
                                "Sélectionner 'Annuler' pour annuler l'importation du fichier.",
                        ButtonType.CANCEL, ButtonType.NO, ButtonType.YES);
                al.setHeaderText(null);
                al.getDialogPane().setPrefWidth(500);
                al.getDialogPane().setPrefHeight(150);

                boolean updatePrestationsDate = false;
                final Optional opt1 = al.showAndWait();
                if (opt1.isPresent()) {
                    if (ButtonType.YES.equals(opt1.get())) {
                        updatePrestationsDate = true;
                    } else if (ButtonType.NO.equals(opt1.get())) {
                        // do nothing
                    } else if (ButtonType.CANCEL.equals(opt1.get())) {
                        // cancel the file import process.
                        return;
                    }
                }

                extractElementsInFile(f, timeStampDate, updatePrestationsDate);
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
     * @param pdf_filename          pdf file to read.
     * @param timeStampDate
     * @param updatePrestationsDate
     */
    private void extractElementsInFile(final File pdf_filename, final LocalDate timeStampDate, final boolean updatePrestationsDate) {
        ArgumentChecks.ensureNonNull("pdf_filename", pdf_filename);

        final PDFTableExtractor extractor = (new PDFTableExtractor())
                .setSource(pdf_filename);

        final List<Integer[]> exceptLines = new ArrayList<>();
        // two first lines of the doc : title and @SystemeEndiguement libelle
        exceptLines.add(new Integer[]{0,0});
        exceptLines.add(new Integer[]{1,0});

        final List<Integer> lastPageExceptLines = new ArrayList<>();
        // exclude last line of the last page -> corresponds to "Période : xx/xx/xxxx - xx/xx/xxxx"
        lastPageExceptLines.add(-1);

        //except lines
        final List<Integer> exceptLineIdxes = new ArrayList<>();
        final Multimap<Integer, Integer> exceptLineInPages = LinkedListMultimap.create();
        for (Integer[] exceptLine : exceptLines) {
            if (exceptLine.length == 1) {
                exceptLineIdxes.add(exceptLine[0]);
            } else if (exceptLine.length == 2) {
                final int lineIdx = exceptLine[0];
                final int pageIdx = exceptLine[1];
                exceptLineInPages.put(pageIdx, lineIdx);
            }
        }
        if (!exceptLineIdxes.isEmpty()) {
            extractor.exceptLine(Ints.toArray(exceptLineIdxes));
        }
        if (!exceptLineInPages.isEmpty()) {
            for (int pageIdx : exceptLineInPages.keySet()) {
                extractor.exceptLine(pageIdx, Ints.toArray(exceptLineInPages.get(pageIdx)));
            }
        }

        //except lines in last page
        if (lastPageExceptLines != null) {
            extractor.exceptLineInLastPage(lastPageExceptLines);
        }
        //begin parsing pdf file
        final List<Table> tables = extractor.extract();
        // Removes the header row of each page's table.
        tables.forEach(table -> table.getRows().remove(0));

        final int columnsNo = tables.get(0).getRows().get(0).getCells().size();

        final Map<String, String> prestationsIdsAndLibelleWithError = new HashMap<>();
        for (Table table: tables) {
            for (TableRow row : table.getRows()) {
                // Check that the last column exists and is not empty.
                // If not -> no identifiant SIRS, which means that the row is part of the previous row.
                boolean oldRow = false;
                String idSirs = "";
                try {
                    idSirs = row.getCells().get(columnsNo - 1).getContent();
                    if (idSirs.isEmpty()) {
                        oldRow = true;
                    }
                } catch (IndexOutOfBoundsException ex) {
                    oldRow = true;
                } finally {
                    if (oldRow) continue;
                    final PrestationRepository prestationRepo = Injector.getBean(PrestationRepository.class);
                    Prestation prestation = null;
                    try {
                        prestation = prestationRepo.get(idSirs);
                    } catch (RuntimeException re) {
                        SIRS.LOGGER.warning("Error while retrieving the Prestation with id " + idSirs);
                        String libelle = row.getCells().get(1).getContent();
                        if ("-".equals(libelle.trim())) {
                            libelle = "libellé non renseigné";
                        }
                        prestationsIdsAndLibelleWithError.put(idSirs, libelle);
                        continue;
                    }
                    if (prestation == null) continue;

                    if (updatePrestationsDate) {
                        prestation.setHorodatageStatusId(RegistreTheme.refTimeStampedStatus);
                        prestation.setHorodatageDate(timeStampDate);
                        prestationRepo.update(prestation);
                    }
                }
            }
        }

        if (prestationsIdsAndLibelleWithError.isEmpty()) return;

        final StringBuilder text = new StringBuilder();
        prestationsIdsAndLibelleWithError.forEach((id, libelle) -> {
            text.append("\n  -  ").append(libelle).append(" / ").append(id);
        });
        final Alert alert = new Alert(Alert.AlertType.WARNING, "Erreur lors de la récupération de " + prestationsIdsAndLibelleWithError.size() + " prestations : \n" + text);
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setPrefHeight(400);
        alert.setResizable(true);
        alert.showAndWait();
    }


    @FXML
    public void showRemoveDialog(ActionEvent event) throws IOException {
        final Dialog dialog    = new Dialog();
        final DialogPane pane  = new DialogPane();
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setTitle("Détruire document");
        dialog.setContentText("Détruire le fichier/dossier dans le système de fichier ?");

        final Optional opt = dialog.showAndWait();
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
            if (f.isDirectory()) {
//                    && verifyDatabaseVersion(f)) {
                String rootPath = f.getPath();

                final Preferences prefs = Preferences.userRoot().node("ReglementaryPlugin");
                prefs.put(ROOT_FOLDER, rootPath);
                importDocButton.disableProperty().set(false);
                deleteDocButton.disableProperty().unbind();
                deleteDocButton.disableProperty().set(false);
                addDocButton.disableProperty().set(false);
                addFolderButton.disableProperty().set(false);
                listButton.disableProperty().set(false);
                // refresh tree
                final File rootDirectory = new File(rootPath);
                updateFileSystem(rootDirectory);
                root.setValue(rootDirectory);
                root.update(root.rootShowHiddenFile);
                updateDatabaseIdentifier(rootDirectory);
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
            switch (ipane.locCombo.getValue()) {
                case NewFolderPane.IN_CURRENT_FOLDER:
                    addToSelectedFolder(folderName);
                    break;
//                case NewFolderPane.IN_ALL_FOLDER:
//                    addToAllFolder(rootDir, folderName);
//                    update();
//                    break;
                case NewFolderPane.IN_SE_FOLDER:
                    addToModelFolder(rootDir, folderName, SE);
                    update();
                    break;
            }
        }
    }

    @FXML
    public void exportOdtSummary(ActionEvent event) {
//        final Dialog dialog    = new Dialog();
//        final DialogPane pane  = new DialogPane();
//        final SaveSummaryPane ipane = new SaveSummaryPane();
//        pane.setContent(ipane);
//        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
//        dialog.setDialogPane(pane);
//        dialog.setResizable(true);
//        dialog.setTitle("Exporter le sommaire");
//
//        final Optional opt = dialog.showAndWait();
//        if(opt.isPresent() && ButtonType.OK.equals(opt.get())){
//            File f = new File(ipane.newFileFIeld.getText());
//            LoadingPane.showDialog(ODTUtils.writeSummary(root, f));
//        }
    }

    @FXML
    public void openDynamicDocTab(ActionEvent event) {
//        Session session = Injector.getSession();
//        final Tab result = session.getOrCreateThemeTab(dynDcTheme);
//        session.getFrame().addTab(result);
    }

    @FXML
    public void hideFiles(ActionEvent event) {
//        FileTreeItem item = (FileTreeItem) tree1.getSelectionModel().getSelectedItem();
//        item.hidden.setValue(!item.hidden.getValue());
//        update();
    }

    @FXML
    public void hideShowFiles(ActionEvent event) {
//        root.rootShowHiddenFile = !root.rootShowHiddenFile;
//        if (root.rootShowHiddenFile) {
//            hideShowButton.setGraphic(new ImageView(SH_HISH_BUTTON_IMAGE));
//        } else {
//            hideShowButton.setGraphic(new ImageView(HI_HISH_BUTTON_IMAGE));
//        }
//        update();
    }

    private File getSelectedFile() {
        TreeItem<File> item = tree1.getSelectionModel().getSelectedItem();
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
//            if (getIsModelFolder(directory)) {
//                directory = new File(directory, DOCUMENT_FOLDER);
//                if (!directory.exists()) {
//                    directory.mkdir();
//                }
//            }
            final File newDir = new File(directory, folderName);
            newDir.mkdir();
            update();
        } else {
            showErrorDialog("Vous devez sélectionner un dossier.");
        }
    }

//    private void addToAllFolder(final File rootDir, final String folderName) {
//        for (File f : rootDir.listFiles()) {
//            if (f.isDirectory()) {
//                if (f.getName().equals(DOCUMENT_FOLDER)) {
//                    final File newDir = new File(f, folderName);
//                    if (!newDir.exists()) {
//                        newDir.mkdir();
//                    }
//                } else {
//                    addToAllFolder(f, folderName);
//                }
//            }
//        }
//    }

    private void addToModelFolder(final File rootDir, final String folderName, final String model) {
        for (File f : rootDir.listFiles()) {
            if (f.isDirectory()) {
                if (getIsModelFolder(f, model)) {
//                    final File docDir = new File(f, DOCUMENT_FOLDER);
//                    if (!docDir.exists()) {
//                        docDir.mkdir();
//                    }
//                    final File newDir = new File(docDir, folderName);
//                    if (!newDir.exists()) {
//                        newDir.mkdir();
//                    }
                    final File newDir = new File(f, folderName);
                    newDir.mkdir();
                    update();
                } else {
                    addToModelFolder(f, folderName, model);
                }
            }
        }
    }

    private static class DOIntegatedCell extends TreeTableCell {

        private final CheckBox box = new CheckBox();

        public DOIntegatedCell() {
            setGraphic(box);
            setAlignment(Pos.CENTER);
            box.disableProperty().bind(editingProperty());
            box.selectedProperty().addListener((observable, oldValue, newValue) -> {
                File f = (File) getItem();
                if (f != null) {
//                        setBooleanProperty(f, DO_INTEGRATED, newValue);
                }
            });
        }

        @Override
        public void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            File f = (File) item;
            if (f == null || f.isDirectory()) {
                box.setVisible(false);
            } else {
                box.setVisible(true);
//                box.setSelected(getBooleanProperty(f, DO_INTEGRATED));
            }
        }
    }

    private static class PublicationCell extends TreeTableCell {

        private final Button button = new Button();

        private final FileTreeItem root;

        public PublicationCell(final FileTreeItem root) {
            setGraphic(button);
            this.root = root;
            button.setGraphic(new ImageView(PUB_BUTTON_IMAGE));
            button.getStyleClass().add(BUTTON_STYLE);
            button.disableProperty().bind(editingProperty());
            button.setOnAction(this::handle);

        }

        public void handle(ActionEvent event) {
//            final FileTreeItem item = (FileTreeItem) getItem();
//            if (getBooleanProperty(item.getValue(), DYNAMIC)) {
//                regenerateDynamicDocument(item.getValue());
//            } else if (item.getValue().getName().equals(DOCUMENT_FOLDER)) {
//                printSynthesisDoc(item);
//            }
        }

        private void printSynthesisDoc(final FileTreeItem item) {
//            final Dialog dialog    = new Dialog();
//            final DialogPane pane  = new DialogPane();
//            final SaveSummaryPane ipane = new SaveSummaryPane();
//            pane.setContent(ipane);
//            pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
//            dialog.setDialogPane(pane);
//            dialog.setResizable(true);
//            dialog.setTitle("Exporter le dossier de synthèse");
//
//            final Optional opt = dialog.showAndWait();
//            if(opt.isPresent() && ButtonType.OK.equals(opt.get())){
//                File f = new File(ipane.newFileFIeld.getText());
//                LoadingPane.showDialog(ODTUtils.writeDoSynth(item, f));
//            }
        }

        private void regenerateDynamicDocument(final File item) {
//            final ModeleRapportRepository modelRepo = Injector.getBean(ModeleRapportRepository.class);
//            String modelId = getProperty(item, MODELE);
//            final String dateRangeMin = getProperty(item, DocumentsPane.DATE_RANGE_MIN);
//            final String dateRangeMax = getProperty(item, DocumentsPane.DATE_RANGE_MAX);
//
//            final NumberRange dateRange;
//            if(dateRangeMin.isEmpty() && dateRangeMax.isEmpty()) {
//                dateRange = null;
//            } else {
//                dateRange = NumberRange.create(dateRangeMin.isEmpty() ? 0 : Long.parseLong(dateRangeMin), true,
//                        dateRangeMax.isEmpty() ? Long.MAX_VALUE : Long.parseLong(dateRangeMax), true);
//            }
//            if (modelId != null && !modelId.isEmpty()) {
//                final ModeleRapport modele = modelRepo.get(modelId);
//                if (modele != null) {
//                    final Task<File> generator = ODTUtils.generateDoc(modele, getTronconList(), item, root.getLibelle(), dateRange);
//                    generator.setOnSucceeded(evt -> Platform.runLater(() -> root.update(false)));
//                    LoadingPane.showDialog(generator);
//                } else {
//                    showErrorDialog("Pas de modèle disponible pour le fichier: " + item.getName());
//                }
//            } else {
//                showErrorDialog("Impossible de résoudre l'identifiant du modèle pour le fichier: " + item.getName());
//            }
        }

//        private Collection<TronconDigue> getTronconList() {
//            final FileTreeItem item = (FileTreeItem) getItem();
//            final File modelFolder  = getModelFolder(item.getValue());
//            Collection<TronconDigue> elements = null;
////            if (getIsModelFolder(modelFolder, SE)) {
////                final SystemeEndiguementRepository sdRepo = (SystemeEndiguementRepository) Injector.getSession().getRepositoryForClass(SystemeEndiguement.class);
////                final SystemeEndiguement sd                = sdRepo.get(modelFolder.getName());
////                final DigueRepository digueRepo          = (DigueRepository) Injector.getSession().getRepositoryForClass(Digue.class);
////                final TronconDigueRepository tronconRepo = (TronconDigueRepository) Injector.getSession().getRepositoryForClass(TronconDigue.class);
////                final Set<TronconDigue> troncons         = new HashSet<>();
////                final List<Digue> digues                 = digueRepo.getBySystemeEndiguement(sd);
////                for(Digue digue : digues){
////                    troncons.addAll(tronconRepo.getByDigue(digue));
////                }
////                return troncons;
////            } else if (getIsModelFolder(modelFolder, TR)) {
////                final TronconDigueRepository tronconRepo = (TronconDigueRepository) Injector.getSession().getRepositoryForClass(TronconDigue.class);
////                return Collections.singleton(tronconRepo.get(modelFolder.getName()));
////            } else {
////                elements = new ArrayList<>();
////            }
//            return elements;
//        }

//        private File getModelFolder(File f) {
//            if (getIsModelFolder(f)) {
//                return f;
//            } else if (!f.getParentFile().equals(root.getValue())) {
//                return getModelFolder(f.getParentFile());
//            }
//            return null;
//        }

        @Override
        public void updateItem(Object item, boolean empty) {
//            super.updateItem(item, empty);
//            final FileTreeItem ft = (FileTreeItem) item;
//            if (ft != null) {
//                final File f          = ft.getValue();
//                if (f != null && (getBooleanProperty(f, DYNAMIC) || f.getName().equals(DOCUMENT_FOLDER))) {
//                    if (getBooleanProperty(f, DYNAMIC)) {
//                        button.setTooltip(new Tooltip("Mettre à jour le fichier dynamique"));
//                    } else if (f.getName().equals(DOCUMENT_FOLDER)) {
//                        button.setTooltip(new Tooltip("Exporter le dossier de synthèse"));
//                    }
//                    button.setVisible(true);
//                } else {
//                    button.setVisible(false);
//                }
//            } else {
//                button.setVisible(false);
//            }
        }
    }

    private static class OpenCell extends TreeTableCell {

        private final Button button = new Button();


        public OpenCell() {
            setGraphic(button);
            button.setGraphic(new ImageView(OP_BUTTON_IMAGE));
            button.getStyleClass().add(BUTTON_STYLE);
            button.disableProperty().bind(editingProperty());
            button.setOnAction(this::handle);

        }

        public void handle(ActionEvent event) {
            final FileTreeItem item = (FileTreeItem) getItem();
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
        public void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            final FileTreeItem ft = (FileTreeItem) item;
            if (ft != null) {
                final File f          = ft.getValue();
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

    private static class FileNameCell extends TreeTableCell {

        private final Label label = new Label();

        public FileNameCell() {
            setGraphic(label);
        }

        @Override
        public void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            File f = (File) item;
            label.opacityProperty().unbind();
            if (f != null) {
                final String name;
                if (getIsModelFolder(f)) {
                    name = getProperty(f, LIBELLE);
                } else {
                    name = f.getName();
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

    private static class TimeStampCell extends TreeTableCell {

        private final Label label = new Label();

        public TimeStampCell() {
            setGraphic(label);
        }

        @Override
        public void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            File f = (File) item;
            label.opacityProperty().unbind();
            if (f != null) {
                final String name = getProperty(f, TIMESTAMP_DATE);
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
