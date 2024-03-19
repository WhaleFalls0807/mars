package com.whaleal.mars.core.command;

import com.whaleal.mars.Constant;
import com.whaleal.mars.core.Mars;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Test;

/**
 * Author: cjq
 * Date: 2022/6/16 0016 13:32
 * FileName: SetFeatureCompatibilityVersionTest
 * Description:
 */
public class SetFeatureCompatibilityVersionTest {
    private Mars mars =  new Mars(Constant.connectionStr);
    /**
     * db.adminCommand( {
     *    setFeatureCompatibilityVersion: <version>,
     *    writeConcern: { wtimeout: <timeout> }
     * } )
     */
    @Test
    public void testForViewFeatureCompatibilityVersion(){
        Document document = mars.executeCommand(Document.parse("{ getParameter: 1, featureCompatibilityVersion: 1 }"));
        Document result = Document.parse("{ \"featureCompatibilityVersion\" : { \"version\" : \"5.0\" }, \"ok\" : 1.0 }\n");
        Assert.assertEquals(result,document);
    }

    @Test
    public void testForSetFeatureCompatibilityVersion(){
        Document document = mars.executeCommand(Document.parse("{ setFeatureCompatibilityVersion: \"5.0\" }"));
        Document result = Document.parse("{\"ok\":1.0}");
        Assert.assertEquals(result,document);
    }

    @Test
    public void testForSetWriteConcernTimeout(){
        Document document = mars.executeCommand(Document.parse("{\n" +
                "   setFeatureCompatibilityVersion: \"5.0\",\n" +
                "   writeConcern: { wtimeout: 5000 }\n" +
                "}"));
        Document result = Document.parse("{ \"ok\" : 1.0 }");
        Assert.assertEquals(result,document);
    }
}
