/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2016, FRANCE-DIGUES,
 * <p>
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.theme.ui;

import com.vividsolutions.jts.geom.*;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.StructBeanSupplier;
import fr.sirs.core.SirsCore;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.model.*;
import fr.sirs.plugin.vegetation.PluginVegetation;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;

import fr.sirs.util.ConvertPositionableCoordinates;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.internal.GeotkFX;

import static fr.sirs.plugin.vegetation.PluginVegetation.*;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class ZoneVegetationPojoTable extends ListenPropertyPojoTable<String> {

    public ZoneVegetationPojoTable(String title, final ObjectProperty<? extends Element> container) {
        super(ZoneVegetation.class, title, container);
        setDeletor((Consumer<Element>) pojo -> {
            if(pojo instanceof ZoneVegetation) ((AbstractSIRSRepository) Injector.getSession().getRepositoryForClass(pojo.getClass())).remove(pojo);
        });
    }

    @Override
    protected StructBeanSupplier getStructBeanSupplier(){
        return new StructBeanSupplier(pojoClass, "documentId", () -> new ArrayList(uiTable.getSelectionModel().getSelectedItems()));
    }

    @Override
    protected ZoneVegetation createPojo() {

        final ZoneVegetation zone;

        final ChoiceStage stage = new ChoiceStage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();

        final Class<? extends ZoneVegetation> retrievedClass = stage.getRetrievedElement().get();
        if (retrievedClass != null) {
            //Création de la zone
            final AbstractSIRSRepository zoneVegetationRepo = Injector.getSession().getRepositoryForClass(retrievedClass);
            zone = (ZoneVegetation) zoneVegetationRepo.create();
            zone.setForeignParentId(getPropertyReference());
            zoneVegetationRepo.add(zone);
            getAllValues().add(zone);

            //Création du traitement associé
            zone.setTraitement(Injector.getSession().getElementCreator().createElement(TraitementZoneVegetation.class));

            // S'il s'agit d'une zone d'invasive ou de peuplement, il faut affecter le type par défaut et effectuer le paramétrage éventuel

            if (retrievedClass == PeuplementVegetation.class) {
                ((PeuplementVegetation) zone).setTypeVegetationId(DEFAULT_PEUPLEMENT_VEGETATION_TYPE);
                paramTraitement(PeuplementVegetation.class, (PeuplementVegetation) zone, DEFAULT_PEUPLEMENT_VEGETATION_TYPE);
            } else if (retrievedClass == InvasiveVegetation.class) {
                ((InvasiveVegetation) zone).setTypeVegetationId(DEFAULT_INVASIVE_VEGETATION_TYPE);
                paramTraitement(InvasiveVegetation.class, (InvasiveVegetation) zone, DEFAULT_INVASIVE_VEGETATION_TYPE);
            } else if (retrievedClass == ArbreVegetation.class) {
                zone.setGeometryType(GeometryType.PONCTUAL);
            }

        } else {
            zone = null;
        }
        return zone;
    }

    @Override
    protected void elementEdited(TableColumn.CellEditEvent<Element, Object> event) {
        final Element obj = event.getRowValue();
        if (obj != null) {
            ((AbstractSIRSRepository) Injector.getSession().getRepositoryForClass(obj.getClass())).update(obj);
        }
    }


    /**
     * Window allowing to define the type of ZoneVegetation at creation stage.
     */
    private static class ChoiceStage extends PojoTableComboBoxChoiceStage<Class<? extends ZoneVegetation>, Class<? extends ZoneVegetation>> {

        private ChoiceStage(){
            super();
            setTitle("Choix du type de zone");
            comboBox.setItems(PluginVegetation.zoneVegetationClasses());
            retrievedElement.bind(comboBox.getSelectionModel().selectedItemProperty());
        }
    }

    // Override method to force to compute the geometry with the specific parameters of ZoneVegetation such as distances from tronçon.
    @Override
    public void setOnPropertyCommit(final TableColumn.CellEditEvent<Element, Object> event) {
        ArgumentChecks.ensureNonNull("Event event", event);
        /*
         * We try to update data. If it's a failure, we store exception
         * to give more information to user. In all cases, a notification
         * is requested to inform user if its modification has succeded
         * or not.
         */
        Exception tmpError = null;
        final Object rowElement = event.getRowValue();
        if (rowElement == null) {
            return;
        }

        // Check / update value
        TablePosition<Element, Object> pos = event.getTablePosition();
        final TableColumn<Element, Object> col = pos.getTableColumn();
        ObservableValue<Object> value = col.getCellObservableValue(pos.getRow());
        if (value instanceof WritableValue) {
            final Object oldValue = value.getValue();
            final Object newValue = event.getNewValue();

            if (oldValue != null && oldValue.equals(newValue)) return;

            try {
                ((WritableValue) value).setValue(event.getNewValue());

                //On recalcule les coordonnées si la colonne modifiée correspond à une des propriétées de coordonnées géo ou linéaire.
                String modifiedPropretieName = ((PojoTable.PropertyColumn) col).getName();
                if ((event.getRowValue() != null) && (PositionableVegetation.class.isAssignableFrom(event.getRowValue().getClass()))) {
                    computeForModifiedPropertie((Positionable) event.getRowValue(), modifiedPropretieName);
                }

                elementEdited(event);
            } catch (Exception e) {
                SIRS.LOGGER.log(Level.WARNING, "Cannot update field.", e);
                tmpError = e;
                // rollback value in case of error.
                ((WritableValue) value).setValue(oldValue);
                event.getTableView().refresh(); // To ensure cell rendering is aware we've rollbacked data.
            }
        } else {
            tmpError = new IllegalStateException(new StringBuilder("Cannot affect read-only property in column ").append(col.getText()).append("from table ").append(titleProperty.get()).toString());
        }

        // Inform user
        final Exception error = tmpError;
        final String message = (error == null)
                ? "Le champs " + event.getTableColumn().getText() + " a été modifié avec succès"
                : "Erreur pendant la mise à jour du champs " + event.getTableColumn().getText();
        final ImageView graphic = new ImageView(error == null ? SIRS.ICON_CHECK_CIRCLE : SIRS.ICON_EXCLAMATION_TRIANGLE);
        final Label messageLabel = new Label(message, graphic);
        if (error == null) {
            showNotification(messageLabel);
        } else {
            final Hyperlink errorLink = new Hyperlink("Voir l'erreur");
            errorLink.setOnMouseClicked(linkEvent -> GeotkFX.newExceptionDialog(message, error).show());
            final HBox container = new HBox(5, messageLabel, errorLink);
            container.setAlignment(Pos.CENTER);
            container.setPadding(Insets.EMPTY);
            showNotification(container);
        }
    }

    /**
     * Méthode permettant de recalculer les coordonnées linéaires (ou
     * Géographiques) lorsqu'une propriété associée aux coordonnées
     * géographiques (respectivement Linéaires) a été modifiée.
     *
     * @param positionableToUpdate l'élément Positionable qui a (déjà!!) été
     * modifié
     * @param modifiedPropretieName le nom de la propriété modifiée : doit
     * correspondre à une (chaîne de caractère) constante de la classe
     * SirsCore.java (validée par le test unitaire
     * SirsCoreTest.test_Nom_Methodes_Positionable_Valides() )
     */
    public static void computeForModifiedPropertie(final Positionable positionableToUpdate, final String modifiedPropretieName) {
        ArgumentChecks.ensureNonNull("Positionable positionable", positionableToUpdate);
        ArgumentChecks.ensureNonNull("Propertie name modifiedPropertirName", modifiedPropretieName);

        //Si le PR a été modifié on ne permet pas le calcul des coordonnées. Pourra évoluer.
        if ((modifiedPropretieName.equals(SirsCore.PR_DEBUT_FIELD)) || (modifiedPropretieName.equals(SirsCore.PR_FIN_FIELD))) {
            throw new RuntimeException("Impossible de recalculer des coordonnées de position uniquement à partir des PR");
        }

        switch (modifiedPropretieName) {
            //Si c'est une coordonnées Géo qui a été modifiée on recalcule les coordonnées linéaires :
            case SirsCore.POSITION_DEBUT_FIELD:
            case SirsCore.POSITION_FIN_FIELD:

                buildGeometryCoord((ZoneVegetation) positionableToUpdate);
                break;

            //Si c'est une coordonnées Linéaire qui a été modifiée on recalcule les coordonnées géo :
            case SirsCore.BORNE_DEBUT_AVAL:
            case SirsCore.BORNE_FIN_AVAL:
            case SirsCore.BORNE_DEBUT_DISTANCE:
            case SirsCore.BORNE_FIN_DISTANCE:
            case SirsCore.BORNE_DEBUT_ID:
            case SirsCore.BORNE_FIN_ID:

                buildLinearGeometry((ZoneVegetation) positionableToUpdate);
                break;

            //Si c'est une distance au tronçon qui a été modifiée on recalcule les coordonnées géo ou linéaire en fonction du paramètre editedGeoCoordinateProperty:
            case SirsCore.DISTANCE_DEBUT_MIN:
            case SirsCore.DISTANCE_DEBUT_MAX:
            case SirsCore.DISTANCE_FIN_MIN:
            case SirsCore.DISTANCE_FIN_MAX:

                if (positionableToUpdate.getEditedGeoCoordinate())
                    buildGeometryCoord((ZoneVegetation) positionableToUpdate);
                else
                    buildLinearGeometry((ZoneVegetation) positionableToUpdate);

                break;
        }
    }

    public static void buildLinearGeometry(final ZoneVegetation zone) {
        String srId = zone.getSystemeRepId();

        // init SR - can be null when new zone de Végétation created but set from PojoTable.
        if (srId == null) {
            final TronconDigue t = ConvertPositionableCoordinates.getTronconFromPositionable(zone);
            zone.setSystemeRepId(t.getSystemeRepDefautId());
            srId = zone.getSystemeRepId();
            if (srId == null) {
                throw new IllegalStateException("Neither the zoneVegetation nor its troncon have a SystemeReperage.");
            }

        }

        final SystemeReperageRepository srRepo = (SystemeReperageRepository) Injector.getSession().getRepositoryForClass(SystemeReperage.class);
        final SystemeReperage sr = srRepo.get(srId);

        PluginVegetation.buildLinearGeometry(zone, sr, Mode.LINEAR_AREA);
    }

    public static void buildGeometryCoord(final ZoneVegetation zone) {
        Point startPoint = zone.getPositionDebut();
        Point endPoint = zone.getPositionFin();

        if (startPoint == null && endPoint == null) return;
        if (startPoint == null) startPoint = endPoint;
        if (endPoint == null) endPoint = startPoint;

        buildCoordGeometry(zone, startPoint, endPoint, Mode.COORD_AREA);
    }

}
