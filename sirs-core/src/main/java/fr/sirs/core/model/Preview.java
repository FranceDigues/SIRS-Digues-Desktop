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
package fr.sirs.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.sirs.core.SirsCore;
import fr.sirs.util.json.LocalDateDeserializer;
import fr.sirs.util.json.LocalDateSerializer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Preview implements AvecFinTemporelle, AvecLibelle, Comparable {

    @JsonProperty("docId")
    private String docId;

    @JsonProperty("docClass")
    private String docClass;

    @JsonProperty("elementId")
    private String elementId;

    @JsonProperty("elementClass")
    private String elementClass;

    @JsonProperty("author")
    private String author;

    @JsonProperty("valid")
    private boolean valid;

    private final SimpleStringProperty designationProperty = new SimpleStringProperty();

    private final SimpleStringProperty libelleProperty = new SimpleStringProperty();

    /**
     * @return the ID of the current element if its a document, or the Id of its
     * top-level container if target element is a contained element.
     */
    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getDocClass() {
        return docClass;
    }

    public void setDocClass(String docClass) {
        this.docClass = docClass;
    }

    /**
     * @return the ID of the target element.
     */
    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public String getElementClass() {
        return elementClass;
    }

    public void setElementClass(String elementClass) {
        this.elementClass = elementClass;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean getValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @JsonProperty("date_fin")
    private final ObjectProperty<LocalDate>  date_fin = new SimpleObjectProperty<>();
    @Override
    public ObjectProperty<LocalDate> date_finProperty() {
        return date_fin;
    }

    @JsonSerialize(using=LocalDateSerializer.class)
    public LocalDate getDate_fin() {
        return date_fin.get();
    }

    @JsonDeserialize(using= LocalDateDeserializer.class)
    public void setDate_fin(LocalDate date_fin) {
        this.date_fin.set(date_fin);
    }

    @JsonProperty("designation")
    public String getDesignation() {
        return designationProperty.get();
    }

    public void setDesignation(String designation) {
        this.designationProperty.set(designation);
    }

    public StringProperty designationProperty() {
        return designationProperty;
    }

    /**
     * Récupération de la classe de l'élément, ou bien en cas de problème (ClassNotFoundException) la classe indiquée en paramètre.
     * @param defaultClass
     * @return
     */
    public Class<?> getJavaClassOr(final Class<?> defaultClass) {
        try {
            return Class.forName(getElementClass(), false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException ex) {
            SirsCore.LOGGER.warning(String.format("unexpected class name %s. Replace by %s.", getElementClass(), defaultClass.getCanonicalName()));
            return defaultClass;
        }
    }

    @Override
    public String toString() {
        return "Preview{" +
                "docId='" + docId + '\'' +
                ", docClass='" + docClass + '\'' +
                ", elementId='" + elementId + '\'' +
                ", elementClass='" + elementClass + '\'' +
                ", author='" + author + '\'' +
                ", valid=" + valid +
                ", date_fin=" + date_fin +
                ", designationProperty=" + designationProperty +
                ", libelleProperty=" + libelleProperty +
                '}';
    }

    @Override
    public StringProperty libelleProperty() {
        return libelleProperty;
    }

    @Override
    public String getLibelle() {
        return libelleProperty.get();
    }

    @JsonProperty("libelle")
    @Override
    public void setLibelle(String libelle) {
        libelleProperty.set(libelle);
    }

    /**
     * Compare previews by class, designation, and finally libelle.
     * @param o The other preview to compare.
     */
    @Override
    public int compareTo(Object o) {
        if (o instanceof Preview) {
            final Preview other = (Preview) o;

            // Class comparison
            int classComparison;
            if (elementClass == null) {
                classComparison = other.elementClass == null ? 0 : 1;
            } else {
                classComparison = other.elementClass == null ? -1 : elementClass.compareTo(other.elementClass);
            }
            if (classComparison != 0) {
                return classComparison;
            }

            int designationComparison = -1;
            final String designation = getDesignation();
            final String otherDesignation = other.getDesignation();
            if (designation == null) {
                designationComparison = otherDesignation == null? 0 : 1;
            } else if (otherDesignation != null) {
                /* If both designation can be converted to numbers, we will
                 * perform a algebric comparison. Otherwise, we'll compare
                 * directly strings.
                 */
                try {
                    designationComparison = Integer.decode(designation).compareTo(Integer.decode(otherDesignation));
                } catch (NumberFormatException e) {
                    designationComparison = designation.compareTo(otherDesignation);
                }
            }

            if (designationComparison != 0) {
                return designationComparison;
            }

            return getLibelle() == null || other.getLibelle() == null ? 0
                    : getLibelle().compareTo(other.getLibelle());
        }

        return -1;
    }
}
