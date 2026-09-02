package com.gestiontache.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskTest {

    private final LocalDate today = LocalDate.now();

    @Test
    void newTaskDefaultsToAFaireAndNotCompleted() {
        Task task = new Task("Tache", "", today);

        assertEquals(TaskStatus.A_FAIRE, task.getStatus());
        assertFalse(task.isCompleted());
    }

    @Test
    void setCompletedTrueSetsStatusToTerminee() {
        Task task = new Task("Tache", "", today);

        task.setCompleted(true);

        assertEquals(TaskStatus.TERMINEE, task.getStatus());
        assertTrue(task.isCompleted());
    }

    @Test
    void settingStatusEnCoursDoesNotMarkCompleted() {
        Task task = new Task("Tache", "", today);

        task.setStatus(TaskStatus.EN_COURS);

        assertEquals(TaskStatus.EN_COURS, task.getStatus());
        assertFalse(task.isCompleted());
    }

    @Test
    void uncheckingACompletedTaskRevertsStatusToAFaire() {
        Task task = new Task("Tache", "", today);
        task.setCompleted(true);

        task.setCompleted(false);

        assertEquals(TaskStatus.A_FAIRE, task.getStatus());
        assertFalse(task.isCompleted());
    }

    @Test
    void uncheckingWhileEnCoursLeavesStatusUnchanged() {
        Task task = new Task("Tache", "", today);
        task.setStatus(TaskStatus.EN_COURS);

        // Already not completed; toggling the checkbox off again must not
        // silently discard the "en cours" status.
        task.setCompleted(false);

        assertEquals(TaskStatus.EN_COURS, task.getStatus());
    }

    @Test
    void settingStatusTermineeMarksCompleted() {
        Task task = new Task("Tache", "", today);
        task.setStatus(TaskStatus.EN_COURS);

        task.setStatus(TaskStatus.TERMINEE);

        assertTrue(task.isCompleted());
    }

    @Test
    void newTaskStartsWithACreationHistoryEntry() {
        Task task = new Task("Tache", "", today);

        assertEquals(1, task.getHistory().size());
        assertEquals("Tache creee", task.getHistory().get(0).getMessage());
    }

    @Test
    void addHistoryEntryAppendsANewDatedEntry() {
        Task task = new Task("Tache", "", today);

        task.addHistoryEntry("Titre modifie");

        assertEquals(2, task.getHistory().size());
        assertEquals("Titre modifie", task.getHistory().get(1).getMessage());
    }

    /**
     * Jackson may call setCompleted(boolean) and setStatus(TaskStatus) in
     * either order when deserializing a JSON file that (self-consistently)
     * has both properties; the final state must not depend on which one
     * runs first.
     */
    @Test
    void deserializationOrderDoesNotMatterForATerminatedTask() {
        Task completedFirst = new Task("Tache", "", today);
        completedFirst.setCompleted(true);
        completedFirst.setStatus(TaskStatus.TERMINEE);

        Task statusFirst = new Task("Tache", "", today);
        statusFirst.setStatus(TaskStatus.TERMINEE);
        statusFirst.setCompleted(true);

        assertEquals(TaskStatus.TERMINEE, completedFirst.getStatus());
        assertTrue(completedFirst.isCompleted());
        assertEquals(TaskStatus.TERMINEE, statusFirst.getStatus());
        assertTrue(statusFirst.isCompleted());
    }

    @Test
    void deserializationOrderDoesNotMatterForAnInProgressTask() {
        Task completedFirst = new Task("Tache", "", today);
        completedFirst.setCompleted(false);
        completedFirst.setStatus(TaskStatus.EN_COURS);

        Task statusFirst = new Task("Tache", "", today);
        statusFirst.setStatus(TaskStatus.EN_COURS);
        statusFirst.setCompleted(false);

        assertEquals(TaskStatus.EN_COURS, completedFirst.getStatus());
        assertFalse(completedFirst.isCompleted());
        assertEquals(TaskStatus.EN_COURS, statusFirst.getStatus());
        assertFalse(statusFirst.isCompleted());
    }
}
