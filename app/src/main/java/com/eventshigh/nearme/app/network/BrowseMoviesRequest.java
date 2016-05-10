package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.stream.OfferObject;
import com.eventshigh.nearme.app.data.stream.PointsObject;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by umesh on 08/05/16.
 */
public class BrowseMoviesRequest extends JsonRequest<BrowseMoviesRequest.MovieBrowseListobject> {


    public static class MovieBrowseListobject {
        public final ArrayList<MovieDetailObject> movies;

        public final ArrayList<MovieDetailObject> upcomingMovies;

        public final ArrayList<String> languages;

        public MovieBrowseListobject(ArrayList<MovieDetailObject> movies, ArrayList<MovieDetailObject> upcomingMovies, ArrayList<String> languages) {
            this.movies = movies;
            this.upcomingMovies = upcomingMovies;
            this.languages = languages;
        }


    }


    public static void submit(Context context, City city, Priority priority,
                              Object tag, boolean shouldBypassCache, Response.Listener<MovieBrowseListobject> listener,
                              Response.ErrorListener errorListener) {

        String url = EventsHighEndpoints.getApiEndpointForMoviesList(city.name());

        BrowseMoviesRequest request = new BrowseMoviesRequest(
                context, url, shouldBypassCache, priority, listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }

    Context context;
    Priority priority;

    public BrowseMoviesRequest(Context context, String url, boolean shouldBypassCache, Priority priority, Response.Listener<MovieBrowseListobject> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(false);
        this.context = context;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<MovieBrowseListobject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonString);
            ArrayList<MovieDetailObject> movies = new ArrayList<>();
            JSONArray moviesArray = jsonObject.getJSONArray("movies");
            if(moviesArray!=null ){
                for(int i=0;i<moviesArray.length();i++){
                    movies.add(new MovieDetailObject(context,moviesArray.getJSONObject(i)));
                }
            }
            ArrayList<MovieDetailObject> upcomingMovies = new ArrayList<>();
            JSONArray upcomingMoviessArray = jsonObject.getJSONArray("coming_soon_movies");
            if(upcomingMoviessArray!=null ){
                for(int i=0;i<upcomingMoviessArray.length();i++){
                    upcomingMovies.add(new MovieDetailObject(context,upcomingMoviessArray.getJSONObject(i)));
                }
            }
            ArrayList<String> languages = new ArrayList<>();
            JSONArray languagesArray = jsonObject.getJSONArray("languages");
            if(languagesArray!=null){
                for(int i=0;i<languagesArray.length();i++){
                    languages.add(languagesArray.getJSONObject(i).getString("language"));
                }
            }

            return Response.success(new MovieBrowseListobject(movies,upcomingMovies,languages),
                    HttpHeaderParser.parseCacheHeaders(response));

        } catch (JSONException |UnsupportedEncodingException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }

    }

}
