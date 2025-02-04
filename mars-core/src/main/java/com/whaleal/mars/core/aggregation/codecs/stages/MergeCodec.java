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
import com.whaleal.mars.core.aggregation.expressions.impls.Expression;
import com.whaleal.mars.core.aggregation.stages.Merge;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings({"rawtypes", "unchecked"})
public class MergeCodec extends StageCodec<Merge> {
    public MergeCodec(MongoMappingContext mapper) {
        super(mapper);
    }

    @Override
    public Class<Merge> getEncoderClass() {
        return Merge.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Merge merge, EncoderContext encoderContext) {
        ExpressionHelper.document(writer, () -> {
            String collection = merge.getType() != null
                    ? getMapper().getEntityModel(merge.getType()).getCollectionName()
                    : merge.getCollection();
            String database = merge.getDatabase();

            if (database == null) {
                writer.writeString("into", collection);
            } else {
                ExpressionHelper.document(writer, "into", () -> {
                    writer.writeString("db", database);
                    writer.writeString("coll", collection);
                });
            }

            List<String> on = merge.getOn();
            if (on != null) {
                if (on.size() == 1) {
                    writer.writeString("on", on.get(0));
                } else {
                    ExpressionHelper.array(writer, "on", () -> on.forEach(writer::writeString));
                }
            }
            Map<String, Expression> variables = merge.getVariables();
            if (variables != null) {
                ExpressionHelper.document(writer, "let", () -> {
                    for (Entry<String, Expression> entry : variables.entrySet()) {
                        ExpressionHelper.value(getMapper(), writer, entry.getKey(), entry.getValue(), encoderContext);
                    }
                });
            }
            writeEnum(writer, "whenMatched", merge.getWhenMatched());
            ExpressionHelper.value(getMapper(), writer, "whenMatched", merge.getWhenMatchedPipeline(), encoderContext);
            writeEnum(writer, "whenNotMatched", merge.getWhenNotMatched());
        });
    }

    private void writeEnum(BsonWriter writer, String name, Enum<?> value) {
        if (value != null) {
            writer.writeString(name, value.name().toLowerCase());
        }
    }
}
