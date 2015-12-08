
package fr.sirs.plugin.vegetation;

import fr.sirs.SIRS;
import fr.sirs.core.model.PlanVegetation;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.GridPane;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;

/**
 * Panneau d'ajout de couche de données analyzées.
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXPlanLayerPane extends GridPane{

    @FXML private ChoiceBox<Integer> uiAnnee;
    @FXML private CheckBox uiParcelleType;
    @FXML private CheckBox uiTrmtReel;
    @FXML private CheckBox uiTrmtPlanif;
    @FXML private Button uiAddButton;

    public FXPlanLayerPane() {
        SIRS.loadFXML(this);

        uiAddButton.disableProperty().bind(
                uiTrmtReel.selectedProperty().not()
                .and(uiTrmtPlanif.selectedProperty().not())
                .and(uiParcelleType.selectedProperty().not())
                .or(uiAnnee.valueProperty().isNull())
        );

        ObjectProperty<PlanVegetation> planProperty = VegetationSession.INSTANCE.planProperty();
        planProperty.addListener((obs, oldValue, newValue) -> {
            initialize(newValue);
        });
        initialize(planProperty.get());
    }

    private void initialize(final PlanVegetation plan){
        if (plan == null) {
            uiAnnee.setItems(null);
        } else {
            final int anneDebut = plan.getAnneeDebut();
            final int anneFin = plan.getAnneeFin();

            final ObservableList<Integer> years = FXCollections.observableArrayList();
            for (int year = anneDebut; year < anneFin; year++) {
                years.add(year);
            }
            uiAnnee.setItems(years);
            if (!years.isEmpty())
                uiAnnee.valueProperty().set(years.get(0));
        }
    }

    @FXML
    public void addLayer(ActionEvent event) {
        PlanVegetation plan = VegetationSession.INSTANCE.planProperty().get();
        final Integer year = uiAnnee.valueProperty().get();
        final MapItem vegetationGroup = VegetationSession.INSTANCE.getVegetationGroup();

        if(uiParcelleType.isSelected()){
            final MapLayer layer = VegetationSession.parcellePanifState(plan, year, null);
            vegetationGroup.items().add(layer);
        }

        if(uiTrmtReel.isSelected()){
            final MapLayer layer = VegetationSession.parcelleTrmtState(plan, year, null);
            vegetationGroup.items().add(layer);
        }

        if(uiTrmtPlanif.isSelected()){
            final MapLayer layer = VegetationSession.vegetationPlanifState(plan, year);
            vegetationGroup.items().add(layer);
        }
    }

}
