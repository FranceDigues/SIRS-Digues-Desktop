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
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.TronconDigueRepository;
import fr.sirs.core.model.Digue;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.core.model.AmenagementHydraulique;
import fr.sirs.util.SirsStringConverter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.geotoolkit.gui.javafx.util.FXTableCell;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @author Maxime Gavens (Geomatys)
 */
public class FXDiguePane extends FXDiguePaneStub {

    private static final String AH_COL = "amenagementHydrauliqueId";

    @Autowired private Session session;

    @FXML private VBox centerContent;

    private final TronconPojoTable table;


    protected FXDiguePane() {
        super();
        Injector.injectDependencies(this);
        table = new TronconPojoTable(elementProperty());
        table.editableProperty().set(false);
        table.parentElementProperty().bind(elementProperty);
        centerContent.getChildren().add(table);
    }

    public FXDiguePane(final Digue digue){
        this();
        this.elementProperty().set(digue);
    }

    /**
     *
     * @param observable
     * @param oldValue
     * @param newValue
     */
    @Override
    public void initFields(ObservableValue<? extends Digue> observable, Digue oldValue, Digue newValue) {
        super.initFields(observable, oldValue, newValue);
        if (newValue != null) {
            table.setTableItems(()->FXCollections.observableArrayList(((TronconDigueRepository) session.getRepositoryForClass(TronconDigue.class)).getByDigue(newValue)));
        }
    }

    private class TronconPojoTable extends PojoTable {

        public TronconPojoTable(final ObjectProperty<? extends Element> container) {
            super(TronconDigue.class, "Tronçons de la digue", container);
            createNewProperty.set(false);
            fichableProperty.set(false);
            uiAdd.setVisible(false);
            uiFicheMode.setVisible(false);
            uiDelete.setVisible(false);
            setDeletor(input -> {
                if (input instanceof TronconDigue) {
                    ((TronconDigue)input).setDigueId(null);
                    session.getRepositoryForClass((Class)input.getClass()).update(input);
                }
            });

            ObservableList<TableColumn<Element, ?>> columns = getColumns();
            TableColumn<Element, ?> ahCol = null;
            for (TableColumn<Element, ?> c: columns) {
                if (c instanceof PropertyColumn) {
                    if (AH_COL.equals(((PropertyColumn)c).getName())) {
                        ahCol = c;
                        break;
                    }
                }
            }
            if (ahCol != null) columns.remove(ahCol);
            columns.add(new AmenagementHydrauliqueColumn());
        }

        @Override
        protected TronconDigue createPojo() {
            throw new UnsupportedOperationException("Vous ne devez pas créer de tronçon à partir d'ici !");
        }
    }

    private class AmenagementHydrauliqueColumn extends TableColumn<Element, Element>{

        public AmenagementHydrauliqueColumn() {
            super("Amenagement Hydraulique");
            setEditable(false);
            setSortable(false);
            setResizable(true);
            setPrefWidth(200);

            setCellValueFactory((TableColumn.CellDataFeatures<Element, Element> param) -> {
                return new SimpleObjectProperty<>(param.getValue());
            });

            setCellFactory((TableColumn<Element, Element> param) -> {
                return new AmenagementHydrauliqueTableCell();
            });
        }
    }

    private class AmenagementHydrauliqueTableCell extends FXTableCell<Element, Element> {

        //private final CheckBox isAmenagementHydrauliqueCheckBox = new CheckBox();
        private final ComboBox<Preview> uiAmenagementHydrauliqueBox = new ComboBox();

        public AmenagementHydrauliqueTableCell() {
            super();
            HBox hb = new HBox();
            hb.getChildren().add(uiAmenagementHydrauliqueBox);
            //hb.getChildren().add(isAmenagementHydrauliqueCheckBox);
            setGraphic(hb);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setAlignment(Pos.CENTER);

            // Used only to indicate if the troncon is an Amenagement hydraulique
            // Autodetermined by the combobox
            //isAmenagementHydrauliqueCheckBox.disableProperty().setValue(true);

            ObservableList<Preview> items = SIRS.observableList(Injector.getSession().getPreviews().getByClass(AmenagementHydraulique.class));
            ObservableList<Preview> it = FXCollections.observableArrayList(items);
            it.add(null);

            uiAmenagementHydrauliqueBox.setItems(it.sorted());
            uiAmenagementHydrauliqueBox.setConverter(new SirsStringConverter());
        }

        @Override
        protected void updateItem(Element item, boolean empty) {
            super.updateItem(item, empty);

            if (item instanceof TronconDigue) {
                final TronconDigue td = (TronconDigue) item;
                final String amenagementHydrauliqueId = td.getAmenagementHydrauliqueId();

                if (amenagementHydrauliqueId == null) {
                    //isAmenagementHydrauliqueCheckBox.selectedProperty().setValue(false);
                } else {
                    //isAmenagementHydrauliqueCheckBox.selectedProperty().setValue(true);
                    final Preview preview = Injector.getSession().getPreviews().get(amenagementHydrauliqueId);
                    SingleSelectionModel<Preview> selectionModel = uiAmenagementHydrauliqueBox.getSelectionModel();
                    selectionModel.select(preview);
                }
            }
            uiAmenagementHydrauliqueBox.valueProperty().addListener((ObservableValue<? extends Preview> ov, Preview oldValue, Preview newValue) -> {
                if (item instanceof TronconDigue) {
                    final TronconDigue troncon = (TronconDigue) item;
                    final AbstractSIRSRepository<AmenagementHydraulique> ahRep = session.getRepositoryForClass(AmenagementHydraulique.class);
                    if (newValue != null) {
                        // Si la nouvelle valeur n'est pas vide, on set au
                        // Troncon l'identifiant de l'aménagement hydraulique et
                        // on set à l'AH l'identifiant du troncon.
                        troncon.setAmenagementHydrauliqueId(newValue.getElementId());
                        AmenagementHydraulique ah = ahRep.get(newValue.getElementId());
                        ah.setLinearId(troncon.getId());
                        ahRep.update(ah);
                    } else {
                        // Sinon on set au troncon la valeur null pour
                        // l'identifiant AH et on set à l'ancienne valeur de l'AH
                        // le null pour son linéaire.
                        troncon.setAmenagementHydrauliqueId(null);
                        if (oldValue != null) {
                            AmenagementHydraulique oldAh = ahRep.get(oldValue.getElementId());
                            oldAh.setLinearId(null);
                            ahRep.update(oldAh);
                        }
                    }
                    session.getRepositoryForClass(TronconDigue.class).update(troncon);
                }
            });
        }
    }
}
