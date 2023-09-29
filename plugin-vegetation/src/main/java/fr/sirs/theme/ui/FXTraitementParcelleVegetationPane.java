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
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.RefSousTraitementVegetation;
import fr.sirs.core.model.RefTraitementVegetation;
import fr.sirs.core.model.TraitementParcelleVegetation;
import static fr.sirs.plugin.vegetation.PluginVegetation.sousTypeTraitementFromTypeTraitementId;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class FXTraitementParcelleVegetationPane extends FXTraitementParcelleVegetationPaneStub {
    
    public FXTraitementParcelleVegetationPane(final TraitementParcelleVegetation traitementParcelleVegetation){
        super(traitementParcelleVegetation);

        ui_typeTraitementId.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {

            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if(newValue instanceof RefTraitementVegetation){
                    final List<RefSousTraitementVegetation> sousTraitements = sousTypeTraitementFromTypeTraitementId(((RefTraitementVegetation) newValue).getId());
                    SIRS.initCombo(ui_sousTypeTraitementId, FXCollections.observableList(sousTraitements), null);
                } else {
                    SIRS.initCombo(ui_sousTypeTraitementId, FXCollections.emptyObservableList(), null);
                }
            }
        });
    }

    @Override
    protected void initFields(ObservableValue<? extends TraitementParcelleVegetation > observableElement, TraitementParcelleVegetation oldElement, TraitementParcelleVegetation newElement) {
        super.initFields(observableElement, oldElement, newElement);

        final AbstractSIRSRepository<RefSousTraitementVegetation> repoSousTraitements = Injector.getSession().getRepositoryForClass(RefSousTraitementVegetation.class);
        final List<RefSousTraitementVegetation> sousTraitements = sousTypeTraitementFromTypeTraitementId(newElement.getTypeTraitementId());
        final RefSousTraitementVegetation currentSousTraitement = newElement.getSousTypeTraitementId() == null ? null : repoSousTraitements.get(newElement.getSousTypeTraitementId());
        SIRS.initCombo(ui_sousTypeTraitementId, FXCollections.observableList(sousTraitements), currentSousTraitement);
    }
}
