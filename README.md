# noteSolver - A JOSM Plugin
[![Twitter](https://img.shields.io/badge/Twitter-@kmpoppe-1DA1F2.svg?style=for-the-badge&logo=twitter)](https://twitter.com/kmpoppe)
[![Custom badge](https://img.shields.io/static/v1?label=TELEGRAM&message=%40kmpoppe&color=0088ff&logo=telegram&style=for-the-badge)](https://t.me/kmpoppe)
[![GitHub All Releases](https://img.shields.io/github/downloads/kmpoppe/noteSolver/total?style=for-the-badge)](https://github.com/kmpoppe/noteSolver/releases/latest)
[<img src="https://cdn.buymeacoffee.com/buttons/default-blue.png" height="28">](https://www.buymeacoffee.com/kmpoppe)

This plugin for JOSM allows resolving a Note directly after uploading some changes.

The Changeset comment automatically gets an attachment to the comment showing the URL of the Note.

Once the changes are uploaded, the Note that was referenced gets a comment showing the URL of the Changeset and gets resolved.

This plugin is now listed in the global JOSM plugin list.

## Contributing

If you want to contribute to this plugin, please read [CONTRIBUTING.md](/CONTRIBUTING.md).

## How to install this plugin manually

Download [notesolver.jar](https://github.com/kmpoppe/noteSolver/releases/latest/download/notesolver.jar) and copy it to "plugins" inside the [user-data directory](https://josm.openstreetmap.de/wiki/Help/Preferences#JOSMpreferencedatacachedirectories).

Afterwards, restart JOSM,  go to settings, Plug-Ins and enable "notesolver". Now you have to restart JOSM again.

## How to use this plugin

The plugin adds a main menu entry called "Note Solver". This menu will list all notes remembered for automatic resolving. Once a Note is selected on either the Map or in the list of Notes, a small Context-Menu-Popup shows that allows remembering or forgetting the Note you just selected.

When you prepare the changes for upload, the Changeset comment will contain "Closes https://www.openstreetmap.org/note/{NoteID}" for every Note that's in the list. If you confirm the question for automatic resolve with Yes, every note will be closed with "Resolved with Changeset https://www.openstreetmap.org/changeset/{ChangesetID}". 
