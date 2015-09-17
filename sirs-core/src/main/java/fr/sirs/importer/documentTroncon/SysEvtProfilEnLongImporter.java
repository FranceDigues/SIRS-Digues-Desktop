package fr.sirs.importer.documentTroncon;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import static fr.sirs.core.LinearReferencingUtilities.buildGeometry;
import fr.sirs.core.model.BorneDigue;
import static fr.sirs.core.model.ElementCreator.createAnonymValidElement;
import fr.sirs.core.model.ProfilLong;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.importer.AccessDbImporterException;
import fr.sirs.importer.v2.linear.BorneDigueImporter;
import static fr.sirs.importer.DbImporter.TableName.SYS_EVT_PROFIL_EN_LONG;
import fr.sirs.importer.v2.linear.SystemeReperageImporter;
import fr.sirs.importer.documentTroncon.document.profilLong.ProfilEnLongImporter;
import fr.sirs.importer.v2.linear.TronconGestionDigueImporter;
import fr.sirs.importer.v2.CorruptionLevel;
import fr.sirs.importer.v2.ErrorReport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.ektorp.CouchDbConnector;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
class SysEvtProfilEnLongImporter extends GenericPositionDocumentImporter<ProfilLong> {

    private final ProfilEnLongImporter profilLongImporter;

    SysEvtProfilEnLongImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector,
            final TronconGestionDigueImporter tronconGestionDigueImporter,
            final BorneDigueImporter borneDigueImporter,
            final SystemeReperageImporter systemeReperageImporter,
            final ProfilEnLongImporter profilLongImporter) {
        super(accessDatabase, couchDbConnector, tronconGestionDigueImporter,
                borneDigueImporter, systemeReperageImporter);
        this.profilLongImporter = profilLongImporter;
    }

    private enum Columns {
        ID_DOC,
//        id_nom_element, // Redondant avec ID_DOC
//        ID_SOUS_GROUPE_DONNEES, // Redondant avec le type de données
//        LIBELLE_TYPE_DOCUMENT, // Redondant avec le type de document
//        DECALAGE_DEFAUT, // Affichage
//        DECALAGE, // Affichage
//        LIBELLE_SYSTEME_REP, // Redondant avec l'importaton des SR
//        NOM_BORNE_DEBUT, // Redondant avec l'importation des bornes
//        NOM_BORNE_FIN, // Redondant avec l'importation des bornes
//        NOM_PROFIL_EN_TRAVERS,
//        LIBELLE_MARCHE,
//        INTITULE_ARTICLE,
//        TITRE_RAPPORT_ETUDE,
//        ID_TYPE_RAPPORT_ETUDE,
//        TE16_AUTEUR_RAPPORT,
//        DATE_RAPPORT,
        ID_TRONCON_GESTION,
//        ID_TYPE_DOCUMENT,
//        ID_DOSSIER,
//        DATE_DEBUT_VAL,
//        DATE_FIN_VAL,
        PR_DEBUT_CALCULE,
        PR_FIN_CALCULE,
        X_DEBUT,
        Y_DEBUT,
        X_FIN,
        Y_FIN,
        ID_SYSTEME_REP,
        ID_BORNEREF_DEBUT,
        AMONT_AVAL_DEBUT,
        DIST_BORNEREF_DEBUT,
        ID_BORNEREF_FIN,
        AMONT_AVAL_FIN,
        DIST_BORNEREF_FIN,
        COMMENTAIRE,
//        REFERENCE_PAPIER,
//        REFERENCE_NUMERIQUE,
//        REFERENCE_CALQUE,
//        DATE_DOCUMENT,
//        NOM,
//        TM_AUTEUR_RAPPORT,
//        ID_MARCHE,
//        ID_INTERV_CREATEUR,
//        ID_ORG_CREATEUR,
//        ID_ARTICLE_JOURNAL,
//        ID_PROFIL_EN_TRAVERS,
//        ID_TYPE_DOCUMENT_A_GRANDE_ECHELLE,
//        ID_CONVENTION,
//        ID_RAPPORT_ETUDE,
//        ID_AUTO
    }

    @Override
    protected List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (Columns c : Columns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }

    @Override
    public String getTableName() {
        return SYS_EVT_PROFIL_EN_LONG.toString();
    }

//    @Override
//    protected void preCompute() throws IOException {
//
//        positions = new HashMap<>();
//        positionsByTronconId = new HashMap<>();
//
//        final Iterator<Row> it = context.inputDb.getTable(getTableName()).iterator();
//        while (it.hasNext()){
//            final Row row = it.next();
//            final ProfilLong documentTroncon = createAnonymValidElement(ProfilLong.class);
//            positions.put(row.getInt(Columns.ID_DOC.toString()), documentTroncon);
//
//            final Integer tronconId = row.getInt(Columns.ID_TRONCON_GESTION.toString());
//            if(positionsByTronconId.get(tronconId)==null)
//                positionsByTronconId.put(tronconId, new ArrayList<>());
//            positionsByTronconId.get(tronconId).add(documentTroncon);
//        }
//    }
//
//    @Override
//    protected void compute() throws IOException, AccessDbImporterException {
//
//        final Iterator<Row> it = context.inputDb.getTable(getTableName()).iterator();
//        while (it.hasNext()){
//            final Row row = it.next();
//            final ProfilLong docTroncon = importRow(row);
//
//            // Don't set the old ID, but save it into the dedicated map in order to keep the reference.
//            positions.put(row.getInt(Columns.ID_DOC.toString()), docTroncon);
//
//            // Set the list ByTronconId
//            List<ProfilLong> listByTronconId = positionsByTronconId.get(row.getInt(Columns.ID_TRONCON_GESTION.toString()));
//            if (listByTronconId == null) {
//                listByTronconId = new ArrayList<>();
//                positionsByTronconId.put(row.getInt(Columns.ID_TRONCON_GESTION.toString()), listByTronconId);
//            }
//            listByTronconId.add(docTroncon);
//
//        }
//        computed=true;
//    }

    @Override
    public  importRow(Row row) throws IOException, AccessDbImporterException {

        final TronconDigue troncon = tronconGestionDigueImporter.getTronconsDigues().get(row.getInt(Columns.ID_TRONCON_GESTION.toString()));
        final Map<Integer, BorneDigue> bornes = borneDigueImporter.getBorneDigue();
        final Map<Integer, SystemeReperage> systemesReperage = systemeReperageImporter.getSystemeRepLineaire();
        final Map<Integer, ProfilLong> profilsLong = profilLongImporter.getRelated();

        final ProfilLong profilLong;
        if(profilsLong.get(row.getInt(Columns.ID_DOC.toString()))!=null){
            profilLong = profilsLong.get(row.getInt(Columns.ID_DOC.toString()));
        } else{
            profilLong = createAnonymValidElement(ProfilLong.class);
        }

        profilLong.setLinearId(troncon.getId());

        if (row.getDouble(Columns.PR_DEBUT_CALCULE.toString()) != null) {
            profilLong.setPrDebut(row.getDouble(Columns.PR_DEBUT_CALCULE.toString()).floatValue());
        }

        if (row.getDouble(Columns.PR_FIN_CALCULE.toString()) != null) {
            profilLong.setPrFin(row.getDouble(Columns.PR_FIN_CALCULE.toString()).floatValue());
        }

        try {
            context.setGeoPositions(row, profilLong);
        } catch (TransformException ex) {
            context.reportError(new ErrorReport(ex, row, getTableName(), null, profilLong, null, "Cannnot set geographic position.", CorruptionLevel.FIELD));
        }

        profilLong.setCommentaire(row.getString(Columns.COMMENTAIRE.toString()));

        /*
         1- La base du Rhône indique que tous les ID_PROFIL_EN_LONG de la table
         DOCUMENT sont absent de SYS_EVT_PROFIL_EN_LONG.
         2- Elle permet également de se rendre compte que tous les
         ID_PROFIL_EN_LONG de la table DOCUMENT sont nuls.
         3- Ainsi que du fait que les ID_PROFIL_EN_LONG de la table
         PROFIL_EN_LONG sont égaux aux ID_DOC des tables DOCUMENT et
         SYS_EVT_PROFIL_EN_LONG

        =========

        Cela provien en fait de ce que les profils en long sont une espèce d'hybride
        entre un document et une position de document associée à un seul document.

        On les considèrera donc comme des positions de documents "étendues" par des attributs de documents "non localisés".

         */
//        if (row.getInt(Columns.ID_DOC.toString()) != null) {
//            if (profilsLong.get(row.getInt(Columns.ID_DOC.toString())) != null) {
//                docTroncon.setSirsdocument(profilsLong.get(row.getInt(Columns.ID_DOC.toString())).getId());
//            }
//        }


        if (row.getInt(Columns.ID_SYSTEME_REP.toString()) != null) {
            profilLong.setSystemeRepId(systemesReperage.get(row.getInt(Columns.ID_SYSTEME_REP.toString())).getId());
        }

        if (row.getDouble(Columns.ID_BORNEREF_DEBUT.toString()) != null) {
            profilLong.setBorneDebutId(bornes.get((int) row.getDouble(Columns.ID_BORNEREF_DEBUT.toString()).doubleValue()).getId());
        }

        profilLong.setBorne_debut_aval(row.getBoolean(Columns.AMONT_AVAL_DEBUT.toString()));

        if (row.getDouble(Columns.DIST_BORNEREF_DEBUT.toString()) != null) {
            profilLong.setBorne_debut_distance(row.getDouble(Columns.DIST_BORNEREF_DEBUT.toString()).floatValue());
        }

        if (row.getDouble(Columns.ID_BORNEREF_FIN.toString()) != null) {
            profilLong.setBorneFinId(bornes.get((int) row.getDouble(Columns.ID_BORNEREF_FIN.toString()).doubleValue()).getId());
        }

        profilLong.setBorne_fin_aval(row.getBoolean(Columns.AMONT_AVAL_FIN.toString()));

        if (row.getDouble(Columns.DIST_BORNEREF_FIN.toString()) != null) {
            profilLong.setBorne_fin_distance(row.getDouble(Columns.DIST_BORNEREF_FIN.toString()).floatValue());
        }

        profilLong.setDesignation(String.valueOf(row.getInt(Columns.ID_DOC.toString())));
        profilLong.setGeometry(buildGeometry(troncon.getGeometry(), profilLong, tronconGestionDigueImporter.getBorneDigueRepository()));

        return profilLong;
    }
}
