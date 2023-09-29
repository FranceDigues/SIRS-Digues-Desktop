function(doc) {
    if(doc['@class']=='fr.sirs.core.model.DesordreDependance' && !doc.date_fin) {
        emit(doc.amenagementHydrauliqueId, doc._id);
    }
}