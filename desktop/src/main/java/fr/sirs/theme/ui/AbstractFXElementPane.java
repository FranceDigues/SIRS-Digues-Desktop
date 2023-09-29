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

import fr.sirs.Printable;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.BorderPane;

/**
 * Base implementation for {@link Element} editor. Simply provide element property
 * defined in {@link FXElementPane}.
 *
 * @author Alexis Manin (Geomatys)
 * @param <T> Type of element to edit.
 */
public abstract class AbstractFXElementPane<T extends Element> extends BorderPane implements FXElementPane<T>, Printable{

    protected final ObjectProperty<T> elementProperty = new SimpleObjectProperty<>();
    private final BooleanProperty editableProperty = new SimpleBooleanProperty();

    @Override
    public void setElement(T element) {
        elementProperty.set(element);
    }

    @Override
    public ObjectProperty<T> elementProperty() {
        return elementProperty;
    }

    @Override
    public BooleanProperty disableFieldsProperty() {
        return editableProperty;
    }

    @Override
    public ObjectProperty getPrintableElements() {
        return elementProperty;
    }

    @Override
    public String getPrintTitle() {
        final Element element = elementProperty.get();
        if(element==null){
            return "";
        }else{
            LabelMapper mapper = LabelMapper.get(element.getClass());
            return mapper.mapClassName();
        }
    }

    /**
     * Method called when closing the FXFreeTab containing this Pane.
     * <p>
     * This method is necessary as when reopening the element's pane,
     * a new tab is created with a brand new Pane and the old one remains existing with its listeners,
     * leading to as many calls to the listeners as the tab was closed/reopened
     * <p>
     * Method to be overriden in case more listeners are added in the FXPane of the element.
     */
    public void removeListenersBeforeClosingTab() {}

}
