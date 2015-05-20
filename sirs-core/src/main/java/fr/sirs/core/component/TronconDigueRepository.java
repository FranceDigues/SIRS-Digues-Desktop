package fr.sirs.core.component;

import fr.sirs.core.InjectorCore;
import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.View;
import org.ektorp.support.Views;
import org.springframework.beans.factory.annotation.Autowired;

import fr.sirs.core.JacksonIterator;
import fr.sirs.core.SirsCoreRuntimeExecption;
import fr.sirs.core.model.Digue;
import fr.sirs.core.model.ElementCreator;
import fr.sirs.core.model.GardeTroncon;
import fr.sirs.core.model.PeriodeLocaliseeTroncon;
import fr.sirs.core.model.ProprieteTroncon;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import org.apache.sis.util.ArgumentChecks;

/**
 * Outil gérant les échanges avec la bdd CouchDB pour tous les objets tronçons.
 * 
 * Note : Le cache qui permet de garder une instance unique en mémoire pour un 
 * tronçon donné est extrêmement important pour les opérations de sauvegarde. 
 * 
 * Ex : On a un tronçon A, qui contient une crête tata et un talus de digue toto.
 * 
 * On ouvre l'éditeur de toto. Le tronçon A est donc chargé en mémoire. 
 * Dans le même temps on ouvre le panneau d'édition pour tata. On doit donc aussi 
 * charger le tronçon A en mémoire.
 * 
 * Maintenant, que se passe -'il sans cache ? On a deux copies du tronçon A en  
 * mémoire pour une même révision (disons 0).
 * 
 * Si on sauvegarde toto, tronçon A passe en révision 1 dans la bdd, mais pour 
 * UNE SEULE des 2 copies en mémoire. En conséquence de quoi, lorsqu'on veut 
 * sauvegarder tata, on a un problème : on demande à la base de faire une mise à
 * jour de la révision 1 en utilisant un objet de la révision 0.
 * 
 * Résultat : ERREUR ! 
 * 
 * Avec le cache, les deux éditeurs pointent sur la même copie en mémoire. Lorsqu'un
 * éditeur met à jour le tronçon, la révision de la copie est indentée, le deuxième
 * éditeur a donc un tronçon avec un numéro de révision correct.
 * 
 * @author Samuel Andrés (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
@Views({
        @View(name = "all", map = "function(doc) {if(doc['@class']=='fr.sirs.core.model.TronconDigue') {emit(doc._id, doc._id)}}"),
        @View(name = "stream", map = "function(doc) {if(doc['@class']=='fr.sirs.core.model.TronconDigue') {emit(doc._id, doc)}}"),
        @View(name = "streamLight", map = "classpath:TronconDigueLight-map.js"),
        @View(name = TronconDigueRepository.BY_DIGUE_ID, map = "function(doc) {if(doc['@class']=='fr.sirs.core.model.TronconDigue') {emit(doc.digueId, doc._id)}}") })
public class TronconDigueRepository extends AbstractSIRSRepository<TronconDigue> {
    
    public static final String BY_DIGUE_ID = "byDigueId";
    
    private final HashMap<String, Callable<List<? extends PeriodeLocaliseeTroncon>>> viewMapPeriodesTroncon = new HashMap<>();
    
    @Autowired
    public TronconDigueRepository(CouchDbConnector db) {
        super(TronconDigue.class, db);
        initStandardDesignDocument();
        viewMapPeriodesTroncon.put(GARDE_TRONCON, this::getAllGardes);
        viewMapPeriodesTroncon.put(PROPRIETE_TRONCON, this::getAllProprietes);
    }

    public List<TronconDigue> getByDigue(final Digue digue) {
        ArgumentChecks.ensureNonNull("Digue parent", digue);
        return this.queryView(BY_DIGUE_ID, digue.getId());
    }

    @Override
    public void remove(TronconDigue entity) {
        ArgumentChecks.ensureNonNull("Tronçon à supprimer", entity);
        constraintDeleteBorneAndSR(entity);
        super.remove(entity);
    }

    @Override
    public Class<TronconDigue> getModelClass() {
        return TronconDigue.class;
    }

    @Override
    public TronconDigue create() {
        final SessionGen session = InjectorCore.getBean(SessionGen.class);
        if(session!=null && session instanceof OwnableSession){
            final ElementCreator elementCreator = ((OwnableSession) session).getElementCreator();
            return elementCreator.createElement(TronconDigue.class);
        } else {
            throw new SirsCoreRuntimeExecption("Pas de session courante");
        }
    }

    /**
     * Return a light version of the tronçon, without sub-structures.
     * 
     * Note : As the objects returned here are incomplete, they're not cached.
     * 
     * @return 
     */
    public List<TronconDigue> getAllLight() {
        final JacksonIterator<TronconDigue> ite = JacksonIterator.create(TronconDigue.class, db.queryForStreamingView(createQuery("streamLight")));
        final List<TronconDigue> lst = new ArrayList<>();
        while (ite.hasNext()) {
            lst.add(ite.next());
        }
        return lst;
    }
    
    public static final String GARDE_TRONCON = "GardeTroncon";

    @View(name = GARDE_TRONCON, map = "classpath:GardeTroncon-map.js")
    public List<GardeTroncon> getAllGardes() {
        return db.queryView(createQuery(GARDE_TRONCON),
                GardeTroncon.class);
    }

    public static final String PROPRIETE_TRONCON = "ProprieteTroncon";

    @View(name = PROPRIETE_TRONCON, map = "classpath:ProprieteTroncon-map.js")
    public List<ProprieteTroncon> getAllProprietes() {
        return db.queryView(createQuery(PROPRIETE_TRONCON),
                ProprieteTroncon.class);
    }

    public JacksonIterator<TronconDigue> getAllIterator() {
        return JacksonIterator.create(TronconDigue.class,
                db.queryForStreamingView(createQuery("stream")));
    }
    
    public JacksonIterator<TronconDigue> getAllLightIterator() {
        return JacksonIterator.create(TronconDigue.class,
                db.queryForStreamingView(createQuery("streamLight")));
    }
    
    /**
     * Cette contrainte s'assure de supprimer les SR et bornes associées au troncon
     * en cas de suppression.
     * 
     */
    private void constraintDeleteBorneAndSR(TronconDigue entity){
        //on supprime tous les SR associés
        final SystemeReperageRepository srrepo = new SystemeReperageRepository(db);
        final List<SystemeReperage> srs = srrepo.getByTroncon(entity);
        for(SystemeReperage sr : srs){
            srrepo.remove(sr, entity);
        }
        // TODO : Set an end date to bornes which are not used anymore.
    }

    @Override
    protected TronconDigue onLoad(TronconDigue toLoad) {
        new DefaultSRChangeListener(toLoad);
        return toLoad;
    }
}
