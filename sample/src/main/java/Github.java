import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.param.Headers;
import org.mariotaku.restfu.annotation.param.KeyValue;
import org.mariotaku.restfu.annotation.param.Path;
import org.mariotaku.restfu.annotation.param.Queries;
import org.mariotaku.restfu.callback.RawCallback;

import java.util.List;

/**
 * Created by mariotaku on 16/1/17.
 */
public interface Github {

    @GET("/repos/{owner}/{repo}/contributors")
    @Queries(@KeyValue(key = "test", value = "ok"))
    @Headers(@KeyValue(key = "X-Invalid-Character", value = "无效字符"))
    List<Contributor> contributors(@Path(value = "owner") String owner, @Path(value = "repo") String repo) throws GithubException;

    @GET("/repos/{owner}/{repo}/contributors")
    void rawContributors(@Path(value = "owner") String owner, @Path(value = "repo") String repo,
                    RawCallback callback) throws GithubException;
}
