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
package com.whaleal.mars.codecs.internal;

import org.bson.BsonInvalidOperationException;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import com.whaleal.mars.codecs.RepresentationConfigurable;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.types.Decimal128;

import static java.lang.String.format;
import static org.bson.BsonType.*;


public class ShortCodec implements Codec<Short>, RepresentationConfigurable<Short> {

    private BsonType representation;

    public ShortCodec() {
        this.representation = INT32;
    }

    private ShortCodec(final BsonType representation) {
        this.representation = representation;
    }

    @Override
    public void encode(final BsonWriter writer, final Short value, final EncoderContext encoderContext) {
        switch (representation) {
            case INT32:
                writer.writeInt32(value.intValue());
                break;
            case INT64:
                writer.writeInt64(value.longValue());
                break;
            case DOUBLE:
                writer.writeDouble(value.doubleValue());
                break;
            case DECIMAL128:
                writer.writeDecimal128(new Decimal128(value));
                break;
            case STRING:
                writer.writeString(value.toString());
                break;
            default:
                throw new BsonInvalidOperationException("Cannot encode a  short to a " + representation);
        }
    }

    @Override
    public Short decode(final BsonReader reader, final DecoderContext decoderContext) {
        int value = NumberCodecHelper.decodeInt(reader);
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new BsonInvalidOperationException(format("%s can not be converted into a Short.", value));
        }
        return (short) value;
    }

    @Override
    public Class<Short> getEncoderClass() {
        return Short.class;
    }

    @Override
    public BsonType getRepresentation() {
        return representation;
    }

    @Override
    public Codec<Short> withRepresentation(BsonType bsonType) {
        if (representation != BsonType.STRING && representation != INT32 && representation != INT64 && representation != DOUBLE && representation != DECIMAL128) {
            throw new CodecConfigurationException(representation + " is not a supported representation for Short");
        }
        return new ShortCodec(bsonType);
    }
}
