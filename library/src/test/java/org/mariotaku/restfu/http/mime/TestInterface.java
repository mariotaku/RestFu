package org.mariotaku.restfu.http.mime;

import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.param.Header;
import org.mariotaku.restfu.annotation.param.Query;

/**
 * Created by mariotaku on 2017/3/25.
 */
public interface TestInterface {

    @GET("/test/header")
    void testStringArrayHeader(@Header("X-RestFu-Header") String[] array) throws HttpRequestInfoException;

    @GET("/test/header")
    void testStringArrayDelimHeader(@Header(value = "X-RestFu-Header", arrayDelimiter = ',') String[] array) throws HttpRequestInfoException;

    @GET("/test/query")
    void testStringArrayQuery(@Query("array") String[] array) throws HttpRequestInfoException;

    @GET("/test/query")
    void testStringArrayDelimQuery(@Query(value = "array", arrayDelimiter = ',') String[] array) throws HttpRequestInfoException;

}
