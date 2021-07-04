package org.openstreetmap.josm.plugins.notesolver;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.actions.*;
import org.openstreetmap.josm.actions.upload.*;
import org.openstreetmap.josm.actions.downloadtasks.*;

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

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

public class NoteSolverPlugin extends Plugin {
	static JMenu noteSolverMenu;
	public NoteList rememberedNotes = new NoteList();
	private NoteList solvedNotes = new NoteList();
	public Note selectedNote;
	private int lastChangeSet;
	private boolean autoUploadDecision = false;
	NoteSolverPlugin me = this;
	int maxMenuItemLen = 50;
	String crLf = "" + (char)13 + (char)10;
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
			if (rememberedNotes != null && rememberedNotes.size() > 0) {
				String noteList = "";
				for (Note note : solvedNotes)
					if (rememberedNotes.containsNote(note)) rememberedNotes.remove(note);

				for (Note note : rememberedNotes)
					noteList = noteList + crLf + noteShortText(note);

				int outVal = JOptionPane.showConfirmDialog(
					null,
					"Automatically Resolve Note" + (rememberedNotes.size() > 1 ? "s" : "") + crLf + noteList + crLf + "?", 
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
						for (Note note : solvedNotes) {
							String noteLink = "Closes " + getUrl(note, linkTypes.NOTE);
							if (comment != null) {
								comment = comment.replace("; " + noteLink, "");
								comment = comment.replace(noteLink, "");
							}
						}
						for (Note note : rememberedNotes) {
							String noteLink = "Closes " + getUrl(note, linkTypes.NOTE);
							comment = (comment != null ? (comment.contains(noteLink) ? comment : comment + "; " + noteLink) : noteLink);
						}
						MainApplication.getLayerManager().getEditDataSet().addChangeSetTag("created_by", "noteSolver_plugin/" + myPluginInformation.version);
						MainApplication.getLayerManager().getEditDataSet().addChangeSetTag("comment", comment);
					}
					returnValue = true;
				}
			}
			return returnValue;
		}
	};

	private final JosmAction forgetNoteAction = new JosmAction() {
		private static final long serialVersionUID = 1927873880648933879L;
		@Override
		public void actionPerformed(ActionEvent event) {
			if (selectedNote == null) {
				JOptionPane.showMessageDialog(null, "No Note selected.");
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
				JOptionPane.showMessageDialog(null, "No Note selected.");
			} else {
				if (rememberedNotes != null && !rememberedNotes.containsNote(selectedNote))
					rememberedNotes.add(selectedNote);
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
				Point p = MainApplication.getMainFrame().getMousePosition();
				if (p != null) {
					Component c = MainApplication.getMainFrame().getComponentAt(p);
					JComponent jc = (JComponent)c;
					if (c != null) {
						contextMenu.setInvoker(c);
						contextMenu.setLocation(p);
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

	private final DataSetListener dataSetListener = new DataSetListener() {
		@Override
		public void otherDatasetChange(AbstractDatasetChangedEvent event)
		{
			ProgressMonitor pm = null;
			if (event.getType() == AbstractDatasetChangedEvent.DatasetEventType.CHANGESET_ID_CHANGED && autoUploadDecision) {
				int thisChangeSet = event.getPrimitives().iterator().next().getChangesetId();
				if (lastChangeSet != thisChangeSet) {
					lastChangeSet = thisChangeSet;
					for (Note note : rememberedNotes) {
						NoteData noteData = new NoteData(java.util.Collections.singleton(note));
						if (note.getState() != State.CLOSED) {
							try {
								noteData.closeNote(note, "Resolved with changeset " + getUrl(thisChangeSet, linkTypes.CHANGESET));
								UploadNotesTask uploadNotesTask = new UploadNotesTask();
								uploadNotesTask.uploadNotes(noteData, pm);
							} catch (Exception e) {
								JOptionPane.showMessageDialog(null, "Note exception: " + e.getMessage());
							}
						}
						solvedNotes.add(note);
					}
					rememberedNotes = new NoteList();
					event.getDataset().addChangeSetTag("comment", "");
					updateMenu();
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
		for (JMenuItem j : mainMenuEntries(menuTypes.MAIN)) {
			noteSolverMenu.add(j);
		}
		noteSolverMenu.addSeparator();
		if (rememberedNotes != null && rememberedNotes.size() > 0) {
			noteSolverMenu.add(new JMenuItem("List of selected Notes"));
			noteSolverMenu.addSeparator();
			for (JMenuItem j : mainMenuEntries(menuTypes.NOTELIST))
				noteSolverMenu.add(j);
		}
	}

	private List<JMenuItem> mainMenuEntries(Enum<menuTypes> menuType) {
		List<JMenuItem> returnList = new ArrayList<>();
		if (menuType == menuTypes.MAIN) {
			boolean bEnaPre = selectedNote != null && rememberedNotes != null;
			boolean bEnaLst = (bEnaPre && rememberedNotes.contains(selectedNote));
			boolean bEnaCls = (bEnaPre && selectedNote.getState() == Note.State.CLOSED);
			JMenuItem addAction = new JMenuItem("Remember Note");
			addAction.setToolTipText("Add the selected Note to the list of Notes that should be automatically closed when uploading a changeset");
			addAction.addActionListener(rememberNoteAction);
			addAction.setEnabled(bEnaPre && !bEnaLst && !bEnaCls);
			returnList.add(addAction);
			JMenuItem removeAction = new JMenuItem("Forget Note");
			removeAction.setToolTipText("Remove the selected Note from the list of Notes that should be automatically closed when uploading a changeset");
			removeAction.addActionListener(forgetNoteAction);
			removeAction.setEnabled(bEnaPre && bEnaLst);
			returnList.add(removeAction);
		} else if (menuType == menuTypes.NOTELIST) {
			if (rememberedNotes != null) {
				for(Note note : rememberedNotes) {
					JMenuItem jMenuItem = new JMenuItem(noteShortText(note));
					jMenuItem.setToolTipText(note.getFirstComment().toString());
					jMenuItem.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent ev) {
							rememberedNotes.remove(note);
							updateMenu();
						}
					});
					jMenuItem.setEnabled(true);
					returnList.add(jMenuItem);
				}
			}
		}
		return returnList;
	}

	private String noteShortText(Note n) {
		String firstComment = n.getFirstComment().toString();
		if (firstComment.length() > maxMenuItemLen) firstComment = firstComment.substring(0, maxMenuItemLen) + "...";
		String menuItemText = 
			"* " + Long.toString(n.getId()) + " " +
			"[" + n.getFirstComment().getUser().getName().toString() + 
			// " @ " + DateFormat.getDateInstance().format(n.getCreatedAt()) + 
			": " + firstComment + 
			"]";
		return menuItemText;
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
		String customServerUrl = Config.getPref().get("osm-server.url");
		String serverUrl = 
			(customServerUrl != null && customServerUrl != "" ? 
			customServerUrl.replace(".org/api", ".org") : 
			"https://www.openstreetmap.org");
		if (!serverUrl.endsWith("/")) serverUrl += "/";
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
	private enum linkTypes {
		NOTE, CHANGESET
	};
	private enum menuTypes {
		MAIN, NOTELIST
	};
}

class NoteList extends ArrayList<Note> {
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
