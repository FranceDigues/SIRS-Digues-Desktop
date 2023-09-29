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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.ektorp.CouchDbConnector;
import org.ektorp.Options;
import org.ektorp.changes.ChangesCommand;
import org.ektorp.changes.ChangesFeed;
import org.ektorp.changes.DocumentChange;
import fr.sirs.core.DocHelper;

import fr.sirs.core.SirsCore;
import fr.sirs.core.model.Element;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * This component forward all document events to its listener.
 *
 * @author olivier.nouguier@geomatys.com
 */
public class DocumentChangeEmiter {

    /**
     * Group all events spaced by less than specified bulk time in a single event shot.
     */
    private static final long BULK_TIME = 1;
    private static final TimeUnit BULK_UNIT = TimeUnit.SECONDS;

    private static final char[] BUFFER = new char[4096];

    /** A pattern to identify new elements. They must have a revision number equal to 1 */
    private static final Pattern FIRST_REVISION = Pattern.compile("^1\\D.*");

    // TODO : Transform to Observable list ?
    private final List<DocumentListener> listeners = new ArrayList<>();
    private final CouchDbConnector connector;

    public DocumentChangeEmiter(CouchDbConnector connector) {
        this.connector = connector;
    }

    public Thread start() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                final ChangesCommand cmd = new ChangesCommand.Builder().includeDocs(true).since(connector.getDbInfo().getUpdateSeq()).build();
                final ChangesFeed feed = connector.changesFeed(cmd);
                while (feed.isAlive()) {
                    try {
                        handleChanges(feed);
                    } catch (Exception e) {
                        log(e);
                    }
                }
            };
        };

        thread.setName("CouchDB change watch");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    protected Optional<Element> getDeleted(final String deletedDoc) throws IOException, ClassNotFoundException {
        final DeletedCouchDbDocument deleted = DocHelper.SIMPLE_MAPPER.readValue(deletedDoc, DeletedCouchDbDocument.class);
        return DocHelper.toElement(getAsString(deleted.getId(), Optional.of(deleted.getRevision())));
    }

    private String getAsString(String id, Optional<String> rev) throws IOException {
        try (final InputStream stream = rev.isPresent() ? connector.getAsStream(id, new Options().revision(rev.get())) : connector.getAsStream(id);
                final InputStreamReader reader = new InputStreamReader(stream, "UTF-8")) {
            final StringBuilder builder = new StringBuilder();
            int readSize;
            while ((readSize = reader.read(BUFFER)) >= 0) {
                builder.append(BUFFER, 0, readSize);
            }

            return builder.toString();
        }
    }

    public synchronized boolean addListener(DocumentListener listener) {
        return listeners.add(listener);
    }

    public synchronized boolean removeListener(DocumentListener listener) {
        return listeners.remove(listener);
    }

    /**
     * @return a view of all listeners currently active. Never null, but can be empty.
     */
    public synchronized List<DocumentListener> getListenersUnmodifiable() {
        return Collections.unmodifiableList(listeners);
    }

    private void handleChanges(ChangesFeed feed) throws InterruptedException {

        final Thread currentThread = Thread.currentThread();
        final Map<Class, List<Element>> tmpAddedElements = new HashMap<>();
        final Map<Class, List<Element>> tmpChangedElements = new HashMap<>();
        Set<String> removedElements = new HashSet<>();

        while (!currentThread.isInterrupted()) {
            if(feed.isAlive()){
                final DocumentChange change = feed.next(BULK_TIME, BULK_UNIT);
                if (change == null)
                    break;

                try {
                    if (change.isDeleted()) {
                        removedElements.add(change.getId());
                    } else if (change.getRevision()!=null && FIRST_REVISION.matcher(change.getRevision()).find()) {
                        DocHelper.toElement(change.getDoc()).ifPresent((Element e) -> putElement(e, tmpAddedElements));
                    } else if(change.getDoc()!=null) {
                        DocHelper.toElement(change.getDoc()).ifPresent((Element e) -> putElement(e, tmpChangedElements));
                    }
                    else {
                        SirsCore.LOGGER.log(Level.WARNING, String.format("Unknown change detected that is neither deletion, creation or document update"));
                    }
                } catch (Exception e) {
                    SirsCore.LOGGER.log(Level.WARNING, "An error occurred while analyzing a database change. Change will be ignored.", e);
                }
            }
        }

        // Prevent modification
        final Map<Class, List<Element>> addedElements = Collections.unmodifiableMap(tmpAddedElements);
        final Map<Class, List<Element>> changedElements = Collections.unmodifiableMap(tmpChangedElements);
        removedElements = Collections.unmodifiableSet(removedElements);

        List<DocumentListener> docCreated = new ArrayList<>();
        List<DocumentListener> docChanged = new ArrayList<>();
        List<DocumentListener> docDeleted = new ArrayList<>();
        for (DocumentListener listener : getListenersUnmodifiable()) {
            if (!addedElements.isEmpty()) {
                docCreated.add(listener);
            }
            if (!changedElements.isEmpty()) {
                docChanged.add(listener);
            }
            if (!removedElements.isEmpty()) {
                docDeleted.add(listener);
            }
        }

        for (DocumentListener dl : docCreated) {
            dl.documentCreated(addedElements);
        }
        for (DocumentListener dl : docChanged) {
            dl.documentChanged(changedElements);
        }
        for (DocumentListener dl : docDeleted) {
            dl.documentDeleted(removedElements);
        }
    }

    private static void putElement(final Element e, final Map<Class, List<Element>> output) {
        List<Element> registry = output.get(e.getClass());
        if (registry == null) {
            registry = new ArrayList<>();
            output.put(e.getClass(), registry);
        }
        registry.add(e);
    }

    private void log(Exception e) {
        SirsCore.LOGGER.log(Level.WARNING, e.getMessage(), e);
    }
}
