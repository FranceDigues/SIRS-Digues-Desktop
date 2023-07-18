
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.PropertiesFileUtilities;
import fr.sirs.Session;
import fr.sirs.core.SirsCore;
import fr.sirs.core.model.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import org.geotoolkit.gui.javafx.util.AbstractPathTextField;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Class used to group prestations' linked pojotables  in 'categories' as for Desordres.
 * The group should be introduced in .jet files to automatically generate it for each reference with a categoried element types.
 * @author Maxime Gavens (Geomatys)
 */
public class FXPrestationPane extends FXPrestationPaneStub {
    // As the Prestation's attribute syntheseTablePath is a String, the .jet automatically creates
    // a TextField ui_syntheseTablePath in FXPrestationPaneStub.java that shall also appear in FXPrestationPane.fxml.
    // However, we would like to have a FXHorodatageFileField instead of the basic TextField.
    // To do so, the ui_syntheseTablePath is removed from the parent and we add synthesisTableField instead.
    private FXHorodatageFileField ui_synthesisTableFieldStart;
    @FXML
    private GridPane ui_horodatagePane;
    @FXML
    private Label ui_errorMessageStart;
    private FXHorodatageFileField ui_synthesisTableFieldEnd;
    @FXML
    private Label ui_errorMessageEnd;

    @FXML private FXValidityPeriodPane uiValidityPeriod;

    /**
     * REDMINE 7782 - doc "Prestation cochée par défaut.xlsx"
     * List of RefPrestation ids for which the prestation shall be automatically added to the SE registre.
     * 1	Entretien
     * 2	Construction
     * 3	Confortement
     * 9	Diagnostic visuel
     * 10	Intervention d'urgence sur végétation
     * 11	Travaux de confortement en urgence
     * 12	Contrôle
     * 13	Coupe
     * 14	Débroussaillage
     * 15	Plantation
     * 16	Traitement d'invasive
     * 17	Sondage à la pelle
     * 18	Réparation
     * 19	Pose de portail
     * 20	Entretien de portail
     * 21	Sondage destructif
     * 22	Panneau électrique
     * 23	Sondage pénétrométrique
     * 24	Evacuation d'ordures
     * 25	Abattage arbre isolé
     * 26	Traitement de terrier
     * 27	Création de merlon
     * 28	Traitement de passage sauvage
     * 29	Visite technique approfondie
     * 30	Inspection de digues
     * 32	Déterrage de blaireaux
     * 33	Dessouchage
     * 34	Sondage carotté
     * 35	Travaux d'entretien forestier
     * 36	Surveillance continue
     * 37	Surveillance annuelle
     * 38	Fauchage
     * 39	Capture de lapins
     */
    private static final List<Integer> typeInRegistreIds = Arrays.asList(1, 2, 3, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
            22, 23, 24, 25, 26, 27, 28, 29, 30, 32, 33, 34, 35, 36, 37, 38, 39);

    // hack because if user select "Cancel" in datePicker changelistener -> reset date to oldValue and goes back into changeListener again.
    private boolean resetEndDate = false;

    private final DatePicker dateFinPicker = uiValidityPeriod.getDateFinPicker();

    public FXPrestationPane(final Prestation prestation){
        super(prestation);
        final Session session = Injector.getBean(Session.class);
        ui_desordreIds.setContent(() -> {
            // HACK_REDMINE_7605 - TronconLit has Prestation and DesordreLit : best would be to create PrestationLit and adapt all plugin-lit models
            Prestation presta = elementProperty().get();
            if (presta == null) throw new IllegalStateException("The prestation cannot be null");
            String linearId = presta.getLinearId();
            if (linearId == null) throw new IllegalStateException("The linearId of the prestation cannot be null");
            TronconDigue troncon = session.getRepositoryForClass(TronconDigue.class).get(linearId);
            if (troncon == null) throw new IllegalStateException("No element found for id " + linearId);
            if ("TronconLit".equals(troncon.getClass().getSimpleName()))
                desordreIdsTable = new PrestationDesordresPojoTable(elementProperty(), true);
            else desordreIdsTable = new PrestationDesordresPojoTable(elementProperty(), false);
            desordreIdsTable.editableProperty().bind(disableFieldsProperty().not());
            desordreIdsTable.createNewProperty().set(false);
            updateDesordreIdsTable(session, elementProperty.get());
            return desordreIdsTable;
        });
        // ui_syntheseTablePath is removed as it is replaced by ui_synthesisTableField
        ui_horodatagePane.getChildren().remove(ui_syntheseTablePathStart);
        ui_horodatagePane.getChildren().remove(ui_syntheseTablePathEnd);
        ui_horodatagePane.add(ui_synthesisTableFieldStart, 1, 4);
        ui_horodatagePane.add(ui_synthesisTableFieldEnd, 1, 8);

        setTableField(ui_synthesisTableFieldStart, ui_errorMessageStart);

        setTableField(ui_synthesisTableFieldEnd, ui_errorMessageEnd);

        // The Prestation shall automatically be added to the SE registre depending on the type of Prestation.
        ui_typePrestationId.valueProperty().addListener(autoSelectRegistreListener());

        dateFinPicker.valueProperty().removeListener(uiValidityPeriod.getEndDateListener());
        dateFinPicker.valueProperty().addListener(dateFinPickerListener());
    }

    /**
     * Listener on dateFinPicker.
     * If the Prestation has the status "Horodaté" and its end date has not been set already, show a confirmation dialog :
     * <ul>
     *     <li>Oui : the date is set and the prestation's timpestamped status is set to "Non horodaté"</li>
     *     <li>Non : the date is set and the prestation's timpestamped status is not updated </li>
     *     <li>Annuler : the date is not changed and the prestation's timpestamped status is not updated</li>
     * </ul>
     * @return the @{@link ChangeListener}
     */
    private ChangeListener<LocalDate> dateFinPickerListener() {
        return (obs, oldValue, newValue) -> {
            if (!uiValidityPeriod.checkEndDateOk(oldValue, newValue)) return;
            if (resetEndDate) {
                resetEndDate = false;
                return;
            }

            final RefHorodatageStatus selectedItem = (RefHorodatageStatus) ui_horodatageStatusId.getSelectionModel().getSelectedItem();
            if (ui_registreAttribution.isSelected() && HorodatageReference.getRefTimeStampedStatus().equals(selectedItem.getId())) {
                String message;
                if (ui_horodatageEndDate.getValue() == null) {
                    message = "La prestation a déjà été horodatée pour sa date de début." +
                            "\n\nSouhaitez-vous l'horodater une nouvelle fois pour la date de fin ?" +
                            "\n\nSi oui, le statut d'horodatage passera automatiquement en \"Non horodaté\"." +
                            "\n\nSi Annuler, la date de fin ne sera pas modifiée.";
                } else {
                    message = "La prestation a déjà été horodatée pour sa date de fin." +
                            "\n\nSouhaitez-vous l'horodater une nouvelle fois pour la date de fin ?" +
                            "\n\nSi oui, le statut d'horodatage passera automatiquement en \"Non horodaté\"." +
                            "\n\nSi Annuler, la date de fin ne sera pas modifiée.";
                }

                Platform.runLater(() -> {
                    final Optional optional = PropertiesFileUtilities.showConfirmationDialog(message, "Horodatage pour date de fin", 600, 200, true);
                    if (optional.isPresent()) {
                        if (ButtonType.YES.equals(optional.get())) {
                            final Optional<RefHorodatageStatus> notTimeStampedItem = ui_horodatageStatusId.getItems().stream().filter(s -> HorodatageReference.getRefNonTimeStampedStatus().equals(((RefHorodatageStatus) s).getId())).findFirst();
                            if (!notTimeStampedItem.isPresent())
                                throw new IllegalStateException("Missing \"Non horodaté\" item in ui_horodatageStatusId");
                            ui_horodatageStatusId.getSelectionModel().select(notTimeStampedItem.get());
                        } else if (ButtonType.NO.equals(optional.get())) {
                            // do nothing
                        } else if (ButtonType.CANCEL.equals(optional.get())) {
                            resetEndDate = true;
                            dateFinPicker.setValue(oldValue);
                        }
                    }
                });
            }
        };
    }

    private void setTableField(FXHorodatageFileField ui_synthesisTableFieldStart, Label ui_errorMessageStart) {
        ui_synthesisTableFieldStart.disableFieldsProperty.bind(disableFieldsProperty());
        ui_errorMessageStart.visibleProperty().bind(ui_errorMessageStart.textProperty().isNotEmpty());
        ui_errorMessageStart.setTextFill(Color.RED);
        ui_errorMessageStart.setFont(new Font(12));
    }

    /**
     * Change listener on typePrestationId to auto select/deselect the registreAttribution checkbox.
     * @return the @{@link ChangeListener}
     */
    private ChangeListener<RefPrestation> autoSelectRegistreListener() {
        return (obs, oldValue, newValue) -> {
            if (newValue != null) {
                final String id = newValue.getId();
                autoSelectRegistre(elementProperty().get(), id);
            }
        };
    }

    /**
     * Update prestation's registreAttribution value depending on the typePrestationId.
     * @param prestation the prestation to update.
     * @param refTypeId a prestation's type reference id with the format : "RefPrestation:2"
     */
    static void autoSelectRegistre(final Prestation prestation, final String refTypeId) {
        final int length = "RefPrestation:".length();
        if (refTypeId != null && refTypeId.length() > length) {
            final String refTypeIdShort = refTypeId.substring(length);
            if (refTypeIdShort != null)
                prestation.setRegistreAttribution(typeInRegistreIds.contains(refTypeIdShort));
        }
    }

    /**
     * Initialize fields at element setting.
     */
    @Override
    protected void initFields(ObservableValue<? extends Prestation > observableElement, Prestation oldElement, Prestation newElement) {
        super.initFields(observableElement, oldElement, newElement);
        // Cannot instantiate ui_synthesisTableField when creating the class attributes as it passes through initFields()
        // before creating the class attributes, leading to a NullPointerException.
        if (ui_synthesisTableFieldStart == null ) ui_synthesisTableFieldStart = new FXHorodatageFileField(ui_errorMessageStart);
        if (ui_synthesisTableFieldEnd == null ) ui_synthesisTableFieldEnd = new FXHorodatageFileField(ui_errorMessageEnd);
        // Unbind fields bound to previous element.
        if (oldElement != null) {
            // Propriétés de Prestation
            ui_synthesisTableFieldStart.textProperty().unbindBidirectional(oldElement.syntheseTablePathStartProperty());
            ui_synthesisTableFieldStart.setText(null);
            ui_synthesisTableFieldEnd.textProperty().unbindBidirectional(oldElement.syntheseTablePathEndProperty());
            ui_synthesisTableFieldEnd.setText(null);
        }

        if (newElement != null) {

            /*
             * Bind control properties to Element ones.
             */
            // Propriétés de Prestation
            // * syntheseTablePath
            ui_synthesisTableFieldStart.textProperty().bindBidirectional(newElement.syntheseTablePathStartProperty());
            ui_synthesisTableFieldStart.checkInputTextValid();
            ui_synthesisTableFieldEnd.textProperty().bindBidirectional(newElement.syntheseTablePathEndProperty());
            ui_synthesisTableFieldEnd.checkInputTextValid();
        }
    }

    private class FXHorodatageFileField extends AbstractPathTextField {

        private final BooleanProperty disableFieldsProperty = new SimpleBooleanProperty();

        /**
         * Supported formats for timestamped files.
         */
        private final List<String> supportedFormat = Collections.singletonList("*.pdf");

        private final Label ui_errorMessage;

        public FXHorodatageFileField(final Label ui_errorMessage) {
            this.ui_errorMessage = ui_errorMessage;
            inputText.disableProperty().bind(disableFieldsProperty);
            choosePathButton.disableProperty().bind(disableFieldsProperty);

            inputText.focusedProperty().addListener((obs, oldValue, newValue) -> {
                if (oldValue && !newValue) {
                    checkInputTextValid();
                }
            });

            // Override the OnAction set in the parent.
            choosePathButton.setOnAction((ActionEvent e)-> {
                final String content = chooseInputContent();
                if (content != null) {
                    setText(content);
                }
                checkInputTextValid();
            });
        }

        private void checkInputTextValid() {
            final String text = this.getText();
            if (text != null && !text.isEmpty()) {
                if (!text.endsWith(".pdf")) {
                    ui_errorMessage.setText("Veuillez sélectionner un fichier au format PDF.");
                    return;
                }

                final File syntheseFile = new File(text);
                if (!syntheseFile.exists()) {
                    ui_errorMessage.setText("Le fichier est introuvable.");
                    return;
                }
            }
            ui_errorMessage.setText("");
        }

        @Override
        protected String chooseInputContent() {
            final FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Supported formats for cover and conclusion pages", supportedFormat));

            try {
                URI uriForText = getURIForText(getText());
                final Path basePath = Paths.get(uriForText);
                if (Files.isDirectory(basePath)) {
                    chooser.setInitialDirectory(basePath.toFile());
                } else if (Files.isDirectory(basePath.getParent())) {
                    chooser.setInitialDirectory(basePath.getParent().toFile());
                }
            } catch (Exception e) {
                // Well, we'll try without it...
                SirsCore.LOGGER.log(Level.FINE, "Input path cannot be decoded.", e);
            }
            File returned = chooser.showOpenDialog(null);
            if (returned == null) {
                return null;
            } else {
                return (completor.root != null) ?
                        completor.root.relativize(returned.toPath()).toString() : returned.getAbsolutePath();
            }
        }

        @Override
        protected URI getURIForText(String inputText) throws Exception {
            return new URI(inputText);
        }
    }
}
