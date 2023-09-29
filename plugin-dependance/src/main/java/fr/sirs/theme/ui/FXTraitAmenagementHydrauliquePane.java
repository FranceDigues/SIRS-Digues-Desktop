
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.*;
import fr.sirs.util.property.SirsPreferences;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class FXTraitAmenagementHydrauliquePane extends AbstractFXElementPane<TraitAmenagementHydraulique> {

    protected final Previews previewRepository;
    protected LabelMapper labelMapper;
    private String oldValueAhId;

    @FXML private FXValidityPeriodPane uiValidityPeriod;

    // Propriétés de TraitAmenagementHydraulique
    @FXML protected TextArea ui_commentaire;
    @FXML protected ComboBox ui_amenagementHydrauliqueId;
    @FXML protected Button ui_amenagementHydrauliqueId_link;

    // Maybe to come
    //@FXML FXPositionDependancePane uiPosition;

    /**
     * Constructor. Initialize part of the UI which will not require update when
     * element edited change.
     */
    protected FXTraitAmenagementHydrauliquePane() {
        SIRS.loadFXML(this, TraitAmenagementHydraulique.class);
        final Session session = Injector.getBean(Session.class);
        previewRepository = session.getPreviews();
        elementProperty().addListener(this::initFields);

        uiValidityPeriod.disableFieldsProperty().bind(disableFieldsProperty());
        uiValidityPeriod.targetProperty().bind(elementProperty());

        /*
        * Disabling rules.
        */
        ui_commentaire.setWrapText(true);
        ui_commentaire.editableProperty().bind(disableFieldsProperty().not());
        ui_amenagementHydrauliqueId.disableProperty().bind(disableFieldsProperty());
        ui_amenagementHydrauliqueId_link.disableProperty().bind(ui_amenagementHydrauliqueId.getSelectionModel().selectedItemProperty().isNull());
        ui_amenagementHydrauliqueId_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_amenagementHydrauliqueId_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_amenagementHydrauliqueId.getSelectionModel().getSelectedItem()));
        //uiPosition.disableFieldsProperty().bind(disableFieldsProperty());

        //uiPosition.dependanceProperty().bind(elementProperty);
    }

    public FXTraitAmenagementHydrauliquePane(final TraitAmenagementHydraulique traitAmenagementHydraulique){
        this();
        this.elementProperty().set(traitAmenagementHydraulique);
    }

    /**
     * Initialize fields at element setting.
     */
    protected void initFields(ObservableValue<? extends TraitAmenagementHydraulique > observableElement, TraitAmenagementHydraulique oldElement, TraitAmenagementHydraulique newElement) {
        // Unbind fields bound to previous element.
        if (oldElement != null) {
            // Propriétés de TraitAmenagementHydraulique
            ui_commentaire.textProperty().unbindBidirectional(oldElement.commentaireProperty());
            ui_commentaire.setText(null);
            // Propriétés de AvecGeometrie
            // Propriétés de AvecSettableGeometrie
        }

        final Session session = Injector.getBean(Session.class);

        if (newElement == null) {

            ui_amenagementHydrauliqueId.setItems(null);
        } else {


            /*
            * Bind control properties to Element ones.
            */
            // Propriétés de TraitAmenagementHydraulique
            // * commentaire
            ui_commentaire.textProperty().bindBidirectional(newElement.commentaireProperty());
            final Preview linearPreview = newElement.getAmenagementHydrauliqueId() == null ? null : previewRepository.get(newElement.getAmenagementHydrauliqueId());
            oldValueAhId = newElement.getAmenagementHydrauliqueId();
            // HACK-REDMINE-4408 : hide archived AH from selection lists
            SIRS.initCombo(ui_amenagementHydrauliqueId, SIRS.observableList(
                    previewRepository.getByClass(linearPreview == null ? AmenagementHydraulique.class : linearPreview.getJavaClassOr(AmenagementHydraulique.class))).sorted(),
                    linearPreview, SirsPreferences.getHideArchivedProperty(), true);
            // Propriétés de AvecGeometrie
            // Propriétés de AvecSettableGeometrie
        }
    }
    @Override
    public void preSave() {
        final TraitAmenagementHydraulique element = (TraitAmenagementHydraulique) elementProperty().get();
        element.setCommentaire(ui_commentaire.getText());

        Object cbValue;
        cbValue = ui_amenagementHydrauliqueId.getValue();
        if (cbValue instanceof Preview) {
            element.setAmenagementHydrauliqueId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setAmenagementHydrauliqueId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setAmenagementHydrauliqueId(null);
        }
    }
}