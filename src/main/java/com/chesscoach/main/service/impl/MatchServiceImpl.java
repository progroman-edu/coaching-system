// This service implementation contains business logic for Match operations.
package com.chesscoach.main.service.impl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chesscoach.main.dto.match.MatchCreateRequest;
import com.chesscoach.main.dto.match.MatchGenerationRequest;
import com.chesscoach.main.dto.match.MatchResultRequest;
import com.chesscoach.main.dto.match.MatchResultResponse;
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
import com.chesscoach.main.service.SwissTournamentService;
import com.chesscoach.main.util.RoundRobinGenerator;
import com.chesscoach.main.util.SwissPairingGenerator.Pairing;

@Service
public class MatchServiceImpl implements MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchServiceImpl.class);

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchResultRepository matchResultRepository;
    private final TraineeRepository traineeRepository;
    private final RatingService ratingService;
    private final SwissTournamentService swissTournamentService;

    @Value("${app.rating.default:1200}")
    private int defaultRating;

    public MatchServiceImpl(
        MatchRepository matchRepository,
        MatchParticipantRepository matchParticipantRepository,
        MatchResultRepository matchResultRepository,
        TraineeRepository traineeRepository,
        RatingService ratingService,
        SwissTournamentService swissTournamentService
    ) {
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.matchResultRepository = matchResultRepository;
        this.traineeRepository = traineeRepository;
        this.ratingService = ratingService;
        this.swissTournamentService = swissTournamentService;
    }

    private int getRapidCurrentRating(Trainee trainee) {
        RapidRating rapid = trainee.getRapidRating();
        if (rapid == null || rapid.getCurrentRating() == null) {
            return defaultRating;
        }
        return rapid.getCurrentRating();
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
    @Transactional(readOnly = true)
    public List<MatchSummaryResponse> listMatches() {
        List<Match> matches = matchRepository.findAll(
            Sort.by(Sort.Direction.DESC, "scheduledDate").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        List<MatchSummaryResponse> summaries = new ArrayList<>();
        for (Match match : matches) {
            List<MatchParticipant> participants = matchParticipantRepository.findByMatchIdOrderByBoardNumberAsc(match.getId());
            List<Long> traineeIds = participants.stream().map(p -> p.getTrainee().getId()).toList();
            Map<Long, Trainee> traineeMap = traineeRepository.findAllById(traineeIds).stream()
                .collect(Collectors.toMap(Trainee::getId, Function.identity()));
            summaries.add(toSummary(match, traineeMap, resolveStoredResult(match)));
        }
        return summaries;
    }

    @Override
    @Transactional
    public List<MatchSummaryResponse> generateSwiss(MatchGenerationRequest request) {
        List<Long> requestedTraineeIds = normalizeTraineeIds(request.getTraineeIds());
        List<Trainee> tournamentTrainees = requestedTraineeIds.isEmpty()
            ? traineeRepository.findAllWithRatingsOrderedByRating()
            : fetchTrainees(requestedTraineeIds);
        if (tournamentTrainees.isEmpty()) {
            throw new ResourceNotFoundException("No trainees available for Swiss pairing");
        }
        List<Long> tournamentTraineeIds = tournamentTrainees.stream().map(Trainee::getId).toList();

        // Auto-calculate roundNumber if not provided
        Integer requestRound = request.getRoundNumber();
        int roundNumber = (requestRound != null && requestRound > 0)
            ? requestRound
            : autoCalculateNextRound(MatchFormat.SWISS, tournamentTraineeIds);
        
        log.info("Generating Swiss pairings for {} trainees, round {}", tournamentTraineeIds.size(), roundNumber);
        
        List<Pairing> pairings = swissTournamentService.generateNextRound(tournamentTraineeIds, roundNumber);
        
        // Map trainee IDs for lookup
        Map<Long, Trainee> traineeMap = traineesByIds(tournamentTrainees);
        List<MatchSummaryResponse> summaries = new ArrayList<>();
        for (Pairing pairing : pairings) {
            Match match = createMatchRecord(
                MatchFormat.SWISS,
                roundNumber,
                LocalDate.now(),
                pairing.whiteTraineeId(),
                pairing.blackTraineeId()
            );

            // Store swiss_round_number in participants
            List<MatchParticipant> participants = matchParticipantRepository.findByMatchId(match.getId());
            for (MatchParticipant participant : participants) {
                participant.setSwissRoundNumber(roundNumber);
                matchParticipantRepository.save(participant);
            }

            String result = pairing.blackTraineeId() == null ? "BYE" : null;
            summaries.add(toSummary(match, traineeMap, result));
        }
        
        log.debug("Generated {} Swiss pairings for round {}", summaries.size(), roundNumber);
        return summaries;
    }

    @Override
    @Transactional
    public List<MatchSummaryResponse> generateRoundRobin(MatchGenerationRequest request) {
        List<Trainee> trainees = fetchTrainees(request.getTraineeIds());
        List<Long> ids = trainees.stream().map(Trainee::getId).toList();
        if (ids.isEmpty()) {
            throw new ResourceNotFoundException("No trainees provided for round robin pairing");
        }
        
        // Auto-calculate roundNumber if not provided
        Integer requestRound = request.getRoundNumber();
        int roundNumber = (requestRound != null && requestRound > 0)
            ? requestRound
            : autoCalculateNextRound(MatchFormat.ROUND_ROBIN, ids);
        
        log.info("Generating Round Robin pairings for {} trainees, round {}", ids.size(), roundNumber);
        
        Map<Long, Trainee> traineeMap = traineesByIds(trainees);

        List<MatchSummaryResponse> summaries = new ArrayList<>();
        for (RoundRobinGenerator.Pairing pairing : RoundRobinGenerator.generateForRound(ids, roundNumber)) {
            Match match = createMatchRecord(
                MatchFormat.ROUND_ROBIN,
                roundNumber,
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
    public MatchResultResponse recordResult(MatchResultRequest request) {
        log.info("Recording match result for match: {}", request.getMatchId());
        
        Match match = matchRepository.findById(request.getMatchId())
            .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + request.getMatchId()));

        if (!matchResultRepository.findByMatchIdOrderByPlayedAtDesc(match.getId()).isEmpty()) {
            throw new ConflictException("Result already recorded for match: " + match.getId());
        }
        
        // Validate state transition
        if (!MatchStatus.canTransitionTo(match.getStatus(), MatchStatus.COMPLETED)) {
            throw new ConflictException("Cannot record result: match status " + match.getStatus() + 
                " cannot transition to COMPLETED");
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
        
        // Build response with match details and rating changes
        MatchResultResponse response = MatchResultResponse.builder()
            .matchId(match.getId())
            .resultType(result.getResultType().name())
            .whiteScore(result.getWhiteScore())
            .blackScore(result.getBlackScore())
            .recordedAt(savedResult.getPlayedAt().toLocalDateTime())
            .message("Match result recorded successfully")
            .build();
        
        return response;
    }

    @Override
    @Transactional
    public MatchSummaryResponse rollbackMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + matchId));

        List<MatchResult> results = matchResultRepository.findByMatchIdOrderByPlayedAtDesc(matchId);
        boolean hadResult = !results.isEmpty();
        MatchStatus targetStatus = hadResult ? MatchStatus.SCHEDULED : MatchStatus.CANCELLED;

        if (!MatchStatus.canTransitionTo(match.getStatus(), targetStatus)) {
            throw new ConflictException("Cannot rollback match status " + match.getStatus() + " to " + targetStatus);
        }

        if (hadResult) {
            matchResultRepository.delete(results.get(0));
            ratingService.rebuildFromMatchHistory();
        }

        match.setStatus(targetStatus);
        Match savedMatch = matchRepository.save(match);

        List<MatchParticipant> participants = matchParticipantRepository.findByMatchIdOrderByBoardNumberAsc(savedMatch.getId());
        Map<Long, Trainee> traineeMap = traineeRepository.findAllById(
            participants.stream().map(participant -> participant.getTrainee().getId()).toList()
        ).stream().collect(Collectors.toMap(Trainee::getId, Function.identity()));

        return toSummary(savedMatch, traineeMap, hadResult ? "ROLLED_BACK" : "CANCELLED");
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
            summary.setStatus(result.getMatch().getStatus().name());
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
        List<Long> normalizedIds = normalizeTraineeIds(ids);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Trainee> traineesById = traineeRepository.findAllById(normalizedIds).stream()
            .collect(Collectors.toMap(Trainee::getId, Function.identity()));
        if (traineesById.size() != normalizedIds.size()) {
            throw new ResourceNotFoundException("One or more trainees not found");
        }
        return normalizedIds.stream()
            .map(traineesById::get)
            .toList();
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
        response.setRoundNumber(match.getRoundNumber());
        response.setStatus(match.getStatus().name());
        if (!participants.isEmpty()) {
            MatchParticipant white = participants.stream()
                .filter(p -> p.getPieceColor() == PieceColor.WHITE)
                .findFirst()
                .orElse(participants.get(0));
            response.setWhitePlayerId(white.getTrainee().getId());
            response.setWhitePlayer(traineeMap.get(white.getTrainee().getId()).getName());
            if (participants.size() > 1) {
                MatchParticipant black = participants.stream()
                    .filter(p -> p.getPieceColor() == PieceColor.BLACK)
                    .findFirst()
                    .orElseGet(() -> participants.stream()
                        .filter(p -> !p.getTrainee().getId().equals(white.getTrainee().getId()))
                        .findFirst()
                        .orElse(null));
                response.setBlackPlayerId(black != null ? black.getTrainee().getId() : null);
                response.setBlackPlayer(black != null
                    ? traineeMap.get(black.getTrainee().getId()).getName()
                    : "BYE");
            } else {
                response.setBlackPlayerId(null);
                response.setBlackPlayer("BYE");
            }
        }
        response.setResult(result);
        return response;
    }

    private String resolveStoredResult(Match match) {
        List<MatchResult> results = matchResultRepository.findByMatchIdOrderByPlayedAtDesc(match.getId());
        if (!results.isEmpty()) {
            return results.get(0).getResultType().name();
        }
        return match.getStatus() == MatchStatus.CANCELLED ? MatchStatus.CANCELLED.name() : null;
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

    private int autoCalculateNextRound(MatchFormat format, List<Long> traineeIds) {
        Integer maxExistingRound = traineeIds == null || traineeIds.isEmpty()
            ? matchRepository.findMaxRoundNumberByFormat(format)
            : matchRepository.findMaxRoundNumberByFormatAndTraineeIds(format, traineeIds);
        if (maxExistingRound == null) {
            return 1;
        }
        return maxExistingRound + 1;
    }

    /**
     * Calculate the maximum rounds for Round-Robin tournament.
     * For Round-Robin: max rounds = N - 1 (each player plays every other player once)
     */
    private int calculateMaxRoundRobinRounds(int participantCount) {
        if (participantCount <= 1) {
            return 0;
        }
        return participantCount - 1;
    }

    @Override
    public int calculateMaxRoundsForFormat(String format, int participantCount) {
        if ("SWISS".equalsIgnoreCase(format)) {
            return swissTournamentService.calculateMaxRounds(participantCount);
        } else if ("ROUND_ROBIN".equalsIgnoreCase(format)) {
            return calculateMaxRoundRobinRounds(participantCount);
        }
        throw new IllegalArgumentException("Unknown format: " + format);
    }

    private List<Long> normalizeTraineeIds(List<Long> traineeIds) {
        if (traineeIds == null || traineeIds.isEmpty()) {
            return List.of();
        }
        return traineeIds.stream()
            .filter(id -> id != null && id > 0)
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                ArrayList::new
            ));
    }

}
