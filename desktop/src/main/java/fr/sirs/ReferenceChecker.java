package fr.sirs;

import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.ReferenceType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javafx.concurrent.Task;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import static fr.sirs.SIRS.REFERENCE_GET_ID;
import static java.util.logging.Level.WARNING;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class ReferenceChecker extends Task<Void> {
    
    
    private static final String BOOLEAN_PRIMITIVE_NAME = "boolean";
    private static final String FLOAT_PRIMITIVE_NAME = "float";
    private static final String DOUBLE_PRIMITIVE_NAME = "double";
    private static final String INTEGER_PRIMITIVE_NAME = "int";
    private static final String LONG_PRIMITIVE_NAME = "long";

    private String referencesDirectoryPath;
    
    private ReferenceChecker(){}
    
    public ReferenceChecker(final String referencesDirectoryPath){
        this.referencesDirectoryPath = referencesDirectoryPath;
        updateTitle("Vérification des références");
    }

    private final Map<Class<ReferenceType>, Map<ReferenceType, ReferenceType>> incoherentReferences = new HashMap<>();
    
    private final Map<Class, List<ReferenceType>> serverInstancesNotLocal = new HashMap<>();
    public Map<Class, List<ReferenceType>> getServerInstancesNotLocal() { return serverInstancesNotLocal;}

    private final Map<Class<ReferenceType>, List<ReferenceType>> localInstancesNotOnTheServer = new HashMap<>();
    public Map<Class<ReferenceType>, List<ReferenceType>> getLocalInstancesNotOnTheServer() { return localInstancesNotOnTheServer;}

    private List<Class<? extends ReferenceType>> localClassReferences;
    public List<Class<? extends ReferenceType>> getLocalClassReferences() {return localClassReferences;}

    private List<String> localClassNameReferencesNotOnServer;
    public List<String> getLocalClassNameReferencesNotOnServer() {return localClassNameReferencesNotOnServer;}

    private void checkAllReferences() {
        updateMessage("Vérification des références.");

        ////////////////////////////////////////////////////////////////////////
        /*
         Récupération des classes de référence de l'application. 
         */
        localClassReferences = Session.getReferences();
        final int progressSize = localClassReferences.size();
        int progress = 0;
        updateProgress(0, progressSize);
        
        /*
         On vérifie ensuite l'identité des instances : pour chaque classe de 
         référence locale, pourvu qu'elle soit référencée sur le serveur,
         on va vérifier que les instances sont "identiques" (identité à définir).
         */
        updateMessage("Vérification des instances de références.");
        for (final Class reference : localClassReferences) {
            updateMessage("Vérification des instances de la classe de référence "+reference.getName());
            try {
                final ReferenceClassChecker referenceClassChecker = new ReferenceClassChecker(reference);
                referenceClassChecker.checkReferenceClass();
                referenceClassChecker.update();
            } catch (IOException ex) {
                SIRS.LOGGER.log(WARNING, "Unable to check reference class "+reference.getName(), ex);
            }
            updateProgress(progress++, progressSize);
        }
    }

    @Override
    protected Void call() throws Exception {
        checkAllReferences();
        return null;
    }

    public final class ReferenceClassChecker {

        private final Class<ReferenceType> referenceClass;
        private List<ReferenceType> fileReferences;
        private List<ReferenceType> localReferences;

        private ReferenceClassChecker(final Class referenceClass) {
            this.referenceClass = referenceClass;
        }

        /**
         * Check one reference class
         *
         * @param referenceClass
         */
        private boolean checkReferenceClass() throws IOException {
            try {
                final File referenceFile = retriveFileFromURL(referenceURL());

                fileReferences = readReferenceFile(referenceFile);
                localReferences = Injector.getSession().getRepositoryForClass(referenceClass).getAll();

                final List<ReferenceType> currentServerInstances = new ArrayList(fileReferences);

                /*
                 On vérifie que toutes les instances du serveur sont présentes localement.
                 Sinon, on récupère les instances du serveur non présentes localement.
                 */
                final Method getId = referenceClass.getMethod(REFERENCE_GET_ID);

                /*
                 Pour toutes les instances de références locales :
                 1) On vérifie qu'elle est bien présente sur les serveur ;
                 2) Que son état sur le serveur est identique à son état local ;
                 3) Si elle est présente sur le serveur et que son état a changé, on enregistre les deux états dans la map dédiée.
                 4) Si elle n'est pas présente sur le serveur, on l'enregistre dans la liste dédiée.
                 */
                for (final ReferenceType localReferenceInstance : localReferences) {
                    boolean presentOnServer = false;

                    try {
                        final Object localId = getId.invoke(localReferenceInstance);
                        for (final ReferenceType serverReferenceInstance : currentServerInstances) {
                            try {
                                final Object serverId = getId.invoke(serverReferenceInstance);
                                if (localId instanceof String
                                        && localId.equals(serverId)) {
                                    presentOnServer = true;
                                    currentServerInstances.remove(serverReferenceInstance);
                                    if (!sameReferences(localReferenceInstance, serverReferenceInstance)) {
                                        registerIncoherentReferences(localReferenceInstance, serverReferenceInstance);
                                    }
                                    break;
                                }
                            } catch (IllegalAccessException | InvocationTargetException ex) {
                                SIRS.LOGGER.log(Level.SEVERE, ex.getMessage());
                            }
                        }
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        SIRS.LOGGER.log(Level.SEVERE, ex.getMessage());
                    }

                    if (!presentOnServer) {
                        if (localInstancesNotOnTheServer.get(referenceClass) == null) {
                            localInstancesNotOnTheServer.put(referenceClass, new ArrayList());
                        }
                        localInstancesNotOnTheServer.get(referenceClass).add(localReferenceInstance);
                    }
                }

                /*
                 Maintenant qu'on a vérifié que toutes les références locales de la 
                 classe étaient sur le serveur (cas des instances de références qui 
                 auraient été supprimées sur le serveur), il faut vérifier s'il n'y 
                 aurait pas des instances de références qui seraient présentes sur 
                 le serveur mais pas localement (cas des instances de références qui 
                 auraient été ajoutées sur le serveur). 
            
                 Pour cela Il faut examiner uniquement les instances de références du 
                 serveur qui n'auraient pas trouvé de référence locale 
                 correspondante (danslla liste currentServerInstances qui a été mise 
                 à jour au fur et à mesure).
                 */
                serverInstancesNotLocal.put(referenceClass, currentServerInstances);

            } catch (MalformedURLException ex) {
                SIRS.LOGGER.log(Level.SEVERE, ex.getMessage());
                return false;
            } catch (NoSuchMethodException | SecurityException ex) {
                SIRS.LOGGER.log(Level.SEVERE, ex.getMessage());
                return false;
            }
            return true;
        }

        /**
         * Build URL for given reference class.
         *
         * @param locationDir
         * @param referenceClass
         * @return
         * @throws MalformedURLException
         */
        private URL referenceURL() throws MalformedURLException {
            return new URL(referencesDirectoryPath + referenceClass.getSimpleName() + ".csv");
        }
        
        
        /**
         * 
         * Builds a reference instance form A CSV record.
         *
         * Note : this method needs the name fiels of the given referenceClass
         * map the header column names.
         *
         * This method supports the following field types :
         *
         * <ol>
         * <li>String. Note if the column name is set to "id", the method
         * generates an id by concatenating the referenceClass simple name to
         * ":" and to the value of the id column;</li>
         * <li>LocalDate (see ISO_DATE format);</li>
         * <li>LocalDateTime (see ISO_DATE_TIME format).</li>
         * </ol>
         * 
         * @param record
         * @return
         * @throws NoSuchMethodException
         * @throws InstantiationException
         * @throws IllegalAccessException
         * @throws IllegalArgumentException
         * @throws InvocationTargetException 
         */
        private ReferenceType buildReferenceInstance(final CSVRecord record) {

            if (record == null) {
                return null;
            }

            ReferenceType referenceInstance = null;

            try {
                final Constructor<ReferenceType> constructor = referenceClass.getConstructor();
                referenceInstance = constructor.newInstance();
                referenceInstance.setValid(true);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                SIRS.LOGGER.log(Level.SEVERE, null, ex);
            }

            for (final String header : record.toMap().keySet()) {
                try {
                    final Method getter = referenceClass.getMethod("get" + header.substring(0, 1).toUpperCase() + header.substring(1));
                    final Class type = getter.getReturnType();
                    final Method setter = referenceClass.getMethod("set" + header.substring(0, 1).toUpperCase() + header.substring(1), type);

                    if (String.class.equals(type)) {
                        
                        // POUR LES REFERENCES, L'identifiant sert
                        if ("id".equals(header)) {
                            // On construit un identifiant couchDB en concaténant le nom de la référence à l'identifiant avec le séparateur ":"
                            setter.invoke(referenceInstance, referenceClass.getSimpleName() + ":" + record.get(header));
                            // Pour les références, l'identifiant des fichiers sert à construire la désignation
                            referenceClass.getMethod(SIRS.REFERENCE_SET_DESIGNATION, String.class).invoke(referenceInstance, record.get(header));
                        } else {
                            setter.invoke(referenceInstance, record.get(header));
                        }
                    } else if(Integer.class.equals(type) || INTEGER_PRIMITIVE_NAME.equals(type.getName())){
                        setter.invoke(referenceInstance, Integer.parseInt(record.get(header)));
                    } else if(Long.class.equals(type) || LONG_PRIMITIVE_NAME.equals(type.getName())){
                        setter.invoke(referenceInstance, Long.parseLong(record.get(header)));
                    } else if(Float.class.equals(type) || FLOAT_PRIMITIVE_NAME.equals(type.getName())){
                        setter.invoke(referenceInstance, Float.parseFloat(record.get(header)));
                    } else if(Double.class.equals(type) || DOUBLE_PRIMITIVE_NAME.equals(type.getName())){
                        setter.invoke(referenceInstance, Double.parseDouble(record.get(header)));
                    } else if (LocalDateTime.class.equals(type)) {
                        try {
                            setter.invoke(referenceInstance, LocalDateTime.of(LocalDate.parse(record.get(header), DateTimeFormatter.ISO_DATE), LocalTime.MIN));
                        } catch (DateTimeParseException ex) {
                            try {
                                setter.invoke(referenceInstance, LocalDateTime.parse(record.get(header), DateTimeFormatter.ISO_DATE_TIME));
                            } catch (DateTimeParseException ex2) {
                                SIRS.LOGGER.log(Level.FINE, ex2.getMessage());
                            }
                            SIRS.LOGGER.log(Level.FINE, ex.getMessage());
                        }
                    } else if (LocalDate.class.equals(type)) {
                        try {
                            setter.invoke(referenceInstance, LocalDate.parse(record.get(header), DateTimeFormatter.ISO_DATE));
                        } catch (DateTimeParseException ex) {
                            SIRS.LOGGER.log(Level.FINE, ex.getMessage());
                        }
                    }

                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                    SIRS.LOGGER.log(Level.FINE, null, ex);
                }
            }

            return referenceInstance;
        }

        private List<ReferenceType> readReferenceFile(final File file) throws FileNotFoundException, IOException {
            
            final List<ReferenceType> fileRefs = new ArrayList<>();
            
            try(final InputStream inputStream = new FileInputStream(file)){
                final Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(new InputStreamReader(inputStream, "UTF-8"));
                for (final CSVRecord record : records) {
                    final ReferenceType referenceInstance = buildReferenceInstance(record);
                    if(referenceInstance!=null) fileRefs.add(referenceInstance);
                }
            }
            return fileRefs;
        }
        
        private void update(){
            final List<ReferenceType> updated = new ArrayList<>();
            if(fileReferences!=null){
                final AbstractSIRSRepository repository = Injector.getSession().getRepositoryForClass(referenceClass);
                for(final ReferenceType fileReference : fileReferences){
                    ReferenceType localInstance = null;
                    for(final ReferenceType localReference : localReferences){
                        if(fileReference.equals(localReference)) {
                            localInstance = localReference;
                            break;
                        }
                    }

                    if(localInstance==null){
                        updated.add(fileReference); // On mémorise la référence à ajouter
                    }
                    else{
                        if(!localInstance.toString().equals(fileReference.toString())){
                            try {
                                final Method getRevision = referenceClass.getMethod("getRevision");
                                final Method setRevision = referenceClass.getMethod("setRevision", String.class);
                                final String revision = (String) getRevision.invoke(localInstance);
                                setRevision.invoke(fileReference, revision);
                                updated.add(fileReference);// On mémorise la référence à mettre à jour
                            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                SIRS.LOGGER.log(WARNING, ex.getMessage());
                            }
                        }
                    }
                }
                if(!updated.isEmpty()) repository.executeBulk(updated);
            }
            
            // On élimine les instances de références mises à jour des map correspondantes du ReferenceChecker (on se base sur l'identifiant).
            if(serverInstancesNotLocal.get(referenceClass)!=null){
                serverInstancesNotLocal.get(referenceClass).removeAll(updated);
            } else {
                SIRS.LOGGER.log(WARNING, referenceClass.getCanonicalName() + " n'a pas été correctement récupérée du serveur.");
            }
            
            // On retire les références mises à jour de la liste des instances incohérentes.
            final Map<ReferenceType, ReferenceType> incoherentInstances = incoherentReferences.get(referenceClass);
            if(incoherentInstances!=null){
                for(final ReferenceType updatedInstance : updated){
                    final Set<ReferenceType> keySet = incoherentInstances.keySet();
                    for(final ReferenceType localInstanceKey : keySet){
                        if(incoherentInstances.get(localInstanceKey)==updatedInstance){
                            incoherentInstances.remove(localInstanceKey);
                            break;
                        }
                    }
                }
            }
        }

        private void registerIncoherentReferences(final ReferenceType localReferenceInstance, final ReferenceType serverReferenceInstance) {

            if (referenceClass == null 
                    || localReferenceInstance == null 
                    || serverReferenceInstance == null) 
                return;

            if (incoherentReferences.get(referenceClass) == null) 
                incoherentReferences.put(referenceClass, new HashMap<>());

            incoherentReferences.get(referenceClass).put(localReferenceInstance, serverReferenceInstance);
        }

    }

    /**
     * Compare reference instances.
     *
     * @param localReferenceInstance
     * @param serverReferenceInstance
     * @return
     */
    private static boolean sameReferences(final ReferenceType localReferenceInstance, 
            final ReferenceType serverReferenceInstance) {
        // equals ne vérifie pas l'identité de contenu, mais l'égalité des identifiants !!!!
        // La méthode equals ne se basant que sur les ID, on vérifie en plus l'égalité des contenus avec "toString"
        return localReferenceInstance.equals(serverReferenceInstance)
                && localReferenceInstance.toString().equals(serverReferenceInstance.toString());
    }
    
    /**
     * Return the file content located at the URL.
     *
     * @param url
     * @return null if the input URL is null
     * @throws IOException
     */
    private static File retriveFileFromURL(final URL url) throws IOException {

        if (url == null) {
            return null;
        }

        final File file = File.createTempFile("tempReference", ".csv"); 
        
        final URLConnection connection = url.openConnection();
        try{
            try (final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
                 final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file))) {
                
                int r = 0;
                while (true) {
                    r = inputStreamReader.read();
                    if (r != -1) {
                        outputStreamWriter.write(r);
                    } else {
                        break;
                    }
                }
                outputStreamWriter.flush();
                outputStreamWriter.close();
                inputStreamReader.close();
            } 
        } catch (NullPointerException ex){
            SIRS.LOGGER.log(Level.WARNING, ex.getMessage());
            return null;
        }
        return file;
    }
}
