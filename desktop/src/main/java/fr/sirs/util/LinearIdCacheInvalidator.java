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

import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.DocumentChangeEmiter;
import fr.sirs.core.component.DocumentListener;
import fr.sirs.core.model.Element;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Watches CouchDB changes for linear object creation and deletion.
 * When a linear object (TronconDigue, Berge, TronconLit) is created or deleted,
 * invalidates the corresponding cache in {@link LinearIdFilterFunction}.
 * <p>
 * This ensures that map layers using the {@link LinearIdFilterFunction} are automatically
 * updated when new linear objects are created, without needing to rebuild the entire map context.
 * <p>
 * Redmine ticket: 7631
 *
 * @author Estelle Idee (Geomatys)
 * @see LinearIdFilterFunction
 */
@Component
public class LinearIdCacheInvalidator implements DocumentListener {

    /**
     * Set of linear type simple names that should trigger cache invalidation.
     * These are the only classes that can be referenced by linearId.
     */
    private static final Set<String> LINEAR_TYPE_NAMES = new HashSet<>(Arrays.asList(
            "TronconDigue",
            "Berge",
            "TronconLit"
    ));

    @Autowired
    private Session session;

    @PostConstruct
    void init() {
        try {
            final DocumentChangeEmiter emiter = session.getApplicationContext().getBean(DocumentChangeEmiter.class);
            emiter.addListener(this);
        } catch (NoSuchBeanDefinitionException e) {
            SIRS.LOGGER.log(Level.WARNING, "No document change emitter registered.", e);
        }
    }

    @Override
    public void documentCreated(Map<Class, List<Element>> added) {
        // Invalidate cache only for the specific linear types that were created
        for (Class<?> clazz : added.keySet()) {
            if (LINEAR_TYPE_NAMES.contains(clazz.getSimpleName())) {
                LinearIdFilterFunction.invalidateCache(clazz.getCanonicalName());
            }
        }
    }

    @Override
    public void documentChanged(Map<Class, List<Element>> changed) {
        // No need to invalidate on updates - IDs don't change
    }

    @Override
    public void documentDeleted(Set<String> deleted) {
        LinearIdFilterFunction.invalidateCacheByObjectId(deleted);
    }
}
