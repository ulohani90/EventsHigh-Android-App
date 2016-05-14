package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by umesh on 09/05/16.
 */
public class MovieInfoObject implements Parcelable {
    private int id;
    private String name;
    private String img_url;
    private String director;
    private String duration;

    private ArrayList<String> cast;
    private ArrayList<String> genre;

    private long release_date;

    private String synopsis;

    private ArrayList<String> launguages;

    private String youtubeVideoId;

    private String certification;

    private int imdbRatingCount;
    private double imdbRatingValue;

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(img_url);
        dest.writeString(director);
        dest.writeString(duration);
        dest.writeLong(release_date);
        dest.writeString(synopsis);
        dest.writeStringList(cast);
        dest.writeStringList(genre);
        dest.writeStringList(launguages);
        dest.writeString(youtubeVideoId);
        dest.writeInt(imdbRatingCount);
        dest.writeDouble(imdbRatingValue);
        dest.writeString(certification);
    }

    public MovieInfoObject(Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.img_url = in.readString();
        this.director = in.readString();
        this.duration = in.readString();
        this.release_date = in.readLong();
        this.synopsis = in.readString();
        this.cast = new ArrayList<>();
        in.readStringList(cast);
        this.genre = new ArrayList<>();
        in.readStringList(genre);
        this.launguages = new ArrayList<>();
        in.readStringList(launguages);
        this.youtubeVideoId = in.readString();
        this.imdbRatingCount = in.readInt();
        this.imdbRatingValue = in.readDouble();
        this.certification = in.readString();
    }

    public MovieInfoObject(JSONObject obj) {
        try {

            this.id = obj.getInt("id");
            this.name = Utils.checkIfUnknown(obj.getString("name"));
            this.img_url = Utils.checkIfUnknown(obj.getString("img_url"));
            this.director = Utils.checkIfUnknown(obj.getString("director"));
            this.duration = Utils.checkIfUnknown(obj.getString("duration"));
            this.release_date = DateTimeUtils.parseMovieTime(obj.getString("release_date"));
            this.synopsis = Utils.checkIfUnknown(obj.getString("synopsis"));
            this.cast = new ArrayList<>();
            JSONArray castArray = obj.getJSONArray("cast");
            if (castArray != null) {
                for (int i = 0; i < castArray.length(); i++) {
                    this.cast.add(castArray.getString(i));
                }
            }
            this.genre = new ArrayList<>();
            JSONArray genreArray = obj.getJSONArray("genre");
            if (genreArray != null) {
                for (int i = 0; i < genreArray.length(); i++) {
                    this.genre.add(genreArray.getString(i));
                }
            }
            this.launguages = new ArrayList<>();
            JSONArray launguageArray = obj.getJSONArray("language");
            if (launguageArray != null) {
                for (int i = 0; i < launguageArray.length(); i++) {
                    launguages.add(launguageArray.getString(i));
                }
            }
            this.youtubeVideoId = Utils.checkIfUnknown(obj.getJSONObject("movie_attributes").getString("youtube_video"));

                this.imdbRatingCount = obj.getJSONObject("movie_attributes").getJSONObject("imdb_rating")
                        .getInt("rating_count");
                this.imdbRatingValue = obj.getJSONObject("movie_attributes").getJSONObject("imdb_rating")
                        .getDouble("rating_value");


            this.certification = Utils.checkIfUnknown(obj.getString("certification"));
        } catch (Exception e) {

        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImg_url() {
        return img_url;
    }

    public void setImg_url(String img_url) {
        this.img_url = img_url;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public ArrayList<String> getCast() {
        return cast;
    }

    public void setCast(ArrayList<String> cast) {
        this.cast = cast;
    }

    public ArrayList<String> getGenre() {
        return genre;
    }

    public void setGenre(ArrayList<String> genre) {
        this.genre = genre;
    }

    public long getRelease_date() {
        return release_date;
    }

    public void setRelease_date(long release_date) {
        this.release_date = release_date;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public ArrayList<String> getLaunguages() {
        return launguages;
    }

    public void setLaunguages(ArrayList<String> launguages) {
        this.launguages = launguages;
    }

    public String getYoutubeVideoId() {
        return youtubeVideoId;
    }

    public void setYoutubeVideoId(String youtubeVideoId) {
        this.youtubeVideoId = youtubeVideoId;
    }

    public Double getImdbRatingValue() {
        return imdbRatingValue;
    }

    public void setImdbRatingValue(Double imdbRatingValue) {
        this.imdbRatingValue = imdbRatingValue;
    }

    public int getImdbRatingCount() {
        return imdbRatingCount;
    }

    public void setImdbRatingCount(int imdbRatingValue) {
        this.imdbRatingValue = imdbRatingCount;
    }


    public String getCertification() {
        return certification;
    }

    public void setCertification(String certification) {
        this.certification = certification;

    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<MovieInfoObject> CREATOR =
            new Parcelable.Creator<MovieInfoObject>() {
                public MovieInfoObject createFromParcel(Parcel in) {
                    return new MovieInfoObject(in);

                }

                public MovieInfoObject[] newArray(int size) {
                    return new MovieInfoObject[size];
                }
            };

}