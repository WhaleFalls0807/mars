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
package com.whaleal.mars.core.aggregation.codecs.stages;

import com.whaleal.mars.codecs.MongoMappingContext;
import com.whaleal.mars.core.aggregation.codecs.ExpressionHelper;
import com.whaleal.mars.core.aggregation.stages.CollectionStats;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class CollectionStatsCodec extends StageCodec<CollectionStats> {
    public CollectionStatsCodec(MongoMappingContext mapper) {
        super(mapper);
    }

    @Override
    public Class<CollectionStats> getEncoderClass() {
        return CollectionStats.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, CollectionStats value, EncoderContext encoderContext) {
        ExpressionHelper.document(writer, () -> {
            if (value.getHistogram()) {
                ExpressionHelper.document(writer, "latencyStats", () -> writer.writeBoolean("histograms", true));
            }
            if (value.getScale() != null) {
                ExpressionHelper.document(writer, "storageStats", () -> writer.writeInt32("scale", value.getScale()));
            }
            if (value.getCount()) {
                ExpressionHelper.document(writer, "count", () -> {
                });
            }
        });
    }
}
