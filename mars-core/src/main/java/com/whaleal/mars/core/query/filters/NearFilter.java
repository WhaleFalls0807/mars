package com.whaleal.mars.core.query.filters;

import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import com.mongodb.client.model.geojson.Point;


import com.whaleal.mars.codecs.MongoMappingContext;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.Map;

/**
 * Defines a filter for $near and $nearSphere queries
 *
 */
public class NearFilter extends Filter {
    private Double maxDistance;
    private Double minDistance;
    private CoordinateReferenceSystem crs;

    NearFilter(String filterName, String field, Point point) {
        super(filterName, field, point);
    }

    /**
     * @param opts the options to apply
     */

    public void applyOpts(Map<?, ?> opts) {
        maxDistance = (Double) opts.get("$maxDistance");
        minDistance = (Double) opts.get("$minDistance");
    }

    /**
     * Sets the max distance to consider
     *
     * @param maxDistance the max
     * @return this
     */
    public NearFilter maxDistance(Double maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }

    /**
     * Sets the min distance to consider
     * @param minDistance the min
     * @return this
     */
    public NearFilter minDistance(Double minDistance) {
        this.minDistance = minDistance;
        return this;
    }

    /**
     * Sets the coordinate reference system to use
     *
     * @param crs the crs
     * @return this
     */
    public NearFilter crs(CoordinateReferenceSystem crs) {
        this.crs = crs;
        return this;
    }

    @Override
    public void encode( MongoMappingContext mapper, BsonWriter writer, EncoderContext context) {
        writer.writeStartDocument(path(mapper.getMapper()));
        if (isNot()) {
            writer.writeStartDocument("$not");
        }
        writer.writeStartDocument(getName());
        writer.writeName("$geometry");
        writeUnnamedValue(getValue(mapper), mapper, writer, context);
        if (maxDistance != null) {
            writeNamedValue("$maxDistance", maxDistance, mapper, writer, context);
        }
        if (minDistance != null) {
            writeNamedValue("$minDistance", minDistance, mapper, writer, context);
        }
        if (crs != null) {
            writeNamedValue("crs", crs, mapper, writer, context);
        }
        writer.writeEndDocument();
        if (isNot()) {
            writer.writeEndDocument();
        }
        writer.writeEndDocument();
    }

}
