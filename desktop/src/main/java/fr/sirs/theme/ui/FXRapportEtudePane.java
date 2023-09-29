
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.model.RapportEtude;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.image.ImageView;

/**
 * Class used to group prestations' linked pojotables  in 'categories' as for Desordres.
 * The group should be introduce in .jet files to automatically generate it for each reference with a categoried element types.
 * @author Estelle Idée (Geomatys)
 */
public class FXRapportEtudePane extends FXRapportEtudePaneStub {

    //EVOLUTION-Redmine-7389 : link rapportEtude to SystemeEndiguement
    @FXML protected ComboBox ui_systemeEndiguementId;
    @FXML protected Button ui_systemeEndiguementId_link;


    public FXRapportEtudePane(final RapportEtude rapportEtude){
        super(rapportEtude);
        ui_systemeEndiguementId.disableProperty().bind(disableFieldsProperty());
        ui_systemeEndiguementId_link.disableProperty().bind(ui_systemeEndiguementId.getSelectionModel().selectedItemProperty().isNull());
        ui_systemeEndiguementId_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_systemeEndiguementId_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_systemeEndiguementId.getSelectionModel().getSelectedItem()));
        elementProperty().addListener(this::initFields);
    }
}