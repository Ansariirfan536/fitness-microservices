
package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public Recommendation generateRecommendation(Activity activity) {
        String prompt = createPromptForActivity(activity);
        log.info("Sending structured prompt to Gemini for User: {}", activity.getUserId());

        String aiResponse = geminiService.getRecommendations(prompt);
        //log.info("RESPONSE FROM AI: \n{}", aiResponse);

        System.out.println("Generate AI Response Successfully Please Check  \uD83C\uDF89 \uD83C\uDF89 \uD83D\uDE0D");

        return processAIResponse(activity, aiResponse);
    }

    private Recommendation processAIResponse(Activity activity, String aiResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(aiResponse);
            JsonNode textNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .get("parts")
                    .get(0)
                    .path("text");

            String jsonContent = textNode.asText()
                    .replaceAll("```json\\n", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode analysisJson = objectMapper.readTree(jsonContent);
            JsonNode analysisNode = analysisJson.path("analysis");
            StringBuilder fullAnalysis = new StringBuilder();
            addAnalysisSection(fullAnalysis, analysisNode, "overall", "Overall:");
            addAnalysisSection(fullAnalysis, analysisNode, "pace", "Pace :");
            addAnalysisSection(fullAnalysis, analysisNode, "heartRate", "Heart Rate:");
            addAnalysisSection(fullAnalysis, analysisNode, "caloriesBurned", "Calories Burned:");

            List<String> improvements = extractImprovements(analysisJson.path("improvements"));
            List<String> suggestions = extractSuggestions(analysisJson.path("suggestions"));
            List<String> safety = extractGuidlines(analysisJson.path("safety"));

            return Recommendation.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .type(activity.getType().toString())
                    .recommendation(fullAnalysis.toString().trim())
                    .improvements(improvements)
                    .suggestions(suggestions)
                    .safety(safety)
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error processing AI response: ", e);
            return createDefaultRecommendation(activity);
        }
    }

    private Recommendation createDefaultRecommendation(Activity activity) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .type(activity.getType().toString())
                .recommendation("Unable to generate detailed analysis")
                .improvements(Collections.singletonList("Continue with your current routine"))
                .suggestions(Collections.singletonList("Consider consulting a fitness consultant"))
                .safety(Arrays.asList("Always warm up before exercise", "Stay hydrated"))
                .createdAt(LocalDateTime.now())
                .build();
    }


    private List<String> extractGuidlines(JsonNode safetyNode) {
        List<String> safety = new ArrayList<>();
        if (safetyNode.isArray()) {
            safetyNode.forEach(item -> safety.add(item.asText()));
        }
        return safety.isEmpty() ? Collections.singletonList("Follow safety guidelines") : safety;
    }

    private List<String> extractSuggestions(JsonNode suggestionsNode) {
        List<String> suggestions = new ArrayList<>();
        if (suggestionsNode.isArray()) {
            suggestionsNode.forEach(suggestion -> {
                String workout = suggestion.path("workout").asText();
                String description = suggestion.path("description").asText();
                suggestions.add(String.format("%s: %s", workout, description));
            });
        }
        return suggestions.isEmpty() ? Collections.singletonList("No specific suggestion provided") : suggestions;
    }

    private List<String> extractImprovements(JsonNode improvementsNode) {
        List<String> improvements = new ArrayList<>();
        if (improvementsNode.isArray()) {
            improvementsNode.forEach(improvement -> {
                String area = improvement.path("area").asText();
                String detail = improvement.path("recommendation").asText();
                improvements.add(String.format("%s: %s", area, detail));
            });
        }
        return improvements.isEmpty() ? Collections.singletonList("No specific improvements provided") : improvements;
    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prifix) {
        if (!analysisNode.path(key).isMissingNode()) {
            fullAnalysis.append(prifix)
                    .append(analysisNode.path(key).asText())
                    .append("\n\n");
        }
    }

    private String createPromptForActivity(Activity activity) {
        String metricsString = (activity.getAdditionalMetrics() != null && !activity.getAdditionalMetrics().isEmpty())
                ? activity.getAdditionalMetrics().toString()
                : "None";

        return String.format("""
            {
              "analysis": {
                "overall": "Provide a very encouraging, friendly, and motivational breakdown of their workout performance here.",
                "pace": "Analyze their consistency or speed based on duration.",
                "heartRate": "Analyze heart rate intensity if available, or provide general cardiovascular feedback.",
                "caloriesBurned": "Analyze calorie burning efficiency."
              },
              "improvements": [
                {
                  "area": "Name of area needing attention (e.g., Stamina, Posture, Breathing)",
                  "recommendation": "Detailed friendly tip on how to improve this area."
                }
              ],
              "suggestions": [
                {
                  "workout": "Name of the next recommended exercise or workout variant",
                  "description": "Fun and descriptive explanation of how to do it."
                }
              ],
              "safety": [
                "Safety and recovery tip 1 (e.g., Hydration, Stretching)",
                "Safety and recovery tip 2"
              ]
            }

            Analyze this activity:
            Activity Type: %s
            Duration: %d minutes
            Calories Burned: %d
            Additional Metrics: %s

            Provide detailed analysis focusing on performance, improvements, next workout recommendations, and safety tips.
            Ensure the response follows the EXACT JSON format shown above. Do not wrap the response in markdown blocks like ```json or ```, return ONLY the raw JSON string.
            Use a highly supportive, human-like personal trainer tone inside the JSON fields (Feel free to use Hindi/Hinglish keywords to connect organically with Indian users).
            """,
                activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurned(),
                metricsString
        );
    }
}