package com.tramchester.unit.graph.inMemory;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.inMemory.GraphIdListInMem;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import com.tramchester.graph.core.inMemory.NodeIdInMemory;
import com.tramchester.graph.core.inMemory.SearchStateKey;
import com.tramchester.graph.search.inMemory.NodeSearchState;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NodeSearchStateTest extends EasyMockSupport {

    @Test
    void shouldOrderOnLongestPathFirst() {
        TramDuration duration = TramDuration.ZERO;

        GraphPathInMemory pathA = createMockPathOfLength(8);
        GraphPathInMemory pathB = createMockPathOfLength(2);
        GraphPathInMemory pathC = createMockPathOfLength(11);

        replayAll();
        NodeSearchState stateA = NodeSearchState.createNodeSearchState(getStateKey(1), duration, pathA, false);
        NodeSearchState stateB = NodeSearchState.createNodeSearchState(getStateKey(2), duration, pathB, false);
        NodeSearchState stateC = NodeSearchState.createNodeSearchState(getStateKey(3), duration, pathC, false);

        PriorityQueue<NodeSearchState> queue = new PriorityQueue<>();
        queue.add(stateA);
        queue.add(stateB);
        queue.add(stateC);


        NodeSearchState first = queue.poll();
        NodeSearchState second = queue.poll();
        NodeSearchState thrid = queue.poll();

        verifyAll();

        assertEquals(stateC, first);
        assertEquals(stateA, second);
        assertEquals(stateB, thrid);

    }

    @Test
    void shouldOrderOnDurationAfterLongestPath() {

        GraphPathInMemory pathA = createMockPathOfLength(8);
        GraphPathInMemory pathB = createMockPathOfLength(8);
        GraphPathInMemory pathC = createMockPathOfLength(4);

        replayAll();
        NodeSearchState stateA = NodeSearchState.createNodeSearchState(getStateKey(1), TramDuration.ofSeconds(50), pathA, false);
        NodeSearchState stateB = NodeSearchState.createNodeSearchState(getStateKey(2), TramDuration.ofSeconds(45), pathB, false);
        NodeSearchState stateC = NodeSearchState.createNodeSearchState(getStateKey(3), TramDuration.ofSeconds(60), pathC, false);

        PriorityQueue<NodeSearchState> queue = new PriorityQueue<>();
        queue.add(stateA);
        queue.add(stateB);
        queue.add(stateC);

        NodeSearchState first = queue.poll();
        NodeSearchState second = queue.poll();
        NodeSearchState third = queue.poll();

        verifyAll();

        assertEquals(stateB, first);
        assertEquals(stateA, second);
        assertEquals(stateC, third);

    }

    @Test
    void shouldOrderOnShortestPathFirstWhenMultipleJumpQueue() {
        TramDuration duration = TramDuration.ZERO;

        GraphPathInMemory pathA = createMockPathOfLength(8);
        GraphPathInMemory pathB = createMockPathOfLength(2);
        GraphPathInMemory pathC = createMockPathOfLength(11);

        replayAll();
        NodeSearchState stateA = NodeSearchState.createNodeSearchState(getStateKey(1), duration, pathA, true);
        NodeSearchState stateB = NodeSearchState.createNodeSearchState(getStateKey(2), duration, pathB, true);
        NodeSearchState stateC = NodeSearchState.createNodeSearchState(getStateKey(3), duration, pathC, true);

        PriorityQueue<NodeSearchState> queue = new PriorityQueue<>();
        queue.add(stateA);
        queue.add(stateB);
        queue.add(stateC);

        NodeSearchState first = queue.poll();
        NodeSearchState second = queue.poll();
        NodeSearchState thrid = queue.poll();

        verifyAll();

        assertEquals(stateB, first);
        assertEquals(stateA, second);
        assertEquals(stateC, thrid);
    }

    @Test
    void shouldOrderOnDurationWhenSamePathLenAndMultipleJumpQueue() {

        GraphPathInMemory pathA = createMockPathOfLength(2);
        GraphPathInMemory pathB = createMockPathOfLength(2);
        GraphPathInMemory pathC = createMockPathOfLength(11);

        replayAll();
        NodeSearchState stateA = NodeSearchState.createNodeSearchState(getStateKey(1), TramDuration.ofSeconds(40), pathA, true);
        NodeSearchState stateB = NodeSearchState.createNodeSearchState(getStateKey(2), TramDuration.ofSeconds(30), pathB, true);
        NodeSearchState stateC = NodeSearchState.createNodeSearchState(getStateKey(3), TramDuration.ofSeconds(10), pathC, true);

        PriorityQueue<NodeSearchState> queue = new PriorityQueue<>();
        queue.add(stateA);
        queue.add(stateB);
        queue.add(stateC);

        NodeSearchState first = queue.poll();
        NodeSearchState second = queue.poll();
        NodeSearchState thrid = queue.poll();

        verifyAll();

        assertEquals(stateB, first);
        assertEquals(stateA, second);
        assertEquals(stateC, thrid);
    }

    @Test
    void shouldJumpQueue() {
        TramDuration duration = TramDuration.ZERO;

        GraphPathInMemory pathA = createMockPathOfLength(8);
        GraphPathInMemory pathB = createMockPathOfLength(2);
        GraphPathInMemory pathC = createMockPathOfLength(11);

        replayAll();
        NodeSearchState stateA = NodeSearchState.createNodeSearchState(getStateKey(1), duration, pathA, false);
        NodeSearchState stateB = NodeSearchState.createNodeSearchState(getStateKey(2), duration, pathB, true);
        NodeSearchState stateC = NodeSearchState.createNodeSearchState(getStateKey(3), duration, pathC, false);

        PriorityQueue<NodeSearchState> queue = new PriorityQueue<>();
        queue.add(stateA);
        queue.add(stateB);
        queue.add(stateC);

        NodeSearchState first = queue.poll();
        NodeSearchState second = queue.poll();
        NodeSearchState thrid = queue.poll();

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
