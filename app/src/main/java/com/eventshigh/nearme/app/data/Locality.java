package com.eventshigh.nearme.app.data;

import java.util.ArrayList;
import java.util.List;

public enum  Locality {
    BANGALORE_INDIRANAGAR(City.BANGALORE, "Indira Nagar"),
    BANGALORE_JAYANAGAR(City.BANGALORE, "Jayanagar"),
    BANGALORE_KORAMANGALA(City.BANGALORE, "Koramangala"),
    BANGALORE_MG_ROAD(City.BANGALORE, "MG Road"),
    BANGALORE_JP_NAGAR(City.BANGALORE, "JP Nagar"),
    BANGALORE_ASHOK_NAGAR(City.BANGALORE, "Ashok Nagar"),
    BANGALORE_VASANTH_NAGAR(City.BANGALORE, "Vasanth Nagar"),
    BANGALORE_RESIDENCY_ROAD(City.BANGALORE, "Residency Road"),
    BANGALORE_HAL(City.BANGALORE, "HAL"),
    BANGALORE_WHITEFIELD(City.BANGALORE, "Whitefield"),
    BANGALORE_RICHMOND_TOWN(City.BANGALORE, "Richmond Town"),
    BANGALORE_MALLESHWARAM(City.BANGALORE, "Malleshwaram"),
    BANGALORE_HSR_LAYOUT(City.BANGALORE, "HSR Layout"),
    BANGALORE_BANASHANKARI(City.BANGALORE, "Banashankari"),
    BANGALORE_MAHADEVAPURA(City.BANGALORE, "Mahadevapura"),
    BANGALORE_ULSOOR(City.BANGALORE, "Ulsoor"),
    BANGALORE_HOSUR_ROAD(City.BANGALORE, "Hosur Road"),
    BANGALORE_BASAVANAGUDI(City.BANGALORE, "Basavanagudi"),
    BANGALORE_DOMLUR(City.BANGALORE, "Domlur"),
    DELHI_CONNAUGHT_PLACE(City.DELHI, "Connaught Place"),
    DELHI_GURGAON(City.DELHI, "Gurgaon"),
    DELHI_HAUZ_KHAS(City.DELHI, "Hauz Khas"),
    DELHI_NOIDA(City.DELHI, "Noida"),
    DELHI_LODHI_COLONY(City.DELHI, "Lodhi Colony"),
    DELHI_DIPLOMATIC_ENCLAVE(City.DELHI, "Diplomatic Enclave"),
    DELHI_NEW_FRIENDS_COLONY(City.DELHI, "New Friends Colony"),
    DELHI_SAKET(City.DELHI, "Saket"),
    DELHI_GREATER_KAILASH(City.DELHI, "Greater Kailash"),
    DELHI_VASANT_KUNJ(City.DELHI, "Vasant Kunj"),
    DELHI_DEFENCE_COLONY(City.DELHI, "Defence Colony"),
    DELHI_MANDI_HOUSE(City.DELHI, "Mandi House"),
    DELHI_UTTAM_NAGAR(City.DELHI, "Uttam Nagar"),
    DELHI_CHANAKYAPURI(City.DELHI, "Chanakyapuri"),
    DELHI_RAJOURI_GARDEN(City.DELHI, "Rajouri Garden"),
    DELHI_LAJPAT_NAGAR(City.DELHI, "Lajpat Nagar"),
    DELHI_PRAGATI_MAIDAN(City.DELHI, "Pragati Maidan"),
    DELHI_JANAKPURI(City.DELHI, "Janakpuri"),
    MUMBAI_ANDHERI(City.MUMBAI, "Andheri"),
    MUMBAI_BANDRA(City.MUMBAI, "Bandra"),
    MUMBAI_JUHU(City.MUMBAI, "Juhu"),
    MUMBAI_NARIMAN_POINT(City.MUMBAI, "Nariman Point"),
    MUMBAI_SENAPATI_BAPAT_MARG(City.MUMBAI, "Senapati Bapat Marg"),
    MUMBAI_POWAI(City.MUMBAI, "Powai"),
    MUMBAI_LOWER_PAREL(City.MUMBAI, "Lower Parel"),
    MUMBAI_VILE_PARLE(City.MUMBAI, "Vile Parle"),
    MUMBAI_DADAR(City.MUMBAI, "Dadar"),
    MUMBAI_MAHALAXMI(City.MUMBAI, "MahaLaxmi"),
    MUMBAI_SANTACRUZ(City.MUMBAI, "Santacruz"),
    MUMBAI_BORIVALI(City.MUMBAI, "Borivali"),
    MUMBAI_WORLI(City.MUMBAI, "Worli"),
    MUMBAI_WESTERN_EXPRESS_HIGHWAY(City.MUMBAI, "Western Express Highway"),
    MUMBAI_GOREGAON(City.MUMBAI, "Goregaon"),
    MUMBAI_JUHU_TARA_ROAD(City.MUMBAI, "Juhu Tara Road"),
    MUMBAI_FORT(City.MUMBAI, "Fort"),
    MUMBAI_LINKING_ROAD(City.MUMBAI, "Linking Road"),
    MUMBAI_BANDRA_KURLA_COMPLEX(City.MUMBAI, "Bandra Kurla Complex"),
    CHENNAI_ANNA_SALAI(City.CHENNAI, "Anna Salai"),
    CHENNAI_T_NAGAR(City.CHENNAI, "T. Nagar"),
    CHENNAI_IIT_MADRAS(City.CHENNAI, "IIT Madras"),
    CHENNAI_VELACHERY(City.CHENNAI, "Velachery"),
    CHENNAI_GUINDY(City.CHENNAI, "Guindy"),
    CHENNAI_NANDAMBAKKAM(City.CHENNAI, "Nandambakkam"),
    CHENNAI_MYLAPORE(City.CHENNAI, "Mylapore"),
    CHENNAI_CHETPET(City.CHENNAI, "Chetpet"),
    CHENNAI_THIRUVANMIYUR(City.CHENNAI, "Thiruvanmiyur"),
    CHENNAI_ALWARPET(City.CHENNAI, "Alwarpet"),
    CHENNAI_TRIPLICANE(City.CHENNAI, "Triplicane"),
    CHENNAI_ADYAR(City.CHENNAI, "Adyar"),
    CHENNAI_EGMORE(City.CHENNAI, "Egmore"),
    CHENNAI_ROYAPETTAH(City.CHENNAI, "Royapettah"),
    CHENNAI_NUNGAMBAKKAM(City.CHENNAI, "Nungambakkam"),
    CHENNAI_RAMAPURAM(City.CHENNAI, "Ramapuram"),
    CHENNAI_R_A_PURAM(City.CHENNAI, "R A Puram"),
    CHENNAI_TEYNAMPET(City.CHENNAI, "teynampet");


    public final City city;
    public final String name;

    Locality(City city, String name) {
        this.city = city;
        this.name = name;
    }

    public static List<Locality> getLocalities(City city,boolean isAll) {
        List<Locality> localities = new ArrayList<>();
        for (Locality locality : Locality.values()) {
            if (locality.city == city) {
                localities.add(locality);
                if(!isAll && localities.size() == 4){
                    return localities;
                }
            }
        }

        return localities;
    }



    public static Locality getLocality(String localityName) {
        String localityNameInUpperCase = localityName.toUpperCase();
        for (Locality locality : Locality.values()) {
            if (locality.name.equalsIgnoreCase(localityNameInUpperCase)) {
                return locality;
            }
        }

        return null;
    }



    public String getImageUrl() {
        return "https://assets.eventshigh.com/localities/" + toString().toLowerCase() + ".jpg";
    }

    public TrendingTopic asTrendingTopic() {
        return new TrendingTopic(name, getImageUrl(), null);
    }
}
