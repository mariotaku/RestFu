import com.google.gson.annotations.SerializedName;

public class Contributor {
    @SerializedName("login")
    public String login;
    @SerializedName("contributions")
    public int contributions;

    @Override
    public String toString() {
        return "Contributor{" +
                "login='" + login + '\'' +
                ", contributions=" + contributions +
                '}';
    }
}