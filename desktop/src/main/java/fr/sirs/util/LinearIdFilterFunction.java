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
import fr.sirs.Session;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.Identifiable;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.filter.function.AbstractFunction;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Dynamic filter function that checks if a feature's linearId belongs to a specific
 * linear type (e.g., TronconDigue, Berge, TronconLit).
 * <p>
 * This filter is evaluated at render time, ensuring that newly created linear objects
 * are automatically included in filtered layers without needing to rebuild the map context.
 * <p>
 * Redmine ticket: 7631
 * <p>
 * The function caches linear IDs per type. The cache is invalidated by
 * {@link LinearIdCacheInvalidator} when linear objects are created or deleted.
 * <p>
 * Usage:
 * <pre>
 * Filter filter = FF.equals(
 *     FF.function(LinearIdFilterFunction.NAME, FF.literal("fr.sirs.core.model.Berge")),
 *     FF.literal(true)
 * );
 * </pre>
 *
 * @author Estelle Idee (Geomatys)
 * @see LinearIdCacheInvalidator
 */
public class LinearIdFilterFunction extends AbstractFunction {
    public static final String NAME = "linearIdFilter";

    /**
     * Cache of linear IDs per type to avoid repeated database queries.
     * Key: canonical class name, Value: Set of IDs.
     * Cache is invalidated by {@link LinearIdCacheInvalidator} when documents change.
     */
    private static final Map<String, Set<String>> cache = new ConcurrentHashMap<>();

    /**
     * The canonical name of the linear type to filter by.
     */
    private final String linearTypeCanonicalName;

    /**
     * Creates a new LinearIdFilterFunction.
     *
     * @param linearTypeCanonicalName Literal containing the canonical class name of the linear type
     *                                 (e.g., "fr.sirs.core.model.Berge")
     */
    public LinearIdFilterFunction(final Literal linearTypeCanonicalName) {
        super(NAME, new Expression[]{linearTypeCanonicalName}, null);
        ArgumentChecks.ensureNonNull("linearTypeCanonicalName", linearTypeCanonicalName);
        this.linearTypeCanonicalName = linearTypeCanonicalName.getValue().toString();
    }

    /**
     * Evaluates whether the given feature's linearId belongs to the configured linear type.
     *
     * @param object The feature being evaluated (must be a BeanFeature)
     * @return true if the feature's linearId matches an ID of the configured linear type, false otherwise
     */
    @Override
    public Object evaluate(Object object) {
        if (!(object instanceof BeanFeature)) {
            return false;
        }

        final BeanFeature feature = (BeanFeature) object;
        final Object linearIdObj = feature.getPropertyValue("linearId");
        if (linearIdObj == null) return false;

        final String linearId = linearIdObj.toString();

        // Get cached IDs, loading from database if not present
        final Set<String> linearIds = cache.computeIfAbsent(
                linearTypeCanonicalName,
                LinearIdFilterFunction::loadIds
        );

        return linearIds.contains(linearId);
    }

    /**
     * Loads IDs from the database for the given linear type.
     *
     * @param canonicalName The canonical class name of the linear type
     * @return Set of IDs for the linear type, or empty set if repository not found
     */
    private static Set<String> loadIds(String canonicalName) {
        try {
            final Session session = Injector.getBean(Session.class);
            if (session == null) {
                return Collections.emptySet();
            }

            final AbstractSIRSRepository<?> repo = session.getRepositoryForType(canonicalName);
            if (repo == null) {
                return Collections.emptySet();
            }

            return repo.getAll().stream()
                    .map(Identifiable::getId)
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            SIRS.LOGGER.log(Level.WARNING, "Error while recovering ids from database for the linear type : " + canonicalName, e);
            return Collections.emptySet();
        }
    }

    /**
     * Invalidates the cache for a specific linear type.
     * Called by {@link LinearIdCacheInvalidator} when linear objects are created or deleted.
     *
     * @param canonicalName The canonical class name to invalidate, or null to invalidate all
     */
    public static void invalidateCache(String canonicalName) {
        if (canonicalName == null) {
            cache.clear();
        } else {
            cache.remove(canonicalName);
        }
    }

    /**
     * On deletion, we don't have the class of the deleted element.
     * Look for deleted ids in cache and invalidate corresponding cache.
     * @param ids : ids of the deleted elements.
     */
    public static void invalidateCacheByObjectId(Set<String> ids) {
        for (Map.Entry<String, Set<String>> stringSetEntry : cache.entrySet()) {
            final Set<String> cacheValues = stringSetEntry.getValue();
            final String className = stringSetEntry.getKey();
            for (String id : ids) {
                if (cacheValues != null && cacheValues.contains(id)) {
                    invalidateCache(className);
                }
            }
        }
    }
}
