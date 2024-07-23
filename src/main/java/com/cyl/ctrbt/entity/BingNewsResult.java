package com.cyl.ctrbt.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class BingNewsResult {

    private String name;
    private String url;
    private Image image;
    private String description;
    private List<Provider> provider;
    private String datePublished;
    private String category;
    private List<About> about;

    @Getter
    @Setter
    @ToString
    public static class Image {
        private Thumbnail thumbnail;
    }

    @Getter
    @Setter
    @ToString
    public static class Thumbnail {
        private String contentUrl;
        private int width;
        private int height;
    }

    @Getter
    @Setter
    @ToString
    public static class Provider {
        private String _type;
        private String name;
        private Image image;
    }

    @Getter
    @Setter
    @ToString
    public static class About {
        private String readLink;
        private String name;
    }
}
