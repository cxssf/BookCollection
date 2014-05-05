package com.x22.bookcollection.app.model;

public class BookItem {
    private int id;
    private String title;
    private String author;

    public BookItem() {

    }

    public BookItem(String title, String author) {
        this(-1, title, author);
    }

    public BookItem(int id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return title;
    }
}
