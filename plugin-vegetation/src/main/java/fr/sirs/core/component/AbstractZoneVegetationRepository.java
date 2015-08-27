package fr.sirs.core.component;

import fr.sirs.core.model.ParcelleVegetation;
import fr.sirs.core.model.ZoneVegetation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @param <T>
 */
public abstract class AbstractZoneVegetationRepository<T extends ZoneVegetation> extends AbstractSIRSRepository<T> {
    
    public static final String BY_PARCELLE_ID = "byParcelleId";
    
    public AbstractZoneVegetationRepository(Class<T> type, CouchDbConnector db) {
        super(type, db);
    }
    
    public List<T> getByParcelleId(final String parcelleId) {
        ArgumentChecks.ensureNonNull("Parcelle", parcelleId);
        return this.queryView(BY_PARCELLE_ID, parcelleId);
    }
    
    public List<T> getByParcelle(final ParcelleVegetation parcelle){
        ArgumentChecks.ensureNonNull("Parcelle", parcelle);
        return this.getByParcelleId(parcelle.getId());
    }

    public List<T> getByParcelleIds(final String... parcelleIds) {
        ArgumentChecks.ensureNonNull("Parcelles", parcelleIds);
        return this.queryView(BY_PARCELLE_ID, (Object[]) parcelleIds);
    }

    public List<T> getByParcelleIds(final Collection<String> parcelleIds) {
        ArgumentChecks.ensureNonNull("Parcelles", parcelleIds);
        return this.queryView(BY_PARCELLE_ID, parcelleIds);
    }

    /**
     * Need to loop over parcelles to extract their ids. For retrieving zones
     * by parcelle from a loop, prefer extracting ids once out of the loop and
     * use getByParcelleIds.
     * 
     * @param parcelles
     * @return 
     */
    public List<T> getByParcelles(final Collection<ParcelleVegetation> parcelles) {
        ArgumentChecks.ensureNonNull("Parcelles", parcelles);
        final Collection<String> parcelleIds = new ArrayList<>();
        for(final ParcelleVegetation parcelle : parcelles) parcelleIds.add(parcelle.getId());
        return getByParcelleIds(parcelleIds);
    }

    /**
     * Need to loop over parcelles to extract their ids. For retrieving zones
     * by parcelle from a loop, prefer extracting ids once out of the loop and
     * use getByParcelleIds.
     * 
     * @param parcelles
     * @return 
     */
    public List<T> getByParcelles(final ParcelleVegetation... parcelles) {
        ArgumentChecks.ensureNonNull("Parcelles", parcelles);
        final Collection<String> parcelleIds = new ArrayList<>();
        for(final ParcelleVegetation parcelle : parcelles) parcelleIds.add(parcelle.getId());
        return getByParcelleIds(parcelleIds);
    }
}
