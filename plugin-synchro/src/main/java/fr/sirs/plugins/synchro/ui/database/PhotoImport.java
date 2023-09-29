package fr.sirs.plugins.synchro.ui.database;

import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.model.SIRSFileReference;
import fr.sirs.plugins.synchro.common.PhotoAndTroncon;
import fr.sirs.plugins.synchro.concurrent.AsyncPool;
import fr.sirs.plugins.synchro.ui.PhotoDestination;
import fr.sirs.plugins.synchro.ui.PrefixComposer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class PhotoImport extends ScrollPane {

    @FXML
    private GridPane uiImportGridPane;

    public PhotoImport(final Session session, final AsyncPool executor) {
        SIRS.loadFXML(this);

        this.setFitToWidth(true);

        final PhotoDestination photoDestination = new PhotoDestination(session);
        photoDestination.setPathSelector();
        final PrefixComposer prefixPane = new PrefixComposer();

        final ObjectBinding<Function<SIRSFileReference, String>> prefixBuilder = prefixPane.getPrefixBuilder();
        final ObjectBinding<Function<PhotoAndTroncon, Path>> destBuilder = Bindings.createObjectBinding(() -> {
            final Path p = photoDestination.getDestination().get();
            if (p == null)
                return null;
            Function<SIRSFileReference, String> prefixer = prefixBuilder.get();
            if (prefixer == null) {
                prefixer = file -> {
                    final String chemin = file.getChemin();
                    if (chemin == null) {
                        return file.getId();
                    }
                    return Paths.get(chemin).getFileName().toString();};
            }
            final Function<SIRSFileReference, String> finalPrefixer = prefixer;
            final Function<Optional<String>, String> intermediateDirectory = idTroncon -> photoDestination.getDirectoryNameFromTronconId(idTroncon);

            return file -> SIRS.concatenatePaths(p, intermediateDirectory.apply(file.getTronconId()), finalPrefixer.apply(file.getPhoto()));

        }, photoDestination.getDestination(), prefixBuilder);

        final PhotoDownload downloadPane = new PhotoDownload(executor, session, destBuilder, photoDestination.getImportProperty());

        downloadPane.getTronconIds().addListener((o, old, tronconsIds) -> photoDestination.update(tronconsIds));

        final PhotoPurge photoPurge = new PhotoPurge(executor, session);
        uiImportGridPane.add(downloadPane,     0, 0);
        uiImportGridPane.add(photoDestination, 1, 0);
        uiImportGridPane.add(prefixPane,       0, 1);
        uiImportGridPane.add(photoPurge,       1, 1);
    }
}
