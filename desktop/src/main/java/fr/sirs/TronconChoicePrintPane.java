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
package fr.sirs;

import com.vividsolutions.jts.geom.Point;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.SirsCore;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.model.*;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.ui.Growl;
import fr.sirs.util.SirsStringConverter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import fr.sirs.util.property.SirsPreferences;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import org.ektorp.DocumentNotFoundException;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.gui.javafx.util.FXNumberCell;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.referencing.LinearReferencing;

/**
 * Classe abstraite de factorisation des fonctionnalités communes aux panneaux
 * d'impression permettant de choisir des tronçons et des PRs pour restreindre
 * les objets à inclure dans le document à imprimer.
 *
 * @author Samuel Andrés (Geomatys)
 */
public abstract class TronconChoicePrintPane extends BorderPane {

    @FXML protected Tab uiTronconChoice;
    @FXML protected FXPrestationPredicater uiPrestationPredicater;

    @FXML protected CheckBox uiOptionExcludeValid;
    @FXML protected CheckBox uiOptionExcludeInvalid;

    // Garde en cache les PRs de début et de fin de sections de tronçons imprimables (ajustables pour limiter l'impression à des parties de tronçons seulement).
    protected final Map<String, ObjectProperty<Number>[]> ajustedPrsByTronconId = new HashMap<>();

    // Garde en cache les PRs de début et de fin des tronçons (non ajustables, mais gardés à titre indicatif).
    protected final Map<String, ObjectProperty<Number>[]> originalPrsByTronconId = new HashMap<>();

    protected final TronconChoicePojoTable tronconsTable = new TronconChoicePojoTable();

    public TronconChoicePrintPane(final Class forBundle) {
        SIRS.loadFXML(this, forBundle);
        final Session session = Injector.getSession();

        final List<TronconDigue> byClass = session.getRepositoryForClass(TronconDigue.class).getAll();
        // HACK-REDMINE-4408 : hide archived troncons from selection lists
        final List<TronconDigue> list;
        if (SirsPreferences.getHideArchivedProperty()) {
            final Predicate<TronconDigue> isNotArchived = tl -> tl.getDate_fin() == null || tl.getDate_fin().isAfter(LocalDate.now());
            list = byClass.stream().filter(isNotArchived).collect(Collectors.toList());
        } else {
            list = byClass;
        }
        tronconsTable.setTableItems(()-> (ObservableList) SIRS.observableList(list));
        tronconsTable.commentAndPhotoProperty().set(false);
        uiTronconChoice.setContent(tronconsTable);
        tronconsTable.getSelectedItems().addListener((ListChangeListener.Change<? extends Element> ch) -> filterPrestations(ch));

        uiOptionExcludeValid.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) uiOptionExcludeInvalid.selectedProperty().setValue(false);
        });
        uiOptionExcludeInvalid.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) uiOptionExcludeValid.selectedProperty().setValue(false);
        });
    }

    private void filterPrestations(ListChangeListener.Change<? extends Element> ch) {
        uiPrestationPredicater.updateList(ch);
    }

    protected class TronconChoicePojoTable extends PojoTable {

        public TronconChoicePojoTable() {
            super(TronconDigue.class, "Tronçons", null, false); //le dernier input 'false" permet de ne pas appliquer les préférences utilisateur depuis le constructeur parent.
            getColumns().remove(editCol);
            editableProperty.set(false);
            createNewProperty.set(false);
            fichableProperty.set(false);
            uiAdd.setVisible(false);
            uiFicheMode.setVisible(false);
            uiDelete.setVisible(false);

            final TableView table = getTable();
            table.editableProperty().unbind();
            table.setEditable(true);
            for(final Object o : table.getColumns()){
                if(o instanceof TableColumn){
                    final TableColumn c = (TableColumn)o;
                    c.editableProperty().unbind();
                    c.setEditable(false);
                }
            }

            getColumns().add(new EditablePRColumn("PR début sélectionné", ExtremiteTroncon.DEBUT));
            getColumns().add(new EditablePRColumn("PR fin sélectionné", ExtremiteTroncon.FIN));
            getColumns().add(new OriginalPRColumn("PR minimum existant", ExtremiteTroncon.DEBUT));
            getColumns().add(new OriginalPRColumn("PR maximum existant", ExtremiteTroncon.FIN));

            // application des préférence (après la suppression de la colonne 'editcol'
            applyPreferences();
            listenPreferences();
        }
    }

    private enum ExtremiteTroncon {DEBUT, FIN}

    protected abstract InvalidationListener getParameterListener();

    private class EditablePRColumn extends TableColumn {

        public EditablePRColumn(final String text, final ExtremiteTroncon extremite){
            super(text);
            setEditable(true);

            setCellFactory(new Callback<TableColumn<TronconDigue, Number>, TableCell<TronconDigue, Number>>() {

                @Override
                public TableCell<TronconDigue, Number> call(TableColumn<TronconDigue, Number> param) {
                    final TableCell<TronconDigue, Number> tableCell = new PREditCell(Float.class, getParameterListener());
                    tableCell.setEditable(true);
                    return tableCell;
                }
            });

            setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TronconDigue, Number>, ObservableValue<Number>>() {

                @Override
                public ObservableValue<Number> call(TableColumn.CellDataFeatures<TronconDigue, Number> param) {

                    if(param!=null && param.getValue()!=null){
                        final TronconDigue troncon = param.getValue();

                        if(troncon.getSystemeRepDefautId()!=null
                                && troncon.getGeometry()!=null
                                && troncon.getId()!=null){

                            return getOrComputePR(troncon, extremite, ajustedPrsByTronconId);
                        }
                    }
                    return null;
                }
            });
        }
    }

    private class PREditCell<TronconDigue, T> extends FXNumberCell<T> {

        private PREditCell(final Class clazz, final InvalidationListener invalidListener) {
            super(clazz);
            field.valueProperty().addListener(invalidListener); //Or not WeakListener?
            field.valueProperty().addListener(n -> commitEdit(field.valueProperty().get()));
        }
    }

    private class OriginalPRColumn extends TableColumn {

        public OriginalPRColumn(final String text, final ExtremiteTroncon extremite){
            super(text);

            setEditable(false);

            setCellFactory(new Callback<TableColumn<TronconDigue, Number>, TableCell<TronconDigue, Number>>() {

                @Override
                public TableCell<TronconDigue, Number> call(TableColumn<TronconDigue, Number> param) {
                    final TableCell<TronconDigue, Number> tableCell = new FXNumberCell(Float.class);
                    tableCell.setEditable(false);
                    return tableCell;
                }
            });

            setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TronconDigue, Number>, ObservableValue<Number>>() {

                @Override
                public ObservableValue<Number> call(TableColumn.CellDataFeatures<TronconDigue, Number> param) {

                    if(param!=null && param.getValue()!=null){
                        final TronconDigue troncon = param.getValue();

                        if(troncon.getSystemeRepDefautId()!=null
                                && troncon.getGeometry()!=null
                                && troncon.getId()!=null){

                            return getOrComputePR(troncon, extremite, originalPrsByTronconId);
                        }
                    }
                    return null;
                }
            });
        }
    }

    private ObjectProperty<Number> getOrComputePR(final TronconDigue troncon, final ExtremiteTroncon extremite, final Map<String, ObjectProperty<Number>[]> prCache){

        // Si on est à la fin du tronçon le pr se trouve à l'index 1 du tableau, sinon, par défaut on se place au début et on met l'index à 0
        final int index = extremite==ExtremiteTroncon.FIN ? 1:0;
        final ObjectProperty<Number> prProperty;

        if(prCache.get(troncon.getId())==null)
            prCache.put(troncon.getId(), new ObjectProperty[2]);

        // Si le PR de l'extrémité voulue du tronçon n'a pas encore été calculé.
        if (prCache.get(troncon.getId())[index]==null) {
            prProperty = new SimpleObjectProperty<>();

            TaskManager.INSTANCE.submit(new computePRTask(troncon, extremite, prProperty));

            prCache.get(troncon.getId())[index] = prProperty;
        }
        else {
            prProperty = prCache.get(troncon.getId())[index];
        }
        return prProperty;
    }

    /**
     * Tâche asynchrone pour le calcul des valeurs de PR (début et fin) calculées.
     */
    private final class computePRTask extends Task<Number> {

        private final TronconDigue troncon;
        private final ExtremiteTroncon extremite;

        final ObjectProperty<Number> prProperty;

        computePRTask(final TronconDigue troncon, final ExtremiteTroncon extremite, final ObjectProperty<Number> prProperty ){
            this.troncon=troncon;
            this.extremite=extremite;
            this.prProperty=prProperty;

        }

        @Override
        protected Number call() throws Exception {

                final SystemeReperage sr = Injector.getSession().getRepositoryForClass(SystemeReperage.class).get(troncon.getSystemeRepDefautId());
                final LinearReferencing.SegmentInfo[] tronconSegments = LinearReferencingUtilities.buildSegments(LinearReferencing.asLineString(troncon.getGeometry()));

                final Point point;
                switch (extremite) {
                    case FIN:
                        final LinearReferencing.SegmentInfo lastSegment = tronconSegments[tronconSegments.length - 1];
                        point = GO2Utilities.JTS_FACTORY.createPoint(lastSegment.getPoint(lastSegment.length, 0));
                        break;
                    case DEBUT:
                    default:
                        point = GO2Utilities.JTS_FACTORY.createPoint(tronconSegments[0].getPoint(0, 0));
                        break;
                }
                return TronconUtils.computePR(tronconSegments, sr, point, Injector.getSession().getRepositoryForClass(BorneDigue.class));
        }

        @Override
        protected void succeeded() {
            prProperty.set(getValue());
            super.succeeded();
        }

        @Override
        protected void failed() {
            SIRS.LOGGER.log(Level.WARNING, "Cannot compute PR for linear " + troncon.getId(), getException());// SYM-1700
                final String tronconTitle = new SirsStringConverter().toString(troncon);
                new Growl(Growl.Type.WARNING, "Impossible de calculer les PRs du tronçon "+tronconTitle+". Veuillez vérifier les informations de référencement linéaire.").showAndFade();
            super.failed();
        }
    }

    protected class TypeChoicePojoTable extends PojoTable {

        public TypeChoicePojoTable(final Class clazz, final String title) {
            super(clazz, title, (ObjectProperty<Element>) null);
            getColumns().remove(editCol);
            editableProperty.set(false);
            createNewProperty.set(false);
            fichableProperty.set(false);
            uiAdd.setVisible(false);
            uiFicheMode.setVisible(false);
            uiDelete.setVisible(false);
        }
    }

    /**
     * Check that given object is located in one of the specified linears.
     * @param <T>
     */
    final protected class LinearPredicate<T extends AvecForeignParent> implements Predicate<T> {

        final Set<String> acceptedIds;

        public LinearPredicate() {
            acceptedIds = tronconsTable.getSelectedItems().stream().map(input -> input.getId()).collect(Collectors.toSet());
        }

        @Override
        public boolean test(T t) {
            return acceptedIds.isEmpty() || (t.getForeignParentId() != null && acceptedIds.contains(t.getForeignParentId()));
        }
    }

    /**
     * Check that given object PRs are found in selected PRs in its parent linear.
     * @param <T> Type of object to test.
     */
    final protected class PRPredicate<T extends Positionable & AvecForeignParent> implements Predicate<T> {

        @Override
        public boolean test(final T candidate) {
            final String linearId = candidate.getForeignParentId();
            if (linearId == null)
                return false;

            final ObjectProperty<Number>[] userPRs = ajustedPrsByTronconId.get(linearId);
            // Note : Can happen if user has not visualized the entire linear table,
            // because the PRs parameters are populated lazily.
            if (userPRs == null)
                return true;

            final float startPR = (userPRs[0] == null || userPRs[0].get() == null) ? Float.NaN : userPRs[0].get().floatValue();
            final float endPR = (userPRs[1] == null || userPRs[1].get() == null) ? Float.NaN : userPRs[1].get().floatValue();

            if (!Float.isNaN(startPR) && candidate.getPrFin() < startPR)
                return false;

            return Float.isNaN(endPR) || candidate.getPrDebut() <= endPR;
        }
    }

    final protected class ValidPredicate implements Predicate<Element> {
        @Override
        public boolean test(Element t) {
            if (t.getValid()) {
                if (!uiOptionExcludeValid.isSelected()) return true;
            } else {
                if (!uiOptionExcludeInvalid.isSelected()) return true;
            }
            return false;
        }
    }

    final protected class isNotOnArchivedTroncon<T extends AvecForeignParent> implements Predicate<T> {
        @Override
        public boolean test(final T candidate) {
            String linearId = candidate.getForeignParentId();
            if (linearId == null) return true;
            LocalDate tronconDateFin;
            try {
                tronconDateFin = Injector.getSession().getPreviews().get(linearId).getDate_fin();
                return tronconDateFin == null || tronconDateFin.isAfter(LocalDate.now());
            } catch (DocumentNotFoundException dnfe) {
                SirsCore.LOGGER.log(Level.WARNING, "No document found for " + linearId, dnfe);
                return true;
            }
        }
    }
}
