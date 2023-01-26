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
		final boolean useLocalLanguageInChangeset = Config.getPref().getBoolean("noteSolver.useLocalLanguageInChangeset", true);
		final boolean useLocalLanguageInNote      = Config.getPref().getBoolean("noteSolver.useLocalLanguageInNote", true);
		JCheckBox checkUseLocalLanguageInChangeset = new JCheckBox(I18n.tr("Use your language in Changeset Comments"), useLocalLanguageInChangeset);
		JCheckBox checkUseLocalLanguageInNote = new JCheckBox(I18n.tr("Use your language in Note Comments"), useLocalLanguageInNote);
		Box box = new Box(BoxLayout.Y_AXIS);
		
		box.add(checkUseLocalLanguageInChangeset);
		box.add(Box.createRigidArea(new Dimension(0, 5)));
		box.add(checkUseLocalLanguageInNote);
		settingsPanel.add(box);
		final int result = JOptionPane.showOptionDialog(null, settingsPanel, "noteSolver Settings",
		JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE,
		null, null, null);
		// String x1 = (useLocalLanguageInChangeset ? "yes" : "no");
		// String x2 = (useLocalLanguageInNote ? "yes": "no");
		// JOptionPane.showMessageDialog(null, x1 + "*" + x2);
		return true;
	}
}