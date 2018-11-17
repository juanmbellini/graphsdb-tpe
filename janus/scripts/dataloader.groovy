loadStops = false; // Indicates whether stops should be loaded or just venues with categories and category. Set as required
// Dataset file
dataFile = "/Users/jbellini/Dropbox/ITBA/Electivas/NoSQL/TPE/tpgrafo.csv";  // Set as required
csvHeader = "userid|latitude|longitude|utctimestamp|venueid|venuecategory|cattype";
// Initialze graph
configFile = "/Users/jbellini/Dropbox/ITBA/Electivas/NoSQL/TPE/janus/dataset-5-100.properties"; //  Set as required
graph = JanusGraphFactory.open(configFile);

// Schema stuff
categoryLabelName = 'Category';
categoriesLabelName = 'Categories';
venuesLabelName = 'Venues';
stopLabelName = 'Stop';

useridPropertyName = 'userid';
utctimestampPropertyName = 'utctimestamp';
tposPropertyName = 'tpos';
venueidPropertyName = 'venueid';
latitudePropertyName = 'latitude';
longitudePropertyName = 'longitude';
venuecategoryPropertyName = 'venuecategory';
cattypePropertyName = 'cattype';

isVenueEdgeName = 'isVenue';
hasCategoryEdgeName = 'hasCategory';
subCategoryOfEdgeName = 'subCategoryOf';
trajStepEdgeName = 'trajStep';

import java.text.SimpleDateFormat;
parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


new File(dataFile).eachLine{
    l ->
        if (l.equals(csvHeader)) {
            return; // Do not take into account the header
        }
        // Split line according to the column separator
        data = l.split("\\|");
        // Initialize variables (to avoid accesing by index, which is less readable)
        userid = data[0];
        latitude = data[1];
        longitude = data[2];
        utctimestamp = parser.parse(data[3]);
        venueid = data[4];
        venuecategory = data[5];
        cattype = data[6];

        // First, get the category vertex (create it if not exists)
        try {
            // If already initialized, then get it
            categoryVertex = graph.traversal().V().hasLabel(categoryLabelName).has(cattypePropertyName, cattype).next();
        } catch (NoSuchElementException e) {
            // In case there is no Category vertex with the given type, then add it
            categoryVertex = graph.addVertex(label, categoryLabelName);
            categoryVertex.property(cattypePropertyName, cattype);
        }

        // Then, get the categories vertex (create it if not exists)
        try {
            // If already initialized, then get it
            categoriesVertex = graph.traversal().V().hasLabel(categoriesLabelName).has(venuecategoryPropertyName, venuecategory).next();
        } catch (NoSuchElementException e) {
            // In case there is no Categories vertex with the given type, then add it
            categoriesVertex = graph.addVertex(label, categoriesLabelName);
            categoriesVertex.property(venuecategoryPropertyName, venuecategory);
            categoriesVertex.addEdge(subCategoryOfEdgeName, categoryVertex);
        }

        // Then, add the venue (create it if not exists)
        try {
            // If already initialized, then get it
            venuesVertex = graph.traversal().V().hasLabel(venuesLabelName).has(venueidPropertyName, venueid).next();
        } catch (NoSuchElementException e) {
            // In case there is no Venues vertex with the given id, then add it
            venuesVertex = graph.addVertex(label, venuesLabelName)
            venuesVertex.property(venueidPropertyName, venueid);
            venuesVertex.property(latitudePropertyName, latitude);
            venuesVertex.property(longitudePropertyName, longitude);
            venuesVertex.addEdge(hasCategoryEdgeName, categoriesVertex);
        }

        if (loadStops) {
            // When the flag is enabled, stops should be added from the provided csv
            // We assume that no row is repeated, so we don't check if already exists
            // We also assume that the CSV is not ordered
            stopVertex = graph.addVertex(label, stopLabelName);
            stopVertex.property(useridPropertyName, userid);
            stopVertex.property(utctimestampPropertyName, utctimestamp);
            stopVertex.addEdge(isVenueEdgeName, venuesVertex);
        }
}
graph.tx().commit(); // Commit (because if something fails, we don't have to perform the previous step again)

// Up to now we have all data loaded (also stops if flag is enabled)
// Now, if the flag is enabled, we have to relate stops with the "trajStep" relation
// Also (if flag is enabled), we have to add the tpos values
// So, first, we need to get all the stop vertices for a given user
if (loadStops) {
    import org.janusgraph.graphdb.vertices.CacheVertex;
    map = graph.traversal().V().hasLabel(stopLabelName).order().by(utctimestampPropertyName).group().by(useridPropertyName).next(); // Map<Long, ArrayList<Vertex>>
    for (List<CacheVertex> list : map.values()) {
        tpos = 0;
        if (!list.isEmpty()) {
            prev = list.remove(0);
            for (CacheVertex actual : list) {
                prev.property(tposPropertyName, tpos);
                prev.addEdge(trajStepEdgeName, actual);
                prev = actual;
                tpos++;
            }
        }
    }    
    graph.tx().commit();
}

