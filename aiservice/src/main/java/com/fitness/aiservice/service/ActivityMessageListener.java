
package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.respository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListener {

    private final ActivityAIService activityAIService;
    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper; // ⚡ ObjectMapper inject kar liya

    @KafkaListener(topics = "${kafka.topic.name}", groupId = "activity-processor-group")
    public void processActivity(String activityJson) {
        log.info("Received Activity JSON from Kafka: {}", activityJson);
        try {
            Activity activity = objectMapper.readValue(activityJson, Activity.class);

            // ⚡ Yahan check kar ki activity object sahi se map ho raha hai ya nahi
            log.info("Parsed Activity ID: {}, Type: {}", activity.getId(), activity.getType());

            Recommendation recommendation = activityAIService.generateRecommendation(activity);
            recommendationRepository.save(recommendation);
            log.info("Recommendation successfully generated and saved for activity ID: {}", activity.getId());
        } catch (Exception e) {
            // ⚡ Yahan error ko clearly print karvao taaki pata chale kahan fat raha hai
            log.error("CRITICAL ERROR processing activity from Kafka: ", e);
            e.printStackTrace();
        }

    }
}