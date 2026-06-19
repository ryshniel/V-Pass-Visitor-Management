package com.example.v_pass;

public class VisitorResponse {
    private String status;
    private String visitor_id;
    private String name;
    private String plate_number;
    private String message;
    private String photo_url;

    // Getters to fetch the data in your Activities
    public String getStatus() { return status; }
    public String getVisitorId() { return visitor_id; }
    public String getName() { return name; }
    public String getPlateNumber() { return plate_number; }
    public String getMessage() { return message; }
    public String getPhotoUrl() { return photo_url; }
}