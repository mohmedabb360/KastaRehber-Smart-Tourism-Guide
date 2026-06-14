package com.example.turist_rehberi;

public class PlaceModel {
    private String id;
    private String name;
    private String category;
    private String description;
    private String budget;
    private String idealTime;
    private int durationMinutes;
    private int effortLevel;
    private double lat;
    private double lng;

    // 🚀 الإضافة الجديدة: 3 صور بدل صورة وحدة
    private String imageUrl1;
    private String imageUrl2;
    private String imageUrl3;

    // 🚀 الإضافة الجديدة: متغير لحفظ حالة إنجاز الزيارة
    private boolean isVisited;

    public PlaceModel() {
        // مطلوب للفايربيس
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }

    public String getIdealTime() { return idealTime; }
    public void setIdealTime(String idealTime) { this.idealTime = idealTime; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getEffortLevel() { return effortLevel; }
    public void setEffortLevel(int effortLevel) { this.effortLevel = effortLevel; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public String getImageUrl1() { return imageUrl1; }
    public void setImageUrl1(String imageUrl1) { this.imageUrl1 = imageUrl1; }

    public String getImageUrl2() { return imageUrl2; }
    public void setImageUrl2(String imageUrl2) { this.imageUrl2 = imageUrl2; }

    public String getImageUrl3() { return imageUrl3; }
    public void setImageUrl3(String imageUrl3) { this.imageUrl3 = imageUrl3; }

    public boolean isVisited() { return isVisited; }
    public void setVisited(boolean visited) { isVisited = visited; }
}