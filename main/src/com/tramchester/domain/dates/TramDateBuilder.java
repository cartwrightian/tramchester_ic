package com.tramchester.domain.dates;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class TramDateBuilder {
    public static YearBuilder forYear(int year) {
        return new YearBuilder(year);
    }

    public static class YearBuilder {
        private final int year;
        private final Set<TramDate> dates;
        private final Set<MonthBuilder> monthBuilders;

        public YearBuilder(int year) {
            this.year = year;
            dates = new HashSet<>();
            monthBuilders = new HashSet<>();
        }

        public YearBuilder add(int month, int day) {
            dates.add(TramDate.of(year, month, day));
            return this;
        }

        public Dates build() {
            Dates result = Dates.of(dates);
            monthBuilders.stream().flatMap(MonthBuilder::buildForMonth).forEach(result::add);
            return result;
        }

        public MonthBuilder forMonth(int month) {
            MonthBuilder builder = new MonthBuilder(this, month);
            monthBuilders.add(builder);
            return builder;
        }
    }

    public static class MonthBuilder {
        private final YearBuilder parent;
        private final int month;
        private final Set<Integer> days;

        public MonthBuilder(YearBuilder parent, int month) {
            this.parent = parent;
            this.month = month;
            days =new HashSet<>();
        }

        public MonthBuilder add(int... daysOfMonth) {
            Arrays.stream(daysOfMonth).forEach(days::add);
            return this;
        }

        Stream<TramDate> buildForMonth() {
            int year = parent.year;
            return days.stream().map(day -> TramDate.of(year, month, day));
        }

        public YearBuilder toYear() {
            return parent;
        }

        public Dates build() {
            return parent.build();
        }
    }
}
