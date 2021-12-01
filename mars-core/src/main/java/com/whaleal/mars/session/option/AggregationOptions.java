/**
 *    Copyright 2020-present  Shanghai Jinmu Information Technology Co., Ltd.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the Server Side Public License, version 1,
 *    as published by Shanghai Jinmu Information Technology Co., Ltd.(The name of the development team is Whaleal.)
 *
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    Server Side Public License for more details.
 *
 *    You should have received a copy of the Server Side Public License
 *    along with this program. If not, see
 *    <http://www.whaleal.com/licensing/server-side-public-license>.
 *
 *    As a special exception, the copyright holders give permission to link the
 *    code of portions of this program with the OpenSSL library under certain
 *    conditions as described in each individual source file and distribute
 *    linked combinations including the program with the OpenSSL library. You
 *    must comply with the Server Side Public License in all respects for
 *    all of the code used other than as permitted herein. If you modify file(s)
 *    with this exception, you may extend this exception to your version of the
 *    file(s), but you are not obligated to do so. If you do not wish to do so,
 *    delete this exception statement from your version. If you delete this
 *    exception statement from all source files in the program, then also delete
 *    it in the license file.
 */
package com.whaleal.mars.session.option;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import org.bson.Document;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Defines options to be applied to an aggregation pipeline.
 */
@SuppressWarnings("unused")
public class AggregationOptions implements ReadConfigurable<AggregationOptions> {
    private boolean allowDiskUse;
    private Integer batchSize;
    private boolean bypassDocumentValidation;
    private Collation collation;
    private Long maxTimeMS;
    private ClientSession clientSession;
    private ReadPreference readPreference;
    private ReadConcern readConcern;
    private WriteConcern writeConcern;
    private Document hint;

    /**
     * @return the configuration value
     */
    public boolean allowDiskUse() {
        return allowDiskUse;
    }

    /**
     * Enables writing to temporary files.
     *
     * @param allowDiskUse true to enable
     * @return this
     */
    public AggregationOptions allowDiskUse(boolean allowDiskUse) {
        this.allowDiskUse = allowDiskUse;
        return this;
    }

    /**
     * Applies the configured options to the collection.
     *
     * @param documents  the stage documents
     * @param collection the collection to configure
     * @param resultType the result type
     * @param <T>        the collection type
     * @param <S>        the result type
     * @return the updated collection
     */
    public <S, T> AggregateIterable<S> apply(List<Document> documents, MongoCollection<T> collection,
                                             Class<S> resultType) {
        MongoCollection<T> bound = collection;
        if (readConcern != null) {
            bound = bound.withReadConcern(readConcern);
        }
        if (readPreference != null) {
            bound = bound.withReadPreference(readPreference);
        }
        AggregateIterable<S> aggregate = bound.aggregate(documents, resultType)
                .allowDiskUse(allowDiskUse)
                .bypassDocumentValidation(bypassDocumentValidation);
        if (batchSize != null) {
            aggregate.batchSize(batchSize);
        }
        if (collation != null) {
            aggregate.collation(collation);
        }
        if (maxTimeMS != null) {
            aggregate.maxTime(getMaxTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        }
        if (hint != null) {
            aggregate.hint(hint);
        }

        return aggregate;
    }

    /**
     * @return the configuration value
     */
    public int batchSize() {
        return batchSize;
    }

    /**
     * Sets the batch size for fetching results.
     *
     * @param batchSize the size
     * @return this
     */
    public AggregationOptions batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * @return the configuration value
     */
    public boolean bypassDocumentValidation() {
        return bypassDocumentValidation;
    }

    /**
     * Enables the aggregation to bypass document validation during the operation. This lets you insert documents that do not
     * meet the validation requirements.
     * <p>
     * Applicable only if you specify the $out or $merge aggregation stages.
     *
     * @param bypassDocumentValidation true to enable the bypass
     * @return this
     */
    public AggregationOptions bypassDocumentValidation(boolean bypassDocumentValidation) {
        this.bypassDocumentValidation = bypassDocumentValidation;
        return this;
    }


    public AggregationOptions clientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
        return this;
    }


    public ClientSession clientSession() {
        return clientSession;
    }

    /**
     * @return the configuration value
     */
    public Collation collation() {
        return collation;
    }

    /**
     * Specifies the collation to use for the operation.
     * <p>
     * Collation allows users to specify language-specific rules for string comparison, such as rules for letter case and accent marks.
     *
     * @param collation the collation to use
     * @return this
     */
    public AggregationOptions collation(Collation collation) {
        this.collation = collation;
        return this;
    }

    /**
     * @return the configuration value
     */
    public boolean getAllowDiskUse() {
        return allowDiskUse;
    }

    /**
     * @return the configuration value
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * @return the configuration value
     */
    public boolean getBypassDocumentValidation() {
        return bypassDocumentValidation;
    }

    /**
     * @return the configuration value
     */
    public Collation getCollation() {
        return collation;
    }

    /**
     * @param unit the target unit type
     * @return the configuration value
     */
    public long getMaxTime(TimeUnit unit) {
        return unit.convert(maxTimeMS, TimeUnit.MILLISECONDS);
    }

    /**
     * @return the configuration value
     */
    public long getMaxTimeMS() {
        return maxTimeMS;
    }

    /**
     * @return the configuration value
     */
    public ReadConcern getReadConcern() {
        return readConcern;
    }

    /**
     * @return the configuration value
     */
    public ReadPreference getReadPreference() {
        return readPreference;
    }

    /**
     * @return the hint for which index to use. A null value means no hint is set.
     * @mongodb.server.release 3.6
     */
    public Document hint() {
        return hint;
    }

    /**
     * Sets the hint for which index to use. A null value means no hint is set.
     *
     * @param hint the hint
     * @return this
     */
    public AggregationOptions hint(String hint) {
        this.hint = new Document("hint", hint);
        return this;
    }

    @Override
    public <C> MongoCollection<C> prepare(MongoCollection<C> collection) {
        MongoCollection<C> updated = collection;
        WriteConcern writeConcern = writeConcern();
        if (writeConcern != null) {
            updated = updated.withWriteConcern(writeConcern);
        }
        ReadConcern readConcern = getReadConcern();
        if (readConcern != null) {
            updated = updated.withReadConcern(readConcern);
        }
        ReadPreference readPreference = getReadPreference();
        if (readPreference != null) {
            updated = updated.withReadPreference(readPreference);
        }

        return updated;
    }

    @Override
    public <T> T getOriginOptions() {
        throw new UnsupportedOperationException("AggregationOptions  can't get OriginOptions ");
    }

    /**
     * Specifies the read concern.
     *
     * @param readConcern the read concern to use
     * @return this
     */
    public AggregationOptions readConcern(ReadConcern readConcern) {
        this.readConcern = readConcern;
        return this;
    }

    /**
     * Sets the read preference to use
     *
     * @param readPreference the read preference
     * @return this
     */
    public AggregationOptions readPreference(ReadPreference readPreference) {
        this.readPreference = readPreference;
        return this;
    }

    /**
     * @return the configuration value
     */
    public long maxTimeMS() {
        return maxTimeMS;
    }

    /**
     * Specifies a time limit in milliseconds for processing operations on a cursor. If you do not specify a value for maxTimeMS,
     * operations will not time out. A value of 0 explicitly specifies the default unbounded behavior.
     *
     * @param maxTimeMS the max time in milliseconds
     * @return this
     */
    public AggregationOptions maxTimeMS(long maxTimeMS) {
        this.maxTimeMS = maxTimeMS;
        return this;
    }

    /**
     * @return the configuration value
     */
    public ReadConcern readConcern() {
        return readConcern;
    }

    /**
     * @return the configuration value
     */
    public ReadPreference readPreference() {
        return readPreference;
    }

    /**
     * Sets the write concern to use
     *
     * @param writeConcern the write concern
     * @return this
     */
    public AggregationOptions writeConcern( WriteConcern writeConcern ) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * @return the configuration value
     */

    public WriteConcern writeConcern() {
        return writeConcern;
    }
}
