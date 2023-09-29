/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2016, FRANCE-DIGUES,
 * <p>
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.util;

import fr.sirs.Injector;
import fr.sirs.SIRS;

import static fr.sirs.SIRS.BUNDLE_KEY_CLASS_ABREGE;

import fr.sirs.Session;
import fr.sirs.core.model.AmenagementHydrauliqueView;
import fr.sirs.core.model.AvecLibelle;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Contact;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.Organisme;
import fr.sirs.core.model.PositionDocument;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.ReferenceType;
import fr.sirs.core.model.Role;

import static fr.sirs.core.model.Role.ADMIN;
import static fr.sirs.core.model.Role.EXTERN;
import static fr.sirs.core.model.Role.GUEST;
import static fr.sirs.core.model.Role.USER;

import fr.sirs.core.model.SQLQuery;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.index.ElementHit;
import fr.sirs.util.property.ShowCasePossibility;
import fr.sirs.util.property.SirsPreferences;
import java.lang.reflect.Method;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.StringConverter;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.feature.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Try to return a simple and readable name for any element given as argument.
 *
 * @author Alexis Manin
 * @author Johann Sorel
 */
public class SirsStringConverter extends StringConverter {

    private final WeakHashMap<String, Object> FROM_STRING = new WeakHashMap<>();

    public static final String LABEL_SEPARATOR = " : ";
    public static final String DESIGNATION_SEPARATOR = " - ";

    /**
     * Find a simple name for input object.
     * @param item The object to find a name for.
     * @return A title for input item, or a null or empty value, if none have been found.
     */
    @Override
    public String toString(Object item) {
        final ShowCasePossibility currentShowcase = SirsPreferences.INSTANCE.getShowCase();
        switch (currentShowcase) {
            case BOTH: //"Abstract : Designation" attendu
                return toString(item, true, true);
            case ABSTRACT:  //"Abstract" (only)
                return toString(item, true, false);
            case FULL_NAME://"Designation"/Nom complet seulement
                return toString(item, false, true);
            default:
                throw new IllegalStateException("Unexpected value for ShowCase preference : " + currentShowcase);
        }
    }


    public String toString(Object item, final boolean prefixed) {
        return toString(item, prefixed, true);
    }

    public String toString(Object item, final boolean prefixed, final boolean suffixed) {
        if (item instanceof SystemeReperageBorne) {
            final SystemeReperageBorne srb = (SystemeReperageBorne) item;
            final Session session = Injector.getBean(Session.class);
            item = session.getRepositoryForClass(BorneDigue.class).get(srb.getBorneId());
        }

        StringBuilder text = new StringBuilder();
        // Start title with element designation
        if (prefixed) {
            text.append(getDesignation(item));
            if (!suffixed) {
                return convertAndRegister(text, item);
            }
        }

        // Search for a name or label associated to input object
        if (item instanceof Contact) {
            final Contact c = (Contact) item;
            if (c.getNom() != null && !c.getNom().isEmpty()) {
                if (text.length() > 0) text.append(LABEL_SEPARATOR);
                text.append(c.getNom());
            }
            if (c.getPrenom() != null && !c.getPrenom().isEmpty()) {
                if (text.length() > 0) text.append(' ');
                text.append(c.getPrenom());
            }
        } else if (item instanceof Organisme) {
            final Organisme o = (Organisme) item;
            if (o.getNom() != null && !o.getNom().isEmpty()) {
                if (text.length() > 0) text.append(LABEL_SEPARATOR);
                text.append(o.getNom());
            }
        } else if (item instanceof ElementHit) {
            text.append(((ElementHit) item).getLibelle());
        } else if (item instanceof String) {
            text.append((String) item);
        } else if (item instanceof CoordinateReferenceSystem) {
            text.append(((CoordinateReferenceSystem) item).getName().toString());
        } else if (item instanceof PropertyType) {
            text = text.append(((PropertyType) item).getName().tip().toString());
        } else if (item instanceof Role) {
            if (item == ADMIN) text.append("Administrateur");
            else if (item == USER) text.append("Utilisateur");
            else if (item == GUEST) text.append("Invité");
            else if (item == EXTERN) text.append("Externe");
        } else if (item instanceof Class) {
            final LabelMapper mapper = LabelMapper.get((Class) item);
            if (mapper != null) {
                text.append(mapper.mapClassName());
            } else {
                text.append(((Class) item).getSimpleName());
            }
        } else if (item != null && item.getClass().isEnum()) {
            text.append(((Enum) item).name());
        } else if (item instanceof Character) {
            text.append(item);
        }

        // Whatever object we've got, if we can append a libelle, we do.
        if (item instanceof AvecLibelle) {
            final AvecLibelle libelle = (AvecLibelle) item;
            if (libelle.getLibelle() != null
                    && !libelle.getLibelle().isEmpty()) {
                if (text.length() > 0) text.append(LABEL_SEPARATOR);
                text.append(libelle.getLibelle());
            }
        }

        // Si on a un résultat vide, on retourne la désignation, même si on a
        // demandé un résultat non préfixé.
        if (!prefixed && text.length() == 0)
            text.append(getDesignation(item));

        return convertAndRegister(text, item);
    }

    /**
     * Convert input StringBuilder in String and register the (text,item) couple
     * in FROM_STRING WeakHashMap.
     *
     * @param text StringBuilder to convert.
     * @param item Object to associate with the returned String.
     */
    private String convertAndRegister(StringBuilder text, Object item) {
        final String result = text.toString();
        if (result != null && !result.isEmpty()) {
            FROM_STRING.put(result, item);
        }
        return result;
    }

    public void registerList(final List<? extends Object> objList) {
        for (Object obj : objList) {
            register(toString(obj), obj);
        }
    }

    public void register(final String text, Object item) {
        if (text != null && !text.isEmpty()) FROM_STRING.put(text, item);
    }

    private static String getDesignation(Object item) {
        if (item instanceof Element) {
            return getDesignation((Element) item);
        } else if (item instanceof Preview) {
            return getDesignation((Preview) item);
        } else if (item instanceof SQLQuery) {
            return ((SQLQuery) item).getLibelle();
        } else if (item instanceof AmenagementHydrauliqueView) {
            return getDesignation((AmenagementHydrauliqueView) item);
        } else return "";
    }

    public static String getDesignation(final Preview source) {
        ArgumentChecks.ensureNonNull("Preview to get designation for", source);
        final LabelMapper labelMapper = source.getElementClass() == null ? null : getLabelMapperForClass(source.getElementClass());
        String prefixedDesignation = (labelMapper == null) ? "" : labelMapper.mapPropertyName(BUNDLE_KEY_CLASS_ABREGE);
        if (source.getDesignation() != null) {
            if (!"".equals(prefixedDesignation)) {
                prefixedDesignation += DESIGNATION_SEPARATOR;
            }
            prefixedDesignation += source.getDesignation();
        }
        return prefixedDesignation;
    }

    public static String getDesignation(final Element source) {
        if (source instanceof ReferenceType) {
            try {
                final Method method = source.getClass().getMethod("getAbrege");
                method.setAccessible(true);
                return (String) method.invoke(source);
            } catch (ReflectiveOperationException | SecurityException e) {
                SIRS.LOGGER.log(Level.FINE, null, e);
                // No abrege available, use default method.
            }
        }

        final LabelMapper labelMapper = LabelMapper.get(source.getClass());
        String prefixedDesignation = (labelMapper == null) ? "" : labelMapper.mapPropertyName(BUNDLE_KEY_CLASS_ABREGE);
        if (source.getDesignation() != null) {
            if (!"".equals(prefixedDesignation)) {
                prefixedDesignation += DESIGNATION_SEPARATOR;
            }
            prefixedDesignation += source.getDesignation();
        }

        if (source instanceof PositionDocument) {
            if (((PositionDocument) source).getSirsdocument() != null) {
                final Preview preview = Injector.getSession().getPreviews().get(((PositionDocument) source).getSirsdocument());
                if (preview != null && preview.getElementClass() != null) {
                    try {
                        final LabelMapper documentLabelMapper = LabelMapper.get(Class.forName(preview.getElementClass(), true, Thread.currentThread().getContextClassLoader()));
                        if (documentLabelMapper != null) {
                            prefixedDesignation += " [" + documentLabelMapper.mapClassName() + "] ";
                        }
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(SirsStringConverter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        return prefixedDesignation;
    }

    public static String getDesignation(final AmenagementHydrauliqueView ahv) {
        String prefixedDesignation = "AH";

        if (ahv.getDesignation() != null) {
            prefixedDesignation += DESIGNATION_SEPARATOR;
            prefixedDesignation += ahv.getDesignation();
        }
        return prefixedDesignation;
    }

    @Override
    public Object fromString(String string) {
        return FROM_STRING.get(string);
    }

    /**
     *
     * @return the labelMapper for the given class name, or null if there is no
     * class for the given name.
     */
    private static LabelMapper getLabelMapperForClass(final String className) {
        try {
            return LabelMapper.get(Class.forName(className, true, Thread.currentThread().getContextClassLoader()));
        } catch (ClassNotFoundException ex) {
            SIRS.LOGGER.log(Level.WARNING, null, ex);
        }
        return null;
    }

}
