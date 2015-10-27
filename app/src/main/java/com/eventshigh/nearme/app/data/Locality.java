package com.eventshigh.nearme.app.data;

import java.util.ArrayList;
import java.util.List;

public enum  Locality {
    BANGALORE_INDIRANAGAR(City.BANGALORE, "Indira Nagar"),
    BANGALORE_JAYANAGAR(City.BANGALORE, "Jayanagar"),
    BANGALORE_KORAMANGALA(City.BANGALORE, "Koramangala"),
    BANGALORE_MG_ROAD(City.BANGALORE, "MG Road"),
    DELHI_CONNAUGHT_PLACE(City.DELHI, "Connaught Place"),
    DELHI_GURGAON(City.DELHI, "Gurgaon"),
    DELHI_HAUZ_KHAS(City.DELHI, "Hauz Khas"),
    DELHI_NOIDA(City.DELHI, "Noida"),
    MUMBAI_ANDHERI(City.MUMBAI, "Andheri"),
    MUMBAI_BANDRA(City.MUMBAI, "Bandra"),
    MUMBAI_JUHU(City.MUMBAI, "Juhu"),
    MUMBAI_NARIMAN_POINT(City.MUMBAI, "Nariman Point");

    public final City city;
    public final String name;

    Locality(City city, String name) {
        this.city = city;
        this.name = name;
    }

    public static List<Locality> getLocalities(City city) {
        List<Locality> localities = new ArrayList<>();
        for (Locality locality : Locality.values()) {
            if (locality.city == city) {
                localities.add(locality);
            }
        }

        return localities;
    }

    public String getImageUrl() {
        return "https://assets.eventshigh.com/localities/" + toString().toLowerCase() + ".jpg";
    }

    public TrendingTopic asTrendingTopic() {
        return new TrendingTopic(name, getImageUrl(), null);
    }
}
