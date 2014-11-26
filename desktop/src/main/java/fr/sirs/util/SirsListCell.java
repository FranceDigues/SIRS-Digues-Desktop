
package fr.sirs.util;

import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.query.ElementHit;
import javafx.scene.control.ListCell;
import org.opengis.feature.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * ListCell pour affichage de divers type :
 * - CoordinateReferenceSystem
 * - SystemeReperage
 * - BorneDigue
 * - PropertyType
 * 
 * @author Johann Sorel (Geomatys)
 */
public class SirsListCell extends ListCell {

    @Override
    protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText("");
            return;
        }

        if (item instanceof CoordinateReferenceSystem) {
            setText(((CoordinateReferenceSystem) item).getName().toString());
        } else if (item instanceof SystemeReperage) {
            setText(((SystemeReperage) item).getLibelle());
        } else if (item instanceof BorneDigue) {
            setText(((BorneDigue) item).getLibelle());
        } else if (item instanceof PropertyType) {
            setText(((PropertyType) item).getName().tip().toString());
        } else if(item instanceof ElementHit){
            setText(((ElementHit) item).getLibelle());
        } else {
            setText("");
        }

    }

}
