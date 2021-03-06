package com.pinternals.ir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.net.UnknownServiceException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import com.sap.JaxbNote;
import com.sap.Snotes;


public class NotesRetriever {
//	private static Logger log = Logger.getLogger(NotesRetriever.class.toString());
	static final Charset utf8 = Charset.forName("UTF-8");
	public static final String version = "v0.0.3";

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
				+ "SUPP.GETNOTES.CMD            makes query script\n"
				+ "SUPP.CACHE                   handle into db\n"
				+ "SUPP.GETNOTES [dtfrom dtto]  {online} request for notes list, store zip\n"
				+ "\n"
				+ "Launchpad.support.sap.com commands are:\n" 
//				+ "LPAD.SYNC                    mutual sync <area>.db with notes.db\n"
				+ "LPAD.CACHE [nick] ...        UNSUPPORTED since v0.0.2: import Zip4SSet\n"
				+ "LPAD.IMPORT [nick] ...       import from <nick>/facets into nick.db\n"
//				+ "LPAD.DEEP                    {online} ask for notes content\n"
//				+ "LPAD.Z1 <dba>                {test} z1-test given dba\n"
//				+ "LPAD.Z2 <dba>                {online,test} z2-test given dba\n"
//				+ "LPAD.Z3 <dba>                {test} z3-test given dba\n"
				+ "LPAD.GETNOTES [nick] ...     {online} ask unknown application areas\n"
				+ "\n"
				+ "support.sap.com/swdc commands are:\n" 
				+ "SWDC.TEST                    {online}test log on to SWDC\n"
				+ "\n"
				+ "NWA file1 ... fileN          test configuration\n"
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
			HttpHost prHost = null;
			Credentials prCred = null;
			NotesDB db = null;
			int rc = 0, i, j;
			Launchpad l = null;
			Support sup = null;
			Swdc swdc = null;
			List<Integer> vedro = new ArrayList<Integer>();
			boolean b = false;
			BufferedWriter bw;
			List<AZ> azs = null;

			if (cm.equals("SUPP.GETNOTES.CMD")) {
				new Support(null).zZz(cache, null, null, true);
				return;
			} else if (cm.equals("LPAD.DL")) {
				l = new Launchpad(cache, uname, prHost, prCred);
				Path z = Cache.fs.getPath(args.get(1));
				final List<AZ> azs2 = new ArrayList<AZ>();
				Consumer<String> ac = (x) -> {
					String t[] = x.split("\\t");
					int k = Integer.parseInt(t[0]);
					char lang = t[1].charAt(0);
					int m = Integer.parseInt(t[2]);
					vedro.add(k);
					azs2.add(new AZ(k, m, lang));
				};
				Files.lines(z).forEach(ac); 
				while (vedro.size()>0) {
					l.dlNotes(azs2, vedro, 10000000);
				}
				return;
			}
			// Offline commands: database and cache are required
			db = new NotesDB(cache.notesdb, false, true, false);
			db.initArea();
			Area.reload();
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
				case "LPAD.IMPORT":
					l = new Launchpad(cache);
					l.areaList(db);
					for (NotesDB dba: l.dbas) {
						if (as==1 || args.contains(dba.getNick()) )
							dba.importFacets(db, dba.pathdb.resolve("../facets").normalize());
					}
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
				case "SWDC.TEST":
					System.out.println("Online support.sap.com/swdc for " + uname);
					WebClient wc = null;
					i = 10;
					while (i-->0 && wc==null) {
						try {
							wc = Launchpad.getLaunchpad(uname, prHost, prCred);
						} catch (UnknownServiceException se) {
					    	System.err.println("Unexpected answer: " + se.getMessage());
						}
					}
					if (wc==null) throw new RuntimeException("Cannot log on to LPAD");
					swdc = new Swdc(cache, wc); //uname, prHost, prCred);
					swdc.test();
					b = false;
					break;
				case "HREN":
					l = new Launchpad(cache);
					l.areaList(db);
					List<String> areas = new ArrayList<String>();
					List<Object[]> ns = new ArrayList<Object[]>();
					for (NotesDB dba: l.dbas) {
						areas.addAll(Area.nickToArea.get((dba.getNick())) );
					}
					Iterator<Path> it = cache.getSupportSapComNotesZip();
//					Map<String,String> cached = db.w44();  		

					while (it.hasNext()) {
						Path q = it.next();
						String x = q.getFileName().toString().replace(".zip", ".xml");
						ZipInputStream zis = new ZipInputStream(Files.newInputStream(q), utf8);
						ZipEntry ze = zis.getNextEntry();
						while(!ze.getName().equals(x)) {
							ze = zis.getNextEntry();
						}
						if (ze.getName().equals(x)) {
							Snotes sn = JAXB.unmarshal(zis, Snotes.class);
//							System.out.println(x + "\t" + sn.getNS().size() + "\t" + ze.getLastModifiedTime().toInstant().toString());
							for (JaxbNote n: sn.getNS()) if (areas.contains(n.getA())) 
								ns.add(new Object[]{n.getA(), n.getN(), n.getL()});
						}
						zis.close();
					}
					for (NotesDB dba: l.dbas) {
						areas.clear();
						areas.addAll( Area.nickToArea.get(dba.getNick()) );
						List<AZ> ozs = dba.getNotesDBA(db.cat, db.prio);
						
						for (Object[] zz: ns) if (areas.contains(zz[0])) {
							int num = (int)zz[1];
							String lang = (String)zz[2];
							for (AZ y: ozs) if (y.num==num) {
								assert (lang.equals(y.mprop.getLanguage()));
							}
//							System.out.println(String.format("%d\t%s\t%s\t%s", n.getN(), n.getA(), n.getL(), n.getD()) );
						}
//						if (as==1 || args.contains(dba.getNick()) )
//							dba.importFacets(db, dba.pathdb.resolve("../facets").normalize());
					}
					break;
				case "NWA":
					l = new Launchpad(cache);
					l.areaList(db);
					NWA n = new NWA(args.get(1));
					for (String s: args.subList(2, as) ) {
						Path p = FileSystems.getDefault().getPath(s);
						n.addSysInfo(p);
					}
					for (NotesDB dba: l.dbas) n.check(db, dba);
					break;
				case "SLD":
					for (String s: args.subList(1, as)) {
						List<java.util.Properties> prop = NWA.parseSLD(FileSystems.getDefault().getPath(s));
						db.putSWCV(prop);
					}
					break;
				case "Report":
//					assert Area.areaToCode.get("SBO")!=null : Area.areaToCode;
					l = new Launchpad(cache);
					db.stat1upd(l.getFS());
					//&& x.num>1600000 
					azs = db.stat1get((x)->x.dled==0 && !Area.codeToArea.get(x.area).startsWith("SBO"));
//					azs.sort(Comparator.comparing(z1->z1.num));
					i = 1;
					bw = Files.newBufferedWriter(FileSystems.getDefault().getPath(String.format("task%d", i)) );
					j = 10000;
					for (AZ az: azs) {
						bw.write(String.format("%d\t%s\t%d\t%s%n", az.num, az.lang, az.mark, Area.codeToArea.get(az.area)));
						if (--j==0) {
							bw.close();
							i++;
							bw = Files.newBufferedWriter(FileSystems.getDefault().getPath(String.format("task%d", i)) );
							j = 10000;
						}
					}
					bw.close();
//					l.checkFS(azs, vedro);
//					db.stat1(l.dbas, true);
					break;
				case "Z1": case "Z2": case "Z3": case "Z4": case "Z5":
					l = new Launchpad(cache, uname, prHost, prCred);
					l.areaList(db);
//					Predicate<AZ> zbc = (x) -> !x.area.startsWith("SBO") && x.num>2000000;
//					vedro = db.stat1(l.dbas, true, zbc);
//					System.out.println(vedro.size());
					azs = db.getNotesCDB_byNums(vedro);
					db.close();
					db = null;
//					if ("Z2".equals(cm))
//						
//					else if ("Z3".equals(cm))
//						azs.sort(Comparator.comparing(z1->-z1.num));
//					else if ("Z4".equals(cm))
//						azs.sort(Comparator.comparing(z1->z1.area));
//					else if ("Z5".equals(cm))
//						azs.sort(Comparator.comparing(z1->z1.area.length() ));
//					while (vedro.size()>0) {
//						l.checkFS(azs, vedro);
//						l.dlNotes(azs, vedro);
//					}
//					for (AZ az: azs) System.out.println(String.format("%d %s", az.num, az.area));
//					
					break;
//				case "Z2":
//					l = new Launchpad(cache, uname, prHost, prCred);
//					l.areaList(db);
//					vedro = db.stat1(l.dbas, false);
//					while ( l.dlNotes(db, vedro, 100, cmp) ) {}
//					break;
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