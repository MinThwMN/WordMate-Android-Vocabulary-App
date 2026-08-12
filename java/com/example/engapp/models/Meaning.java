package com.example.engapp.models;

public class Meaning {
    private String vi;
    private String en;
    private String ja;
    private String ko;

    public Meaning() {
    }

    public Meaning(String vi, String en, String ja, String ko) {
        this.vi = vi;
        this.en = en;
        this.ja = ja;
        this.ko = ko;
    }

    public static Meaning fromSingleMeaning(String meaning) {
        return new Meaning(meaning, meaning, meaning, meaning);
    }

    public String getVi() {
        return vi != null ? vi : "";
    }

    public String getEn() {
        return en != null ? en : "";
    }

    public String getJa() {
        return ja != null ? ja : "";
    }

    public String getKo() {
        return ko != null ? ko : "";
    }

    public void setVi(String vi) {
        this.vi = vi;
    }

    public void setEn(String en) {
        this.en = en;
    }

    public void setJa(String ja) {
        this.ja = ja;
    }

    public void setKo(String ko) {
        this.ko = ko;
    }
}