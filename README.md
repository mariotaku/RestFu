# RestFu
Simple REST library for Android, does not ships with HTTP client nor data deserializer.

You need to implement on your own, but this also allows you to choose your favorite implementation.

````java
public interface API {

  @GET("/api/{path}")
  ResultObject doGetMethod(@Query("param") param, @Path("path") path) throws APIException;

  @POST("/api/endpoint")
  @Body(BodyType.FORM)
  ResultObject doGetMethod(@Form("param") param) throws APIException;
}
````
