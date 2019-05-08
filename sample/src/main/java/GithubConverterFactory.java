import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.mariotaku.restfu.RestConverter;
import org.mariotaku.restfu.http.HttpResponse;
import org.mariotaku.restfu.http.mime.Body;
import org.mariotaku.restfu.http.mime.SimpleBody;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

/**
 * Created by mariotaku on 16/1/17.
 */
public class GithubConverterFactory implements RestConverter.Factory<GithubException> {

    @NotNull
    @Override
    public RestConverter<HttpResponse, ?, GithubException> forResponse(@NotNull Type toType) {
        return new JsonConverter(toType);
    }

    @NotNull
    @Override
    public RestConverter<?, Body, GithubException> forRequest(@NotNull Type fromType) {
        return new ParamBodyConverter();
    }

    private static class JsonConverter implements RestConverter<HttpResponse, Object, GithubException> {
        private final Type toType;
        Gson gson = new Gson();

        public JsonConverter(Type toType) {
            this.toType = toType;
        }

        @NotNull
        @Override
        public Object convert(@NotNull HttpResponse from) throws IOException {
            return gson.fromJson(new InputStreamReader(from.getBody().stream()), toType);
        }
    }

    private static class ParamBodyConverter implements RestConverter<Object, Body, GithubException> {
        @NotNull
        @Override
        public Body convert(@NotNull Object from) throws ConvertException, IOException {
            return SimpleBody.wrap(from);
        }
    }
}
