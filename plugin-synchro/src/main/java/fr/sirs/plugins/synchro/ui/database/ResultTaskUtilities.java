package fr.sirs.plugins.synchro.ui.database;

import fr.sirs.SIRS;
import fr.sirs.plugins.synchro.attachment.AttachmentsSizeAndTroncons;
import fr.sirs.ui.Growl;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
final class ResultTaskUtilities {


    final static EventHandler<WorkerStateEvent> failedEstimation(final Task t) {
        return evt -> {
            SIRS.LOGGER.log(Level.WARNING, "Estimation failed", t.getException());
            Platform.runLater(() -> new Growl(Growl.Type.ERROR, "Impossible d'estimer le volume des données à télécharger.").showAndFade());
                };
    }

    final static EventHandler<WorkerStateEvent> succedEstimation(final Task<Map.Entry<Long, Long>> t, final Label nbToSet, final Label sizeToSet) {
        return evt -> Platform.runLater(() -> {
            Map.Entry<Long, Long> value = t.getValue();
            final long nb = value.getKey();
            nbToSet.setText(nb < 0? "inconnu" : Long.toString(nb));
            sizeToSet.setText(SIRS.toReadableSize(value.getValue()));
        });
    }


    final static EventHandler<WorkerStateEvent> succedSizeAndTronconsEstimation(final Task<AttachmentsSizeAndTroncons> t, final Label nbToSet, final Label sizeToSet, final ObjectProperty<Set<String>> tronconsIds) {
        return evt -> Platform.runLater(() -> {
            AttachmentsSizeAndTroncons value = t.getValue();
            final long nb = value.getCount();
            nbToSet.setText(nb < 0? "inconnu" : Long.toString(nb));
            sizeToSet.setText(SIRS.toReadableSize(value.getSize()));
            tronconsIds.set(value.getTronconIds());
        });
    }
}
