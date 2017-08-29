package com.example.android.newsapp;

/**
 * Class to store the news data
 */
class News {
    private final String mTitle;
    private final String mSection;
    private final String mAuthor;
    private final String mDate;
    private final String mUrl;

    News(String mTitle, String mSection, String mUrl, String mAuthor, String mDate) {
        this.mTitle = mTitle;
        this.mSection = mSection;
        this.mUrl = mUrl;
        this.mAuthor = mAuthor;
        this.mDate = mDate;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSection() {
        return mSection;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getDate() {
        return mDate;
    }

    public String getUrl() {
        return mUrl;
    }
}
