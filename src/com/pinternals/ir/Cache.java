package com.pinternals.ir;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javafx.util.Pair;

import org.apache.commons.lang3.StringEscapeUtils;

public class Cache {
	private static final Charset utf8 = NotesRetriever.utf8;
	public static final FileSystem fs = FileSystems.getDefault();
	Map<Integer,Path> cacheRawVer = new HashMap<Integer,Path>();
	Path root, supportSapComNotes, notesdb, launchpad;

	Cache(String d) throws IOException {
		root = fs.getPath(d);
		assert Files.isDirectory(root);
		
		notesdb = root.resolve("notes.db");
		assert Files.isRegularFile(notesdb) : "notes.db isn't exist";
		
		supportSapComNotes = root.resolve("support-sap-com-notes");
		if (!Files.exists(supportSapComNotes)) Files.createDirectory(supportSapComNotes);

		launchpad = root.resolve("launchpad");
		if (!Files.exists(launchpad)) Files.createDirectory(launchpad);
	}
	Iterator<Path> getSupportSapComNotesZip() throws IOException {
		DirectoryStream<Path> ex = Files.newDirectoryStream(supportSapComNotes, 
				"notes_[1-2][0-9][0-9][0-9][0-9][0-9][0-9][0-9]--[1-2][0-9][0-9][0-9][0-9][0-9][0-9][0-9].zip");
		return ex.iterator();
	}
	Path getLaunchpadArealist() throws IOException {
		return launchpad.resolve("arealist.txt");
	}
	Path getTmpDir() throws IOException {
		Path p = Files.createTempDirectory(root, "tmp_sup_");
		return p;
	}
	void storeXml(ParseIndexContext ctx, Path xml) throws IOException {
		Files.createFile(xml);
		PrintWriter p = new PrintWriter(Files.newBufferedWriter(xml, utf8));
		p.print("<?xml version='1.1' encoding='" + NotesRetriever.utf8.name() + "'?><snotes>");

		ctx.list.sort(Comparator.comparing(o1 -> o1.number ));
		for (NoteListItem i: ctx.list) {
			p.print("\n<n");
			p.print(" n='" + i.number + "'");
			if (i.mark!=null) p.print(" m='" + i.mark + "'");
			p.print(" a='" + i.apparea + "'");
			p.print(" l='" + i.asklangu + "'");
			p.print(" d='" + DateTimeFormatter.ISO_LOCAL_DATE.format(i.date) + "'");
			p.print(" c='" + i.category + "'");
			p.print(" p='" + i.priority + "'");
			if (i.objid!=null) p.print(" o='" + i.objid + "'");
			p.print(">" + StringEscapeUtils.escapeXml11(i.title) + "</n>");
		}
		p.flush();
		for (Pair<String,String> a: ctx.lareas) {
			p.print("\n<area rcode='"+a.getKey()+"' value='" + a.getValue() +"'/>");
		}
		p.flush();
//		p.print("\n<!-- [Statistics]");
//		if (dsd==0 && dod==0) {
//			p.print("\tNotes collected: " + q + ", no duplicates found");
//		} else {
//			p.print("\nUnique notes collected: " + q);
//			p.print("\nDuplicate notes with the same date: " + dsd);
//			p.print("\nDuplocate notes with the other date: " + dod);
//		}
//		p.println("\t-->");
		p.print("\n</snotes>");
		p.close();
	}
}
