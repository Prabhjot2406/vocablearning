package com.example.vocablearning.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;



import com.example.vocablearning.Model.Word;
import com.example.vocablearning.Service.AIService;
import com.example.vocablearning.Service.WordService;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {


    private final WordService wordService;
    private final AIService aiservice;
    private List<Word> wordList = new ArrayList<>(); // In-memory list to store words
    

    // Dependency injection of WordService
    // @Autowired
    // WordService wordService;
    public HomeController(WordService wordService, AIService aiservice) { // Constructor injection
        this.wordService = wordService;
        this.aiservice = aiservice;
    }


    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/get-data")
    public String getData() {
        return "get-data";
    }

    @GetMapping("/add-word")
    public String addData(Model model) {
        {
            model.addAttribute("word", new Word(1L,"Jubilant", "Expressing great happiness", "The jubilant crowd cheered loudly.")); // Add a Word object to the model
            return "add-word";
        }
    }

    @GetMapping("/get-word-list")
    @ResponseBody
    public List<Word> getWordList() {
        return wordService.readWords(); // Return the list of words as a string
    }

    @PostMapping("/add-word")
    @ResponseBody
    public List<Word> addWord(@ModelAttribute Word word, Model model) {

        model.addAttribute("word", word); 

        wordService.createWord(word); // Call the createWord method to save the word


        // wordList.add(word); // Add the word to the in-memory list
        System.out.println("Word: " + word.getWord());
        System.out.println("Meaning: " + word.getMeaning());
        System.out.println("Sentence: " + word.getSentence());
        wordList.add(word);
        return wordService.readWords(); // Return the list of words as a string
    }

    @PostMapping("/delete-word")  // need to add new button in html
    @ResponseBody
    public Boolean deleteWord(@RequestParam String word) {
        return wordService.deleteWord(word);
    }

    @PostMapping("/generate-word-details")
    @ResponseBody
    public Word generateWordDetails(@RequestParam String word) {
        
            return aiservice.generateWordDetails(word);

    }
}
