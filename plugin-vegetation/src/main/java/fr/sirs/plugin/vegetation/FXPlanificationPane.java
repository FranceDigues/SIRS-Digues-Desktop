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
package fr.sirs.plugin.vegetation;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.property.SirsPreferences;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import static fr.sirs.plugin.vegetation.FXPlanTable.Mode.PLANIFICATION;

/**
 * Panneau de planification des parcelles du plan, ouvert lors de la sélection d'un plan dans la liste proposée au chemin suivant :
 *
 * "Plan de gestion" > "planification"
 *
 * @author Johann Sorel (Geomatys)
 * @author Samuel Andrés (Geomatys)
 *
 * @see FXPlanTable
 */
public class FXPlanificationPane extends BorderPane {

    @FXML private GridPane uiHeader;

    public FXPlanificationPane() {
        SIRS.loadFXML(this);
        initialize();
    }

    private void initialize() {

        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        final Session session = Injector.getSession();

        // Choix du tronçon.
        final ComboBox<Preview> uiTroncons = new ComboBox<>();
        // HACK-REDMINE-4408 : hide archived Troncons from selection lists
        SIRS.initCombo(uiTroncons, SIRS.observableList(session.getPreviews().getByClass(TronconDigue.class)).sorted(),
                null, SirsPreferences.getHideArchivedProperty(), true);
        final Label lblTroncon = new Label("Tronçon : ");
        lblTroncon.getStyleClass().add("label-header");

        uiHeader.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        uiHeader.getStyleClass().add("blue-light");
        uiHeader.setHgap(10);
        uiHeader.setVgap(10);
        uiHeader.setPadding(new Insets(10, 10, 10, 10));
        uiHeader.add(lblTroncon, 1, 0);
        uiHeader.add(uiTroncons, 2, 0);
        final Label lblTitle = new Label("Planification des parcelles");
        lblTitle.setPadding(new Insets(0, 40, 0, 40));
        lblTitle.getStyleClass().add("label-header");
        lblTitle.setStyle("-fx-font-size: 1.5em;");
        uiHeader.add(lblTitle, 0, 0);


        if(VegetationSession.INSTANCE.planProperty().getValue()!=null){
            setCenter(new FXPlanTable(VegetationSession.INSTANCE.planProperty().getValue(), uiTroncons.getValue() == null? null : uiTroncons.getValue().getElementId(), PLANIFICATION, null, 0));
        }

        //on ecoute les changements de troncon et de plan
        final ChangeListener chgListener = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
            if(VegetationSession.INSTANCE.planProperty().getValue()!=null){
                setCenter(new FXPlanTable(VegetationSession.INSTANCE.planProperty().getValue(), uiTroncons.getValue() == null? null : uiTroncons.getValue().getElementId(), PLANIFICATION, null, 0));
            }
            else setCenter(null);
        };

        VegetationSession.INSTANCE.planProperty().addListener(new WeakChangeListener(chgListener));
        uiTroncons.valueProperty().addListener(chgListener);
    }
}