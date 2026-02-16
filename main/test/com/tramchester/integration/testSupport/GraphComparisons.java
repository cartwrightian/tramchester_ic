package com.tramchester.integration.testSupport;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
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
            final Map<GraphPropertyKey, Object> relationshipProps = outgoingNeo4J.getAllProperties();

            // Note: have to match relationships via end node labels and props
            final Map<GraphPropertyKey, Object> endNodeProps = destinationNode.getAllProperties();
            final ImmutableEnumSet<GraphLabel> endNodeLabels = destinationNode.getLabels();

            // types
            final List<GraphRelationship> matchType = inMemoryNode.getRelationships(txnInMem, Outgoing, type).toList();
            assertFalse(matchType.isEmpty(), "Failed to match relationship type " + type + " within " +
                            inMemoryNode.getRelationships(txnInMem, Outgoing, allTypes).toList() + " at "
                    + inMemoryNode + "  " + inMemoryNode.getAllProperties() + " neo4J node props " + neo4JNode.getAllProperties());

            // relationship props
            final List<GraphRelationship> matchProps = matchType.stream().
                    filter(relat -> {
                        final Map<GraphPropertyKey, Object> allProperties = fixUpDayOffsetIfNeeded(relat.getAllProperties());
                        return compareProps(relationshipProps, allProperties, false);
                    }).toList();
            assertFalse(matchProps.isEmpty(), "mismatch on relationship props " + prettyPrintProps(relationshipProps) + " not matching any of " + matchType);

            // end node labels
            final List<GraphRelationship> matchNodeLabels = matchProps.stream().
                    filter(relat -> relat.getEndNode(txnInMem).getLabels().equals(endNodeLabels)).
                    toList();
            assertFalse(matchNodeLabels.isEmpty());

            // end node props
            final List<GraphRelationship> matchNodeProps = matchNodeLabels.stream().
                    filter(relat -> compareProps(endNodeProps,
                            fixUpDayOffsetIfNeeded(relat.getEndNode(txnInMem).getAllProperties()), false)).
                    toList();
            assertFalse(matchNodeProps.isEmpty(), "No matches for " + endNodeProps + " and any of " + matchNodeLabels);

            assertEquals(1, matchNodeProps.size(), endNodeProps + " is not matched by exactly one " + matchNodeProps);

            final GraphRelationship matchingRelationship = matchNodeProps.getFirst();

            visitMatchedNodes(destinationNode, matchingRelationship.getEndNode(txnInMem), depth-1);
        });

    }

    private String prettyPrintProps(Map<GraphPropertyKey, Object> props) {
        List<String> strings = props.entrySet().stream().map(item -> String.format("%s=%s", item.getKey(), prettyPrintObj(item.getValue()))).toList();
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        for (int i = 0; i < strings.size(); i++) {
            if (i>0) {
                builder.append(", ");
            }
            builder.append(strings.get(i));
        }
        builder.append('}');
        return builder.toString();
    }

    private String prettyPrintObj(Object value) {
        if (value instanceof String[] stringArray) {
            return Arrays.asList(stringArray).toString();
        } else {
            return value.toString();
        }
    }

    private Map<GraphPropertyKey, Object> fixUpDayOffsetIfNeeded(Map<GraphPropertyKey, Object> properties) {
        if (properties.containsKey(TIME)) {
            if (properties.containsKey(DAY_OFFSET)) {
                return properties;
            }
            // else need to add flag
            final TramTime time = (TramTime) properties.get(TIME);
            if (time.isNextDay()) {
                final HashMap<GraphPropertyKey, Object> replacement = new HashMap<>(properties);
                replacement.put(DAY_OFFSET, true);
                return replacement;
            }
        }
        return properties;

    }

    // TODO Push comparisons into test help and add logging
    private boolean compareProps(Map<GraphPropertyKey, Object> propsA, Map<GraphPropertyKey, Object> propsB, boolean logging) {
        final String diag = propsA + " vs " + propsB;
        if (propsA.size()!=propsB.size()) {
            if (logging) {
                logger.error("mismatch on size " + diag);
            }

            return false;
        }
        final Set<GraphPropertyKey> keysA = propsA.keySet();
        final Set<GraphPropertyKey> keysB = propsB.keySet();
        if (!keysA.equals(keysB)) {
            if (logging) {
                logger.error("mismatch on key set " + keysA + " vs " + keysB);
            }
            return false;
        }

        final Map<GraphPropertyKey, Object> mismatch = new HashMap<>();
        for(final GraphPropertyKey key : keysA) {
            final Object valueA = propsA.get(key);
            final Object valueB = propsB.get(key);

            final String diagOnProp = " for key: %s A:%s vs B:%s".formatted(key, valueA, valueB);

            final boolean matched;
            if (key.equals(COST)) {
                long seconds = (long) valueA;
                matched = Duration.ofSeconds(seconds).equals(valueB);
                if ((!matched) && logging) {
                    logger.error("mismatch on duration " + diagOnProp);
                }
            } else if (key.equals(TRANSPORT_MODES)) {
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
            } else if (key.equals(TRANSPORT_MODE)) {
                final TransportMode expectedMode = TransportMode.fromNumber((Short) valueA);
                matched = expectedMode.equals(valueB);
                if ((!matched) && logging) {
                    logger.error("mismatch on mode " + diagOnProp);
                }
            } else if (key.equals(TRIP_ID_LIST)) {
                final String[] arrayA = (String[]) valueA;
                final ImmutableIdSet<Trip> tripsB = (ImmutableIdSet<Trip>) valueB;
                if (arrayA.length == tripsB.size()) {
                    final List<String> stringsA = Arrays.asList(arrayA);
                    final ImmutableIdSet<Trip> tripsA = stringsA.stream().map(Trip::createId).collect(IdSet.idCollector());
                    matched = tripsA.equals(tripsB);
                } else {
                    if (logging) {
                        logger.error("Length mismatch between " + arrayA.length + " and " + tripsB.size() + " for " + diag);
                    }
                    matched = false;
                }
                if ((!matched) && logging) {
                    logger.error("mismatch on trip ids " + diagOnProp);
                }
            } else if (key.equals(TIME)) {
                final String txtA = valueA.toString();
                TramTime timeA = TramTime.parse(txtA);
                final boolean dayOffset = (Boolean) propsA.getOrDefault(DAY_OFFSET, false);
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

    public void checkProps(final GraphEntity<?> graphEntityA, final GraphEntity<?> graphEntityB) {
        final Map<GraphPropertyKey, Object> propsA = graphEntityA.getAllProperties();
        final Map<GraphPropertyKey, Object> propsB = graphEntityB.getAllProperties();

        compareProps(propsA, propsB, false);
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

            final Map<GraphPropertyKey, Object> expectedProps = expected.getAllProperties();
            final Set<GraphRelationship> match = beginAndEndMatch.stream().
                    filter(graphRelationship -> compareProps(expectedProps, graphRelationship.getAllProperties(), false)).
                    collect(Collectors.toSet());
            assertEquals(1, match.size(),"mismatch for " + expectedProps + " from " + beginAndEndMatch + " for relationship " + expected);
        }
    }


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

            final Map<GraphPropertyKey, Object> expectedProps = relationshipA.getAllProperties();
            final Set<GraphRelationship> match = typeMatches.stream().
                    filter(graphRelationship -> compareProps(expectedProps, graphRelationship.getAllProperties(), false)).
                    collect(Collectors.toSet());

            assertEquals(1, match.size(), "mismatch for " + expectedProps + " from found " + typeMatches +
                    " for expected relationship " + relationshipA);

        }
    }


}
