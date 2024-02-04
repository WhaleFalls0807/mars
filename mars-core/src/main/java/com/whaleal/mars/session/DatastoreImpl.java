/**
 * Copyright 2020-present  Shanghai Jinmu Information Technology Co., Ltd.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by Shanghai Jinmu Information Technology Co., Ltd.(The name of the development team is Whaleal.)
 * <p>
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Server Side Public License for more details.
 * <p>
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.whaleal.com/licensing/server-side-public-license>.
 * <p>
 * As a special exception, the copyright holders give permission to link the
 * code of portions of this program with the OpenSSL library under certain
 * conditions as described in each individual source file and distribute
 * linked combinations including the program with the OpenSSL library. You
 * must comply with the Server Side Public License in all respects for
 * all of the code used other than as permitted herein. If you modify file(s)
 * with this exception, you may extend this exception to your version of the
 * file(s), but you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version. If you delete this
 * exception statement from all source files in the program, then also delete
 * it in the license file.
 */
package com.whaleal.mars.session;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.CreateViewOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ValidationOptions;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.Nullable;

import com.whaleal.mars.codecs.MarsOrmException;
import com.whaleal.mars.codecs.MongoMappingContext;
import com.whaleal.mars.codecs.pojo.EntityModel;
import com.whaleal.mars.codecs.pojo.PropertyModel;
import com.whaleal.mars.codecs.pojo.annotations.CappedAt;
import com.whaleal.mars.codecs.pojo.annotations.Collation;
import com.whaleal.mars.codecs.pojo.annotations.Concern;
import com.whaleal.mars.codecs.pojo.annotations.TimeSeries;
import com.whaleal.mars.codecs.writer.DocumentWriter;
import com.whaleal.mars.core.aggregation.AggregationPipeline;
import com.whaleal.mars.core.gridfs.GridFsObject;
import com.whaleal.mars.core.gridfs.GridFsResource;
import com.whaleal.mars.core.index.Index;
import com.whaleal.mars.core.index.IndexDirection;
import com.whaleal.mars.core.index.IndexHelper;
import com.whaleal.mars.core.index.IndexUtil;
import com.whaleal.mars.core.internal.diagnostics.logging.LogFactory;
import com.whaleal.mars.core.internal.diagnostics.logging.Logger;
import com.whaleal.mars.core.query.*;
import com.whaleal.mars.session.option.FindOneAndDeleteOptions;
import com.whaleal.mars.session.option.FindOneAndReplaceOptions;
import com.whaleal.mars.session.option.FindOneAndUpdateOptions;
import com.whaleal.mars.session.option.IndexOptions;
import com.whaleal.mars.session.option.InsertManyOptions;
import com.whaleal.mars.session.option.InsertOneOptions;
import com.whaleal.mars.session.option.TimeSeriesOptions;
import com.whaleal.mars.session.option.UpdateOptions;
import com.whaleal.mars.session.option.*;
import com.whaleal.mars.session.transactions.MarsTransaction;
import com.whaleal.mars.util.*;
import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.whaleal.mars.util.Assert.*;


/**
 * 关于数据操作的数据操作的一些具体实现
 *
 * @Date 2020-12-03
 */
public class DatastoreImpl extends AggregationImpl implements Datastore {


    private static  final Logger  LOGGER = LogFactory.getLogger(DatastoreImpl.class);

    private final Lock lock = new ReentrantLock();
    private final MongoClient mongoClient;

    private final GridFSBucket defaultGridFSBucket;
    //缓存 collectionName weak reference
    private final Map< Class< ? >, String > collectionNameCache = new WeakHashMap< Class< ? >, String >();
    //  Operations
    private DatastoreOperations operations;

    protected DatastoreImpl( DatastoreImpl datastore ) {
        super(datastore.database, datastore.mapper);
        this.mongoClient = datastore.mongoClient;
        this.defaultGridFSBucket = datastore.defaultGridFSBucket;
        this.operations = new CollectionOperations();

        automatic();


    }

    protected DatastoreImpl( MongoClient mongoClient, String databaseName ) {
        super(mongoClient.getDatabase(databaseName));
        this.mongoClient = mongoClient;
        this.defaultGridFSBucket = GridFSBuckets.create(super.database);
        this.operations = new CollectionOperations();
        automatic();
    }

    protected DatastoreImpl( MongoClient mongoClient, MongoMappingContext mapper ) {
        super(mapper.getDatabase(), mapper);
        this.mongoClient = mongoClient;
        this.defaultGridFSBucket = GridFSBuckets.create(super.database);
        this.operations = new CollectionOperations();

        automatic();
    }


    private void automatic(){
        for (Class< ? > entity : mapper.getInitialEntitySet()) {
            if (mapper.isAutoIndexCreation()) {
                lock.lock();
                try {
                    ensureIndexes(entity);
                } finally {
                    lock.unlock();
                }
            }
        }
    }


    public MongoClient getMongoClient() {
        return this.mongoClient;
    }

    public MongoDatabase getDatabase() {
        return super.database;
    }

    public String getDatabaseName(){
        return getDatabase().getName();
    }

    public MongoDatabase getDatabase( String databaseName ) {
        MongoNamespace.checkDatabaseNameValidity(databaseName);
        return this.mongoClient.getDatabase(databaseName).withCodecRegistry(super.mapper.getCodecRegistry());
    }

    protected DatastoreImpl operations( DatastoreOperations operations ) {
        this.operations = operations;
        return this;
    }


    @Override
    public MongoCollection< Document > getCollection( String collectionName ) {
        MongoNamespace.checkCollectionNameValidity(collectionName);
        return this.database.getCollection(collectionName);
    }

    @Override
    public < T > boolean collectionExists( Class< T > entityClass ) {
        Assert.notNull(entityClass, "Class must not be null");
        return collectionExists(getCollectionName(entityClass));
    }

    @Override
    public boolean collectionExists( String collectionName ) {
        Assert.notNull(collectionName, "CollectionName must not be null");
        for (String name : this.getDatabase().listCollectionNames()) {
            if (collectionName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public < T > MongoCollection< T > getCollection( Class< T > type ) {
        Assert.notNull(type, "type can not be null");

        String nameCache = null;
        if ((nameCache = collectionNameCache.get(type)) != null) {
            MongoCollection< T > collection = this.database.getCollection(nameCache, type);
            this.withConcern(collection, type);
            return collection;
        }

        EntityModel entityModel = mapper.getEntityModel(type);

        String collectionName = entityModel.getCollectionName();

        collectionNameCache.put(type, collectionName);

        MongoCollection< T > collection = this.database.getCollection(collectionName, type);

        this.withConcern(collection, type);
        return collection;
    }


    public < T > MongoCollection< T > getCollection( Class< T > type, String collectionName ) {
        if (type ==null) {
            type = (Class< T >) Document.class;
        }
        //集合名不为空，则有优先使用集合名
        if (collectionName != null) {

            MongoCollection< T > collection = this.database.getCollection(collectionName, type);

            return this.withConcern(collection, type);
        } else {
            return getCollection(type);
        }

    }


    @Override
    public Document executeCommand( String jsonCommand ) {
        Assert.notNull(jsonCommand);
        return this.database.runCommand(Document.parse(jsonCommand));
    }

    @Override
    public Document executeCommand( Document command ) {
        Assert.notNull(command);
        return this.database.runCommand(command);
    }

    @Override
    public Document executeCommand( Document command, ReadPreference readPreference ) {
        Assert.notNull(command);
        return this.database.runCommand(command, readPreference);
    }

    @Override
    public boolean exists( Query query, Class< ? > entityClass, String collectionName ) {

        Assert.notNull(query, "Query can not  be null");
        hasText(collectionName, "CollectionName passed in to exist can't be null");

        Optional< ? > o = doFindOne(query, entityClass, collectionName);
        return o.isPresent();
    }


    @Override
    public < T > UpdateResult replace( Query query, T replacement, ReplaceOptions options, String collectionName ) {

        Assert.notNull(query, "Query must not be null");
        hasText(collectionName, "Collection name must not be null or empty");
        Assert.notNull(replacement, "Replacement must not be null!");
        Assert.notNull(options, "Options must not be null Use ReplaceOptions#new() instead");

        MongoCollection< T > collection = this.getCollection((Class< T >) replacement.getClass(), collectionName);

        if (query.getHint() != null) {

            String hint = query.getHint();
            try {
                Document parse = Document.parse(hint);
                options.hint(parse);
            } catch (Exception e) {
                options.hintString(hint);
            }
        }

        if (query.getCollation().isPresent()) {
            com.mongodb.client.model.Collation collation = query.getCollation().get();
            options.collation(collation);
        }
        Meta meta = query.getMeta();
        if (meta.hasValues()) {

            if (StrUtil.hasText(meta.getComment())) {
                options.comment(meta.getComment());
            }

        }


        collection = query.getWriteConcern() == null ? this.withConcern(collection, replacement.getClass()) : this.withConcern(collection, replacement.getClass()).withWriteConcern(query.getWriteConcern());

        //  todo  优化  这块的 query  相关 需要优化
        return this.operations.replaceOne(collection, replacement, query.getQueryObject(), options);
        //return collection.replaceOne(query.getQueryObject(), replacement, options);

    }

    //根据_id进行删除
    @Override
    public com.mongodb.client.result.DeleteResult delete( Object object, String collectionName ) {
        Assert.notNull(object, "object can not be null");

        Object id = this.mapper.getId(object);
        Assert.notNull(id, "Object must has _id");

        Query query = Query.query(Criteria.where("_id").is(id));


        return doDelete(query, object.getClass(), collectionName, new DeleteOptions(), false);
    }

    @Override
    public com.mongodb.client.result.DeleteResult delete( Query query, Class< ? > entityClass, String collectionName ) {
        return doDelete(query, entityClass, collectionName, new com.mongodb.client.model.DeleteOptions(), false);
    }

    @Override
    public com.mongodb.client.result.DeleteResult deleteMulti( Query query, Class< ? > entityClass, String collectionName ) {
        return doDelete(query, entityClass, collectionName, new com.mongodb.client.model.DeleteOptions(), true);
    }


    @SuppressWarnings("ConstantConditions")
    protected < T > com.mongodb.client.result.DeleteResult doDelete( Query query, @Nullable Class< T > entityClass, String collectionName, com.mongodb.client.model.DeleteOptions options,
                                                                     boolean multi ) {
        Assert.notNull(query, "Query must not be null");
        hasText(collectionName, "Collection name must not be null or empty");

        if (query.getHint() != null) {
            String hint = query.getHint();
            try {
                Document parse = Document.parse(hint);
                options.hint(parse);
            } catch (Exception e) {
                options.hintString(hint);
            }
        }

        if (query.getCollation().isPresent()) {
            options.collation(query.getCollation().get());
        }
        Meta meta = query.getMeta();
        if (meta.hasValues()) {
            if (StrUtil.hasText(meta.getComment())) {
                options.comment(meta.getComment());
            }
        }
        Document removeQuery = query.getQueryObject();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Remove using query: %s in collection: %s.", removeQuery.toJson(),
                    collectionName));
        }

        MongoCollection< T > collection = this.getCollection(entityClass, collectionName);

        // 如果对要删除的记录数有限制，就根据_id进行删除
        //先查询要删除的文档的_id 然后根据id去删除对应文档
        if (query.getLimit() > 0 || query.getSkip() > 0) {
            MongoCursor< T > cursor = collection.find(query.getQueryObject()).cursor();
            Set< Object > ids = new LinkedHashSet<>();
            while (cursor.hasNext()) {
                ids.add(Document.parse(cursor.next().toString()).get("_id"));
            }
            removeQuery = new Document("_id", new Document("$in", ids));
        }
        //获取写偏好
        WriteConcern writeConcernToUse = query.getWriteConcern();
        if (writeConcernToUse!=null) {
            collection.withWriteConcern(writeConcernToUse);
        }


        return multi ? operations.deleteMany(collection, removeQuery, options) : operations.deleteOne(collection, removeQuery, options);
       /* return multi ? collection.deleteMany(removeQuery, options)
                : collection.deleteOne(removeQuery, options);*/


    }

    protected < T > FindIterable< T > doFind( Query query, @Nullable Class< T > entityClass, String collectionName ) {
        Assert.notNull(query, "Query must not be null");
        hasText(collectionName, "CollectionName must not be null or empty");

        MongoCollection< T > collection = this.getCollection(entityClass, collectionName);


        collection = query.getReadConcern() == null ? collection : collection.withReadConcern(query.getReadConcern());

        collection = query.getReadPreference() == null ? collection : collection.withReadPreference(query.getReadPreference());

        FindIterable< T > findIterable = this.operations.find(collection, query.getQueryObject());

        //FindIterable< T > findIterable = collection.find(session, query.getQueryObject());

        if (!query.getFieldsObject().isEmpty()) {
            findIterable = findIterable.projection(query.getFieldsObject());
        }

        if (StrUtil.hasText(query.getHint())) {
            findIterable = findIterable.hint(Document.parse(query.getHint()));
        }

        if (!query.getSortObject().isEmpty()) {
            findIterable = findIterable.sort(query.getSortObject());
        }

        if ( query.getSkip() > 0) {
            findIterable = findIterable.skip((int) (query.getSkip()));
        }

        if ( query.getLimit() > 0) {
            findIterable = findIterable.limit(query.getLimit());
        }

        if(query.getCollation().isPresent()){
            findIterable =  findIterable.collation(query.getCollation().get());
        }

        if (query.getMeta() !=null) {
            Meta meta = query.getMeta();
            if (StrUtil.isEmptyIfStr(meta.getComment())) {
                findIterable = findIterable.comment(meta.getComment());
            }
            if (meta.getAllowDiskUse() !=null  && meta.getAllowDiskUse()) {
                findIterable = findIterable.allowDiskUse(meta.getAllowDiskUse());
            }
            if (meta.getCursorBatchSize() !=null  && meta.getCursorBatchSize()  >0) {
                findIterable = findIterable.batchSize(meta.getCursorBatchSize());
            }
            if (meta.getMaxTimeMsec()!=null  &&meta.getMaxTimeMsec() >0) {
                findIterable = findIterable.maxAwaitTime(query.getMeta().getMaxTimeMsec(), TimeUnit.MICROSECONDS);
            }

            for (Meta.CursorOption option : query.getMeta().getFlags()) {
                switch (option) {
                    case NO_TIMEOUT:
                        findIterable = findIterable.noCursorTimeout(true);
                        break;
                    case PARTIAL:
                        findIterable = findIterable.partial(true);
                        break;
                    case SECONDARY_READS:
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("%s is no supported flag.", option));
                }
            }
        }

        return findIterable;
        //MongoCursor< T > iterator = findIterable.iterator();
        //return new QueryCursor<>(iterator);
    }

    @Override
    public < T > QueryCursor< T > find( Query query, Class< T > entityClass, String collectionName ) {
        QueryCursor< T > cursor = new QueryCursor<>(doFind(query, entityClass, collectionName).iterator());
        return cursor;
    }

    @Override
    public < T > Optional< T > findOne( Query query, @Nullable Class< T > entityClass, String collectionName ) {
        if (query.isSorted()) {
            return doFindOne(query, entityClass, collectionName);
        } else {
            MongoCursor< T > iterator = doFind(query, entityClass, collectionName).limit(1).iterator();
            QueryCursor< T > cursor = new QueryCursor<>(iterator);
            if (cursor.hasNext()) {
                return Optional.of(cursor.next());
            }
            return Optional.empty();
        }
    }

    public < T > Optional< T > findById( Object id, Class< T > entityClass, String collectionName ) {
        Assert.notNull(id, "Id must not be null");
        Assert.notNull(collectionName, "CollectionName must not be null");
        return doFindOne(Query.query(Criteria.where("_id").is(id)), entityClass, collectionName);
    }

    //    @Override
    private < T > T doInsertOne( T entity, InsertOneOptions options, String collectionName ) {
        Assert.notNull(entity, "entity must not be null");
        hasText(collectionName, "collectionName must not be null");



        //根据传入的集合名和实体类获取对应的MongoCollection对象
        MongoCollection< T > collection = this.getCollection((Class< T >) entity.getClass(), collectionName);

        collection = prepareConcern(collection, options);
        this.operations.insertOne(collection, entity, options.getOriginOptions());
        //InsertOneResult result = insertOneExecute(session, collection, options, entity);

        return entity;
    }

    @Override
    public < T > List< T > insert( List< T > batchToSave, Class< ? > entityClass ) {
        String collectionName = this.mapper.determineCollectionName(entityClass, null);
        return doInsertMany(batchToSave, new InsertManyOptions(), collectionName);
    }

    @Override
    public < T > List< T > insert( List< T > batchToSave, String collectionName ) {
        return doInsertMany(batchToSave, new InsertManyOptions(), collectionName);
    }

    @Override
    public < T > List< T > insertAll( List< T > entities ) {
        List< T > collection = new ArrayList<>();
        for (T t : entities) {
            String collectionName = this.getCollectionName(t.getClass());
            T insert = this.doInsertOne(t, new InsertOneOptions(), collectionName);
            collection.add(insert);
        }
        return collection;
    }

    @Override
    public < T > List< T > insert( List< T > entities, Class< ? > entityClass, InsertManyOptions options ) {
        String collectionName = this.mapper.determineCollectionName(entityClass, null);
        return this.doInsertMany(entities, options, collectionName);
    }

    @Override
    public < T > List< T > insert( List< T > entities, String collectionName, InsertManyOptions options ) {
        return doInsertMany(entities, options, collectionName);
    }

    @Override
    public < T > T insert( T objectToSave, String collectionName ) {
        return doInsertOne(objectToSave, new InsertOneOptions(), collectionName);
    }


    private < T > List< T > doInsertMany( List< T > entities, InsertManyOptions options, String collectionName ) {

        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("entities in operation can't be null or empty ");
        }


        Class< T > type = null;

        for (T entity : entities) {

            type = (Class< T >) entity.getClass();

            break;
        }

        MongoCollection< T > collection = this.getCollection(type, collectionName);

        collection = prepareConcern(collection, options);

        this.operations.insertMany(collection, entities, options.getOriginOptions());


        return entities;

        //return (Collection< T >) insertManyExecute(session, collection, options, entities);

    }

    @Override
    public < T > UpdateResult updateEntity( Query query, T entity, UpdateOptions options, String collectionName ) {

        Document entityDoc = this.toDocument(entity);
        if (entityDoc == null) {
            throw new IllegalArgumentException();
        }
        entityDoc = new Document("$set",entityDoc);

        MongoCollection< ? > collection = this.getCollection(entity.getClass(), collectionName);

        collection = prepareConcern(collection, options);

        //UpdateResult result = updateExecute(session, collection, query, options, entityDoc);

        return options.isMulti() ? this.operations.updateMany(collection, query.getQueryObject(), entityDoc, options.getOriginOptions())
                : this.operations.updateOne(collection, query.getQueryObject(), entityDoc, options.getOriginOptions());


        // return result;

    }

    /**
     * update的另一种形式
     */
    @Override
    public < T > UpdateResult update( Query query, UpdateDefinition update, Class< T > entityClass, UpdateOptions options, String collectionName ) {

        //update中的arrayList
        List< UpdateDefinition.ArrayFilter > arrayFilters = update.getArrayFilters();
        //option中的arrayList
        List< UpdateDefinition.ArrayFilter > arrayFilters1 = options.getArrayFilters();
        //最终的arraylist
        List< Document > arrayFilterList = new ArrayList<>();
        if (ListUtil.isNotEmpty(arrayFilters)) {
            for (int i = 0; i < arrayFilters.size(); i++) {
                Document document = arrayFilters.get(i).toData();
                arrayFilterList.add(document);
            }
            if (ListUtil.isNotEmpty(arrayFilters1)) {
                //保存arrayFilter
                for (int i = 0; i < arrayFilters1.size(); i++) {
                    Document document = arrayFilters1.get(i).toData();
                    arrayFilterList.add(document);
                }
            }
/*
            // todo
            // option 本身 是否包含相关  arrayFilters
            // 是否冲突
            //判断UpdateOption本身是否包含相关的arrayFilters
            List<Document> arrayFiltersOption = (List<Document>) options.getOriginOptions().getArrayFilters();
            //如果update的arrayFilter没有包含option的arrayFilter所有条件，就合并两个arrayFilter
            if(!arrayFilterList.containsAll(arrayFiltersOption)){
                arrayFilterList.addAll(arrayFiltersOption);
            }
*/
            options.arrayFilters(arrayFilterList);

        }


        MongoCollection< T > collection = this.getCollection(entityClass, collectionName);
        collection = prepareConcern(collection, options);

        if (update instanceof UpdatePipeline) {
            //todo
            // 针对所有的 UpdateDefinition
            //getUpdateObject () 方法 在这里都需要处理
            throw  new UnsupportedOperationException("UpdatePipeline  is not supported  in current version  ") ;
            //return null;
        } else {


            UpdateResult result = updateDefinitionExecute( collection, query,options, update.getUpdateObject());

            return result;
        }

    }


    @Override
    public UpdateResult upsert( Query query, UpdateDefinition update, Class< ? > entityClass ) {
        return doUpdate(getCollectionName(entityClass), query, update, entityClass, true, false);
    }

    @Override
    public UpdateResult upsert( Query query, UpdateDefinition update, String collectionName ) {
        return doUpdate(collectionName, query, update, null, true, false);
    }

    @Override
    public UpdateResult upsert( Query query, UpdateDefinition update, Class< ? > entityClass, String collectionName ) {

        Assert.notNull(entityClass, "EntityClass must not be null");

        return doUpdate(collectionName, query, update, entityClass, true, false);
    }

    @Override
    public UpdateResult updateFirst( Query query, UpdateDefinition update, Class< ? > entityClass ) {
        return doUpdate(getCollectionName(entityClass), query, update, entityClass, false, false);
    }

    @Override
    public UpdateResult updateFirst( Query query, UpdateDefinition update, String collectionName ) {
        return doUpdate(collectionName, query, update, null, false, false);
    }

    @Override
    public UpdateResult updateFirst( Query query, UpdateDefinition update, Class< ? > entityClass, String collectionName ) {

        Assert.notNull(entityClass, "EntityClass must not be null");

        return doUpdate(collectionName, query, update, entityClass, false, false);
    }

    @Override
    public UpdateResult updateMulti( Query query, UpdateDefinition update, Class< ? > entityClass ) {
        return doUpdate(getCollectionName(entityClass), query, update, entityClass, false, true);
    }

    @Override
    public UpdateResult updateMulti( Query query, UpdateDefinition update, String collectionName ) {
        return doUpdate(collectionName, query, update, null, false, true);
    }

    @Override
    public UpdateResult updateMulti( Query query, UpdateDefinition update, Class< ? > entityClass, String collectionName ) {

        Assert.notNull(entityClass, "EntityClass must not be null");

        return doUpdate(collectionName, query, update, entityClass, false, true);
    }

    @Override
    public < T > List< T > save( List< T > entities ) {
        List< T > list = new ArrayList<>();
        for (T entity : entities) {
            String collectionName = this.getCollectionName(entity.getClass());
            T t = doSave(entity, new InsertOneOptions(), collectionName);
            list.add(t);
        }
        return list;
    }

    @Override
    public < T > List< T > save( List< T > entities, String collectionName ) {
        List< T > list = new ArrayList<>();
        for (T entity : entities) {
            T t = doSave(entity, new InsertOneOptions(), collectionName);
            list.add(t);
        }
        return list;
    }

    @Override
    public < T > T save( T entity, String collectionName ) {
        return doSave(entity, new InsertOneOptions(), collectionName);
    }

    @SuppressWarnings("ConstantConditions")
    protected < T > UpdateResult doUpdate( String collectionName, Query query, UpdateDefinition update,
                                           @Nullable Class< ? > entityClass, boolean upsert, boolean multi ) {

        Assert.notNull(collectionName, "CollectionName must not be null");
        Assert.notNull(query, "Query must not be null");
        Assert.notNull(update, "Update must not be null");

        if (query.isSorted() && LOGGER.isWarnEnabled()) {

            LOGGER.warn(String.format("%s does not support sort ('%s'); Please use findAndModify() instead",
                    upsert ? "Upsert" : "UpdateFirst", query.getSortObject().toJson()));
        }

        com.mongodb.client.model.UpdateOptions opts = new com.mongodb.client.model.UpdateOptions();

        opts.upsert(upsert);
        if (query.getCollation().isPresent()) {

            opts.collation(query.getCollation().get());

        }

        if (query.getHint() != null) {
            String hint = query.getHint();

            try {
                Document parse = Document.parse(hint);
                opts.hint(parse);
            } catch (Exception e) {
                opts.hintString(hint);
            }
        }


        if (query.getMeta().getComment() != null) {
            opts.comment(query.getMeta().getComment());
        }

        if (update.hasArrayFilters()) {
            opts.arrayFilters(update.getArrayFilters().stream().map(x -> x.toData()).collect(Collectors.toList()));
        }

        MongoCollection< ? > collection = entityClass == null ? getDatabase().getCollection(collectionName, Document.class) : withConcern(getDatabase().getCollection(collectionName, entityClass), entityClass);


        com.mongodb.client.result.UpdateResult result = multi ? collection.updateMany(query.getQueryObject(), update.getUpdateObject(), opts) : collection.updateOne(query.getQueryObject(), update.getUpdateObject(), opts);

        return result;

    }


    @Override
    public MarsSessionImpl startSession() {
        return new MarsSessionImpl(this, mongoClient.startSession());
    }


    @Override
    public MarsSessionImpl startSession( ClientSessionOptions options ) {

        return new MarsSessionImpl(this, mongoClient.startSession(options));
    }

    @Override
    public MongoMappingContext getMapper() {
        return mapper;
    }

    @Override
    @Deprecated
    public < T > T findAndModify( Query query, UpdateDefinition update, FindOneAndUpdateOptions options, Class< T > entityClass, String collectionName ) {
        Assert.notNull(query, "Query must not be null!");
        Assert.notNull(update, "Update must not be null!");
        Assert.notNull(options, "Options must not be null!");
        Assert.notNull(entityClass, "EntityClass must not be null!");
        Assert.notNull(collectionName, "CollectionName must not be null!");


        //  do with collation
        if(query.getCollation().isPresent() && options.getCollation().isPresent()){
            throw new IllegalArgumentException(
                    "Both Query and FindOneAndModifyOptions define a collation. Please provide the collation only via one of the two.");
        };


        return doFindAndModify(collectionName, query, entityClass, update, options);
    }

    @Override
    @Deprecated
    public < S, T > T findAndReplace( Query query, S replacement, FindOneAndReplaceOptions options, Class< S > entityType, String collectionName, Class< T > resultType ) {
        Assert.notNull(query, "Query must not be null!");
        Assert.notNull(replacement, "Replacement must not be null!");
        Assert.notNull(options, "Options must not be null! Use FindOneAndReplaceOptions#new() instead.");
        Assert.notNull(entityType, "EntityType must not be null!");
        Assert.notNull(collectionName, "CollectionName must not be null!");
        Assert.notNull(resultType, "ResultType must not be null! Use Object.class instead.");

        //  sort  skip 已经不重要了 可以呗忽略了
        isTrue(query.getLimit() <= 1, "Query must not define a limit other than 1 ore none!");
        isTrue(query.getSkip() <= 0, "Query must not define skip.");




        if (entityType == resultType) {
            //S oneAndReplace = this.database.getCollection(collectionName, entityType).findOneAndReplace(marsSession, query.getQueryObject(), replacement, options.getOriginOptions());

            MongoCollection< S > collection = this.database.getCollection(collectionName,entityType);

            return  (T) this.operations.findOneAndReplace(collection, query.getQueryObject(), replacement, options.getOriginOptions());

        }

        Document replacemenDocument = this.toDocument(replacement);

        MongoCollection< Document > collection = this.database.getCollection(collectionName);

        Document oneAndReplace = this.operations.findOneAndReplace(collection, query.getQueryObject(), replacemenDocument, options.getOriginOptions());

        if(oneAndReplace == null){

            return null ;
        }else{
            T t = this.mapper.fromDocument(resultType, oneAndReplace);

            return t ;
        }


        //Document oneAndReplace = this.database.getCollection(collectionName).findOneAndReplace(marsSession, query.getQueryObject(), document, options.getOriginOptions());
       /* if (oneAndReplace == null) {
            return null;
        }*/

        //T first = this.database.getCollection(collectionName, resultType).find(marsSession, new Document("_id", oneAndReplace.get("_id"))).limit(1).first();

        //return first;


    }

    @Override
    @Deprecated
    public < T > T findAndDelete( Query query, Class< T > entityClass, String collectionName, FindOneAndDeleteOptions options ) {
        Assert.notNull(query, "Query must not be null!");
        Assert.notNull(entityClass, "EntityClass must not be null!");
        Assert.notNull(collectionName, "CollectionName must not be null!");

        MongoCollection< T > collection = this.database.getCollection(collectionName, entityClass);
        T oneAndDelete = this.operations.findOneAndDelete(collection, query.getQueryObject(), options.getOriginOptions());

        return oneAndDelete;
    }


    @Override
    public String getCollectionName( Class< ? > entityClass ) {

        return this.mapper.getEntityModel(entityClass).getCollectionName();

    }


    private < T > T doFindAndModify( String collectionName, Query query, Class< T > entityClass, UpdateDefinition update, FindOneAndUpdateOptions optons ) {
        MongoCollection< T > collection = getCollection(entityClass, collectionName);

        Document updateObject = update.getUpdateObject();

        T oneAndUpdate = this.operations.findOneAndUpdate(collection, query.getQueryObject(), updateObject, optons.getOriginOptions());

        return oneAndUpdate ;

    }


    //索引操作
    @Override
    public void createIndex( Index index, String collectionName ) {

        //ClientSession session = this.startSession();

        MongoCollection collection = database.getCollection(collectionName);

//        CrudExecutor crudExecutor = CrudExecutorFactory.create(CrudEnum.INDEX_CREATE_ONE);


        //createIndexExecute( collection,  index.getIndexOptions(), index);
        this.operations.createIndex(collection , new IndexModel(index.getIndexKeys(),index.getIndexOptions()));

    }


    @Override
    public < T > void ensureIndexes( Class< T > entityClass, String collectionName ) {

        final IndexHelper indexHelper = new IndexHelper(this.mapper);
        String collName = this.mapper.determineCollectionName(entityClass, collectionName);
        indexHelper.createIndex(this.database.getCollection(collName), this.mapper.getEntityModel(entityClass));

    }

    @Override
    public void dropIndex( Index index, String collectionName ) {

        //ClientSession session = this.startSession();

        MongoCollection collection = database.getCollection(collectionName);

//        CrudExecutor crudExecutor = CrudExecutorFactory.create(CrudEnum.INDEX_DROP_ONE);

        //dropOneIndexExecute(collection,  index);

        collection.dropIndex(index.getIndexKeys());

        this.operations.dropIndex(collection,index.getIndexKeys());

    }

    @Override
    public void dropIndexes( String collectionName ) {

        //ClientSession session = this.startSession();

        MongoCollection collection = database.getCollection(collectionName);

//        CrudExecutor crudExecutor = CrudExecutorFactory.create(CrudEnum.INDEX_DROP_MANY);

        //dropManyIndexExecute(session, collection, null, null, null);

        //collection.dropIndexes();
        this.operations.dropIndexes(collection);

    }

    @Override
    public List< Index > getIndexes( String collectionName ) {

        //ClientSession session = this.startSession();

        MongoCollection collection = database.getCollection(collectionName);

//        CrudExecutor crudExecutor = CrudExecutorFactory.create(CrudEnum.INDEX_FIND);

        //List< Index > execute = findManyIndexExecute(session, collection, null, null, null);

        //session.close();
        MongoCursor<Document> iterator = this.operations.getIndexes(collection).iterator();

        List<Index>  indexes = new ArrayList<>();
        while (iterator.hasNext()){
            indexes.add(IndexUtil.of(iterator.next()));
        }
        return indexes;

    }


    /**
     * 将Collections<T> 转成 List<Entity>
     */
    public < T > List< Document > toDocuments( Collection< ? extends T > entities ) {

        if (entities.isEmpty()) {
            return new ArrayList< Document >();
        }

        return entities.stream().map(x -> mapper.toDocument(x)).collect(Collectors.toList());

    }


    private < T > MongoCollection< T > prepareConcern( MongoCollection< T > collection, Options options ) {
        return options.prepare(collection);

    }

    @Override
    public void setWriteConcern( WriteConcern writeConcern ) {
        super.setWriteConcern(writeConcern);
        this.database = database.withWriteConcern(writeConcern);
    }

    @Override
    public void setReadConcern( ReadConcern readConcern ) {
        super.setReadConcern(readConcern);
        this.database = database.withReadConcern(readConcern);
    }

    @Override
    public void setReadPreference( ReadPreference readPerference ) {
        super.setReadPreference(readPerference);
        this.database = database.withReadPreference(readPerference);
    }

    /**
     * save与insert是否有异
     * insert会抛异常
     * save若新增数据的主键已经存在，则会对当前已经存在的数据进行修改操作
     * 这里需要修改
     * function(obj, opts) {
     * if (obj == null)
     * throw Error("can't save a null");
     * <p>
     * if (typeof (obj) == "number" || typeof (obj) == "string")
     * throw Error("can't save a number or string");
     * <p>
     * if (typeof (obj._id) == "undefined") {
     * obj._id = new ObjectId();
     * return this.insert(obj, opts);
     * } else {
     * return this.update({_id: obj._id}, obj, Object.merge({upsert: true}, opts));
     * }
     * }
     */


    private < T > T doSave( T entity, InsertOneOptions options, String collectionName ) {

        if (entity == null) {
            return null;
        }


        final EntityModel model = this.mapper.getEntityModel(entity.getClass());
        final PropertyModel idField = model.getIdProperty();
        //如果对象内部属性中含有_id字段且_id 字段有值，则执行save操作，没有则执行insert
        if (idField != null && idField.getPropertyAccessor().get(entity) != null) {
            //  调用replace
            ReplaceOptions replaceOptiion = new ReplaceOptions()
                    .bypassDocumentValidation(options.getBypassDocumentValidation())
                    .upsert(true);

            Document document = this.toDocument(entity);

            replace(new Query(Criteria.where("_id").is(document.get("_id"))), entity, replaceOptiion, collectionName);
        } else {

            Document document = this.toDocument(entity);
            if(document.containsKey("_id")){
                ReplaceOptions replaceOptiion = new ReplaceOptions()
                        .bypassDocumentValidation(options.getBypassDocumentValidation())
                        .upsert(true);
                replace(new Query(Criteria.where("_id").is(document.get("_id"))), entity, replaceOptiion, collectionName);

            }else {

                doInsertOne(entity, options, collectionName);
            }

        }

        return entity;

    }


    @Override
    public < T > T storeGridFs( GridFsObject< T, InputStream > upload, String bucketName ) {

        GridFSUploadOptions uploadOptions = computeUploadOptionsFor(upload.getOptions().getContentType(),
                upload.getOptions().getMetadata());

        if (upload.getOptions().getChunkSize() > 0) {
            uploadOptions.chunkSizeBytes(upload.getOptions().getChunkSize());
        }

        if (upload.getFileId() == null) {
            return (T) getGridFsBucket(bucketName).uploadFromStream(upload.getFilename(), upload.getContent(), uploadOptions);
        }

        getGridFsBucket(bucketName).uploadFromStream(BsonUtil.simpleToBsonValue(upload.getFileId()), upload.getFilename(),
                upload.getContent(), uploadOptions);

        return upload.getFileId();
    }

    @Override
    public ObjectId storeGridFs( InputStream content, String filename, String contentType,
                                 Object metadata, String bucketName ) {
        return storeGridFs(content, filename, contentType, metadata, bucketName);
    }

    @Override
    public GridFSFile findOneGridFs( Query query, String bucketName ) {
        return findGridFs(query, bucketName).first();
    }

    @Override
    public void rename( ObjectId id, String newFilename, String bucketName ) {

        getGridFsBucket(bucketName).rename(id, newFilename);

    }

    @Override
    public GridFSFindIterable findGridFs( Query query, String bucketName ) {

        Assert.notNull(query, "Query must not be null!");

        Document queryObject = query.getQueryObject();
        Document sortObject = query.getSortObject();

        GridFSFindIterable iterable = getGridFsBucket(bucketName).find(queryObject).sort(sortObject);
        if (query.getSkip() > 0) {
            iterable = iterable.skip(Math.toIntExact(query.getSkip()));
        }

        if (query.getLimit() > 0) {
            iterable = iterable.limit(query.getLimit());
        }

        MongoCursor< GridFSFile > iterator = iterable.iterator();
        Assert.notNull(iterator.tryNext(), "No file found with the query");

        return iterable;
    }


    @Override
    public void deleteGridFs( ObjectId id, String bucketName ) {

        getGridFsBucket(bucketName).delete(id);

    }


    @Override
    public ClassLoader getClassLoader() {
        return database.getClass().getClassLoader();
    }

    @Override
    public void deleteGridFs( Query query, String bucketName ) {

        GridFSFindIterable iterable = findGridFs(query);
        MongoCursor< GridFSFile > iterator = iterable.iterator();
        Assert.notNull(iterator.tryNext(), "no file found to delete");
        while (iterator.hasNext()) {
            getGridFsBucket(bucketName).delete(iterator.next().getObjectId());
        }

    }


    private GridFSBucket getGridFsBucket( String bucketName ) {
        return bucketName == null ? defaultGridFSBucket : GridFSBuckets.create(database, bucketName);
    }

    @Override
    public GridFsResource getResource( GridFSFile file, String bucketName ) {

        Assert.notNull(file, "GridFSFile must not be null!");

        return new GridFsResource(file, getGridFsBucket(bucketName).openDownloadStream(file.getId()));
    }


    public Document toDocument( Object entity ) {
        final EntityModel entityModel = this.mapper.getEntityModel(entity.getClass());

        DocumentWriter writer = new DocumentWriter(mapper);
        this.mapper.getCodecRegistry().get(entityModel.getType())
                .encode(writer, entity, EncoderContext.builder().build());

        return writer.getDocument();
    }


    /**
     *
     * @param clazz
     * @param <T>
     * @return
     */
    @Override
    public < T > long estimatedCount( Class< T > clazz ) {

        String collectionName = this.mapper.determineCollectionName(clazz, null);
        if (LOGGER.isDebugEnabled()) {

            LOGGER.debug(String.format("Executing count: %s in collection %s ", "{}", collectionName));
        }
        return this.database.getCollection(collectionName).estimatedDocumentCount();

    }

    @Override
    public long count( Query query, Class< ? > entityClass, String collectionName ) {

        String s = this.mapper.determineCollectionName(entityClass, collectionName);

        return count(query, s);
    }


    @Override
    public < T > long count( Query query, Class< T > clazz ) {

        com.mongodb.client.model.CountOptions countOptions = decorateCountOption(query);

        String collectionName = this.mapper.determineCollectionName(clazz, null);
        if (LOGGER.isDebugEnabled()) {

            LOGGER.debug(String.format("Executing count: %s in collection %s ", query.getQueryObject().toJson(), collectionName));
        }

        return this.operations.countDocuments(this.database.getCollection(collectionName),query.getQueryObject(),countOptions);


        //return this.database.getCollection(collectionName).countDocuments(query.getQueryObject(), countOptions);
    }


    @Override
    public < T > long count( MarsSession session, Query query, Class< T > entityClass ) {
        return count(session, query, entityClass, null);
    }

    @Override
    public < T > long count( MarsSession session, Query query, String collectionName ) {
        return count(session, query, null, collectionName);
    }

    @Override
    public long count( MarsSession session, Query query, Class< ? > entityClass, String collectionName ) {
        String s = this.mapper.determineCollectionName(entityClass, collectionName);
        com.mongodb.client.model.CountOptions countOptions = decorateCountOption(query);
        return this.database.getCollection(s).countDocuments(session, query.getQueryObject(), countOptions);
    }

    @Override
    public < T > long estimatedCount( String collectionName ) {


        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Executing estimatedCount: %s in collection: %s", "{}", collectionName));
        }

        return this.database.getCollection(collectionName).estimatedDocumentCount(new com.mongodb.client.model.EstimatedDocumentCountOptions());
    }

    @Override
    public < T > long count( Query query, String collectionName ) {
        com.mongodb.client.model.CountOptions countOptions = decorateCountOption(query);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Executing count: %s in collection: {}", query.getQueryObject().toJson(), collectionName));
        }
        return this.database.getCollection(collectionName).countDocuments(query.getQueryObject(), countOptions);
    }


    //只传入实体类的方式，根据实体类的注解获取CollectionOptions
    @Override
    public < T > MongoCollection< Document > createCollection( Class< T > entityClass ) {
        Assert.notNull(entityClass, "EntityClass must not be null!");

        return createCollection(entityClass, scanEntityCollectionOptions(entityClass));
    }

    private GridFSUploadOptions computeUploadOptionsFor( String contentType, Document metadata ) {

        Document targetMetadata = new Document();

        if (StrUtil.hasText(contentType)) {
            targetMetadata.put(GridFsResource.CONTENT_TYPE_FIELD, contentType);
        }

        if (metadata != null) {
            targetMetadata.putAll(metadata);
        }

        GridFSUploadOptions options = new GridFSUploadOptions();
        options.metadata(targetMetadata);

        return options;
    }

    /**
     * 创建集合
     * 根据类名和传递的集合参数名，找出最终的集合名称，并且将类上面的注解声明的内容放入 CollectionOptions 中
     *
     * @param entityClass       实体类
     * @param collectionOptions 收集选项
     * @return {@link MongoCollection<Document>}
     */
    @Override
    public < T > MongoCollection< Document > createCollection( Class< T > entityClass,
                                                               CreateCollectionOptions collectionOptions ) {

        Assert.notNull(entityClass, "EntityClass must not be null!");

        //CreateCollectionOptions options = collectionOptions != null ? collectionOptions : new CreateCollectionOptions();

        //Document document = convertToDocument(options);

        return doCreateCollection(this.mapper.getEntityModel(entityClass).getCollectionName(), collectionOptions);
    }

    @Override
    public MongoCollection< Document > createCollection( String collectionName ) {
        Assert.notNull(collectionName, "CollectionName must not be null!");

        return doCreateCollection(collectionName, new CreateCollectionOptions());
    }

    @Override
    public MongoCollection< Document > createCollection( String collectionName, CreateCollectionOptions collectionOptions ) {
        Assert.notNull(collectionName, "collectionName must not be null");
        return doCreateCollection(collectionName, collectionOptions);
    }

    @Override
    public MongoCollection< Document > createView( String name, Class< ? > source, AggregationPipeline pipeline, CreateViewOptions options ) {
        String collectionName = this.mapper.getEntityModel(source).getCollectionName();
        return doCreateView(name, collectionName, pipeline, options);

    }

    @Override
    public MongoCollection< Document > createView( String name, String source, AggregationPipeline pipeline, CreateViewOptions options ) {
        return doCreateView(name, source, pipeline, options);
    }

    public MongoCollection< Document > doCreateView( String name, String collectionName, AggregationPipeline pipeline, CreateViewOptions options ) {
        hasText(collectionName, "CollectionName can not be null");
        Assert.notNull(pipeline, "Pipeline can not be null");
        lock.lock();
        try {
            MongoDatabase database = this.getDatabase();

            MarsSession session = this.startSession();

            database.createView(session, name, collectionName, getDocuments(pipeline.getInnerStage()), options);
            return database.getCollection(name, Document.class);
        } finally {


            lock.unlock();
        }


    }

    @Override
    public Set< String > getCollectionNames() {
        MongoDatabase db = this.getDatabase();
        Set< String > result = new LinkedHashSet<>();
        for (String name : db.listCollectionNames()) {
            result.add(name);
        }
        return result;
    }

    @Override
    public < T > void dropCollection( Class< T > entityClass ) {
        dropCollection(this.mapper.getEntityModel(entityClass).getCollectionName());
    }

    @Deprecated
    public MongoCollection< Document > createCollection( String collectionName, CollectionOptions collectionOptions ) {

        Assert.notNull(collectionName, "CollectionName must not be null!");
        return doCreateCollection(collectionName, convertToDocument(collectionOptions));
    }

    @Override
    public void dropCollection( String collectionName ) {
        Assert.notNull(collectionName, "CollectionName must not be null!");
        lock.lock();
        try {
            MongoCollection< Document > collection = this.database.getCollection(collectionName);
            collection.drop();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Dropped collection %s",
                        collection.getNamespace() != null ? collection.getNamespace().getCollectionName() : collectionName));
            }
        } finally {
            lock.unlock();
        }

    }


    @Deprecated
    protected Document convertToDocument( CollectionOptions collectionOptions ) {
        Document document = new Document();
        if (collectionOptions != null) {

            collectionOptions.getCapped().ifPresent(val -> document.put("capped", val));
            collectionOptions.getSize().ifPresent(val -> document.put("size", val));
            collectionOptions.getMaxDocuments().ifPresent(val -> document.put("max", val));
            collectionOptions.getCollation().ifPresent(val -> document.append("collation", val.asDocument()));

            collectionOptions.getValidationOptions().ifPresent(it -> {
                it.getValidationLevel().ifPresent(val -> document.append("validationLevel", val.getValue()));
                it.getValidationAction().ifPresent(val -> document.append("validationAction", val.getValue()));
            });

            Optional< TimeSeriesOptions > timeSeries = collectionOptions.getTimeSeriesOptions();

            if (timeSeries.isPresent()) {
                TimeSeriesOptions timeSeriesOptions = timeSeries.get();
                Document document1 = new Document();
                if (!StrUtil.isNullOrEmpty(timeSeriesOptions.getTimeField())) {
                    document1.append("timeField", timeSeriesOptions.getTimeField());
                }

                if (!StrUtil.isNullOrEmpty(timeSeriesOptions.getMetaField())) {
                    document1.append("metaField", timeSeriesOptions.getMetaField());
                }

                if (timeSeriesOptions.getGranularity() !=null) {
                    document1.append("granularity", timeSeriesOptions.getGranularity());
                }

                if (timeSeriesOptions.getExpireAfterSeconds() > 0) {
                    document1.append("expireAfterSeconds", timeSeriesOptions.getExpireAfterSeconds());
                }
                document.append("timeseries", document1);
            }
        }
        return document;
    }


    @Deprecated
    protected Document convertToDocument( CreateCollectionOptions collectionOptions ) {
        Document document = new Document();
        if (collectionOptions != null) {
            document.put("capped", collectionOptions.isCapped());
            if (collectionOptions.getSizeInBytes() >0) {
                document.put("size", collectionOptions.getSizeInBytes());
            }
            if (collectionOptions.getMaxDocuments() >0) {
                document.put("max", collectionOptions.getMaxDocuments());
            }
            if (collectionOptions.getCollation()!=null) {
                document.put("collation", collectionOptions.getCollation());
            }

            if (collectionOptions.getValidationOptions()!=null) {
                ValidationOptions validationOptions = collectionOptions.getValidationOptions();
                if (validationOptions.getValidationLevel()!=null) {
                    document.append("validationLevel", validationOptions.getValidationLevel().getValue());
                }
                if (validationOptions.getValidationAction()!=null) {
                    document.append("validationAction", validationOptions.getValidationAction().getValue());
                }
            }

//            collectionOptions.getCapped().ifPresent(val -> document.put("capped", val));
//            collectionOptions.getSize().ifPresent(val -> document.put("size", val));
//            collectionOptions.getMaxDocuments().ifPresent(val -> document.put("max", val));
//            collectionOptions.getCollation().ifPresent(val -> document.append("collation", val.asDocument()));

//            collectionOptions.getValidationOptions().ifPresent(it -> {
//                it.getValidationLevel().ifPresent(val -> document.append("validationLevel", val.getValue()));
//                it.getValidationAction().ifPresent(val -> document.append("validationAction", val.getValue()));
//            });

            com.mongodb.client.model.TimeSeriesOptions timeSeriesOptions = collectionOptions.getTimeSeriesOptions();
            if (timeSeriesOptions!=null) {
//                TimeSeriesOptions timeSeriesOptions = timeSeries.get();
                Document document1 = new Document();
                if (!StrUtil.isNullOrEmpty(timeSeriesOptions.getTimeField())) {
                    document1.append("timeField", timeSeriesOptions.getTimeField());
                }

                if (!StrUtil.isNullOrEmpty(timeSeriesOptions.getMetaField())) {
                    document1.append("metaField", timeSeriesOptions.getMetaField());
                }

                if ((timeSeriesOptions.getGranularity()!=null)) {
                    document1.append("granularity", timeSeriesOptions.getGranularity());
                }

                //todo 这一项需要考虑
//                if (ObjectUtil.isNotEmpty(timeSeriesOptions.getExpireAfterSeconds())){
//                    document1.append("expireAfterSeconds",timeSeriesOptions.getExpireAfterSeconds());
//                }
                document.append("timeseries", document1);
            }
        }
        return document;
    }


    protected MongoCollection< Document > doCreateCollection( String collectionName, CreateCollectionOptions createcollectionOptions ) {
        lock.lock();
        MongoCollection< Document > coll ;
        try{
            this.database.createCollection(collectionName, createcollectionOptions);

            coll = database.getCollection(collectionName, Document.class);
            //如果配置文件中开启了自动创建索引，则在表创建成功后创建索引
            if (this.mapper.isAutoIndexCreation()) {
                Class< Object > entity = this.mapper.getClassFromCollection(collectionName);
                ensureIndexes(entity, coll.getNamespace().getCollectionName());
            }

        } finally {
            lock.unlock();
        }

        return coll;
    }

    protected MongoCollection< Document > doCreateCollection( String collectionName, Document collectionOptions ) {
        lock.lock();
        try {
            com.mongodb.client.model.CreateCollectionOptions co = new com.mongodb.client.model.CreateCollectionOptions();

            if (collectionOptions.containsKey("capped")) {
                co.capped((Boolean) collectionOptions.get("capped"));
            }
            if (collectionOptions.containsKey("size")) {
                co.sizeInBytes(((Number) collectionOptions.get("size")).longValue());
            }
            if (collectionOptions.containsKey("max")) {
                co.maxDocuments(((Number) collectionOptions.get("max")).longValue());
            }

            //如果选项里面有关于collation方面的操作，
            if (collectionOptions.containsKey("collation")) {
                Document source = collectionOptions.get("collation",Document.class);

                com.mongodb.client.model.Collation.Builder collation = com.mongodb.client.model.Collation.builder();




                if (source.containsKey("strength")) {
                    collation = collation.collationStrength(CollationStrength.fromInt(source.getInteger("strength")));
                }
                if (source.containsKey("caseLevel")) {
                    collation = collation.caseLevel(source.getBoolean("caseLevel"));
                }
                if (source.containsKey("caseFirst")) {
                    collation = collation.collationCaseFirst(CollationCaseFirst.fromString(source.getString("caseFirst")));
                }
                if (source.containsKey("numericOrdering")) {
                    collation = collation.numericOrdering(source.getBoolean("numericOrdering"));
                }
                if (source.containsKey("alternate")) {
                    collation = collation.collationAlternate(CollationAlternate.fromString(source.getString("alternate")));
                }
                if (source.containsKey("maxVariable")) {
                    collation = collation.collationMaxVariable(CollationMaxVariable.fromString(source.getString("maxVariable")));
                }
                if (source.containsKey("backwards")) {
                    collation = collation.backwards(source.getBoolean("backwards"));
                }
                if (source.containsKey("normalization")) {
                    collation = collation.normalization(source.getBoolean("normalization"));
                }
              /*  if (source.containsKey("version")) {
                    collation = Optional.of(source.get("version").toString());
                }*/

                co.collation(collation.build());
            }

            if (collectionOptions.containsKey("validator")) {

                com.mongodb.client.model.ValidationOptions options = new com.mongodb.client.model.ValidationOptions();

                if (collectionOptions.containsKey("validationLevel")) {
                    options.validationLevel(ValidationLevel.fromString(collectionOptions.getString("validationLevel")));
                }
                if (collectionOptions.containsKey("validationAction")) {
                    options.validationAction(ValidationAction.fromString(collectionOptions.getString("validationAction")));
                }
                options.validator(collectionOptions.get("validator", Document.class));
                co.validationOptions(options);
            }

            if (collectionOptions.containsKey("timeseries")) {
                Document timeseries = collectionOptions.get("timeseries", Document.class);

                com.mongodb.client.model.TimeSeriesOptions timeSeriesOptions = null;
                String timeField = timeseries.getString("timeField");
                String metaField = timeseries.getString("metaField");
                if (!StrUtil.isNullOrEmpty(timeField)) {
                    timeSeriesOptions = new com.mongodb.client.model.TimeSeriesOptions(timeField);
                }

                //不需要手动对这个参数进行合法性校验
                if (!StrUtil.isNullOrEmpty(metaField)) {
                    timeSeriesOptions.metaField(timeseries.getString("metaField"));
                }

                if (timeseries.get("granularity")!=null) {
                    timeSeriesOptions.granularity(timeseries.get("granularity", TimeSeriesGranularity.class));
                }

                //需要先判断是否开启TTL，如果开启了并且传入了过期时间，则指定
                if (timeseries.getLong("expireAfterSeconds")>0) {
                    co.expireAfter(timeseries.getLong("expireAfterSeconds"), TimeUnit.SECONDS);
                }
                co.timeSeriesOptions(timeSeriesOptions);

            }

            this.database.createCollection(collectionName, co);

            MongoCollection< Document > coll = database.getCollection(collectionName, Document.class);
            //如果配置文件中开启了自动创建索引，则在表创建成功后创建索引
            if (this.mapper.isAutoIndexCreation()) {
                Class< Object > entity = this.mapper.getClassFromCollection(collectionName);
                ensureIndexes(entity, coll.getNamespace().getCollectionName());
            }
            return coll;
        } finally {
            lock.unlock();
        }
    }


    public < T > MongoCollection< T > withConcern( MongoCollection< T > collection, Class< ? > clazz ) {

        EntityModel< ? > entityModel = this.mapper.getEntityModel(clazz);

        Concern annotation = entityModel.getAnnotation(Concern.class);

        //判断实体类是否有@Concern注解
        if (annotation != null) {

            if (WriteConcern.valueOf(annotation.writeConcern())!=null) {
                collection = collection.withWriteConcern(WriteConcern.valueOf(annotation.writeConcern()));
            }

            if (ReadPreference.valueOf(annotation.readPreference())!=null) {
                collection = collection.withReadPreference(ReadPreference.valueOf(annotation.readPreference()));
            }

            ReadConcernLevel readConcernLevel = ReadConcernLevel.fromString(annotation.readConcern());
            if (readConcernLevel!=null) {
                collection.withReadConcern(new ReadConcern(readConcernLevel));
            }

        }
        return collection;

    }

    private CollectionOptions getCollectionOptions( Class< ? > entity ) {

        EntityModel entityModel = this.mapper.getEntityModel(entity);
        Assert.notNull(entity, "EntityClass must not be null!");
        CollectionOptions options = CollectionOptions.empty();


        //TODO it may contains some bug，so please use it carefully.
        CappedAt capped = (CappedAt) entityModel.getAnnotation(CappedAt.class);

        if (capped !=null) {

            options = options.capped();
            options = options.size(capped.value());
            options = options.maxDocuments(capped.count());
        }


        Collation collation = (Collation) entityModel.getAnnotation(Collation.class);

        if (collation !=null) {
            com.mongodb.client.model.Collation collationOption = com.mongodb.client.model.Collation.builder()
                    .locale(collation.locale())
                    .caseLevel(collation.caseLevel())
                    .collationCaseFirst(collation.caseFirst())
                    .collationStrength(collation.strength())
                    .numericOrdering(collation.numericOrdering())
                    .collationAlternate(collation.alternate())
                    .collationMaxVariable(collation.maxVariable())
                    .backwards(collation.backwards())
                    .normalization(collation.normalization())
                    .build();

            //System.out.println(collationOption);
            options = options.collation(collationOption);

        }

        TimeSeries timeSeries = (TimeSeries) entityModel.getAnnotation(TimeSeries.class);

        if (timeSeries!=null) {
            TimeSeriesOptions toptions = TimeSeriesOptions.timeSeries(timeSeries.timeField());
            if (StrUtil.hasText(timeSeries.metaField())) {

                if (entityModel.getPropertyModel(timeSeries.metaField()) == null) {
                    throw new MarsOrmException(
                            String.format("Meta field '%s' does not exist in type %s", timeSeries.metaField(), entity.getName()));
                }

                toptions = toptions.metaField(timeSeries.metaField());
            }
            if (timeSeries.granularity()!=null) {
                toptions = toptions.granularity(timeSeries.granularity());

            }

            if (timeSeries.enableExpire()) {
                if (timeSeries.expireAfterSeconds()>0) {
                    toptions = toptions.expireAfterSeconds(timeSeries.expireAfterSeconds());
                }
            }

            options = options.timeSeries(toptions);

        }


        return options;
    }

    //todo 此方法是额外写的方法 与getCollectionOptions一致 是用来解决报错 后续可能不需要
    private CreateCollectionOptions scanEntityCollectionOptions( Class< ? > entity ) {

        EntityModel entityModel = this.mapper.getEntityModel(entity);
        Assert.notNull(entity, "EntityClass must not be null!");
        CreateCollectionOptions options = new CreateCollectionOptions();

        //TODO it may contains some bug，so please use it carefully.
        CappedAt capped = (CappedAt) entityModel.getAnnotation(CappedAt.class);

        if (capped!=null) {

            options = options.capped(true);
            options = options.sizeInBytes(capped.value());
            options = options.maxDocuments(capped.count());
        }


        Collation collation = (Collation) entityModel.getAnnotation(Collation.class);

        if (null!= collation) {
            com.mongodb.client.model.Collation collationOption = com.mongodb.client.model.Collation.builder()
                    .locale(collation.locale())
                    .caseLevel(collation.caseLevel())
                    .collationCaseFirst(collation.caseFirst())
                    .collationStrength(collation.strength())
                    .numericOrdering(collation.numericOrdering())
                    .collationAlternate(collation.alternate())
                    .collationMaxVariable(collation.maxVariable())
                    .backwards(collation.backwards())
                    .normalization(collation.normalization())
                    .build();

            //System.out.println(collationOption);
            options = options.collation(collationOption);

        }

        TimeSeries timeSeries = (TimeSeries) entityModel.getAnnotation(TimeSeries.class);

        if (null !=timeSeries) {
            com.mongodb.client.model.TimeSeriesOptions toptions = new com.mongodb.client.model.TimeSeriesOptions(timeSeries.timeField());
//            TimeSeriesOptions toptions = TimeSeriesOptions.timeSeries(timeSeries.timeField());
            if (StrUtil.hasText(timeSeries.metaField())) {

                if (entityModel.getPropertyModel(timeSeries.metaField()) == null) {
                    throw new MarsOrmException(
                            String.format("Meta field '%s' does not exist in type %s", timeSeries.metaField(), entity.getName()));
                }

                toptions = toptions.metaField(timeSeries.metaField());
            }
            if (timeSeries.granularity()!=null) {
                toptions = toptions.granularity(timeSeries.granularity());

            }

            if (timeSeries.enableExpire()) {
                if ((timeSeries.expireAfterSeconds()) >0) {
                    options = options.expireAfter(timeSeries.expireAfterSeconds(),TimeUnit.SECONDS);
                    /*if (TimeSeriesGranularity.SECONDS.equals(toptions.getGranularity())) {
                        options.expireAfter(timeSeries.expireAfterSeconds(), TimeUnit.SECONDS);
                    }

                    if (TimeSeriesGranularity.MINUTES.equals(toptions.getGranularity())) {
                        options.expireAfter(timeSeries.expireAfterSeconds(), TimeUnit.MINUTES);
                    }

                    if (TimeSeriesGranularity.HOURS.equals(toptions.getGranularity())) {
                        options.expireAfter(timeSeries.expireAfterSeconds(), TimeUnit.HOURS);
                    }*/
                }
            }

            options = options.timeSeriesOptions(toptions);

        }


        return options;
    }

    private < T > T findAllExecute( ClientSession session, MongoCollection collection, Query query, Options options, Object data ) {

        FindIterable findIterable;

        //todo  query  解析缺少
        //  projection
        //  find  操作 没有 option   参数

        if (session == null) {
            findIterable = collection.find(query.getQueryObject());

        } else {
            findIterable = collection.find(session, query.getQueryObject());
        }

        if (!query.getFieldsObject().isEmpty()) {
            findIterable.projection(query.getFieldsObject());
        }


        if (query.isSorted()) {
            findIterable = findIterable.sort(query.getSortObject());
        }

        if (query.getCollation().orElse(null) != null) {
            findIterable = findIterable.collation(query.getCollation().get());
        }

        if (query.getSkip() > 0) {
            findIterable = findIterable.skip((int) query.getSkip());
        }

        if (query.getLimit() > 0) {
            findIterable = findIterable.limit(query.getLimit());
        }


        return (T) findIterable.iterator();

    }

    /**
     * find 操作本身没有相关 的Option 参数
     *
     * @param query
     * @param <T>
     * @return
     */
    private < T > Optional< T > doFindOne( Query query, @Nullable Class< T > entityClass, String collectionName ) {

        FindIterable< T > ts = doFind(query, entityClass, collectionName);
        ts.limit(1);
        return Optional.ofNullable(ts.first());
    }


    public < T > QueryCursor< T > findDistinct( Query query, String field, String collectionName, Class< ? > entityClass, Class< T > resultClass ) {

        MongoCollection< ? > collection = this.getCollection(entityClass, collectionName);
        QueryCursor< T > result = this.findDistinctExecute( collection, query, field, resultClass);
        return result;
    }


    private < T > QueryCursor< T > findDistinctExecute( MongoCollection collection, Query query, String field, Class< T > resultClass ) {

        DistinctIterable< T > distinctIterable;
        if (query != null) {
            distinctIterable = collection.distinct( field, query.getQueryObject(), resultClass);
            if (query.getCollation().orElse(null) != null) {

                distinctIterable = distinctIterable.collation(query.getCollation().get());
            }
        } else {
            distinctIterable = collection.distinct(field, null, resultClass);
        }
        return new QueryCursor< T >(distinctIterable.iterator());
    }


    @Deprecated
    private < T > T insertOneExecute( ClientSession session, MongoCollection collection, InsertOneOptions options, Object data ) {

        InsertOneResult insertOneResult;
        if (options == null) {

            if (session == null) {
                insertOneResult = collection.insertOne(data);

            } else {
                insertOneResult = collection.insertOne(session, data);

            }
            return (T) insertOneResult;

        }

        if (!(options instanceof InsertOneOptions)) {
            throw new ClassCastException();
        }

        if (session == null) {
            insertOneResult = collection.insertOne(data, options.getOriginOptions());

        } else {
            insertOneResult = collection.insertOne(session, data, options.getOriginOptions());
        }
        return (T) insertOneResult;
    }

    @Deprecated
    private < T > Collection< T > insertManyExecute( ClientSession session, MongoCollection collection, InsertManyOptions options, Collection< T > data ) {

        // InsertManyResult insertManyResult = new InsertManyResult();

        //options == null是另外一种情况
        if (options == null) {

            if (session == null) {

                collection.insertMany((List< T >) data);

            } else {

                collection.insertMany(session, (List< T >) data);

            }
            return data;

        }

        //options != null是一种情况
        if (!(options instanceof InsertManyOptions)) {
            throw new ClassCastException();
        }

        InsertManyOptions insertManyOptions = options;

        if (session == null) {


            collection.insertMany((List) data);

        } else {

            collection.insertMany(session, (List) data);

        }

        return data;
    }

    @Deprecated
    private < T > T updateExecute( ClientSession session, MongoCollection collection, Query query, UpdateOptions options, Object data ) {

        if (!(options instanceof UpdateOptions)) {
            throw new ClassCastException();
        }

//        UpdateOptions option = (UpdateOptions) options;

        Document updateOperations = new Document("$set", new Document((Map) data));

        UpdateResult updateResult;

        if (options.isMulti()) {

            if (session == null) {
//                updateResult.setOriginUpdateResult(collection.updateMany(query.getQueryObject(), updateOperations, option.getOriginOptions()));
                updateResult = collection.updateMany(query.getQueryObject(), updateOperations, options.getOriginOptions());
            } else {
                updateResult = collection.updateMany(session, query.getQueryObject(), updateOperations, options.getOriginOptions());
            }
            return (T) updateResult;

        } else {

            if (session == null) {
                updateResult = collection.updateOne(query.getQueryObject(), updateOperations, options.getOriginOptions());
            } else {
                updateResult = collection.updateOne(session, query.getQueryObject(), updateOperations, options.getOriginOptions());
            }

            return (T) updateResult;

        }

    }

    @Deprecated
    private < T > UpdateResult updateDefinitionExecute(  MongoCollection<T> collection, Query query, UpdateOptions options, Document data ) {

/*
        Document dd = (Document) data;
        dd.containsKey(UpdatePipeline.Updatepipeline);
        List< Document > list = dd.getList(UpdatePipeline.Updatepipeline, Document.class);*/

       /* if (!(options instanceof UpdateOptions)) {
            throw new ClassCastException();
        }*/

        //UpdateOptions option = (UpdateOptions) options;

        UpdateResult updateResult;

        if (options.isMulti()) {

            updateResult = this.operations.updateMany(collection , query.getQueryObject(), (Document)data, options.getOriginOptions());
           /* if (session == null) {
                updateResult = collection.updateMany(query.getQueryObject(), (Document) data, option.getOriginOptions());
            } else {
                updateResult = collection.updateMany(session, query.getQueryObject(), (Document) data, option.getOriginOptions());
            }*/

            return  updateResult;

        } else {

            updateResult = this.operations.updateOne(collection ,query.getQueryObject() ,(Document) data, options.getOriginOptions());

            /*
            if (session == null) {
                updateResult = collection.updateOne(query.getQueryObject(), (Document) data, option.getOriginOptions());
            } else {
                updateResult = collection.updateOne(session, query.getQueryObject(), (Document) data, option.getOriginOptions());
            }*/

            return  updateResult;

        }


    }


    @Deprecated
    private < T > T createIndexExecute(  MongoCollection collection ,  Options options, Object data ) {

        Index index = (Index) data;

        IndexOptions indexOptions = index.getIndexOptions();

        if (indexOptions == null) {


                collection.createIndex(index.getIndexKeys());


        } else {


                collection.createIndex(index.getIndexKeys(), indexOptions.getOriginOptions());

        }

        return null;
    }

    @Deprecated
    private < T > void dropOneIndexExecute( MongoCollection collection, Index index ) {


            collection.dropIndex(index.getIndexKeys());


    }

    @Deprecated
    private < T > T dropManyIndexExecute( ClientSession session, MongoCollection collection, Query query, Options options, Object data ) {

        if (session == null) {
            collection.dropIndexes();

        } else {
            collection.dropIndexes(session);

        }

        return null;
    }


    @Deprecated
    private < T > T findManyIndexExecute( ClientSession session, MongoCollection collection, Query query, Options options, Object data ) {

        ListIndexesIterable indexIterable = null;

        if (session == null) {
            indexIterable = collection.listIndexes();

        } else {

            indexIterable = collection.listIndexes(session);
        }
        MongoCursor iterator = indexIterable.iterator();
        List indexes = new ArrayList();
        while (iterator.hasNext()) {

            Document o = (Document) iterator.next();

            IndexOptions indexOptions = new IndexOptions();
            if (o.get("background") != null) {

                //todo  具体值可能为多种类型  如 "true"  true  1 1.0  "1.0"  "5.0"  甚至为 任意字符"xxx"尤其是老版本 情况很多 这里并不能全部列举
                // C语言决策. C语言编程假定任何非零和非空值为真，并且如果它是零或null，那么它被假定为假值
                // 主要为 boolean  类型 String  类型  数值类型
                if (o.get("background") instanceof Boolean) {
                    indexOptions.background((Boolean) o.get("background"));
                } else if (o.get("background") instanceof String) {
                    indexOptions.background(true);
                } else if (o.get("background") instanceof Number) {
                    // 非 0 为真
                    double v;
                    try {
                        v = Double.valueOf(o.get("background").toString());
                        if (v > 0) {
                            indexOptions.background(true);
                        } else indexOptions.background(v < 0);
                    } catch (Exception e) {
                        LOGGER.warn(String.format("Index background Option parse error from  index name %s  with background value %s ", o.get("name"), o.get("background")));
                        indexOptions.background(true);
                    }
                }

            }

            if (o.get("unique") != null) {
                indexOptions.unique((Boolean) o.get("unique"));
            }
            if (o.get("name") != null) {
                indexOptions.name((String) o.get("name"));
            }

            if (o.get("partialFilterExpression") != null) {
                indexOptions.partialFilterExpression((Bson) o.get("partialFilterExpression"));
            }
            if (o.get("sparse") != null) {
                indexOptions.sparse((Boolean) o.get("sparse"));
            }
            if (o.get("expireAfterSeconds") != null) {
                Long expireAfter = ((Double) Double.parseDouble(o.get("expireAfterSeconds").toString())).longValue();
                //秒以下会丢失
                indexOptions.expireAfter(expireAfter, TimeUnit.SECONDS);
            }

            if (o.get("hidden") != null) {
                indexOptions.hidden((Boolean) o.get("hidden"));
            }

            if (o.get("storageEngine") != null) {
                //不常用到
                indexOptions.storageEngine((Bson) o.get("storageEngine"));
            }

            //---------deal with Collation

            if (o.get("collation") != null) {
                com.mongodb.client.model.Collation.Builder collationBuilder = com.mongodb.client.model.Collation.builder();
                Document collation = (Document) o.get("collation");
                if (collation.get("locale") != null) {
                    collationBuilder.locale(collation.getString("locale"));
                }
                if (collation.get("caseLevel") != null) {
                    collationBuilder.caseLevel(collation.getBoolean("caseLevel"));
                }
                if (collation.get("caseFirst") != null) {
                    collationBuilder.collationCaseFirst(CollationCaseFirst.fromString(collation.getString("caseFirst")));
                }
                if (collation.get("strength") != null) {
                    collationBuilder.collationStrength(CollationStrength.fromInt(
                            ((Double) Double.parseDouble(collation.get("strength").toString())).intValue()
                    ));
                }
                if (collation.get("numericOrdering") != null) {
                    collationBuilder.numericOrdering(collation.getBoolean("numericOrdering"));
                }
                if (collation.get("alternate") != null) {
                    collationBuilder.collationAlternate(CollationAlternate.fromString(collation.getString("alternate")));
                }
                if (collation.get("maxVariable") != null) {
                    collationBuilder.collationMaxVariable(CollationMaxVariable.fromString(collation.getString("maxVariable")));
                }
                if (collation.get("normalization") != null) {
                    collationBuilder.normalization(collation.getBoolean("normalization"));
                }
                if (collation.get("backwards") != null) {
                    collationBuilder.backwards(collation.getBoolean("backwards"));
                }
                indexOptions.collation(collationBuilder.build());
            }

            //---------deal with Text


            if (o.get("weights") != null) {
                indexOptions.weights((Bson) o.get("weights"));
            }
            if (o.get("textIndexVersion") != null) {
                indexOptions.textVersion(((Double) Double.parseDouble(o.get("textIndexVersion").toString())).intValue());
            }
            if (o.get("default_language") != null) {
                indexOptions.defaultLanguage((String) o.get("default_language"));
            }
            if (o.get("language_override") != null) {
                indexOptions.languageOverride(o.get("language_override").toString());
            }

            //--------deal with wildcard

            if (o.get("wildcardProjection") != null) {
                indexOptions.wildcardProjection((Bson) o.get("wildcardProjection"));
            }

            //---------deal with geoHaystack
            if (o.get("bucketSize") != null) {
                indexOptions.bucketSize(Double.parseDouble(o.get("bucketSize").toString()));
            }
            //---------deal with  2d

            if (o.get("bits") != null) {
                indexOptions.bits(((Double) Double.parseDouble(o.get("bits").toString())).intValue());
            }
            if (o.get("max") != null) {
                indexOptions.max((Double.parseDouble(o.get("max").toString())));
            }
            if (o.get("min") != null) {
                indexOptions.min((Double.parseDouble(o.get("min").toString())));
            }

            //---------------deal with 2dsphere

            if (o.get("2dsphereIndexVersion") != null) {
                indexOptions.sphereVersion(((Double) Double.parseDouble((o.get("2dsphereIndexVersion").toString()))).intValue());
            }

            //------ let it be backgroud
            indexOptions.background(true);

            Document key = (Document) o.get("key");

            Index index = new Index();

            Set< String > strings = key.keySet();

            for (String keyName : strings) {
                index.on(keyName, IndexDirection.fromValue(key.get(keyName)));

            }
            index.setOptions(indexOptions);

            indexes.add(index);

        }

        return (T) indexes;
    }


    @Deprecated
    private < T > T createManyIndexExecute( ClientSession session, MongoCollection collection, Query query, Options options, Object data ) {

        List< Index > indexes = (List) data;
        if (session == null) {

            for (Index index : indexes) {

                if (index.getIndexOptions() == null) {
                    collection.createIndex(index.getIndexKeys());
                } else {
                    collection.createIndex(index.getIndexKeys(), index.getIndexOptions().getOriginOptions());
                }
            }

        } else {

            for (Index index : indexes) {
                if (index.getIndexOptions() == null) {
                    collection.createIndex(session, index.getIndexKeys());
                } else {
                    collection.createIndex(session, index.getIndexKeys(), index.getIndexOptions().getOriginOptions());
                }
            }
        }

        return null;
    }


    @Override
    public < T > T withTransaction( MarsTransaction< T > body ) {
        return this.doTransaction(startSession(), body);
    }

    @Override
    public < T > T withTransaction( MarsTransaction< T > transaction, ClientSessionOptions options ) {
        return this.doTransaction(startSession(options), transaction);
    }

    private < T > T doTransaction( MarsSessionImpl marsSession, MarsTransaction< T > body ) {
        try {
            if (marsSession == null) {
                throw new IllegalStateException("No session could be found for the transaction.");
            }
            return marsSession.withTransaction(() -> body.execute(marsSession));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        } finally {
            if (marsSession != null) {
                try {
                    marsSession.close();
                } catch (Exception e) {

                }
            }

        }
    }

    private com.mongodb.client.model.CountOptions decorateCountOption( Query query ) {
        com.mongodb.client.model.CountOptions options = new com.mongodb.client.model.CountOptions();

        if (query.getLimit() > 0) {
            options.limit(query.getLimit());
        }
        if (query.getSkip() > 0) {
            options.skip((int) query.getSkip());
        }
        if (StrUtil.hasText(query.getHint())) {
            String hint = query.getHint();
            try {
                Document parse = Document.parse(hint);
                options.hint(parse);
            } catch (Exception e) {
                options.hintString(hint);
            }

        }

        return options;
    }


    public abstract static class DatastoreOperations {
        public abstract < T > String createIndex( MongoCollection< T > collection, IndexModel index );

        public abstract < T > void dropIndex( MongoCollection< T > collection, Document bsonkey );

        public abstract < T > List< String > createIndexes( MongoCollection< T > collection, List< IndexModel > indexes );

        public abstract < T > void dropIndexes( MongoCollection< T > collection );

        public abstract < T > ListIndexesIterable< Document > getIndexes( MongoCollection< T > collection );


        public abstract < T > long countDocuments( MongoCollection< T > collection, Document queryDocument, com.mongodb.client.model.CountOptions options );

        public abstract < T > DeleteResult deleteMany( MongoCollection< T > collection, Document queryDocument, com.mongodb.client.model.DeleteOptions options );

        public abstract < T > DeleteResult deleteOne( MongoCollection< T > collection, Document queryDocument, com.mongodb.client.model.DeleteOptions options );

        public abstract < E > FindIterable< E > find( MongoCollection< E > collection, Document queryDocument );

        public abstract < E > DistinctIterable< E > distinct( MongoCollection< E > collection, Document query, String fieldName, Class< E > resultClass );

        @Nullable
        public abstract < T > T findOneAndDelete( MongoCollection< T > collection, Document queryDocument, com.mongodb.client.model.FindOneAndDeleteOptions options );

        @Nullable
        public abstract < T > T findOneAndUpdate( MongoCollection< T > collection, Document queryDocument, Document updateOperations, com.mongodb.client.model.FindOneAndUpdateOptions options );

        @Nullable
        public abstract < T > T findOneAndReplace( MongoCollection< T > collection, Document queryDocument, T entity, com.mongodb.client.model.FindOneAndReplaceOptions options );

        public abstract < T > InsertManyResult insertMany( MongoCollection< T > collection, List< T > list, com.mongodb.client.model.InsertManyOptions options );

        public abstract < T > InsertOneResult insertOne( MongoCollection< T > collection, T entity, com.mongodb.client.model.InsertOneOptions options );

        public abstract < T > UpdateResult replaceOne( MongoCollection< T > collection, T entity, Document queryDocument, com.mongodb.client.model.ReplaceOptions options );

        public abstract Document runCommand( Document command );

        public abstract < T > UpdateResult updateMany( MongoCollection< T > collection, Document queryDocument, Document updateOperations,
                                                       com.mongodb.client.model.UpdateOptions options );

        public abstract < T > UpdateResult updateMany( MongoCollection< T > collection, Document queryDocument, List< Document > updateOperations,
                                                       com.mongodb.client.model.UpdateOptions options );

        public abstract < T > UpdateResult updateOne( MongoCollection< T > collection, Document queryDocument, Document updateOperations,
                                                      com.mongodb.client.model.UpdateOptions options );

        public abstract < T > UpdateResult updateOne( MongoCollection< T > collection, Document queryDocument, List< Document > updateOperations,
                                                      com.mongodb.client.model.UpdateOptions options );

    }

    /**
     * 具体操作的一种实现
     *
     * @see Datastore
     * @see com.whaleal.mars.session.MarsSessionImpl
     */
    private class CollectionOperations extends DatastoreOperations {

        @Override
        public String createIndex( MongoCollection collection, IndexModel index ) {
            return collection.createIndex(index.getKeys(), index.getOptions());
        }


        @Override
        public < T > void dropIndex( MongoCollection< T > collection, Document bsonkey  ) {
            collection.dropIndex(bsonkey);
        }

        @Override
        public < T > List< String > createIndexes( MongoCollection< T > collection, List< IndexModel > indexes ) {
            return collection.createIndexes(indexes);
        }

        @Override
        public < T > void dropIndexes( MongoCollection< T > collection ) {
            collection.dropIndexes();
        }

        @Override
        public < T > ListIndexesIterable< Document > getIndexes( MongoCollection< T > collection ) {
            return collection.listIndexes();

        }

        @Override
        public < T > long countDocuments( MongoCollection< T > collection, Document query, com.mongodb.client.model.CountOptions options ) {
            return collection.countDocuments(query, options);
        }

        @Override
        public < T > DeleteResult deleteMany( MongoCollection< T > collection, Document queryDocument, com.mongodb.client.model.DeleteOptions options ) {
            return collection.deleteMany(queryDocument, options);
        }

        @Override
        public < T > DeleteResult deleteOne( MongoCollection< T > collection, Document queryDocument, com.mongodb.client.model.DeleteOptions options ) {
            return collection.deleteOne(queryDocument, options);
        }

        @Override
        public < E > FindIterable< E > find( MongoCollection< E > collection, Document query ) {
            return collection.find(query);
        }

        @Override
        public < E > DistinctIterable< E > distinct( MongoCollection< E > collection, Document query, String fieldName, Class< E > resultClass ) {

            return collection.distinct(fieldName, query, resultClass);
        }

        @Override
        public < T > T findOneAndDelete( MongoCollection< T > collection, Document queryDocument, com.mongodb.client.model.FindOneAndDeleteOptions options ) {
            return collection.findOneAndDelete(queryDocument, options);
        }

        @Override
        public < T > T findOneAndUpdate( MongoCollection< T > collection, Document queryDocument, Document update, com.mongodb.client.model.FindOneAndUpdateOptions options ) {
            return collection.findOneAndUpdate(queryDocument, update, options);
        }

        @Override
        public < T > T findOneAndReplace( MongoCollection< T > collection, Document queryDocument, T entity, com.mongodb.client.model.FindOneAndReplaceOptions options ) {
            return collection.findOneAndReplace(queryDocument,entity,options);
        }


        @Override
        public < T > InsertManyResult insertMany( MongoCollection< T > collection, List< T > list, com.mongodb.client.model.InsertManyOptions options ) {
            return collection.insertMany(list, options);
        }

        @Override
        public < T > InsertOneResult insertOne( MongoCollection< T > collection, T entity, com.mongodb.client.model.InsertOneOptions options ) {
            return collection.insertOne(entity, options);
        }

        @Override
        public < T > UpdateResult replaceOne( MongoCollection< T > collection, T entity, Document filter, com.mongodb.client.model.ReplaceOptions options ) {
            return collection.replaceOne(filter, entity, options);
        }

        @Override
        public Document runCommand( Document command ) {
            return mongoClient
                    .getDatabase("admin")
                    .runCommand(command);
        }

        @Override
        public < T > UpdateResult updateMany( MongoCollection< T > collection, Document queryObject, Document updateOperations,
                                              com.mongodb.client.model.UpdateOptions options ) {
            return collection.updateMany(queryObject, updateOperations, options);
        }

        @Override
        public < T > UpdateResult updateOne( MongoCollection< T > collection, Document queryObject, Document updateOperations,
                                             com.mongodb.client.model.UpdateOptions options ) {
            return collection.updateOne(queryObject, updateOperations, options);
        }

        @Override
        public < T > UpdateResult updateMany( MongoCollection< T > collection, Document queryObject, List< Document > updateOperations,
                                              com.mongodb.client.model.UpdateOptions options ) {
            return collection.updateMany(queryObject, updateOperations, options);
        }

        @Override
        public < T > UpdateResult updateOne( MongoCollection< T > collection, Document queryObject, List< Document > updateOperations,
                                             com.mongodb.client.model.UpdateOptions options ) {
            return collection.updateOne(queryObject, updateOperations, options);
        }
    }


}



