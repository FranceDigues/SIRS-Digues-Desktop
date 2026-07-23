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

import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.MetadataException;
import fr.sirs.SIRS;
import fr.sirs.core.model.Photo;
import fr.sirs.ui.Growl;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 * @author Samuel Andrés (Geomatys)
 */
public class FXPhotoPane extends FXPhotoPaneStub {

    // Propriétés de Positionable
    @FXML protected FXPositionablePane uiPositionable;

    @FXML protected ScrollPane ui_scroll_pane;
    @FXML protected StackPane ui_photo_stack;
    @FXML protected HBox ui_hbox_container;

    private boolean isVerticalPhoto = false;

    /**
     * Constructor. Initialize part of the UI which will not require update when element edited change.
     */
    private FXPhotoPane() {
        super();
        ui_scroll_pane.setMinWidth(USE_PREF_SIZE);
        ui_scroll_pane.setPrefWidth(USE_COMPUTED_SIZE);
        ui_scroll_pane.setBorder(Border.EMPTY);

        ui_hbox_container.setFillHeight(true);
        ui_photo.setPreserveRatio(true);
        ui_chemin.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (newValue != null) {
                final URI uri = ui_chemin.getURI();
                ui_photo.setImage(SIRS.getOrLoadImage(uri.toString()));
                ui_photo.minWidth(0);
                ui_photo.minHeight(0);
                Image image = ui_photo.getImage();
                if (image == null) {
                    new Growl(Growl.Type.WARNING, "Impossible de charger l'image. Veuillez vérifier le chemin d'accès.").showAndFade();
                    return;
                }
                double imageHeight;
                double imageWidht;
                try {
                    final SIRS.ImageInformation imageInformation = SIRS.readImageInformation(new File(uri.getPath()));
                    ui_photo.setRotate(imageInformation.rotation);
                    imageHeight = imageInformation.height;
                    imageWidht = imageInformation.width;
                } catch (IOException | MetadataException | ImageProcessingException e ) {
                    imageHeight = image.getHeight();
                    imageWidht = image.getWidth();
                }

                isVerticalPhoto = imageHeight > imageWidht;

                // Compute height when parent size or padding change.
                ui_photo.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> {
                    double height;
                    // HACK-REDMINE-7842 : limit the image height to the max height for portrait photos.
                    // Don't apply to landscape pictures otherwise the picture box will be smaller than expected.
                    final double rotate = ui_photo.getRotate();
                    final Insets padding = ui_photo_stack.getPadding();
                    if (isVerticalPhoto) {
                        if (rotate == 0 || rotate == 180) {
                            height = Math.min(ui_hbox_container.getHeight(), getHeight());
                            if (padding != null) {
                                height -= padding.getBottom() + padding.getTop();
                            }
                        } else {
                            // Image rotated with inversion of width and height
                            height = getWidth() - ui_scroll_pane.getWidth() - ui_hbox_container.getSpacing();
                            if (padding != null) {
                                height -= padding.getLeft() + padding.getRight();
                            }
                        }
                    } else {
                        height = ui_hbox_container.getHeight();
                        if (padding != null) {
                            height -= padding.getBottom() + padding.getTop();
                        }
                    }

                    return Math.max(0, height);
                }, ui_hbox_container.heightProperty(), ui_photo_stack.paddingProperty()));

                // Compute width when parent size or padding change.
                ui_photo.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> {
                    double width;
                    final double rotate = ui_photo.getRotate();
                    final Insets padding = ui_photo_stack.getPadding();
                    if (rotate == 0 || rotate == 180) {
                        width = getWidth() - ui_scroll_pane.getWidth() - ui_hbox_container.getSpacing();
                        if (padding != null) {
                            width -= padding.getLeft() + padding.getRight();
                        }
                    } else {
                        // Image rotated with inversion of width and height
                        width = Math.min(ui_hbox_container.getHeight(), getHeight());
                        if (padding != null) {
                            width -= padding.getBottom() + padding.getTop();
                        }
                    }
                    return Math.max(0, width);
                }, widthProperty(), ui_scroll_pane.widthProperty(), ui_hbox_container.spacingProperty()));
            } else {
                ui_photo.setImage(null);
            }
        });
    }

    public FXPhotoPane(final Photo photo) {
        this();
        this.elementProperty().set(photo);
    }
}
