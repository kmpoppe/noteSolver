package org.openstreetmap.josm.plugins.notesolver;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class MultiBrowserOpen {

	public static boolean openUrl(String url) {
		// OUT("\nWelcome to Multi Brow Pop.\nThis aims to popup a browsers in multiple operating systems.\nGood luck!\n");

		// String url = "http://www.birdfolk.co.uk/cricmob";
		// OUT("We're going to this page: "+ url);

		String myOS = System.getProperty("os.name").toLowerCase();
		// OUT("(Your operating system is: "+ myOS +")\n");

		boolean returnValue = true;

		try {
			if (Desktop.isDesktopSupported()) { // Probably Windows
				// OUT(" -- Going with Desktop.browse ...");
				Desktop desktop = Desktop.getDesktop();
				desktop.browse(new URI(url));
			} else { // Definitely Non-windows
				Runtime runtime = Runtime.getRuntime();
				if (myOS.contains("mac")) { // Apples
					// OUT(" -- Going on Apple with 'open'...");
					runtime.exec("open " + url);
				} 
				else if (myOS.contains("nix") || myOS.contains("nux")) { // Linux flavours 
					// OUT(" -- Going on Linux with 'xdg-open'...");
					runtime.exec("xdg-open " + url);
				}
				else {
					returnValue = false;
				}
			}
			return returnValue;
		}
		catch(IOException | URISyntaxException eek) {
			return false;
		}
	}
}