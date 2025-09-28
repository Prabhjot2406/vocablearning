package com.example.vocablearning.Model;

public class Word {
    
    private Long id;
    private String word;
    private String meaning;
    private String sentence;

    public Word(Long id, String word, String meaning, String sentence) {
        this.id = id;
        this.word = word;
        this.meaning = meaning;
        this.sentence = sentence;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }
}