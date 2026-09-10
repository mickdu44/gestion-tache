package com.gestiontache.controller;

import com.gestiontache.model.SubTask;
import com.gestiontache.model.Task;

/** Called when a subtask is checked/unchecked directly from a Kanban postit card. */
@FunctionalInterface
interface SubtaskToggleHandler {
    void toggle(Task task, SubTask subTask, boolean completed);
}
