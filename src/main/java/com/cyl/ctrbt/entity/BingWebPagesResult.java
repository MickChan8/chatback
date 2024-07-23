package com.cyl.ctrbt.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BingWebPagesResult {
    private String id;
    private String name;
    private String url;
    private boolean isFamilyFriendly;
    private String displayUrl;
    private String snippet;
    private String dateLastCrawled;
    private String cachedPageUrl;
    private String language;
    private boolean isNavigational;
    private boolean noCache;
}


