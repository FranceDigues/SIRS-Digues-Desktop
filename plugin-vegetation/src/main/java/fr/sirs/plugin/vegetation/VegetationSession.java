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
package fr.sirs.plugin.vegetation;

import com.vividsolutions.jts.geom.Geometry;
import fr.sirs.Injector;
import fr.sirs.Session;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsDBInfo;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.AbstractZoneVegetationRepository;
import fr.sirs.core.component.ArbreVegetationRepository;
import fr.sirs.core.component.HerbaceeVegetationRepository;
import fr.sirs.core.component.InvasiveVegetationRepository;
import fr.sirs.core.component.ParcelleVegetationRepository;
import fr.sirs.core.component.PeuplementVegetationRepository;
import fr.sirs.core.component.PlanVegetationRepository;
import fr.sirs.core.component.SirsDBInfoRepository;
import fr.sirs.core.model.ArbreVegetation;
import fr.sirs.core.model.HerbaceeVegetation;
import fr.sirs.core.model.InvasiveVegetation;
import fr.sirs.core.model.ParamCoutTraitementVegetation;
import fr.sirs.core.model.ParcelleVegetation;
import fr.sirs.core.model.PeuplementVegetation;
import fr.sirs.core.model.PlanVegetation;
import fr.sirs.core.model.RefTraitementVegetation;
import fr.sirs.core.model.TraitementParcelleVegetation;
import fr.sirs.core.model.TraitementZoneVegetation;
import fr.sirs.core.model.ZoneVegetation;
import fr.sirs.plugin.vegetation.map.PlanifState;
import static fr.sirs.plugin.vegetation.map.PlanifState.NON_PLANIFIE;
import static fr.sirs.plugin.vegetation.map.PlanifState.PLANIFIE;
import static fr.sirs.plugin.vegetation.map.PlanifState.PLANIFIE_PREMIERE_FOIS;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javax.swing.ImageIcon;
import org.apache.sis.measure.Units;
import org.apache.sis.storage.DataStoreException;
import org.ektorp.DocumentNotFoundException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.data.memory.MemoryFeatureStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.style.MutableFeatureTypeStyle;
import org.geotoolkit.style.MutableRule;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.style.RandomStyleBuilder;
import static org.geotoolkit.style.StyleConstants.DEFAULT_ANCHOR_POINT;
import static org.geotoolkit.style.StyleConstants.DEFAULT_DISPLACEMENT;
import static org.geotoolkit.style.StyleConstants.DEFAULT_GRAPHIC_ROTATION;
import static org.geotoolkit.style.StyleConstants.LITERAL_ONE_FLOAT;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.style.ExternalGraphic;
import org.opengis.style.ExternalMark;
import org.opengis.style.Fill;
import org.opengis.style.Graphic;
import org.opengis.style.GraphicalSymbol;
import org.opengis.style.LineSymbolizer;
import org.opengis.style.Mark;
import org.opengis.style.PointSymbolizer;
import org.opengis.style.Stroke;

/**
 * Session de vegetation.
 * Contient le plan de gestion en cours.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class VegetationSession {

    private static final String PLAN_GESTION_PREF_KEY = "Plan de gestion";

    public static final VegetationSession INSTANCE = new VegetationSession();
    public static final String PLANIFIE_TRAITE = "Planifié / Traité";
    public static final String PLANIFIE_NON_TRAITE = "Planifié / Non traité";
    public static final String NON_PLANIFIE_TRAITE = "Non planifié / Traité";
    public static final String NON_PLANIFIE_NON_TRAITE = "Non planifié / Non traité";
    public static final String PLANIFIE_FUTUR = "Planifié (futur)";
    public static final String NON_PLANIFIE_FUTUR = "Non planifié (futur)";

    private final ObjectProperty<PlanVegetation> planProperty = new SimpleObjectProperty<>();

    private final PlanVegetationRepository planRepo;
    private final ArbreVegetationRepository arbreRepo;
    private final HerbaceeVegetationRepository herbaceeRepo;
    private final InvasiveVegetationRepository invasiveRepo;
    private final ParcelleVegetationRepository parcelleRepo;
    private final PeuplementVegetationRepository peuplementRepo;

    private final MapItem vegetationGroup;

    private VegetationSession() {
        final Session session = Injector.getSession();
        planRepo = (PlanVegetationRepository) session.getRepositoryForClass(PlanVegetation.class);
        arbreRepo = (ArbreVegetationRepository) session.getRepositoryForClass(ArbreVegetation.class);
        herbaceeRepo = (HerbaceeVegetationRepository) session.getRepositoryForClass(HerbaceeVegetation.class);
        invasiveRepo = (InvasiveVegetationRepository) session.getRepositoryForClass(InvasiveVegetation.class);
        parcelleRepo = (ParcelleVegetationRepository) session.getRepositoryForClass(ParcelleVegetation.class);
        peuplementRepo = (PeuplementVegetationRepository) session.getRepositoryForClass(PeuplementVegetation.class);

        vegetationGroup = MapBuilder.createItem();
        vegetationGroup.setName("Végétation");
        vegetationGroup.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);

        final String dbUUID;
        final Optional<SirsDBInfo> info = Injector.getBean(SirsDBInfoRepository.class).get();
        if (info.isPresent()) {
            dbUUID = info.get().getUuid();
        } else {
            dbUUID = null;
        }

        final Preferences userPrefs = Preferences.userNodeForPackage(VegetationSession.class);

        try {
            if (userPrefs.nodeExists(dbUUID)) {
                final String pid = userPrefs.node(dbUUID).get(PLAN_GESTION_PREF_KEY, null);
                if (pid != null && !pid.isEmpty()) {
                    planProperty.set(planRepo.get(pid));
                }
            }
        } catch (BackingStoreException | DocumentNotFoundException e) {
            SirsCore.LOGGER.log(Level.WARNING, "Le plan de gestion par défaut ne peut être activé.", e);
        }

        planProperty.addListener((obs, oldValue, newValue) -> {
            final Preferences node = Preferences.userNodeForPackage(VegetationSession.class).node(dbUUID);
            if (newValue == null || newValue.getId() == null) {
                node.remove(PLAN_GESTION_PREF_KEY);
            } else {
                node.put(PLAN_GESTION_PREF_KEY, newValue.getId());
            }
        });
    }

    public MapItem getVegetationGroup() {
        return vegetationGroup;
    }

    public PlanVegetationRepository getPlanRepository() {
        return planRepo;
    }

    public ArbreVegetationRepository getArbreRepo() {
        return arbreRepo;
    }

    public HerbaceeVegetationRepository getHerbaceeRepo() {
        return herbaceeRepo;
    }

    public InvasiveVegetationRepository getInvasiveRepo() {
        return invasiveRepo;
    }

    public ParcelleVegetationRepository getParcelleRepo() {
        return parcelleRepo;
    }

    public PeuplementVegetationRepository getPeuplementRepo() {
        return peuplementRepo;
    }

    public List<HerbaceeVegetation> getHerbaceeByPlan(String planId){
        //herbaceeRepo.getByParcelleId(planId);
        return null;
    }

    /**
     * Plan de gestion actif.
     *
     * @return
     */
    public ObjectProperty<PlanVegetation> planProperty() {
        return planProperty;
    }

    /**
     * Le cout d'exploitation d'une liste de parcelles pour une année est donné
     * par la somme des couts des traitements de la parcelle dont la date
     * correspond à l'année indiquée.
     * @param year
     * @param parcelles
     * @return
     */
    public static double exploitationCost(final int year, final List<ParcelleVegetation> parcelles){

        double cost = 0.;

        for(final ParcelleVegetation parcelle : parcelles){
            for(final TraitementParcelleVegetation traitement : parcelle.getTraitements()){
                if(traitement.getDate()!=null && traitement.getDate().getYear()==year){
                    cost+=traitement.getCout();
                }
            }
        }
        return cost;
    }

    /**
     * Calcul du coût planifié d'un plan pour une année.
     *
     * Pour une année de planification, le coût est calculé en faisant la somme
     * de tous les coûts de traitements sur toutes les zones de la parcelle, dès
     * que cette dernière est planifiée "traitée" pour cette année.
     *
     * Calcul du coût d'une zone :
     * ==========================
     * Le coût surfacique de traitement, entré dans les paramètres du plan, est
     * multiplié par la survace de la zone (s'il s'agit d'une surface), ou bien
     * ajouté tel quel s'il s'agit d'un arbre.
     *
     * Pour une zone de végétation, le traitement n'est pris en compte que s'il
     * n'a pas été spécifié comme "hors-gestion".
     *
     * Prise en compte des traitements ponctuels et non-ponctuels :
     * ============================================================
     * Enfin, une zone de végétation est associée à deux traitements :
     * un ponctuel et un non ponctuel.
     *
     * Les traitements ponctuels sont pris en compte pour toutes les années
     * planifiées alors que le traitement ponctuel n'est pris en compte que la
     * première année du plan, pourvu que la parcelle soit planifiée cette
     * année-là.
     *
     * @param plan
     * @param yearIndex
     * @param parcelles
     * @return
     */
    public static double estimatedPlanificationCost(final PlanVegetation plan, final int yearIndex, final List<ParcelleVegetation> parcelles){
        /*
        En mode planification le coût suppose que l'on fasse la somme de
        tous les côuts de traitements des zones de la parcelle, dès que
        celle-ci est planifiée "traitée".

        Les coûts planifiés se trouvent dans le plan.
        */
        double cout = 0.0;
        final ObservableList<ParamCoutTraitementVegetation> params = plan.getParamCout();

        // Map d'indexation des paramètres qui ont un traitement et un sous-traitement.
        final Map<Entry<String, String>, ParamCoutTraitementVegetation> indexedParams1 = new HashMap<>();

        // Map d'indexation des paramètres qui ont un traitement mais pas de sous-traitement.
        final Map<String, ParamCoutTraitementVegetation> indexedParams2 = new HashMap<>();

        // Indexation des paramètres.
        for(final ParamCoutTraitementVegetation param : params){
            if(param.getTypeTraitementId()!=null){
                if(param.getSousTypeTraitementId()!=null){
                    indexedParams1.put(new HashMap.SimpleEntry<>(param.getTypeTraitementId(), param.getSousTypeTraitementId()), param);
                }
                else{
                    indexedParams2.put(param.getTypeTraitementId(), param);
                }
            }
        }

        /*
        Calcul des coûts proprement dit
        */

        //On parcourt toutes les parcelles du tableau
        for (final ParcelleVegetation parcelle : parcelles){

            // La parcelle est-elle planifiée cette année ?
            final PlanifState planifState = getParcellePlanifState(parcelle, yearIndex);

            // On parcourt toutes les zones de végétation de la parcelle
            final ObservableList<? extends ZoneVegetation> allZoneVegetationByParcelleId = AbstractZoneVegetationRepository.getAllZoneVegetationByParcelleId(parcelle.getId(), Injector.getSession());

            for(final ZoneVegetation zone : allZoneVegetationByParcelleId){

                /*
                Les invasives entrent dans le calcul tous les ans, et les autres
                zones uniquement les années planifiées.
                */
                if(!(zone instanceof InvasiveVegetation) && planifState==NON_PLANIFIE) continue;

                // On vérifie que la zone a bien un traitement planifié et que celui-ci entre dans le plan de gestion
                if(zone.getTraitement()!=null && !zone.getTraitement().getHorsGestion()){

                    // On récupère le traitement ponctuel et non ponctuel de la zone
                    final TraitementZoneVegetation traitement = zone.getTraitement();

                    /*
                    On commence par s'occuper du traitement ponctuel
                    !!! (UNIQUEMENT SI ON EST LA PREMIÈRE ANNÉE DE TRAITEMENT DE LA PARCELLE DANS LE PLAN) !!!
                    */
                    if(planifState==PLANIFIE_PREMIERE_FOIS){
                        final String traitementPonctuelId = traitement.getTypeTraitementPonctuelId();
                        final String sousTraitementPonctuelId = traitement.getSousTypeTraitementPonctuelId();

                        // On récupère et on ajoute le cout sur la zone de la combinaison traitement/sous-traitement
                        if(traitementPonctuelId!=null){
                            final ParamCoutTraitementVegetation p;
                            if(sousTraitementPonctuelId!=null){
                                p=indexedParams1.get(new HashMap.SimpleEntry<>(traitementPonctuelId, sousTraitementPonctuelId));
                            }
                            else{
                                p=indexedParams2.get(traitementPonctuelId);
                            }

                            if(p!=null) cout+=computePlanifiedCost(zone, p);
                        }
                    }


                    /*
                    Puis on s'occupe du traitement non ponctuel
                    */
                    final String traitementId = traitement.getTypeTraitementId();
                    final String sousTraitementId = traitement.getSousTypeTraitementId();

                    // On récupère et on ajoute le cout sur la zone de la combinaison traitement/sous-traitement
                    if(traitementId!=null){
                        final ParamCoutTraitementVegetation p;
                        if(sousTraitementId!=null){
                            p=indexedParams1.get(new HashMap.SimpleEntry<>(traitementId, sousTraitementId));
                        }
                        else{
                            p=indexedParams2.get(traitementId);
                        }

                        if(p!=null) cout+=computePlanifiedCost(zone, p);
                    }
                }
            }
        }

        return cout;
    }

    /**
     * Calcule la planification d'un coût sur une zone de végétation à partir du
     * coût paramétré.
     *
     * Si la zone est un arbre, le coût est considéré comme unitaire pour la zone.
     * Si la zone est d'un autre type, le coût est considéré comme surfacique et donc multiplié par la surface de la zone.
     *
     * @param zone
     * @param param
     * @return
     */
    private static double computePlanifiedCost(final ZoneVegetation zone, final ParamCoutTraitementVegetation param){
        if(zone.getGeometry()!=null){
            // Dans le cas des arbres, le coût est unitaire
            if(zone instanceof ArbreVegetation){
                return param.getCout();
            }
            // Dans le cas des autres zones, le coût est surfacique
            else{
                return zone.getGeometry().getArea() * param.getCout();
            }
        }
        else return 0.0;
    }

    /**
     * Retourne vrai si la parcelle est traité pour l'année donnée.
     *
     * Il suffit qu'un traitement ait eu lieu pour que la parcelle soit déclarée
     * traitée dans l'année.
     *
     * @param parcelle
     * @param year
     * @return
     */
    public static boolean isParcelleTraitee(ParcelleVegetation parcelle, int year){
        boolean done = false;
        for(TraitementParcelleVegetation traitement : parcelle.getTraitements()){
            if(traitement.getDate()!=null && traitement.getDate().getYear() == year){
                done = true;
                break;
            }
        }
        return done;
    }

    /**
     * Retourne l'etat de planification de la parcelle pour l'année donnée.
     *
     * @param plan
     * @param parcelle
     * @param year
     * @return
     */
    public static PlanifState getParcellePlanifState(final PlanVegetation plan, final ParcelleVegetation parcelle, final int year){
        return getParcellePlanifState(parcelle, year - plan.getAnneeDebut());
    }

    /**
     * Retourne l'etat de planification de la parcelle pour la planification à l'index donné.
     *
     * @param parcelle
     * @param index
     * @return
     */
    public static PlanifState getParcellePlanifState(final ParcelleVegetation parcelle, final int index){
        final List<Boolean> planifications = parcelle.getPlanifications();

        if(planifications==null || planifications.size()<=index) return NON_PLANIFIE;

        if(!planifications.get(index)) return NON_PLANIFIE;

        /* À ce stade, on est sûr que la parcelle est planifiée pour l'année que
        l'on examine. Mais est-ce la première fois ? Pour cela il faut aller
        voir si la parcelle avait également été planifiée une année antérieure
        du plan.
        */
        for(int i=0; i<index; i++){
            if(planifications.get(i)) return PLANIFIE;
        }
        return PLANIFIE_PREMIERE_FOIS;
    }

    /**
     * String en fonction de l'etat des traitements.
     * Voir constantes : ETAT_X
     *
     * @param parcelle
     * @param planifie
     * @param year
     * @return
     */
    public static String getParcelleEtat(final ParcelleVegetation parcelle, final boolean planifie, final int year){
        final int thisYear = LocalDate.now().getYear();

        final boolean done = isParcelleTraitee(parcelle, year);

        if(year>thisYear){
            //pas de couleur pour les années futures
            return planifie ? PLANIFIE_FUTUR : NON_PLANIFIE_FUTUR;
        }

        if(done){
            if(planifie){
                return PLANIFIE_TRAITE;
            } else{
                return NON_PLANIFIE_TRAITE;
            }
        }else{
            if(planifie){
                // Les parcelles planifiées non traitées de l'année en cours sont considérées comme planifiées dans le futur.
                if(year==thisYear) return PLANIFIE_FUTUR;
                else return PLANIFIE_NON_TRAITE;
            } else{
                return NON_PLANIFIE_NON_TRAITE;
            }
        }
    }

    public static void setCheckBoxColor(final CheckBox cb, final String state){
        final Color color = getParcelleEtatColor(state);
        if(color==null){
            cb.setBackground(Background.EMPTY);
        }else{
            cb.setBackground(new Background(new BackgroundFill(color, new CornerRadii(30), new Insets(5))));
        }
    }

    /**
     * Couleur en fonction de l'etat des traitements.
     * Void : Non planifié, Non traité
     * Orange : Non planifié, traité
     * Rouge : Planifié, Non traité
     * Vert : Planifié, traité
     *
     * @param state
     * @return
     */
    public static Color getParcelleEtatColor(final String state){
        if (state==null) return null;
        switch(state){
            case PLANIFIE_TRAITE : return Color.GREEN;
            case PLANIFIE_NON_TRAITE : return Color.RED;
            case NON_PLANIFIE_TRAITE : return Color.ORANGE;
            case NON_PLANIFIE_NON_TRAITE : return null;
            default : return null;
        }
    }

    /**
     * Creation d'une couche carto contenant l'etat du traitement des parcelles :
     * - traité
     * - non traité
     *
     * @param plan
     * @param year
     * @param parcelles liste des parcelles voulue ou nulle pour toute
     * @return
     */
    public static MapLayer parcelleTrmtState(final PlanVegetation plan, final int year, Collection<ParcelleVegetation> parcelles){
        //etat des parcelles : traité, non traité
        final ParcelleVegetationRepository parcelleRepo = VegetationSession.INSTANCE.getParcelleRepo();

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("type");
        ftb.add("id", String.class);
        ftb.add("geometry", Geometry.class, Injector.getSession().getProjection());
        ftb.add("etat", String.class);
        final FeatureType ft = ftb.buildFeatureType();

        final MemoryFeatureStore store = new MemoryFeatureStore(ft, true);
        FeatureWriter writer;
        try {
            writer = store.getFeatureWriterAppend(ft.getName());
        } catch (DataStoreException ex) {
            //n'arrive pas avec un feature store en mémoire
            throw new RuntimeException(ex);
        }

        if(parcelles==null) parcelles = parcelleRepo.getByPlanId(plan.getId());

        for(ParcelleVegetation pv : parcelles){
            final Feature feature = writer.next();
            feature.setPropertyValue("id", pv.getId());
            feature.setPropertyValue("geometry", pv.getGeometry());
            feature.setPropertyValue("etat", VegetationSession.isParcelleTraitee(pv, year));
            feature.getUserData().put(BeanFeature.KEY_BEAN, pv);
            writer.write();
        }
        writer.close();


        final FeatureCollection col = store.createSession(true).getFeatureCollection(QueryBuilder.all(ft.getName()));
        final MapLayer layer = MapBuilder.createFeatureLayer(col);
        layer.setName("Traitement réel "+year);
        layer.setStyle(createTraitementReelStyle());

        return layer;
    }

    /**
     * Creation d'une couche carto contenant l'etat des parcelles :
     * - traité
     * - planifié
     * - non traité
     * - non planifié ...
     *
     * @param plan
     * @param year
     * @param parcelles liste des parcelles voulue ou nulle pour toute
     * @return
     */
    public static MapLayer parcellePanifState(final PlanVegetation plan, final int year, Collection<ParcelleVegetation> parcelles){
        //etat des parcelles : planifié, traité, non planifié etc...

        final ParcelleVegetationRepository parcelleRepo = VegetationSession.INSTANCE.getParcelleRepo();

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("type");
        ftb.add("id", String.class);
        ftb.add("geometry", Geometry.class, Injector.getSession().getProjection());
        ftb.add("etat", String.class);
        final FeatureType ft = ftb.buildFeatureType();

        final MemoryFeatureStore store = new MemoryFeatureStore(ft, true);
        FeatureWriter writer;
        try {
            writer = store.getFeatureWriterAppend(ft.getName());
        } catch (DataStoreException ex) {
            //n'arrive pas avec un feature store en mémoire
            throw new RuntimeException(ex);
        }

        if(parcelles==null) parcelles = parcelleRepo.getByPlanId(plan.getId());

        for(ParcelleVegetation pv : parcelles){
            final List<Boolean> planifications = pv.getPlanifications();
            int index = year - plan.getAnneeDebut();
            boolean planified = false;
            if(planifications!=null && planifications.size()>index) planified = planifications.get(index);

            final Feature feature = writer.next();
            feature.setPropertyValue("id", pv.getId());
            feature.setPropertyValue("geometry", pv.getGeometry());
            feature.setPropertyValue("etat", VegetationSession.getParcelleEtat(pv, planified, year));
            feature.getUserData().put(BeanFeature.KEY_BEAN, pv);
            writer.write();
        }
        writer.close();


        final FeatureCollection col = store.createSession(true).getFeatureCollection(QueryBuilder.all(ft.getName()));
        final MapLayer layer = MapBuilder.createFeatureLayer(col);
        layer.setName("Etat parcelle "+year);
        layer.setStyle(createParcelleStateStyle());

        return layer;
    }

    public static MapLayer vegetationPlanifState(PlanVegetation plan, int year){
        //etat des vegetations par type de traitement a faire

        final ParcelleVegetationRepository parcelleRepo = VegetationSession.INSTANCE.getParcelleRepo();

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("type");
        ftb.add("id", String.class);
        ftb.add("geometry", Geometry.class, Injector.getSession().getProjection());
        ftb.add("etat", String.class);
        final FeatureType ft = ftb.buildFeatureType();

        final MemoryFeatureStore store = new MemoryFeatureStore(ft, true);
        FeatureWriter writer;
        try {
            writer = store.getFeatureWriterAppend(ft.getName());
        } catch (DataStoreException ex) {
            //n'arrive pas avec un feature store en mémoire
            throw new RuntimeException(ex);
        }

        //on liste tous les traitements
        final AbstractSIRSRepository<RefTraitementVegetation> trmtRepo = Injector.getSession().getRepositoryForClass(RefTraitementVegetation.class);
        final Map<String,String> trmts = new HashMap<>();
        for(RefTraitementVegetation trmt : trmtRepo.getAll()){
            trmts.put(trmt.getDocumentId(), trmt.getLibelle());
        }
        trmts.put(null, "-");

        for(ParcelleVegetation pv : parcelleRepo.getByPlanId(plan.getId())){

            //on regarde si c'est la premiere année
            final PlanifState planifState = VegetationSession.getParcellePlanifState(plan, pv, year);

            final List<ZoneVegetation> vegetations = new ArrayList<>();
            vegetations.addAll(VegetationSession.INSTANCE.getHerbaceeRepo().getByParcelleId(pv.getDocumentId()));
            vegetations.addAll(VegetationSession.INSTANCE.getInvasiveRepo().getByParcelleId(pv.getDocumentId()));
            vegetations.addAll(VegetationSession.INSTANCE.getPeuplementRepo().getByParcelleId(pv.getDocumentId()));
            vegetations.addAll(VegetationSession.INSTANCE.getArbreRepo().getByParcelleId(pv.getDocumentId()));

            for(ZoneVegetation zone : vegetations){
                String etat = null;

                if(zone.getTraitement()!=null){
                    if(planifState==PLANIFIE_PREMIERE_FOIS){
                        //premiere année
                        String tid = zone.getTraitement().getTypeTraitementPonctuelId();
                        if(tid==null || tid.isEmpty()){
                            tid = zone.getTraitement().getTypeTraitementId();
                        }
                        etat = trmts.get(tid);
                    }else if(planifState==PLANIFIE_PREMIERE_FOIS){
                        final String tid = zone.getTraitement().getTypeTraitementId();
                        etat = trmts.get(tid);
                    }
                }

                final Feature feature = writer.next();
                feature.setPropertyValue("id", pv.getId());
                feature.setPropertyValue("geometry", zone.getGeometry());
                feature.setPropertyValue("etat", etat);
                feature.getUserData().put(BeanFeature.KEY_BEAN, pv);
                writer.write();
            }
        }
        writer.close();


        final FeatureCollection col = store.createSession(true).getFeatureCollection(QueryBuilder.all(ft.getName()));
        final MapLayer layer = MapBuilder.createFeatureLayer(col);
        layer.setName("Traitement planifié "+year);
        layer.setStyle(createTraitementPlanifiéStyle(trmts.values()));

        return layer;
    }

    private static MutableStyle createTraitementReelStyle() {
        final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;

        final MutableStyle style = SF.style();
        final MutableFeatureTypeStyle fts = SF.featureTypeStyle();
        style.featureTypeStyles().add(fts);

        final MutableRule traiteRule = createParcelleRule(Boolean.TRUE,      new java.awt.Color(0.0f, 0.7f, 0.0f, 0.6f));
        traiteRule.setName("Traité");
        traiteRule.setDescription(SF.description(traiteRule.getName(),traiteRule.getName()));

        final MutableRule nonTraiteRule = createParcelleRule(Boolean.FALSE,      new java.awt.Color(0.7f, 0.0f, 0.0f, 0.6f));
        nonTraiteRule.setName("Non traité");
        nonTraiteRule.setDescription(SF.description(nonTraiteRule.getName(),nonTraiteRule.getName()));

        fts.rules().add(traiteRule);
        fts.rules().add(nonTraiteRule);

        return style;
    }

    private static MutableStyle createTraitementPlanifiéStyle(Collection<String> types){

        final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;
        final FilterFactory2 FF = GO2Utilities.FILTER_FACTORY;

        final MutableStyle style = SF.style();
        final MutableFeatureTypeStyle fts = SF.featureTypeStyle();
        style.featureTypeStyles().add(fts);

        for(String str : types){

            final java.awt.Color color;
            if(str.equals("-")){
                color = java.awt.Color.LIGHT_GRAY;
            }else{
                color = RandomStyleBuilder.randomColor();
            }


            //point rule
            final MutableRule rulePoint = SF.rule();
            rulePoint.setName(str);
            rulePoint.setDescription(SF.description(str, str));
            rulePoint.setFilter(
                    FF.and(
                        FF.equals(FF.function("geometryType", FF.property("geometry")),FF.literal("Point")),
                        FF.equals(FF.property("etat"), FF.literal(str)))
                    );

            final Stroke stroke = SF.stroke(java.awt.Color.BLACK, 1);
            final Fill fill = SF.fill(color);
            final ExternalMark extMark;
            try {
                extMark = SF.externalMark(
                        SF.onlineResource(IconBuilder.FONTAWESOME.toURI()),
                        "ttf",FontAwesomeIcons.ICON_TREE.codePointAt(0));
            } catch (URISyntaxException ex) {
                //n'arrivera pas
                throw new RuntimeException(ex);
            }

            final Mark mark = SF.mark(extMark, fill, stroke);

            final Expression size = GO2Utilities.FILTER_FACTORY.literal(16);
            final List<GraphicalSymbol> symbols = new ArrayList<>();
            symbols.add(mark);
            final Graphic graphic = SF.graphic(symbols, LITERAL_ONE_FLOAT,
                    size, DEFAULT_GRAPHIC_ROTATION, DEFAULT_ANCHOR_POINT, DEFAULT_DISPLACEMENT);
            rulePoint.symbolizers().add(SF.pointSymbolizer("", FF.property("geometry"), null, Units.POINT, graphic));

            fts.rules().add(rulePoint);

            //polygon rule
            final MutableRule rulePoly = SF.rule();
            rulePoly.setName(str);
            rulePoly.setDescription(SF.description(str, str));
            rulePoly.setFilter(
                    FF.and(
                        FF.equals(FF.function("geometryType", FF.property("geometry")),FF.literal("Polygon")),
                        FF.equals(FF.property("etat"), FF.literal(str)))
                    );
            final Stroke strokePoly = SF.stroke(java.awt.Color.BLACK, 1);
            final Fill fillPoly = SF.fill(color);
            rulePoly.symbolizers().add(SF.polygonSymbolizer(strokePoly, fillPoly, null));


            fts.rules().add(rulePoly);
        }

        return style;
    }

    private static MutableStyle createParcelleStateStyle() {
        final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;

        final MutableStyle style = SF.style();
        final MutableFeatureTypeStyle fts = SF.featureTypeStyle();
        style.featureTypeStyles().add(fts);

        fts.rules().add(createParcelleRule(VegetationSession.PLANIFIE_TRAITE,      new java.awt.Color(0.0f, 0.7f, 0.0f, 0.6f)));
        fts.rules().add(createParcelleRule(VegetationSession.PLANIFIE_NON_TRAITE,   new java.awt.Color(0.7f, 0.0f, 0.0f, 0.6f)));
        fts.rules().add(createParcelleRule(VegetationSession.PLANIFIE_FUTUR,       new java.awt.Color(0.0f, 0.0f, 0.7f, 0.6f)));
        fts.rules().add(createParcelleRule(VegetationSession.NON_PLANIFIE_TRAITE,   new java.awt.Color(0.6f, 0.4f, 0.0f, 0.6f)));
        fts.rules().add(createParcelleRule(VegetationSession.NON_PLANIFIE_NON_TRAITE,new java.awt.Color(0.7f, 0.7f, 0.7f, 0.6f)));
        fts.rules().add(createParcelleRule(VegetationSession.NON_PLANIFIE_FUTUR,    new java.awt.Color(0.7f, 0.7f, 0.7f, 0.6f)));

        return style;
    }

    private static MutableRule createParcelleRule(Object state, java.awt.Color color){
        final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;
        final FilterFactory2 FF = GO2Utilities.FILTER_FACTORY;
        final MutableRule rule = SF.rule();

        rule.setFilter(FF.equals(FF.property("etat"), FF.literal(state)));
        rule.setName(String.valueOf(state));
        rule.setDescription(SF.description(String.valueOf(state), String.valueOf(state)));


        final BufferedImage img = new BufferedImage(4, 100, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 2, 100);
        g.dispose();

        final ExternalGraphic external = SF.externalGraphic(new ImageIcon(img),Collections.EMPTY_LIST);

        final Expression rotationStart = FF.subtract(FF.literal(0),FF.function("toDegrees", FF.function("startAngle", FF.property("geometry"))));
        final Expression rotationEnd = FF.subtract(FF.literal(0),FF.function("toDegrees", FF.function("endAngle", FF.property("geometry"))));

        final Expression size = GO2Utilities.FILTER_FACTORY.literal(200);
        final List<GraphicalSymbol> symbols = new ArrayList<>();
        symbols.add(external);
        final Graphic graphicStart = SF.graphic(symbols, LITERAL_ONE_FLOAT,
                size, rotationStart, DEFAULT_ANCHOR_POINT, DEFAULT_DISPLACEMENT);
        final Graphic graphicEnd = SF.graphic(symbols, LITERAL_ONE_FLOAT,
                size, rotationEnd, DEFAULT_ANCHOR_POINT, DEFAULT_DISPLACEMENT);

        final PointSymbolizer ptStart = SF.pointSymbolizer("", FF.function("startPoint", FF.property("geometry")), null, Units.POINT, graphicStart);
        final PointSymbolizer ptEnd = SF.pointSymbolizer("", FF.function("endPoint", FF.property("geometry")), null, Units.POINT, graphicEnd);

        rule.symbolizers().add(ptStart);
        rule.symbolizers().add(ptEnd);

        //line
        final Stroke lineStroke = SF.stroke(color, 4, null);
        final LineSymbolizer lineSymbol = SF.lineSymbolizer(lineStroke, null);
        rule.symbolizers().add(lineSymbol);

        return rule;
    }
}
