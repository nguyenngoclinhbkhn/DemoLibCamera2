package com.cpr.lib_camera2.model;

public class Image {
    private String name;
    private Long date;
    private String path;

    public Image(String name, Long date, String path) {
        this.name = name;
        this.date = date;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
