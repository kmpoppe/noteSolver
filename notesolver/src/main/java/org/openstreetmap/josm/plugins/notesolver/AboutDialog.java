package org.openstreetmap.josm.plugins.notesolver;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import org.openstreetmap.josm.plugins.*;
import org.openstreetmap.josm.spi.preferences.Config;

import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.OpenBrowser;

import org.openstreetmap.josm.*;

public class AboutDialog {
	public static boolean showAboutDialog() {
		final JPanel aboutPanel = new JPanel();
		Box box = new Box(BoxLayout.Y_AXIS);
		JLabel hyperlink = new JLabel("https://github.com/kmpoppe/noteSolver");
		hyperlink.setForeground(Color.BLUE.darker());
		hyperlink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		hyperlink.addMouseListener(new MouseAdapter() {
 
			@Override
			public void mouseClicked(MouseEvent e) {
				OpenBrowser.displayUrl("https://github.com/kmpoppe/noteSolver");
			}
		 
			@Override
			public void mouseEntered(MouseEvent e) {
				// the mouse has entered the label
			}
		 
			@Override
			public void mouseExited(MouseEvent e) {
				// the mouse has exited the label
			}
		});
		JLabel title = new JLabel(I18n.tr("About noteSolver Plugin"));
		title.setHorizontalAlignment(SwingConstants.CENTER);
		Font f = title.getFont();
		title.setFont(f.deriveFont(f.getStyle() | Font.BOLD));

		box.add(title);
		box.add(Box.createRigidArea(new Dimension(0, 5)));
		box.add(new JSeparator());
		box.add(Box.createRigidArea(new Dimension(0, 5)));
		box.add(new JLabel(I18n.tr("Maintainer:")));
		box.add(new JLabel("Kai Michael Poppe (kmpoppe) "));
		box.add(new JLabel(I18n.tr("Online source code:")));
		box.add(hyperlink);
		box.add(new JLabel(I18n.tr("Contributors:")));
		box.add(new JLabel("Johannes Roessel (ygra), Florian Schaefer (floscher) "));
		box.add(new JLabel("David Moraris Ferreira, David Baumann"));
		box.add(new JLabel(I18n.tr("License: GPL-3.0")));
		box.add(Box.createRigidArea(new Dimension(0, 5)));
		box.add(new JSeparator());
		box.add(Box.createRigidArea(new Dimension(0, 5)));
		box.add(new JLabel(I18n.tr("Translations:")));
		box.add(new JLabel("Korney San (Belarussian, Russian), Olivier Nauwelaers (Dutch) "));
		box.add(new JLabel("David Moraris Ferreira, Laurent, Lejun (French)")); 
		box.add(new JLabel("Benjamin Kleinmanns (German), David Moraris Ferreira (Portugese) "));
		box.add(new JLabel("Andres Gomez (Spanish (CO), Spanish (LA)) "));
		box.add(new JLabel("Carl Rosengren (Swedish) "));
		
		aboutPanel.add(box);
		JOptionPane.showMessageDialog(
			null, 
			aboutPanel, 
			I18n.tr("About noteSolver Plugin"),
			JOptionPane.PLAIN_MESSAGE,
			null
		);
		return true;
	}
}
