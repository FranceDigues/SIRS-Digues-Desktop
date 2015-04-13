/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.sirs.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextInputControl;
import org.geotoolkit.gui.javafx.util.TextFieldCompletion;
import org.geotoolkit.internal.Loggers;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class DirectoryContent extends TextFieldCompletion {
    
    /**
     * A path which will be used as root when we will search paths available for
     * input text. If set, all choices returned by {@link #getChoices(java.lang.String) }
     * will be relative paths from specified root path.
     */
    public Path root;
    
    public DirectoryContent(final TextInputControl source) {
        super(source);
    }

    @Override
    protected ObservableList<String> getChoices(final String text) {
        final ArrayList<String> result = new ArrayList<>();

        try {
            final Path origin;
            if (root == null) {
                if (text == null) {
                    origin = Paths.get(System.getProperty("user.home"));
                } else {
                    origin = Paths.get(text);
                }
            } else {
                if (text == null) {
                    origin = root;
                } else {
                    origin = root.resolve(text);
                }
            }
            
            if (Files.isRegularFile(origin)) {
                result.add(toString(origin));
            } else if (Files.isDirectory(origin)) {
                Files.walk(origin, 1).forEach((final Path child) -> result.add(toString(child)));
            } else if (Files.isDirectory(origin.getParent())) {
                final String fileStart = origin.getFileName().toString().toLowerCase();
                Files.walk(origin.getParent(), 1)
                        .filter((final Path p) -> {
                            return (p.getFileName() != null && p.getFileName().toString().toLowerCase().startsWith(fileStart));
                        })
                        .forEach((final Path child) -> result.add(toString(child)));
            }
        } catch (Exception e) {
            Loggers.JAVAFX.log(Level.FINE, "Cannot find completion for input path", e);
        }
        return FXCollections.observableList(result);
    }
    
    protected String toString(Path p) {
        if (p == null) return "";
        return (root != null)? root.relativize(p).toString() : p.toString();
    }
}
