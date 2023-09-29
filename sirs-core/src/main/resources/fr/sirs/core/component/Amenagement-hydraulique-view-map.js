function(doc) {
    if (doc['@class']=='fr.sirs.core.model.AmenagementHydraulique') {
        emit(doc._id, {id: doc._id, type: doc.typeId, superficie: doc.superficie, capaciteStockage: doc.capaciteStockage, profondeurMoyenne: doc.profondeurMoyenne, designation: doc.designation});
    }
}
