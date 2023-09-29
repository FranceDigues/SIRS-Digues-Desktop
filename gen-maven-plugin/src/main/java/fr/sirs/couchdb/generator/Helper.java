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
package fr.sirs.couchdb.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

public class  Helper {

    public static final String REFERENCES_SUPER_TYPE = "ReferenceType";


    public static final String SYSTEME_REP_DZ_PROFIL_EN_LONG_REF = "systemeRepDzId";
    public static final String POINT_LEVE_DZ_PROFIL_EN_LONG_REF = "pointsLeveDZ";
    public static final String SYSTEME_REP_DZ_LIGNE_EAU_REF = "systemeRepDzId";
    public static final String MESURE_DZ_LIGNE_EAU_REF = "mesuresDZ";

    public static final String TRONCON_DIGUE_CLASS_NAME = "TronconDigue";
    public static final String POSITION_DOCUMENT_CLASS_NAME = "PositionDocument";
    public static final String POSITION_DOCUMENT_PROFIL_TRAVERS_CLASS_NAME = "PositionProfilTravers";
    public static final String POSITION_CONVENTION_CLASS_NAME = "PositionConvention";
    public static final String PROFIL_LONG_CLASS_NAME = "ProfilLong";
    public static final String LIGNE_EAU_CLASS_NAME = "LigneEau";
    public static final String OBJET_CLASS_NAME = "Objet";
    public static final String ABSTRACT_OBSERVATION_CLASS_NAME = "AbstractObservation";
    public static final String ABSTRACT_POSITION_DOCUMENT_CLASS_NAME = "AbstractPositionDocument";
    public static final String ABSTRACT_POSITION_DOCUMENT_ASSOCIABLE_CLASS_NAME = "AbstractPositionDocumentAssociable";
    public static final String POSITIONABLE_CLASS_NAME = "Positionable";
    public static final String POSITIONABLE_VEGETATION_CLASS_NAME = "PositionableVegetation";
    public static final String SIRS_DOCUMENT_CLASS_NAME = "SIRSDocument";
    public static final String PROFIL_TRAVERS_CLASS_NAME = "ProfilTravers";
    public static final String CONVENTION_CLASS_NAME = "Convention";
    public static final String PROPRIETE_TRONCON_CLASS_NAME = "ProprieteTroncon";
    public static final String GARDE_TRONCON_CLASS_NAME = "GardeTroncon";
    public static final String ABSTRACT_PHOTO_CLASS_NAME = "AbstractPhoto";
    public static final String ZONE_VEGETATION_CLASS_NAME = "ZoneVegetation";
    public static final String PARCELLE_VEGETATION_CLASS_NAME = "ParcelleVegetation";

    public static final String LINEAR_ID_FIELD_NAME = "linearId";
    public static final String LIBELLE_FIELD_NAME = "libelle";
    public static final String COMMENTAIRE_FIELD_NAME = "commentaire";
    public static final String CHEMIN_FIELD_NAME = "chemin";
    public static final String DATE_MAJ_FIELD_NAME = "dateMaj";
    public static final String DATE_DEBUT_FIELD_NAME = "date_debut";
    public static final String DATE_FIN_FIELD_NAME = "date_fin";
    public static final String DESIGNATION_FIELD_NAME = "designation";
    public static final String VALID_FIELD_NAME = "valid";
    public static final String AUTHOR_FIELD_NAME = "author";
    public static final String SUITE_FIELD_NAME = "suite";
    public static final String EVOLUTION_FIELD_NAME = "evolution";

    public static final String CLASS_BUNDLE_FIELD_NAME = "class";
    public static final String CLASS_PLURAL_BUNDLE_FIELD_NAME = "classPlural";
    public static final String CLASS_ABREGE_BUNDLE_FIELD_NAME = "classAbrege";

    public static final String SIRSDOCUMENT_REFERENCE_NAME = "sirsdocument";
    public static final String PARCELLE_ID_REFERENCE_NAME = "parcelleId";
    public static final String PHOTOS_REFERENCE_NAME = "photos";

    public static final String AVEC_LIBELLE_INTERFACE_NAME = "AvecLibelle";
    public static final String AVEC_COMMENTAIRE_INTERFACE_NAME = "AvecCommentaire";
    public static final String AVEC_PHOTOS_INTERFACE_NAME = "AvecPhotos";
    public static final String AVEC_DATE_MAJ_INTERFACE_NAME = "AvecDateMaj";
    public static final String AVEC_BORNES_TEMPORELLES_INTERFACE_NAME = "AvecBornesTemporelles";
    public static final String AVEC_FOREIGN_PARENT_INTERFACE_NAME = "AvecForeignParent";

    public static final String ANNOTATION_COUCHDB_DOCUMENT = "couchDBDocument";
    public static final String ANNOTATION_OWNER = "owner";
    public static final String ANNOTATION_IMPORT_POINT = "importPoint";
    public static final String ANNOTATION_FOREIGN_PARENT = "foreignParent";
    public static final String ANNOTATION_FX_PROPERTY_OPERATION = "fxProperty";
    public static final String ANNOTATION_LOCAL_DATE = "localDate";
    public static final String ANNOTATION_LOCAL_DATE_TIME = "localDateTime";

    public static final String FOREIGN_PARENT_GETTER = "getForeignParentId";
    public static final String FOREIGN_PARENT_SETTER = "setForeignParentId";

    public static final String POSITION_DOCUMENT_UI = "ui_positionDocument";
    public static final String POSITION_DOCUMENT_UI_TAB_TITLE = "Positions";
    public static final String POSITION_CONVENTION_UI_TAB_TITLE = "Objets associés";
    public static final String PHOTO_UI = "ui_photo";
    public static final String COMMENTAIRE_UI = "ui_"+COMMENTAIRE_FIELD_NAME;
    public static final String CHEMIN_UI = "ui_"+CHEMIN_FIELD_NAME;

    public static final String ZONE_VEGETATION_UI = "ui_zoneVegetation";

    public static final String ETAT_OUVRAGE_COM_FIELD_NAME = "etatOuvrageCom";
    public static final String ETAT_ACCESSOIRE_COM_FIELD_NAME = "etatAccessoireCom";
    public static final String ETAT_GENIE_CIVIL_COM_FIELD_NAME = "etatGenieCivilCom";
    public static final String MANOEUVRE_OUVRAGE_COM_FIELD_NAME = "manoeuvreOuvrageCom";

    public static final String OBJET_TYPE = "Objet";
    public static final String LINEAR_ID_UI = "ui_"+LINEAR_ID_FIELD_NAME;

    protected String pakage;

    protected Map<String, String> propertyDeclarations = new HashMap<>();

    protected Map<String, String> propertyImplementations = new HashMap<>();

    protected List<String> stripedPackages = new ArrayList<>();

    public EClass eClass;

    protected HashSet<String> imports = new HashSet<>();

    protected final  String className;

    /**
     * Super Type.
     */
    protected String parent;

    public Helper(EObject eObject) {
        this.eClass = (EClass) eObject;
        className = className(eClass.getName());

        EList<EClass> eSuperTypes = eClass.getESuperTypes();
        if (!eSuperTypes.isEmpty()) {
            for (final EClass eSuperType : eSuperTypes) {
                if (!eSuperType.isInterface()) {
                    parent = className(eSuperType.getName());
                    break; // Only one parent class in Java !
                }
            }
        }
    }

    public String getClassName() {
        return className;
    }

    public String getFXPaneName() {
        return "FX"+className+"Pane";
    }

    public String getInstanceName() {
        return lcFirst(className);
    }

    public EList<EAttribute> getEAttributes() {
        return eClass.getEAttributes();
    }

    public EList<EAttribute> getEAllAttributes() {
        return eClass.getEAllAttributes();
    }

    public List<EAttribute> getAllSingleAttributes() {
        final List<EAttribute> ret = new ArrayList<>();
        for(final EAttribute eAtt : eClass.getEAllAttributes()) {
            if(!eAtt.isMany()) ret.add(eAtt);
        }
        return ret;
    }

    public List<EClass> getInterfaces(){
        final List<EClass> interfaces = new ArrayList<>();
        for(final EClass candidate : eClass.getEAllSuperTypes()){
            if(candidate.isInterface()) interfaces.add(candidate);
        }
        return interfaces;
    }

    public List<EReference> getEReferencesToImplement(){
        final List<EReference> referencesToImplement = new ArrayList<>();
        for(final EClass interf : getInterfaces()){
            referencesToImplement.addAll(interf.getEReferences());
        }
        return referencesToImplement;
    }

    public List<EReference> getEReferences() {
        final EList<EReference> tmpRefs = eClass.getEReferences();
        final List<EReference> result = new ArrayList<>();
        for (EReference tmp : tmpRefs) {
            final EReference oppositeRef = tmp.getEOpposite();
            if (oppositeRef == null || !oppositeRef.isContainment()) {
                result.add(tmp);
            }
        }
        return result;
    }

    public EList<EReference> getEAllReferences() {
        return eClass.getEAllReferences();
    }

    /**
     * @param returnAbstract If true, return references of both concrete and abstract types.
     * If false, returns only references to concrete types.
     *
     * @return all references from {@link #eClass} and its inherited classes which
     * are unique ([0..n] without aggregation / composition).
     */
    public List<EReference> getAllSingleReferences(final boolean returnAbstract) {
        final List<EReference> ret = new ArrayList<>();
        for(final EReference eRef : eClass.getEAllReferences()) {
            if(eRef.isContainment()) continue;
            else if(eRef.getEReferenceType().isAbstract() && !returnAbstract) continue;
            else if(!eRef.isMany()) ret.add(eRef);
        }
        return ret;
    }

    /**
     * @return all references from {@link #eClass} and its inherited classes which
     * are multiple and not abstract ([0..n] with aggregation / composition).
     */
    public List<EReference> getAllMultipleReferences() {
        List<EReference> ret = new ArrayList<>();
        for(EReference   eRef : eClass.getEAllReferences()) {
            if(eRef.getEReferenceType().isAbstract())
                continue;
            if(eRef.isMany())
                ret.add(eRef);
        }

        return ret;
    }

    public List<String> imports() {
        final List<String> ret = new ArrayList<>();
        ret.addAll(imports);
        Collections.sort(ret);
        return ret;
    }

    public String className(String className) {
        if("java.util.Date".equals(className))
            return "LocalDateTime";
        className = stripPackageFromClassName(className);
        return className;
    }

    public String className(final EStructuralFeature eStrucFeat) {
        final String className = getClassName(eStrucFeat);
        if("java.util.Date".equals(className)){
            if(eStrucFeat.getEAnnotation(ANNOTATION_LOCAL_DATE)!=null)
                return "LocalDate";
            else if(eStrucFeat.getEAnnotation(ANNOTATION_LOCAL_DATE_TIME)!=null)
                return "LocalDateTime";
            else // DEFAULT LocalDateTime
                return "LocalDateTime";
        }
        return stripPackageFromClassName(className);
    }

    public String getFXImplementation(EStructuralFeature att) {
        String clazzName = getClassName(att);
        if (propertyImplementations.containsKey(clazzName)) {
            if(att.getUpperBound()>1){
                return "FXCollections.observableArrayList";
            } else{
                return propertyImplementations.get(clazzName);
            }
        }
//        if (att.isMany()) {
//            return "--bservableList<" + className(att) + ">";
//        }
        return "SimpleObjectProperty<" + className(att) + ">";
    }

    public String getFXDeclaration(EStructuralFeature att) {
        final String clazzName = getClassName(att);
        if (propertyDeclarations.containsKey(clazzName)) {
            if(att.getUpperBound()>1){
                return "ObservableList<"+className(att)+">";
            } else{
                return propertyDeclarations.get(clazzName);
            }
        }
        if (att.isMany()) {
            return "ObservableList<" + className(att) + ">";
        }
        return "ObjectProperty<" + className(att) + ">";
    }

    public static String getClassName(final EStructuralFeature att) {
        String clazzName = att.getEType().getInstanceTypeName();
        if (clazzName == null) {
            clazzName = att.getEType().getName();
        }
        return clazzName;
    }

    public String getAttributeClassName(final EStructuralFeature att, final boolean asParameter) {
        final String clazzName = className(att);

        if (att.isMany()) {
            if(asParameter) return "List<" + className(clazzName) + ">";
            return "ObservableList<" + className(clazzName) + ">";
        }
        return clazzName;
    }

    public boolean isPrimitiveType(final EStructuralFeature att){
        switch(getAttributeClassName(att, false)){
            case "boolean":
            case "char":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
                return true;
            default: return false;
        }
    }

    private String stripPackageFromClassName(String className) {
        for (String pkg : stripedPackages) {
            if (className.startsWith(pkg)) {
                className = className.substring(pkg.length());
            }
        }
        return className;
    }


//    public boolean isAbstractObservation() {
//            return isAbstractObservation(eClass);
//    }

    public static final boolean isAbstractObservation(EReference eRef) {
        EClass refClass = eRef.getEReferenceType();
        if (refClass == null)
            throw new IllegalStateException("Failed to identify EClass linked with the EReference : "+eRef.getName());
        EList<EClass> eAllSuperTypes = refClass.getEAllSuperTypes();
        for (EClass superEClass : eAllSuperTypes) {
            if (ABSTRACT_OBSERVATION_CLASS_NAME.equals(superEClass.getName()))
                return true;
        }
        return false;
    }

    public boolean isSIRSDocument(){
        final List<EClass> interfs = getInterfaces();
        for(final EClass interf : interfs){
            if(SIRS_DOCUMENT_CLASS_NAME.equals(interf.getName())) return true;
        }
        return false;
    }

    public boolean isParcelleVegetation(){
        return PARCELLE_VEGETATION_CLASS_NAME.equals(eClass.getName());
    }

    public String getPositionDocumentHeader(){
        if(isConvention()) return POSITION_CONVENTION_UI_TAB_TITLE;
        else return POSITION_DOCUMENT_UI_TAB_TITLE;
    }

    public static String getZonesVegetationHeader(){
        return "Zones de végétation";
    }

    public boolean isProfilTravers(){
        return PROFIL_TRAVERS_CLASS_NAME.equals(eClass.getName());
    }

    public boolean isConvention(){
        return CONVENTION_CLASS_NAME.equals(eClass.getName());
    }

    public boolean isProfilLong(){
        return PROFIL_LONG_CLASS_NAME.equals(eClass.getName());
    }

    public boolean isLigneEau(){
        return LIGNE_EAU_CLASS_NAME.equals(eClass.getName());
    }

    public boolean isPhoto(){
        final List<EClass> interfs = getInterfaces();
        for(final EClass interf : interfs){
            if(ABSTRACT_PHOTO_CLASS_NAME.equals(interf.getName())) return true;
        }
        return false;
    }

    public String getter(EStructuralFeature esf) {
        return "get" + ucFirst(esf.getName());
    }

    public String setter(EStructuralFeature esf) {
        return "set" + ucFirst(esf.getName());
    }

    public static String ucFirst(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static String lcFirst(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    public String getPackage() {
        return pakage;
    }

    public String getViewLabel() {
        return "_id";
    }

//    public boolean isConcrete() {
//        return !eClass.isAbstract();
//    }

    public boolean isAbstract() {
        return eClass.isAbstract();
    }

    public static boolean hasDocument(EClass eClass) {
        return eClass.getEAnnotation(ANNOTATION_COUCHDB_DOCUMENT) != null;
    }

    public boolean hasForeignParentReference(){
        for(final EClass eClazz : eClass.getEAllSuperTypes()){
            for(final EReference eRef : eClazz.getEAllReferences()){
                if(isForeignParentReference(eRef)) return true;
            }
        }
        return false;
    }

    public static boolean isOwnerReference(EReference reference) {
        return reference.getEAnnotation(ANNOTATION_OWNER) != null;
    }

    public static boolean isForeignParentReference(EReference reference) {
        return reference.getEAnnotation(ANNOTATION_FOREIGN_PARENT) != null;
    }

    public static boolean isImportPointReference(EReference reference) {
        return reference.getEAnnotation(ANNOTATION_IMPORT_POINT) != null;
    }

    public static boolean isReferenceTypeReference(EReference reference){
        return isReferenceType(reference.getEReferenceType());
    }

    public static boolean isReferenceType(EClass eClazz){
        final List<EClass> superTypes = eClazz.getEAllSuperTypes();
        for(final EClass superType : superTypes){
            if(REFERENCES_SUPER_TYPE.equals(superType.getName())) return true;
        }
        return false;
    }

    public static String lcFirst(EStructuralFeature esf) {
        return lcFirst(esf.getName());
    }

    public static String ucFirst(EStructuralFeature esf) {
        return ucFirst(esf.getName());
    }

    public boolean isInterface(){
        return(eClass.isInterface());
    }

    public String getExtends() {
        if(isInterface()){
            EList<EClass> eSuperTypes = eClass.getESuperTypes();
            if (!eSuperTypes.isEmpty()) {
                String result = "extends";
                for (final EClass eSuperType : eSuperTypes) {
                    result += " "+className(eSuperType.getName())+",";
                }
                return result.substring(0, result.length()-1);//remove the last coma.
            }
            else return "";
        }
        else if (parent != null) {
            return " extends " + parent;
        } else {
            return "";
        }
    }

    /**
     * Check if the input reference can be edited using a combo-box. The combo
     * box lists all elements of the repository the reference comes from.
     * To be valid, the reference must be a String representing a document Id.
     * @param ref
     * @return If a combo box can be used as editor, false otherwise.
     */
    public static boolean isComboBoxReference(final EReference ref) {
            // Sont exclus des combobox les éléments suivants :
            if(ref.isContainment() // 1) élément contenu par agrégation/composition
                    || (!Helper.hasDocument(ref.getEReferenceType()) && (!ref.getEReferenceType().isInterface() && !ref.getEReferenceType().isAbstract())) // 2) élément qui n'est pas un couchDBDocument et qui n'est pas une interface et qui n'est pas abstrait (car susceptible d'etre implémentée par un couchDBDocument)  // La condition sur les interface est ajoutée pour avoir accès aux classes implémentant l'interface des documents du SIRS
                    || ref.isMany()) // 3) Toute référence à cardinalité multiple
                return false;
//            if(!Helper.hasDocument(ref.getEReferenceType())) System.out.println("REFERENCE VERS UN ÉLÉMENT NON DOCUMENT : "+ref.getName()+" : "+ref.getEReferenceType().getName());
            return true;
    }

    /**
     * Check if input reference can be displayed as a table of bound values.
     * It's possible only if input reference is a list of identifiers.
     * @param ref
     * @return True if we can use a PojoTable to edit input reference.
     */
    public static boolean isTableReference(final EReference ref) {
        return ref.isMany();
    }

    /**
     * Check if we can make a simple link to another pane to edit input reference.
     * It's true only if input reference is an internal object of a document.
     * @param ref
     * @return True if we can put a link to an editor for input reference.
     */
    public static boolean isContainedSingleReference(final EReference ref) {
        return (ref.isContainment() && !ref.isMany());
    }

    public static boolean isContainedMultipleReference(final EReference ref) {
        return (ref.isContainment() && ref.isMany());
    }
}
