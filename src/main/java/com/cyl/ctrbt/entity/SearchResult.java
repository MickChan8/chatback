package com.cyl.ctrbt.entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchResult {
    private String title;
    private String url;
    private String summary;

    public SearchResult(String title, String url, String summary) {
        setTitle(title);
        setUrl(url);
        setSummary(summary);
    }

}
