package com.whaleal.mars.core.aggregation.expressions.impls;


import com.whaleal.mars.codecs.MongoMappingContext;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

public class ValueExpression extends Expression implements SingleValuedExpression {
    private final Object object;

    public ValueExpression(Object value) {
        super("unused");
        object = value;
    }

    @Override
    public void encode( MongoMappingContext mapper, BsonWriter writer, EncoderContext encoderContext) {
        if (object != null) {
            Codec codec = mapper.getCodecRegistry().get(object.getClass());
            encoderContext.encodeWithChildContext(codec, writer, object);
        } else {
            writer.writeNull();
        }
    }

    @Override
    public String toString() {
        return String.format("ValueExpression{value=%s}", getValue());
    }
}
