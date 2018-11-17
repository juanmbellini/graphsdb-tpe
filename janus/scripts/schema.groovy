configFile = "/Users/jbellini/Dropbox/ITBA/Electivas/NoSQL/TPE/janus/provided_data.properties"; // Set as required
graph = JanusGraphFactory.open(configFile);
graph.tx().rollback() //Never create new indexes while a transaction is active
mgmt = graph.openManagement();

stopLabel = mgmt.makeVertexLabel('Stop').make();
userIdProperty = mgmt.makePropertyKey('userid').dataType(Long.class).cardinality(Cardinality.SINGLE).make();
utctimestampProperty =  mgmt.makePropertyKey('utctimestamp').dataType(Date.class).cardinality(Cardinality.SINGLE).make();
mgmt.makePropertyKey('tpos').dataType(Integer.class).cardinality(Cardinality.SINGLE).make();
// mgmt.buildIndex('stopComposite', Vertex.class).addKey(userIdProperty).addKey(utctimestampProperty).indexOnly(stopLabel).buildCompositeIndex();

venuesLabel = mgmt.makeVertexLabel('Venues').make();
venueidProperty = mgmt.makePropertyKey('venueid').dataType(String.class).cardinality(Cardinality.SINGLE).make();
mgmt.makePropertyKey('latitude').dataType(Double.class).cardinality(Cardinality.SINGLE).make();
mgmt.makePropertyKey('longitude').dataType(Double.class).cardinality(Cardinality.SINGLE).make();
mgmt.buildIndex('byVenueIdComposite', Vertex.class).addKey(venueidProperty).indexOnly(venuesLabel).buildCompositeIndex();

categoriesLabel = mgmt.makeVertexLabel('Categories').make();
venuecategoryProperty = mgmt.makePropertyKey('venuecategory').dataType(String.class).cardinality(Cardinality.SINGLE).make();
mgmt.buildIndex('byVenuecategoryComposite', Vertex.class).addKey(venuecategoryProperty).indexOnly(categoriesLabel).buildCompositeIndex();

categoryLabel = mgmt.makeVertexLabel('Category').make();
cattypeProperty = mgmt.makePropertyKey('cattype').dataType(String.class).cardinality(Cardinality.SINGLE).make();
mgmt.buildIndex('byCattypeComposite', Vertex.class).addKey(cattypeProperty).indexOnly(categoryLabel).buildCompositeIndex();


mgmt.makeEdgeLabel('isVenue').multiplicity(MANY2ONE).make();
mgmt.makeEdgeLabel('hasCategory').multiplicity(MANY2ONE).make();
mgmt.makeEdgeLabel('subCategoryOf').multiplicity(MANY2ONE).make();
mgmt.makeEdgeLabel('trajStep').multiplicity(ONE2ONE).make();

mgmt.commit();



// graph.tx().rollback() //Never create new indexes while a transaction is active
// mgmt = graph.openManagement()
// name = mgmt.getPropertyKey('name')
// age = mgmt.getPropertyKey('age')
// mgmt.buildIndex('byNameComposite', Vertex.class).addKey(name).buildCompositeIndex()
// mgmt.buildIndex('byNameAndAgeComposite', Vertex.class).addKey(name).addKey(age).buildCompositeIndex()
// mgmt.commit()
// //Wait for the index to become available
// ManagementSystem.awaitGraphIndexStatus(graph, 'byNameComposite').call()
// ManagementSystem.awaitGraphIndexStatus(graph, 'byNameAndAgeComposite').call()
// //Reindex the existing data
// mgmt = graph.openManagement()
// mgmt.updateIndex(mgmt.getGraphIndex("byNameComposite"), SchemaAction.REINDEX).get()
// mgmt.updateIndex(mgmt.getGraphIndex("byNameAndAgeComposite"), SchemaAction.REINDEX).get()
// mgmt.commit()