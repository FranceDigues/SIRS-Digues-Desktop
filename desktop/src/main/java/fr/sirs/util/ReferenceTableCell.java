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
package fr.sirs.util;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.SirsCore;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.DocumentChangeEmiter;
import fr.sirs.core.component.DocumentListener;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Identifiable;
import fr.sirs.core.model.LeveProfilTravers;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.util.property.SirsPreferences;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

import javafx.beans.property.SimpleStringProperty;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.DocumentNotFoundException;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.util.FXTableCell;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * Display element attributes which are merely a relation to another element.
 * We find what is the other element to display a title for it.
 *
 * Note : implements {@link ChangeListener}, so we update graphic according to
 * the text in the cell.
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 * @param <S>
 */
public class ReferenceTableCell<S> extends FXTableCell<S, String> implements ChangeListener<String> {

    public static final Image ICON_LINK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_LINK, 16, FontAwesomeIcons.DEFAULT_COLOR), null);

    public static final String OBJECT_DELETED = "Objet supprimé !";

    /**
     * A cache whose key is the ID of the referenced object, and value is the label
     * to display for it.
     */
    private static final Map<String, StringProperty> CACHED_VALUES = new WeakHashMap<>();
    private static final LibelleUpdater LIBELLE_UPDATER = new LibelleUpdater();

    private static final ChangeListener CLEAR_CACHE_LISTENER = (c, o, n) -> {
        if (!CACHED_VALUES.isEmpty()) CACHED_VALUES.clear();
    };

    static {
        SirsPreferences.INSTANCE.showCasePropProperty().addListener(CLEAR_CACHE_LISTENER);
    }

    private final Class refClass;
    private ComboBox editor = new ComboBox();

    public ReferenceTableCell(final Class referenceClass) {
        ArgumentChecks.ensureNonNull("Reference class", referenceClass);
        refClass = referenceClass;
        setAlignment(Pos.CENTER);
        setContentDisplay(ContentDisplay.LEFT);
        setAlignment(Pos.CENTER_LEFT);

        textProperty().addListener(this);

        // Check if we're already listening on document update. If not, we register our listener.
        try {
            DocumentChangeEmiter docChange = Injector.getBean(DocumentChangeEmiter.class);
            if (!docChange.getListenersUnmodifiable().contains(LIBELLE_UPDATER)) {
                docChange.addListener(LIBELLE_UPDATER);
            }
        } catch (NoSuchBeanDefinitionException e) {
            SIRS.LOGGER.log(Level.FINE, "Cannot register a listener on CouchDB doc change, because the emitter is not present in spring context.", e);
        }
        /*
         * Listener to update displayed txt according to selected {@link SirsPreferences#getShowCase()}
         * Todo (try to move it at table or column level rather than at cell level to reduce the number of listeners)
         */
        final ChangeListener updateTextListener = (c, o, n) -> {
            editor = new ComboBox(); //Ensure comboBox is refreshed
            this.cancelEdit(); //Cancel also update item and ensure update even if currently editing
        };
        SirsPreferences.INSTANCE.showCasePropProperty().addListener(updateTextListener);
    }

    @Override
    public void terminateEdit() {
        setGraphic(null);
        Object newValue = editor.getValue();
        if (newValue == null) {
            commitEdit(null);
        }
        if (newValue instanceof Preview) {
            commitEdit(((Preview) newValue).getElementId());
        } else if (newValue instanceof Identifiable) {
            commitEdit(((Identifiable) newValue).getId());
        } else {
            cancelEdit();
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        updateItem(getItem(), false);
    }

    @Override
    public void startEdit() {
        // TODO : make search asynchronous ?
        final ObservableList items;
        final Previews previews = Injector.getSession().getPreviews();
        // First, we'll search for a set of possible values.
        if (SystemeReperageBorne.class.isAssignableFrom(refClass) || BorneDigue.class.isAssignableFrom(refClass)) {
            items = findBornes();
        } else {
            items = SIRS.observableList(previews.getByClass(refClass)).sorted();
        }
        if (items != null && !items.isEmpty()) {

            // Analyze current item to determine default selection
            final String elementId = getItem();
            Object selected = null;
            if (elementId != null && !elementId.isEmpty()) {
                try {
                    if (SystemeReperageBorne.class.isAssignableFrom(refClass) || BorneDigue.class.isAssignableFrom(refClass)) {
                        selected = Injector.getSession().getElement(elementId).orElse(null);
                    } else {
                        selected = previews.get(elementId);
                    }
                } catch (DocumentNotFoundException e) {
                    SirsCore.LOGGER.fine("No document found for id " + elementId);
                }
            }

            SIRS.initCombo(editor, items, selected);
            super.startEdit();
            textProperty().unbind();
            setText(null);
            setGraphic(editor);
            editor.requestFocus();
        } else {
            cancelEdit();
        }
    }

    private ObservableList<BorneDigue> findBornes() {
        SystemeReperage sr = findSystemeReperage();
        AbstractSIRSRepository<BorneDigue> repo = Injector.getSession().getRepositoryForClass(BorneDigue.class);
        if (sr == null) {
            return SIRS.observableList(repo.getAll());
        } else {
            final Set<String> borneIds = new HashSet<>(sr.systemeReperageBornes.size());
            sr.systemeReperageBornes.stream().forEach(srb -> borneIds.add(srb.getBorneId()));
            return SIRS.observableList(repo.get(borneIds.toArray(new String[0])));
        }
    }

    private SystemeReperage findSystemeReperage() {
        SystemeReperage toUse = null;
        String item = getItem();
        final Element element = Injector.getSession().getElement(item).orElse(null);
        if (element instanceof SystemeReperageBorne) {
            Element parent = ((SystemeReperageBorne) element).getParent();
            if (parent instanceof SystemeReperage) {
                toUse = (SystemeReperage) parent;
            }
        }

        // Cannot determine SR from cell value. We'll try from row value
        if (toUse == null) {
            if (getTableRow() != null && (getTableRow().getItem() instanceof Positionable)) {
                final Positionable tmpPos = (Positionable) getTableRow().getItem();
                final String srid = tmpPos.getSystemeRepId();
                if (srid != null && !srid.isEmpty()) {
                    try {
                        toUse = Injector.getSession().getRepositoryForClass(SystemeReperage.class).get(srid);
                    } catch (DocumentNotFoundException e) {
                        SirsCore.LOGGER.fine("No SystemeReperage for id ".concat(srid));
                    }
                }
            }
        }
        return toUse;
    }

    @Override
    protected void updateItem(final String item, final boolean empty) {
        StringProperty text;
        if (empty || item == null || item.isEmpty()) {
            text = null;
        } else {
            text = CACHED_VALUES.get(item);
            // L'entrée nest pas dans le cache, on va chercher l'info en base.
            if (text == null) {
                // On essaye de récupérer le preview label, car l'objet en entrée doit être un ID.
                try {
                    // Cas spécifique aux références vers les levés de profil en travers (depuis les tableaux de LevePositionProfilTravers) : on veut afficher la date (SYM-1587)
                    if (LeveProfilTravers.class.equals(refClass)) {
                        final LeveProfilTravers lpt = Injector.getSession().getRepositoryForClass(LeveProfilTravers.class).get(item);
                        text = new SimpleStringProperty(lpt.getDateLeve() == null ? "" : lpt.getDateLeve().toString());
                        CACHED_VALUES.put(item, text);
                    } else {
                        final Preview tmpPreview = getPreview(item);
                        if (tmpPreview != null) {
                            text = tmpPreview.libelleProperty();
                            text.setValue(new SirsStringConverter().toString(tmpPreview));
                            if (text.get() == null) {
                                // Si l'objet pointé n'a pas de libellé, une valeur par défaut est nécessaire
                                // afin de montrer qu'une liaison existe réellement.
                                text.setValue("Objet sans libellé");
                            }
                            CACHED_VALUES.put(item, text);
                        }
                    }
                } catch (DocumentNotFoundException ex) {
                    // La preview n'a pas pu être trouvée, ce qui indique que l'objet pointé a été supprimé.
                    text = new SimpleStringProperty(OBJECT_DELETED);
                }
            }
        }

        super.updateItem(item, empty);

        if (text == null) {
            textProperty().unbind();
            setText(null);
        } else {
            // Ensure we've got only one listener registered.
            textProperty().bind(text);
        }
    }

    private static Preview getPreview(final String elementId) throws DocumentNotFoundException {
        return Injector.getSession().getPreviews().get(elementId);
    }

    @Override
    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (newValue == null) {
            setGraphic(null);
        } else {
            setGraphic(new ImageView(ICON_LINK));
            if (OBJECT_DELETED.equalsIgnoreCase(newValue)) {
                setTextFill(Color.RED);
            } else {
                setTextFill(Color.BLACK);
            }
        }
    }

    /**
     * Listen on changes in database, to update cell label when needed.
     */
    private static class LibelleUpdater implements DocumentListener {

        @Override
        public void documentCreated(Map<Class, List<Element>> added) {}

        @Override
        public void documentChanged(Map<Class, List<Element>> changed) {
            if (CACHED_VALUES.size() < 1) return;
            for (final List<Element> elements : changed.values()) {
                for (final Element e : elements) {
                    final StringProperty tmpProp = CACHED_VALUES.get(e.getId());
                    if (tmpProp != null) {
                        // We have to retrieve preview, because we cannot just
                        // use SirsStringConverter or libelle.
                        Preview preview = getPreview(e.getId());
                        if (preview != null) {
                            final Runnable fxUpdate = () -> tmpProp.set(preview.getLibelle());
                            if (Platform.isFxApplicationThread()) {
                                fxUpdate.run();
                            } else {
                                Platform.runLater(fxUpdate);
                            }
                        } // else... can it really happen ?
                    }
                }
            }
        }

        @Override
        public void documentDeleted(Set<String> deleted) {
            if (CACHED_VALUES.size() < 1)
                return;
            for (final String id : deleted) {
                final StringProperty tmpProp = CACHED_VALUES.get(id);
                if (tmpProp != null) {
                    final Runnable fxUpdate = () -> tmpProp.set(OBJECT_DELETED);
                    if (Platform.isFxApplicationThread()) {
                        fxUpdate.run();
                    } else {
                        Platform.runLater(fxUpdate);
                    }
                }
            }
        }
    }
}
