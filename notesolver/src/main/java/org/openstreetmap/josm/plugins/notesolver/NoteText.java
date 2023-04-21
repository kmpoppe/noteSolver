package org.openstreetmap.josm.plugins.notesolver;

import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.data.notes.*;

public class NoteText {
	private static int maxMenuItemLen = 50;
	public static String noteShortText(Note n) {
		String firstComment = "";
		String firstUser = "";
		if (n.getFirstComment() != null) {
			firstUser = n.getFirstComment().getUser().getName().toString();
			firstComment = n.getFirstComment().toString();
			if (firstComment.length() > maxMenuItemLen) firstComment = firstComment.substring(0, maxMenuItemLen) + "...";
		} else {
			firstUser = I18n.tr("Deleted user");
			firstComment = I18n.tr("Deleted comment");
		}
		String menuItemText =   
			"* " + Long.toString(n.getId()) + " " +
			"[" + firstUser + 
			": " + firstComment + 
			"]";
		return menuItemText;
	}
}