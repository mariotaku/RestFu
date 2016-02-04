import okhttp3.OkHttpClient;
import org.mariotaku.restfu.RestAPIFactory;
import org.mariotaku.restfu.http.Endpoint;
import org.mariotaku.restfu.okhttp.OkHttpRestClient;

/**
 * Created by mariotaku on 16/1/17.
 */
public class Main {
    public static void main(String[] args) throws GithubException {
        RestAPIFactory<GithubException> factory = new RestAPIFactory<>();
        factory.setHttpClient(new OkHttpRestClient(new OkHttpClient()));
        factory.setEndpoint(new Endpoint("https://api.github.com"));
        factory.setExceptionFactory(new GithubExceptionFactory());
        factory.setRestConverterFactory(new GithubConverterFactory());
        Github github = factory.build(Github.class);
        System.out.println(github.contributors("mariotaku", "RestFu"));
    }
}
