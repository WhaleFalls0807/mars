package com.whaleal.mars.core.command;

import com.whaleal.mars.Constant;
import com.whaleal.mars.core.Mars;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Test;

/**
 * Author: cjq
 * Date: 2022/6/16 0016 15:48
 * FileName: InvalidateUserCacheTest
 * Description:
 */
public class InvalidateUserCacheTest {

    private Mars mars = new Mars(Constant.connectionStr);


    /**
     * db.runCommand( { invalidateUserCache: 1 } )
     */
    @Test
    public void testForInvalidateUserCache(){
        Document document = mars.executeCommand("{ invalidateUserCache: 1 }");
        System.out.println(document);
    }
}
