

package fr.sym.map;

import fr.sym.map.navigation.FXPanAction;
import fr.sym.map.navigation.FXZoomAllAction;
import fr.sym.map.navigation.FXZoomInAction;
import fr.sym.map.navigation.FXZoomNextAction;
import fr.sym.map.navigation.FXZoomOutAction;
import fr.sym.map.navigation.FXZoomPreviousAction;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import org.controlsfx.control.action.ActionUtils;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXNavigationBar extends ToolBar {

    private static final String LEFT   = "buttongroup-left";
    private static final String CENTER = "buttongroup-center";
    private static final String RIGHT  = "buttongroup-right";
    
    public FXNavigationBar(FXMap map) {
        getStylesheets().add("/fr/sym/buttonbar.css");
        
        final Button butAll = new FXZoomAllAction(map).createButton(ActionUtils.ActionTextBehavior.HIDE);
        final Button butPrevious = new FXZoomPreviousAction(map).createButton(ActionUtils.ActionTextBehavior.HIDE);
        final Button butNext = new FXZoomNextAction(map).createButton(ActionUtils.ActionTextBehavior.HIDE);
        butAll.getStyleClass().add(LEFT);
        butPrevious.getStyleClass().add(CENTER);
        butNext.getStyleClass().add(RIGHT);
        final HBox hboxAction = new HBox(butAll,butPrevious,butNext);
        
        final ToggleButton butIn = new FXZoomInAction(map).createToggleButton(ActionUtils.ActionTextBehavior.HIDE);
        final ToggleButton butOut = new FXZoomOutAction(map).createToggleButton(ActionUtils.ActionTextBehavior.HIDE);
        final ToggleButton butPan = new FXPanAction(map,false).createToggleButton(ActionUtils.ActionTextBehavior.HIDE);
        butIn.getStyleClass().add(LEFT);
        butOut.getStyleClass().add(CENTER);
        butPan.getStyleClass().add(RIGHT);
        final HBox hboxHandler = new HBox(butIn,butOut,butPan);
        
        
        getItems().add(hboxAction);
        getItems().add(hboxHandler);
        
    }
    
    
}
