package com.eventshigh.nearme.app.data;

import com.eventshigh.nearme.app.utils.Utils;

/**
 * Represents the City as supported by EventsHigh.
 */
public enum City {
    BANGALORE("GMT+0530", "IN"),
    DELHI("GMT+0530", "IN"),
    MUMBAI("GMT+0530", "IN");

    public final String timeZone;
    public final String countryCode;

    City(String timeZone, String countryCode) {
        this.timeZone = timeZone;
        this.countryCode = countryCode;
    }

    public static City getCity(String cityName) {
        String cityNameInUpperCase = cityName.toUpperCase();
        for (City city : City.values()) {
            if (city.name().equals(cityNameInUpperCase)) {
                return city;
            }
        }

        return null;
    }

    public static String[] getValuesAsString() {
        City[] cities = values();
        String[] cityNames = new String[cities.length];
        for (int i = 0; i < cities.length; i++) {
            cityNames[i] = Utils.capitalize(cities[i].name());
        }

        return cityNames;
    }
}
