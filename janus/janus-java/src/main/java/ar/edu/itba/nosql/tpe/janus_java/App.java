package ar.edu.itba.nosql.tpe.janus_java;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;

/**
 * Main class.
 */
public class App {

    /**
     * Entry point.
     *
     * @param args Program arguments.
     */
    public static void main(String[] args) {
        final double before = System.currentTimeMillis();
        final String configFile = "/Users/jbellini/Dropbox/ITBA/Electivas/NoSQL/TPE/janus/provided_data.properties";
        final JanusGraph graph = JanusGraphFactory.open(configFile);
        final GraphTraversal<Vertex, Vertex> t = graph.traversal().V();
        int count = 0;
        while (t.hasNext()) {
            count++;
            t.next();
        }
        System.out.println("Vertex amount: " + count);
        graph.close();
        System.out.println("Time elapsed: " + (System.currentTimeMillis() - before) + " ms");
    }
}
