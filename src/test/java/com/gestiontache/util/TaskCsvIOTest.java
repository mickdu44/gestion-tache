package com.gestiontache.util;

import com.gestiontache.model.Priority;
import com.gestiontache.model.Task;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskCsvIOTest {

    @Test
    void exportsThenImportsTheSameTasks() throws IOException {
        Task first = new Task("Facturer le client", "Envoyer avant vendredi, ligne 2", LocalDate.of(2026, 3, 10));
        first.setPriority(Priority.HAUTE);
        Task second = new Task("Rediger le compte-rendu", "", LocalDate.of(2026, 3, 11));
        second.setCompleted(true);

        StringWriter out = new StringWriter();
        TaskCsvIO.export(List.of(first, second), out);

        List<Task> imported = TaskCsvIO.importFrom(new StringReader(out.toString()));

        assertEquals(2, imported.size());
        Task importedFirst = imported.get(0);
        assertEquals("Facturer le client", importedFirst.getTitle());
        assertEquals("Envoyer avant vendredi, ligne 2", importedFirst.getDescription());
        assertEquals(LocalDate.of(2026, 3, 10), importedFirst.getDate());
        assertEquals(Priority.HAUTE, importedFirst.getPriority());
        assertEquals(false, importedFirst.isCompleted());

        Task importedSecond = imported.get(1);
        assertEquals("Rediger le compte-rendu", importedSecond.getTitle());
        assertTrue(importedSecond.isCompleted());
    }

    @Test
    void importedTasksGetFreshIdsRatherThanReusingAny() throws IOException {
        Task original = new Task("Tache", "", LocalDate.of(2026, 1, 1));

        StringWriter out = new StringWriter();
        TaskCsvIO.export(List.of(original), out);
        List<Task> imported = TaskCsvIO.importFrom(new StringReader(out.toString()));

        assertEquals(1, imported.size());
        assertTrue(!imported.get(0).getId().equals(original.getId()) && imported.get(0).getId() != null);
    }

    @Test
    void skipsRowsWithBlankTitleOrInvalidDate() throws IOException {
        String csv = "titre,description,date,priorite,termine\n"
                + ",description,2026-01-01,HAUTE,false\n"
                + "Titre valide,desc,pas-une-date,HAUTE,false\n"
                + "Titre correct,desc,2026-02-01,BASSE,true\n";

        List<Task> imported = TaskCsvIO.importFrom(new StringReader(csv));

        assertEquals(1, imported.size());
        assertEquals("Titre correct", imported.get(0).getTitle());
        assertEquals(Priority.BASSE, imported.get(0).getPriority());
        assertTrue(imported.get(0).isCompleted());
    }

    @Test
    void unknownPriorityFallsBackToDefault() throws IOException {
        String csv = "titre,description,date,priorite,termine\n"
                + "Tache,desc,2026-01-01,INEXISTANTE,false\n";

        List<Task> imported = TaskCsvIO.importFrom(new StringReader(csv));

        assertEquals(1, imported.size());
        assertEquals(Priority.MOYENNE, imported.get(0).getPriority());
    }
}
