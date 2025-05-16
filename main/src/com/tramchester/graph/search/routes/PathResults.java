package com.tramchester.graph.search.routes;

import java.util.stream.Stream;

public interface PathResults {

    boolean hasAny();

    int getDepth();

    int numberPossible();

    class HasPathResults implements PathResults {
        private final QueryPathsWithDepth.QueryPath pathFor;

        public HasPathResults(QueryPathsWithDepth.QueryPath pathFor) {
            this.pathFor = pathFor;
        }

        @Override
        public boolean hasAny() {
            return pathFor.anyMatch(queryPath -> queryPath.hasAny());
        }

        @Override
        public int getDepth() {
            return pathFor.getDepth();
        }

        @Override
        public int numberPossible() {
            return pathFor.size();
        }

        // test support, get at underlying details
        public Stream<QueryPathsWithDepth.QueryPath> forTesting() {
            return pathFor.forTesting().filter(QueryPathsWithDepth::hasAny);
        }

        @Override
        public String toString() {
            return "HasPathResults{" +
                    "pathFor=" + pathFor +
                    '}';
        }
    }

    class NoPathResults implements PathResults {

        @Override
        public boolean hasAny() {
            return false;
        }

        @Override
        public int getDepth() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int numberPossible() {
            return 0;
        }

    }

}
