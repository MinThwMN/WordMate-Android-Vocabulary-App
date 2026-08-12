package com.example.engapp.models;

public class Word {
    private String wordId;
    private String topicId;
    private String word;
    private String ipa;
    private Meaning meaning;
    private String example;
    private String type;

    public Word() {
    }

    public Word(String wordId, String topicId, String word, String ipa,
                String meaning, String example, String type) {
        this.wordId = wordId;
        this.topicId = topicId;
        this.word = word;
        this.ipa = ipa;
        this.meaning = Meaning.fromSingleMeaning(meaning);
        this.example = example;
        this.type = type;
    }

    public Word(String wordId, String topicId, String word, String ipa,
                Meaning meaning, String example, String type) {
        this.wordId = wordId;
        this.topicId = topicId;
        this.word = word;
        this.ipa = ipa;
        this.meaning = meaning;
        this.example = example;
        this.type = type;
    }

    public String getWordId() {
        return wordId;
    }

    public String getTopicId() {
        return topicId;
    }

    public String getWord() {
        return word;
    }

    public String getIpa() {
        return ipa;
    }

    /*
     * Giữ hàm này để code cũ không bị lỗi.
     * Mặc định trả về tiếng Việt.
     */
    public String getMeaning() {
        return getMeaningByLanguage("vi");
    }

    public Meaning getMeaningObject() {
        return meaning;
    }

    public String getMeaningByLanguage(String languageCode) {
        if (meaning == null) {
            return "";
        }

        if (languageCode == null || languageCode.trim().isEmpty()) {
            languageCode = "vi";
        }

        switch (languageCode) {
            case "en":
                return meaning.getEn();

            case "ja":
                return meaning.getJa();

            case "ko":
                return meaning.getKo();

            case "vi":
            default:
                return meaning.getVi();
        }
    }

    public String getExample() {
        return example;
    }

    public String getType() {
        return type;
    }

    public void setWordId(String wordId) {
        this.wordId = wordId;
    }

    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public void setIpa(String ipa) {
        this.ipa = ipa;
    }

    public void setMeaning(String meaning) {
        this.meaning = Meaning.fromSingleMeaning(meaning);
    }

    public void setMeaning(Meaning meaning) {
        this.meaning = meaning;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public void setType(String type) {
        this.type = type;
    }
}