package org.openstreetmap.josm.plugins.notesolver;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.openstreetmap.josm.actions.*;
import org.openstreetmap.josm.actions.upload.*;

import org.openstreetmap.josm.data.*;
import org.openstreetmap.josm.data.notes.*;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.data.osm.NoteData.*;
import org.openstreetmap.josm.data.osm.event.*;

import org.openstreetmap.josm.gui.*;
import org.openstreetmap.josm.gui.layer.*;
import org.openstreetmap.josm.gui.layer.LayerManager.*;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

import org.openstreetmap.josm.plugins.*;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

public class NoteSolverPlugin extends Plugin {
	static JMenu noteSolverMenu;
	public List<Note> rememberedNotes = new ArrayList<>();
	private List<Note> solvedNotes = new ArrayList<>();
	public Note selectedNote;
	private int lastChangeSet;
	private boolean autoUploadDecision = false;
	NoteSolverPlugin me = this;
	int maxMenuItemLen = 50;

	public NoteSolverPlugin(final PluginInformation info) {
		super(info);
		// Create Menu
		updateMenu();
		setEnabledMenu(false);
		// Register Layer Change Listener for Note Selection and Dataset Change
		
		MainApplication.getLayerManager().addLayerChangeListener(layerChangeListener);
		// Register Upload Hook
		UploadAction.registerUploadHook(uploadHook, false);
	}

	private final UploadHook uploadHook = new UploadHook() {
		@Override
		public boolean checkUpload(APIDataSet apiDataSet) {
			boolean returnValue = true;
			if (rememberedNotes != null && rememberedNotes.size() > 0) {
				String noteList = "";
				for (Note note : solvedNotes) {
					if (rememberedNotes.contains(note)) rememberedNotes.remove(note);
				}
				for (Note note : rememberedNotes) {
					noteList = noteList + (char)13 + (char)10 + noteShortText(note);
				}
				int outVal = JOptionPane.showConfirmDialog(
					null,
					"Automatically Resolve Note" + (rememberedNotes.size() > 1 ? "s" : "") + (char)13 + (char)10 + noteList + (char)13 + (char)10 + "?", 
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
							String noteLink = "Closes https://www.openstreetmap.org/note/" + Long.toString(note.getId());
							comment = comment.replace("; " + noteLink, "");
							comment = comment.replace(noteLink, "");
						}
						for (Note note : rememberedNotes) {
							String noteLink = "Closes https://www.openstreetmap.org/note/" + Long.toString(note.getId());
							comment = (comment != null ? (comment.contains(noteLink) ? comment : comment + "; " + noteLink) : noteLink);
						}
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
				boolean alreadyAdded = false;
				if (rememberedNotes != null) {
					for(Note note : rememberedNotes)
						if (note.getId() == selectedNote.getId()) alreadyAdded = true;
					if (alreadyAdded) rememberedNotes.remove(selectedNote);
				}
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
				boolean alreadyAdded = false;
				if (rememberedNotes != null) {
					for(Note note : rememberedNotes)
						if (note.getId() == selectedNote.getId()) alreadyAdded = true;
					if (!alreadyAdded) rememberedNotes.add(selectedNote);
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
				for (JMenuItem j : mainMenuEntries()) {
					contextMenu.add(j);
				}
				Point p = MainApplication.getMainFrame().getMousePosition();
				contextMenu.setInvoker(MainApplication.getMainFrame().getComponentAt(p));
				contextMenu.setLocation(p);
				contextMenu.setEnabled(true);
				contextMenu.setVisible(true);
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
		public void changeListeners(Layer layer, boolean isRemove) {
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

		@Override
		public void otherDatasetChange(AbstractDatasetChangedEvent event)
		{
			if (event.getType() == AbstractDatasetChangedEvent.DatasetEventType.CHANGESET_ID_CHANGED && autoUploadDecision) {
				Collection<? extends OsmPrimitive> c = event.getPrimitives();
				Iterator<? extends OsmPrimitive> oIter = c.iterator();
				int thisChangeSet = oIter.next().getChangesetId();
				if (lastChangeSet != thisChangeSet) {
					lastChangeSet = thisChangeSet;
					for (Note note : rememberedNotes) {
						NoteData noteData = new NoteData(java.util.Collections.singleton(note));
						noteData.closeNote(note, "Resolved with changeset https://www.openstreetmap.org/changeset/" + Integer.toString(thisChangeSet));
						UploadNotesTask uploadNotesTask = new UploadNotesTask();
						ProgressMonitor pm = null;
						uploadNotesTask.uploadNotes(noteData, pm);
						solvedNotes.add(note);
					}
					rememberedNotes = new ArrayList<>();
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
	};

	public void updateMenu() {
		final MainMenu menu = MainApplication.getMenu();

		if (noteSolverMenu == null) {
			noteSolverMenu = menu.addMenu(
				"Note Solver", 
				"Note Solver", 
				0,
				menu.getDefaultMenuPos(),
				"help"
			);
		} else {
			noteSolverMenu.removeAll();
		}

		if (rememberedNotes != null) {
			for (JMenuItem j : mainMenuEntries()) {
				noteSolverMenu.add(j);
			}
		}
		noteSolverMenu.addSeparator();
		noteSolverMenu.add(new JMenuItem("List of selected Notes"));
		noteSolverMenu.addSeparator();
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
				noteSolverMenu.add(jMenuItem);
			}
		}
	}

	private List<JMenuItem> mainMenuEntries() {
		List<JMenuItem> returnList = new ArrayList<>();
		JMenuItem addAction = new JMenuItem("Remember Note");
		addAction.setToolTipText("Add the selected Note to the list of Notes that should be automatically closed when uploading a changeset");
		addAction.addActionListener(rememberNoteAction);
		addAction.setEnabled((selectedNote != null && rememberedNotes != null && !rememberedNotes.contains(selectedNote) && selectedNote.getState() != Note.State.CLOSED));
		returnList.add(addAction);
		JMenuItem removeAction = new JMenuItem("Forget Note");
		removeAction.setToolTipText("Remove the selected Note from the list of Notes that should be automatically closed when uploading a changeset");
		removeAction.addActionListener(forgetNoteAction);
		removeAction.setEnabled(selectedNote != null && rememberedNotes != null && rememberedNotes.contains(selectedNote));
		returnList.add(removeAction);
		return returnList;
	}

	private String noteShortText(Note n) {
		String firstComment = n.getFirstComment().toString();
		if (firstComment.length() > maxMenuItemLen) firstComment = firstComment.substring(0, maxMenuItemLen) + "...";
		String menuItemText = 
			"* " + Long.toString(n.getId()) + " " +
			"[" + n.getFirstComment().getUser().getName().toString() + 
			" @ " + DateFormat.getDateInstance().format(n.getCreatedAt()) + 
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
		
		for (final Component me : noteSolverMenu.getMenuComponents()) {
			if (me instanceof JMenuItem) {
				((JMenuItem) me).setEnabled(isEnabled);
			}
		}
		
	}
}