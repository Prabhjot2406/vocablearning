package com.example.vocablearning.Service;

import java.util.List;

import com.example.vocablearning.Model.Word;

public interface WordService {

    String createWord(Word word);
    List<Word> readWords();
    String updateWord(Word word);
    Boolean deleteWord(String word);

}
