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
package fr.sirs.util;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.SirsCore;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.AbstractObservation;
import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.core.model.OuvrageHydrauliqueAssocie;
import fr.sirs.core.model.ReferenceType;
import fr.sirs.core.model.ReseauHydrauliqueFerme;
import fr.sirs.core.model.SIRSFileReference;
import fr.sirs.core.model.Utilisateur;
import fr.sirs.util.property.Reference;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @param <T>
 */
public class ObjectDataSource<T> implements JRDataSource {

    protected final Iterator<T> iterator;
    protected T currentObject;
    protected final Previews previewRepository;
    protected final SirsStringConverter stringConverter;

    protected boolean isPhoto = false;

    public ObjectDataSource(final Iterable<T> iterable) {
        this(iterable, null);
    }

    public ObjectDataSource(final Iterable<T> iterable, final Previews previewLabelRepository) {
        this(iterable, previewLabelRepository, null);
    }

    public ObjectDataSource(final Iterable<T> iterable, final Previews previewsRepository, final SirsStringConverter stringConverter) {
        ArgumentChecks.ensureNonNull("iterable", iterable);
        iterator = iterable.iterator();
        this.previewRepository = previewsRepository;
        this.stringConverter = stringConverter;
    }

    @Override
    public boolean next() throws JRException {
        if (iterator.hasNext()) {
            currentObject = iterator.next();
            isPhoto = (currentObject instanceof AbstractPhoto);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Object getFieldValue(final JRField jrf) throws JRException {
        final String name = jrf.getName();
        try {
            if (isPhoto && Image.class.isAssignableFrom(jrf.getValueClass())) {
                final Path docPath = SirsCore.getDocumentAbsolutePath((SIRSFileReference) currentObject);
                final javafx.scene.image.Image img;
                if (Files.isReadable(docPath)) {
                    img = SIRS.getOrLoadImage(docPath.toUri().toString());
                } else {
                    img = null;
                }

                if (img != null) {
                    return SwingFXUtils.fromFXImage(img, null);
                } else {
                    return noImage();
                }
            }

            final Method getter = currentObject.getClass().getMethod("get" + name.substring(0, 1).toUpperCase() + name.substring(1));
            //final Object propertyValue = getter.invoke(currentObject);
            Object propertyValue = getter.invoke(currentObject);

            //System.out.println(currentObject);
            if ((name.equals("commentaire")) && (propertyValue == null)) { // Mettre un enum à la place de commentaire à associer au reseauFields <-> JRColumnParameter

                Boolean instanceOfOuvrageHydrauliqueAssocie = currentObject instanceof OuvrageHydrauliqueAssocie;
                Boolean instanceOfReseauHydrauliqueFerme = currentObject instanceof ReseauHydrauliqueFerme;

                // S'il n'y a pas de commentaire on récupère, si elle existe la dernière observation
                // des ouvrages associés au réseau hydraulique fermé de la fiche
                // (respectivement des réseaux hydrauliques fermés associé à l'ouvrage hydraulique associé de la fiche)
                //
                // Note :  la demande ne concernait pas les fiches désordres : doit-on l'impliquer dans cette modification?
                // Les modifications réalisées devraient impliquer ces fiches (désordres) dans la rubrique 'réseaux et ouvrages'
                // Mais pas dans la rubrique voirie.
                if ((!(this instanceof ReseauHydrauliqueFermeDataSource)) && (!(this instanceof OuvrageHydrauliqueAssocieDataSource))
                        && (instanceOfOuvrageHydrauliqueAssocie || instanceOfReseauHydrauliqueFerme)) {

                    ObservableList<? extends AbstractObservation> observations;

                    if (instanceOfOuvrageHydrauliqueAssocie) {
                        observations = ((OuvrageHydrauliqueAssocie) currentObject).getObservations();

                    } else { //(instanceOfReseauHydrauliqueFerme)
                        observations = ((ReseauHydrauliqueFerme) currentObject).getObservations();
                    }

                    propertyValue = lastObservation(observations);

                }

            }

            if (propertyValue != null) {
                final Reference ref = getter.getAnnotation(Reference.class);
                if (ref != null) {
                    return parsePropertyValue(propertyValue, ref.ref(), jrf.getValueClass());
                } else if (SirsCore.AUTHOR_FIELD.equals(name) && propertyValue instanceof String) {
                    return Injector.getSession().getRepositoryForClass(Utilisateur.class).get((String) propertyValue).getLogin();
                } else {
                    return parsePropertyValue(propertyValue, null, jrf.getValueClass());
                }
            }
        } catch (Exception ex) {
            SIRS.LOGGER.log(Level.WARNING, "Impossible to print a field value.", ex);
        }

        //No field that match this name, looks like the feature type
        //used is not the exact one returned by the JasperReportservice.
        //This is not necessarly an error if for exemple someone ignore
        //some attribut from the template because he doesn't need them.
        return null;
    }

    /**
     * Extract information from input object to put it in an object of queried
     * type.
     *
     * @param propertyValue The object to get data from.
     * @param refClass If input object is a reference to an element, this class
     * give the pointed element type. Can be null.
     * @param outputClass The type of object to return.
     * @return Extracted information, or null if analysis failed.
     */
    protected Object parsePropertyValue(Object propertyValue, final Class refClass, final Class outputClass) {

        // 0- Cas des collections : on renvoie une liste imprimable constituée à partir du traitement de chaque élément de la collection.
        if (propertyValue instanceof Collection) {
            final PrintableArrayList resultList = new PrintableArrayList(propertyValue instanceof List);
            for (final Object data : (Collection) propertyValue) {
                resultList.add(parsePropertyValue(data, refClass, String.class));
            }

            return resultList;
        }

        // 1- Cas des "références" et des prévisualisations : il faut un traitement préalable pour récupérer la vraie valeur.
        if (refClass != null) {
            if (!refClass.isAssignableFrom(propertyValue.getClass()) && (propertyValue instanceof String)) {
                if (ReferenceType.class.isAssignableFrom(refClass)) {
                    propertyValue = Injector.getSession().getRepositoryForClass(refClass).get((String) propertyValue);
                } else {
                    propertyValue = previewRepository.get((String) propertyValue);
                }
            }
        }

        // 2a- Cas général des propriétés simples  : le type demandé correspond au type du champ.
        if (outputClass.isAssignableFrom(propertyValue.getClass())) {
            // Si la valeur est une chaîne de caractères, on force le remplacement de la police de caractères au cas où il s'agirait de HTML.
            if (propertyValue instanceof String) {
                return forceArialFontFace((String) propertyValue);
            } else {
                return propertyValue;
            }
        }

        // 2b- Cas des "références" et des prévisualisations : on confie la conversion en chaîne de caractères au convertisseur du SIRS.
        if (String.class.isAssignableFrom(outputClass)) {
            return stringConverter.toString(propertyValue);
        } // 2c- ?
        else {
            return ObjectConverters.convert(propertyValue, outputClass);
        }
    }

    /**
     * Force la police de caractère indiquée dans les champs formatés en HTML à
     * utiliser la fonte Arial partout où elle est précisée.
     *
     * @param value
     * @return
     */
    static String forceArialFontFace(final String value) {
        return value.replaceAll("font face=\"[^\"]*\"", "font face=\"Arial\"");
    }

    /**
     * Méthode retournant la dernière observation parmi une observable liste
     * d'objet héritant de la classe AbstractObservation
     *
     * @param observations
     * @return
     */
    static final String lastObservation(ObservableList<? extends AbstractObservation> observations) {
        if ((observations!=null) && (observations.size() > 0)) {
            
             //min car OBSERVATION_COMPARATOR classe les observations dans l'ordre inverse des dates (LocalDate)
            AbstractObservation lastObservation = Collections.min(observations, SirsComparator.OBSERVATION_COMPARATOR);
            
            Optional<LocalDate> observationDate = Optional.ofNullable(lastObservation.getDate());
            Optional<String> observationEvolution = Optional.ofNullable(lastObservation.getEvolution());

            if (observationEvolution.isPresent()) {
                return observationDate.map(obsDate
                        -> "Observation du "
                        + obsDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " : " + observationEvolution.get())
                        .orElse("Observation : " + observationEvolution.get());
            }else if(observationDate.isPresent()){
                return "Observation du "
                        + observationDate.get().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " : pas d'évolution renseignée.";
            }
        }

        return "Ni commentaire ni observation.";
    }

    public static BufferedImage noImage() {
        try {
            return javax.imageio.ImageIO.read(ObjectDataSource.class.getResource("/fr/sirs/images/imgNotFound.png"));
        } catch (IOException ex) {
            throw new RuntimeException("Missing resource: /fr/sirs/images/imgNotFound.png", ex);
        }
    }
}
