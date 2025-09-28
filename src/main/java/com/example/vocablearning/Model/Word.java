package com.example.vocablearning.Model;

public class Word {
    private String word;
    private String meaning;
    private String sentence;

    public Word(String word, String meaning, String sentence) {
        this.word = word;
        this.meaning = meaning;
        this.sentence = sentence;
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