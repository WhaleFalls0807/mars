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

import com.mongodb.CreateIndexCommitQuorum;
import com.mongodb.WriteConcern;

import java.util.concurrent.TimeUnit;

/**
 * @author cx
 * @Date 2020/12/15
 */
public class CreateIndexOptions implements WriteConfigurable<CreateIndexOptions> {

    private WriteConcern writeConcern;

    private com.mongodb.client.model.CreateIndexOptions originCreateIndexOptions;

    public CreateIndexOptions() {
        originCreateIndexOptions = new com.mongodb.client.model.CreateIndexOptions();
    }

    public CreateIndexOptions(com.mongodb.client.model.CreateIndexOptions originCreateIndexOptions) {
        this.originCreateIndexOptions = originCreateIndexOptions;
    }


    public long getMaxTime(final TimeUnit timeUnit) {
        return originCreateIndexOptions.getMaxTime(timeUnit);
    }


    public CreateIndexCommitQuorum getCommitQuorum() {
        return originCreateIndexOptions.getCommitQuorum();
    }

    public CreateIndexOptions maxTime(long maxTime, TimeUnit timeUnit) {
        originCreateIndexOptions.maxTime(maxTime, timeUnit);
        return this;
    }

    public CreateIndexOptions commitQuorum(CreateIndexCommitQuorum commitQuorum) {
        originCreateIndexOptions.commitQuorum(commitQuorum);
        return this;
    }

    @Override
    public CreateIndexOptions writeConcern(WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    @Override
    public WriteConcern writeConcern() {
        return writeConcern;
    }

    @Override
    public com.mongodb.client.model.CreateIndexOptions getOriginOptions() {
        if (originCreateIndexOptions == null) {
            originCreateIndexOptions = new com.mongodb.client.model.CreateIndexOptions();
        }
        return originCreateIndexOptions;
    }
}
