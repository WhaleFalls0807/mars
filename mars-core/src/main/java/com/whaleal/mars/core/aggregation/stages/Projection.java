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
package com.whaleal.mars.core.aggregation.stages;

import com.whaleal.mars.codecs.MarsOrmException;
import com.whaleal.mars.core.aggregation.expressions.Expressions;
import com.whaleal.mars.core.aggregation.expressions.impls.Expression;
import com.whaleal.mars.core.aggregation.expressions.impls.Fields;
import com.whaleal.mars.core.aggregation.expressions.impls.PipelineField;

import java.util.ArrayList;
import java.util.List;


public class Projection extends Stage {
    private Fields<Projection> includes;
    private Fields<Projection> excludes;
    private boolean suppressId;

    protected Projection() {
        super("$project");
    }


    public static Projection of() {
        return new Projection();
    }


    public Projection exclude(String name) {
        return exclude(name, Expressions.value(false));
    }


    public List<PipelineField> getFields() {
        List<PipelineField> fields = new ArrayList<>();

        if (includes != null) {
            fields.addAll(includes.getFields());
        }
        if (excludes != null) {
            fields.addAll(excludes.getFields());
        }
        if (suppressId) {
            fields.add(new PipelineField("_id", Expressions.value(false)));
        }
        return fields;
    }


    public Projection include(String name, Expression value) {
        if (includes == null) {
            includes = Fields.on(this);
        }
        includes.add(name, value);
        validateProjections();
        return this;
    }


    public Projection include(String name) {
        return include(name, Expressions.value(true));
    }


    public Projection suppressId() {
        suppressId = true;
        return this;
    }

    private Projection exclude(String name, Expression value) {
        if (excludes == null) {
            excludes = Fields.on(this);
        }
        excludes.add(name, value);
        validateProjections();
        return this;
    }

    private void validateProjections() {
        if (includes != null && excludes != null) {
            if (excludes.size() > 1 || !"_id".equals(excludes.getFields().get(0).getName())) {
                throw new MarsOrmException();
            }
        }
    }
}
