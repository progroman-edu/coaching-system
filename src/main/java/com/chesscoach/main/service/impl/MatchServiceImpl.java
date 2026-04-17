// This service implementation contains business logic for Match operations.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.dto.match.MatchCreateRequest;
import com.chesscoach.main.dto.match.MatchGenerationRequest;
import com.chesscoach.main.dto.match.MatchResultRequest;
import com.chesscoach.main.dto.match.MatchSummaryResponse;
import com.chesscoach.main.exception.ConflictException;
import com.chesscoach.main.exception.ResourceNotFoundException;
import com.chesscoach.main.model.Match;
import com.chesscoach.main.model.MatchFormat;
import com.chesscoach.main.model.MatchParticipant;
import com.chesscoach.main.model.MatchResult;
import com.chesscoach.main.model.MatchResultType;
import com.chesscoach.main.model.MatchStatus;
import com.chesscoach.main.model.PieceColor;
import com.chesscoach.main.model.RapidRating;
import com.chesscoach.main.model.Trainee;
import com.chesscoach.main.repository.MatchParticipantRepository;
import com.chesscoach.main.repository.MatchRepository;
import com.chesscoach.main.repository.MatchResultRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.MatchService;
import com.chesscoach.main.service.RatingService;
import com.chesscoach.main.util.RoundRobinGenerator;
import com.chesscoach.main.util.SwissPairingGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MatchServiceImpl implements MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchServiceImpl.class);

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchResultRepository matchResultRepository;
    private final TraineeRepository traineeRepository;
    private final RatingService ratingService;

    @Value("${app.rating.default:1200}")
    private int defaultRating;

    public MatchServiceImpl(
        MatchRepository matchRepository,
        MatchParticipantRepository matchParticipantRepository,
        MatchResultRepository matchResultRepository,
        TraineeRepository traineeRepository,
        RatingService ratingService
    ) {
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.matchResultRepository = matchResultRepository;
        this.traineeRepository = traineeRepository;
        this.ratingService = ratingService;
    }

    private int getRapidCurrentRating(Trainee trainee) {
        RapidRating rapid = trainee.getRapidRating();
        return rapid != null && rapid.getCurrentRating() != null ? rapid.getCurrentRating() : defaultRating;
    }

    @Override
    @Transactional
    public MatchSummaryResponse createMatch(MatchCreateRequest request) {
        log.info("Creating match with trainees: {}", request.getTraineeIds());
        
        MatchFormat format = parseFormat(request.getFormat());
        List<Trainee> trainees = fetchTrainees(request.getTraineeIds());
        if (trainees.isEmpty()) {
            throw new ResourceNotFoundException("No trainees provided for match creation");
        }
        if (trainees.size() > 2) {
            throw new IllegalArgumentException("Create match supports only 1 or 2 trainees");
        }
        Long whiteId = trainees.get(0).getId();
        Long blackId = trainees.size() > 1 ? trainees.get(1).getId() : null;
        Match match = createMatchRecord(format, 1, request.getScheduledDate(), whiteId, blackId);
        
        log.debug("Match created: id={}, format={}", match.getId(), format);
        return toSummary(match, traineesByIds(trainees), null);
    }

    @Override
    @Transactional
    public List<MatchSummaryResponse> generateSwiss(MatchGenerationRequest request) {
        log.info("Generating Swiss pairings for {} trainees, round {}", 
            request.getTraineeIds().size(), request.getRoundNumber());
        
        List<Trainee> trainees = fetchTrainees(request.getTraineeIds());
        List<Long> sortedIds = trainees.stream()
            .sorted(Comparator.comparing(this::getRapidCurrentRating).reversed())
            .map(Trainee::getId)
            .toList();

        Map<Long, Trainee> traineeMap = traineesByIds(trainees);
        List<MatchSummaryResponse> summaries = new ArrayList<>();
        for (SwissPairingGenerator.Pairing pairing : SwissPairingGenerator.generate(sortedIds)) {
            Match match = createMatchRecord(MatchFormat.SWISS, request.getRoundNumber(), LocalDate.now(), 
                pairing.whiteTraineeId(), pairing.blackTraineeId());
            String result = pairing.blackTraineeId() == null ? "BYE" : null;
            summaries.add(toSummary(match, traineeMap, result));
        }
        
        log.debug("Generated {} Swiss pairings", summaries.size());
        return summaries;
    }

    @Override
    @Transactional
    public List<MatchSummaryResponse> generateRoundRobin(MatchGenerationRequest request) {
        log.info("Generating Round Robin pairings for {} trainees, round {}", 
            request.getTraineeIds().size(), request.getRoundNumber());
        
        List<Trainee> trainees = fetchTrainees(request.getTraineeIds());
        List<Long> ids = trainees.stream().map(Trainee::getId).toList();
        Map<Long, Trainee> traineeMap = traineesByIds(trainees);

        List<MatchSummaryResponse> summaries = new ArrayList<>();
        for (RoundRobinGenerator.Pairing pairing : RoundRobinGenerator.generateForRound(ids, request.getRoundNumber())) {
            Match match = createMatchRecord(
                MatchFormat.ROUND_ROBIN,
                request.getRoundNumber(),
                LocalDate.now(),
                pairing.whiteTraineeId(),
                pairing.blackTraineeId()
            );
            String result = pairing.blackTraineeId() == null ? "BYE" : null;
            summaries.add(toSummary(match, traineeMap, result));
        }
        
        log.debug("Generated {} Round Robin pairings", summaries.size());
        return summaries;
    }

    @Override
    @Transactional
    public MatchResultRequest recordResult(MatchResultRequest request) {
        log.info("Recording match result for match: {}", request.getMatchId());
        
        Match match = matchRepository.findById(request.getMatchId())
            .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + request.getMatchId()));
        
        // Validate state transition
        if (!MatchStatus.canTransitionTo(match.getStatus(), MatchStatus.COMPLETED)) {
            throw new ConflictException("Cannot record result: match status " + match.getStatus() + 
                " cannot transition to COMPLETED");
        }
        
        if (!matchResultRepository.findByMatchIdOrderByPlayedAtDesc(match.getId()).isEmpty()) {
            throw new ConflictException("Result already recorded for match: " + match.getId());
        }

        validateStrictChessScores(request.getWhiteScore(), request.getBlackScore());

        List<MatchParticipant> participants = matchParticipantRepository.findByMatchIdOrderByBoardNumberAsc(match.getId());
        if (participants.size() != 2) {
            throw new IllegalArgumentException("Only 2-player matches can be recorded via this endpoint");
        }
        
        Long expectedWhiteId = participants.stream()
            .filter(p -> p.getPieceColor() == PieceColor.WHITE)
            .map(p -> p.getTrainee().getId())
            .findFirst()
            .orElse(participants.get(0).getTrainee().getId());
        Long expectedBlackId = participants.stream()
            .filter(p -> p.getPieceColor() == PieceColor.BLACK)
            .map(p -> p.getTrainee().getId())
            .findFirst()
            .orElseGet(() -> participants.stream()
                .map(p -> p.getTrainee().getId())
                .filter(id -> !id.equals(expectedWhiteId))
                .findFirst()
                .orElse(participants.get(1).getTrainee().getId()));

        Trainee white = traineeRepository.findById(expectedWhiteId)
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + expectedWhiteId));
        Trainee black = traineeRepository.findById(expectedBlackId)
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + expectedBlackId));
        
        if (expectedWhiteId.equals(expectedBlackId)) {
            throw new IllegalArgumentException("White and black players must be different trainees");
        }

        MatchResult result = new MatchResult();
        result.setMatch(match);
        result.setWhiteTrainee(white);
        result.setBlackTrainee(black);
        result.setWhiteScore(request.getWhiteScore());
        result.setBlackScore(request.getBlackScore());
        result.setResultType(resolveResultType(request.getWhiteScore(), request.getBlackScore()));
        result.setPlayedAt(OffsetDateTime.now());
        MatchResult savedResult = matchResultRepository.save(result);

        ratingService.applyMatchResultRatingUpdate(savedResult);

        match.setStatus(MatchStatus.COMPLETED);
        matchRepository.save(match);
        
        log.info("Match result recorded: {} vs {}, result={}", 
            white.getName(), black.getName(), result.getResultType());
        
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchSummaryResponse> getHistoryByTrainee(Long traineeId) {
        log.debug("Retrieving match history for trainee: {}", traineeId);
        
        List<MatchResult> results = matchResultRepository
            .findByWhiteTraineeIdOrBlackTraineeIdOrderByPlayedAtDesc(traineeId, traineeId);
        return results.stream().map(result -> {
            MatchSummaryResponse summary = new MatchSummaryResponse();
            summary.setMatchId(result.getMatch().getId());
            summary.setScheduledDate(result.getMatch().getScheduledDate());
            summary.setFormat(result.getMatch().getFormat().name());
            summary.setWhitePlayer(result.getWhiteTrainee().getName());
            summary.setBlackPlayer(result.getBlackTrainee().getName());
            summary.setResult(result.getResultType().name());
            return summary;
        }).toList();
    }

    private Match createMatchRecord(MatchFormat format, int roundNumber, LocalDate date, Long whiteId, Long blackId) {
        Match match = new Match();
        match.setFormat(format);
        match.setRoundNumber(roundNumber);
        match.setScheduledDate(date);
        match.setStatus(MatchStatus.SCHEDULED);
        Match savedMatch = matchRepository.save(match);

        if (whiteId != null && blackId == null) {
            matchParticipantRepository.save(createParticipant(savedMatch, whiteId, PieceColor.NONE, 1, true));
            return savedMatch;
        }
        if (whiteId != null) {
            matchParticipantRepository.save(createParticipant(savedMatch, whiteId, PieceColor.WHITE, 1, false));
        }
        if (blackId != null) {
            matchParticipantRepository.save(createParticipant(savedMatch, blackId, PieceColor.BLACK, 2, false));
        }
        return savedMatch;
    }

    private MatchParticipant createParticipant(Match match, Long traineeId, PieceColor color, int boardNumber, boolean bye) {
        Trainee trainee = traineeRepository.findById(traineeId)
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + traineeId));
        MatchParticipant participant = new MatchParticipant();
        participant.setMatch(match);
        participant.setTrainee(trainee);
        participant.setPieceColor(color);
        participant.setBoardNumber(boardNumber);
        participant.setStartRating(getRapidCurrentRating(trainee));
        participant.setPointsEarned(bye ? 1.0 : 0.0);
        participant.setBye(bye);
        return participant;
    }

    private MatchFormat parseFormat(String format) {
        try {
            return MatchFormat.valueOf(format.trim().toUpperCase().replace('-', '_'));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported match format: " + format);
        }
    }

    private List<Trainee> fetchTrainees(List<Long> ids) {
        List<Trainee> trainees = traineeRepository.findAllById(ids);
        if (trainees.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more trainees not found");
        }
        return trainees;
    }

    private Map<Long, Trainee> traineesByIds(List<Trainee> trainees) {
        return trainees.stream().collect(Collectors.toMap(Trainee::getId, Function.identity()));
    }

    private MatchSummaryResponse toSummary(Match match, Map<Long, Trainee> traineeMap, String result) {
        List<MatchParticipant> participants = matchParticipantRepository.findByMatchIdOrderByBoardNumberAsc(match.getId());
        MatchSummaryResponse response = new MatchSummaryResponse();
        response.setMatchId(match.getId());
        response.setScheduledDate(match.getScheduledDate());
        response.setFormat(match.getFormat().name());
        if (!participants.isEmpty()) {
            MatchParticipant white = participants.stream()
                .filter(p -> p.getPieceColor() == PieceColor.WHITE)
                .findFirst()
                .orElse(participants.get(0));
            response.setWhitePlayer(traineeMap.get(white.getTrainee().getId()).getName());
            if (participants.size() > 1) {
                MatchParticipant black = participants.stream()
                    .filter(p -> p.getPieceColor() == PieceColor.BLACK)
                    .findFirst()
                    .orElseGet(() -> participants.stream()
                        .filter(p -> !p.getTrainee().getId().equals(white.getTrainee().getId()))
                        .findFirst()
                        .orElse(null));
                response.setBlackPlayer(black != null
                    ? traineeMap.get(black.getTrainee().getId()).getName()
                    : "BYE");
            } else {
                response.setBlackPlayer("BYE");
            }
        }
        response.setResult(result);
        return response;
    }

    private MatchResultType resolveResultType(double whiteScore, double blackScore) {
        if (whiteScore > blackScore) {
            return MatchResultType.WHITE_WIN;
        }
        if (blackScore > whiteScore) {
            return MatchResultType.BLACK_WIN;
        }
        return MatchResultType.DRAW;
    }

    private void validateStrictChessScores(double whiteScore, double blackScore) {
        boolean whiteWin = whiteScore == 1.0 && blackScore == 0.0;
        boolean blackWin = whiteScore == 0.0 && blackScore == 1.0;
        boolean draw = whiteScore == 0.5 && blackScore == 0.5;
        if (!whiteWin && !blackWin && !draw) {
            throw new IllegalArgumentException("Score must be one of: 1-0, 0-1, or 0.5-0.5");
        }
    }

}

