# RestFu
Simple REST library for Android, does not ship with HTTP client or data deserializer.

You need to implement on your own, but this also allows you to choose your favorite implementation, and reduce dependencies

Declare API calls in your Java interface

````java
public interface API {

  @GET("/api/{path}")
  ResultObject doGetMethod(@Query("param") String param, @Path("path") String path) throws APIException;

  @POST("/api/endpoint")
  @Body(BodyType.FORM)
  ResultObject doPostMethod(@Form("param") String param) throws APIException;
}
````

Generate implementation using `RestAPIFactory`

````java
RestAPIFactory<APIException> factory = new RestAPIFactory<>();
factory.setEndpoint(new Endpoint("https://example.com"));
factory.setExceptionFactory(new APIExceptionFactory());
factory.setRestConverterFactory(new APIDataConverterFactory<APIException>());
factory.setHttpClient(new OkHttpRestClient());
API api = factory.build(API.class);
````

Use it!

````java
try {
    api.doPostMethod("value");
} catch (APIException e) {
    // Error handling
}
````