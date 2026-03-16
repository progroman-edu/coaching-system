// This file implements Chess.com PubAPI calls and optional trainee rating sync.
package com.chesscoach.main.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chesscoach.main.dto.chesscom.ChessComRatingResponse;
import com.chesscoach.main.dto.chesscom.ChessComSyncRatingResponse;
import com.chesscoach.main.exception.ResourceNotFoundException;
import com.chesscoach.main.model.RatingsHistory;
import com.chesscoach.main.model.Trainee;
import com.chesscoach.main.repository.RatingsHistoryRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.ChessComService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class ChessComServiceImpl implements ChessComService {

    private final ObjectMapper objectMapper;
    private final TraineeRepository traineeRepository;
    private final RatingsHistoryRepository ratingsHistoryRepository;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Value("${app.chesscom.base-url:https://api.chess.com/pub}")
    private String baseUrl;

    @Value("${app.chesscom.user-agent:ChessCoachMain/1.0 (contact: coach@example.com)}")
    private String userAgent;

    @Value("${app.chesscom.timeout-seconds:15}")
    private int timeoutSeconds;

    public ChessComServiceImpl(
        ObjectMapper objectMapper,
        TraineeRepository traineeRepository,
        RatingsHistoryRepository ratingsHistoryRepository
    ) {
        this.objectMapper = objectMapper;
        this.traineeRepository = traineeRepository;
        this.ratingsHistoryRepository = ratingsHistoryRepository;
    }

    @Override
    public ChessComRatingResponse getRatings(String username) {
        JsonNode stats = fetchJson("/player/" + encode(username.toLowerCase()) + "/stats");
        System.out.println("DEBUG Chess.com stats for " + username + ": " + stats.toString());
        
        Integer rapidRating = getRating(stats, "chess_rapid");
        Integer blitzRating = getRating(stats, "chess_blitz");
        Integer bulletRating = getRating(stats, "chess_bullet");
        System.out.println("DEBUG ratings - rapid: " + rapidRating + ", blitz: " + blitzRating + ", bullet: " + bulletRating);
        
        ChessComRatingResponse response = new ChessComRatingResponse();
        response.setUsername(username);
        response.setRapid(rapidRating);
        response.setBlitz(blitzRating);
        response.setBullet(bulletRating);
        response.setPuzzleRush(getPuzzleRush(stats));
        return response;
    }

    @Override
    public JsonNode getArchives(String username) {
        return fetchJson("/player/" + encode(username.toLowerCase()) + "/games/archives");
    }

    @Override
    public JsonNode getMonthlyGames(String username, int year, int month) {
        String paddedMonth = String.format("%02d", month);
        return fetchJson("/player/" + encode(username.toLowerCase()) + "/games/" + year + "/" + paddedMonth);
    }

    @Override
    public JsonNode getAllModeHistory(String username, Integer limitArchives) {
        JsonNode archivesResponse = getArchives(username);
        JsonNode archives = archivesResponse.path("archives");

        ObjectNode result = objectMapper.createObjectNode();
        ObjectNode summary = objectMapper.createObjectNode();
        ObjectNode groupedGames = objectMapper.createObjectNode();
        groupedGames.putArray("rapid");
        groupedGames.putArray("blitz");
        groupedGames.putArray("bullet");
        groupedGames.putArray("daily");
        groupedGames.putArray("other");

        int requestedLimit = limitArchives == null ? Integer.MAX_VALUE : Math.max(1, limitArchives);
        int processedArchives = 0;
        int totalGames = 0;

        if (archives.isArray()) {
            for (int i = archives.size() - 1; i >= 0 && processedArchives < requestedLimit; i--) {
                JsonNode archiveNode = archives.get(i);
                if (!archiveNode.isTextual()) {
                    continue;
                }

                JsonNode monthData = fetchAbsoluteJson(archiveNode.asText());
                JsonNode games = monthData.path("games");
                if (!games.isArray()) {
                    processedArchives++;
                    continue;
                }

                for (JsonNode game : games) {
                    totalGames++;
                    String timeClass = game.path("time_class").asText("");
                    ArrayNode bucket = switch (timeClass) {
                        case "rapid" -> (ArrayNode) groupedGames.path("rapid");
                        case "blitz" -> (ArrayNode) groupedGames.path("blitz");
                        case "bullet" -> (ArrayNode) groupedGames.path("bullet");
                        case "daily" -> (ArrayNode) groupedGames.path("daily");
                        default -> (ArrayNode) groupedGames.path("other");
                    };
                    bucket.add(game);
                }

                processedArchives++;
            }
        }

        summary.put("username", username);
        summary.put("processedArchives", processedArchives);
        summary.put("totalArchivesAvailable", archives.isArray() ? archives.size() : 0);
        summary.put("totalGames", totalGames);

        result.set("summary", summary);
        result.set("groupedGames", groupedGames);
        return result;
    }

    @Override
    @Transactional
    public ChessComSyncRatingResponse syncTraineeRating(Long traineeId, String mode) {
        String normalizedMode = normalizeMode(mode);
        Trainee trainee = traineeRepository.findById(traineeId)
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + traineeId));

        if (trainee.getChessUsername() == null || trainee.getChessUsername().isBlank()) {
            throw new IllegalArgumentException("Trainee has no chessUsername configured");
        }

        String usernameToFetch = trainee.getChessUsername().toLowerCase();
        System.out.println("DEBUG syncTraineeRating - username from trainee: " + usernameToFetch);
        
        ChessComRatingResponse ratings = getRatings(usernameToFetch);
        String resolvedMode = normalizedMode;
        Integer target = resolveRatingByMode(ratings, normalizedMode);
        if (target == null) {
            for (String fallback : List.of("rapid", "blitz", "bullet")) {
                if (fallback.equals(normalizedMode)) {
                    continue;
                }
                target = resolveRatingByMode(ratings, fallback);
                if (target != null) {
                    resolvedMode = fallback;
                    break;
                }
            }
        }
        if (target == null) {
            throw new IllegalStateException("No rapid/blitz/bullet rating found for user " + trainee.getChessUsername());
        }

        int oldRating = trainee.getCurrentRating() != null ? trainee.getCurrentRating() : target;
        trainee.setCurrentRating(target);
        trainee.setCurrentRatingMode(resolvedMode);
        applyHighestByMode(trainee, resolvedMode, target);
        traineeRepository.save(trainee);
        recomputeRankings();

        RatingsHistory history = new RatingsHistory();
        history.setTrainee(trainee);
        history.setMatchResult(null);
        history.setOldRating(oldRating);
        history.setNewRating(target);
        history.setRatingChange(target - oldRating);
        history.setNotes("Synced from Chess.com " + resolvedMode + " rating");
        ratingsHistoryRepository.save(history);

        ChessComSyncRatingResponse response = new ChessComSyncRatingResponse();
        response.setTraineeId(traineeId);
        response.setChessUsername(trainee.getChessUsername());
        response.setMode(resolvedMode);
        response.setOldRating(oldRating);
        response.setNewRating(target);
        return response;
    }

    private void recomputeRankings() {
        List<Trainee> leaderboard = traineeRepository.findAllByOrderByCurrentRatingDescIdAsc();
        int rank = 1;
        for (Trainee trainee : leaderboard) {
            trainee.setRanking(rank++);
        }
        traineeRepository.saveAll(leaderboard);
    }

    private JsonNode fetchJson(String path) {
        return fetchJsonUri(URI.create(baseUrl + path));
    }

    private JsonNode fetchAbsoluteJson(String absoluteUrl) {
        return fetchJsonUri(URI.create(absoluteUrl));
    }

    private JsonNode fetchJsonUri(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(Math.max(1, timeoutSeconds)))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("DEBUG API URL: " + uri + " | Status: " + response.statusCode());
            if (response.statusCode() == 404) {
                throw new IllegalStateException("Chess.com player not found");
            }
            if (response.statusCode() == 429) {
                throw new IllegalStateException("Chess.com rate limit exceeded");
            }
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Chess.com API error: HTTP " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chess.com API call was interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to call Chess.com API: " + ex.getMessage(), ex);
        }
    }

    private static Integer getRating(JsonNode root, String nodeName) {
        if (root == null || root.isMissingNode()) {
            return null;
        }

        JsonNode modeNode = root.get(nodeName);
        if (modeNode == null || modeNode.isMissingNode()) {
            return null;
        }

        JsonNode lastNode = modeNode.get("last");
        if (lastNode == null || lastNode.isMissingNode()) {
            return null;
        }

        JsonNode ratingNode = lastNode.get("rating");
        if (ratingNode == null || !ratingNode.isNumber()) {
            return null;
        }

        return ratingNode.intValue();
    }

    private static Integer getPuzzleRush(JsonNode root) {
        JsonNode node = root.path("puzzle_rush").path("best").path("score");
        return node.isInt() ? node.intValue() : null;
    }

    private static String normalizeMode(String mode) {
        String value = mode == null ? "rapid" : mode.trim().toLowerCase();
        if (!value.equals("rapid") && !value.equals("blitz") && !value.equals("bullet")) {
            throw new IllegalArgumentException("Unsupported mode: " + mode + " (allowed: rapid, blitz, bullet)");
        }
        return value;
    }

    private static Integer resolveRatingByMode(ChessComRatingResponse ratings, String mode) {
        return switch (mode) {
            case "rapid" -> ratings.getRapid();
            case "blitz" -> ratings.getBlitz();
            case "bullet" -> ratings.getBullet();
            default -> null;
        };
    }

    private static void applyHighestByMode(Trainee trainee, String mode, int rating) {
        switch (mode) {
            case "rapid" -> trainee.setHighestRapidRating(maxValue(trainee.getHighestRapidRating(), rating));
            case "blitz" -> trainee.setHighestBlitzRating(maxValue(trainee.getHighestBlitzRating(), rating));
            case "bullet" -> trainee.setHighestBulletRating(maxValue(trainee.getHighestBulletRating(), rating));
            default -> {
            }
        }
    }

    private static int maxValue(Integer current, int candidate) {
        return current == null ? candidate : Math.max(current, candidate);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
