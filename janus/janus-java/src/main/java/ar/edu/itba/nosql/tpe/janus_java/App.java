package ar.edu.itba.nosql.tpe.janus_java;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class.
 */
public class App {

    private final static Logger LOGGER = LoggerFactory.getLogger(App.class);

    /**
     * Entry point.
     *
     * @param args Program arguments.
     */
    public static void main(final String... args) {
        final String configFile = "/Users/jbellini/Dropbox/ITBA/Electivas/NoSQL/TPE/janus/dataset-5-100.properties";
        try (final JanusGraph graph = JanusGraphFactory.open(configFile)) {

            final GraphTraversal<Vertex, ?> result = graph.traversal().V()
                    .match(
                            buildMatchForCategory("home", "Home"),
                            buildMatchForCategory("station", "Station"),
                            buildMatchForCategory("airport", "Airport"),
                            __.as("home")
                                    .out("trajStep")
                                    .as("station")
                                    .out("trajStep")
                                    .as("airport")

                    )
                    .select("home", "station", "airport")
                    .by(__.out("isVenue").out("hasCategory").out("subCategoryOf").properties("cattype"));


            while (result.hasNext()) {
                System.out.println(result.next());
            }

        } catch (Throwable e) {
            LOGGER.error("An exception was thrown", e);
        }
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
        return __.as(label).hasLabel("Stop")
                .out("isVenue")
                .out("hasCategory")
                .out("subCategoryOf")
                .has("cattype", category);
    }
}
