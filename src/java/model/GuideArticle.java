package model;

import java.util.ArrayList;
import java.util.List;

public class GuideArticle {

    public static class Faq {

        private final String question;
        private final String answer;

        public Faq(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String getQuestion() {
            return question;
        }

        public String getAnswer() {
            return answer;
        }
    }

    public static class Source {

        private final String title;
        private final String url;
        private final boolean verified;

        public Source(String title, String url, boolean verified) {
            this.title = title;
            this.url = url;
            this.verified = verified;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public boolean isVerified() {
            return verified;
        }
    }

    private String id;
    private String categoryId;
    private String title;
    private String slug;
    private String difficulty;
    private int readingTime;
    private String summary;
    private String overview;
    private String whyItMatters;
    private String whenToDo;
    private String costs;
    private String realCase;
    private String verifyNote;
    private String actionLabel;
    private String actionUrl;
    private final List<String> targetModels = new ArrayList<>();
    private final List<String> keywords = new ArrayList<>();
    private final List<String> steps = new ArrayList<>();
    private final List<String> checklist = new ArrayList<>();
    private final List<String> commonMistakes = new ArrayList<>();
    private final List<Faq> faqs = new ArrayList<>();
    private final List<Source> sources = new ArrayList<>();
    private String searchText = "";

    public boolean isNeedsVerification() {
        if (verifyNote != null && !verifyNote.isEmpty()) {
            return true;
        }
        for (Source s : sources) {
            if (!s.isVerified()) {
                return true;
            }
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getReadingTime() {
        return readingTime;
    }

    public void setReadingTime(int readingTime) {
        this.readingTime = readingTime;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getWhyItMatters() {
        return whyItMatters;
    }

    public void setWhyItMatters(String whyItMatters) {
        this.whyItMatters = whyItMatters;
    }

    public String getWhenToDo() {
        return whenToDo;
    }

    public void setWhenToDo(String whenToDo) {
        this.whenToDo = whenToDo;
    }

    public String getCosts() {
        return costs;
    }

    public void setCosts(String costs) {
        this.costs = costs;
    }

    public String getRealCase() {
        return realCase;
    }

    public void setRealCase(String realCase) {
        this.realCase = realCase;
    }

    public String getVerifyNote() {
        return verifyNote;
    }

    public void setVerifyNote(String verifyNote) {
        this.verifyNote = verifyNote;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public List<String> getTargetModels() {
        return targetModels;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public List<String> getSteps() {
        return steps;
    }

    public List<String> getChecklist() {
        return checklist;
    }

    public List<String> getCommonMistakes() {
        return commonMistakes;
    }

    public List<Faq> getFaqs() {
        return faqs;
    }

    public List<Source> getSources() {
        return sources;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
}
