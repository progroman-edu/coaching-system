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
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chesscoach.main.dto.chesscom.ChessComRatingResponse;
import com.chesscoach.main.dto.chesscom.ChessComSyncRatingResponse;
import com.chesscoach.main.exception.ResourceNotFoundException;
import com.chesscoach.main.model.BlitzRating;
import com.chesscoach.main.model.BulletRating;
import com.chesscoach.main.model.RapidRating;
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

    private static final Logger log = LoggerFactory.getLogger(ChessComServiceImpl.class);

    private final ObjectMapper objectMapper;
    private final TraineeRepository traineeRepository;
    private final RatingsHistoryRepository ratingsHistoryRepository;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Value("${app.chesscom.base-url:https://api.chess.com/pub}")
    private String baseUrl;

    @Value("${app.chesscom.user-agent}")
    private String userAgent;

    @Value("${app.chesscom.timeout-seconds:15}")
    private int timeoutSeconds;

    @Value("${app.chesscom.retry-max-attempts:3}")
    private int maxRetries;

    @Value("${app.chesscom.retry-backoff-multiplier:2}")
    private int backoffMultiplier;

    @Value("${app.rating.default:1200}")
    private int defaultRating;

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
    @Cacheable(value = "chesscom_ratings", key = "#username", unless = "#result == null")
    public ChessComRatingResponse getRatings(String username) {
        String normalizedUsername = normalizeUsername(username);
        log.info("Fetching Chess.com ratings for username: {}", normalizedUsername);
        
        try {
            JsonNode stats = fetchJsonWithRetry("player/" + encode(normalizedUsername) + "/stats");
            ChessComRatingResponse response = new ChessComRatingResponse();
            response.setUsername(normalizedUsername);
            
            // Current ratings
            response.setRapid(getRating(stats, "chess_rapid", "last"));
            response.setBlitz(getRating(stats, "chess_blitz", "last"));
            response.setBullet(getRating(stats, "chess_bullet", "last"));
            
            // Best/Peak ratings
            response.setRapidBest(getRating(stats, "chess_rapid", "best"));
            response.setBlitzBest(getRating(stats, "chess_blitz", "best"));
            response.setBulletBest(getRating(stats, "chess_bullet", "best"));
            
            response.setPuzzleRush(getPuzzleRush(stats));
            log.debug("Successfully fetched ratings for {}: rapid={}, blitz={}, bullet={}", 
                normalizedUsername, response.getRapid(), response.getBlitz(), response.getBullet());
            return response;
        } catch (Exception ex) {
            log.warn("Failed to fetch Chess.com ratings for {}: {}", normalizedUsername, ex.getMessage());
            return null;
        }
    }

    @Override
    public JsonNode getArchives(String username) {
        return fetchJson("player/" + encode(normalizeUsername(username)) + "/games/archives");
    }

    @Override
    public JsonNode getMonthlyGames(String username, int year, int month) {
        String paddedMonth = String.format("%02d", month);
        return fetchJson("player/" + encode(normalizeUsername(username)) + "/games/" + year + "/" + paddedMonth);
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
    @CacheEvict(value = "chesscom_ratings", key = "#traineeId")
    public ChessComSyncRatingResponse syncTraineeRating(Long traineeId, String mode) {
        String normalizedMode = normalizeMode(mode);
        Trainee trainee = traineeRepository.findById(traineeId)
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + traineeId));

        if (trainee.getChessUsername() == null || trainee.getChessUsername().isBlank()) {
            throw new IllegalArgumentException("Trainee has no chessUsername configured");
        }

        log.info("Syncing Chess.com ratings for trainee {} ({}), mode: {}", traineeId, trainee.getChessUsername(), normalizedMode);
        
        ChessComRatingResponse ratings = getRatings(trainee.getChessUsername());
        if (ratings == null) {
            log.warn("Failed to sync ratings: Chess.com API returned null for trainee {}", traineeId);
            throw new IllegalStateException("Failed to fetch ratings from Chess.com");
        }
        
        Integer rapid = ratings.getRapid();
        Integer rapidBest = ratings.getRapidBest();
        Integer blitz = ratings.getBlitz();
        Integer blitzBest = ratings.getBlitzBest();
        Integer bullet = ratings.getBullet();
        Integer bulletBest = ratings.getBulletBest();

        // Update all rating entities with both current and best ratings
        updateRapidRating(trainee, rapid, rapidBest);
        updateBlitzRating(trainee, blitz, blitzBest);
        updateBulletRating(trainee, bullet, bulletBest);

        // Determine target rating based on mode
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
            log.error("No rapid/blitz/bullet rating found for user {}", trainee.getChessUsername());
            throw new IllegalStateException("No rapid/blitz/bullet rating found for user " + trainee.getChessUsername());
        }

        int oldRating = getRatingForMode(trainee, resolvedMode);
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

        log.info("Successfully synced rating for trainee {}: {} -> {} ({})", traineeId, oldRating, target, resolvedMode);
        
        ChessComSyncRatingResponse response = new ChessComSyncRatingResponse();
        response.setTraineeId(traineeId);
        response.setChessUsername(trainee.getChessUsername());
        response.setMode(resolvedMode);
        response.setOldRating(oldRating);
        response.setNewRating(target);
        return response;
    }

    private void updateRapidRating(Trainee trainee, Integer rating, Integer bestRating) {
        RapidRating rapid = trainee.getRapidRating();
        if (rapid == null) {
            rapid = new RapidRating();
            rapid.setTrainee(trainee);
            trainee.setRapidRating(rapid);
        }
        if (rating != null) {
            rapid.setCurrentRating(rating);
        }
        if (bestRating != null) {
            if (rapid.getHighestRating() == null || bestRating > rapid.getHighestRating()) {
                rapid.setHighestRating(bestRating);
            }
        }
    }

    private void updateBlitzRating(Trainee trainee, Integer rating, Integer bestRating) {
        BlitzRating blitz = trainee.getBlitzRating();
        if (blitz == null) {
            blitz = new BlitzRating();
            blitz.setTrainee(trainee);
            trainee.setBlitzRating(blitz);
        }
        if (rating != null) {
            blitz.setCurrentRating(rating);
        }
        if (bestRating != null) {
            if (blitz.getHighestRating() == null || bestRating > blitz.getHighestRating()) {
                blitz.setHighestRating(bestRating);
            }
        }
    }

    private void updateBulletRating(Trainee trainee, Integer rating, Integer bestRating) {
        BulletRating bullet = trainee.getBulletRating();
        if (bullet == null) {
            bullet = new BulletRating();
            bullet.setTrainee(trainee);
            trainee.setBulletRating(bullet);
        }
        if (rating != null) {
            bullet.setCurrentRating(rating);
        }
        if (bestRating != null) {
            if (bullet.getHighestRating() == null || bestRating > bullet.getHighestRating()) {
                bullet.setHighestRating(bestRating);
            }
        }
    }

    private int getRatingForMode(Trainee trainee, String mode) {
        return switch (mode) {
            case "rapid" -> trainee.getRapidRating() != null && trainee.getRapidRating().getCurrentRating() != null 
                ? trainee.getRapidRating().getCurrentRating() : defaultRating;
            case "blitz" -> trainee.getBlitzRating() != null && trainee.getBlitzRating().getCurrentRating() != null 
                ? trainee.getBlitzRating().getCurrentRating() : defaultRating;
            case "bullet" -> trainee.getBulletRating() != null && trainee.getBulletRating().getCurrentRating() != null 
                ? trainee.getBulletRating().getCurrentRating() : defaultRating;
            default -> defaultRating;
        };
    }

    private void recomputeRankings() {
        List<Trainee> trainees = traineeRepository.findAll();
        trainees.sort((a, b) -> {
            int ratingA = getRatingForMode(a, "rapid");
            int ratingB = getRatingForMode(b, "rapid");
            if (ratingA != ratingB) {
                return Integer.compare(ratingB, ratingA);
            }
            return Long.compare(a.getId(), b.getId());
        });
        int rank = 1;
        for (Trainee trainee : trainees) {
            trainee.setRanking(rank++);
        }
        traineeRepository.saveAll(trainees);
    }

    private JsonNode fetchJson(String path) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        if (normalizedBaseUrl.endsWith("/player") && normalizedPath.startsWith("player/")) {
            normalizedPath = normalizedPath.substring("player/".length());
        }
        return fetchJsonUri(URI.create(normalizedBaseUrl + "/" + normalizedPath));
    }

    private JsonNode fetchAbsoluteJson(String absoluteUrl) {
        return fetchJsonUri(URI.create(absoluteUrl));
    }

    private JsonNode fetchJsonWithRetry(String path) {
        return fetchJsonUriWithRetry(URI.create(buildFullUrl(path)), 1);
    }

    private String buildFullUrl(String path) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        if (normalizedBaseUrl.endsWith("/player") && normalizedPath.startsWith("player/")) {
            normalizedPath = normalizedPath.substring("player/".length());
        }
        return normalizedBaseUrl + "/" + normalizedPath;
    }

    private JsonNode fetchJsonUriWithRetry(URI uri, int attemptNumber) {
        try {
            return fetchJsonUri(uri);
        } catch (Exception ex) {
            if (attemptNumber >= maxRetries) {
                log.error("All {} attempts failed for Chess.com API call to {}: {}", attemptNumber, uri, ex.getMessage());
                throw ex;
            }

            long delayMs = (long) Math.pow(backoffMultiplier, attemptNumber - 1) * 1000;
            log.warn("Chess.com API call failed (attempt {}/{}), retrying in {}ms: {}", 
                attemptNumber, maxRetries, delayMs, ex.getMessage());

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Retry sleep was interrupted", ie);
            }

            return fetchJsonUriWithRetry(uri, attemptNumber + 1);
        }
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
            
            log.debug("Making Chess.com API request to: {}", uri);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 500) {
                throw new IllegalStateException("Chess.com API server error: HTTP " + response.statusCode());
            }
            if (response.statusCode() >= 400) {
                log.warn("Chess.com API error: HTTP {} for {}", response.statusCode(), uri);
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

    private static Integer getRating(JsonNode root, String nodeName, String ratingType) {
        JsonNode node = root.path(nodeName).path(ratingType).path("rating");
        return node.isInt() ? node.intValue() : 0;
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

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String normalizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("username is required");
        }
        String normalized = username.trim();
        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("username is required");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
