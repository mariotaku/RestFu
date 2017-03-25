package org.mariotaku.restfu.http.mime;

import org.junit.Assert;
import org.junit.Test;
import org.mariotaku.restfu.RestAPIFactory;
import org.mariotaku.restfu.http.Endpoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by mariotaku on 2017/3/25.
 */
public class ParamTest {

    @Test
    public void testStringArrayParam() throws IOException {
        TestInterface ti = getTestInterface();
        try {
            ti.testStringArrayParam(new String[]{"1", "2", "3"});
        } catch (HttpRequestInfoException e) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            e.request.getBody().writeTo(os);
            Assert.assertEquals("array=1&array=2&array=3", os.toString("UTF-8"));
        }
    }

    @Test
    public void testStringArrayDelimParam() throws IOException {
        TestInterface ti = getTestInterface();
        try {
            ti.testStringArrayDelimParam(new String[]{"1", "2", "3"});
        } catch (HttpRequestInfoException e) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            e.request.getBody().writeTo(os);
            Assert.assertEquals("array=1%2C2%2C3", os.toString("UTF-8"));
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
