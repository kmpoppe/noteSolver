package org.openstreetmap.josm.plugins.notesolver;

import org.openstreetmap.josm.data.notes.*;

public class NoteText {
    private static int maxMenuItemLen = 50;
    public static String noteShortText(Note n) {
		String firstComment = n.getFirstComment().toString();
		if (firstComment.length() > maxMenuItemLen) firstComment = firstComment.substring(0, maxMenuItemLen) + "...";
		String menuItemText =   
			"* " + Long.toString(n.getId()) + " " +
			"[" + n.getFirstComment().getUser().getName().toString() + 
			": " + firstComment + 
			"]";
		return menuItemText;
	}
}