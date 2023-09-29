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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

public class ModelHelper extends Helper {

    private boolean generateEquals;
    boolean isStub = false;

    public ModelHelper(EObject eObject, String pack, boolean generateEquals) {
        this(eObject, pack, generateEquals, false);
    }

    public ModelHelper(EObject eObject, String pack, boolean generateEquals, boolean isStub) {
        super(eObject);
        this.isStub = isStub;
        this.pakage = pack;
        this.generateEquals = generateEquals;
        stripedPackages.add(pack + ".");
        stripedPackages.add(((EPackage) eObject.eContainer()).getName() + '.');
        stripedPackages.add("java.lang.");

        for (Iterator<EStructuralFeature> iterator = eClass.getEAllStructuralFeatures().iterator(); iterator.hasNext();) {
            EStructuralFeature eStructuralFeature = iterator.next();
            if(eStructuralFeature.isMany()) {
                imports.add("java.util.List");
                imports.add("java.util.ArrayList");
                imports.add("javafx.collections.*");
                break;
            }
        }

        imports.add("fr.sirs.util.property.Internal");
        imports.add("javafx.beans.value.ChangeListener");
        imports.add("javafx.beans.value.ObservableValue");
        imports.add("java.time.LocalDate");
        imports.add("com.vividsolutions.jts.geom.Geometry");

        for (EStructuralFeature att : eClass.getEStructuralFeatures()) {
            handleFXReferences(att);
            EClassifier eType = att.getEType();
            if (eType == null) {
                throw new RuntimeException(att.getName() + " in "
                        + eClass.getName() + " has no type defined");
            }
            String className = eType.getInstanceTypeName();
            if (className == null) {
                className = att.getEType().getName();
            }
            imports.add("javafx.beans.property.*");
            switch (className) {
            case "java.lang.String":
                propertyDeclarations.put("java.lang.String", "StringProperty");
                propertyImplementations.put("java.lang.String", "SimpleStringProperty");
                break;
            case "java.lang.Boolean":
                propertyDeclarations.put("java.lang.Boolean", "BooleanProperty");
                propertyImplementations.put("java.lang.Boolean","SimpleBooleanProperty");
            case "boolean":
                propertyDeclarations.put("boolean", "BooleanProperty");
                propertyImplementations.put("boolean", "SimpleBooleanProperty");

                break;
            case "java.lang.Integer":
            case "int":
                propertyDeclarations.put("java.lang.Integer", "IntegerProperty");
                propertyDeclarations.put("int", "IntegerProperty");
                propertyImplementations.put("java.lang.Integer", "SimpleIntegerProperty");
                propertyImplementations.put("int", "SimpleIntegerProperty");

                break;
            case "java.lang.Float":
            case "float":
                propertyDeclarations.put("java.lang.Float", "FloatProperty");
                propertyDeclarations.put("float", "FloatProperty");
                propertyImplementations.put("java.lang.Float", "SimpleFloatProperty");
                propertyImplementations.put("float", "SimpleFloatProperty");

                break;
            case "java.lang.Double":
            case "double":
                propertyDeclarations.put("java.lang.Double", "DoubleProperty");
                propertyDeclarations.put("double", "DoubleProperty");
                propertyImplementations.put("java.lang.Double", "SimpleDoubleProperty");
                propertyImplementations.put("double", "SimpleDoubleProperty");

                break;
            case "java.util.List":
                propertyDeclarations.put("java.util.List", "List");
                propertyImplementations.put("java.util.List", "ArrayList");
            case "java.util.Date":
                imports.add("java.time.LocalDateTime");
                imports.add("java.time.LocalDate");
                imports.add("fr.sirs.util.json.LocalDateTimeDeserializer");
                imports.add("fr.sirs.util.json.LocalDateTimeSerializer");
                imports.add("fr.sirs.util.json.LocalDateDeserializer");
                imports.add("fr.sirs.util.json.LocalDateSerializer");
                imports.add("com.fasterxml.jackson.databind.annotation.JsonSerialize");
                imports.add("com.fasterxml.jackson.databind.annotation.JsonDeserialize");
                stripedPackages.add("java.time.");
                break;
            case "Geometry":
                imports.add("fr.sirs.util.json.GeometryDeserializer");
                imports.add("fr.sirs.util.json.GeometrySerializer");
                imports.add("com.vividsolutions.jts.geom.Geometry");
                imports.add("com.fasterxml.jackson.databind.annotation.JsonSerialize");
                imports.add("com.fasterxml.jackson.databind.annotation.JsonDeserialize");

                break;
            case "Point":
                imports.add("com.fasterxml.jackson.databind.annotation.JsonSerialize");
                imports.add("com.fasterxml.jackson.databind.annotation.JsonDeserialize");
                imports.add("fr.sirs.util.json.GeometryDeserializer");
                imports.add("fr.sirs.util.json.GeometrySerializer");
                imports.add("com.vividsolutions.jts.geom.Point");

            case "TypeRive":
                break;
            default:
                // System.err.println("hola: " + className);
            }
        }

        imports.add("javafx.beans.property.ObjectProperty");
        imports.add("javafx.beans.property.SimpleObjectProperty");
        imports.add("javafx.beans.property.StringProperty");
        imports.add("javafx.beans.property.SimpleStringProperty");
        imports.add("com.fasterxml.jackson.annotation.JsonIgnore");

        if (!isAbstract() && !isInterface()) {
            imports.add("org.springframework.beans.factory.config.ConfigurableBeanFactory");
            imports.add("org.springframework.context.annotation.Scope");
            imports.add("org.springframework.stereotype.Component");
            imports.add("com.fasterxml.jackson.annotation.JsonInclude");
            imports.add("com.fasterxml.jackson.annotation.JsonInclude.Include");
            imports.add("com.fasterxml.jackson.annotation.JsonTypeInfo");
            imports.add("com.fasterxml.jackson.annotation.JsonIgnoreProperties");
        }

        if (hasDocument()) {
            imports.add("java.util.*");
            imports.add("java.io.*");
            imports.add("com.fasterxml.jackson.annotation.JsonProperty");
            imports.add("org.ektorp.*");
            imports.add("org.ektorp.support.*");
            imports.add("org.ektorp.util.*");

            if (hasContainmentReferences()) {
                imports.add("com.fasterxml.jackson.annotation.JsonManagedReference");
            }
        } else {
            UUID.randomUUID().toString();
            imports.add("com.fasterxml.jackson.annotation.JsonBackReference");
            imports.add("com.fasterxml.jackson.annotation.JsonProperty");
            imports.add("java.util.UUID");
        }

        if (hasReference()) {
            imports.add("fr.sirs.util.property.Reference");
        }
    }

    public String getTableName() {
        return getClassName();
    }

    private boolean hasReference() {
        for (final EReference att : eClass.getEReferences()) {
            return true;
        }
        return false;
    }

    private boolean hasContainmentReferences() {
        for (EReference att : eClass.getEReferences()) {
            imports.add("fr.sirs.util.property.Reference");
            if (att.isContainment())
                return true;
        }
        return false;
    }

    private void handleFXReferences(EStructuralFeature att) {

        if (att instanceof EReference) {
            if (((EReference) att).isContainment()) {
                imports.add("com.fasterxml.jackson.annotation.JsonManagedReference");
            }
        }

        if (att.isMany()) {
            imports.add("java.util.List");
            imports.add("javafx.collections.*");
        }
    }

    public String getSerializer(EStructuralFeature att) {
        String className = getClassName(att);
        switch (className) {
        case "java.util.Date":
            if(att.getEAnnotation(ANNOTATION_LOCAL_DATE)!=null)
                return "\n    @JsonSerialize(using=LocalDateSerializer.class)";
            else if(att.getEAnnotation(ANNOTATION_LOCAL_DATE_TIME)!=null)
                return "\n    @JsonSerialize(using=LocalDateTimeSerializer.class)";
            else // DEFAULT LocalDateTime
                return "\n    @JsonSerialize(using=LocalDateTimeSerializer.class)";
        case "Geometry":
        case "Point":
            return "\n    @JsonSerialize(using=GeometrySerializer.class)";
        default:
            break;
        }
        return "";
    }

    public String getDeserializer(EStructuralFeature att) {
        String className = getClassName(att);
        switch (className) {
        case "java.util.Date":
            if(att.getEAnnotation(ANNOTATION_LOCAL_DATE)!=null)
                return "\n    @JsonDeserialize(using=LocalDateDeserializer.class)";
            else if(att.getEAnnotation(ANNOTATION_LOCAL_DATE_TIME)!=null)
                return "\n    @JsonDeserialize(using=LocalDateTimeDeserializer.class)";
            else // DEFAULT LocalDateTime
                return "\n    @JsonDeserialize(using=LocalDateTimeDeserializer.class)";
        case "Geometry":
        case "Point":
            return "\n    @JsonDeserialize(using=GeometryDeserializer.class)";
        default:
            break;
        }
        return "";
    }

    public String getFXPaneName() {
        if(isStub) return super.getFXPaneName()+"Stub";
        else return super.getFXPaneName();
    }

    public boolean isContained() {
        return !Helper.hasDocument(eClass);
    }

    public boolean hasDocument() {
        return Helper.hasDocument(eClass);
    }

    public boolean dupplicateCouchDBDocument() {
        return hasDocument()
                && !"org.ektorp.support.CouchDbDocument".equals(parent);
    }

    public String getType(EReference att) {
        return className(getClassName(att)) + ".class";
    }

    public List<EClass> getTypeTree() {
        final List<EClass> eClasses = new ArrayList<>();
        eClasses.add(eClass);
        for (final EClass e : eClass.getEAllSuperTypes()) {
            if (!POSITIONABLE_CLASS_NAME.equals(e.getName()) && !POSITIONABLE_VEGETATION_CLASS_NAME.equals(e.getName()))
                eClasses.add(e);
        }
        return eClasses;
    }

    public boolean extendsObjet() {
        return extendsObjet(eClass);
    }

    public boolean extendsObjet(EClass eClass) {
        EList<EClass> eAllSuperTypes = eClass.getEAllSuperTypes();
        for (EClass superEClass : eAllSuperTypes) {
            if (OBJET_CLASS_NAME.equals(superEClass.getName()))
                return true;
        }
        return false;
    }

    public boolean isPositionable() {
        return isPositionable(eClass);
    }

    public boolean isPositionableVegetation() {
        return isPositionableVegetation(eClass);
    }

    public boolean isObjet() {
        return eClass.getName().equals(OBJET_CLASS_NAME);
    }

    public boolean generateEquals() {
        return generateEquals;
    }

    private boolean isPositionable(EClass eClass) {
        EList<EClass> eAllSuperTypes = eClass.getEAllSuperTypes();
        for (EClass superEClass : eAllSuperTypes) {
            if (POSITIONABLE_CLASS_NAME.equals(superEClass.getName()))
                return true;
        }
        return false;
    }

    private boolean isPositionableVegetation(EClass eClass) {
        EList<EClass> eAllSuperTypes = eClass.getEAllSuperTypes();
        for (EClass superEClass : eAllSuperTypes) {
            if (POSITIONABLE_VEGETATION_CLASS_NAME.equals(superEClass.getName()))
                return true;
        }
        return false;
    }

    public boolean isAvecCommentaire() {
        if (eClass.getEStructuralFeature(COMMENTAIRE_FIELD_NAME)!=null) return true;
        final EList<EClass> eAllSuperTypes = eClass.getEAllSuperTypes();
        for (final EClass superEClass : eAllSuperTypes) {
            if (superEClass.getEStructuralFeature(COMMENTAIRE_FIELD_NAME)!=null) return true;
        }
        return false;
    }

    public static boolean isManaged(EAttribute eAtt) {
        if (COMMENTAIRE_FIELD_NAME.equals(eAtt.getName())) {
            return true;
        }
        switch (eAtt.getEType().getName()) {
            case "java.lang.Integer":
            case "java.lang.Long":
            case "EFloat":
            case "EDouble":
            case "EInt":
            case "EDate":
            case "EBoolean":
            case "EString":
            case "String":
                return true;
        }
        return false;
    }

    /**
     * Returns the name of the controller for the given StructuralFeature.
     * @param eAtt
     * @return the name of the controller or null if there is no controller for
     * the StructuralFeature.
     */
    public String getXControl(EStructuralFeature eAtt) {
        if(eAtt instanceof EAttribute){
            if(eAtt.isMany()) return null;
            else {
                if (null != eAtt.getName()) switch (eAtt.getName()) {
                    case CHEMIN_FIELD_NAME:
                        return "FXFileTextField";
                    case COMMENTAIRE_FIELD_NAME:
                    case SUITE_FIELD_NAME:
                    case EVOLUTION_FIELD_NAME:
                    case ETAT_OUVRAGE_COM_FIELD_NAME:
                    case ETAT_ACCESSOIRE_COM_FIELD_NAME:
                    case ETAT_GENIE_CIVIL_COM_FIELD_NAME:
                    case MANOEUVRE_OUVRAGE_COM_FIELD_NAME:
                        return "TextArea";
                    default:
                        break;
                }
                switch (eAtt.getEType().getName()) {
                case "java.lang.Integer":
                case "java.lang.Long":
                case "EFloat":
                case "EDouble":
                case "EInt":
                    return "Spinner";

                case "EDate":
                    if(eAtt.getEAnnotation(ANNOTATION_LOCAL_DATE)!=null)
                        return "DatePicker";
                    else if(eAtt.getEAnnotation(ANNOTATION_LOCAL_DATE_TIME)!=null)
                        return "FXDateField";
                    else // DEFAULT LocalDateTime
                        return "FXDateField";
                case "EBoolean":
                    return "CheckBox";
                }
            }
        }
        // Si c'est une référence
        else if (eAtt instanceof EReference) {
            if(Helper.isContainedSingleReference((EReference) eAtt)){
                return "FXComponentField";
            }
        }
        return "TextField";
    }

    public List<EClass> getAllChildrenClass() {

        TreeIterator<EObject> eAllContents = eClass.eContainer().eAllContents();
        List<EClass> ret = new ArrayList<>();
        while (eAllContents.hasNext()) {
            EObject next = eAllContents.next();
            if (next.equals(eClass))
                continue;
            if (next instanceof EClass) {
                EClass new_name = (EClass) next;
                if (new_name.isAbstract())
                    continue;
                if (eClass.isSuperTypeOf(new_name)) {
                    ret.add((EClass) next);
                }

            }
        }
        return ret;
    }

    public String getTableName(EReference ref) {
        return getClassName(ref);
    }

    public String getView(final EReference ref) {
        String className = getClassName(ref);
        return "repositoryFor" + className;
    }

    public String getDeducedInterfaces(){
        return getAvecLibelleInterface()
                + getAvecCommentaireInterface()
                + getAvecDateMajInterface()
                + getAvecBornesTemporellesInterface()
                + getAvecForeignParentInterface()
                + getAvecPhotosInterface();
    }

    public String getAvecLibelleInterface() {
        if (eClass.getEStructuralFeature(LIBELLE_FIELD_NAME) != null) {
            return ", " + AVEC_LIBELLE_INTERFACE_NAME;
        } else {
            return "";
        }
    }

    public String getAvecCommentaireInterface() {
        if (eClass.getEStructuralFeature(COMMENTAIRE_FIELD_NAME) != null) {
            return ", " + AVEC_COMMENTAIRE_INTERFACE_NAME;
        } else {
            return "";
        }
    }

    public String getAvecPhotosInterface() {
        final EStructuralFeature structuralFeature = eClass.getEStructuralFeature(PHOTOS_REFERENCE_NAME);
        if (structuralFeature != null) {
            final String generic;
            final EAnnotation annot = structuralFeature.getEAnnotation("photoType");
            if(annot !=null){
                final List<EObject> refs = annot.getReferences();
                if(refs!=null && refs.size()==1){
                    final EObject ref = refs.get(0);
                    if(ref instanceof EClass){
                        generic = ((EClass) ref).getName();
                    }
                    else generic = "Photo";
                }
                else generic = "Photo";
            }
            else generic = "Photo";
            return ", " + AVEC_PHOTOS_INTERFACE_NAME+"<"+generic+">";
        } else {
            return "";
        }
    }

    public String getAvecDateMajInterface() {
        if (eClass.getEStructuralFeature(DATE_MAJ_FIELD_NAME) != null) {
            return ", " + AVEC_DATE_MAJ_INTERFACE_NAME;
        } else {
            return "";
        }
    }

    public String getAvecBornesTemporellesInterface() {
        if (eClass.getEStructuralFeature(DATE_DEBUT_FIELD_NAME) != null
                && eClass.getEStructuralFeature(DATE_FIN_FIELD_NAME) != null) {
            return ", " + AVEC_BORNES_TEMPORELLES_INTERFACE_NAME;
        } else {
            return "";
        }
    }

    public String getAvecForeignParentInterface() {
        boolean hasForeignParent = false;
        for(final EReference eReference : eClass.getEAllReferences()){
            if(isForeignParentReference(eReference)){
                if(hasForeignParent) throw new UnsupportedOperationException("Only one foreing parent is supported.");
                else hasForeignParent = true;
            }
        }
        if (hasForeignParent) {
            return ", "+ AVEC_FOREIGN_PARENT_INTERFACE_NAME;
        } else {
            return "";
        }
    }

    private String elementInterfaces = null;
    public String getModelInterfaces() {
        if(elementInterfaces==null) {
            final List<EClass> interfaces = getInterfaces();
            elementInterfaces="";
            if(!interfaces.isEmpty()) {
                for(final EClass interf : interfaces){
                    elementInterfaces+=(interf.getName()+", ");
                }
                elementInterfaces = elementInterfaces.substring(0, elementInterfaces.length()-2);
            }
        }

        return elementInterfaces;
    }

    public EList<EOperation> getEOperations(){
        return eClass.getEOperations();
    }

    public EList<EOperation> getEAllOperations(){
        return eClass.getEAllOperations();
    }

    public String getOperationName(final EOperation op){
        return op.getName();
    }

    public String getJavaType(final EClassifier classifier){
        if(classifier==null){
            return "void";
        } else if("EString".equals(classifier.getName())){
            return "String";
        } else if("EDate".equals(classifier.getName())){
//            if(classifier.getEAnnotation(ANNOTATION_LOCAL_DATE_TIME)!=null)
//                return "LocalDateTime";
//            else if(classifier.getEAnnotation(ANNOTATION_LOCAL_DATE)!=null)
//                return "LocalDate";
//            else
                return "LocalDate";
        } else if("EReference".equals(classifier.getName())){
            return "String";
        } else if("Geometry".equals(classifier.getName())){
            return "Geometry";
        } else if("ObjectProperty".equals(classifier.getName())){
            return "ObjectProperty";
        } else{
            return "Type de retour non supporté";
        }
    }

    public String getReturnType(final EOperation operation){
        if(operation.getEAnnotation(ANNOTATION_FX_PROPERTY_OPERATION)!=null){
            if(operation.getEType()==null){
                return "void";
            } else if("EString".equals(operation.getEType().getName())){
                return "StringProperty";
            } else if("EReference".equals(operation.getEType().getName())){
                return "StringProperty";
            } else{
                return "Type de retour non supporté";
            }
        } else {
            return getJavaType(operation.getEType());
        }
    }

    public String getSignature(final EOperation operation){
        StringBuilder signature = new StringBuilder();
        signature.append(getReturnType(operation))
                .append(" ")
                .append(operation.getName())
                .append('(');

        final List<EParameter> parameters = operation.getEParameters();
        if(parameters!=null && !parameters.isEmpty()){
            EParameter parameter = parameters.get(0);
                signature
                        .append(getJavaType(parameter.getEType()))
                        .append(" ")
                        .append(parameter.getName());
            for (int i = 1 ; i < parameters.size(); i++) {
                parameter = parameters.get(i);
                signature.append(", ")
                        .append(getJavaType(parameter.getEType()))
                        .append(" ")
                        .append(parameter.getName());
            }
        }

        signature.append(')');
        return signature.toString();
    }

    public String getParentTableName() {

        /*
        On commencer par chercher dans les classes référencées par la classe,
        si l'une d'elle a une référence opposée qui est une composition.
        */
        final EList<EReference> eReferences = eClass.getEAllReferences();
        for (final EReference eReference : eReferences) {
            final EReference eOpposite = eReference.getEOpposite();
            if (eOpposite != null && eOpposite.isContainment()){
                return getTableName(eReference);
            }
        }

        // Sinon, on recherche dans tout le paquetage
        final EList<EObject> eContents = eClass.eContainer().eContents();
        /*
        Pour éviter de dispatcher les clefs étrangères vers plusieurs tables
         Il faut renvoyer faux les conteneurs dans les cas suivants :
        -> plusieurs conteneurs
        -> un seul conteneur mais abstrait
        */
        final List<EClass> candidates = new ArrayList<>();
        for (final EObject eObject : eContents) {

            /*
            Pour les classes uniquement : on se base sur la première classe
            trouvée qui référence la classe courante.
            */
            if (eObject instanceof EClass) {
                final EClass clzz = (EClass) eObject;
                for (final EReference eReference : clzz.getEReferences()) {
                    if (eReference.getEType().equals(eClass)){
                        candidates.add(clzz);
                    }
                }
            }
        }

        if(candidates.size()==1) return getTableName(candidates.get(0));
        else throw new RuntimeException("Could not determine parentTableName for: "  + eClass.getName());

    }

    private String getTableName(EClass clzz) {
        return clzz.getName();
//        return normalizeClassName(clzz.getName());
    }

    public boolean hasContainerConcreteAndUnique() {

        /*
        On commence par chercher dans les classes référencées par la classe,
        si l'une d'elle a une référence opposée qui est une composition.
        */
        final EList<EReference> eReferences = eClass.getEAllReferences();
        for (final EReference eReference : eReferences) {
            final EReference eOpposite = eReference.getEOpposite();
            if (eOpposite != null && eOpposite.isContainment()){
                return !eReference.getEReferenceType().isAbstract();
            }
        }

        // Sinon, on recherche dans tout le paquetage
        final EList<EObject> eContents = eClass.eContainer().eContents();
        /*
        Pour éviter de dispatcher les clefs étrangères vers plusieurs tables
         Il faut renvoyer faux les conteneurs dans les cas suivants :
        -> plusieurs conteneurs
        -> un seul conteneur mais abstrait
        */
        final List<EClass> candidates = new ArrayList<>();
        for (final EObject eObject : eContents) {

            /*
            Pour les classes uniquement : on se base sur la première classe
            trouvée qui référence la classe courante.
            */
            if (eObject instanceof EClass) {
                final EClass clzz = (EClass) eObject;
                for (final EReference eReference : clzz.getEReferences()) {
                    if (eReference.getEType().equals(eClass)){
                        candidates.add(clzz);
                    }
                }
            }
        }

        if(candidates.isEmpty())
            throw new RuntimeException("Could not determine if container is concrete for: " + eClass.getName());
        else if(candidates.size()==1)
            return !candidates.get(0).isAbstract();
        else return false;
    }

    public boolean hasBornesTemporelles() {
        return (eClass.getEStructuralFeature(DATE_DEBUT_FIELD_NAME) != null
                && eClass.getEStructuralFeature(DATE_FIN_FIELD_NAME) != null);
    }

    /**
     *
     * @param clazz
     * @return True if input class represents a SIRS Reference type, false otherwise.
     */
    public static boolean isReferenceType(EClass clazz) {
        if ("referencetype".equalsIgnoreCase(clazz.getName()))
            return true;
        else
            for (final EClass superType : clazz.getEAllSuperTypes()) {
                if ("referencetype".equalsIgnoreCase(superType.getName()))
                    return true;
            }

        return false;
    }
}
