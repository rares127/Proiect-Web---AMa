package ama.models;

import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;

public class Abbreviation {
    private int id;
    private String name;
    private int languageId;
    private int domainId;
    private int userId;
    private String docbook;
    private String description;
    private int views;
    private int likes;
    private int favorites;
    private Timestamp createdAt;

    private Language language;
    private Domain domain;
    private User user;
    private List<String> meanings;

    public Abbreviation() {
        this.meanings = new ArrayList<>();
    }

    public Abbreviation(String name, int languageId, int domainId, int userId, String description) {
        this();
        this.name = name;
        this.languageId = languageId;
        this.domainId = domainId;
        this.userId = userId;
        this.description = description;
        this.views = 0;
        this.likes = 0;
        this.favorites = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLanguageId() {
        return languageId;
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public int getDomainId() {
        return domainId;
    }

    public void setDomainId(int domainId) {
        this.domainId = domainId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDocbook() {
        return docbook;
    }

    public void setDocbook(String docbook) {
        this.docbook = docbook;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getFavorites() {
        return favorites;
    }

    public void setFavorites(int favorites) {
        this.favorites = favorites;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<String> getMeanings() {
        return meanings;
    }

    public void setMeanings(List<String> meanings) {
        this.meanings = meanings;
    }

    @Override
    public String toString() {
        return "Abbreviation{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", views=" + views +
                ", likes=" + likes +
                ", favorites=" + favorites +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Abbreviation that = (Abbreviation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}