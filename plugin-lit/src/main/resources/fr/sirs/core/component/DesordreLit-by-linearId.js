function(doc) {
    if(doc['@class']=='fr.sirs.core.model.DesordreLit' && !doc.date_fin) {
        emit(doc.linearId, doc._id);
    }
}