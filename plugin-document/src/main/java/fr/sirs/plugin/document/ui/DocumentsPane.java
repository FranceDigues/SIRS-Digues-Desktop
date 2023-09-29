
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
package fr.sirs.plugin.document.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.plugin.document.DocumentManagementTheme;
import fr.sirs.plugin.document.FileTreeItem;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import fr.sirs.core.component.SystemeEndiguementRepository;
import fr.sirs.core.component.DigueRepository;
import fr.sirs.core.component.ModeleRapportRepository;
import fr.sirs.core.component.TronconDigueRepository;
import fr.sirs.core.model.SystemeEndiguement;
import fr.sirs.core.model.Digue;
import fr.sirs.core.model.Role;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.core.model.report.ModeleRapport;
import fr.sirs.plugin.document.DynamicDocumentTheme;
import fr.sirs.plugin.document.ODTUtils;

import static fr.sirs.plugin.document.DocumentPropertiesFileUtilities.*;
import fr.sirs.ui.Growl;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import org.apache.sis.measure.NumberRange;
import org.geotoolkit.nio.IOUtilities;

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
    private TreeTableView<File> tree;

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

    @FXML
    private TreeTableColumn ui_name;
    @FXML
    private TreeTableColumn ui_modifDate;
    @FXML
    private TreeTableColumn ui_size;
    @FXML
    private TreeTableColumn ui_inventory;
    @FXML
    private TreeTableColumn ui_classement;
    @FXML
    private TreeTableColumn ui_doSynthesis;
    @FXML
    private TreeTableColumn ui_publish;
    @FXML
    private TreeTableColumn ui_open;

    protected static final String BUTTON_STYLE = "buttonbar-button";

    private static final Image ADDF_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/add_folder.png"));
    private static final Image ADDD_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/add_doc.png"));
    private static final Image IMP_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/import.png"));
    private static final Image DEL_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/remove.png"));
    private static final Image SET_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/set.png"));
    private static final Image LIST_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/list.png"));
    private static final Image PUB_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/publish2.png"), 17, 20, false, false);
    private static final Image OP_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/ouvrir2.png"));
    private static final Image HIDE_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/cocher-decocher.png"));
    private static final Image HI_HISH_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/afficher.png"));
    private static final Image SH_HISH_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/masquer.png"));

    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy");

    public static final String UNCLASSIFIED     = "Non classés";
    public static final String SAVE_FOLDER      = "Sauvegarde";
    public static final String DOCUMENT_FOLDER  = "Dossier d'ouvrage";
    public static final String ROOT_FOLDER      = "symadrem.root.foler";

    // SIRS hidden file properties
    public static final String INVENTORY_NUMBER = "inventory_number";
    public static final String CLASS_PLACE      = "class_place";
    public static final String DO_INTEGRATED    = "do_integrated";
    public static final String LIBELLE          = "libelle";
    public static final String DYNAMIC          = "dynamic";
    public static final String MODELE           = "modele";
    public static final String HIDDEN           = "hidden";
    public static final String DATE_RANGE_MIN   = "dateRangeMin";
    public static final String DATE_RANGE_MAX   = "dateRangeMax";

    private static final Logger LOGGER = Logging.getLogger("fr.sirs");

    private final FileTreeItem root;

    private final DynamicDocumentTheme dynDcTheme;

    public DocumentsPane(final FileTreeItem root, final DynamicDocumentTheme dynDcTheme) {
        ArgumentChecks.ensureNonNull("root", root);
        SIRS.loadFXML(this);
        Injector.injectDependencies(this);
        this.root = root;
        this.dynDcTheme = dynDcTheme;

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

        tree.getColumns().forEach(column -> {
            column.setEditable(false);
            if (column.equals(ui_name)) {
                column.setCellValueFactory(getSimpleObjectFileCallback());
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
            } else if (column.equals(ui_size)) {
                column.setCellValueFactory((Callback) param -> {
                    final FileTreeItem f = (FileTreeItem) ((CellDataFeatures) param).getValue();
                    if (f != null) {
                        return new SimpleStringProperty(f.getSize());
                    }
                    return null;
                });
            } else if (column.equals(ui_inventory)) {
                column.setCellValueFactory(getSimpleObjectFileCallback());
                column.setCellFactory(param -> new PropertyCell(INVENTORY_NUMBER));
            } else if (column.equals(ui_classement)) {
                column.setCellValueFactory(getSimpleObjectFileCallback());
                column.setCellFactory(param -> new PropertyCell(CLASS_PLACE));
            } else if (column.equals(ui_doSynthesis)) {
                column.setCellValueFactory(getSimpleObjectFileCallback());
                column.setCellFactory(param -> new DOIntegatedCell());
            } else if (column.equals(ui_publish)) {
                column.setCellValueFactory(param -> {
                    final FileTreeItem f = (FileTreeItem) param.getValue();
                    return new SimpleObjectProperty(f);
                });
                column.setCellFactory(param -> new PublicationCell(root));
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

        final Preferences prefs = Preferences.userRoot().node("DocumentPlugin");
        final String rootPath   = prefs.get(ROOT_FOLDER, null);

        if (rootPath != null && verifyDatabaseVersion(new File(rootPath))) {
            final File rootDirectory = new File(rootPath);
            root.setValue(rootDirectory);
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

    private static Callback getSimpleObjectFileCallback() {
        return param -> {
            final TreeItem item = ((CellDataFeatures) param).getValue();
            if (item != null) {
                final File f = (File) item.getValue();
                return new SimpleObjectProperty(f);
            }
            return null;
        };
    }

    @FXML
    public void showImportDialog(ActionEvent event) throws IOException {
        final Dialog dialog    = new Dialog();
        final DialogPane pane  = new DialogPane();
        final ImportPane ipane = new ImportPane();
        pane.setContent(ipane);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setTitle("Import de document");

        final Optional opt = dialog.showAndWait();
        if(opt.isPresent() && ButtonType.OK.equals(opt.get())){
            File f = new File(ipane.fileField.getText());
            final File directory = getSelectedFile();
            if (directory != null && directory.isDirectory()) {
                final File newFile = new File(directory, f.getName());
                Files.copy(f.toPath(), newFile.toPath());
                setProperty(newFile, INVENTORY_NUMBER, ipane.inventoryNumField.getText());
                setProperty(newFile, CLASS_PLACE,      ipane.classPlaceField.getText());

                // refresh tree
                update();
            }
        }
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
        if(opt.isPresent() && ButtonType.OK.equals(opt.get())){
            File f = new File(ipane.rootFolderField.getText());
            if (f.isDirectory() && verifyDatabaseVersion(f)) {
                String rootPath = f.getPath();

                final Preferences prefs = Preferences.userRoot().node("DocumentPlugin");
                prefs.put(ROOT_FOLDER, rootPath);
                importDocButton.disableProperty().set(false);
                deleteDocButton.disableProperty().unbind();
                deleteDocButton.disableProperty().set(false);
                addDocButton.disableProperty().set(false);
                addFolderButton.disableProperty().set(false);
                listButton .disableProperty().set(false);
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
                case NewFolderPane.IN_ALL_FOLDER:
                    addToAllFolder(rootDir, folderName);
                    update();
                    break;
                case NewFolderPane.IN_SE_FOLDER:
                    addToModelFolder(rootDir, folderName, SE);
                    update();
                    break;
                case NewFolderPane.IN_DG_FOLDER:
                    addToModelFolder(rootDir, folderName, DG);
                    update();
                    break;
                case NewFolderPane.IN_TR_FOLDER:
                    addToModelFolder(rootDir, folderName, TR);
                    update();
                    break;
            }
        }
    }

    @FXML
    public void exportOdtSummary(ActionEvent event) {
        final Dialog dialog    = new Dialog();
        final DialogPane pane  = new DialogPane();
        final SaveSummaryPane ipane = new SaveSummaryPane();
        pane.setContent(ipane);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setTitle("Exporter le sommaire");

        final Optional opt = dialog.showAndWait();
        if(opt.isPresent() && ButtonType.OK.equals(opt.get())){
            File f = new File(ipane.newFileFIeld.getText());
            DocumentLoadingPane.showDialog(ODTUtils.writeSummary(root, f));
        }
    }

    @FXML
    public void openDynamicDocTab(ActionEvent event) {
        Session session = Injector.getSession();
        final Tab result = session.getOrCreateThemeTab(dynDcTheme);
        session.getFrame().addTab(result);
    }

    @FXML
    public void hideFiles(ActionEvent event) {
        FileTreeItem item = (FileTreeItem) tree.getSelectionModel().getSelectedItem();
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
            if (getIsModelFolder(directory)) {
                directory = new File(directory, DOCUMENT_FOLDER);
                if (!directory.exists()) {
                    directory.mkdir();
                }
            }
            final File newDir = new File(directory, folderName);
            newDir.mkdir();
            update();
        } else {
            showErrorDialog("Vous devez sélectionner un dossier.");
        }
    }

    private void addToAllFolder(final File rootDir, final String folderName) {
        for (File f : rootDir.listFiles()) {
            if (f.isDirectory()) {
                if (f.getName().equals(DOCUMENT_FOLDER)) {
                    final File newDir = new File(f, folderName);
                    if (!newDir.exists()) {
                        newDir.mkdir();
                    }
                } else {
                    addToAllFolder(f, folderName);
                }
            }
        }
    }

    private void addToModelFolder(final File rootDir, final String folderName, final String model) {
        for (File f : rootDir.listFiles()) {
            if (f.isDirectory()) {
                if (getIsModelFolder(f, model)) {
                    final File docDir = new File(f, DOCUMENT_FOLDER);
                    if (!docDir.exists()) {
                        docDir.mkdir();
                    }
                    final File newDir = new File(docDir, folderName);
                    if (!newDir.exists()) {
                        newDir.mkdir();
                    }
                } else {
                    addToModelFolder(f, folderName, model);
                }
            }
        }
    }

    private static class DOIntegatedCell<S> extends TreeTableCell<S, File> {

        private final CheckBox box = new CheckBox();

        public DOIntegatedCell() {
            setGraphic(box);
            setAlignment(Pos.CENTER);
            box.disableProperty().bind(editingProperty());
            box.selectedProperty().addListener((observable, oldValue, newValue) -> {
                File f = getItem();
                if (f != null) {
                    setBooleanProperty(f, DO_INTEGRATED, newValue);
                }
            });
        }

        @Override
        public void updateItem(File item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || item.isDirectory()) {
                box.setVisible(false);
            } else {
                box.setVisible(true);
                box.setSelected(getBooleanProperty(item, DO_INTEGRATED));
            }
        }
    }

    private static class PropertyCell<S> extends TreeTableCell<S, File> {

        private TextField text = new TextField();

        private final String property;

        public PropertyCell(final String property) {
            this.property = property;
            setGraphic(text);
            text.disableProperty().bind(editingProperty());
            text.textProperty().addListener((observable, oldValue, newValue) -> {
                File f = getItem();
                if (f != null) {
                    setProperty(f, property, newValue);
                }
            });
        }

        @Override
        public void updateItem(File item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || item.isDirectory()) {
                text.setVisible(false);
            } else {
                text.setVisible(true);
                text.setText(getProperty(item, property));
            }
        }
    }

    private static class PublicationCell<S> extends TreeTableCell<S, FileTreeItem> {

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
            final FileTreeItem item = getItem();
            if (getBooleanProperty(item.getValue(), DYNAMIC)) {
                regenerateDynamicDocument(item.getValue());
            } else if (item.getValue().getName().equals(DOCUMENT_FOLDER)) {
                printSynthesisDoc(item);
            }
        }

        private void printSynthesisDoc(final FileTreeItem item) {
            final Dialog dialog    = new Dialog();
            final DialogPane pane  = new DialogPane();
            final SaveSummaryPane ipane = new SaveSummaryPane();
            pane.setContent(ipane);
            pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
            dialog.setDialogPane(pane);
            dialog.setResizable(true);
            dialog.setTitle("Exporter le dossier de synthèse");

            final Optional opt = dialog.showAndWait();
            if(opt.isPresent() && ButtonType.OK.equals(opt.get())){
                File f = new File(ipane.newFileFIeld.getText());
                DocumentLoadingPane.showDialog(ODTUtils.writeDoSynth(item, f));
            }
        }

        private void regenerateDynamicDocument(final File item) {
            final ModeleRapportRepository modelRepo = Injector.getBean(ModeleRapportRepository.class);
            String modelId = getProperty(item, MODELE);
            final String dateRangeMin = getProperty(item, DocumentsPane.DATE_RANGE_MIN);
            final String dateRangeMax = getProperty(item, DocumentsPane.DATE_RANGE_MAX);

            final NumberRange dateRange;
            if(dateRangeMin.isEmpty() && dateRangeMax.isEmpty()) {
                dateRange = null;
            } else {
                dateRange = NumberRange.create(dateRangeMin.isEmpty() ? 0 : Long.parseLong(dateRangeMin), true,
                        dateRangeMax.isEmpty() ? Long.MAX_VALUE : Long.parseLong(dateRangeMax), true);
            }
            if (modelId != null && !modelId.isEmpty()) {
                final ModeleRapport modele = modelRepo.get(modelId);
                if (modele != null) {
                    final Task<File> generator = ODTUtils.generateDoc(modele, getTronconList(), item, root.getLibelle(), dateRange);
                    generator.setOnSucceeded(evt -> Platform.runLater(() -> root.update(false)));
                    DocumentLoadingPane.showDialog(generator);
                } else {
                    showErrorDialog("Pas de modèle disponible pour le fichier: " + item.getName());
                }
            } else {
                showErrorDialog("Impossible de résoudre l'identifiant du modèle pour le fichier: " + item.getName());
            }
        }

        private Collection<TronconDigue> getTronconList() {
            final FileTreeItem item = getItem();
            final File modelFolder  = getModelFolder(item.getValue());
            Collection<TronconDigue> elements;
            if (getIsModelFolder(modelFolder, SE)) {
                final SystemeEndiguementRepository sdRepo = (SystemeEndiguementRepository) Injector.getSession().getRepositoryForClass(SystemeEndiguement.class);
                final SystemeEndiguement sd                = sdRepo.get(modelFolder.getName());
                final DigueRepository digueRepo          = (DigueRepository) Injector.getSession().getRepositoryForClass(Digue.class);
                final TronconDigueRepository tronconRepo = (TronconDigueRepository) Injector.getSession().getRepositoryForClass(TronconDigue.class);
                final Set<TronconDigue> troncons         = new HashSet<>();
                final List<Digue> digues                 = digueRepo.getBySystemeEndiguement(sd);
                for(Digue digue : digues){
                    troncons.addAll(tronconRepo.getByDigue(digue));
                }
                return troncons;
            } else if (getIsModelFolder(modelFolder, TR)) {
                final TronconDigueRepository tronconRepo = (TronconDigueRepository) Injector.getSession().getRepositoryForClass(TronconDigue.class);
                return Collections.singleton(tronconRepo.get(modelFolder.getName()));
            } else {
                elements = new ArrayList<>();
            }
            return elements;
        }

        private File getModelFolder(File f) {
            if (getIsModelFolder(f)) {
                return f;
            } else if (!f.getParentFile().equals(root.getValue())) {
                return getModelFolder(f.getParentFile());
            }
            return null;
        }

        @Override
        public void updateItem(FileTreeItem item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
                final File f          = item.getValue();
                if (f != null && (getBooleanProperty(f, DYNAMIC) || f.getName().equals(DOCUMENT_FOLDER))) {
                    if (getBooleanProperty(f, DYNAMIC)) {
                        button.setTooltip(new Tooltip("Mettre à jour le fichier dynamique"));
                    } else if (f.getName().equals(DOCUMENT_FOLDER)) {
                        button.setTooltip(new Tooltip("Exporter le dossier de synthèse"));
                    }
                    button.setVisible(true);
                } else {
                    button.setVisible(false);
                }
            } else {
                button.setVisible(false);
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
                final File f          = item.getValue();
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
}
