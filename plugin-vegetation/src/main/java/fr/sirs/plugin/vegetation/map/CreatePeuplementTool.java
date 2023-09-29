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
package fr.sirs.plugin.vegetation.map;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.PeuplementVegetation;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.RefEtatSanitaireVegetation;
import fr.sirs.core.model.RefTypePeuplementVegetation;
import fr.sirs.plugin.vegetation.PluginVegetation;
import fr.sirs.util.ResourceInternationalString;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.edition.AbstractEditionToolSpi;
import org.geotoolkit.gui.javafx.render2d.edition.EditionTool;

import static fr.sirs.plugin.vegetation.PluginVegetation.DEFAULT_PEUPLEMENT_VEGETATION_TYPE;
import static fr.sirs.plugin.vegetation.map.EditVegetationUtils.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class CreatePeuplementTool extends CreateVegetationPolygonTool<PeuplementVegetation> {

    public static final Spi SPI = new Spi();

    //Add editable fields ticket redmine 7741
    private final ComboBox<Preview> comboTypeVegetation = new ComboBox<>();

    private final NumberTextField densiteField = new NumberTextField();
    private final NumberTextField hauteurField = new NumberTextField();
    private final NumberTextField diametreField = new NumberTextField();
    private final ComboBox<Preview> ui_etatSanitaireId = new ComboBox<>();

    public static final class Spi extends AbstractEditionToolSpi {

        public Spi() {
            super("CreatePeuplement",
                    new ResourceInternationalString("fr/sirs/plugin/vegetation/bundle",
                            "fr.sirs.plugin.vegetation.map.CreatePeuplementTool.title", CreatePeuplementTool.class.getClassLoader()),
                    new ResourceInternationalString("fr/sirs/plugin/vegetation/bundle",
                            "fr.sirs.plugin.vegetation.map.CreatePeuplementTool.abstract", CreatePeuplementTool.class.getClassLoader()),
                    new Image("fr/sirs/plugin/vegetation/peuplement.png"));
        }

        @Override
        public boolean canHandle(Object candidate) {
            return true;
        }

        @Override
        public EditionTool create(FXMap map, Object layer) {
            return new CreatePeuplementTool(map);
        }
    }

    public CreatePeuplementTool(FXMap map) {
        super(map, SPI, PeuplementVegetation.class);

        final Previews previewRepository = Injector.getSession().getPreviews();
        SIRS.initCombo(comboTypeVegetation, FXCollections.observableList(
                        previewRepository.getByClass(RefTypePeuplementVegetation.class)),
                previewRepository.get(DEFAULT_PEUPLEMENT_VEGETATION_TYPE));
        SIRS.initCombo(ui_etatSanitaireId, FXCollections.observableList(
                        previewRepository.getByClass(RefEtatSanitaireVegetation.class)),
                vegetation == null ? null : vegetation.getEtatSanitaireId());

        final GridPane attributeGrid = new GridPane();
        attributeGrid.setHgap(2);
        attributeGrid.setVgap(4);

        attributeGrid.add(generateHeaderLabel(LABEL_TYPE_VEGETATION),0,0);
        attributeGrid.add(comboTypeVegetation,1,0);
        attributeGrid.add(generateHeaderLabel(LABEL_DENSITE),0,1);
        attributeGrid.add(densiteField,1,1);
        attributeGrid.add(generateHeaderLabel(LABEL_HAUTEUR),0,2);
        attributeGrid.add(hauteurField,1,2);
        attributeGrid.add(generateHeaderLabel(LABEL_DIAMETRE),0,3);
        attributeGrid.add(diametreField,1,3);
        attributeGrid.add(generateHeaderLabel(LABEL_ETAT_SANITAIRE_ID),0,4);
        attributeGrid.add(ui_etatSanitaireId,1,4);

        final VBox center = (VBox) wizard.getCenter();
        center.getChildren().add(4,attributeGrid);
    }

    @Override
    void saveAction(final boolean saveInBase) {
        super.saveAction(false);

        vegetation.setTypeVegetationId(getElementIdOrnull(comboTypeVegetation));
        final Double densite = densiteField.getValue();
        final Double hauteur = hauteurField.getValue();
        final Double diametre = diametreField.getValue();
        if (densite != null) vegetation.setDensite(densite);
        if (hauteur != null) vegetation.setHauteur(hauteur);
        if (diametre != null) vegetation.setDiametre(diametre);

        vegetation.setEtatSanitaireId(getElementIdOrnull(ui_etatSanitaireId));

        if (saveInBase) super.saveInBase();
    }

    @Override
    void reset() {
        super.reset();
        comboTypeVegetation.getSelectionModel().clearSelection();
        diametreField.replaceText(0, 1, "0");
        hauteurField.replaceText(0, 1, "0");
        densiteField.replaceText(0, 1, "0");
        ui_etatSanitaireId.getSelectionModel().clearSelection();
    }

    @Override
    protected PeuplementVegetation newVegetation() {
        final PeuplementVegetation candidate = super.newVegetation();

        //classement indéfini
        candidate.setTypeVegetationId(DEFAULT_PEUPLEMENT_VEGETATION_TYPE);

        /*
        Si on peut, on paramètre le traitement qui a été associé dans super.newVegetation();
        Il est nécessaire pour cela d'associer un identifiant de parcelle à la zone de végétation.
        */
        if (parcelle != null && parcelle.getId() != null) {
            candidate.setParcelleId(parcelle.getId());
            PluginVegetation.paramTraitement(PeuplementVegetation.class, candidate, DEFAULT_PEUPLEMENT_VEGETATION_TYPE);
        }

        return candidate;
    }

}
