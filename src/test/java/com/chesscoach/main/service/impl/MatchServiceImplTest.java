package com.chesscoach.main.service.impl;

import com.chesscoach.main.dto.match.MatchResultRequest;
import com.chesscoach.main.exception.ConflictException;
import com.chesscoach.main.model.Match;
import com.chesscoach.main.model.MatchFormat;
import com.chesscoach.main.model.MatchResult;
import com.chesscoach.main.model.MatchStatus;
import com.chesscoach.main.repository.MatchParticipantRepository;
import com.chesscoach.main.repository.MatchRepository;
import com.chesscoach.main.repository.MatchResultRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.RatingService;
import com.chesscoach.main.service.SwissTournamentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchParticipantRepository matchParticipantRepository;
    @Mock
    private MatchResultRepository matchResultRepository;
    @Mock
    private TraineeRepository traineeRepository;
    @Mock
    private RatingService ratingService;
    @Mock
    private SwissTournamentService swissTournamentService;

    @InjectMocks
    private MatchServiceImpl matchService;

    @Test
    void recordResultRejectsDuplicateResultBeforeStatusTransitionCheck() {
        Match match = new Match();
        match.setId(42L);
        match.setFormat(MatchFormat.SWISS);
        match.setStatus(MatchStatus.COMPLETED);

        MatchResult existingResult = new MatchResult();
        existingResult.setMatch(match);

        MatchResultRequest request = new MatchResultRequest();
        request.setMatchId(42L);
        request.setWhiteScore(1.0);
        request.setBlackScore(0.0);

        when(matchRepository.findById(42L)).thenReturn(Optional.of(match));
        when(matchResultRepository.findByMatchIdOrderByPlayedAtDesc(42L)).thenReturn(List.of(existingResult));

        assertThatThrownBy(() -> matchService.recordResult(request))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Result already recorded for match: 42");

        verify(matchParticipantRepository, never()).findByMatchIdOrderByBoardNumberAsc(42L);
        verify(ratingService, never()).applyMatchResultRatingUpdate(existingResult);
    }
}
