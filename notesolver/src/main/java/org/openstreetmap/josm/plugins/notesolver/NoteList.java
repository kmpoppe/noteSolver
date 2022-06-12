package org.openstreetmap.josm.plugins.notesolver;

import org.openstreetmap.josm.data.notes.*;

import java.util.ArrayList;

public class NoteList extends ArrayList<Note> {
	private static final long serialVersionUID = 1L;
	public boolean containsNote(Note checkNote) {
		return
			this.stream()
			.filter((note) -> note.getId() == checkNote.getId())
			.findFirst()
			.orElse(null)
			!= null;
	}
}
