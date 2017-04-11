import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mariotaku.restfu.ExceptionFactory;
import org.mariotaku.restfu.http.HttpRequest;
import org.mariotaku.restfu.http.HttpResponse;

/**
 * Created by mariotaku on 16/1/17.
 */
public class GithubExceptionFactory implements ExceptionFactory<GithubException> {
    @Override
    @NotNull
    public GithubException newException(@Nullable Throwable cause, @Nullable HttpRequest request, @Nullable HttpResponse response) {
        return new GithubException();
    }
}
