package com.monew.dto.news;

public record NaverNewsItem(
    String title,
    String originallink,
    String link,
    String description,
    String pubDate
) {

}
