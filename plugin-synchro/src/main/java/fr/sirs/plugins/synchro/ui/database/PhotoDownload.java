package fr.sirs.plugins.synchro.ui.database;

import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.plugins.synchro.attachment.AttachmentUtilities;
import fr.sirs.plugins.synchro.attachment.AttachmentsSizeAndTroncons;
import fr.sirs.plugins.synchro.common.DocumentUtilities;
import fr.sirs.plugins.synchro.common.PhotoAndTroncon;
import fr.sirs.plugins.synchro.common.PhotoFinder;
import fr.sirs.plugins.synchro.common.PhotosTronconWrapper;
import fr.sirs.plugins.synchro.concurrent.AsyncPool;
import fr.sirs.ui.Growl;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;
import org.geotoolkit.gui.javafx.util.TaskManager;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class PhotoDownload extends StackPane {

    @FXML
    private Button uiCancel;

    @FXML
    private VBox uiProgressPane;

    @FXML
    private VBox uiForm;

    @FXML
    private Label uiProgressLabel;

    @FXML
    private RadioButton uiDateTrigger;

    @FXML
    private DatePicker uiDate;

    @FXML
    private Button uiEstimate;

    @FXML
    private Label uiNb;

    @FXML
    private Label uiSize;

    private boolean estimated = false;

    private final ObjectProperty<Set<String>> tronconIds =new SimpleObjectProperty<>();

    private final Session session;

    private final ObservableValue<Function<PhotoAndTroncon, Path>> destinationProvider;

    private final AsyncPool pool;

    public PhotoDownload(final AsyncPool pool, final Session session,
            final ObservableValue<Function<PhotoAndTroncon, Path>> destinationProvider,
            final BooleanProperty importProperty) {
        ArgumentChecks.ensureNonNull("Asynchronous executor", pool);
        ArgumentChecks.ensureNonNull("Session", session);
        ArgumentChecks.ensureNonNull("Path provider", destinationProvider);
        ArgumentChecks.ensureNonNull("importProperty", importProperty);
        SIRS.loadFXML(this);
        this.pool = pool;
        this.session = session;
        this.destinationProvider = destinationProvider;

        uiForm.disableProperty().bind(uiProgressPane.visibleProperty());
        uiDate.disableProperty().bind(uiDateTrigger.selectedProperty().not());
        importProperty.addListener((o,oldV,newV)-> {if (oldV != null)  importPhotos();});
    }

    ObjectProperty<Set<String>> getTronconIds() {
        return tronconIds;
    }

    @FXML
    void estimate(ActionEvent event) {
        final Stream<PhotoAndTroncon> photos = getPhotographs();

        final CouchDbConnector connector = session.getConnector();

        final Task<AttachmentsSizeAndTroncons> t = AttachmentUtilities.estimateSizeAndTroncons(connector, photos);

        t.setOnFailed(ResultTaskUtilities.failedEstimation(t));
        t.setOnSucceeded(ResultTaskUtilities.succedSizeAndTronconsEstimation(t, uiNb, uiSize, tronconIds));

        uiProgressPane.visibleProperty().bind(t.runningProperty());
        uiCancel.setOnAction(evt -> t.cancel(true));

        TaskManager.INSTANCE.submit(t);
        estimated = true;
    }

    void importPhotos() {
        if (!estimated) {
            final Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer le Téléchargement ?\nSi vous souhaitez choisir au préalable le(s) répertoire(s) de destination, Cliquez d'abord sur le bouton \"Estimer\".", ButtonType.NO, ButtonType.YES);
            confirmation.setResizable(true);
            final Optional<ButtonType> res = confirmation.showAndWait();
            if (!(res.isPresent() && ButtonType.YES.equals(res.get()))) {
                return;
            }
        }

        final CouchDbConnector connector = session.getConnector();
        final Function<PhotoAndTroncon, Path> destinationFinder = destinationProvider.getValue();
        if (destinationFinder == null) {
            final Alert alert = new Alert(Alert.AlertType.ERROR, "Aucune destination spécifiée pour télécharger les données", ButtonType.OK);
            alert.setResizable(true);
            alert.show();
            return;
        }

        // Note : incremented in handleResult method.
        final LongProperty count = new SimpleLongProperty(0);
        final Function<PhotoAndTroncon, LongProperty> downloader = photo -> {
            final Path output = destinationFinder.apply(photo);
            try {
                Files.createDirectories(output.getParent());
                download(connector, photo.getPhoto(), output);
                return count;
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        };

        final Task<Void> download = pool.prepare(downloader)
                .setTarget(getPhotographs())
                .setWhenComplete(this::handleResult)
                .build();

        uiProgressPane.visibleProperty().bind(download.runningProperty());
        uiProgressLabel.textProperty().bind(Bindings.createStringBinding(() -> "Images téléchargées : "+count.get(), count));

        uiCancel.setOnAction(evt -> download.cancel(true));

        TaskManager.INSTANCE.submit(download);
    }

    private Stream<PhotoAndTroncon> getPhotographs() {
        Stream<PhotosTronconWrapper> distantPhotos = new PhotoFinder(session).get();

        if (uiDateTrigger.isSelected()) {
            final LocalDate since = uiDate.getValue().minusDays(1);
            distantPhotos = distantPhotos.map(wrapper -> wrapper.applyFilter(photo -> photo.getDate() != null && since.isBefore(photo.getDate())));
        }

        final CouchDbConnector connector = session.getConnector();
        return distantPhotos
                .flatMap(wrapper -> wrapper.getPhotosAndTronçons())
                // Skip photographs already downloaded
                .filter(photo -> !DocumentUtilities.isFileAvailable(photo.getPhoto()))
                // Find photographs uploaded in database.
                .filter(photo -> AttachmentUtilities.isAvailable(connector, photo.getPhoto()));
    }

    private void handleResult(final LongProperty count, final Throwable error) {
        if (error == null) {
            Platform.runLater(() -> count.set(count.get() + 1));
        } else if (error instanceof Error) {
            throw (Error) error;
        } else {
            SIRS.LOGGER.log(Level.WARNING, "Cannot download image", error);
            final String msg = String.format("Une image ne peut être téléchargée.%nCause : %s", error.getLocalizedMessage() == null? "Inconnue" : error.getLocalizedMessage());
            Platform.runLater(() -> new Growl(Growl.Type.ERROR, msg).showAndFade());
        }
    }

    private static void download(final CouchDbConnector connector, final AbstractPhoto photo, Path destination) throws IOException {
        AttachmentUtilities.download(connector, photo, destination);

        /* Once we've downloaded the image, we'll try to delete it from database.
         * Note : If this operation fails, we still consider operation as a
         * success, because the image is available locally.
         */
        try {
            AttachmentUtilities.delete(connector, photo);
        } catch (Exception e) {
            SIRS.LOGGER.log(Level.WARNING, "Cannot delete image from database", e);
        }
    }
}
