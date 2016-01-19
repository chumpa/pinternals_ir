package com.pinternals.ir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
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
	static SimpleDateFormat xmld = new SimpleDateFormat("yyyy-MM-dd");
	boolean need() throws ParseException {
		boolean b = true;
		int i = 0;
		Date gd = xmld.parse(dateS), vd;
//		System.out.println("Date:" + date);
		for (Version v: vers) {
			String r = v.releasedon.substring(0, 10);
			vd = xmld.parse(r);
			i = vd.compareTo(gd);
			if (i==0) b = false;
//			System.out.println("releasedon:" + v.releasedon + "|" + vd.compareTo(gd));
		}
		return b;
	}
	Properties a = new Properties();
	void putProperty(String n, String v) {
//		<d:>0000565196</d:SapNotesNumber>
//		<d:>012006153200001835992002</d:SapNotesKey>
//		<d:Title>XI: Zusätzliche Mappingtypen für Partner-Mappings</d:Title>
//		<d:Type>SAP Note</d:Type>
//		<d:Version>1</d:Version>
//		<d:Priority>Korrektur mit hoher Priorität</d:Priority>
//		<d:Category>Programmfehler</d:Category>
//		<d:ReleasedOn>2002-10-23T11:04:22</d:ReleasedOn>
//		<d:ComponentKey>BC-XI-IBC-MAP</d:ComponentKey>
//		<d:ComponentText>Mapping</d:ComponentText>
//		<d:Language>D</d:>
//		<d:LanguageText>
//		<d:Favorite>
		switch (n) {
		case "SapNotesNumber":
			if (Integer.valueOf(v)!=num) throw new RuntimeException("Number mismatch");
			break;
		case "SapNotesKey":
			objid2 = new String(v);
			break;
		case "Title":
			title = new String(v);
			break;
		case "Type":
			type = new String(v);
			break;
		case "Priority":
			priority = new String(v);
			break;
		case "Category":
			category = new String(v);
			break;
		case "ComponentKey":
			if (!v.equals(area)) throw new RuntimeException("Area mismatch");
			break;
		case "ComponentText":
			areadescr = new String(v);
			break;
		case "Language":
			if (!v.equals(lang)) badLang = true;
			break;
		case "Version":
			newVers = new String(v); 
			break;
		case "ReleasedOn":
			newReleasedOn = new String(v);
			break;
		}
	}
}

enum EScheme {
	CompareVersions, NotePrintVersion, NoteLastFancyVersion, NoteListForm, NoteStatistic,
	NoteWUL, NoteAttachment, NoteChangeLog, InternalMemo, SSCR, Pilot,
	CWB, Nit, DPA, PutDownloadBasket, NoteList,
	
	//util
	ZVersions, ZRoot, UserProfile
	;
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
					+ "STOREPASSWD                    make <uname>.pwd file with stored password\n"
					+ "INITDB                         init database\n"
					+ "GETLIST [dtfrom [dtto]]        ask SCN, make notes_yyyyMMyy--yyyyMMyy_[D,E].zip\n"
					+ "PARSELIST                      notes_yyyyMMyy--yyyyMMyy_[D,E].zip into xml\n"
					+ "TESTRAW <raw-dir>              test raw lists\n"
					+ "CACHE                          handle cache\n"
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
//			NotesRetriever nr = new NotesRetriever();

			HttpHost prHost = null;
			Credentials prCred = null;
			if (cmd.hasOption("proxy")) {
				prHost = Utils.createProxyHost(cmd.getOptionValue("proxy"));
				prCred = Utils.createProxyCred(cmd.getOptionValue("proxy"));
			}

			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
			LocalDate b = LocalDate.parse("19900101", dtf), t, tm = LocalDate.now();
			Iterator<Path> it = cache.a();
			if (cm.equals("GETLISTCMD")) {
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
			}
			if (!cmd.hasOption('u')) {
				System.err.println("Error: user is not given. Use '-u uname'");
				return;
			} 
			String uname = cmd.getOptionValue('u');
			NotesDB db = new NotesDB(cache.notesdb, true);
			if (cm.equals("GETLIST")) {
				System.out.println("NotesRetriever for " + uname);
				String dtfrom = null, dtto = null;
				if (as>2) {
					dtfrom = args.get(1);
					dtto = args.get(2);
				}
				Support sup = new Support(Utils.makeHttpClient(uname, prHost, prCred));
				sup.zZz(cache, dtfrom, dtto);
				Support.cache(cache, db);	
			} else if (cm.equals("CACHE")) {
				Support.cache(cache, db);
			} else if (cm.equals("Z2")) {
				Launchpad l = new Launchpad(cache.launchpad);
				l.z2(cache, db);
//				WebClient wc = Launchpad.getLaunchpad(uname, prHost, prCred);
			} else if (cm.equals("Z3")) {
				Launchpad l = new Launchpad(cache.launchpad);
				l.z2(cache, db);
				l.z3(cache, db, uname, prHost, prCred);
			}

			
			//			if (cm.equals("TESTRAW")) {
//				if (as==1) {
//					System.err.println("Path is required");
//					System.exit(-1);
//				}
//				Path qq = FileSystems.getDefault().getPath(args.get(1));
//				System.out.println("qq="+qq);
//				for (Pair<Character,Path> x: Cache.filterRawFiles(qq)) {
//					ParseIndexContext ctx = new ParseIndexContext(sdSUser, x.getKey());
//					Path raw = x.getValue();
//					nr.parseIndex(raw.toFile(), ctx);
//					String t = raw.getFileName().toString();
//					if (ctx.errors>0) {
//						System.err.println("Archive " + t + " has errors: " + ctx.errors);
//						for (String z: ctx.lerrors) System.err.println(z);
////						x.getValue().renameTo(new File(x.getValue().getAbsolutePath() + ".errors"));
//					} else {
//						System.out.println("Archive " + t + " OK (" + ctx.list.size() + " notes)");
//					}
//				}
//			} 

//			if (cm.equals("CACHE")) {
//				for (Pair<Character,Path> x: cache.getRawFiles(true)) {
//					ParseIndexContext ctx = new ParseIndexContext(zip);// sdSUser, x.getKey());
//					Path raw = x.getValue();
//					Path xml = cache.getPathXml(raw);
//					System.out.println(raw + "\t-> " + xml);
//					Support.parseIndex(raw.toFile(), ctx);
//					cache.storeXml(ctx, xml);
//				}
//				// читаем файлы по порядку и строим отчёт
//				DirectoryStream<Path> xmls = Files.newDirectoryStream(cache.xmldir, "notes_*--*.xml");
//				db.walk(xmls.iterator(), false, cache.newnotes);
//				return;
//			}

//			if (cm.equals("GETAREA")) {
//				assert as>1;
//				WebClient wc = null;
//				for (String area: args.subList(1, as)) {
//					System.out.print(area); 
//					List<AZ> q = cache.getZ2(area);
//					System.out.print("\t" + q.size());
//					List<AZ> r = db.getZ2(q, area);
//					System.out.print("\t" + r + "\n");
//					if (wc==null) wc = Launchpad.getLaunchpad(uname, prHost, prCred);
//					NotesRetriever.getZ2(cache, db, wc, r);
//				}
//				if (wc!=null) wc.close();
//			} else  {
//				System.err.println("unknown command: " + cm);
//				System.exit(-1);
//			}
		}
	}
	
//	static List<Path> getZ2(Cache cache, NotesDB db, WebClient wc, List<AZ> r) throws FailingHttpStatusCodeException, IOException, SQLException {
//		List<Path> x = new ArrayList<Path>(r.size());
//		for (AZ az: r) {
//	    	URL u = Launchpad.getByScheme(az.lang, az.num, 0);
//	    	XmlPage xm = wc.getPage(u);
//
//	    	String s = String.format("%010d_root_%s.xml", az.num, az.lang);
//	    	Path q = Cache.fs.getPath(cache.newdir.toString(), s);
//	    	BufferedWriter pw = Files.newBufferedWriter(q);
//	    	pw.write(xm.getWebResponse().getContentAsString());
//	    	pw.close();
//	    	
//	    	assert xm instanceof XmlPage;
//	    	NodeList o = xm.getChildNodes();
//	    	Node n = o.item(0);
//	    	assert n.getLocalName().equals("entry");
//	    	o = n.getChildNodes();
//	    	n = null;
//	    	if (o!=null) for (int i=0; i<o.getLength(); i++) {
//	    		n = o.item(i);
//	    		if (n.getNodeType()==Node.ELEMENT_NODE && n.getLocalName().equals("content")) break;
//	    	}
//	    	assert n!=null;
//	    	o = n.getChildNodes();
//	    	assert o!=null && o.getLength()==1;
//	    	n = o.item(0);
//	    	assert n.getLocalName().equals("properties");
//	    	o = n.getChildNodes();
//	    	if (o!=null) for (int i=0; i<o.getLength(); i++) {
//	    		n = o.item(i);
//	    		assert n.getNodeType()==Node.ELEMENT_NODE;
//	    		az.putProperty(n.getLocalName(), n.getTextContent());
//	    	}
//	    	if (az.badLang) {
//	    		System.err.println("Bad language:" + az.num + az.lang);
//	    		db.setMissedTitle(az.num, az.lang);
//	    	} else {
//	    		db.updateVer(az);
//	    	}
////	    	break;
//		}
//		return x;
//	}
}

//List<Integer> unknown = db.getUnknownObjid("%", 10000);
//List<String> objs = new ArrayList<String>(unknown.size());
//if (unknown.size()>0) {
//	for (int number: unknown) {
//		String x = nr.getObjid(null, null, cache, number, false);
//		objs.add(x);
//		if (x!=null) System.out.print(number + ",");
//	}
//	int q = db.updateObjid(unknown, objs);
//	System.out.println("Commited new objid: " + q);
//}
//// смотрим, версии каких нот читались последними
//List<Pair<Integer, Path>> vers = cache.getRawVers(true);
//for (Pair<Integer, Path> p: vers) {
//	System.out.println(p.getKey().toString());
//	db.updateVer(p.getKey(), p.getValue());
//}
//if (vers.size()>0) Files.move(cache.dirtyver, cache.dirtybak); 
//} else if (cm.equals("SW")) {
//if (!cmd.hasOption('c')) {
//	System.err.println("Path to cache is required");
//	System.exit(-1);
//}
//String t = as>1 ? args.get(1) : "%";
//PrintWriter pw = new PrintWriter(Files.newOutputStream(cache.dirtyver));
//db.skywalk(t, pw);
//pw.close();
//} else if (cm.equals("NV")) {
//// notes-version
//if (!cmd.hasOption('c')) {
//	System.err.println("Path to cache is required");
//	System.exit(-1);
//}
//nr.aa(cmd);
//CloseableHttpClient cl = Utils.makeHttpClient(nr.uname, prHost, prCred);
//System.out.println("NotesRetriever for " + nr.uname);
//HttpClientContext htx = nr.probeUrl(cl, "https://service.sap.com", "");
//Path y = as<2 ? cache.dirtyver : Cache.fs.getPath(args.get(1));
//Scanner a = new Scanner(y);
//while (a.hasNextLine()) {
//	String z = a.nextLine();
//	System.out.println(z);
//	String x[] = z.split("\t");
//	int number = Integer.valueOf(x[0]);
//	String objid = new String(x[1]);
//	nr.notever(cl, htx, cache, number, objid, true, true);
//}
//a.close();
//} else if (cm.equals("OBJID")) {
//// notes-version
//if (!cmd.hasOption('c')) {
//	System.err.println("Path to cache is required");
//	System.exit(-1);
//}
//nr.aa(cmd);
//List<Integer> unknown = db.getUnknownObjid("%", 3);
//db.close();
//unknown.sort(Comparator.comparing(x -> x % (Math.rint(10))));
//
//List<String> objs = new ArrayList<String>(unknown.size());
////System.out.println(unknown);
//if (unknown.size()>0) {
//	CloseableHttpClient cl = Utils.makeHttpClient(nr.uname, prHost, prCred);
//	System.out.println("NotesRetriever for " + nr.uname);
//	HttpClientContext htx = nr.probeUrl(cl, "https://service.sap.com", "");
//	for (int number: unknown) {
//		String x = nr.getObjid(cl, htx, cache, number, true);
//		objs.add(x);
//		if (x!=null) System.out.print(number + ",");
//	}
//	System.out.println("Commited: " + objs.size());
//}
//} else if (cm.equals("REP")) {
//db.report(args.get(1));
//db.close();
////db.skywalk2(args.get(1)+"%", new PrintWriter(System.out));
//


/*
https://websmp230.sap-ag.de/~form/handler?_APP=01100107900000000342&_EVENT=REDIR&_NNUM=2048282&_NLANG=

[0]	 	[1]	 	[2]	Application Area	[3]	Number	[4]	Short text	[5]	Released On	[6]	Category	[7]	Priority	
[0]	 	[1]	1.	[2]	BC-JAS	[3]	1906728 *	[4]	How to customize favicon.ico image for SAP NetWeaver 7.0X version	[5]	09.09.2014	[6]	How To	[7]	Normal	
[0]	unchecked	[1]	2.	[2]	BC-JAS	[3]	1878985	[4]	SAP NetWeaver AS Java 7.30 SP9 List of corrections	[5]	06.08.2013	[6]	Upgrade information	[7]	Recommendations / Additional Info	
*/


/*


//			SimpleDateFormat sdSUser = new SimpleDateFormat("dd.MM.yyyy");

*/