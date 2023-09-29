function(doc) {
    if(doc['@class']=='fr.sirs.core.model.Desordre' && !doc.date_fin) {
        emit(doc.linearId, doc._id);
    }
}