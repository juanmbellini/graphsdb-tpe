// Dataset file
dataFile = "/Users/jbellini/Dropbox/ITBA/Electivas/NoSQL/TPE/output-5-100.csv";  // Set as required
csvHeader = "userid;venueid;utctimestamp;tpos"

// Initialze graph
configFile = "/Users/jbellini/Dropbox/ITBA/Electivas/NoSQL/TPE/janus/dataset-5-100.properties"; //  Set as required
graph = JanusGraphFactory.open(configFile);

venuesLabelName = 'Venues';
stopLabelName = 'Stop';

useridPropertyName = 'userid';
venueidPropertyName = 'venueid'
utctimestampPropertyName = 'utctimestamp'
tposPropertyName = 'tpos'

isVenueEdgeName = 'isVenue';
trajStepEdgeName = 'trajStep'

import java.text.SimpleDateFormat;
parser = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

prevUserId = null;
prevVertex = null;
new File(dataFile).eachLine{
    l ->
        if (l.equals(csvHeader)) {
            return; // Do not take into account the header
        }
        // Split line according to the column separator
        data = l.split(";");
        // Initialize variables (to avoid accesing by index, which is less readable)
        userid = data[0];
        venueid = data[1];
        utctimestamp = parser.parse(data[2]);
        tpos = data[3];

        // First, we must get the venue
        try {
            // If already initialized, then get it
            venuesVertex = graph.traversal().V().hasLabel(venuesLabelName).has(venueidPropertyName, venueid).next();
        } catch (NoSuchElementException e) {
            // In case there is no Venues vertex with the given id, then stop script
            System.err.println("No venue with id " + venueid);
            throw e;
        }

        // When generating synthetic data, rows are ordered by userid and by tpos
        stopVertex = graph.addVertex(label, stopLabelName);
        stopVertex.property(useridPropertyName, userid);
        stopVertex.property(utctimestampPropertyName, utctimestamp);
        stopVertex.property(tposPropertyName, tpos);
        stopVertex.addEdge(isVenueEdgeName, venuesVertex);

        // We must link vertex with the trajStep 
        if (prevUserId == userid) {
            prevVertex.addEdge(trajStepEdgeName, stopVertex);
        }
        prevUserId = userid;
        prevVertex = stopVertex;
}
graph.tx().commit(); // Commit (because if something fails, we don't have to perform the previous step again)
