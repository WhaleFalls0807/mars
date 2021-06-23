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
package com.whaleal.mars.session.result;

import org.bson.BsonValue;

import java.util.Map;

/**
 * @author cx
 * @Date 2020/12/22
 */
public class InsertManyResult extends com.mongodb.client.result.InsertManyResult {

    private com.mongodb.client.result.InsertManyResult originInsertManyResult;

    public InsertManyResult(com.mongodb.client.result.InsertManyResult originInsertManyResult) {
        this.originInsertManyResult = originInsertManyResult;
    }

    public InsertManyResult() {

    }

    @Override
    public boolean wasAcknowledged() {
        return originInsertManyResult.wasAcknowledged();
    }

    @Override
    public Map<Integer, BsonValue> getInsertedIds() {
        return originInsertManyResult.getInsertedIds();
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        InsertManyResult that = (InsertManyResult) obj;

        if (originInsertManyResult != that.originInsertManyResult) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + (originInsertManyResult != null ? originInsertManyResult.hashCode() : 0);

        return result;
    }

    @Override
    public String toString() {
        return "InsertManyResult{" +
                "originInsertManyResult=" + originInsertManyResult +
                '}';
    }

    public com.mongodb.client.result.InsertManyResult getOriginInsertManyResult() {
        return originInsertManyResult;
    }

    public void setOriginInsertManyResult(com.mongodb.client.result.InsertManyResult originInsertManyResult) {
        this.originInsertManyResult = originInsertManyResult;
    }
}
