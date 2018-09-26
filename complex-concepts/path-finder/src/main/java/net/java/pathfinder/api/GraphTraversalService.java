package net.java.pathfinder.api;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import net.java.pathfinder.internal.GraphDao;

@Path("/graph-traversal")
@RequestScoped
public class GraphTraversalService {

    private static final Logger LOGGER
            = Logger.getLogger(GraphTraversalService.class.getCanonicalName());

    @Inject
    private GraphDao dao;
    private final Random random = new Random();
    private static final long ONE_MIN_MS = 1000 * 60;
    private static final long ONE_DAY_MS = ONE_MIN_MS * 60 * 24;

    @GET
    @Path("/shortest-path")
    @Produces("application/json")
    // TODO Add internationalized messages for constraints.
    public List<TransitPath> findShortestPath(
            @QueryParam("origin") String originUnLocode,
            @QueryParam("destination") String destinationUnLocode,
            @QueryParam("deadline") String deadline) {
        Date date = nextDate(new Date());

        List<String> allVertices = dao.listLocations();
        allVertices.remove(originUnLocode);
        allVertices.remove(destinationUnLocode);

        int candidateCount = getRandomNumberOfCandidates();
        List<TransitPath> candidates = new ArrayList<>(
                candidateCount);

        for (int i = 0; i < candidateCount; i++) {
            allVertices = getRandomChunkOfLocations(allVertices);
            List<TransitEdge> transitEdges = new ArrayList<>(
                    allVertices.size() - 1);
            String firstLegTo = allVertices.get(0);

            Date fromDate = nextDate(date);
            Date toDate = nextDate(fromDate);
            date = nextDate(toDate);

            transitEdges.add(new TransitEdge(
                    dao.getVoyageNumber(originUnLocode, firstLegTo),
                    originUnLocode, firstLegTo, fromDate, toDate));

            for (int j = 0; j < allVertices.size() - 1; j++) {
                String current = allVertices.get(j);
                String next = allVertices.get(j + 1);
                fromDate = nextDate(date);
                toDate = nextDate(fromDate);
                date = nextDate(toDate);
                transitEdges.add(new TransitEdge(dao.getVoyageNumber(current,
                        next), current, next, fromDate, toDate));
            }

            String lastLegFrom = allVertices.get(allVertices.size() - 1);
            fromDate = nextDate(date);
            toDate = nextDate(fromDate);
            transitEdges.add(new TransitEdge(
                    dao.getVoyageNumber(lastLegFrom, destinationUnLocode),
                    lastLegFrom, destinationUnLocode, fromDate, toDate));

            candidates.add(new TransitPath(transitEdges));
        }

        LOGGER.log(Level.INFO, "Path finder service called for {0} to {1}",
                new Object[]{originUnLocode, destinationUnLocode});

        return candidates;
    }

    private Date nextDate(Date date) {
        return new Date(date.getTime() + ONE_DAY_MS
                + (random.nextInt(1000) - 500) * ONE_MIN_MS);
    }

    private int getRandomNumberOfCandidates() {
        return 3 + random.nextInt(3);
    }

    private List<String> getRandomChunkOfLocations(List<String> allLocations) {
        Collections.shuffle(allLocations);
        int total = allLocations.size();
        int chunk = total > 4 ? 1 + new Random().nextInt(5) : total;
        return allLocations.subList(0, chunk);
    }
}
