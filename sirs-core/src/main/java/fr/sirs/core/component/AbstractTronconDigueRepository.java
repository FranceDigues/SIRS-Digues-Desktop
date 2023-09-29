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
package fr.sirs.core.component;

import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Digue;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.PRComputer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentOperationResult;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.support.View;
import org.ektorp.support.Views;
import org.geotoolkit.gui.javafx.util.TaskManager;

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
 *
 * @param <T> The model class managed by the repository.
 */
@Views({
@View(name = AbstractTronconDigueRepository.BY_DIGUE_ID, map = "function(doc) {if(doc['@class'] && doc.digueId) {emit(doc.digueId, doc._id)}}"),
@View(name = AbstractTronconDigueRepository.BY_BORNE_ID, map = "function(doc) {if(Array.isArray(doc.borneIds)) {for (var i = 0 ; i < doc.borneIds.length ; i++) emit(doc.borneIds[i], doc._id)}}")
})
public class AbstractTronconDigueRepository<T extends TronconDigue> extends AbstractSIRSRepository<T> {

    public static final String ALL_TRONCON_IDS = "allIdsAndDesignation";
    public static final String PHOTOS_BY_TRONCON_ID = "photosByTronconId";
    public static final String STREAM_LIGHT = "streamLight";
    public static final String BY_DIGUE_ID = "byDigueId";
    public static final String BY_BORNE_ID = "byBorneId";
    public static final String BY_AH_ID = "byAhId";

    protected final ArrayList<WeakReference<T>> prUpdates = new ArrayList<>();
    protected final ConcurrentHashMap<T, PRComputer> prComputings = new ConcurrentHashMap<>();

    protected AbstractTronconDigueRepository(CouchDbConnector db, Class<T> typeClass) {
        super(typeClass, db);
        initStandardDesignDocument();
    }

    public List<T> getByDigue(final Digue digue) {
        ArgumentChecks.ensureNonNull("Digue parent", digue);
        return this.queryView(BY_DIGUE_ID, digue.getId());
    }

    @Override
    public void remove(T entity) {
        ArgumentChecks.ensureNonNull("Tronçon à supprimer", entity);
        constraintDeleteBoundEntities(entity);
        super.remove(entity);
    }

    @Override
    public void update(T entity) {
        checkPRUpdate(Collections.singleton(entity));
        super.update(entity);
    }

    @Override
    public List<DocumentOperationResult> executeBulk(T... bulkList) {
        checkPRUpdate(Arrays.asList(bulkList));
        return super.executeBulk(bulkList);
    }

    @Override
    public List<DocumentOperationResult> executeBulk(Collection<T> bulkList) {
        checkPRUpdate(bulkList);
        return super.executeBulk(bulkList);
    }

    @Override
    public Class<T> getModelClass() {
        return type;
    }

    @Override
    public T create() {
        final SessionCore session = InjectorCore.getBean(SessionCore.class);
        if(session!=null){
            return session.getElementCreator().createElement(type);
        } else {
            throw new SirsCoreRuntimeException("Pas de session courante");
        }
    }

    /**
     * Cette contrainte s'assure de supprimer les SR et bornes associées au troncon
     * en cas de suppression.
     */
    private void constraintDeleteBoundEntities(TronconDigue entity) {
        //on supprime tous les SR associés
        final SystemeReperageRepository srrepo = InjectorCore.getBean(SystemeReperageRepository.class);
        final List<SystemeReperage> srs = srrepo.getByLinearId(entity.getId());
        for(SystemeReperage sr : srs) {
            srrepo.remove(sr, entity);
        }

        final SessionCore session = InjectorCore.getBean(SessionCore.class);
        List<Positionable> boundPositions = TronconUtils.getPositionableList(entity);
        for (Positionable p : boundPositions) {
            if (p.getDocumentId().equals(p.getId())) {
                try {
                    AbstractSIRSRepository repo = session.getRepositoryForClass(p.getClass());
                    repo.remove(p);
                } catch (Exception e) {
                    SirsCore.LOGGER.log(Level.WARNING, "An element bound to the troncon cannot be deleted : " + p.getId(), e);
                }
            }
        }

        // Supprime toutes les bornes utilisées uniquement par ce troncon.
        final HashSet<String> bornesToDelete = new HashSet<>(entity.getBorneIds());
        ViewQuery usedBornes = createQuery(BY_BORNE_ID).includeDocs(false).keys(bornesToDelete);
        ViewResult queryResult = db.queryView(usedBornes);
        queryResult.forEach(row -> {
            if (!row.getId().equals(entity.getId())) {
                bornesToDelete.remove(row.getKey());
            }
        });

        if (bornesToDelete.isEmpty())
            return;

        // On peut faire un bulk delete, tous les SR et objets sur le tronçon ont disparu. Il n'y a plus de risque de corruption d'un positionnement.
        final AbstractSIRSRepository<BorneDigue> borneRepo = session.getRepositoryForClass(BorneDigue.class);
        final List<BorneDigue> tmpBornes = borneRepo.get(bornesToDelete);
        final List<DocumentOperationResult> bulkResult = borneRepo.executeBulkDelete(tmpBornes);
        // Si une erreur s'est produite lors de la suppression, on liste les opérations échouées, et on renvoie une alerte à l'utilisateur.
        if (bulkResult != null && !bulkResult.isEmpty()) {

            // TODO : système de message pour communiquer entre core & desktop ?
            SirsCore.LOGGER.warning(() -> {
                final HashMap<String, BorneDigue> borneMap = new HashMap<>(tmpBornes.size());
                for (final BorneDigue tmpBorne : tmpBornes) {
                    borneMap.put(tmpBorne.getId(), tmpBorne);
                }

                final StringBuilder msgBuilder = new StringBuilder("Les bornes suivantes n'ont pas pu être supprimées :");
                for (final DocumentOperationResult r : bulkResult) {
                    BorneDigue bd = borneMap.get(r.getId());
                    msgBuilder.append('\n').append(bd.getLibelle()).append("\nErreur : ").append(r.getError()).append("\nCause : ").append(r.getReason());
                }

                return msgBuilder.toString();
            });
        }
    }

    @Override
    protected T onLoad(final T toLoad) {
        toLoad.geometryProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                registerForPRComputing(toLoad);
            }
        });
        new DefaultSRChangeListener(toLoad, this);
        return toLoad;
    }

    public void registerForPRComputing(final T tr) {
        if (tr == null)
            return;
        synchronized (prUpdates) {
            final Iterator<WeakReference<T>> it = prUpdates.iterator();
            T tmp;
            while (it.hasNext()) {
                tmp = it.next().get();
                if (tmp == null) {
                    it.remove();
                } else if (tr == tmp) {
                    return;
                }
            }

            // Object has not been found into registered ones, we can add it now.
            prUpdates.add(new WeakReference<>(tr));
        }
    }

    /**
     * Check if any submitted object needs a PR update (compute PRs of all objects
     * positioned amongst it), and launch it.
     * @param toCheck The {@link TronconDigue} objects to check.
     */
    private void checkPRUpdate(final Collection<T> toCheck) {
        if (toCheck == null || toCheck.size() < 1)
            return;

        final HashSet<T> trs = new HashSet<>(toCheck);
        synchronized (prUpdates) {
            final Iterator<WeakReference<T>> it = prUpdates.iterator();
            while (it.hasNext()) {
                final T tmp = it.next().get();
                if (tmp == null) {
                    it.remove();
                } else if (trs.remove(tmp)) {
                    PRComputer computer = prComputings.get(tmp);
                    if (computer != null) {
                        computer.cancel();
                    }

                    computer = new PRComputer(tmp);
                    prComputings.put(tmp, computer);
                    computer.runningProperty().addListener((obs, oldValue, newValue) -> {
                        if (oldValue && !newValue)
                            prComputings.remove(tmp);
                    });
                    TaskManager.INSTANCE.submit(computer);
                    it.remove();
                }
            }
        }
    }
}
