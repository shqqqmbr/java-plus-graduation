package ru.practicum.service;


import ru.practicum.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.avro.UserActionAvro;

import java.util.List;

public interface SimilarityService {

    List<EventSimilarityAvro> updateSimilarity(UserActionAvro userAction);

}