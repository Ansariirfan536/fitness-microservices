package com.fitness.activityservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.activityservice.UserNotFoundException;
import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserValidationService userValidationService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Hardcoded topic name to prevent any property loading issues
    private static final String TOPIC_NAME = "fitness-activity-topic";

    public ActivityResponse trackActivity(ActivityRequest request) {
        //boolean isValidUser = userValidationService.validateUser(request.getUserId());
        boolean isValidUser = true;

        if (!isValidUser) {
            throw new UserNotFoundException("Invalid user: " + request.getUserId());
        }

        Activity activity = Activity.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .duration(request.getDuration())
                .caloriesBurned(request.getCaloriesBurned())
                .startTime(request.getStartTime())
                .additionalMetrics(request.getAdditionalMetrics())
                .build();

        Activity savedActivity = activityRepository.save(activity);
        log.info("Activity saved in DB, now sending JSON string to Kafka topic: {}", TOPIC_NAME);


        try {
            String jsonPayload = objectMapper.writeValueAsString(savedActivity);
            kafkaTemplate.send(TOPIC_NAME, savedActivity.getUserId(), jsonPayload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Kafka send success! Partition: {}, Offset: {}",
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Kafka send failed: ", ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error while sending to Kafka: ", e);
        }

        return mapToResponse(savedActivity);
    }

    private ActivityResponse mapToResponse(Activity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setUserId(activity.getUserId());
        response.setType(activity.getType());
        response.setDuration(activity.getDuration());
        response.setCaloriesBurned(activity.getCaloriesBurned());
        response.setStartTime(activity.getStartTime());
        response.setAdditionalMetrics(activity.getAdditionalMetrics());
        response.setCreatedAt(activity.getCreatedAt());
        response.setUpdatedAt(activity.getUpdatedAt());
        return response;
    }

    public List<ActivityResponse> getUserActivities(String userId) {
        // Agar userId null ya empty hai toh 500 error ke bajaye khali list return karein
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Activity> activityList = activityRepository.findByUserId(userId);
        return activityList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ActivityResponse getActivityById(String id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + id));
        return mapToResponse(activity);
    }
}