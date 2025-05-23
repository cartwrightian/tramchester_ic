package com.tramchester.graph.search.routes;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.InterchangeStation;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface QueryPathsWithDepth {
    boolean hasAny();

    int getDepth();

    interface QueryPath extends QueryPathsWithDepth {

        @Deprecated
        Stream<QueryPath> forTesting();

        boolean forTesting(Predicate<InterchangeStation> stationPredicate);

        int size();

        boolean anyMatch(final Predicate<QueryPath> predicate);
    }

    class AnyOf implements QueryPath {
        private final List<QueryPath> paths;

        AnyOf(final Set<? extends QueryPath> paths) {
            this.paths = new ArrayList<>(paths);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnyOf that = (AnyOf) o;
            return paths.equals(that.paths);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paths);
        }

        @Override
        public Stream<QueryPath> forTesting() {
            return paths.stream();
        }

        @Override
        public String toString() {
            return "AnyOfContained{" + toString(paths) +
                    '}';
        }

        private String toString(final List<QueryPath> paths) {
            final StringBuilder output = new StringBuilder();
            paths.forEach(interchangePath -> {
                output.append(System.lineSeparator());
                output.append(interchangePath.toString());
            });
            output.append(System.lineSeparator());
            return output.toString();
        }

        @Override
        public boolean forTesting(final Predicate<InterchangeStation> stationPredicate) {
            return paths.stream().anyMatch(path -> path.forTesting(stationPredicate));
        }

        @Override
        public boolean hasAny() {
            return paths.stream().anyMatch(QueryPath::hasAny);
        }

        @Override
        public int getDepth() {
            final OptionalInt findMaximum = paths.stream().mapToInt(QueryPath::getDepth).max();
            return findMaximum.orElse(Integer.MAX_VALUE);
        }

        @Override
        public int size() {
            return paths.size();
        }

        @Override
        public boolean anyMatch(final Predicate<QueryPath> predicate) {
            return paths.stream().anyMatch(predicate);
        }
    }

    class BothOf implements QueryPath {
        private final QueryPath pathsA;
        private final QueryPath pathsB;

        public BothOf(final QueryPath pathsA, final QueryPath pathsB) {
            this.pathsA = pathsA;
            this.pathsB = pathsB;
        }

        public QueryPath getFirst() {
            return pathsA;
        }

        public QueryPath getSecond() {
            return pathsB;
        }

        @Override
        public Stream<QueryPath> forTesting() {
            return Stream.concat(pathsA.forTesting(), pathsB.forTesting());
        }

        @Override
        public boolean anyMatch(final Predicate<QueryPath> predicate) {
            return pathsA.anyMatch(predicate) || pathsB.anyMatch(predicate);
        }

        @Override
        public boolean forTesting(final Predicate<InterchangeStation> stationPredicate) {
            return pathsA.forTesting(stationPredicate) && pathsB.forTesting(stationPredicate);
        }

        @Override
        public boolean hasAny() {
            return pathsA.hasAny() && pathsB.hasAny();
        }

        @Override
        public int getDepth() {
            final int contained = Math.max(pathsA.getDepth(), pathsB.getDepth());
            return contained + 1;
        }

        @Override
        public int size() {
            return pathsA.size() + pathsB.size();
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BothOf that = (BothOf) o;
            return pathsA.equals(that.pathsA) && pathsB.equals(that.pathsB);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pathsA, pathsB);
        }

        @Override
        public String toString() {
            return "BothOfPaths{" +
                    "pathsA=" + pathsA +
                    ", pathsB=" + pathsB +
                    '}';
        }

    }

    class ZeroPaths implements QueryPath {

        private static final ZeroPaths theInstance = new ZeroPaths();

        public static ZeroPaths get() {
            return theInstance;
        }

        private ZeroPaths() {

        }

        @Override
        public boolean forTesting(Predicate<InterchangeStation> stationPredicate) {
            return false;
        }

        @Override
        public boolean hasAny() {
            return false;
        }

        @Override
        public int getDepth() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean anyMatch(final Predicate<QueryPath> predicate) {
            return false;
        }

        @Override
        public Stream<QueryPath> forTesting() {
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "ZeroPaths{}";
        }
    }

    class AnyOfInterchanges implements QueryPath {
        // any of these changes being available makes the path valid
        private final Set<InterchangeStation> changes;

        public AnyOfInterchanges(final Set<InterchangeStation> changes) {
            if (changes==null) {
                throw new RuntimeException("Cannot pass in null changes");
            }
            if (changes.isEmpty()) {
                throw new RuntimeException("Cannot pass in no interchanges");
            }
            if (changes.size()==1) {
                throw new RuntimeException("Not for 1 change " + HasId.asIds(changes));
            }
            this.changes = changes;
        }

        public static QueryPath Of(final Set<InterchangeStation> changes) {
            if (changes.size()==1) {
                final InterchangeStation change = changes.iterator().next();
                return new SingleInterchange(change);
            } else {
                return new AnyOfInterchanges(changes);
            }
        }

        @Override
        public String toString() {
            return "AnyOfInterchanges{" +
                    "changes=" + HasId.asIds(changes) +
                    '}';
        }

        @Override
        public Stream<QueryPath> forTesting() {
            return changes.stream().map(SingleInterchange::new);
        }

        @Override
        public boolean anyMatch(final Predicate<QueryPath> predicate) {
            return changes.stream().map(SingleInterchange::new).anyMatch(predicate);
        }

        @Override
        public boolean hasAny() {
            return true;
        }

        @Override
        public int getDepth() {
            return 1;
        }

        @Override
        public int size() {
            return changes.size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnyOfInterchanges that = (AnyOfInterchanges) o;
            return changes.equals(that.changes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(changes);
        }

        @Override
        public boolean forTesting(Predicate<InterchangeStation> stationPredicate) {
            return changes.stream().anyMatch(stationPredicate);
        }
    }

    class SingleInterchange implements QueryPath {

        private final InterchangeStation interchangeStation;

        public SingleInterchange(final InterchangeStation interchangeStation) {
            this.interchangeStation = interchangeStation;
        }

        @Override
        public Stream<QueryPath> forTesting() {
            return Stream.of(new SingleInterchange(interchangeStation));
        }

        @Override
        public boolean forTesting(final Predicate<InterchangeStation> stationPredicate) {
            return stationPredicate.test(interchangeStation);
        }

        @Override
        public boolean hasAny() {
            return true;
        }

        @Override
        public int getDepth() {
            return 1;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean anyMatch(final Predicate<QueryPath> predicate) {
            return predicate.test(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SingleInterchange that = (SingleInterchange) o;
            return interchangeStation.equals(that.interchangeStation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(interchangeStation);
        }

        @Override
        public String toString() {
            return "SingleInterchange{" +
                    "change=" + interchangeStation.getId() +
                    '}';
        }
    }




}
