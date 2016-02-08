package com.pinternals.ir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXB;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.http.HttpHost;
import org.apache.http.auth.Credentials;

import com.gargoylesoftware.htmlunit.WebClient;


public class NotesRetriever {
//	private static Logger log = Logger.getLogger(NotesRetriever.class.toString());
	public static final Charset utf8 = Charset.forName("UTF-8");
	public static final String version = "v0.0.2";

	public static void main(String[] argv) throws Exception {
		Options opts = new Options();
		opts.addOption("v", "version");

		opts.addOption("h", false, "usage help");		
		Option o = new Option("u", "user", true, "S-user");
		o.setRequired(false);
		opts.addOption(o);

		o = new Option("c", "cache", true, "cache directory");
		o.setRequired(false);
		opts.addOption(o);

		o = new Option(null, "proxy", true, "proxy");
		o.setRequired(false);
		opts.addOption(o);

		o = new Option("d", "debug", false, "debug mode");
		o.setRequired(false);
		opts.addOption(o);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = parser.parse(opts, argv);

		if (cmd.hasOption('v')) {
			System.out.println(version);
		} else if (cmd.hasOption('h')) {
			formatter.printHelp("java [-ea] -jar pinternals_ir.jar [options] [command]", 
				"Options are:\n"
				+ " -ea                enable asserts (JVM-specific option)", opts, 
				"\n"
				+ "Common commands are:\n"
				+ "STOREPASSWD                  make <uname>.pwd file with stored password\n"
				+ "INITDB                       create empty database\n"
				+ "\nSupport.sap.com commands are:\n"
				+ "SUPP.GETNOTES [dtfrom dtto]  {online} request for notes list, store zip\n"
				+ "SUPP.GETNOTES.CMD            makes query script\n"
				+ "SUPP.CACHE                   handle into db\n"
				+ "\n"
				+ "Launchpad.support.sap.com commands are:\n" 
//				+ "LPAD.SYNC                    mutual sync <area>.db with notes.db\n"
				+ "LPAD.GETNOTES [nick] ...     {online} ask unknown application areas\n"
				+ "LPAD.IMPORT [nick] ...       import from <nick>/facets into nick.db\n"
//				+ "LPAD.DEEP                    {online} ask for notes content\n"
//				+ "LPAD.Z1 <dba>                {test} z1-test given dba\n"
//				+ "LPAD.Z2 <dba>                {online,test} z2-test given dba\n"
//				+ "LPAD.Z3 <dba>                {test} z3-test given dba\n"
				+ "\n"
				+ "support.sap.com/swdc commands are:\n" 
				+ "SWDC.TEST                    test log on to SWDC\n"
				+ "\n"
				+ ""
				);
			return;
		} else {
			List<String> args = cmd.getArgList();
			assert args!=null;
			int as = args.size();
			if (as==0) {
				System.out.println("Usage help: java -jar pinternals_ir.jar -h");
				return;
			}
			String cm = args.get(0);
			if (cm.equals("INITDB")) {
				// создаёт локальную БД (не в кеше!)
				Path d = FileSystems.getDefault().getPath("notes.db");
				if (Files.exists(d))
					System.err.println(d + " is already exists, cannot overwrite");
				else
					new NotesDB(d, true, true, false);
				return;
			}
			String uname = null;
			if (cmd.hasOption('u')) uname = cmd.getOptionValue('u');
			if (cm.equals("STOREPASSWD")) {
				if (uname==null) {
					System.err.println("Error: user is not given. Use '-u uname'");
					return;
				} else {
				System.out.print(String.format("Enter the password for %s: ", uname));
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in, utf8));
					cm = br.readLine();
					br.close();
					Utils.putPasswd(uname, cm);
					return;
				}
			}
			Cache cache = new Cache(cmd.getOptionValue('c'));
			if (cm.equals("SUPP.GETNOTES.CMD")) {
				new Support(null).zZz(cache, null, null, true);
				return;
			}
			// Offline commands: database and cache are required
			HttpHost prHost = null;
			Credentials prCred = null;
			NotesDB db = null;
			db = new NotesDB(cache.notesdb, false, true, false);
			int rc = 0;
			Launchpad l = null;
			Support sup = null;
			Swdc swdc = null;
			boolean b = false;
			try {
				switch (cm) {
//				case "LPAD.SYNC":
//					throw new RuntimeException("Deprecated");
//					l = new Launchpad(cache, null, null, null);
//					l.areaList(db);
//					for (NotesDB dba: l.dbas) db.sync(dba);
//					break;
				case "SUPP.CACHE":
					Support.cache(cache, db);
					break;
				case "LPAD.CACHE":
					l = new Launchpad(cache);
					l.areaList(db);
					for (NotesDB dba: l.dbas) 
						l.cacheTmpdir(dba, true);
					break;
				default:
					if (uname==null) {
						System.err.println("Error: user is not given. Use '-u uname'");
						break;
					} 
					if (cmd.hasOption("proxy")) {
						prHost = Utils.createProxyHost(cmd.getOptionValue("proxy"));
						prCred = Utils.createProxyCred(cmd.getOptionValue("proxy"));
					}
					b = true;
				}
				if (b) switch (cm) {
				case "SUPP.GETNOTES":
					System.out.println("Online support.sap.com for " + uname);
					String dtfrom = null, dtto = null;
					if (as>2) {
						dtfrom = args.get(1);
						dtto = args.get(2);
					}
					sup = new Support(Utils.makeHttpClient(uname, prHost, prCred));
					sup.zZz(cache, dtfrom, dtto, false);
					Support.cache(cache, db);
					b = false;
					break;
				case "LPAD.GETNOTES":
					System.out.println("Online launchpad.support.sap.com for " + uname);
					l = new Launchpad(cache, uname, prHost, prCred);
					l.areaList(db);
					for (NotesDB dba: l.dbas) {
						String nick = dba.getNick();
						Path facets = dba.pathdb.resolve("../facets").normalize();
						if (as==1 || args.contains(nick)) {
							while ( l.getNotes(db, dba, facets) )
								System.out.println(nick + " passed");
							args.remove(nick);
						} else {
							System.err.println(String.format("warn: %s not used", nick));
						}
					}
					for (String x: args.subList(1, args.size())) System.out.println(String.format("warn: %s not found", x));
					b = false;
					break;
				case "LPAD.IMPORT":
					l = new Launchpad(cache);
					l.areaList(db);
					Pattern xp = Pattern.compile("(\\d+)_(.)_(.+)\\.xml");
					for (NotesDB dba: l.dbas) {
						StringJoiner sj = new StringJoiner("\n");
						String nick = dba.getNick();
						Path facets = dba.pathdb.resolve("../facets").normalize(), p;
						int i = 0;
						if (as==1 || args.contains(nick)) {
							Iterator<Path> it = Files.newDirectoryStream(facets, "000*.xml").iterator();
							while (it.hasNext()) {
								p = it.next();
								Matcher m = xp.matcher(p.getFileName().toString());
								assert m.matches() && m.groupCount()==3 : m;
								com.sap.lpad.Entry en = JAXB.unmarshal(Files.newInputStream(p), com.sap.lpad.Entry.class);
								com.sap.lpad.Properties pr = en.getContent().getProperties();
								int n2 = Integer.parseInt(m.group(1)), n3 = Integer.parseInt(pr.getSapNotesNumber()), v3 = Integer.parseInt(pr.getVersion());
								assert n2==n3 : "Note number error: " + n2 + "/" + n3 + "/" + p.getFileName().toString();
								String l2 = m.group(2), l22 = m.group(3);
								if (!pr.getLanguage().equals(l2) || !pr.getVersion().equals(l22)) {
									sj.add(String.format("ren %s %s", p.getFileName().toString(), String.format("%010d_%s_%d.xml", n2, pr.getLanguage(), v3)));
								} else {
									dba.putA01(en);
									dba.putA02(n3, pr.getLanguage(), v3, Files.getLastModifiedTime(p).toInstant(), null, null, null, null);
									if (++i>999) {
										System.out.println(i + " commited");
										dba.commit();
										i=0;
									}
								}
							}
							System.out.println(i + " commited");
							dba.commit();
							if (sj.length()>0) {
								BufferedWriter bw = Files.newBufferedWriter(facets.resolve("renamer.bat"), utf8); 
								bw.write(sj.toString());
								bw.close();
							}
						}
					}
					break;
				case "SWDC.TEST":
					System.out.println("Online support.sap.com/swdc for " + uname);
					WebClient cl = Launchpad.getLaunchpad(uname, prHost, prCred);
					swdc = new Swdc(cache, cl); //uname, prHost, prCred);
					swdc.test();
					b = false;
					break;
				case "":
					break;

				default:
					System.err.println(String.format("Unknown command: %s%nTry -h for help", cm));
					rc = -1;
					b = true;
				}
			} catch (SQLException e) {
				throw e;
			} finally {
				if (l!=null) l.close();
				if (swdc!=null) swdc.close();
				if (sup!=null) sup.close();
				if (db!=null) db.close();
			}
			System.exit(rc);
		}
	}
}
//
//					Map<Path,NotesDB> coll = l.importAreasDb(db, false);
//					for (NotesDB dba: l.pathToDbas.values()) assert !dba.isClosed();
//					l.deepAreas(db, coll);
//					for (Path q: l.pathToDbas.keySet()) {
//						NotesDB dba = l.pathToDbas.get(q);
////						assert !dba.isClosed();
//						System.out.println(String.format("Path %s", q.toString()));
//						
//						List<AZ> azl = dba.getZ3b(), azu;
//						Path tmd = Cache.fs.getPath(q.toString(), "tmp");
//						while (azl.size()>0) {
//							System.out.println("To check yet: " + azl.size());
//							azu = l.deepAreaTest2(azl, tmd, 3);
//							azl = azu;
//						}
						// TODO code for 111111111111111111
//					}
//				} else if (cm.equals("LPAD.Z1")) {
//					NotesDB dba = NotesDB.openDBA(Cache.fs.getPath(args.get(1)));
////					l.deepAreaTest(db, dba);
//				} else if (cm.equals("LPAD.Z2")) {
////					l.deepAreaTest2(db, Cache.fs.getPath(args.get(1)), true);
//				} else if (cm.equals("LPAD.Z3")) {
////					l.deepAreaTest2(db, Cache.fs.getPath(args.get(1)), false);