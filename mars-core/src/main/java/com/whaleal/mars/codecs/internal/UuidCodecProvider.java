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

 import org.bson.UuidRepresentation;
 import org.bson.codecs.Codec;
 import org.bson.codecs.UuidCodec;
 import org.bson.codecs.configuration.CodecProvider;
 import org.bson.codecs.configuration.CodecRegistry;

 import java.util.UUID;


 public class UuidCodecProvider implements CodecProvider {

     private UuidRepresentation uuidRepresentation;

     public UuidCodecProvider(final UuidRepresentation uuidRepresentation) {
         this.uuidRepresentation = uuidRepresentation;
     }

     @Override
     @SuppressWarnings("unchecked")
     public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
         if (clazz == UUID.class) {
             return (Codec<T>) (new UuidCodec(uuidRepresentation));
         }
         return null;
     }
 }
