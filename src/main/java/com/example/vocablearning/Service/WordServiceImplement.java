package com.example.vocablearning.Service;

import com.example.vocablearning.Model.Word;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class WordServiceImplement implements WordService {

    private List<Word> wordList = new ArrayList<>();

    public String createWord(Word word) {
        wordList.add(word);
        return "word added successfully";
    }

    public List<Word> readWords() {
        return wordList;
    }

    public String updateWord(Word word) {
        // Implementation code here
        return "word updated successfully";
    }

    public Boolean deleteWord(String wordremove) {
        wordList.removeIf(word -> word.getWord().equalsIgnoreCase(wordremove));
        return true;
    }
}
