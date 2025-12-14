package com.tramchester.integration.testSupport;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphPropertyKey.*;
import static com.tramchester.graph.core.GraphDirection.Outgoing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class GraphComparisons {
    private static final Logger logger = LoggerFactory.getLogger(GraphComparisons.class);
    private final GraphTransaction txnNeo4J;
    private final GraphTransaction txnInMem;

    public GraphComparisons(GraphTransaction neo4JTxn, GraphTransaction txnInMem) {
        this.txnNeo4J = neo4JTxn;
        this.txnInMem = txnInMem;
    }

    public void visitMatchedNodes(final GraphNode neo4JNode, final GraphNode inMemoryNode, final int depth) {

        final EnumSet<TransportRelationshipTypes> allTypes = EnumSet.allOf(TransportRelationshipTypes.class);

        if (depth==0) {
            return;
        }

        assertEquals(neo4JNode.getLabels(), inMemoryNode.getLabels());
        checkProps(neo4JNode, inMemoryNode);

        final Stream<GraphRelationship> allOutgoingNeo4J = neo4JNode.getRelationships(txnNeo4J, Outgoing, allTypes);

        allOutgoingNeo4J.forEach(outgoingNeo4J -> {
            final GraphNode destinationNode = outgoingNeo4J.getEndNode(txnNeo4J);

            final TransportRelationshipTypes type = outgoingNeo4J.getType();
            final Map<String, Object> relationshipProps = outgoingNeo4J.getAllProperties();

            // Note: have to match relationships via end node labels and props
            final Map<String, Object> endNodeProps = destinationNode.getAllProperties();
            final EnumSet<GraphLabel> endNodeLabels = destinationNode.getLabels();

            // types
            final List<GraphRelationship> matchType = inMemoryNode.getRelationships(txnInMem, Outgoing, type).toList();
            assertFalse(matchType.isEmpty(), "Failed to match relationship type " + type + " within " +
                            inMemoryNode.getRelationships(txnInMem, Outgoing, allTypes).toList() + " at "
                    + inMemoryNode + "  " + inMemoryNode.getAllProperties() + " neo4J node props " + neo4JNode.getAllProperties());

            // relationship props
            final List<GraphRelationship> matchProps = matchType.stream().
                    filter(relat -> compareProps(relationshipProps, relat.getAllProperties(), false)).toList();
            assertFalse(matchProps.isEmpty(), "mismatch on relationship props " + relationshipProps + " vs " + matchType);

            // end node labels
            final List<GraphRelationship> matchNodeLabels = matchProps.stream().
                    filter(relat -> relat.getEndNode(txnInMem).getLabels().equals(endNodeLabels)).
                    toList();
            assertFalse(matchNodeLabels.isEmpty());

            // end node props
            final List<GraphRelationship> matchNodeProps = matchNodeLabels.stream().
                    filter(relat -> compareProps(endNodeProps, relat.getEndNode(txnInMem).getAllProperties(), false)).
                    toList();
            assertFalse(matchNodeProps.isEmpty());

            assertEquals(1, matchNodeProps.size(), outgoingNeo4J + " is not matched by " + matchNodeProps);

            final GraphRelationship matchingRelationship = matchNodeProps.getFirst();

            visitMatchedNodes(destinationNode, matchingRelationship.getEndNode(txnInMem), depth-1);
        });

    }

    // TODO Push comparisons into test help and add logging
    private boolean compareProps(Map<String, Object> propsA, Map<String, Object> propsB, boolean logging) {
        final String diag = propsA + " vs " + propsB;
        if (propsA.size()!=propsB.size()) {
            logger.error("mismatch on size " + diag);
            return false;
        }
        final Set<String> keysA = propsA.keySet();
        final Set<String> keysB = propsB.keySet();
        if (!keysA.equals(keysB)) {
            if (logging) {
                logger.error("mismatch on key set " + keysA + " vs " + keysB);
            }
            return false;
        }

        final Map<String, Object> mismatch = new HashMap<>();
        for(final String key : keysA) {
            final Object valueA = propsA.get(key);
            final Object valueB = propsB.get(key);

            final String diagOnProp = " for key: %s A:%s vs B:%s".formatted(key, valueA, valueB);

            final boolean matched;
            if (key.equals(COST.getText())) {
                long seconds = (long) valueA;
                matched = Duration.ofSeconds(seconds).equals(valueB);
                if ((!matched) && logging) {
                    logger.error("mismatch on duration " + diagOnProp);
                }
            } else if (key.equals(TRANSPORT_MODES.getText())) {
                final short[] arrayA = (short[]) valueA;
                final EnumSet<TransportMode> itemsB = (EnumSet<TransportMode>) valueB;
                if (arrayA.length==itemsB.size()) {
                    final List<Short> numsB = itemsB.stream().map(TransportMode::getNumber).toList();
                    boolean flag = true;
                    for (int i = 0; i < arrayA.length; i++) {
                        flag = flag && numsB.contains(arrayA[i]);
                    }
                    matched = flag;
                } else {
                    matched = false;
                }
                if ((!matched) && logging) {
                    logger.error("mismatch on modes " + diagOnProp);
                }
            } else if (key.equals(TRANSPORT_MODE.getText())) {
                final TransportMode expectedMode = TransportMode.fromNumber((Short) valueA);
                matched = expectedMode.equals(valueB);
                if ((!matched) && logging) {
                    logger.error("mismatch on mode " + diagOnProp);
                }
            } else if (key.equals(TRIP_ID_LIST.getText())) {
                final String[] arrayA = (String[]) valueA;
                final IdSet<Trip> tripsB = (IdSet<Trip>) valueB;
                if (arrayA.length == tripsB.size()) {
                    final List<String> stringsA = Arrays.asList(arrayA);
                    final IdSet<Trip> tripsA = stringsA.stream().map(Trip::createId).collect(IdSet.idCollector());
                    matched = tripsA.equals(tripsB);
                } else {
                    logger.error("Length mismatch between " + arrayA.length + " and " + tripsB + " for " + diag);
                    matched = false;
                }
                if ((!matched) && logging) {
                    logger.error("mismatch on trip ids " + diagOnProp);
                }
            } else if (key.equals(TIME.getText())) {
                final String txtA = valueA.toString();
                TramTime timeA = TramTime.parse(txtA);
                final boolean dayOffset = (Boolean) propsA.getOrDefault(DAY_OFFSET.getText(), false);
                if (dayOffset) {
                    timeA = TramTime.nextDay(timeA);
                }
                matched = timeA.equals(valueB);
                if ((!matched) && logging) {
                    logger.error("mismatch on time " + diagOnProp);
                }
            }
            else {
                matched = valueA.equals(valueB);
                if ((!matched) && logging) {
                    logger.error("mismatch on objects " + diagOnProp);
                }
            }
            if (!matched) {
                mismatch.put(key, valueB);
            }
        }
        if (logging) {
            if (!mismatch.isEmpty()) {
                logger.error("mismatch on values " + mismatch + " for: " + diag);
            } else {
                logger.info("matched " + diag);
            }
        }

        return mismatch.isEmpty();
    }

//    private boolean compareArrays(short[] arrayA, short[] arrayB) {
//        if (arrayA.length!=arrayB.length) {
//            logger.error("length mismatch " + arrayA + " vs " + arrayB);
//        }
//        boolean matched = true;
//        for (int i = 0; i < arrayA.length; i++) {
//            matched = matched && arrayA[i]==arrayB[i];
//        }
//        if (!matched) {
//            logger.error("mismatch " + arrayA + " vs " + arrayB);
//        }
//        return matched;
//    }


    public void checkProps(final GraphEntity graphEntityA, final GraphEntity graphEntityB) {
        final Map<String, Object> propsA = graphEntityA.getAllProperties();
        final Map<String, Object> propsB = graphEntityB.getAllProperties();

        matchProps(propsA, propsB);

//        assertEquals(propsA.size(), propsB.size());
//
//        for(final String key : propsA.keySet()) {
//            assertTrue(propsB.containsKey(key));
//            if (key.equals(TRANSPORT_MODES.getText())) {
//                final short[] arrayA = (short[]) propsA.get(key);
//                final short[] arrayB = (short[]) propsB.get(key);
//                assertArrayEquals(arrayA, arrayB, "mismatch on " + key + " for " + graphEntityA + " and " + graphEntityB);
//            } else {
//                assertEquals(propsA.get(key), propsB.get(key), "mismatch on " + key + " for " + graphEntityA + " and " + graphEntityB);
//            }
//        }
    }


    private boolean checkTrips(final Map<String, Object> expected, final Map<String, Object> props) {
        final String[] arrayA = (String[]) expected.get(TRIP_ID_LIST.getText());
        final String[] arrayB = (String[]) props.get(TRIP_ID_LIST.getText());
        if (arrayA.length!=arrayB.length) {
            return false;
        }

        final Set<String> setA = new HashSet<>(Arrays.asList(arrayA));
        final Set<String> setB = new HashSet<>(Arrays.asList(arrayB));

        return setA.equals(setB);
    }


    public void checkRelationshipsMatch(final List<GraphRelationship> listA, final List<GraphRelationship> listB) {
        for(final GraphRelationship expected : listA) {
            final IdFor<? extends CoreDomain> startId = expected.getStartDomainId(txnNeo4J);
            final IdFor<? extends CoreDomain> endId = expected.getEndDomainId(txnNeo4J);

            final List<GraphRelationship> beginAndEndMatch = listB.stream().
                    filter(graphRelationship -> graphRelationship.getStartDomainId(txnInMem).equals(startId)).
                    filter(graphRelationship -> graphRelationship.getEndDomainId(txnInMem).equals(endId)).
                    toList();
            assertFalse(beginAndEndMatch.isEmpty(), "Did not match begin " + startId + " and end " + endId + " for any of " + listB);

            final Map<String, Object> expectedProps = expected.getAllProperties();
            final Set<GraphRelationship> match = beginAndEndMatch.stream().
                    filter(graphRelationship -> matchProps(expectedProps, graphRelationship.getAllProperties())).
                    collect(Collectors.toSet());
            assertEquals(1, match.size(),"mismatch for " + expectedProps + " from " + beginAndEndMatch + " for relationship " + expected);
        }
    }


    private boolean matchProps(final Map<String, Object> expected, final Map<String, Object> props) {
        return compareProps(expected, props, false);

//        if (!expected.keySet().equals(props.keySet())) {
//            logger.error("mismatch on keys expected:" + expected.keySet() + " got " + props.keySet());
//            return false;
//        }
//
//        for(final String key : expected.keySet()) {
//            if (key.equals(TRANSPORT_MODES.getText())) {
//                if  (!checkModes(expected, props)) {
//                    logger.error("Failed on modes for expected:" +expected + " vs " + props);
//                    return false;
//                }
//            } else if (key.equals(TRIP_ID_LIST.getText())) {
//                if (!checkTrips(expected, props)) {
//                    logger.error("Failed on trip ids for expected:" +expected + " vs " + props);
//                    return false;
//                }
//            } else {
//                if (!expected.get(key).equals(props.get(key))) {
//                    logger.error("Failed on " + key + " for expected:" +expected + " vs " + props);
//                    return false;
//                }
//            }
//        }
//
//        return true;
    }

//    private static boolean checkModes(final Map<String, Object> expected, final Map<String, Object> props) {
//        final short[] arrayA = (short[]) expected.get(TRANSPORT_MODES.getText());
//        final short[] arrayB = (short[]) props.get(TRANSPORT_MODES.getText());
//        if (arrayA.length!=arrayB.length) {
//            return false;
//        }
//        for (int i = 0; i < arrayA.length; i++) {
//            if (arrayA[i]!=arrayB[i]) {
//                return false;
//            }
//        }
//
//        return true;
//    }

    public void checkSameDirections(final GraphNode graphNodeA, final GraphNode graphNodeB, final GraphDirection direction) {
        final List<GraphRelationship> relationshipsA = graphNodeA.getRelationships(txnNeo4J, direction).toList();
        final List<GraphRelationship> relationshipsB = graphNodeB.getRelationships(txnInMem, direction).toList();

        assertEquals(relationshipsA.size(), relationshipsB.size());

        for (final TransportRelationshipTypes type : TransportRelationshipTypes.values()) {
            long countA = relationshipsA.stream().filter(relationship -> relationship.isType(type)).count();
            long countB = relationshipsB.stream().filter(relationship -> relationship.isType(type)).count();
            assertEquals(countA, countB);
        }

        for (final GraphRelationship relationshipA : relationshipsA) {
            final IdFor<? extends CoreDomain> beginId = relationshipA.getStartDomainId(txnNeo4J);
            final IdFor<? extends CoreDomain> endId = relationshipA.getEndDomainId(txnNeo4J);

            List<GraphRelationship> beingAndEndMatch = relationshipsB.stream().
                    filter(relationship -> relationship.getStartDomainId(txnInMem).equals(beginId)).
                    filter(relationship -> relationship.getEndDomainId(txnInMem).equals(endId)).toList();

            assertFalse(beingAndEndMatch.isEmpty());

            TransportRelationshipTypes typeA = relationshipA.getType();

            List<GraphRelationship> typeMatches = beingAndEndMatch.stream().
                    filter(relationship -> relationship.isType(typeA)).toList();
            assertFalse(typeMatches.isEmpty());

            final Map<String, Object> expectedProps = relationshipA.getAllProperties();
            final Set<GraphRelationship> match = typeMatches.stream().
                    filter(graphRelationship -> matchProps(expectedProps, graphRelationship.getAllProperties())).
                    collect(Collectors.toSet());

            assertEquals(1, match.size(), "mismatch for " + expectedProps + " from found " + typeMatches +
                    " for expected relationship " + relationshipA);

        }
    }


}
