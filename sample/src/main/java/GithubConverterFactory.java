import com.google.gson.Gson;
import org.mariotaku.restfu.RestConverter;
import org.mariotaku.restfu.http.HttpResponse;
import org.mariotaku.restfu.http.mime.BaseBody;
import org.mariotaku.restfu.http.mime.Body;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

/**
 * Created by mariotaku on 16/1/17.
 */
public class GithubConverterFactory implements RestConverter.Factory {

    @Override
    public RestConverter<HttpResponse, ?> fromResponse(Type toType) {
        return new JsonConverter(toType);
    }

    @Override
    public RestConverter<?, Body> toParam(Type fromType) {
        return new ParamBodyConverter();
    }

    private static class JsonConverter implements RestConverter<HttpResponse, Object> {
        private final Type toType;
        Gson gson = new Gson();

        public JsonConverter(Type toType) {
            this.toType = toType;
        }

        @Override
        public Object convert(HttpResponse from) throws IOException {
            return gson.fromJson(new InputStreamReader(from.getBody().stream()), toType);
        }
    }

    private static class ParamBodyConverter implements RestConverter<Object, Body> {
        @Override
        public Body convert(Object from) throws ConvertException, IOException {
            return BaseBody.wrap(from);
        }
    }
}
