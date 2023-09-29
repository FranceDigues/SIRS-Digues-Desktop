package fr.sirs.plugins.synchro.ui;

import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.TronconDigueRepository;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.plugins.synchro.ui.database.PhotoDownload;
import fr.sirs.util.SirsStringConverter;
import fr.sirs.util.property.DocumentRoots;
import fr.sirs.util.property.SirsPreferences;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.internal.GeotkFX;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class PhotoDestination extends StackPane {

    @FXML
    private Label uiRootLabel;

    @FXML
    private Hyperlink uiChooseSubDir;

    @FXML
    private Label uiSubDirLabel;

    @FXML
    private ProgressIndicator uiDestSpaceProgress;

    @FXML
    private FXTronconPathSelector pathSelector; //Risque de fuite mémoire?;

    @FXML
    private Button uiImportBtn;

    /**
     * Property listened from {@link PhotoDownload} to detect the import request
     */
    private final BooleanProperty importProperty= new SimpleBooleanProperty(false);

    public BooleanProperty getImportProperty() {
        return importProperty;
    }

    /**
     * Destination root path, as it should be defined in {@link SirsPreferences.PROPERTIES#DOCUMENT_ROOT}.
     */
    private final SimpleObjectProperty<Path> rootDirProperty = new SimpleObjectProperty<>();

    /**
     * A sub-directory of {@link #rootDirProperty} to put imported photos into.
     */
    private final SimpleObjectProperty<Path> subDirProperty = new SimpleObjectProperty<>();

    private final ObjectBinding<Path> destination;

    final TronconDigueRepository tronconRepository;

    private final HashMap<String, String> idToDirectory = new HashMap<>();

    public PhotoDestination(final Session session) {
        SIRS.loadFXML(this);

        tronconRepository = (TronconDigueRepository) session.getRepositoryForClass(TronconDigue.class);

        final BooleanBinding noRootConfigured = rootDirProperty.isNull();
        uiChooseSubDir.disableProperty().bind(noRootConfigured);
        uiSubDirLabel.disableProperty().bind(noRootConfigured);

        destination = Bindings.createObjectBinding(() -> {
            final Path root = rootDirProperty.get();
            if (root == null)
                return null;
            final Path subDir = subDirProperty.get();
            if (subDir == null) {
                return root;
            }

            return root.resolve(subDir);

        }, rootDirProperty, subDirProperty);

        destination.addListener(this::destinationChanged);

        rootDirProperty.set(DocumentRoots.getPhotoRoot(null, false).orElse(null));
    }

    public void setPathSelector(){
        pathSelector.setPhotoDestination(this);
    }

    public Path getRoot() {
        return rootDirProperty.get();
    }

    public Path getSubDir() {
        return subDirProperty.get();
    }

    public ObjectBinding<Path> getDestination() {
        return destination;
    }

    @FXML
    void chooseSubDirectory(ActionEvent event) {
        final Path root = rootDirProperty.get();
        final Optional<File> chosen = chooseDirectory(root);
        if (chosen.isPresent()) {
            subDirProperty.set(root.relativize(chosen.get().toPath()));
        }
    }

    Optional<File> chooseDirectory(final Path fromPath) {
        ArgumentChecks.ensureNonNull("Root path", fromPath);

        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Répertoire destination");
        chooser.setInitialDirectory(fromPath.toFile());
        return Optional.ofNullable(chooser.showDialog(getScene().getWindow()));
    }

    Optional<Path> chooseDirectoryFromSubDir() {
        final Path subdir = getSubDir();
        final Path from = (subdir!= null)?subdir:getRoot(); //I assume getRoot() can't be null.
        final Optional<File> chosen = chooseDirectory(from);
        return chosen.map(file -> from.relativize(chosen.get().toPath()));
    }

    @FXML
    void configureRoot(ActionEvent event) {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choisir un répertoire racine : ");
        if (rootDirProperty.get() != null) {
            chooser.setInitialDirectory(rootDirProperty.get().toFile());
        }
        File chosen = chooser.showDialog(getScene().getWindow());
        if (chosen != null) {
            rootDirProperty.set(chosen.toPath().toAbsolutePath());
            DocumentRoots.setDefaultPhotoRoot(rootDirProperty.get());
        }
    }

    @FXML
    void importPhotos(ActionEvent event) {
        importProperty.set(!importProperty.get());
    }

    /**
     * Compute back destination usable space each time root or subdirectory change.
     * We do it for both elements, in case sub-directory is not on the same filestore.
     * Ex : root is /media, and sub-directory is myUsbKey/photos
     * @param obs
     * @param oldValue
     * @param newValue
     */
    private void destinationChanged(final ObservableValue<? extends Path> obs, final Path oldValue, final Path newValue) {
        if (newValue == null) {
            uiRootLabel.setText("N/A");
            uiSubDirLabel.setText("N/A");
            uiDestSpaceProgress.setProgress(0);
        } else {
            uiRootLabel.setText(rootDirProperty.get().toString());
            final Path subDir = subDirProperty.get();
            if (subDir == null || subDir.toString().isEmpty()) {
                uiSubDirLabel.setText("N/A");
            } else {
                uiSubDirLabel.setText(subDir.toString());
            }

            updateRelativePaths(oldValue, newValue);

            try {
                final FileStore fileStore = newValue.getFileSystem().provider().getFileStore(newValue);

                final long usableSpace = fileStore.getUsableSpace();
                final long totalSpace = fileStore.getTotalSpace();
                // HACK : Never set to 1 to avoid message print.
                uiDestSpaceProgress.setProgress(totalSpace <= 0 || usableSpace <= 0? 0.99999 : 1 - ((double)usableSpace / totalSpace));
            } catch (IOException e) {
                GeotkFX.newExceptionDialog("L'analyse du dossier destination a échoué. Veuillez choisir un autre dossier destination.", e);
            }
        }
    }

    void updateRelativePaths(final Path oldValue, final Path newValue) {
        ArgumentChecks.ensureNonNull("Path", newValue);
        if (oldValue == null) {
            SIRS.LOGGER.log(Level.INFO, "Null oldPath in PhotoDestination#updateRelativePaths; relative path is not updated.");
        } else {
            idToDirectory.replaceAll((key, path) -> getNewRelativePath(oldValue, newValue, path));
        }
        pathSelector.refresh();

    }

    private String getNewRelativePath(final Path oldRoot, final Path newRoot, final String oldRelative) {
        return newRoot.relativize(oldRoot.resolve(oldRelative)).toString();
    }


    public String getDirectoryNameFromTronconId(final Optional<String> tronconId) {

        if (tronconId.isPresent()) {
            final String id = tronconId.get();

            return idToDirectory.computeIfAbsent(id, key -> getPathForTronconId(id) );

        } else {
            return "";
        }
    }

    void setPathToTroncons(final Path path, final List<String> tronconsIds) {
        ArgumentChecks.ensureNonNull("Path", path);
        ArgumentChecks.ensureNonNull("List of selected tronçons", tronconsIds);

        tronconsIds.forEach( id -> idToDirectory.put(id, path.toString()));

    }

    private String getPathForTronconId(final String id) {
        return SirsStringConverter.getDesignation(tronconRepository.get(id));//Utiliser des Previews?
//                .replaceAll("\\s", "_"); //-> remplace les caractères 'blancs'

    }

    public void update(Set<String> tronconsIds) {
        if (tronconsIds != null) {
            pathSelector.updateTronconList(new ArrayList(tronconsIds));
        } else {
            pathSelector.updateTronconList(new ArrayList()); //On set une liste vide pour retirer les précédent tronçons s'il y en a.
        }
    }

}
