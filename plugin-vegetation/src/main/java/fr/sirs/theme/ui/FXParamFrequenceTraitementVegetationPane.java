
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.InvasiveVegetation;
import fr.sirs.core.model.ParamFrequenceTraitementVegetation;
import fr.sirs.core.model.PeuplementVegetation;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.RefSousTraitementVegetation;
import fr.sirs.core.model.RefTypeInvasiveVegetation;
import fr.sirs.core.model.RefTypePeuplementVegetation;
import fr.sirs.core.model.ZoneVegetation;
import fr.sirs.plugin.vegetation.PluginVegetation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class FXParamFrequenceTraitementVegetationPane extends FXParamFrequenceTraitementVegetationPaneStub {

    @FXML ComboBox<Class<? extends ZoneVegetation>> ui_type;
    
    public FXParamFrequenceTraitementVegetationPane(final ParamFrequenceTraitementVegetation paramFrequenceTraitementVegetation){
        super(paramFrequenceTraitementVegetation);

        // On refait le binding de la liste déroulante des fréquences, pour ne l'activer que pour les paramétrages de traitements ponctuels.
        ui_frequenceId.disableProperty().unbind();
        ui_frequenceId.disableProperty().bind(disableFieldsProperty().or(ui_ponctuel.selectedProperty()));

        // On met également un écouteur sur la case à cocher "ponctuel" de manière à annuler la valeur de la fréquence lorsqu'elle est sélectionnée.
        ui_ponctuel.selectedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if(newValue) ui_frequenceId.getSelectionModel().select(null);
            }
        });

        ui_type.disableProperty().bind(disableFieldsProperty());

        ui_type.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Class<? extends ZoneVegetation>>() {

            @Override
            public void changed(ObservableValue<? extends Class<? extends ZoneVegetation>> observable, Class<? extends ZoneVegetation> oldValue, Class<? extends ZoneVegetation> newValue) {
                initTypeVegetation(newValue);
            }
        });

        ui_traitementId.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {

            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if(newValue instanceof Preview){
                    final AbstractSIRSRepository<RefSousTraitementVegetation> sousTypeRepo = Injector.getSession().getRepositoryForClass(RefSousTraitementVegetation.class);
                    final String traitementId = ((Preview) newValue).getElementId();
                    if(traitementId!=null){
                        final List<RefSousTraitementVegetation> sousTypesDispos = sousTypeRepo.getAll();
                        sousTypesDispos.removeIf((RefSousTraitementVegetation st) -> !traitementId.equals(st.getTraitementId()));
                        SIRS.initCombo(ui_sousTraitementId, FXCollections.observableList(sousTypesDispos), null);
                    }
                }
            }
        });
    }

    private void initTypeVegetation(final Class zoneClass){
        final ParamFrequenceTraitementVegetation param = elementProperty().get();
        if(param!=null){
            if(PeuplementVegetation.class.isAssignableFrom(zoneClass)){
                SIRS.initCombo(ui_typeVegetationId,
                        FXCollections.observableList(previewRepository.getByClass(RefTypePeuplementVegetation.class)),
                        param.getTypeVegetationId() == null ? null : previewRepository.get(param.getTypeVegetationId()));
            } else if(InvasiveVegetation.class.isAssignableFrom(zoneClass)){
                SIRS.initCombo(ui_typeVegetationId,
                        FXCollections.observableList(previewRepository.getByClass(RefTypeInvasiveVegetation.class)),
                        param.getTypeVegetationId() == null ? null : previewRepository.get(param.getTypeVegetationId()));
            } else{
                SIRS.initCombo(ui_typeVegetationId, FXCollections.emptyObservableList(),null);
            }
        }
    }

    /**
     * Initialize fields at element setting.
     * @param observableElement
     * @param oldElement
     * @param newElement
     */
    @Override
    protected void initFields(ObservableValue<? extends ParamFrequenceTraitementVegetation > observableElement, ParamFrequenceTraitementVegetation oldElement, ParamFrequenceTraitementVegetation newElement) {

        super.initFields(observableElement, oldElement, newElement);

        SIRS.initCombo(ui_type, FXCollections.observableList(PluginVegetation.zoneVegetationClasses()), newElement.getType() == null ? null : newElement.getType());

        // Initialisation des sous-types
        final AbstractSIRSRepository<RefSousTraitementVegetation> repoSousTraitements = Injector.getSession().getRepositoryForClass(RefSousTraitementVegetation.class);
        final Map<String, RefSousTraitementVegetation> sousTraitements = new HashMap<>();

        for(final RefSousTraitementVegetation sousTraitement : repoSousTraitements.getAll()){
            sousTraitements.put(sousTraitement.getId(), sousTraitement);
        }
        final List<Preview> sousTraitementPreviews = previewRepository.getByClass(RefSousTraitementVegetation.class);

        PluginVegetation.initComboSousTraitement(newElement.getTraitementId(), newElement.getSousTraitementId(), sousTraitementPreviews, sousTraitements, ui_sousTraitementId);
    }


    @Override
    public void preSave() {
        super.preSave();

        final ParamFrequenceTraitementVegetation element = (ParamFrequenceTraitementVegetation) elementProperty().get();
        element.setType(ui_type.getSelectionModel().getSelectedItem());
    }
}