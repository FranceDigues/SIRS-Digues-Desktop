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

import fr.sirs.CorePlugin;
import fr.sirs.Injector;
import static fr.sirs.core.SirsCore.DIGUE_ID_FIELD;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.Contact;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Digue;
import fr.sirs.core.model.GestionObjet;
import fr.sirs.core.model.Observation;
import fr.sirs.core.model.ObservationOuvrageHydrauliqueAssocie;
import fr.sirs.core.model.Organisme;
import fr.sirs.core.model.OuvrageHydrauliqueAssocie;
import fr.sirs.core.model.Photo;
import fr.sirs.core.model.ProprieteObjet;
import fr.sirs.core.model.RefSecurite;
import fr.sirs.core.model.ReseauHydrauliqueFerme;
import fr.sirs.core.model.TronconDigue;
import static fr.sirs.util.AbstractJDomWriter.NULL_REPLACEMENT;
import static fr.sirs.util.JRDomWriterDesordreSheet.IMAGE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterOuvrageAssocieSheet.RESEAU_FERME_TABLE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterOuvrageAssocieSheet.DESORDRE_TABLE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterOuvrageAssocieSheet.SECURITE_ID_FIELD;
import static fr.sirs.util.JRDomWriterOuvrageAssocieSheet.LENGTH_FIELD;
import static fr.sirs.util.JRDomWriterOuvrageAssocieSheet.MANAGER_FIELD;
import static fr.sirs.util.JRDomWriterOuvrageAssocieSheet.OBSERVATION_TABLE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterOuvrageAssocieSheet.OBSERVATION_SPEC_TABLE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterOuvrageAssocieSheet.OWNER_FIELD;
import static fr.sirs.util.JRDomWriterOuvrageAssocieSheet.PHOTO_DATA_SOURCE;
import java.awt.Dimension;
import java.awt.Image;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import org.apache.sis.measure.Units;
import org.geotoolkit.display.MeasureUtilities;

/**
 * Source de données de remplissage des fiches de réseaux hydrauliques fermés.
 *
 * @author Samuel Andrés (Geomatys)
 */
public class OuvrageHydrauliqueAssocieDataSource extends ObjectDataSource<OuvrageHydrauliqueAssocie> {

    private static Logger LOGGER = Logger.getLogger(OuvrageHydrauliqueAssocieDataSource.class.getName());

    private static final NumberFormat DISTANCE_FORMAT = new DecimalFormat("0.00");

    private final boolean printLocationInsert;


    public OuvrageHydrauliqueAssocieDataSource(Iterable<OuvrageHydrauliqueAssocie> iterable, final boolean printLocationInsert) {
        super(iterable);
        this.printLocationInsert = printLocationInsert;
    }

    public OuvrageHydrauliqueAssocieDataSource(final Iterable<OuvrageHydrauliqueAssocie> iterable, final Previews previewLabelRepository, final boolean printLocationInsert){
        super(iterable, previewLabelRepository);
        this.printLocationInsert = printLocationInsert;
    }

    public OuvrageHydrauliqueAssocieDataSource(final Iterable<OuvrageHydrauliqueAssocie> iterable, final Previews previewLabelRepository, final SirsStringConverter stringConverter, final boolean printLocationInsert){
        super(iterable, previewLabelRepository, stringConverter);
        this.printLocationInsert = printLocationInsert;
    }

    @Override
    public Object getFieldValue(final JRField jrf) throws JRException {

        final String name = jrf.getName();

        if(DIGUE_ID_FIELD.equals(name)){
            if(currentObject!=null&&currentObject.getLinearId()!=null){
                final TronconDigue troncon = Injector.getSession().getRepositoryForClass(TronconDigue.class).get(currentObject.getLinearId());
                if(troncon!=null&&troncon.getDigueId()!=null){
                    return parsePropertyValue(troncon.getDigueId(), Digue.class, String.class);
                }
            }
            return null;
        }
        else if(LENGTH_FIELD.equals(name)){
            return DISTANCE_FORMAT.format(MeasureUtilities.calculateLenght(currentObject.getGeometry(),
                                Injector.getSession().getProjection(), Units.METRE));
        }
        else if(MANAGER_FIELD.equals(name)){
            
            // Recherche d'une période de gestion actuelle.
            GestionObjet todayManagemnt = null;
            final ObservableList<GestionObjet> objectManagemnts = currentObject.getGestions();
            if(objectManagemnts!=null){
                for(final GestionObjet currentManagemnt : objectManagemnts){
                    if(currentManagemnt.getDate_fin()==null // Pour être une potentielle période de gestion actuelle, la période de gestion courante ne doit pas avoir de date de fin. 
                            && (todayManagemnt==null // Si aucune période potentielle de gestion actuelle n'a été affectée, on affecte la période courante d'office.
                            || (todayManagemnt.getDate_debut()==null && currentManagemnt.getDate_debut()!=null) // Si on a affecté une période de gestion sans date de début, on la remplace dès qu'on trouve une période de gestion qui en a une (de manière à pouvoir l'utiliser pour comparer).
                            || (todayManagemnt.getDate_debut()!=null && currentManagemnt.getDate_debut()!=null 
                                    && todayManagemnt.getDate_debut().isAfter(todayManagemnt.getDate_debut())) // Sinon, on compare les dates de début si possible pour affecter la période la plus récente.
                            )){
                        todayManagemnt = currentManagemnt;
                    }
                }
            }
            
            // Si une période de gestion actuelle a été détectée, on recherche le nom de l'organisme correspondant.
            if(todayManagemnt!=null && todayManagemnt.getOrganismeId()!=null){
                final Organisme organisme = Injector.getSession().getRepositoryForClass(Organisme.class).get(todayManagemnt.getOrganismeId());
                if(organisme!=null && organisme.getNom()!=null){
                    return organisme.getNom();
                }
            }
            return NULL_REPLACEMENT;
        }
        else if(OWNER_FIELD.equals(name)){
            
            // Recherche d'une période de propriété actuelle.
            ProprieteObjet todayProperty = null;
            final ObservableList<ProprieteObjet> objectProperties = currentObject.getProprietes();
            if(objectProperties!=null){
                for(final ProprieteObjet currentProperty : objectProperties){
                    if(currentProperty.getDate_fin()==null // Pour être une potentielle période de propriété actuelle, la période de propriété courante ne doit pas avoir de date de fin. 
                            && (todayProperty==null // Si aucune période potentielle de propriété actuelle n'a été affectée, on affecte la période courante d'office.
                            || (todayProperty.getDate_debut()==null && currentProperty.getDate_debut()!=null) // Si on a affecté une période de propriété sans date de début, on la remplace dès qu'on trouve une période de propriété qui en a une (de manière à pouvoir l'utiliser pour comparer).
                            || (todayProperty.getDate_debut()!=null && currentProperty.getDate_debut()!=null 
                                    && todayProperty.getDate_debut().isAfter(todayProperty.getDate_debut())) // Sinon, on compare les dates de début si possible pour affecter la période la plus récente.
                            )){
                        todayProperty = currentProperty;
                    }
                }
            }
            
            // Si une période de propriété actuelle a été détectée, on recherche le nom de l'organisme ou du contact correspondant.
            if(todayProperty!=null){
                if(todayProperty.getOrganismeId()!=null){
                    final Organisme organisme = Injector.getSession().getRepositoryForClass(Organisme.class).get(todayProperty.getOrganismeId());
                    if(organisme!=null && organisme.getNom()!=null){
                        return organisme.getNom();
                    }
                }
                else if(todayProperty.getContactId()!=null){
                    final Contact contact = Injector.getSession().getRepositoryForClass(Contact.class).get(todayProperty.getContactId());
                    if(contact!=null){
                        String result = "";
                        if(contact.getPrenom()!=null){
                            result+=contact.getPrenom();
                        }
                        if(contact.getNom()!=null){
                            if(!result.isEmpty()) result+=" "; 
                            result+=contact.getNom();
                        }
                        if(!result.isEmpty()) return result;
                    }
                }
            }
            return NULL_REPLACEMENT;
        }
        else if(PHOTO_DATA_SOURCE.equals(name)){
            final List<Photo> photos = new ArrayList<>();
            for(final ObservationOuvrageHydrauliqueAssocie observation : currentObject.getObservations()){
                if(observation.getPhotos()!=null && !observation.getPhotos().isEmpty()){
                    photos.addAll(observation.getPhotos());
                }
            }
            if(currentObject.getPhotos()!=null && !currentObject.getPhotos().isEmpty()){
                photos.addAll(currentObject.getPhotos());
            }
            photos.sort(SirsComparator.PHOTO_COMPARATOR);
            return new ObjectDataSource<>(photos, previewRepository, stringConverter);
        }
        else if(OBSERVATION_TABLE_DATA_SOURCE.equals(name) || OBSERVATION_SPEC_TABLE_DATA_SOURCE.equals(name)){
            final ObservableList<ObservationOuvrageHydrauliqueAssocie> observations = currentObject.getObservations();
            observations.sort(SirsComparator.OBSERVATION_COMPARATOR);
            return new ObjectDataSource<>(observations, previewRepository, stringConverter);
        }
        else if(RESEAU_FERME_TABLE_DATA_SOURCE.equals(name)){

            final List<ReseauHydrauliqueFerme> reseauOuvrageList = Injector.getSession().getRepositoryForClass(ReseauHydrauliqueFerme.class).get(currentObject.getReseauHydrauliqueFermeIds());
            
            reseauOuvrageList.sort(SirsComparator.ELEMENT_COMPARATOR);
            return new ObjectDataSource<>(reseauOuvrageList, previewRepository, stringConverter);
        }
        else if(DESORDRE_TABLE_DATA_SOURCE.equals(name)){
            final List<JRDesordreTableRow> desordreRows = new ArrayList<>();
            for(final Desordre des : Injector.getSession().getRepositoryForClass(Desordre.class).getAll()){
                if(des.getOuvrageHydrauliqueAssocieIds().contains(currentObject.getId())){
                    final List<Observation> observations = des.getObservations();
                    for(final Observation obs : observations){
                        desordreRows.add(new JRDesordreTableRow(obs.getDate(), des.getDesignation(), obs.getDesignation(), obs.getUrgenceId(), des.getCommentaire(), des.getDate_fin()!=null));
                    }
                }
            }
            Collections.sort(desordreRows);
            return new ObjectDataSource<>(desordreRows, previewRepository, stringConverter);
        } else if (IMAGE_DATA_SOURCE.equals(name)) {
            if (this.printLocationInsert) {
                final Image img = CorePlugin.takePictureOfElement(currentObject, new Dimension(1750, 1080));
                if (img != null) {
                    return img;
                } else {
                    return noImage();
                }
            } else {
                return null;
            }
        } else if(SECURITE_ID_FIELD.equals(name)){
            if(currentObject != null && currentObject.getSecuriteId() != null){
                return parsePropertyValue(currentObject.getSecuriteId(), RefSecurite.class, String.class);
            }
            return null;
        }
        else return super.getFieldValue(jrf);
    }
}
