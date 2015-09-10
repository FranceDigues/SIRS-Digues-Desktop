
package fr.sirs.plugin.berge.map;

import fr.sirs.core.model.Berge;
import fr.sirs.map.ConvertGeomToTronconHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;

/**
 *
 * @author guilhem
 */
public class ConvertGeomToBergeHandler extends ConvertGeomToTronconHandler {
    
    public ConvertGeomToBergeHandler(FXMap map) {
        super(map);
    }
    
    protected void init() {
        this.typeClass = Berge.class;
        this.typeName = "berge";
    }
}