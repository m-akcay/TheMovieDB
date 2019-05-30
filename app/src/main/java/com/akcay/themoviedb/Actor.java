package com.akcay.themoviedb;

public class Actor {
    private String photoURL;
    private String fullname;
    private double popularity;

    public Actor(String fullname, String photoURL, double popularity)
    {
        this.photoURL = photoURL;
        this.fullname = fullname;
        this.popularity = popularity;
    }

    public String getPhotoURL() { return photoURL; }
    public String getFullname() { return fullname; }
    public double getPopularity() { return popularity; }
    @Override
    public String toString()
    {
        return String.format("%-25s %s\n", fullname, Double.toString(popularity));
    }

}
