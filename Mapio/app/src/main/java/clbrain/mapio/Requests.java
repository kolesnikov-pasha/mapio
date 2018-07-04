package clbrain.mapio;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

class SendCoordinates{
    String user_id;
    Double latitude;
    Double longitude;

    public SendCoordinates() {
    }

    public SendCoordinates(String user_id, Double latitude, Double longitude) {
        this.user_id = user_id;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}

class Color{

    String user_color;

    public Color() {
    }

    public Color(String user_color) {
        this.user_color = user_color;
    }

    public String getUser_color() {
        return user_color;
    }

    public void setUser_color(String user_color) {
        this.user_color = user_color;
    }
}

class Scoreboard {

    @SerializedName("user_id")
    @Expose
    private String userId;
    @SerializedName("user_score")
    @Expose
    private String userScore;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserScore() {
        return userScore;
    }

    public void setUserScore(String userScore) {
        this.userScore = userScore;
    }

}

class SquaresData{

    private Integer horizontal_id, vertical_id;

    private String color;

    public SquaresData() {
    }

    public SquaresData(Integer horizontal_id, Integer vertical_id, String color) {
        this.horizontal_id = horizontal_id;
        this.vertical_id = vertical_id;
        this.color = color;
    }

    public Integer getHorizontal_id() {
        return horizontal_id;
    }

    public void setHorizontal_id(Integer horizontal_id) {
        this.horizontal_id = horizontal_id;
    }

    public Integer getVertical_id() {
        return vertical_id;
    }

    public void setVertical_id(Integer vertical_id) {
        this.vertical_id = vertical_id;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}

interface APIServices{
    //@GET("get_scoreboard")
    //Call<List<String>> getDashList();
    @GET("get_user_score")
    Call<Integer> getUserScore(String uid);
    @GET("get_user_color")
    Call<Color> getUserColor(@Query("user_id") String uid);
    @POST("add_user")
    Call<String> sendUID(@Body String user_id);
    @POST("send_user_coordinates")
    Call<String> sendCoordinates(@Body SendCoordinates sendCoordinates);
    @GET("get_squares_data")
    Call<List<SquaresData>> getSquaresData();
}

public class Requests {

    private String BASE_URL = "http://10.79.10.64:8000/";

    private Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build();

    public APIServices apiServices = retrofit.create(APIServices.class);
}
