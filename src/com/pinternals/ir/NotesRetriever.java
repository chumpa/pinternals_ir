package com.pinternals.ir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.http.HttpHost;
import org.apache.http.auth.Credentials;

// az
class AZ{
	boolean badLang = false;
	int num;
	com.sap.Properties prop;
	public String toString() {
		return ""+num;
	}
	String askdate = null;
	String area, areadescr, dateS, lang, objid=null, objid2=null;
	boolean foundA = false;
	void foundA(String askdate, com.sap.Properties p) {
		prop = p;
		foundA = true;
		this.askdate = askdate;
	}
	AZ(int num, String area, String obj) {
		this.num = num;
		this.objid = obj;
		this.area = area;
	}
	
	String title, type, priority, category, newVers, newReleasedOn;
	class Version {
		int ver;
		String releasedon, isinternal;
		Version(int a, String b) {
			ver = a;
			releasedon = b;
		}
	}
	List<Version> vers = new LinkedList<Version>();
	void addVersion(int ver, String releasedon) {
		Version v = new Version(ver, releasedon);
		vers.add(v);
	}
}


public class NotesRetriever {
	static Logger log = Logger.getLogger(NotesRetriever.class.toString());
	public static Charset utf8 = Charset.forName("UTF-8");

//	static {
//		assert simpleSubstring("123456", "1", "6", 0).equals("2345") : simpleSubstring("123456", "1", "6", 0);
//		assert simpleSubstring("123456", "1", "", 0).equals("23456") : simpleSubstring("123456", "1", "", 0);
//	}


//	static URL getNoteURL(String number, String mode) throws MalformedURLException {
//		// что-то вроде https://service.sap.com/sap/bc/bsp/sno/ui_entry/entry.htm?param=69765F6D6F64653D3030312669765F7361706E6F7465735F6E756D6265723D3232353338343926
//		String c = "https://service.sap.com/sap/bc/bsp/sno/ui_entry/entry.htm";
//		String p = "?param=";
//		String v = "iv_mode=" + mode + "&iv_sapnotes_number=" + number;
//		String w =  DatatypeConverter.printHexBinary(v.getBytes());
//		URL u = new URL(c+p+w);
//		return u;
//	}
//	static URL getNotePdfURL() {
//		// https://websmp130.sap-ag.de/sap/support/notes/convert2pdf/0002253849?sap-language=EN
//		return null;
//	}

//	static URL getNoteStatistic(String key) throws MalformedURLException {
//		// key is like 012006153200001854902015
//		String c = "https://service.sap.com/sap/support/notes/statistic/reads.htm?iv_key=" + key;
//		return new URL(c);
//	}

//	static String[] attributesHtml = {"Application Area", "Number", "Short text", "Released On", "Category", "Priority"};
//	static int[] attrNums = new int[attributesHtml.length];
//	static boolean batch = false, storeHtml = false;
	
//	[0]	 	[1]	 	[2]	Application Area	[3]	Number	[4]	Short text	[5]	Released On	[6]	Category	[7]	Priority	
//	[0]	 	[1]	1.	[2]	BC-JAS	[3]	1906728 *	[4]	How to customize favicon.ico image for SAP NetWeaver 7.0X version	[5]	09.09.2014	[6]	How To	[7]	Normal	


	
//	private boolean noteverDeep(Cache cache, CloseableHttpClient cl, HttpClientContext htx
//			, String lang, int number, String objid, List<Integer> vers) 
//					throws ClientProtocolException, IOException, URISyntaxException {
//    	assert vers.size()>0;
//    	boolean dirty = false;
//    	CloseableHttpResponse rsp;
//		int sz = vers.size(),  max = vers.get(sz-1);
//
//		Path latestPrint = cache.getPathByScheme(EScheme.NotePrintVersion, number, lang, max);
//		if (!Files.isRegularFile(latestPrint)) {
//	    	rsp = cl.execute(new HttpGet(getUrl(htx, EScheme.NotePrintVersion, lang, number)));
//	    	entityToFile(rsp.getEntity(), latestPrint, cp1252, utf8);
//	    	dirty = true;
//		}
//
//		Path latestFancy = cache.getPathByScheme(EScheme.NoteLastFancyVersion, number, lang, max);
//		if (!Files.isRegularFile(latestFancy)) {
//	    	rsp = cl.execute(new HttpGet(getUrl(htx, EScheme.NoteLastFancyVersion, lang, number)));
//	    	entityToFile(rsp.getEntity(), latestFancy, cp1252, utf8);
//	    	dirty = true;
//		}
//
//    	if (sz>1) for (Integer ver: vers.subList(0, sz-1)) {
//    		System.out.println("todo look for version " + ver + "_" + lang);
//    	}
//    	return dirty;
//	}
	

	public static void main(String[] argv) throws Exception {
		Options opts = new Options();
		opts.addOption("h", false, "usage help");		
		Option o = new Option("u", "user", true, "S-user");
		o.setRequired(false);
		opts.addOption(o);
		
		o = new Option("l", "lang", true, "language");
		o.setRequired(false);
		opts.addOption(o);

		o = new Option("c", "cache", true, "cache directory");
		o.setRequired(false);
		opts.addOption(o);
		
		o = new Option(null, "proxy", true, "proxy");
		o.setRequired(false);
		opts.addOption(o);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = parser.parse(opts, argv);

		if (cmd.hasOption('h')) {
			formatter.printHelp("java [-ea] -jar pinternals_ir.jar [options] [command]", 
					"Options are:\n-ea   for enable asserts", opts, 
					"\nCommands are:\n" 
					+ "STOREPASSWD                  make <uname>.pwd file with stored password\n"
					+ "INITDB                       create empty database\n"
					+ "SUPP.GETNOTES [dtfrom dtto]  request for support.sap.com/notes and store notes*zip\n"
					+ "SUPP.GETNOTES.CMD            close to GETSUPNOTES but makes script\n"
					+ "SUPP.CACHE                   cache support-sap-com-notes/notes*zip into db\n"
					+ "LPAD.AREAS                   ask launchpad for unknown application areas\n"
					+ "LPAD                       \n"
					+ "LPAD.2                      \n"
					);
			return;
		} else {
			List<String> args = cmd.getArgList();
			int as = args.size();
			if (args==null || as==0) {
				System.out.println("Usage help: java -jar pinternals_ir.jar -h");
				return;
			} 
			String cm = args.get(0);
			if (cm.equals("STOREPASSWD")) {
				System.out.print("Enter the password: ");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				cm = br.readLine();
				br.close();
				Utils.putPasswd(cmd.getOptionValue('u'), cm);
				return;
			}
			if (cm.equals("INITDB")) {
				// создаёт локальную БД (не в кеше!)
				Path d = FileSystems.getDefault().getPath("notes.db");
				if (Files.exists(d))
					System.err.println(d + " is already exists, cannot overwrite");
				else
					NotesDB.initDB(d);
				return;
			}
			Cache cache = new Cache(cmd.getOptionValue('c'));

			HttpHost prHost = null;
			Credentials prCred = null;
			if (cmd.hasOption("proxy")) {
				prHost = Utils.createProxyHost(cmd.getOptionValue("proxy"));
				prCred = Utils.createProxyCred(cmd.getOptionValue("proxy"));
			}

			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
			LocalDate b = LocalDate.parse("19900101", dtf), t, tm = LocalDate.now();
			Iterator<Path> it = cache.getSupportSapComNotesZip();
			if (cm.equals("SUPP.GETNOTES.CMD")) {
				while (it.hasNext()) {
					Path q = it.next();
					LocalDate from =  LocalDate.parse(q.getFileName().toString().substring(6,14), dtf);
					LocalDate to =  LocalDate.parse(q.getFileName().toString().substring(16,24), dtf);
					while (!b.isEqual(from) && b.isBefore(from)) {
						t = b.plusMonths(1);
						System.out.println(String.format("call test.bat GETLIST %s %s", dtf.format(b), dtf.format(t)));
						b = t;
					}
					b = to;
				}
				while (b.isBefore(tm)) {
					t = b.plusMonths(1);
					System.out.println(String.format("call test.bat GETLIST %s %s", dtf.format(b), dtf.format(t)));
					b = t;
				}
				return;
			}
			if (!cmd.hasOption('u')) {
				System.err.println("Error: user is not given. Use '-u uname'");
				return;
			} 
			String uname = cmd.getOptionValue('u');
			NotesDB db = new NotesDB(cache.notesdb, true);
			if (cm.equals("SUPP.GETNOTES")) {
				System.out.println("NotesRetriever for " + uname);
				String dtfrom = null, dtto = null;
				if (as>2) {
					dtfrom = args.get(1);
					dtto = args.get(2);
				}
				Support sup = new Support(Utils.makeHttpClient(uname, prHost, prCred));
				sup.zZz(cache, dtfrom, dtto);
				Support.cache(cache, db);
				return;
			} else if (cm.equals("SUPP.CACHE")) {
				Support.cache(cache, db);
				return;
			} 
			Launchpad l = new Launchpad(cache, uname, prHost, prCred);
			if (cm.equals("LPAD.AREAS")) {
				for (String p: args.subList(1, as)) l.a2(db, p);
			} else if (cm.equals("LPAD")) {
				l.z2(db);
			} else if (cm.equals("LPAD2")) {
				l.z2(db);
				l.z3(db);
			} else {
				System.err.println(String.format("Unknown command: %s\nTry -h for help", cm));
			}
			if (l!=null) l.close();
		}
	}
	
}
