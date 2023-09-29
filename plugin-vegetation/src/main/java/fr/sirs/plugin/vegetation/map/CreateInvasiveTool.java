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
package fr.sirs.plugin.vegetation.map;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.InvasiveVegetation;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.RefDensiteVegetation;
import fr.sirs.core.model.RefTypeInvasiveVegetation;
import fr.sirs.plugin.vegetation.PluginVegetation;
import fr.sirs.theme.ui.FXPositionableExplicitMode;
import fr.sirs.util.ResourceInternationalString;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.edition.AbstractEditionToolSpi;
import org.geotoolkit.gui.javafx.render2d.edition.EditionTool;

import static fr.sirs.plugin.vegetation.PluginVegetation.DEFAULT_INVASIVE_VEGETATION_TYPE;
import static fr.sirs.plugin.vegetation.map.EditVegetationUtils.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class CreateInvasiveTool extends CreateVegetationPolygonTool<InvasiveVegetation> {

    public static final Spi SPI = new Spi();

    //Add editable fields ticket redmine 7741
    private final ComboBox<Preview> comboTypeVegetation = new ComboBox<>();
    private final ComboBox<Preview>  densiteComboBox = new ComboBox<>();

    public static final class Spi extends AbstractEditionToolSpi{

        public Spi() {
            super("CreateInvasive",
                new ResourceInternationalString("fr/sirs/plugin/vegetation/bundle",
                        "fr.sirs.plugin.vegetation.map.CreateInvasiveTool.title",CreateInvasiveTool.class.getClassLoader()),
                new ResourceInternationalString("fr/sirs/plugin/vegetation/bundle",
                        "fr.sirs.plugin.vegetation.map.CreateInvasiveTool.abstract",CreateInvasiveTool.class.getClassLoader()),
                new Image("fr/sirs/plugin/vegetation/invasives.png"));
        }

        @Override
        public boolean canHandle(Object candidate) {
            return true;
        }

        @Override
        public EditionTool create(FXMap map, Object layer) {
            return new CreateInvasiveTool(map);
        }
    }

    public CreateInvasiveTool(FXMap map) {
        super(map,SPI, InvasiveVegetation.class);

        final Previews previewRepository = Injector.getSession().getPreviews();
        SIRS.initCombo(comboTypeVegetation, FXCollections.observableList(
            previewRepository.getByClass(RefTypeInvasiveVegetation.class)),
            previewRepository.get(DEFAULT_INVASIVE_VEGETATION_TYPE));
        SIRS.initCombo(densiteComboBox, FXCollections.observableList(
                        previewRepository.getByClass(RefDensiteVegetation.class)),
                null);

        final GridPane attributeGrid = new GridPane();
        attributeGrid.setHgap(2);
        attributeGrid.setVgap(2);

        attributeGrid.add(generateHeaderLabel(LABEL_TYPE_VEGETATION),0,0);
        attributeGrid.add(comboTypeVegetation,1,0);
        attributeGrid.add(generateHeaderLabel(LABEL_DENSITE),0,1);
        attributeGrid.add(densiteComboBox,1,1);

        final VBox center = (VBox) wizard.getCenter();
        center.getChildren().add(4,attributeGrid);
    }


    @Override
    void saveAction(final boolean saveInBase) {
        super.saveAction(false);

        vegetation.setTypeVegetationId(getElementIdOrnull(comboTypeVegetation));
        vegetation.setDensiteId(getElementIdOrnull(densiteComboBox));

        if (saveInBase) super.saveInBase();
    }

    @Override
    void reset() {
        super.reset();
        comboTypeVegetation.getSelectionModel().clearSelection();
        densiteComboBox.getSelectionModel().clearSelection();
    }

    @Override
    protected InvasiveVegetation newVegetation() {
        final InvasiveVegetation candidate = super.newVegetation();
        //classement indéfini
        candidate.setTypeVegetationId(DEFAULT_INVASIVE_VEGETATION_TYPE);

        /*
        Si on peut, on paramètre le traitement qui a été associé dans super.newVegetation();
        Il est nécessaire pour cela d'associer un identifiant de parcelle à la zone de végétation.
        */
        if(parcelle!=null && parcelle.getId()!=null){
            candidate.setParcelleId(parcelle.getId());
            PluginVegetation.paramTraitement(InvasiveVegetation.class, candidate, DEFAULT_INVASIVE_VEGETATION_TYPE);
        }
        return candidate;
    }

}
