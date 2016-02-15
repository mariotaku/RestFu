import okhttp3.OkHttpClient;
import org.mariotaku.restfu.RestAPIFactory;
import org.mariotaku.restfu.http.Endpoint;
import org.mariotaku.restfu.okhttp3.OkHttpRestClient;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * Created by mariotaku on 16/1/17.
 */
public class Main {
    public static void main(String[] args) throws GithubException {
        RestAPIFactory<GithubException> factory = new RestAPIFactory<>();
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.proxy(new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("127.0.0.1", 1080)));
        factory.setHttpClient(new OkHttpRestClient(builder.build()));
        factory.setEndpoint(new Endpoint("https://api.github.com"));
        factory.setExceptionFactory(new GithubExceptionFactory());
        factory.setRestConverterFactory(new GithubConverterFactory());
        Github github = factory.build(Github.class);
        System.out.println(github.contributors("mariotaku", "RestFu"));
    }
}
