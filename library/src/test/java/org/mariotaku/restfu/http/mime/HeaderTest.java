package org.mariotaku.restfu.http.mime;

import org.junit.Assert;
import org.junit.Test;
import org.mariotaku.restfu.RestAPIFactory;
import org.mariotaku.restfu.http.Endpoint;

import java.util.List;

/**
 * Created by mariotaku on 2017/3/25.
 */
public class HeaderTest {

    @Test
    public void testStringArrayHeader() {
        TestInterface ti = getTestInterface();
        try {
            ti.testStringArrayHeader(new String[]{"1", "2", "3"});
        } catch (HttpRequestInfoException e) {
            List<String> values = e.request.getHeaders().get("X-RestFu-Header");
            Assert.assertTrue(values.contains("1"));
            Assert.assertTrue(values.contains("2"));
            Assert.assertTrue(values.contains("3"));
        }
    }

    @Test
    public void testStringArrayDelimHeader() {
        TestInterface ti = getTestInterface();
        try {
            ti.testStringArrayDelimHeader(new String[]{"1", "2", "3"});
        } catch (HttpRequestInfoException e) {
            String value = e.request.getHeaders().getFirst("X-RestFu-Header");
            Assert.assertEquals("1,2,3", value);
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
