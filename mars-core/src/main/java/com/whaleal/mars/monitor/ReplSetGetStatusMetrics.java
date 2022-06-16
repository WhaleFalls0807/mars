package com.whaleal.mars.monitor;

import com.mongodb.client.MongoClient;
import org.bson.Document;

import javax.print.Doc;
import java.util.Date;
import java.util.List;

/**
 * @author lyz
 * @description
 * @date 2022-06-15 16:45
 **/
//todo 部分参数可以进行深度解析
public class ReplSetGetStatusMetrics extends AbstractMonitor{

    /**
     * @param mongoClient must not be {@literal null}.
     */
    protected ReplSetGetStatusMetrics(MongoClient mongoClient) {
        super(mongoClient);
    }

    public Integer getSetName(){
        return getReplSetGetStatus().get("myState",Integer.class);
    }

    public Date getDate(){
        return getReplSetGetStatus().getDate("date");
    }

    public int getMyState(){
        return getReplSetGetStatus().getInteger("myState");
    }

    public Long getTerm(){
        return getReplSetGetStatus().getLong("term");
    }

    public String getSyncSourceHost(){
        return getReplSetGetStatus().getString("syncSourceHost");
    }

    public Integer getSyncSourceId(){
        return getReplSetGetStatus().get("syncSourceId",Integer.class);
    }

    public Long getHeartBeatInterval(){
        return getReplSetGetStatus().getLong("heartbeatIntervalMillis");
    }

    public int getMajorityVoteCount(){
        return getReplSetGetStatus().getInteger("majorityVoteCount");
    }

    public int getWriteMajorityCount(){
        return getReplSetGetStatus().getInteger("writeMajorityCount");
    }

    public Integer getVotingMembersCount(){
        return getReplSetGetStatus().getInteger("votingMembersCount");
    }

    public Integer getWritableVotingMembersCount(){
        return getReplSetGetStatus().getInteger("writableVotingMembersCount");
    }

    public Document getOPTimes(){
        return getReplSetGetStatus().get("optimes", Document.class);
    }

    public Document getElectionCandidateMetrics(){
        return getReplSetGetStatus().get("electionCandidateMetrics",Document.class);
    }

    public Document getElectionParticipantMetrics(){
        return getReplSetGetStatus().get("electionParticipantMetrics",Document.class);
    }

    public List<Document> getMembers(){
        return (List<Document>)getReplSetGetStatus().get("members");
    }

    public Document getClusterTime(){
        return getReplSetGetStatus().get("$clusterTime",Document.class);
    }

}
