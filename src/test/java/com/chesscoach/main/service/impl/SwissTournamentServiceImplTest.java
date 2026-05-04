package com.chesscoach.main.service.impl;

import com.chesscoach.main.model.Match;
import com.chesscoach.main.model.MatchFormat;
import com.chesscoach.main.model.MatchParticipant;
import com.chesscoach.main.model.MatchResult;
import com.chesscoach.main.model.MatchResultType;
import com.chesscoach.main.model.RapidRating;
import com.chesscoach.main.model.RematachRound;
import com.chesscoach.main.model.Trainee;
import com.chesscoach.main.repository.MatchParticipantRepository;
import com.chesscoach.main.repository.MatchRepository;
import com.chesscoach.main.repository.RematachRoundRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.SwissScorer;
import com.chesscoach.main.service.SwissTiebreaker;
import com.chesscoach.main.util.SwissPairingGenerator.Pairing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SwissTournamentServiceImplTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchParticipantRepository matchParticipantRepository;
    @Mock
    private TraineeRepository traineeRepository;
    @Mock
    private RematachRoundRepository rematachRoundRepository;
    @Mock
    private SwissScorer swissScorer;
    @Mock
    private SwissTiebreaker swissTiebreaker;

    @InjectMocks
    private SwissTournamentServiceImpl swissTournamentService;

    @Test
    void generateNextRoundUsesScopedParticipants() {
        Trainee trainee1 = trainee(1L, "Alice", 1500);
        Trainee trainee2 = trainee(2L, "Bob", 1400);

        when(matchRepository.findMaxRoundNumberByFormatAndTraineeIds(MatchFormat.SWISS, List.of(1L, 2L))).thenReturn(0);
        when(traineeRepository.findAllByIdInWithRatingsOrderedByRating(List.of(1L, 2L)))
            .thenReturn(List.of(trainee1, trainee2));
        when(swissScorer.getPlayerMatchResultsUpToRound(1L, 0)).thenReturn(List.of());
        when(swissScorer.getPlayerMatchResultsUpToRound(2L, 0)).thenReturn(List.of());
        when(matchParticipantRepository.existsByTraineeIdAndSwissRoundNumberIsNotNullAndByeTrue(1L)).thenReturn(false);
        when(matchParticipantRepository.existsByTraineeIdAndSwissRoundNumberIsNotNullAndByeTrue(2L)).thenReturn(false);

        List<Pairing> pairings = swissTournamentService.generateNextRound(List.of(1L, 2L), 1);

        assertThat(pairings).containsExactly(new Pairing(1L, 2L));
        verify(matchRepository).findMaxRoundNumberByFormatAndTraineeIds(MatchFormat.SWISS, List.of(1L, 2L));
        verify(traineeRepository).findAllByIdInWithRatingsOrderedByRating(List.of(1L, 2L));
        verify(matchRepository, never()).findMaxRoundNumberByFormat(MatchFormat.SWISS);
        verify(traineeRepository, never()).findAllWithRatingsOrderedByRating();
    }

    @Test
    void generateNextRoundAdvancesSkippedScopedRound() {
        Trainee trainee1 = trainee(1L, "Alice", 1500);
        Trainee trainee2 = trainee(2L, "Bob", 1400);

        when(matchRepository.findMaxRoundNumberByFormatAndTraineeIds(MatchFormat.SWISS, List.of(1L, 2L))).thenReturn(2);
        when(traineeRepository.findAllByIdInWithRatingsOrderedByRating(List.of(1L, 2L)))
            .thenReturn(List.of(trainee1, trainee2));
        when(swissScorer.getPlayerMatchResultsUpToRound(1L, 2)).thenReturn(List.of());
        when(swissScorer.getPlayerMatchResultsUpToRound(2L, 2)).thenReturn(List.of());
        when(matchParticipantRepository.existsByTraineeIdAndSwissRoundNumberIsNotNullAndByeTrue(1L)).thenReturn(false);
        when(matchParticipantRepository.existsByTraineeIdAndSwissRoundNumberIsNotNullAndByeTrue(2L)).thenReturn(false);

        List<Pairing> pairings = swissTournamentService.generateNextRound(List.of(1L, 2L), 4);

        assertThat(pairings).containsExactly(new Pairing(1L, 2L));
        verify(swissScorer).getPlayerMatchResultsUpToRound(1L, 2);
        verify(swissScorer).getPlayerMatchResultsUpToRound(2L, 2);
        verify(traineeRepository).findAllByIdInWithRatingsOrderedByRating(List.of(1L, 2L));
    }

    @Test
    void finalizeRoundCreatesScheduledRematchMatchAndRecord() {
        Trainee trainee1 = trainee(1L, "Alice", null);
        Trainee trainee2 = trainee(2L, "Bob", null);

        when(traineeRepository.findAll()).thenReturn(List.of(trainee1, trainee2));
        when(swissTiebreaker.rankTraineesByTiebreaker(any(), eq(3)))
            .thenReturn(List.of(
                new SwissTiebreaker.TraineeRanking(trainee1, 2.5, 5.0, 1),
                new SwissTiebreaker.TraineeRanking(trainee2, 2.5, 5.0, 1)
            ));
        when(swissTiebreaker.shouldCreateRematch(trainee1, trainee2, 3)).thenReturn(true);

        Match originalMatch = new Match();
        originalMatch.setId(100L);
        originalMatch.setFormat(MatchFormat.SWISS);
        originalMatch.setRoundNumber(2);
        originalMatch.setScheduledDate(LocalDate.now().minusDays(1));

        MatchResult drawResult = new MatchResult();
        drawResult.setMatch(originalMatch);
        drawResult.setWhiteTrainee(trainee1);
        drawResult.setBlackTrainee(trainee2);
        drawResult.setResultType(MatchResultType.DRAW);
        when(swissScorer.getPlayerMatchResultsUpToRound(1L, 3)).thenReturn(List.of(drawResult));

        when(rematachRoundRepository.findByTraineePairAndOriginalMatch(1L, 2L, 100L)).thenReturn(Optional.empty());

        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });

        when(traineeRepository.findById(1L)).thenReturn(Optional.of(trainee1));
        when(traineeRepository.findById(2L)).thenReturn(Optional.of(trainee2));
        when(matchParticipantRepository.save(any(MatchParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rematachRoundRepository.save(any(RematachRound.class))).thenAnswer(invocation -> invocation.getArgument(0));

        swissTournamentService.finalizeRound(3);

        ArgumentCaptor<RematachRound> rematchCaptor = ArgumentCaptor.forClass(RematachRound.class);
        verify(rematachRoundRepository).save(rematchCaptor.capture());
        RematachRound savedRematch = rematchCaptor.getValue();

        assertThat(savedRematch.getStatus()).isEqualTo(RematachRound.RematachStatus.SCHEDULED);
        assertThat(savedRematch.getRematachMatch()).isNotNull();
        assertThat(savedRematch.getRematachMatch().getId()).isEqualTo(200L);
        assertThat(savedRematch.getReason()).contains("Tiebreaker rematch");
    }

    private static Trainee trainee(Long id, String name, Integer rating) {
        Trainee trainee = new Trainee();
        trainee.setId(id);
        trainee.setName(name);
        if (rating != null) {
            RapidRating rapidRating = new RapidRating();
            rapidRating.setTrainee(trainee);
            rapidRating.setCurrentRating(rating);
            trainee.setRapidRating(rapidRating);
        }
        return trainee;
    }
}
