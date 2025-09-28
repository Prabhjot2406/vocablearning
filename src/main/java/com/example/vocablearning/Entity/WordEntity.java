package com.example.vocablearning.Entity ;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;

@Data
@Entity
@Table(name = "words_db")
public class WordEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String word;
    private String meaning;
    private String sentence;
}
