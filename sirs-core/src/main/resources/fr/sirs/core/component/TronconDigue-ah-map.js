function(doc) {
    if(doc['@class']=='fr.sirs.core.model.TronconDigue') {
        emit(doc.amenagementHydrauliqueId, doc._id)                 
    }
}
