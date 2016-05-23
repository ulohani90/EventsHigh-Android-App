package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.eventshigh.nearme.app.user.UserActionHelper;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by umesh on 13/05/16.
 * <p/>
 * Manages the user personalization -- his favourite and dismissed movies. The favourite or
 * dismissed information is stored as {@link MovieMark}. {@link MovieMark} can be updated
 * through {@link Editor} class.
 * <p/>
 * It is very efficient to check for movie mark as it is kept in-memory and backed by
 * DB for persistent storage.
 */
public class MovieMarkerManager {
    /**
     * Movie can be marked as favourite or dismissed by user. This enum lists marks supported.
     * To simplify the DB storage, each mark has int value which is used while persisting
     * to DB.
     */
    public enum MovieMark {
        FAVOURITE(1),
        DISMISSED(2);

        public final int value;

        MovieMark(int value) {
            this.value = value;
        }

        public static boolean isFavourite(@Nullable MovieMark movieMark) {
            return movieMark != null && movieMark == FAVOURITE;
        }

        public static
        @Nullable
        MovieMark getPrefFromValue(int value) {
            for (MovieMark pref : MovieMark.values()) {
                if (pref.value == value) {
                    return pref;
                }
            }

            return null;
        }
    }

    /**
     * Supports the modification for Movie marks. Each instance of editor should be
     * closed after its not needed.
     */
    public class Editor implements Closeable {
        private final SQLiteDatabase database;
        private final List<Thread> threads = new ArrayList<>();
        private final Context context;

        private Editor(Context context) {
            database = new MovieMarkDbHelper(context).getWritableDatabase();
            this.context = context;
        }

        public void close() {
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    // do nothing.
                }
            }
            database.close();
        }

        public Editor recordMovieMark(MovieInfoObject movie, @Nullable MovieMark mark) {
            if (mark == null) {
                removeMovieMark(movie);
            } else {
                movieMarkMap.put(movie.getId() + "", mark);

                threads.add(MovieMarkDbHelper.addEntry(database, movie.getId() + "", mark));
            }
            return this;
        }

        public Editor removeMovieMark(MovieInfoObject movie) {
            MovieMark mark = movieMarkMap.remove(movie.getId() + "");
            if (MovieMark.isFavourite(mark)) {
                new UserActionHelper(context).recordMovieAction(
                        UserActionHelper.MovieAction.REMOVE_FAVORITE, movie.getId() + "");
            }
            threads.add(MovieMarkDbHelper.removeEntry(database, movie.getId() + ""));
            return this;
        }

        public Editor removeMovieMark(String movieId) {
            MovieMark mark = movieMarkMap.remove(movieId);
            if (MovieMark.isFavourite(mark)) {
                new UserActionHelper(context).recordMovieAction(
                        UserActionHelper.MovieAction.REMOVE_FAVORITE, movieId);
            }
            threads.add(MovieMarkDbHelper.removeEntry(database, movieId + ""));
            return this;
        }
    }


    /**
     * Singleton Instance.
     */
    private static MovieMarkerManager instance;

    public static synchronized MovieMarkerManager getInstance(Context context) {
        if (instance == null) {
            instance = new MovieMarkerManager(context);
        }

        return instance;
    }

    private final Context context;
    private final Map<String, MovieMark> movieMarkMap = new ConcurrentHashMap<>();
    private boolean loaded = false;

    private MovieMarkerManager(Context context) {
        this.context = context.getApplicationContext();

        // Start new thread to load movies.
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    refreshListingFromDb();
                    loaded = true;
                    this.notifyAll();
                }
            }
        }).start();
    }

    // Waits for data to be loaded from DB.
    public synchronized void waitForLoading() {
        while (!loaded) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                // ignore.
            }
        }
    }

    public
    @Nullable
    MovieMark getMovieMark(String movieId) {
        return movieMarkMap.get(movieId);
    }

    public Editor getEditor() {
        return new Editor(context);
    }

    public boolean isFavourite(String movieId) {
        return MovieMark.isFavourite(getMovieMark(movieId));
    }

    public List<String> getFavouritedMovies() {
        List<String> favouritedMovies = new ArrayList<>();
        for (Map.Entry<String, MovieMark> entry : movieMarkMap.entrySet()) {
            if (MovieMark.isFavourite(entry.getValue())) {
                favouritedMovies.add(entry.getKey());
            }
        }

        return favouritedMovies;
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private void refreshListingFromDb() {
        SQLiteDatabase database = new MovieMarkDbHelper(this.context).getReadableDatabase();
        try {
            movieMarkMap.clear();
            for (Pair<String, MovieMark> entry : MovieMarkDbHelper.fetchAllEntries(database)) {
                if (entry.second != null) {
                    movieMarkMap.put(entry.first, entry.second);
                }
            }
        } finally {
            database.close();
        }
    }
}
