package com.example.vocablearning.Service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.vocablearning.Model.Word;

@Service
public class AIService {
    
    private final ChatClient chatClient;
    
    @Autowired
    public AIService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    public Word generateWordDetails(String word) {
        Word generatedWord = new Word();
        generatedWord.setWord(word);
        
        try {
            // Generate meaning
            String meaning = generateMeaning(word);
            generatedWord.setMeaning(meaning);
            
            // Generate sentence using the meaning
            String sentence = generateSentence(word, meaning);
            generatedWord.setSentence(sentence);
            
        } catch (Exception e) {
            // Fallback in case AI fails
            generatedWord.setMeaning("Definition for: " + word);
            generatedWord.setSentence("Example sentence with " + word + ".");
        }
        
        return generatedWord;
    }
    
    private String generateMeaning(String word) {
    String prompt = "Define the word '" + word + "' in simple, clear terms. " +
                   "Provide only the definition without any extra text.";
    
    String response = chatClient.prompt(prompt).call().content();
    return (response != null) ? response.trim() : "";
}

    private String generateSentence(String word, String meaning) {
        String prompt = "Create a simple, clear example sentence using the word '" + word + "' " +
                       "which means '" + meaning + "'. " +
                       "Return only the sentence without quotes or extra text. " +
                       "Make it natural and easy to understand.";

    String response = chatClient.prompt(prompt).call().content();
    return (response != null) ? response.trim() : "";
}

}
