package com.example.vocablearning.Service;

import com.example.vocablearning.Entity.WordEntity;
import com.example.vocablearning.Model.Word;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class WordServiceImplement implements WordService {

    @Autowired
    private WordRepository wordRepository;
    private List<Word> wordList = new ArrayList<>();

    public WordServiceImplement(WordRepository wordRepository) {
        this.wordRepository = wordRepository;
    }


    public String createWord(Word word) {
        WordEntity wordEntity = new WordEntity();
        BeanUtils.copyProperties(word, wordEntity);
        wordRepository.save(wordEntity);
        // wordList.add(word);
        return "word added successfully";
    }

    public List<Word> readWords() {
        List<WordEntity> wordEntities = wordRepository.findAll();
        List<Word> words = new ArrayList<>();
        for (WordEntity entity : wordEntities) {
            Word word = new Word();
            BeanUtils.copyProperties(entity, word);
            words.add(word);
        }
        return words;
    }

// Data Flow:
//     Java Object (WordEntity) 
//         ↓ (via WordRepository.save())
//    SQL Table (words_db)
//         ↓ (via WordRepository.findAll())
// Java Objects (List<WordEntity>)


    public String updateWord(Word word) {
        // Implementation code here
        return "word updated successfully";
    }

    public Boolean deleteWord(String wordremove) {
        wordList.removeIf(word -> word.getWord().equalsIgnoreCase(wordremove));
        return true;
    }
}
