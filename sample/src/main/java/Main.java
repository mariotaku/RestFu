import org.apache.commons.io.IOUtils;
import org.mariotaku.restfu.RestAPIFactory;
import org.mariotaku.restfu.callback.RawCallback;
import org.mariotaku.restfu.http.Endpoint;
import org.mariotaku.restfu.http.HttpResponse;
import org.mariotaku.restfu.urlconnection.URLConnectionRestClient;

import java.io.IOException;

/**
 * Created by mariotaku on 16/1/17.
 */
public class Main {
    public static void main(String[] args) throws GithubException {
        RestAPIFactory<GithubException> factory = new RestAPIFactory<>();
        factory.setHttpClient(new URLConnectionRestClient());
        factory.setEndpoint(new Endpoint("https://api.github.com"));
        factory.setExceptionFactory(new GithubExceptionFactory());
        factory.setRestConverterFactory(new GithubConverterFactory());
        Github github = factory.build(Github.class);
        System.out.println(github.contributors("mariotaku", "RestFu"));

        System.out.println("Trying raw request");
        github.rawContributors("mariotaku", "RestFu", new RawCallback<GithubException>() {
            @Override
            public void result(HttpResponse result) throws IOException {
                IOUtils.copy(result.getBody().stream(), System.out);
            }

            @Override
            public void error(GithubException exception) {
                exception.printStackTrace();
            }
        });
    }
}
