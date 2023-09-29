
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.*;
import fr.sirs.util.property.SirsPreferences;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;

import java.util.Optional;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class FXPositionDocumentPane extends AbstractFXElementPane<PositionDocument> {

    protected final Previews previewRepository;
    protected LabelMapper labelMapper;

    // Propriétés de Positionable
    @FXML private FXPositionablePane uiPositionable;

    // Propriétés de PositionDocument
    @FXML protected TextArea ui_commentaire;
    @FXML protected ComboBox ui_sirsdocument;
    @FXML protected Button ui_sirsdocument_link;

    // Propriétés de AvecGeometrie

    // Propriétés de AbstractPositionDocument
    @FXML protected ComboBox ui_linearId;
    @FXML protected Button ui_linearId_link;

    // Propriétés de AbstractPositionDocumentAssociable

    /**
     * Constructor. Initialize part of the UI which will not require update when
     * element edited change.
     */
    protected FXPositionDocumentPane() {
        SIRS.loadFXML(this, PositionDocument.class);
        final Session session = Injector.getBean(Session.class);
        previewRepository = session.getPreviews();
        elementProperty().addListener(this::initFields);

        uiPositionable.disableFieldsProperty().bind(disableFieldsProperty());
        uiPositionable.positionableProperty().bind(elementProperty());

        /*
         * Disabling rules.
         */
        ui_commentaire.setWrapText(true);
        ui_commentaire.editableProperty().bind(disableFieldsProperty().not());
        ui_sirsdocument.disableProperty().bind(disableFieldsProperty());
        ui_sirsdocument_link.disableProperty().bind(ui_sirsdocument.getSelectionModel().selectedItemProperty().isNull());
        ui_sirsdocument_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_sirsdocument_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_sirsdocument.getSelectionModel().getSelectedItem()));
        ui_linearId.disableProperty().bind(disableFieldsProperty());

        /*
        Écouteur permettant de changer le tronçon sur lequel la position de document est située.

        ATTENTION : Lorsqu'on clique trop rapidement en dehors de la liste déroulante après avoir changé de tronçon, on
        rentre à nouveau dans cet écouteur avec newValue=null. Cela provoque entre autres l'impossiblité d'enregistrer
        l'élément en l'absence de linéaire parent.
        */
        ui_linearId.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if(newValue!=oldValue && newValue instanceof Preview){

                    // 1- On récupère le tronçon à partir du Preview.
                    final Optional<? extends Element> element = Injector.getSession().getElement(newValue);

                    if(element.isPresent() && element.get() instanceof TronconDigue){

                        final TronconDigue newTroncon = (TronconDigue) element.get();

                        // 2- On change l'identifiant du tronçon associé à l'élément
                        // Cela est nécessaire pour la mise à jour des informations de position
                        elementProperty.get().setLinearId(newTroncon.getId());

                        // 3- Il est impossible de "deviner" la géométrie de l'objet sur un nouveau tronçon quelconque
                        // On affecte donc a priori comme géométrie la géométrie du tronçon entier.
                        elementProperty.get().setGeometry(newTroncon.getGeometry());

                        // 4- Mise à jour des informations de position
                        uiPositionable.updateSRAndPRInfo();
                    }
                }
            }
        });
        ui_linearId_link.disableProperty().bind(ui_linearId.getSelectionModel().selectedItemProperty().isNull());
        ui_linearId_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_linearId_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_linearId.getSelectionModel().getSelectedItem()));
    }

    public FXPositionDocumentPane(final PositionDocument positionDocument){
        this();
        this.elementProperty().set(positionDocument);
    }

    /**
     * Initialize fields at element setting.
     */
    protected void initFields(ObservableValue<? extends PositionDocument > observableElement, PositionDocument oldElement, PositionDocument newElement) {
        // Unbind fields bound to previous element.
        if (oldElement != null) {
        // Propriétés de PositionDocument
            ui_commentaire.textProperty().unbindBidirectional(oldElement.commentaireProperty());
            ui_commentaire.setText(null);
        // Propriétés de AvecGeometrie
        // Propriétés de AbstractPositionDocument
        // Propriétés de AbstractPositionDocumentAssociable
        }

        final Session session = Injector.getBean(Session.class);

        if (newElement == null) {

                ui_sirsdocument.setItems(null);
                ui_linearId.setItems(null);
        } else {


        /*
         * Bind control properties to Element ones.
         */
        // Propriétés de PositionDocument
        // * commentaire
        ui_commentaire.textProperty().bindBidirectional(newElement.commentaireProperty());
        SIRS.initCombo(ui_sirsdocument, SIRS.observableList(
            previewRepository.getByClass(SIRSDefaultDocument.class)).sorted(),
            newElement.getSirsdocument() == null ? null : previewRepository.get(newElement.getSirsdocument()));
        // Propriétés de AvecGeometrie
        // Propriétés de AbstractPositionDocument
        {
            final Preview linearPreview = newElement.getLinearId() == null ? null : previewRepository.get(newElement.getLinearId());
            // HACK-REDMINE-4408 : hide archived linear from selection lists
            SIRS.initCombo(ui_linearId, SIRS.observableList(
                previewRepository.getByClass(linearPreview == null ? TronconDigue.class : linearPreview.getJavaClassOr(TronconDigue.class))).sorted(),
                    linearPreview, SirsPreferences.getHideArchivedProperty(), true);
        }
        // Propriétés de AbstractPositionDocumentAssociable
        }

    }
    @Override
    public void preSave() {
        final Session session = Injector.getBean(Session.class);
        final PositionDocument element = (PositionDocument) elementProperty().get();


        element.setCommentaire(ui_commentaire.getText());

        uiPositionable.preSave();

        Object cbValue;
        cbValue = ui_sirsdocument.getValue();
        if (cbValue instanceof Preview) {
            element.setSirsdocument(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setSirsdocument(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setSirsdocument(null);
        }
        cbValue = ui_linearId.getValue();
        if (cbValue instanceof Preview) {
            element.setLinearId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setLinearId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setLinearId(null);
        }
    }
}