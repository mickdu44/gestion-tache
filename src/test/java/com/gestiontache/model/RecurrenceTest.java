package com.gestiontache.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecurrenceTest {

    @Test
    void quotidienneMovesToTheNextDay() {
        LocalDate day = LocalDate.of(2026, 3, 10);
        assertEquals(LocalDate.of(2026, 3, 11), Recurrence.QUOTIDIENNE.nextOccurrence(day));
    }

    @Test
    void hebdomadaireMovesOneWeekLater() {
        LocalDate day = LocalDate.of(2026, 3, 10);
        assertEquals(LocalDate.of(2026, 3, 17), Recurrence.HEBDOMADAIRE.nextOccurrence(day));
    }

    @Test
    void joursOuvresSkipsTheWeekendAfterFriday() {
        LocalDate friday = LocalDate.of(2026, 3, 13);
        assertEquals(DayOfWeek.FRIDAY, friday.getDayOfWeek());

        LocalDate next = Recurrence.JOURS_OUVRES.nextOccurrence(friday);

        assertEquals(LocalDate.of(2026, 3, 16), next);
        assertEquals(DayOfWeek.MONDAY, next.getDayOfWeek());
    }

    @Test
    void joursOuvresJustMovesToNextDayOnAWeekday() {
        LocalDate tuesday = LocalDate.of(2026, 3, 10);
        assertEquals(DayOfWeek.TUESDAY, tuesday.getDayOfWeek());

        assertEquals(LocalDate.of(2026, 3, 11), Recurrence.JOURS_OUVRES.nextOccurrence(tuesday));
    }

    @Test
    void aucuneHasNoNextOccurrence() {
        assertThrows(IllegalStateException.class, () -> Recurrence.AUCUNE.nextOccurrence(LocalDate.now()));
    }
}
