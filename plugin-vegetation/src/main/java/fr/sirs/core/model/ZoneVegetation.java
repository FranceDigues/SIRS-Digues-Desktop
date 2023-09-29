

package fr.sirs.core.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import fr.sirs.Injector;
import fr.sirs.util.SIRSAreaComputer;
import fr.sirs.util.SirsStringConverter;
import fr.sirs.util.json.LocalDateDeserializer;
import fr.sirs.util.json.LocalDateSerializer;
import fr.sirs.util.property.Computed;
import fr.sirs.util.property.Internal;
import fr.sirs.util.property.Reference;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javafx.beans.property.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.apache.sis.measure.Units;

public abstract class ZoneVegetation  extends PositionableVegetation
    implements Element , AvecBornesTemporelles, AvecForeignParent ,  AvecGeometrie, AvecCommentaire {

    private static final SirsStringConverter converter = new SirsStringConverter();
    /**
     * JavaFX property for commentaire.
     */
    private final StringProperty  commentaire = new SimpleStringProperty();
    /**
     * @return JavaFX observable property for commentaire.
     */
    public StringProperty commentaireProperty() {
        return commentaire;
    }
    public String getCommentaire(){
        return this.commentaire.get();
    }

    public void setCommentaire(String commentaire){
        this.commentaire.set(commentaire);
    }

    @Override
    @Internal
    @JsonIgnore
    public String getDocumentId(){
        return documentId;
    }

    private String documentId;

    @JsonProperty(required=false)
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    protected final ObjectProperty<Element> parent =  new SimpleObjectProperty<>();

    /**
     * @return the parent {@link Element} of the current element, or null if no parent is set.
     * Note that no CouchDB document has a parent property. Only contained elements have one.
     */
    @Override
    public ObjectProperty<Element> parentProperty() {
        return parent;
    }

    /**
     * @return the parent {@link Element} of the current element, or null if no parent is set.
     * Note that no CouchDB document has a parent property. Only contained elements have one.
     */
    @Override
    public Element getParent(){
       return parent.get();
    }

    @Override
    @JsonBackReference("parent")
    public void setParent(Element parent){
       this.parent.set(parent);
    }
    /**
    * JavaFX property for contactEau.
    */
    private final BooleanProperty  contactEau = new SimpleBooleanProperty();
    /**
    * @return JavaFX observable property for contactEau.
    */
    public BooleanProperty contactEauProperty() {
       return contactEau;
    }
    /**
    * JavaFX property for date_debut.
    */
    private final ObjectProperty<LocalDate>  date_debut = new SimpleObjectProperty<LocalDate>(LocalDateTime.now().toLocalDate());
    /**
    * @return JavaFX observable property for date_debut.
    */
    public ObjectProperty<LocalDate> date_debutProperty() {
       return date_debut;
    }
    /**
    * JavaFX property for date_fin.
    */
    private final ObjectProperty<LocalDate>  date_fin = new SimpleObjectProperty<LocalDate>();
    /**
    * @return JavaFX observable property for date_fin.
    */
    public ObjectProperty<LocalDate> date_finProperty() {
       return date_fin;
    }
    /**
     * JavaFX property for cartoEdited.
     */
    private final BooleanProperty  cartoEdited = new SimpleBooleanProperty();
    /**
     * @return JavaFX observable property for cartoEdited.
     */
    public BooleanProperty cartoEditedProperty() {
        return cartoEdited;
    }

    public final ObjectProperty<TraitementZoneVegetation>  traitement = new SimpleObjectProperty<>();

    {
        traitement.addListener(new ChangeListener<Element>() {

            @Override
            public void changed(ObservableValue<? extends Element> observable, Element oldValue, Element newValue) {
                if(newValue != null) {
                    newValue.setParent(ZoneVegetation.this);
                }
            }
        });
    }
    private final StringProperty typePositionId = new SimpleStringProperty();

    public StringProperty typePositionIdProperty() {
        return this.typePositionId;
    }
    private final StringProperty typeCoteId = new SimpleStringProperty();

    public StringProperty typeCoteIdProperty() {
        return this.typeCoteId;
    }

    public boolean getContactEau(){
        return this.contactEau.get();
    }


    public void setContactEau(boolean contactEau){
        this.contactEau.set(contactEau);
    }

    @JsonSerialize(using=LocalDateSerializer.class)
    public LocalDate getDate_debut(){
        return this.date_debut.get();
    }


    @JsonDeserialize(using=LocalDateDeserializer.class)
    public void setDate_debut(LocalDate date_debut){
        this.date_debut.set(date_debut);
    }

    @JsonSerialize(using=LocalDateSerializer.class)
    public LocalDate getDate_fin(){
        return this.date_fin.get();
    }


    @JsonDeserialize(using=LocalDateDeserializer.class)
    public void setDate_fin(LocalDate date_fin){
        this.date_fin.set(date_fin);
    }
    public boolean getCartoEdited(){
        return this.cartoEdited.get();
    }

    public void setCartoEdited(boolean cartoEdited){
        this.cartoEdited.set(cartoEdited);
    }

    @Internal
    @JsonManagedReference("parent")
    public TraitementZoneVegetation getTraitement(){
            return this.traitement.get();
        }

    public void setTraitement(TraitementZoneVegetation traitement){
            this.traitement.set( traitement );
        }

    @Reference(ref=RefPosition.class)
    public String getTypePositionId(){
        return this.typePositionId.get();
    }

    public void setTypePositionId(String typePositionId){
        this.typePositionId.set( typePositionId );
    }

    @Reference(ref=RefCote.class)
    public String getTypeCoteId(){
        return this.typeCoteId.get();
    }

    public void setTypeCoteId(String typeCoteId){
        this.typeCoteId.set( typeCoteId );
    }
        @Override
    public abstract ZoneVegetation copy();
    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder("[ZoneVegetation ");
        builder.append("borne_debut_aval: ");
        builder.append(getBorne_debut_aval());
        builder.append(", ");
        builder.append("borne_debut_distance: ");
        builder.append(getBorne_debut_distance());
        builder.append(", ");
        builder.append("positionDebut: ");
        builder.append(getPositionDebut());
        builder.append(", ");
        builder.append("prDebut: ");
        builder.append(getPrDebut());
        builder.append(", ");
        builder.append("borne_fin_aval: ");
        builder.append(getBorne_fin_aval());
        builder.append(", ");
        builder.append("borne_fin_distance: ");
        builder.append(getBorne_fin_distance());
        builder.append(", ");
        builder.append("positionFin: ");
        builder.append(getPositionFin());
        builder.append(", ");
        builder.append("prFin: ");
        builder.append(getPrFin());
        builder.append(", ");
        builder.append("geometry: ");
        builder.append(getGeometry());
        builder.append(", ");
        builder.append("valid: ");
        builder.append(getValid());
        builder.append(", ");
        builder.append("author: ");
        builder.append(getAuthor());
        builder.append(", ");
        builder.append("designation: ");
        builder.append(getDesignation());
        builder.append(", ");
        builder.append("longitudeMin: ");
        builder.append(getLongitudeMin());
        builder.append(", ");
        builder.append("longitudeMax: ");
        builder.append(getLongitudeMax());
        builder.append(", ");
        builder.append("latitudeMin: ");
        builder.append(getLatitudeMin());
        builder.append(", ");
        builder.append("latitudeMax: ");
        builder.append(getLatitudeMax());
        builder.append(", ");
        builder.append("geometryMode: ");
        builder.append(getGeometryMode());
        builder.append(", ");
        builder.append("geometryType: ");
        builder.append(getGeometryType());
        builder.append(", ");
        builder.append("explicitGeometry: ");
        builder.append(getExplicitGeometry());
        builder.append(", ");
        builder.append("distanceDebutMin: ");
        builder.append(getDistanceDebutMin());
        builder.append(", ");
        builder.append("distanceDebutMax: ");
        builder.append(getDistanceDebutMax());
        builder.append(", ");
        builder.append("distanceFinMin: ");
        builder.append(getDistanceFinMin());
        builder.append(", ");
        builder.append("distanceFinMax: ");
        builder.append(getDistanceFinMax());
        builder.append(", ");
        builder.append("contactEau: ");
        builder.append(getContactEau());
        builder.append(", ");
        builder.append("date_debut: ");
        builder.append(getDate_debut());
        builder.append(", ");
        builder.append("date_fin: ");
        builder.append(getDate_fin());
        builder.append(", ");
        builder.append("commentaire: ");
        builder.append(getCommentaire());
        builder.append(", ");
        builder.append("cartoEdited: ");
        builder.append(getCartoEdited());
        return builder.toString();
    }

    @Override
    public boolean contentBasedEquals(Element element) {
        if(element instanceof ZoneVegetation) {

            final ZoneVegetation other = (ZoneVegetation) element;
            if (getId() == null) {
                if (other.getId() != null) return false;
            } else if (!getId().equals(other.getId())) return false;
            if (this.getBorne_debut_aval() != other.getBorne_debut_aval()) return false;
            if (this.getBorne_debut_distance() != other.getBorne_debut_distance()) return false;
            if ((this.getPositionDebut()==null ^ other.getPositionDebut()==null) || ( (this.getPositionDebut()!=null && other.getPositionDebut()!=null) && !this.getPositionDebut().equals(other.getPositionDebut()))) return false;
            if (this.getPrDebut() != other.getPrDebut()) return false;
            if (this.getBorne_fin_aval() != other.getBorne_fin_aval()) return false;
            if (this.getBorne_fin_distance() != other.getBorne_fin_distance()) return false;
            if ((this.getPositionFin()==null ^ other.getPositionFin()==null) || ( (this.getPositionFin()!=null && other.getPositionFin()!=null) && !this.getPositionFin().equals(other.getPositionFin()))) return false;
            if (this.getPrFin() != other.getPrFin()) return false;
            if ((this.getGeometry()==null ^ other.getGeometry()==null) || ( (this.getGeometry()!=null && other.getGeometry()!=null) && !this.getGeometry().equals(other.getGeometry()))) return false;
            if (this.getValid() != other.getValid()) return false;
            if ((this.getAuthor()==null ^ other.getAuthor()==null) || ( (this.getAuthor()!=null && other.getAuthor()!=null) && !this.getAuthor().equals(other.getAuthor()))) return false;
            if ((this.getDesignation()==null ^ other.getDesignation()==null) || ( (this.getDesignation()!=null && other.getDesignation()!=null) && !this.getDesignation().equals(other.getDesignation()))) return false;
            if (this.getLongitudeMin() != other.getLongitudeMin()) return false;
            if (this.getLongitudeMax() != other.getLongitudeMax()) return false;
            if (this.getLatitudeMin() != other.getLatitudeMin()) return false;
            if (this.getLatitudeMax() != other.getLatitudeMax()) return false;
            if ((this.getGeometryMode()==null ^ other.getGeometryMode()==null) || ( (this.getGeometryMode()!=null && other.getGeometryMode()!=null) && !this.getGeometryMode().equals(other.getGeometryMode()))) return false;
            if ((this.getGeometryType()==null ^ other.getGeometryType()==null) || ( (this.getGeometryType()!=null && other.getGeometryType()!=null) && !this.getGeometryType().equals(other.getGeometryType()))) return false;
            if ((this.getExplicitGeometry()==null ^ other.getExplicitGeometry()==null) || ( (this.getExplicitGeometry()!=null && other.getExplicitGeometry()!=null) && !this.getExplicitGeometry().equals(other.getExplicitGeometry()))) return false;
            if (this.getDistanceDebutMin() != other.getDistanceDebutMin()) return false;
            if (this.getDistanceDebutMax() != other.getDistanceDebutMax()) return false;
            if (this.getDistanceFinMin() != other.getDistanceFinMin()) return false;
            if (this.getDistanceFinMax() != other.getDistanceFinMax()) return false;
            if (this.getContactEau() != other.getContactEau()) return false;
            if ((this.getDate_debut()==null ^ other.getDate_debut()==null) || ( (this.getDate_debut()!=null && other.getDate_debut()!=null) && !this.getDate_debut().equals(other.getDate_debut()))) return false;
            if ((this.getDate_fin()==null ^ other.getDate_fin()==null) || ( (this.getDate_fin()!=null && other.getDate_fin()!=null) && !this.getDate_fin().equals(other.getDate_fin()))) return false;
            if ((this.getBorneDebutId()==null ^ other.getBorneDebutId()==null) || ( (this.getBorneDebutId()!=null && other.getBorneDebutId()!=null) && !this.getBorneDebutId().equals(other.getBorneDebutId()))) return false;
            if ((this.getBorneFinId()==null ^ other.getBorneFinId()==null) || ( (this.getBorneFinId()!=null && other.getBorneFinId()!=null) && !this.getBorneFinId().equals(other.getBorneFinId()))) return false;
            if ((this.getSystemeRepId()==null ^ other.getSystemeRepId()==null) || ( (this.getSystemeRepId()!=null && other.getSystemeRepId()!=null) && !this.getSystemeRepId().equals(other.getSystemeRepId()))) return false;
            if ((this.getParcelleId()==null ^ other.getParcelleId()==null) || ( (this.getParcelleId()!=null && other.getParcelleId()!=null) && !this.getParcelleId().equals(other.getParcelleId()))) return false;
            if ((this.getTypePositionId()==null ^ other.getTypePositionId()==null) || ( (this.getTypePositionId()!=null && other.getTypePositionId()!=null) && !this.getTypePositionId().equals(other.getTypePositionId()))) return false;
            if ((this.getTypeCoteId()==null ^ other.getTypeCoteId()==null) || ( (this.getTypeCoteId()!=null && other.getTypeCoteId()!=null) && !this.getTypeCoteId().equals(other.getTypeCoteId()))) return false;
            if (this.getCartoEdited() != other.getCartoEdited()) return false;
            if (this.getCommentaire() != other.getCommentaire()) return false;
            return true;
        }
        return false;
    }

    @JsonIgnore
    @Computed
    public String getZoneType(){
        return LabelMapper.get(this.getClass()).mapClassName();
    }

    @JsonIgnore
    @Computed
    public String getVegetationType(){
        final String vegetationId;
        if(this instanceof PeuplementVegetation){
            vegetationId = ((PeuplementVegetation)this).typeVegetationIdProperty().get();
        }else if(this instanceof InvasiveVegetation){
            vegetationId = ((InvasiveVegetation)this).typeVegetationIdProperty().get();
        }
        else {
            vegetationId=null;
        }

        return vegetationId==null ? null : converter.toString(Injector.getSession().getPreviews().get(vegetationId));
    }

    @JsonIgnore
    @Computed
    public String getSurface(){

            if(this!=null){
                final Geometry geom = geometryProperty().get();
                if(geom!=null){
                    return getGeometryInfo(geom);
                }
                else {
                    return null;
                }
            }
            else {
                return null;
            }
    }

    /**
     * Calcul de la surface en m².
     * @param geometry
     * @return
     */
    private static String getGeometryInfo(final Geometry geometry) {
        if (geometry != null && (geometry instanceof Polygon || geometry instanceof MultiPolygon)) {
            final String surface = NumberFormat.getNumberInstance().format(
                    SIRSAreaComputer.calculateArea(geometry, Injector.getSession().getProjection(), Units.SQUARE_METRE));
            return surface;
        }
        else {
            return "";
        }
    }
}

