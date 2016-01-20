import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.param.KeyValue;
import org.mariotaku.restfu.annotation.param.Path;
import org.mariotaku.restfu.annotation.param.Queries;

import java.util.List;

/**
 * Created by mariotaku on 16/1/17.
 */
public interface Github {

    @GET("/repos/{owner}/{repo}/contributors")
    @Queries(@KeyValue(key = "test", value = "ok"))
    List<Contributor> contributors(@Path(value = "owner") String owner, @Path(value = "repo") String repo) throws GithubException;
}
