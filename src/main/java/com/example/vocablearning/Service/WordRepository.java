package com.example.vocablearning.Service;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.vocablearning.Entity.WordEntity;

// Purpose: Provides functions to move objects into/from SQL table

@Repository
public interface WordRepository extends JpaRepository<WordEntity, Long> {

}
