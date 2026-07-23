/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 3 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.gui.javafx.chooser;

import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.prefs.Preferences;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.owc.xml.OwcXmlIO;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * /!\ OVERRIDED FROM GEOTOOLKIT : We need a pointer to last saved/opened xml
 * file to reload it on application startup.
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXContextChooser extends BorderPane {

    private FXContextChooser(){
    }

    public static MapContext showOpenChooser(final Node parent) throws JAXBException, FactoryException, DataStoreException, NoninvertibleTransformException, TransformException{

        final Window owner = parent.getScene().getWindow();
        final FileChooser chooser = new FileChooser();
        chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("OWC Context", "xml"));

        final String prevPath = getPreviousPath();
        if (prevPath != null) {
            final File f = new File(prevPath);
            if(f.exists() && f.isDirectory()){
                chooser.setInitialDirectory(f);
            }
        }

        final File file = chooser.showOpenDialog(owner);
        if(file!=null){
            setPreviousPath(file.getParentFile().getAbsolutePath());
            final MapContext context = OwcXmlIO.read(file);
            setPreviousFile(file.getAbsolutePath());
            // Hack to remove old layers when still present in old contexts created before the Object deletion.
            // Names are taken from the key "class" from the corresponding .properties files
            final List<String> excludedLayers = Arrays.asList("Fondation de berge", "Talus Risberme Berge", "Crête de berge");
            context.items().stream()
                    .filter(i -> "Module berges".equals(i.getName()))
                    .map(mapItem ->
                            mapItem.items()
                    )
                    .flatMap(Collection::stream)
                    .filter(item -> "Autre (berge)".equals(item.getName()))
                    .map(autreBerge -> autreBerge.items())
                    .forEach(autreBerge -> {
                        autreBerge.removeIf(item -> excludedLayers.contains(item.getName()));
                    });

            return context;
        }

        return null;
    }

    public static File showSaveChooser(FXMap map) throws JAXBException, PropertyException, FactoryException, TransformException{

        final MapContext context = map.getContainer().getContext();
        final FileChooser chooser = new FileChooser();
        chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("OWC Context", "xml"));

        final String prevPath = getPreviousPath();
        if (prevPath != null) {
            final File f = new File(prevPath);
            if(f.exists() && f.isDirectory()){
                chooser.setInitialDirectory(f);
            }
        }

        final File file = chooser.showSaveDialog(null);

        if(file!=null){
            context.setAreaOfInterest(map.getCanvas().getVisibleEnvelope2D());
            setPreviousPath(file.getParentFile().getAbsolutePath());
            OwcXmlIO.write(file,context);
            setPreviousFile(file.getAbsolutePath());
        }

        return file;
    }

    public static String getPreviousFile() {
        final Preferences prefs = Preferences.userNodeForPackage(FXContextChooser.class);
        return prefs.get("file", null);
    }

    public static void setPreviousFile(final String file) {
        final Preferences prefs = Preferences.userNodeForPackage(FXContextChooser.class);
        prefs.put("file", file);
    }

    public static String getPreviousPath() {
        final Preferences prefs = Preferences.userNodeForPackage(FXContextChooser.class);
        return prefs.get("path", null);
    }

    public static void setPreviousPath(final String path) {
        final Preferences prefs = Preferences.userNodeForPackage(FXContextChooser.class);
        prefs.put("path", path);
    }
}
