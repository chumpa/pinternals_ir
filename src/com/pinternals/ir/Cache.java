package com.pinternals.ir;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javafx.util.Pair;

import org.apache.commons.lang3.StringEscapeUtils;

public class Cache {
	static FileSystem fs = FileSystems.getDefault();
	Map<Integer,Path> cacheRawVer = new HashMap<Integer,Path>();
	Path root, supportSapComNotes, notesdb, launchpad;

	Path rawdir, xmldir, rawver, dirtyver, dirtybak, newdir, newnotes;
	Cache(String d) throws IOException {
		root = fs.getPath(d);
		assert Files.isDirectory(root);
		
		notesdb = fs.getPath(d, "notes.db");
		assert Files.isRegularFile(notesdb) : "notes.db isn't exist";
		
		supportSapComNotes = fs.getPath(d, "support-sap-com-notes");
		if (!Files.exists(supportSapComNotes)) Files.createDirectory(supportSapComNotes);

		launchpad = fs.getPath(d, "launchpad");
		if (!Files.exists(launchpad)) Files.createDirectory(launchpad);
		
		rawdir = fs.getPath(d, "raw_lists");
//		assert Files.isDirectory(rawdir);
		xmldir = fs.getPath(d, "xml");
//		assert Files.isDirectory(xmldir);
		rawver = fs.getPath(d, "raw_ver");
//		assert Files.isDirectory(rawver);
		dirtyver = fs.getPath(d, "raw_ver", "dirty.txt");
		dirtybak = fs.getPath(d, "raw_ver", "dirty.bak");
		newdir = fs.getPath(d, "newnotes");
//		assert Files.isDirectory(newdir);
		newnotes = fs.getPath(d, "newnotes", "newnotes.txt");
	}
	boolean checkStructure(boolean throwError) {
		return false;
	}
	static List<Pair<Character,Path>> filterRawFiles(Path where) throws IOException {
		assert where!=null;
		List<Pair<Character,Path>> xs = new ArrayList<Pair<Character,Path>>(100);

		for (Path x: Files.newDirectoryStream(where, "*.zip")) {
			String z = x.getFileName().toString();
			if (!z.matches("notes_[0-9]{8}--[0-9]{8}_[DEJ]\\.zip")) continue;
			char lan = z.charAt(z.length()-5);
			assert lan=='D'||lan=='E'||lan=='J';
			xs.add(new Pair<Character,Path>(lan, x));
		}
		return xs;
	}
	Iterator<Path> a() throws IOException {
		DirectoryStream<Path> ex = Files.newDirectoryStream(supportSapComNotes, 
				"notes_[1-2][0-9][0-9][0-9][0-9][0-9][0-9][0-9]--[1-2][0-9][0-9][0-9][0-9][0-9][0-9][0-9].zip");
		return ex.iterator();
	}
	List<Pair<Character,Path>> getRawFiles(boolean onlyupdated) throws IOException {
		List<Pair<Character,Path>> z = filterRawFiles(rawdir);
		if (!onlyupdated) return z;
		for (Iterator<Pair<Character,Path>> it = z.iterator(); it.hasNext();) {
			Pair<Character,Path> p = it.next();
			Path raw = p.getValue();
			Path xml = getPathXml(raw);
			if (!Files.isRegularFile(xml)) continue;
			FileTime rawtime = Files.getLastModifiedTime(raw);
			FileTime xmltime = Files.getLastModifiedTime(xml);
			if (xmltime.compareTo(rawtime)>=0) it.remove(); 
		}
		return z;
	}
	Path getPathXml(Path raw) {
		String n = raw.getFileName().toString();
		n = n.substring(0, n.lastIndexOf(".zip")) + ".xml";
		return Paths.get(xmldir.toString(), n);
	}
	Path getTmpDir() throws IOException {
		Path p = Files.createTempDirectory(root, "tmp_sup_");
		return p;
	}
	void storeXml(ParseIndexContext ctx, Path xml) throws IOException {
		File f = xml.toFile();
		f.createNewFile();
		PrintWriter p = new PrintWriter(f, NotesRetriever.utf8.name());
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
	
	Path rawver(int number) throws IOException {
		assert number > 0;
		Path x = cacheRawVer.get(number);
		if (x!=null) return x; 
		Path f = rawver;
		String s = String.format("%07d", number), t;	// 0000001-9999999
		int i = 0, j = s.length(), sz = 3;
		while (i<j) {
			t = s.substring(i, Math.min(i+sz, s.length()));
			Path g = fs.getPath(f.toString(), t);
			if (!Files.isDirectory(g)) Files.createDirectory(g);
			f = g;
			i+=sz;
			sz++;
		}
		cacheRawVer.put(number, f);
		return f;
	}
	static boolean isLang(String lang) {
		return lang!=null && (lang.equals("E") || lang.equals("D") || lang.equals("J"));
	}
	Path getPathByScheme(EScheme scheme, int number, String lang, Object ... args) throws IOException {
		assert number > 0;
		Path dir = rawver(number);
		switch (scheme) {
		case CompareVersions:
			assert isLang(lang);
			return fs.getPath(dir.toString(), "compare_" + lang + ".html");
			
		case ZVersions:
			return fs.getPath(dir.toString(), "versions.xml");
			
		case NotePrintVersion:
			assert isLang(lang) && args.length>0;
			return fs.getPath(dir.toString(), number + "_" + args[0] + "_print_" + lang + ".html");
			
		case NoteLastFancyVersion:
			assert isLang(lang) && args.length>0;
			return fs.getPath(dir.toString(), number + "_" + args[0] + "_fancy_" + lang + ".html");

		case NoteChangeLog:
			assert lang==null;
			return fs.getPath(dir.toString(), "changelog.html");

		case InternalMemo:
			assert lang==null;
			return fs.getPath(dir.toString(), "intmemo.html");

		case ZRoot:
			assert lang==null;
			return dir;

		case PutDownloadBasket:
			assert lang==null;
			return fs.getPath(dir.toString(), "dl.html");
			
		default:
			throw new RuntimeException("Unsupported scheme: " + scheme + " for note " + number);
		}
	}
	List<Pair<Integer, Path>> getRawVers(boolean onlyupdated) throws IOException {
		List<Pair<Integer, Path>> rez = new ArrayList<Pair<Integer, Path>>(1000);
		if (!Files.isRegularFile(dirtyver)) {
			System.out.println("Warning: no dirty versions");
			return rez;
		}
		Files.deleteIfExists(dirtybak);
		Scanner a = new Scanner(dirtyver);
		while (a.hasNextLine()) {
			String x = a.nextLine();
			String y[] = x.split("\t");
			int number = Integer.valueOf(y[0]);
			Pair<Integer,Path> z = new Pair<Integer,Path>(number, rawver(number));
			rez.add(z);
		}
		a.close();
		return rez;
	}
//	List<AZ> getZ2(String area) throws IOException {
//		List<AZ> x = new ArrayList<AZ>(1000);
//		Scanner a = new Scanner(newnotes, NotesRetriever.utf8.name());
//		a.useDelimiter("\n");
//		Consumer<String> f = (z) -> {
//			String[] t = z.split("\t");
//			if (t[1].equals(area)) x.add(new AZ(t[0], t[1], t[2], t[3]));
//		};
//		a.forEachRemaining(f);
//		a.close();
//		return x;
//	}
}
