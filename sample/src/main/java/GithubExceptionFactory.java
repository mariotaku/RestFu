import org.mariotaku.restfu.ExceptionFactory;
import org.mariotaku.restfu.http.HttpRequest;
import org.mariotaku.restfu.http.HttpResponse;

/**
 * Created by mariotaku on 16/1/17.
 */
public class GithubExceptionFactory implements ExceptionFactory {
    @Override
    public Exception newException(Throwable cause, HttpRequest request, HttpResponse response) {
        return new GithubException();
    }
}
