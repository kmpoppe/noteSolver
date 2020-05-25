# noteSolver - A JOSM Plugin

This plugin for JOSM allows resolving a Note directly after uploading some changes.

The Changeset comment automatically gets an attachment to the comment showing the URL of the Note.

Once the changes are uploaded, the Note that was referenced gets a comment showing the URL of the Changeset and gets resolved.

While this plugin is not yet published to the JOSM repository, use this description of how to add the plugin to JOSM manually.
## How to install this plugin

Download [notesolver.jar](https://github.com/kmpoppe/noteSolver/releases/latest/download/notesolver.jar) and copy it to "plugins" inside the [user-data directory](https://josm.openstreetmap.de/wiki/Help/Preferences#JOSMpreferencedatacachedirectories).

Afterwards, restart JOSM,  go to settings, Plug-Ins and enable "notesolver". Now you have to restart JOSM again.

## How to use this plugin

The plugin adds a main menu entry called "Note Solver". This menu will list all notes remembered for automatic resolving. Once a Note is selected on either the Map or in the list of Notes, a small Context-Menu-Popup shows that allows remembering or forgetting the Note you just selected.

When you prepare the changes for upload, the Changeset comment will contain "Closes https://www.openstreetmap.org/note/{NoteID}" for every Note that's in the list. If you confirm the question for automatic resolve with Yes, every note will be closed with "Resolved with Changeset https://www.openstreetmap.org/changeset/{ChangesetID}". 
