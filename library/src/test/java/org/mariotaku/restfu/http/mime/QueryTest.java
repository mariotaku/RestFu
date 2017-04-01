package org.mariotaku.restfu.http.mime;

import org.junit.Assert;
import org.junit.Test;
import org.mariotaku.restfu.RestAPIFactory;
import org.mariotaku.restfu.http.Endpoint;

/**
 * Created by mariotaku on 2017/3/25.
 */
public class QueryTest {

    @Test
    public void testStringArrayQuery() {
        TestInterface ti = getTestInterface();
        try {
            ti.testStringArrayQuery(new String[]{"1", "2", "3"});
        } catch (HttpRequestInfoException e) {
            Assert.assertEquals("https://example.com/test/query?array=1&array=2&array=3", e.request.getUrl());
        }
    }

    @Test
    public void testStringArrayDelimQuery() {
        TestInterface ti = getTestInterface();
        try {
            ti.testStringArrayDelimQuery(new String[]{"1", "2", "3"});
        } catch (HttpRequestInfoException e) {
            Assert.assertEquals("https://example.com/test/query?array=1%2C2%2C3", e.request.getUrl());
        }
    }

    @Test
    public void testParamAsQuery() throws Exception {
        TestInterface ti = getTestInterface();
        try {
            ti.testParamAsQuery("value");
        } catch (HttpRequestInfoException e) {
            Assert.assertEquals("https://example.com/test/query?name=value", e.request.getUrl());
        }
    }

    private TestInterface getTestInterface() {
        RestAPIFactory<HttpRequestInfoException> factory = new RestAPIFactory<>();
        factory.setEndpoint(new Endpoint("https://example.com"));
        factory.setExceptionFactory(new HttpRequestInfoExceptionFactory());
        factory.setRestConverterFactory(new NullConverterFactory<HttpRequestInfoException>());
        factory.setHttpClient(new DirectThrowRestHttpClient());
        return factory.build(TestInterface.class);
    }

}
