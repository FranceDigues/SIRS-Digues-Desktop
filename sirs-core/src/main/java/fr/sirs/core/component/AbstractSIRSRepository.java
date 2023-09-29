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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import fr.sirs.core.CacheRules;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.model.AvecDateMaj;
import fr.sirs.core.model.AvecForeignParent;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Identifiable;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.ReferenceType;
import fr.sirs.util.ClosingDaemon;
import fr.sirs.util.ConvertPositionableCoordinates;
import fr.sirs.util.StreamingIterable;
import java.lang.ref.PhantomReference;
import java.lang.ref.WeakReference;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javafx.collections.ObservableList;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.Cache;
import org.ektorp.BulkDeleteDocument;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.DocumentOperationResult;
import org.ektorp.StreamingViewResult;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;
import org.geotoolkit.util.collection.CloseableIterator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base pour les outils gérant les échanges avec la bdd CouchDB.
 *
 * Note : Le cache qui permet de garder une instance unique en mémoire pour un
 * objet donné est extrêmement important pour les opérations de sauvegarde.
 *
 * Ex : On a un objet A, éditable. C'est un CouchDB document, avec un id et un
 * numéro de révision.
 *
 * On ouvre l'éditeur de A. Il est donc chargé en mémoire. Dans le même temps on
 * ouvre une table dans un autre onglet. Elle contient tous les objets du même
 * type que A. Elle contient donc aussi A.
 *
 * Maintenant, que se passe t'il sans cache ? On a deux copies de A en mémoire
 * pour une même révision (disons 0).
 *
 * Si on sauvegarde via l'éditeur de A, il passe en révision 1 dans la bdd, mais
 * pour UNE SEULE des 2 copies en mémoire. En conséquence de quoi, lorsqu'on
 * modifie la copie dans la table, on demande à la base de faire une mise à jour
 * de la révision 1 en utilisant un objet de la révision 0.
 *
 * Résultat : ERREUR !
 *
 * Avec le cache, les deux éditeurs pointent sur la même copie en mémoire.
 * Lorsqu'un éditeur met à jour le tronçon, la révision de la copie est
 * indentée, le deuxième éditeur a donc un tronçon avec un numéro de révision
 * correct.
 *
 * NOTE : Pour éviter trop de requêtes et de calculs, on garde une réference vers
 * la liste contenant tous les éléments de ce répertoire, dès lors que l'utilisateur
 * l'a demandée. Pour garder cette liste à jour, on écoute les changements qui
 * interviennent sur CouchDB.
 *
 * @author Alexis Manin (Geomatys)
 * @param <T> The type of object managed by the current repository implementation.
 */
public abstract class AbstractSIRSRepository<T extends Identifiable> extends CouchDbRepositorySupport<T> implements DocumentListener {

    protected final Cache<String, T> cache;

    /**
     * Cache result of {@link #getAll() } method, to avoid querying from database
     * while someone is already working with it.
     */
    private WeakReference<ObservableList<T>> all;

    @Autowired
    protected GlobalRepository globalRepo;

    private StreamingViewIterable allStreaming;

    protected AbstractSIRSRepository(Class<T> type, CouchDbConnector db) {
        super(type, db);
        cache = new Cache(20, 0, CacheRules.cacheElementsOfType(type));
    }

    @Autowired(required=false)
    private void initListener(final DocumentChangeEmiter changeEmiter) {
        if (changeEmiter != null) {
            changeEmiter.addListener(this);
        }
    }

    @Override
    public T get(String id) {
        try {
            return cache.getOrCreate(id, () -> onLoad(super.get(id)));
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            // should never happen...
            throw new RuntimeException(ex);
        }
    }

    @Override
    public synchronized List<T> getAll() {
        if (all != null && all.get() != null) {
            return all.get();
        } else {
            final ObservableList<T> cachedList = SirsCore.observableList(cacheList(globalRepo.getAllForClass(type)));
            all = new WeakReference<>(cachedList);
            return cachedList;
        }
    }

    /**
     *
     * @return An iterable object which provides an iterator querying all documents
     * managed by current using a stream, to avoid memory overload.
     *
     * Note : provided iterators are closeable.
     */
    public synchronized StreamingIterable<T> getAllStreaming() {
        if (allStreaming == null) {
            allStreaming = new StreamingViewIterable(globalRepo.createByClassQuery(type));
        }
        return allStreaming;
    }

    private void checkIntegrity(T entity){
        if(entity instanceof AvecForeignParent){
            if(((AvecForeignParent) entity).getForeignParentId()==null) throw new IllegalArgumentException("L'élément ne peut être enregistré sans élement parent.");
        }
//        if(entity instanceof Positionable) {
//            ConvertPositionableCoordinates.COMPUTE_MISSING_COORD.test((Positionable) entity);
//        }
    }

    /**
     * Retrieve the elements of the given parameter class which Ids are provided
     * as parameters.
     *
     * @param ids Identifier of the documents to read from database.
     * @return Documents found for the given identifiers.
     */
    public List<T> get(final String... ids) {
        return get(Arrays.asList(ids));
    }

    /**
     * Retrieve the elements of the given parameter class which Ids are provided
     * as parameters.
     *
     * @param ids Identifier of the documents to read from database.
     * @return Documents found for the given identifiers.
     */
    public List<T> get(final Collection<String> ids) {
        final ArrayList result = new ArrayList();

        final List<String> toGet = new ArrayList<>(ids);
        final Iterator<String> idIt = toGet.iterator();
        while (idIt.hasNext()) {
            final T cached = cache.get(idIt.next());
            if (cached != null) {
                result.add(cached);
                idIt.remove();
            }
        }

        // On va chercher uniquement les documents qui ne sont pas en cache
        if (!toGet.isEmpty()) {
            final ViewQuery q = new ViewQuery().allDocs().includeDocs(true).keys(toGet);
            final List<T> bulkLoaded = db.queryView(q, getModelClass());

            for (T loaded : bulkLoaded) {
                // Note : It seems queried view returns null objects sometime...
                if (loaded == null)
                    continue;
                loaded = onLoad(loaded);
                cache.put(loaded.getId(), loaded);
                result.add(loaded);
            }
        }

        return result;
    }

    /**
     * Execute bulk for add/update operation on several documents.
     *
     * @param bulkList List of document to add or update
     * @return A list of error reports. Can be empty if all documents have been updated successfully.
     */
    public List<DocumentOperationResult> executeBulk(final Collection<T> bulkList){
        final List<T> cachedBulkList = new ArrayList<>();
        final List<T> entitiesWithoutIDs = new ArrayList<>();
        for(T entity : bulkList){
            if (entity instanceof AvecDateMaj && !(entity instanceof ReferenceType)) {
                ((AvecDateMaj) entity).setDateMaj(LocalDate.now());
            }
            // Put the updated entity into cache in case the old entity is different.
            // Si on n'a pas d'ID, c'est que l'entité est nouvelle et on ne peut pas la trouver dans le cache (NPE). Il faut donc la garder en réserve pour l'ajouter au cache après qu'un ID lui aura été attribué.
            if(entity.getId()==null){
                entitiesWithoutIDs.add(entity);
            }
            else {
                if (entity != cache.get(entity.getId())) {
                    entity = onLoad(entity);
                    cache.put(entity.getId(), entity);
                }
            }
            cachedBulkList.add(entity);
        }
        final List<DocumentOperationResult> result = db.executeBulk(cachedBulkList);

        // Avant de renvoyer le résultat, il faut ajouter au cache les entités qui n'avaient pas d'ID et qui en ont maintenant un après leur premier enregistrement.
        for (T e : entitiesWithoutIDs){
            e = onLoad(e);
            cache.put(e.getId(), e);
        }
        return result;
    }

    /**
     * Execute bulk for add/update operation on several documents.
     *
     * @param bulkList List of document to add or update
     * @return A list of error reports. Can be empty if all documents have been updated successfully.
     */
    public List<DocumentOperationResult> executeBulk(final T... bulkList){
        return executeBulk(Arrays.asList(bulkList));
    }

    /**
     * Execute bulk to delete several documents.
     *
     * @param bulkList List of document to remove.
     * @return A list of error reports. Can be empty if all documents have been deleted successfully.
     */
    public List<DocumentOperationResult> executeBulkDelete(final Iterable<T> bulkList){
        final List<BulkDeleteDocument> toDelete = new ArrayList<>();
        for(final T toBeDeleted : bulkList){
            toDelete.add(BulkDeleteDocument.of(toBeDeleted));
            cache.remove(toBeDeleted.getId());
        }
        return db.executeBulk(toDelete);
    }

    @Override
    public void add(T entity) {
        ArgumentChecks.ensureNonNull("Document à ajouter", entity);
        checkIntegrity(entity);
        //Not sure if needed
        if(entity instanceof Positionable) {
            ConvertPositionableCoordinates.COMPUTE_MISSING_COORD.test((Positionable) entity);
        }
        if (entity instanceof AvecDateMaj && !(entity instanceof ReferenceType)) {
            ((AvecDateMaj)entity).setDateMaj(LocalDate.now());
        }
        super.add(entity);
        cache.put(entity.getId(), onLoad(entity));
    }

    @Override
    public void update(T entity) {
        ArgumentChecks.ensureNonNull("Document à mettre à jour", entity);

        checkIntegrity(entity);
        if (entity instanceof AvecDateMaj && !(entity instanceof ReferenceType)) {
            ((AvecDateMaj) entity).setDateMaj(LocalDate.now());
        }
        super.update(entity);
        // Put the updated entity into cache in case the old entity is different.
        if (entity != cache.get(entity.getId())) {
            cache.put(entity.getId(), onLoad(entity));
        }
    }

    @Override
    public void remove(T entity) {
        ArgumentChecks.ensureNonNull("Document à supprimer", entity);
        cache.remove(entity.getId());
        removeConflictingRevisions(entity);
        super.remove(entity);
    }

    public void clearCache() {
        cache.clear();
    }

    @Override
    protected List<T> queryView(String viewName) {
        return cacheList(super.queryView(viewName));
    }

    @Override
    protected List<T> queryView(String viewName, ComplexKey key) {
        return cacheList(super.queryView(viewName, key));
    }

    @Override
    protected List<T> queryView(String viewName, int key) {
        return cacheList(super.queryView(viewName, key));
    }

    @Override
    protected List<T> queryView(String viewName, String key) {
        return cacheList(super.queryView(viewName, key));
    }

    protected List<T> queryView(String viewName, Object... keys) {
        return this.queryView(viewName, Arrays.asList(keys));
    }

    protected List<T> queryView(String viewName, Collection keys) {
        return cacheList(db.queryView(createQuery(viewName).includeDocs(true).keys(keys), type));
    }

    /**
     * Put all input element in cache, or replace by a previously cached element
     * if an input element Id can be found in the cache. Cannot be null.
     * @param source The list of element to put in cache or replace by previously cached value.
     * @return A list of cached elements. Never null, but can be empty. Should be of the
     * same size as input list.
     *
     * @throws NullPointerException if input list is null.
     */
    protected List<T> cacheList(List<T> source) {
        final List<T> result = new ArrayList<>(source.size());
        for (T element : source) {
            try {
                result.add(cache.getOrCreate(element.getId(), () -> onLoad(element)));
            } catch (Exception ex) {
                // Should never happen ...
                throw new RuntimeException(ex);
            }
        }
        return result;
    }

    /**
     * Perform an operation when loading an object. By default, nothing is done,
     * but implementations can override this to work with an element before putting
     * it in cache.
     * @param loaded The object which must be loaded.
     * @return The object to load. By default, the one in parameter.
     */
    protected T onLoad(final T loaded) {
        return loaded;
    }


    /**
     * @return the class of the managed object type.
     */
    public Class<T> getModelClass(){
        return type;
    }

    /**
     * Create a new instance of Pojo in memory. No creation in database.
     * @return the newly created object.
     */
    public abstract T create();

    /**
     * Return one element of T type.
     * If such elements exist into the database, this method returns the first found of all.
     * If such elements do not exist into the database, this method creates a new one, adds it to the database and returns it.
     * @return a random element found in database.
     */
    public T getOne(){
        if (!cache.isEmpty()) {
            return cache.values().iterator().next();
        }

        try (final CloseableIterator<T> it = getAllStreaming().iterator()) {
            if (it.hasNext()) {
                return it.next();
            } else {
                final T newOne = create();
                add(newOne);
                return newOne;
            }
        }
    }

    public T getFromCache(final String docId) {
        return cache.get(docId);
    }

    /**
     * Provides iterators which stream view result when browsing.
     *
     * TODO : Use {@link PhantomReference} to ensure freed iterators are closed.
     */
    protected class StreamingViewIterable implements StreamingIterable<T> {
        private final ViewQuery query;

        protected StreamingViewIterable(final ViewQuery query) {
            ArgumentChecks.ensureNonNull("View to query", query);
            this.query = query;
        }

        @Override
        public CloseableIterator<T> iterator() {
            final StreamingViewIterator iterator = new StreamingViewIterator(query);
            ClosingDaemon.watchResource(iterator, iterator.result);
            return iterator;
        }
    }

    /**
     * Ensure to delete all conflicting revisions from the entity.
     *
     * This mechanism is necessary because actually once Couchdb delete a document
     * by specifying its latest revision, the revision will get deleted as expected BUT
     * the dormant conflicting revision will immediately revert as the active revision
     * of the document. (origin of the issue 7490)
     *
     */
    public void removeConflictingRevisions(final T entity) {
        final List<List<String>> revisions = globalRepo.getConflictingRevisions(entity.getId());

        if (!revisions.isEmpty()) {
            // Must contains only one list
            if (revisions.size() > 1) SirsCore.LOGGER.log(Level.WARNING, String.format("Found multiple list of conflicting revision IDs ( %s ) for the doc._id=%s", revisions.toString(), entity.getId()));
            for (final String rev: revisions.get(0)) {
                final T toDelete = super.get(entity.getId(), rev);
                super.remove(toDelete);
            }
        }
    }

    /**
     * Create a connection to the given query and iterate through its results.
     * View is query on first {@link #hasNext()} call, to avoid keeping useless
     * connections opened.
     *
     * Note : This implementation is not thread-safe.
     */
    private class StreamingViewIterator implements CloseableIterator<T> {

        private final ViewQuery query;
        private StreamingViewResult result;
        private Iterator<ViewResult.Row> iterator;

        // Will be created only if we can effectively iterate on results.
        private ObjectReader objectReader = new ObjectMapper().reader(type);

        private T next;

        public StreamingViewIterator(ViewQuery query) {
            ArgumentChecks.ensureNonNull("Input query", query);
            this.query = query;
        }

        @Override
        public boolean hasNext() {
            // No element cached, we analyze input stream
            if (next == null) {
                // Open connection on first call.
                if (result == null) {
                    try{
                    result = db.queryForStreamingView(query);
                    } catch (Exception e) {
                        SirsCore.LOGGER.log(Level.WARNING, "Ektorp Streaming iterator failed retrieving next view element !.", e);
                    }
                    if (result.getTotalRows() > 0) {
                        this.iterator = result.iterator();
                        objectReader = new ObjectMapper().reader(type);
                    } else {
                        this.iterator = null;
                    }
                }

                /* If using directly parent iterable in a for each statement,
                 * iterator won't be closed, so we close it automatically when
                 * reaching end of the stream. We have to catch exceptions here, because
                 * it appears that iterator fails on empty views.
                 */
                boolean hasNext = false;
                try {
                    hasNext = iterator != null && iterator.hasNext();
                } catch (DbAccessException e) {
                    // Don't throw error because ektorp fails if view result returns an empty result set...
                    SirsCore.LOGGER.log(Level.FINE, "Ektorp Streaming iterator failed retrieving next view element ! (maybe due to empty result set).", e);
                }

                if (hasNext) {
                    // Cache next element
                    ViewResult.Row nextRow = iterator.next();
                    try {
                        next = cache.getOrCreate(nextRow.getId(), () -> objectReader.readValue(nextRow.getDocAsNode()));
                    } catch (Exception e) {
                        throw new SirsCoreRuntimeException(e);
                    }

                } else {
                    close();
                }
            }

            return next != null;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new IllegalStateException("Cannot call next when there is no more elements available.");
            }

            try {
                return next;
            } finally {
                // allow hasNext() method to search for next element.
                next = null;
            }
        }

        @Override
        public void close() {
            try {
                iterator = null;
                if (result != null) {
                    result.close();
                    result = null;
                }
            } catch (Exception e) {
                SirsCore.LOGGER.log(Level.WARNING, "A streamed CouchDB view result cannot be closed. It's likely to cause memory leaks.", e);
            }
        }
    }

    @Override
    public synchronized void documentCreated(Map<Class, List<Element>> added) {
        SirsCore.fxRunAndWait(() -> {
            final List<T> cached = all == null ? null : all.get();

            if (cached != null) {
                List<T> created = (List<T>) added.get(getModelClass());
                if (created != null) {
                    created = cacheList(created);
                    created.removeAll(cached);
                    cached.addAll(created);
                }
            }
        });
    }

    @Override
    public synchronized void documentChanged(Map<Class, List<Element>> changed) {
        all = null;
    }

    @Override
    public synchronized void documentDeleted(Set<String> deletedObjects) {
        SirsCore.fxRunAndWait(() -> {
            final List<T> cached = all == null ? null : all.get();
            if (cached != null) {
                cached.removeIf(element -> deletedObjects.contains(element.getId()));
            }
        });
    }
}
