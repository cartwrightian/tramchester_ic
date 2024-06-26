package com.tramchester.dataimport.data;

import com.tramchester.caching.CachableData;
import com.tramchester.geo.BoundingBox;

import java.util.Objects;

public class PostcodeHintData implements CachableData {
    private String code;
    private int minEasting;
    private int minNorthing;
    private int maxEasting;
    private int maxNorthing;

    public PostcodeHintData() {
        // for deserialization
    }

    public PostcodeHintData(String code, BoundingBox boundingBox)
    {
        this.code = code;
        this.minEasting = boundingBox.getMinEastings();
        this.minNorthing = boundingBox.getMinNorthings();
        this.maxEasting = boundingBox.getMaxEasting();
        this.maxNorthing = boundingBox.getMaxNorthings();
    }

    public String getCode() {
        return code;
    }

    public int getMinEasting() {
        return minEasting;
    }

    public int getMinNorthing() {
        return minNorthing;
    }

    public int getMaxEasting() {
        return maxEasting;
    }

    public int getMaxNorthing() {
        return maxNorthing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostcodeHintData that = (PostcodeHintData) o;
        return minEasting == that.minEasting && minNorthing == that.minNorthing && maxEasting == that.maxEasting && maxNorthing == that.maxNorthing && code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, minEasting, minNorthing, maxEasting, maxNorthing);
    }

    @Override
    public String toString() {
        return "PostcodeHintData{" +
                "code='" + code + '\'' +
                ", minEasting=" + minEasting +
                ", minNorthing=" + minNorthing +
                ", maxEasting=" + maxEasting +
                ", maxNorthing=" + maxNorthing +
                '}';
    }
}
