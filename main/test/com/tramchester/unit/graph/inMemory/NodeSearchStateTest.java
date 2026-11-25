package com.tramchester.unit.graph.inMemory;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.inMemory.GraphIdListInMem;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.core.inMemory.NodeIdInMemory;
import com.tramchester.graph.core.inMemory.SearchStateKey;
import com.tramchester.graph.search.inMemory.NodeSearchState;
import com.tramchester.graph.search.inMemory.NodeSearchStateFactory;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NodeSearchStateTest extends EasyMockSupport {

    NodeSearchStateFactory<Long> nodeSearchStateFactory;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        nodeSearchStateFactory = new NodeSearchStateFactory<>(Long::compare);
    }

    @Test
    void shouldOrderOnDurationPathFirst() {
        GraphPathInMemory path = createMockPathOfLength(1);

        replayAll();
        NodeSearchState<Long> stateA = nodeSearchStateFactory.createNodeSearchState(getStateKey(1), TramDuration.ofMinutes(8), path, false, 8L);
        NodeSearchState<Long> stateB = nodeSearchStateFactory.createNodeSearchState(getStateKey(2), TramDuration.ofMinutes(2), path, false, 2L);
        NodeSearchState<Long> stateC = nodeSearchStateFactory.createNodeSearchState(getStateKey(3), TramDuration.ofMinutes(11), path, false, 11L);

        PriorityQueue<NodeSearchState<Long>> queue = new PriorityQueue<>();
        queue.add(stateA);
        queue.add(stateB);
        queue.add(stateC);


        NodeSearchState<Long> first = queue.poll();
        NodeSearchState<Long> second = queue.poll();
        NodeSearchState<Long> thrid = queue.poll();

        verifyAll();

        assertEquals(stateC, first);
        assertEquals(stateA, second);
        assertEquals(stateB, thrid);

    }

    @Disabled("no longer using path length")
    @Test
    void shouldOrderOnDurationAfterLongestPath() {

        GraphPathInMemory pathA = createMockPathOfLength(8);
        GraphPathInMemory pathB = createMockPathOfLength(8);
        GraphPathInMemory pathC = createMockPathOfLength(4);

        replayAll();
        NodeSearchState<Long> stateA = nodeSearchStateFactory.createNodeSearchState(getStateKey(1), TramDuration.ofSeconds(50L), pathA, false, 50L);
        NodeSearchState<Long> stateB = nodeSearchStateFactory.createNodeSearchState(getStateKey(2), TramDuration.ofSeconds(45L), pathB, false, 45L);
        NodeSearchState<Long> stateC = nodeSearchStateFactory.createNodeSearchState(getStateKey(3), TramDuration.ofSeconds(60L), pathC, false, 60L);

        PriorityQueue<NodeSearchState<Long>> queue = new PriorityQueue<>();
        queue.add(stateA);
        queue.add(stateB);
        queue.add(stateC);

        NodeSearchState<Long> first = queue.poll();
        NodeSearchState<Long> second = queue.poll();
        NodeSearchState<Long> third = queue.poll();

        verifyAll();

        assertEquals(stateB, first);
        assertEquals(stateA, second);
        assertEquals(stateC, third);

    }

    @Test
    void shouldOrderOnShortestPathFirstWhenMultipleJumpQueue() {
        GraphPathInMemory path = createMockPathOfLength(8);

        replayAll();
        NodeSearchState<Long> stateA = nodeSearchStateFactory.createNodeSearchState(getStateKey(1), TramDuration.ofMinutes(8), path, true, 8L);
        NodeSearchState<Long> stateB = nodeSearchStateFactory.createNodeSearchState(getStateKey(2), TramDuration.ofMinutes(2), path, true, 2L);
        NodeSearchState<Long> stateC = nodeSearchStateFactory.createNodeSearchState(getStateKey(3), TramDuration.ofMinutes(11), path, true, 11L);

        PriorityQueue<NodeSearchState<Long>> queue = new PriorityQueue<>();
        queue.add(stateA);
        queue.add(stateB);
        queue.add(stateC);

        NodeSearchState<Long> first = queue.poll();
        NodeSearchState<Long> second = queue.poll();
        NodeSearchState<Long> thrid = queue.poll();

        verifyAll();

        assertEquals(stateB, first);
        assertEquals(stateA, second);
        assertEquals(stateC, thrid);
    }

    @Disabled("no longer using path length")
    @Test
    void shouldOrderOnDurationWhenSamePathLenAndMultipleJumpQueue() {

        GraphPathInMemory pathA = createMockPathOfLength(2);
        GraphPathInMemory pathB = createMockPathOfLength(2);
        GraphPathInMemory pathC = createMockPathOfLength(11);

        replayAll();
        NodeSearchState<Long> stateA = nodeSearchStateFactory.createNodeSearchState(getStateKey(1), TramDuration.ofSeconds(40L), pathA, true, 40L);
        NodeSearchState<Long> stateB = nodeSearchStateFactory.createNodeSearchState(getStateKey(2), TramDuration.ofSeconds(30L), pathB, true, 30L);
        NodeSearchState<Long> stateC = nodeSearchStateFactory.createNodeSearchState(getStateKey(3), TramDuration.ofSeconds(10L), pathC, true, 10L);

        PriorityQueue<NodeSearchState<Long>> queue = new PriorityQueue<>();
        queue.add(stateA);
        queue.add(stateB);
        queue.add(stateC);

        NodeSearchState<Long> first = queue.poll();
        NodeSearchState<Long> second = queue.poll();
        NodeSearchState<Long> thrid = queue.poll();

        verifyAll();

        assertEquals(stateB, first);
        assertEquals(stateA, second);
        assertEquals(stateC, thrid);
    }

    @Test
    void shouldJumpQueue() {
        GraphPathInMemory path = createMockPathOfLength(8);

        replayAll();
        NodeSearchState<Long> stateA = nodeSearchStateFactory.createNodeSearchState(getStateKey(1), TramDuration.ofMinutes(8), path, false, 8L);
        NodeSearchState<Long> stateB = nodeSearchStateFactory.createNodeSearchState(getStateKey(2), TramDuration.ofMinutes(8), path, true, 8L);
        NodeSearchState<Long> stateC = nodeSearchStateFactory.createNodeSearchState(getStateKey(3), TramDuration.ofMinutes(8), path, false,8L);

        PriorityQueue<NodeSearchState<Long>> queue = new PriorityQueue<>();
        queue.add(stateA);
        queue.add(stateB);
        queue.add(stateC);

        NodeSearchState<Long> first = queue.poll();
        NodeSearchState<Long> second = queue.poll();
        NodeSearchState<Long> thrid = queue.poll();

        verifyAll();

        assertEquals(stateB, first);
        assertEquals(stateC, second);
        assertEquals(stateA, thrid);
    }

    private GraphPathInMemory createMockPathOfLength(final int len) {
        GraphPathInMemory path = createMock(GraphPathInMemory.class);
        EasyMock.expect(path.length()).andStubReturn(len);
        EasyMock.expect(path.duplicate()).andStubReturn(path);
        return path;
    }

    private static @NotNull SearchStateKey getStateKey(final int id) {
        return new SearchStateKey(new NodeIdInMemory(id), new GraphIdListInMem());
    }
}
