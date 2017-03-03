package postit.server.account;

/**
 * Created by Zhan on 2/28/2017.
 */
public class Location {
    double longitute;
    double latitude;

    // CONSTRUCTOR
    public Location(double longitute, double latitude) {
        this.longitute = longitute;
        this.latitude = latitude;
    }

    // GETTERS
    public double getLongitute() {
        return longitute;
    }

    public double getLatitude() {
        return latitude;
    }

    // SETTERS

    public void setLongitute(double longitute) {
        this.longitute = longitute;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}
