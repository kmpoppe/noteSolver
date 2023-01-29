package org.openstreetmap.josm.plugins.notesolver;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.awt.font.*;
import javax.swing.*;
import java.util.*;

import org.openstreetmap.josm.plugins.*;
import org.openstreetmap.josm.spi.preferences.Config;

import org.openstreetmap.josm.tools.I18n;

import org.openstreetmap.josm.*;

public class SettingsDialog {
	public static boolean showSettingsDialog() {
		final JPanel settingsPanel = new JPanel();
		final boolean useLocalLanguageInChangeset  = Config.getPref().getBoolean("noteSolver.useLocalLanguageInChangeset", true);
		final boolean useLocalLanguageInNote       = Config.getPref().getBoolean("noteSolver.useLocalLanguageInNote", true);
		JCheckBox checkUseLocalLanguageInChangeset = new JCheckBox(I18n.tr("Use your language in Changeset Comments"), useLocalLanguageInChangeset);
		JCheckBox checkUseLocalLanguageInNote      = new JCheckBox(I18n.tr("Use your language in Note Comments"), useLocalLanguageInNote);
		final String overrideChangesetComment      = Config.getPref().get("noteSolver.overrideChangesetComment", "");
		final String overrideNoteComment           = Config.getPref().get("noteSolver.overrideNoteComment", "");
		JTextField textOverrideChangesetComment    = new JTextField(overrideChangesetComment);
		JTextField textOverrideNoteComment         = new JTextField(overrideNoteComment);
		Box box = new Box(BoxLayout.Y_AXIS);
		
		box.add(checkUseLocalLanguageInChangeset);
		box.add(checkUseLocalLanguageInNote);
		box.add(Box.createRigidArea(new Dimension(0, 5)));
		box.add(new JSeparator());
		box.add(new JLabel(I18n.tr("Use this changeset comment rather than the default message:")));
		box.add(textOverrideChangesetComment);
		box.add(new JLabel(I18n.tr("Use this Note Comment rather than the default message:")));
		box.add(textOverrideNoteComment);
		box.add(Box.createRigidArea(new Dimension(0, 5)));
		box.add(new JLabel(I18n.tr("Placeholders:")));
		box.add(new JLabel(I18n.tr("##noteSolver:NID## for the Note ID in the changeset comment")));
		box.add(new JLabel(I18n.tr("##noteSolver:CID## for the changeset number in the Note comment"))); 
		box.add(new JLabel(I18n.tr("##noteSolver:LN## for a Link to the Note in the changeset comment")));
		box.add(new JLabel(I18n.tr("##noteSolver:LC## for a Link to the changeset in the Note comment")));
		
		settingsPanel.add(box);
		final int result = 
			JOptionPane.showOptionDialog(
				null, 
				settingsPanel, 
				"noteSolver Settings",
				JOptionPane.OK_CANCEL_OPTION, 
				JOptionPane.PLAIN_MESSAGE,
				null, 
				null, 
				null
			);
		if (result == JOptionPane.OK_OPTION) {
			Config.getPref().putBoolean("noteSolver.useLocalLanguageInChangeset", checkUseLocalLanguageInChangeset.isSelected());
			Config.getPref().putBoolean("noteSolver.useLocalLanguageInNote", checkUseLocalLanguageInNote.isSelected());
			Config.getPref().put("noteSolver.overrideChangesetComment", textOverrideChangesetComment.getText());
			Config.getPref().put("noteSolver.overrideNoteComment", textOverrideNoteComment.getText());
		}
		return true;
	}
}