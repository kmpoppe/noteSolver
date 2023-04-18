package org.openstreetmap.josm.plugins.notesolver;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.actions.*;
import org.openstreetmap.josm.actions.upload.*;

import org.openstreetmap.josm.data.*;
import org.openstreetmap.josm.data.notes.*;
import org.openstreetmap.josm.data.notes.Note.State;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.data.osm.NoteData.*;
import org.openstreetmap.josm.data.osm.event.*;

import org.openstreetmap.josm.gui.*;
import org.openstreetmap.josm.gui.layer.*;
import org.openstreetmap.josm.gui.layer.LayerManager.*;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

import org.openstreetmap.josm.plugins.*;
import org.openstreetmap.josm.spi.preferences.Config;

import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.OpenBrowser;

import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.*;
import javax.swing.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class NoteSolverPlugin extends Plugin {
	static JMenu noteSolverMenu;
	public NoteList rememberedNotes = new NoteList();
	private NoteList solvedNotes = new NoteList();
	public Note selectedNote;
	private int lastChangeSet;
	private boolean autoUploadDecision = false;
	private int requiresUploadCount = 0;

	NoteSolverPlugin me = this;
	private PluginInformation myPluginInformation;

	public NoteSolverPlugin(final PluginInformation info) {
		super(info);
		myPluginInformation = info;
		// Create Menu
		createMenu();
		updateMenu();
		// Register Layer Change Listener for Note Selection and Dataset Change
		// ...andFire... added so that the Plugin can be loaded at runtime and
		// the event handlers get loaded anyway.
		MainApplication.getLayerManager().addAndFireLayerChangeListener(layerChangeListener);
		// Check if a Note is already selected when the plugin gets loaded.
		if (
			MainApplication.getLayerManager().getNoteLayer() != null
			&&
			MainApplication.getLayerManager().getNoteLayer().getNoteData() != null
			&&
			MainApplication.getLayerManager().getNoteLayer().getNoteData().getSelectedNote() != null
		) {
			selectedNote = MainApplication.getLayerManager().getNoteLayer().getNoteData().getSelectedNote();
			updateMenu();
		}
		// Register Upload Hook
		UploadAction.registerUploadHook(uploadHook, false);
	}

	private final UploadHook uploadHook = new UploadHook() {
		@Override
		public boolean checkUpload(APIDataSet apiDataSet) {
			boolean returnValue = true;
			MainApplication.getLayerManager().getEditDataSet().getChangeSetTags().remove("closed:note");
			// check online status before asking whether to close notes
			if (rememberedNotes != null && rememberedNotes.size() > 0) {
				for (Note note : rememberedNotes) {
					String cOnlineStatus = getOnlineNoteStatus(note.getId());
					if (note.getState() == State.CLOSED || cOnlineStatus.toLowerCase().trim().equals("closed")) {
						solvedNotes.add(note);
					}
				}
				for (Note note : solvedNotes)
					if (rememberedNotes.containsNote(note)) rememberedNotes.remove(note);
			}
			if (rememberedNotes != null && rememberedNotes.size() > 0) {

				StringBuilder noteList = new StringBuilder();
				rememberedNotes.forEach((n) -> noteList.append("\n" + NoteText.noteShortText(n)));

				int outVal = JOptionPane.showConfirmDialog(
					null,
					I18n.trn("Automatically resolve Note\n{0}?", "Automatically resolve Notes\n{0}?", rememberedNotes.size(), noteList),
					null, 
					JOptionPane.YES_NO_CANCEL_OPTION, 
					JOptionPane.QUESTION_MESSAGE
				);
				if (outVal == JOptionPane.CANCEL_OPTION) {
					returnValue = false;
				} else {
					autoUploadDecision = (outVal == JOptionPane.YES_OPTION);
					if (autoUploadDecision) {
						String comment = MainApplication.getLayerManager().getEditDataSet().getChangeSetTags().get("comment");
						String closedNoteTag = "";
						String noteLink = "";
						for (Note note : solvedNotes) {
							noteLink = getChangesetComment(note);
							if (comment != null) {
								comment = comment.replace("; " + noteLink, "");
								comment = comment.replace(noteLink, "");
							}
						}
						for (Note note : rememberedNotes) {
							noteLink = getChangesetComment(note);
							comment = (comment != null ? (comment.contains(noteLink) ? comment : comment + "; " + noteLink) : noteLink);
							closedNoteTag = closedNoteTag + (closedNoteTag != "" ? ";" : "") + Long.toString(note.getId());
						}
						MainApplication.getLayerManager().getEditDataSet().addChangeSetTag("created_by", "noteSolver_plugin/" + myPluginInformation.version);
						MainApplication.getLayerManager().getEditDataSet().addChangeSetTag("comment", comment);
						if (closedNoteTag == "")
						{
							MainApplication.getLayerManager().getEditDataSet().getChangeSetTags().remove("closed:note");
						} else 
						{
							MainApplication.getLayerManager().getEditDataSet().addChangeSetTag("closed:note", closedNoteTag);
						}
					}
					returnValue = true;
				}
			}
			return returnValue;
		}
	};

	private final JosmAction settingsDialogMenu = new JosmAction() {
		private static final long serialVersionUID = 1927873880648933878L;
		@Override
		public void actionPerformed(ActionEvent event) {
			SettingsDialog.showSettingsDialog();
		}
	};
	private final JosmAction aboutDialog = new JosmAction() {
		private static final long serialVersionUID = 1927873880648933877L;
		@Override
		public void actionPerformed(ActionEvent event) {
			AboutDialog.showAboutDialog();
		}
	};

	private final JosmAction forgetNoteAction = new JosmAction() {
		private static final long serialVersionUID = 1927873880648933879L;
		@Override
		public void actionPerformed(ActionEvent event) {
			if (selectedNote == null) {
				JOptionPane.showMessageDialog(null, I18n.tr("No Note selected."));
			} else {
				if (rememberedNotes != null && rememberedNotes.containsNote(selectedNote))
					rememberedNotes.remove(selectedNote);
			}
			updateMenu();
		}
	};
	private final JosmAction rememberNoteAction = new JosmAction() {
		private static final long serialVersionUID = 1927873880648933880L;
		@Override
		public void actionPerformed(ActionEvent event) {
			if (selectedNote == null) {
				JOptionPane.showMessageDialog(null, I18n.tr("No Note selected."));
			} else {
				if (rememberedNotes != null && !rememberedNotes.containsNote(selectedNote))
					rememberedNotes.add(selectedNote);
			}
			updateMenu();
		}
	};
	private final JosmAction openNoteAction = new JosmAction() {
		private static final long serialVersionUID = 1927873880648933881L;
		@Override
		public void actionPerformed(ActionEvent event) {
			if (selectedNote == null) {
				JOptionPane.showMessageDialog(null, I18n.tr("No Note selected."));
			} else {
				if (selectedNote != null) {
					OpenBrowser.displayUrl(getServerUrl() + "note/" + Long.toString(selectedNote.getId()));
				}
			}
			updateMenu();
		}
	};
	private final NoteDataUpdateListener noteDataUpdateListener = new NoteDataUpdateListener() {
		@Override
		public void selectedNoteChanged(NoteData noteData) {
			selectedNote = noteData.getSelectedNote();
			if (selectedNote != null) {
				JPopupMenu contextMenu = new JPopupMenu();
				for (JMenuItem menuItem : mainMenuEntries(menuTypes.MAIN)) 
					contextMenu.add(menuItem);
				Dimension size = contextMenu.getPreferredSize();
				MainFrame frame = MainApplication.getMainFrame();
				Point p = frame.getMousePosition();
				if (p != null) {
					Component c = MainApplication.getMainFrame().getComponentAt(p);
					SwingUtilities.convertPointToScreen(p, frame);
					JComponent jc = (JComponent)c;
					if (c != null) {
						contextMenu.setInvoker(c);
						// Show menu slightly above the desired location; otherwise we clash with the
						// note content shown in the map.
						contextMenu.setLocation(p.x, p.y - size.height - 15);
						jc.setComponentPopupMenu(contextMenu);
						contextMenu.setEnabled(true);
						contextMenu.setVisible(true);
					}
				}
			}
		}
		@Override
		public void noteDataUpdated(NoteData noteData) {
			// nothing to do here
		}
	};

	private final LayerChangeListener layerChangeListener = new LayerChangeListener() {
		@Override
		public void layerAdded(LayerAddEvent e) {
			changeListeners(e.getAddedLayer(), false);
		}

		@Override
		public void layerRemoving(LayerRemoveEvent e) {
			changeListeners(e.getRemovedLayer(), true);
		}

		@Override
		public void layerOrderChanged(LayerOrderChangeEvent e) {
			// nothing to do here
		}

		public final void changeListeners(Layer layer, boolean isRemove) {
			if (layer instanceof OsmDataLayer) {
				DataSet ds = ((OsmDataLayer) layer).getDataSet();
				if (!isRemove) {
					ds.addDataSetListener(dataSetListener);
					layer.addPropertyChangeListener(propertyChangeListener);
				} else {
					ds.removeDataSetListener(dataSetListener);
				}
			} else if (layer instanceof NoteLayer) {
				NoteData notes = ((NoteLayer) layer).getNoteData();
				if (!isRemove) {
					notes.addNoteDataUpdateListener(noteDataUpdateListener);
				} else {
					notes.removeNoteDataUpdateListener(noteDataUpdateListener);
				}
			}
		}
	};

	private final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent e) {
			if (e.getPropertyName().endsWith("OsmDataLayer.requiresUploadToServer")) {
				requiresUploadCount += ((Boolean)e.getNewValue() ? 1 : -1);
			}
		}
	};

	private final DataSetListener dataSetListener = new DataSetListener() {
		@Override
		public void otherDatasetChange(AbstractDatasetChangedEvent event)
		{
			ProgressMonitor pm = null;
			if (event.getType() == AbstractDatasetChangedEvent.DatasetEventType.CHANGESET_ID_CHANGED && autoUploadDecision) {				
				lastChangeSet = ((ChangesetIdChangedEvent) event).getNewChangesetId();
				if (requiresUploadCount > 0 && lastChangeSet > 0) {
					for (Note note : rememberedNotes) {
						String cOnlineStatus = getOnlineNoteStatus(note.getId());
						NoteData noteData = new NoteData(java.util.Collections.singleton(note));
						String noteComment = getNoteComment(lastChangeSet);
						if (note.getState() == State.OPEN && cOnlineStatus.toLowerCase().trim().equals("open")) {
							try {
								noteData.closeNote(note, noteComment);
								UploadNotesTask uploadNotesTask = new UploadNotesTask();
								uploadNotesTask.uploadNotes(noteData, pm);
							} catch (Exception e) {
								JOptionPane.showMessageDialog(null, I18n.tr("Upload Note exception:\n{0}", e.getMessage()));
							}
						} else {
							JOptionPane.showMessageDialog(null, I18n.tr("Note {0} was already closed outside of JOSM", Long.toString(note.getId())));
						}
						solvedNotes.add(note);
					}
					rememberedNotes = new NoteList();
					event.getDataset().addChangeSetTag("comment", "");
					event.getDataset().addChangeSetTag("closed:note", "");
					updateMenu();
					autoUploadDecision = false;
					lastChangeSet = 0;
				}
			}
		}

		@Override
		public void dataChanged(DataChangedEvent event) {
		}

		@Override
		public void nodeMoved(NodeMovedEvent event) {
		}

		@Override
		public void wayNodesChanged(WayNodesChangedEvent event) {
		}

		@Override
		public void tagsChanged(TagsChangedEvent event) {
		}

		@Override
		public void relationMembersChanged(RelationMembersChangedEvent event) {
		}

		@Override
		public void primitivesRemoved(PrimitivesRemovedEvent event) {
		}

		@Override
		public void primitivesAdded(PrimitivesAddedEvent event) {
		}
	};

	private void createMenu() {
		final MainMenu menu = MainApplication.getMenu();
		if (noteSolverMenu == null) {
			noteSolverMenu = menu.addMenu(
				"Note Solver", 
				"Note Solver", 
				0,
				menu.getDefaultMenuPos(),
				"help"
			);
		}
	}

	public void updateMenu() {
		noteSolverMenu.removeAll();
		noteSolverMenu.add(createMenuItem(I18n.tr("Settings"), I18n.tr("Plugin settings"), settingsDialogMenu, true));
		noteSolverMenu.addSeparator();
		for (JMenuItem j : mainMenuEntries(menuTypes.MAIN)) {
			noteSolverMenu.add(j);
		}
		noteSolverMenu.addSeparator();
		if (rememberedNotes != null && rememberedNotes.size() > 0) {
			noteSolverMenu.add(new JMenuItem(I18n.tr("List of selected Notes")));
			noteSolverMenu.addSeparator();
			for (JMenuItem j : mainMenuEntries(menuTypes.NOTELIST))
				noteSolverMenu.add(j);
			noteSolverMenu.addSeparator();
		}
		noteSolverMenu.add(createMenuItem(I18n.tr("About"), I18n.tr("About noteSolver Plugin"), aboutDialog, true));
	}

	private List<JMenuItem> mainMenuEntries(Enum<menuTypes> menuType) {
		List<JMenuItem> returnList = new ArrayList<>();
		if (menuType == menuTypes.MAIN) {
			boolean bEnaOpn = selectedNote != null;
			boolean bEnaPre = selectedNote != null && rememberedNotes != null;
			boolean bEnaLst = (bEnaPre && rememberedNotes.contains(selectedNote));
			boolean bEnaCls = (bEnaPre && selectedNote.getState() == Note.State.CLOSED);
			returnList.add(
				createMenuItem(
					I18n.tr("Open in browser"),
					I18n.tr("Try to open the Note on osm.org with the operating system's standard browser"),
					openNoteAction, bEnaOpn
				)
			);
			returnList.add(
				createMenuItem(
					I18n.tr("Remember Note"), 
					I18n.tr("Add the selected Note to the list of Notes that should be automatically closed when uploading a changeset"), 
					rememberNoteAction, bEnaPre && !bEnaLst && !bEnaCls
				)
			);
			returnList.add(
				createMenuItem(
					I18n.tr("Forget Note"), 
					I18n.tr("Remove the selected Note from the list of Notes that should be automatically closed when uploading a changeset"), 
					forgetNoteAction, bEnaPre && bEnaLst
				)
			);
		} else if (menuType == menuTypes.NOTELIST) {
			if (rememberedNotes != null) {
				for(Note note : rememberedNotes) {
					returnList.add(
						createMenuItem(
							NoteText.noteShortText(note), 
							note.getFirstComment().toString(),
							new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent ev) {
									rememberedNotes.remove(note);
									updateMenu();
								}
							}, true
						)
					);
				}
			}
		}
		return returnList;
	}

	private JMenuItem createMenuItem(String title, String tooltip, ActionListener al, boolean enabled) {
		JMenuItem menuItem = new JMenuItem(title);
		menuItem.setToolTipText(tooltip);
		menuItem.addActionListener(al);
		menuItem.setEnabled(enabled);
		return menuItem;
	} 

	@Override
	public void mapFrameInitialized(final MapFrame oldFrame, final MapFrame newFrame) {
		if (newFrame == null)
			setEnabledMenu(false); 
		else {
			setEnabledMenu(true);
		}
	}

	private void setEnabledMenu(final boolean isEnabled) {
		for (final Component menuItem : noteSolverMenu.getMenuComponents()) {
			if (menuItem instanceof JMenuItem) {
				((JMenuItem) menuItem).setEnabled(isEnabled);
			}
		}
	}

	private String getUrl(Object inputObject, Enum<linkTypes> linkType) {
		String returnValue = "";
		String serverUrl = getServerUrl();
		Long thisNumber = 0L;
		if (linkType == linkTypes.NOTE) {
			thisNumber = ((Note)inputObject).getId();
		} else if (linkType == linkTypes.CHANGESET) {
			thisNumber = Integer.toUnsignedLong((Integer)inputObject);
		}
		if (thisNumber > 0L) {
			returnValue = serverUrl + linkType.name().toLowerCase() + "/" + Long.toString(thisNumber);
		}
		return returnValue;
	}

	private String getOnlineNoteStatus(long noteId) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		String result = "";
		String apiUrl = getServerUrl() + "api/0.6/notes/" + Long.toString(noteId);

		try {
			URLConnection conn = new URL(apiUrl).openConnection();
			try (InputStream is = conn.getInputStream()) {
				dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				DocumentBuilder dBuilder = dbf.newDocumentBuilder();
				Document doc = dBuilder.parse(is);
				Element element = doc.getDocumentElement();

				NodeList nodeList = element.getElementsByTagName("status");
				if (nodeList.getLength() > 0) {
					result = nodeList.item(0).getTextContent();
				}
			}
		} catch (Exception e) {
			result = "failure";
		}

		return result;
	}

	private static String getServerUrl() {
		final String customServerUrl = Config.getPref().get("osm-server.url");
		String serverUrl = 
			(customServerUrl != null && customServerUrl != "" ? 
			customServerUrl.replace(".org/api", ".org") : 
			"https://www.openstreetmap.org");
		if (!serverUrl.endsWith("/")) serverUrl += "/";
		return serverUrl;
	}

	private String getChangesetComment(Note note) {
		String retVal = String.format("Closes %s", getUrl(note, linkTypes.NOTE));
		if (Config.getPref().get("noteSolver.overrideChangesetComment") != "") {
			retVal = Config.getPref().get("noteSolver.overrideChangesetComment");
			retVal = retVal.replace("##noteSolver:NID##", Long.toString(note.getId()));
			retVal = retVal.replace("##noteSolver:LN##", getUrl(note, linkTypes.NOTE));
		} else {
			if (Config.getPref().getBoolean("noteSolver.useLocalLanguageInChangeset")) {
				retVal = I18n.tr("Closes {0}", getUrl(note, linkTypes.NOTE));
			}
		}
		return retVal;
	}
	private String getNoteComment(Integer changesetID) {
		String retVal = String.format("Resolved with changeset %s by noteSolver_plugin/%s", getUrl(changesetID, linkTypes.CHANGESET), myPluginInformation.version);
		if (Config.getPref().get("noteSolver.overrideNoteComment") != "") {
			retVal = Config.getPref().get("noteSolver.overrideNoteComment");
			retVal = retVal.replace("##noteSolver:CID##", Integer.toString(changesetID));
			retVal = retVal.replace("##noteSolver:LC##", getUrl(changesetID, linkTypes.CHANGESET));
		} else {
			if (Config.getPref().getBoolean("noteSolver.useLocalLanguageInNote")) {
				retVal = I18n.tr("Resolved with changeset {0} by noteSolver_plugin/{1}", getUrl(changesetID, linkTypes.CHANGESET), myPluginInformation.version);
			}
		}
		return retVal;
	}

	private enum linkTypes {
		NOTE, CHANGESET
	};
	private enum menuTypes {
		MAIN, NOTELIST
	};
}
