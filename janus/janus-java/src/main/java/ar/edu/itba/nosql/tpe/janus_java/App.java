package ar.edu.itba.nosql.tpe.janus_java;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.*;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

/**
 * Main class.
 */
public class App {

    private final static Logger LOGGER = LoggerFactory.getLogger(App.class);

    private final static String STOP_LABEL_VALUE = "Stop";
    private final static String VENUE_LABEL_VALUE = "Venues";
    private final static String SUBCATEGORY_LABEL_VALUE = "Categories";
    private final static String CATEGORY_LABEL_VALUE = "Category";

    private final static String USER_ID_PROPERTY_KEY = "userid";
    private final static String UTC_TIMESTAMP_PROPERTY_KEY = "utctimestamp";
    private final static String TPOS_PROPERTY_KEY = "tpos";
    private final static String VENUE_ID_PROPERTY_KEY = "venueid";
    private final static String LATITUDE_PROPERTY_KEY = "latitude";
    private final static String LONGITUDE_PROPERTY_KEY = "longitude";
    private final static String VENUE_CATEGORY_PROPERTY_KEY = "venuecategory";
    private final static String CATTYPE_PROPERTY_KEY = "cattype";

    private final static String IS_VENUE_EDGE_LABEL = "isVenue";
    private final static String HAS_CATEGORY_EDGE_LABEL = "hasCategory";
    private final static String SUBCATEGORY_EDGE_LABEL = "subCategoryOf";
    private final static String TRAJ_STEP_EDGE_LABEL = "trajStep";

    /**
     * Entry point.
     *
     * @param args Program arguments.
     */
    public static void main(final String... args) {
        final String configFile = "/Users/jbellini/Projects/graphsdb-tpe/janus/config_files/dataset-1000-100.properties";
        try (final JanusGraph graph = JanusGraphFactory.open(configFile)) {
            // createSchema(graph);
            // loadProvidedData(graph, "./tpgrafo.csv", false);
            // loadSyntheticData(graph, "./output-5-100.csv");

            final GraphTraversal<Vertex, ?> result = query3(graph);
            result.toStream().forEach(System.out::println);
        } catch (Throwable e) {
            LOGGER.error("An exception was thrown", e);
        }
    }


    /**
     * The first query.
     *
     * @param graph The graph to be queried.
     * @return A {@link GraphTraversal} with the results.
     */
    private static GraphTraversal<Vertex, ?> query1(final JanusGraph graph) {
        final String homeStepVariable = "home";
        final String stationStepVariable = "station";
        final String airportStepVariable = "airport";
        return graph.traversal().V()
                .hasLabel(CATEGORY_LABEL_VALUE)
                .has(CATTYPE_PROPERTY_KEY, "Home")
                .in(SUBCATEGORY_EDGE_LABEL)
                .in(HAS_CATEGORY_EDGE_LABEL)
                .in(IS_VENUE_EDGE_LABEL)
                .as(homeStepVariable)
                .out(TRAJ_STEP_EDGE_LABEL)
                .filter(__
                        .out(IS_VENUE_EDGE_LABEL)
                        .out(HAS_CATEGORY_EDGE_LABEL)
                        .out(SUBCATEGORY_EDGE_LABEL)
                        .has(CATTYPE_PROPERTY_KEY, "Station"))
                .out(TRAJ_STEP_EDGE_LABEL)
                .filter(__
                        .out(IS_VENUE_EDGE_LABEL)
                        .out(HAS_CATEGORY_EDGE_LABEL)
                        .out(SUBCATEGORY_EDGE_LABEL)
                        .has(CATTYPE_PROPERTY_KEY, "Airport"))
                .select(homeStepVariable)
                .project(USER_ID_PROPERTY_KEY, homeStepVariable, stationStepVariable, airportStepVariable)
                .by(USER_ID_PROPERTY_KEY)
                .by(TPOS_PROPERTY_KEY)
                .by(__.out(TRAJ_STEP_EDGE_LABEL).values(TPOS_PROPERTY_KEY))
                .by(__.out(TRAJ_STEP_EDGE_LABEL).out(TRAJ_STEP_EDGE_LABEL).values(TPOS_PROPERTY_KEY));
    }

    private static GraphTraversal<Vertex, ?> query2(final JanusGraph graph) {
        final String homeStepVariable = "home";
        final String airportStepVariable = "airport";
        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");


        //noinspection unchecked
        return graph.traversal().V()
                .hasLabel(CATEGORY_LABEL_VALUE)
                .has(CATTYPE_PROPERTY_KEY, "Airport")
                .in(SUBCATEGORY_EDGE_LABEL)
                .in(HAS_CATEGORY_EDGE_LABEL)
                .in(IS_VENUE_EDGE_LABEL)
                .as(airportStepVariable)

                .V()
                .hasLabel(CATEGORY_LABEL_VALUE)
                .has(CATTYPE_PROPERTY_KEY, "Home")
                .in(SUBCATEGORY_EDGE_LABEL)
                .in(HAS_CATEGORY_EDGE_LABEL)
                .in(IS_VENUE_EDGE_LABEL)
                .as(homeStepVariable)

                .filter(__.select(homeStepVariable, airportStepVariable)
                        .where(homeStepVariable, P.test((o1, o2) -> {
                            final Vertex v1 = (Vertex) o1;
                            final Vertex v2 = (Vertex) o2;

                            final Long id1 = v1.value(USER_ID_PROPERTY_KEY);
                            final Long id2 = v2.value(USER_ID_PROPERTY_KEY);
                            final Date d1 = v1.value(UTC_TIMESTAMP_PROPERTY_KEY);
                            final Date d2 = v2.value(UTC_TIMESTAMP_PROPERTY_KEY);
                            final Integer tpos1 = v1.value(TPOS_PROPERTY_KEY);
                            final Integer tpos2 = v2.value(TPOS_PROPERTY_KEY);

                            final boolean userIds = id1.equals(id2);
                            final boolean dates = dateFormat.format(d1).equals(dateFormat.format(d2));
                            final boolean tpos = tpos1 < tpos2;

                            return userIds && dates && tpos;
                        }, airportStepVariable))
                )
                .valueMap();
    }

    private static GraphTraversal<Vertex, ?> query3(final JanusGraph graph) {
        final String src = "src";
        final String dst = "dst";
        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        //noinspection unchecked
        return graph.traversal().V()
                .hasLabel(STOP_LABEL_VALUE)
                .as(src)
                .V()
                .hasLabel(STOP_LABEL_VALUE)
                .as(dst)
                .filter(__.select(src, dst)
                        .where(src, P.test((o1, o2) -> {
                            final Vertex v1 = (Vertex) o1;
                            final Vertex v2 = (Vertex) o2;
                            final Long id1 = (Long) v1.values(USER_ID_PROPERTY_KEY).next();
                            final Long id2 = (Long) v2.values(USER_ID_PROPERTY_KEY).next();
                            final Date d1 = (Date) v1.values(UTC_TIMESTAMP_PROPERTY_KEY).next();
                            final Date d2 = (Date) v2.values(UTC_TIMESTAMP_PROPERTY_KEY).next();
                            final Integer tpos1 = (Integer) v1.values(TPOS_PROPERTY_KEY).next();
                            final Integer tpos2 = (Integer) v2.values(TPOS_PROPERTY_KEY).next();

                            final Vertex venueVertex1 = v1.edges(Direction.OUT, IS_VENUE_EDGE_LABEL).next().inVertex();
                            final Vertex venueVertex2 = v2.edges(Direction.OUT, IS_VENUE_EDGE_LABEL).next().inVertex();

                            final String venueId1 = (String) venueVertex1.values(VENUE_ID_PROPERTY_KEY).next();
                            final String venueId2 = (String) venueVertex2.values(VENUE_ID_PROPERTY_KEY).next();

                            final boolean userIds = id1.equals(id2);
                            final boolean dates = dateFormat.format(d1).equals(dateFormat.format(d2));
                            final boolean tpos = tpos1 < tpos2;
                            final boolean venues = venueId1.equals(venueId2);


                            return userIds && dates && tpos && venues;
                        }, dst))
                )
                .valueMap();
    }

    private static GraphTraversal<Vertex, ?> query4(final JanusGraph graph) {
//        final String src = "src";
//        final String dst = "dst";
//        return graph.traversal().V()
//                .hasLabel(STOP_LABEL_VALUE)
//                .as(src)
//                .V()
//                .hasLabel(STOP_LABEL_VALUE)
//                .as(dst);
        return graph.traversal().V();
    }

    /**
     * Builds a match {@link org.apache.tinkerpop.gremlin.process.traversal.Traversal} for stops with in a venue
     * with the given {@code category}, marking the step with the given {@code label}.
     *
     * @param label    The label to be given to the step.
     * @param category The category of the venue.
     * @return The created match traversal.
     */
    private static GraphTraversal<?, Vertex> buildMatchForCategory(final String label, final String category) {
        return __.as(label)
                .out(IS_VENUE_EDGE_LABEL)
                .out(HAS_CATEGORY_EDGE_LABEL)
                .out(SUBCATEGORY_EDGE_LABEL)
                .has(CATTYPE_PROPERTY_KEY, category);
    }

    private static GraphTraversal<?, Vertex> buildCategoryPredicate(final String category) {
        return __
                .out(IS_VENUE_EDGE_LABEL)
                .out(HAS_CATEGORY_EDGE_LABEL)
                .out(SUBCATEGORY_EDGE_LABEL)
                .has(CATTYPE_PROPERTY_KEY, category);
    }


    /**
     * Creates the schema in the given {@code graph}.
     *
     * @param graph The graph in which the schema will be built.
     */
    private static void createSchema(final JanusGraph graph) {
        // Create transaction
        graph.tx().rollback(); // Never create new indexes while a transaction is active
        final JanusGraphManagement management = graph.openManagement();

        // Stops
        final VertexLabel stopLabel = management
                .makeVertexLabel(STOP_LABEL_VALUE)
                .make();
        final PropertyKey userIdPropertyKey = management
                .makePropertyKey(USER_ID_PROPERTY_KEY)
                .dataType(Long.class)
                .cardinality(Cardinality.SINGLE)
                .make();
        management
                .makePropertyKey(UTC_TIMESTAMP_PROPERTY_KEY)
                .dataType(Date.class)
                .cardinality(Cardinality.SINGLE)
                .make();
        final PropertyKey tposPorPropertyKey = management
                .makePropertyKey(TPOS_PROPERTY_KEY)
                .dataType(Integer.class)
                .cardinality(Cardinality.SINGLE)
                .make();
        management.buildIndex("stopUseridTposIndex", Vertex.class)
                .addKey(userIdPropertyKey)
                .addKey(tposPorPropertyKey)
                .indexOnly(stopLabel)
                .buildCompositeIndex();

        // Venues
        final VertexLabel venuesLabel = management
                .makeVertexLabel(VENUE_LABEL_VALUE)
                .make();
        final PropertyKey venueidPropertyKey = management
                .makePropertyKey(VENUE_ID_PROPERTY_KEY)
                .dataType(String.class)
                .cardinality(Cardinality.SINGLE)
                .make();
        management
                .makePropertyKey(LATITUDE_PROPERTY_KEY)
                .dataType(Double.class)
                .cardinality(Cardinality.SINGLE)
                .make();
        management
                .makePropertyKey(LONGITUDE_PROPERTY_KEY)
                .dataType(Double.class)
                .cardinality(Cardinality.SINGLE)
                .make();
        management
                .buildIndex("byVenueIdComposite", Vertex.class)
                .addKey(venueidPropertyKey)
                .indexOnly(venuesLabel)
                .buildCompositeIndex();

        // Categories
        final VertexLabel categoriesLabel = management
                .makeVertexLabel(SUBCATEGORY_LABEL_VALUE)
                .make();
        final PropertyKey venuecategoryPropertyKey = management
                .makePropertyKey(VENUE_CATEGORY_PROPERTY_KEY)
                .dataType(String.class)
                .cardinality(Cardinality.SINGLE)
                .make();
        management.buildIndex("byVenuecategoryComposite", Vertex.class)
                .addKey(venuecategoryPropertyKey)
                .indexOnly(categoriesLabel)
                .buildCompositeIndex();

        // Category
        final VertexLabel categoryLabel = management
                .makeVertexLabel(CATEGORY_LABEL_VALUE)
                .make();
        final PropertyKey cattypePropertyKey = management
                .makePropertyKey(CATTYPE_PROPERTY_KEY)
                .dataType(String.class)
                .cardinality(Cardinality.SINGLE)
                .make();
        management.buildIndex("byCattypeComposite", Vertex.class)
                .addKey(cattypePropertyKey)
                .indexOnly(categoryLabel)
                .buildCompositeIndex();

        // Edges
        management.makeEdgeLabel(IS_VENUE_EDGE_LABEL).multiplicity(Multiplicity.MANY2ONE).make();
        management.makeEdgeLabel(HAS_CATEGORY_EDGE_LABEL).multiplicity(Multiplicity.MANY2ONE).make();
        management.makeEdgeLabel(SUBCATEGORY_EDGE_LABEL).multiplicity(Multiplicity.MANY2ONE).make();
        management.makeEdgeLabel(TRAJ_STEP_EDGE_LABEL).multiplicity(Multiplicity.ONE2ONE).make();

        // Commit
        management.commit();
    }

    /**
     * Loads the provided data (this includes venues, subcategories and categories).
     *
     * @param graph        The graph to which data must be added.
     * @param dataFilePath The path to the file with the data.
     * @param loadStops    Flag indicating whether stops should be loaded also.
     */
    private static void loadProvidedData(final JanusGraph graph,
                                         final String dataFilePath, final boolean loadStops) {
        final String csvHeader = "userid|latitude|longitude|utctimestamp|venueid|venuecategory|cattype";
        final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Files.lines(Paths.get(dataFilePath))
                    .filter(line -> !line.equals(csvHeader))
                    .map(line -> line.split("\\|"))
                    .forEach(data -> {
                        try {
                            final long userid = Long.parseLong(data[0]);
                            final double latitude = Double.parseDouble(data[1]);
                            final double longitude = Double.parseDouble(data[2]);
                            final Date utctimestamp = dateParser.parse(data[3]);
                            final String venueid = data[4];
                            final String venuecategory = data[5];
                            final String cattype = data[6];

                            // First, get the category vertex (create it if not exists)
                            final Vertex categoryVertex = Optional
                                    .of(graph.traversal().V()
                                            .hasLabel(CATEGORY_LABEL_VALUE)
                                            .has(CATTYPE_PROPERTY_KEY, cattype))
                                    .filter(Iterator::hasNext)
                                    .map(Iterator::next)
                                    .orElseGet(() -> {
                                        final Vertex vertex = graph.addVertex(T.label, CATEGORY_LABEL_VALUE);
                                        vertex.property(CATTYPE_PROPERTY_KEY, cattype);
                                        return vertex;
                                    });

                            // Then, get the categories vertex (create it if not exists)
                            final Vertex categoriesVertex = Optional
                                    .of(graph.traversal().V()
                                            .hasLabel(SUBCATEGORY_LABEL_VALUE)
                                            .has(VENUE_CATEGORY_PROPERTY_KEY, venuecategory))
                                    .filter(Iterator::hasNext)
                                    .map(Iterator::next)
                                    .orElseGet(() -> {
                                        final Vertex vertex = graph.addVertex(T.label, SUBCATEGORY_LABEL_VALUE);
                                        vertex.property(VENUE_CATEGORY_PROPERTY_KEY, venuecategory);
                                        vertex.addEdge(SUBCATEGORY_EDGE_LABEL, categoryVertex);
                                        return vertex;
                                    });

                            // Then, add the venue (create it if not exists)
                            final Vertex venuesVertex = Optional
                                    .of(graph.traversal().V()
                                            .hasLabel(VENUE_LABEL_VALUE)
                                            .has(VENUE_ID_PROPERTY_KEY, venueid))
                                    .filter(Iterator::hasNext)
                                    .map(Iterator::next)
                                    .orElseGet(() -> {
                                        final Vertex vertex = graph.addVertex(T.label, VENUE_LABEL_VALUE);
                                        vertex.property(VENUE_ID_PROPERTY_KEY, venueid);
                                        vertex.property(LATITUDE_PROPERTY_KEY, latitude);
                                        vertex.property(LONGITUDE_PROPERTY_KEY, longitude);
                                        vertex.addEdge(HAS_CATEGORY_EDGE_LABEL, categoriesVertex);
                                        return vertex;
                                    });

                            if (loadStops) {
                                // When the flag is enabled, stops should be added from the provided csv
                                // We assume that no row is repeated, so we don't check if already exists
                                // We also assume that the CSV is not ordered
                                final Vertex stopVertex = graph.addVertex(T.label, STOP_LABEL_VALUE);
                                stopVertex.property(USER_ID_PROPERTY_KEY, userid);
                                stopVertex.property(UTC_TIMESTAMP_PROPERTY_KEY, utctimestamp);
                                stopVertex.addEdge(IS_VENUE_EDGE_LABEL, venuesVertex);
                            }

                        } catch (final Throwable e) {
                            throw new RuntimeException("Could not parse file", e);
                        }
                    });
            graph.tx().commit(); // Commit (because if something fails, we don't have to perform the previous step again) TODO: mark operation complete somewhere?


            // Up to now we have all data loaded (also stops if flag is enabled)
            // Now, if the flag is enabled, we have to relate stops with the "trajStep" relation
            // Also (if flag is enabled), we have to add the tpos values
            // So, first, we need to get all the stop vertices for a given user
            if (loadStops) {
                final Map<?, ?> map = graph.traversal().V()
                        .hasLabel(STOP_LABEL_VALUE)
                        .order().by(UTC_TIMESTAMP_PROPERTY_KEY)
                        .group().by(USER_ID_PROPERTY_KEY)
                        .next(); // Map<Long, ArrayList<Vertex>>

                //noinspection unchecked,unchecked
                for (List<Vertex> list : (Collection<List<Vertex>>) map.values()) {
                    int tpos = 0;
                    if (!list.isEmpty()) {
                        Vertex prev = list.remove(0);
                        for (Vertex actual : list) {
                            prev.property(TPOS_PROPERTY_KEY, tpos);
                            prev.addEdge(TRAJ_STEP_EDGE_LABEL, actual);
                            prev = actual;
                            tpos++;
                        }
                    }
                }
                graph.tx().commit();
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("IOException", e);
        }
    }

    /**
     * Loads synthetic data into the given {@code graph}.
     *
     * @param graph        The graph to which data must be added.
     * @param dataFilePath The path to the file with the data.
     */
    private static void loadSyntheticData(final JanusGraph graph, final String dataFilePath) {
        final String csvHeader = "userid;venueid;utctimestamp;tpos";
        final SimpleDateFormat dateParser = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        final Map<Long, List<Vertex>> vertices = new HashMap<>();
        try {
            Files.lines(Paths.get(dataFilePath))
                    .filter(line -> !line.equals(csvHeader))
                    .map(line -> line.split(";"))
                    .forEach(data -> {
                        try {
                            // Initialize variables (to avoid accessing by index, which is less readable)
                            final Long userId = Long.parseLong(data[0]);
                            final String venueid = data[1];
                            final Date utctimestamp = dateParser.parse(data[2]);
                            final int tpos = Integer.parseInt(data[3]);

                            // Then, add the venue (create it if not exists)
                            final Vertex venuesVertex = Optional
                                    .of(graph.traversal().V()
                                            .hasLabel(VENUE_LABEL_VALUE)
                                            .has(VENUE_ID_PROPERTY_KEY, venueid))
                                    .filter(Iterator::hasNext)
                                    .map(Iterator::next)
                                    .orElseThrow(() -> new NoSuchElementException("No Venue with the given id"));

                            // When generating synthetic data, rows are ordered by userid and by tpos
                            final Vertex stopVertex = graph.addVertex(T.label, STOP_LABEL_VALUE);

                            stopVertex.property(USER_ID_PROPERTY_KEY, userId);
                            stopVertex.property(UTC_TIMESTAMP_PROPERTY_KEY, utctimestamp);
                            stopVertex.property(TPOS_PROPERTY_KEY, tpos);
                            stopVertex.addEdge(IS_VENUE_EDGE_LABEL, venuesVertex);

                            final List<Vertex> list = vertices.getOrDefault(userId, new LinkedList<>());
                            list.add(stopVertex);
                            vertices.put(userId, list);
                        } catch (final Throwable e) {
                            throw new RuntimeException("Could not parse file", e);
                        }
                    });

            vertices.values()
                    .forEach(list -> {
                        Vertex prevVertex = list.remove(0);
                        while (!list.isEmpty()) {
                            final Vertex vertex = list.remove(0);
                            prevVertex.addEdge(TRAJ_STEP_EDGE_LABEL, prevVertex);
                            prevVertex = vertex;
                        }
                    });
            graph.tx().commit();
        } catch (final IOException e) {
            throw new UncheckedIOException("IOException", e);
        }
    }
}
