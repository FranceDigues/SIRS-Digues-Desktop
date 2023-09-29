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
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.model.AvecForeignParent;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.theme.AbstractTheme;
import fr.sirs.theme.TronconTheme;
import fr.sirs.util.SimpleFXEditMode;
import fr.sirs.util.property.SirsPreferences;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.List;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXTronconThemePane extends BorderPane {

    // Préview spécifique pour l'affichage des objets n'ayant pas d'identifiant de tronçon.(SYM-1765)
    private static final Preview EMPTY_PREVIEW = new Preview();

    static {
        EMPTY_PREVIEW.setElementClass(TronconDigue.class.getCanonicalName());
        EMPTY_PREVIEW.setLibelle("   Pas de tronçon de rattachement - objets orphelins   ");
    }

    @FXML private BorderPane uiCenter;
    @FXML private ComboBox<Preview> uiLinearChoice;

    private final StringProperty linearIdProperty = new SimpleStringProperty();
    private final Session session = Injector.getBean(Session.class);

    public StringProperty linearIdProperty(){return linearIdProperty;}

    public FXTronconThemePane(TronconTheme.ThemeManager ... groups) {
        SIRS.loadFXML(this, FXTronconThemePane.class, null);

        if (groups.length==1) {
            final Parent content = createContent(groups[0]);
            uiCenter.setCenter(content);
            if(content instanceof BorderPane){
                content.requestFocus();
                final Node center = ((BorderPane)content).getCenter();
                center.requestFocus();
            }

        }else{
            final TabPane pane = new TabPane();
            pane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            for(int i=0; i<groups.length; i++){
                final Tab tab = new Tab(groups[i].getName());
                tab.setContent(createContent(groups[i]));
                pane.getTabs().add(tab);
            }
            uiCenter.setCenter(pane);
        }

        uiLinearChoice.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Preview> observable, Preview oldValue, Preview newValue) -> {
            if (newValue != null) {
                linearIdProperty.set(newValue.getElementId());
            }
        });

        final List<Preview> byClass = session.getPreviews().getByClass(TronconDigue.class);
        if(!byClass.contains(EMPTY_PREVIEW)){
            byClass.add(EMPTY_PREVIEW);
        }
        final ObservableList<Preview> linearPreviews = SIRS.observableList(byClass).sorted();
        // HACK-REDMINE-4408 : hide archived troncons from selection lists
        SIRS.initCombo(uiLinearChoice, linearPreviews, null, SirsPreferences.getHideArchivedProperty(), false);

    }

    protected class TronconThemePojoTable<T extends AvecForeignParent> extends ForeignParentPojoTable<T>{

        private final TronconTheme.ThemeManager<T> group;

        public TronconThemePojoTable(TronconTheme.ThemeManager<T> group, final ObjectProperty<? extends Element> container) {
            super(group.getDataClass(), group.getTableTitle(), container);
            foreignParentIdProperty.addListener(this::updateTable);
            this.group = group;
        }

        private void updateTable(ObservableValue<? extends String> observable, String oldValue, String newValue){
            if(group==null) {
                setTableItems(FXCollections::emptyObservableList);
            } else {
                setTableItems(() -> (ObservableList) group.getExtractor().apply(newValue));
            }
        }
    }

    protected Parent createContent(AbstractTheme.ThemeManager manager) {

        //Composant : Consultation/Edition
        final Separator separator = new Separator();
        separator.setVisible(false);
        final SimpleFXEditMode editMode = new SimpleFXEditMode();
        final HBox topPane = new HBox(separator, editMode);
        HBox.setHgrow(separator, Priority.ALWAYS);

        //Création de la TronconThemePojoTable
        final TronconThemePojoTable table = new TronconThemePojoTable(manager, (ObjectProperty<? extends Element>) null);
        table.setDeletor(manager.getDeletor());
        table.editableProperty.bind(editMode.editionState());
        table.foreignParentProperty().bindBidirectional(linearIdProperty);

        // Remplissage du BorderPane parent (englobant) (center, top, right, bottom, left).
        return new BorderPane(table, topPane, null, null, null);
    }
}