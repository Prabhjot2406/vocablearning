package com.example.vocablearning.Entity ;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;

// Purpose: Maps Java object ↔ SQL table structure

@Data
@Entity //// Java Object Relational Mapping (ORM) to represent a database entity
// JPA needs @Entity annotations to map to database

// ↕️ JPA Maps This entity object To database table. ↕️

@Table(name = "words_db") // Map this entity to the "words_db" table in the database
public class WordEntity {  //jpa says there should be no logic in entity class
    
    @Id // Primary key of the entity
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generate ID values
    private Long id;

    private String word;
    private String meaning;
    private String sentence;
}