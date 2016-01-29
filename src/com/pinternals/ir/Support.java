package com.pinternals.ir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javafx.util.Pair;

import javax.xml.bind.JAXB;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import com.sap.Snotes;
import com.sun.org.apache.xml.internal.security.utils.Base64;

enum EScheme {
	CompareVersions, NotePrintVersion, NoteLastFancyVersion, NoteListForm, NoteStatistic,
	NoteWUL, NoteAttachment, NoteChangeLog, InternalMemo, SSCR, Pilot,
	CWB, Nit, DPA, PutDownloadBasket, NoteList, KBA, Corr, 
	
	//util
	ZVersions, ZRoot, UserProfile
	;
}

class NoteListItem {
	String objid = null; // like @value of <INPUT TYPE="checkbox" NAME="MARK" VALUE="012003146900001956932014">
	String apparea = null;
	int number, rownum;
	String mark = null;
	String href = null;
	String title = null;
	LocalDate date = null;
	String category = null;
	String priority = null;	//
	char asklangu = 0;	// language while searching
	NoteListItem(String x) {
		asklangu = x.charAt(0);
	}
	public String toString() {
		String s = (objid==null ? "" : objid) + "." + apparea + "." + number + "." + (mark==null ? "" : mark);
		return s + "." + category + "." + priority + "\t" + date + "\t" + title + "\t" + href + "\n";
	}
}

class ParseIndexContext {
	String lang;
	int errors;
	DateTimeFormatter dtf;
	List<NoteListItem> list = null;
	List<Pair<String,String>> lareas = null;
	List<Pair<String,String>> lapps = null;
	List<String> lerrors = null;
	ParseIndexContext(DateTimeFormatter dtf, String lang) {
		assert "DEJ".contains(lang) : "Language isn't DEJ";
		assert dtf!=null;
		this.dtf = dtf;
		this.lang = lang;
		errors = 0;
		list = new ArrayList<NoteListItem>(10000);
		lareas = new ArrayList<Pair<String,String>>(2000);
		lapps = new ArrayList<Pair<String,String>>(2000);
		lerrors = new ArrayList<String>();
	}
	void addRawError(String field, int diag, String where) {
		errors++;
		lerrors.add(field+",diag=" + diag + "\t" + where);
	}
	static String simpleSubstring(String field, String where, String prefix, String suffix, int diag, ParseIndexContext ctx) {
		boolean b = where.startsWith(prefix) && where.endsWith(suffix); 
		if (!b && ctx!=null) { 
			ctx.addRawError(field, diag, where);
			return null;
		}
		int l = where.length();
		return new String(where.substring(prefix.length(), l - suffix.length() ));
	}
}

public class Support {
	static final Charset utf8 = Charset.forName("UTF-8");
	static final Charset cp1252 = Charset.forName("cp1252");
	static final Charset shift_jis = Charset.forName("shift_jis");
	CloseableHttpClient cl = null;
	HttpClientContext htx = null;
	String sDF=null, sTZ=null;

	Support(CloseableHttpClient cl) {
		this.cl = cl;
	}

	HttpClientContext probeUrl() 
			throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, URISyntaxException {
		String uri = "https://service.sap.com";
		String appSearchNotes = "/~form/handler?_APP=00200682500000001952&_EVENT=CREATE_SEA";

        htx = HttpClientContext.create();
    	HttpGet g = new HttpGet(uri + appSearchNotes);
    	CloseableHttpResponse rsp = cl.execute(g, htx);
    	StatusLine l = rsp.getStatusLine();
    	assert l.getStatusCode() == 200 : l;
    	return htx;
	}

	static void parseList(int mx, Scanner a, ParseIndexContext ctx) throws IOException, ParseException {
		String p1 = "<TABLE BORDER=\"0\" WIDTH=\"100%\" CELLSPACING=\"0\" CELLPADDING=\"3\">";
		String p2 = "</TABLE>", std = "</SELECT></TD>", op = "<OPTION VALUE=\"", ope = "</OPTION>";
		int st = 0, ln = 0, sx = 0;
		String x, t;

		if ("TODO_parse_apps_areas".equals("")) {
			while(!a.nextLine().trim().equals("<SELECT NAME=\"FIELD_HEAD_THEMK\" SIZE=\"1\" CLASS=\"justify\">"));
			assert a.hasNextLine();
			while (a.hasNextLine()) {
				x = a.nextLine().trim();
				if (x.equals(std)) break;
				t = ParseIndexContext.simpleSubstring("apparea.code", x, op, ope, sx++, ctx); //t like R0000030650 EQT" title="FS-CM (29)">FS-CM (29)
				x = t.substring(0, t.indexOf(" "));	// code
				t = t.substring(t.lastIndexOf(">")+1); // area like FS-CM (29)
				t = t.substring(0, t.indexOf("(")).trim();
				Pair<String,String> p = new Pair<String,String>(x, t);
				if (!ctx.lareas.contains(p)) ctx.lareas.add(p);
			}
			assert a.hasNextLine();
			sx = 0;
			while(a.hasNextLine()) {
				x = a.nextLine().trim();
				if (x.equals("<SELECT NAME=\"FIELD_00200720420000000164\" SIZE=\"1\" CLASS=\"justify\">") ||
					x.equals("<SELECT NAME=\"FIELD_KEYWORDS\" SIZE=\"1\" CLASS=\"justify\">")) break;
			}
			assert a.hasNextLine();
			while (a.hasNextLine()) {
				x = a.nextLine().trim();
				if (x.equals(std)) break;
				t = ParseIndexContext.simpleSubstring("app.code", x, op + " ", ope, sx++, ctx); //t like ' 67837800100900006882                                                                                                                                                                                    EQ" title="SAP Adaptive Server Enterprise 15.7 (92)">SAP Adaptive Server Enterprise 15.7 (92)'
				x = t.substring(0, t.indexOf(" ")).trim();	// code
				t = t.substring(t.lastIndexOf(">")+1); // area like SAP Adaptive Server Enterprise 15.7 (92)
				t = t.substring(0, t.indexOf("(")); 
				ctx.lapps.add(new Pair<String,String>(x, t));
			}
			assert a.hasNextLine();
		}

		while (!a.nextLine().equals("<!------------------- Result ----------------------------->"));
		if (a.findWithinHorizon(p1, 0)!=null) {
			NoteListItem i = null;
			sx = 0;
			ln = 0;
			st = 0;
			while (a.hasNextLine() && sx<=mx) {
				x = a.nextLine().trim();
				if (x.contains(p2)) break;
				if (st==0 && x.equals("<TR>")) st = 1;
				else if (st==1 && x.equals("</TR>")) st = 2;
				else if (st==1) {
					// внутри заголовка
				} else if (st==2 && (x.equals("<tr class=\"lightrow\">") || x.equals("<tr class=\"darkrow\">"))) {
					assert i == null;
					st = 3;
					ln = 0;
					i = new NoteListItem(ctx.lang);
				} else if (st==3 && x.equals("</TR>")) {
					assert i!=null;
					if (i.number!=0) { 
						ctx.list.add(i);
						i = null;
						st = 2;
						sx++;
					}
//					else
//						System.err.println("parse for note '" + i.title + "' failed");
				} else if (st==3) {
					assert i!=null;
					String zz = "<INPUT TYPE=\"checkbox\" NAME=\"MARK\" VALUE=\"";
					String zt = "<TD CLASS=\"result-line\" ALIGN=\"right\"><STRONG>";
					String ze = ".</STRONG></TD>";
					String xt = "<TD CLASS=\"result-line\"><SMALL>", xv = "</SMALL></TD> <!--application area -->";
					String xz = "</SMALL></TD>", xq = "<TD CLASS=\"result-line\" ><SMALL>";
					String yt = xt, yv = "";
					String ae = "<TD CLASS=\"result-line\" ><A HREF=\"", af = "</A></TD>";
					String be = "<SPAN class=note>", bt = "</SPAN>";
					// внутри таблицы
					assert (ln>0 || (ln==0 && x.equals("<TD CLASS=\"result-line\" ALIGN=\"right\">"))) : x;
					if (ln==1 && !x.equals("&nbsp;")) {
						// sometimes instead of <INPUT there is ÿINPUT, so skipe 1st char
						// it's reason of retreat the download!
						i.objid = ParseIndexContext.simpleSubstring("markid", x, zz, "\">", sx, ctx);
					} else if (ln == 3) {
						i.rownum = Integer.valueOf(ParseIndexContext.simpleSubstring("rownum", x, zt, ze, sx, ctx));
						assert i.rownum > 0;
					} else if (ln == 4) {
						i.apparea = ParseIndexContext.simpleSubstring("apparea", x, xt, xv, sx, ctx);
					} else if (ln == 5) {
						i.number = 0;
						try {
							i.number = Integer.valueOf(ParseIndexContext.simpleSubstring("number", x, yt, yv, sx, ctx));
						} catch (NumberFormatException nfe) {
							System.err.println("Note number parsing error for row: " + i.rownum + "\n" + x + ", app area=" + i.apparea + "\n" + nfe);
						}
					} else if (ln == 6 && x.startsWith(be)) {
						i.mark = ParseIndexContext.simpleSubstring("mark", x, be, bt, sx, ctx);
						ln = ln - 1;
					} else if (ln == 7) {
						if (!x.endsWith(af)) {// sometimes the title is split
							x = x + a.nextLine();
						}
						t = ParseIndexContext.simpleSubstring("href/title", x, ae, af, sx, ctx);
						if (t!=null) {
							int q = t.indexOf('"'), q2 = t.lastIndexOf('>');
							i.href = new String(t.substring(0, q));
							i.title = StringEscapeUtils.unescapeHtml4(t.substring(q2+1));
						}
					} else if (ln == 8) { 
						t = ParseIndexContext.simpleSubstring("date", x, xt, xz, sx, ctx);
						i.date =  LocalDate.parse(t, ctx.dtf);
					} else if (ln == 9) {
						i.category = ParseIndexContext.simpleSubstring("category", x, xq, xz, sx, ctx);
					} else if (ln == 10) {
						i.priority = ParseIndexContext.simpleSubstring("priority", x, xq, xz, sx, ctx);
					}
					ln = ln + 1;
				}
			}
		} else {
			throw new RuntimeException("No notes at all");
		}
		return;
	}
	Pair<String,String> getParseUserProfile() throws URISyntaxException, ClientProtocolException, IOException {
		HttpGet g = new HttpGet(getUrl(htx, EScheme.UserProfile, null));
    	HttpEntity e = cl.execute(g).getEntity();

//    	for debug: entityToFile(e.getContent(), new FileOutputStream("userprofile.htm"), cp1252, utf8);

		Scanner a = new Scanner(e.getContent(), cp1252.name());
		String s = "";
		while(!s.endsWith("Date format</span>")) {s = a.nextLine().trim();};
		s = a.nextLine();
		s = a.nextLine().trim();
		s = ParseIndexContext.simpleSubstring("userprofile.dateformat", s, 
			"<select class=\"urDdlWhl1 urV\" id=\"ff_date_format\" ct=\"DropDownListBox\" name=\"ff_date_format\" style=\"width:200;\">", 
			"</select>", 0, null);
		if (s!=null && !"".equals(s)) 
			sDF = (s.split("selected>")[1]).split("<")[0];

		assert s!=null;
		while(!s.endsWith("Time zone</span>")) {s = a.nextLine().trim();};
		s = a.nextLine();
		s = a.nextLine().trim();
		s = ParseIndexContext.simpleSubstring("userprofile.tzformat", s, 
			"<span id=\"ff_time_zone-r\" class=\"urEdf2Whl\"><input type=\"Text\" class=\"urEdf2TxtEnbl urV\" autocomplete=\"off\" id=\"ff_time_zone\" ct=\"InputField\" name=\"ff_time_zone\" st=\"\" tp=\"STRING\" value=\"", 
			"</span>", 0, null);
		if (s!=null) 
			sTZ = s.substring(0, s.indexOf('"'));

		a.close();
		return new Pair<String,String>(sDF, sTZ);
	}

	void iter(Cache cache, int pagesize, int maxpages, String dtfrom, String dtto) 
			throws IOException, URISyntaxException, ParseException {
    	assert sDF!=null && !"".equals(sDF);
    	assert sTZ!=null && !"".equals(sTZ);
    	CloseableHttpResponse rsp;
    	HttpEntity e;
		List<String> ae = new ArrayList<String>(10), aj = new ArrayList<String>(10), ad = new ArrayList<String>(10);
		List<Pair<String,List<String>>> aq = new ArrayList<Pair<String,List<String>>>();
		aq.add(new Pair<String,List<String>>("E", ae));
		aq.add(new Pair<String,List<String>>("D", ad));
		aq.add(new Pair<String,List<String>>("J", aj));

		String pt = "_NNUM=&"
				+ "&00200682500000004914=%s"  
				+ "&00200682500000004876="
				+ "&00200682500000004915=AND"
				+ "&00200682500000004916=ALL"
				+ "&00200682500000005447=L"
				+ "&SEARCH_AREA="
				+ "&00200682500000005040=NO_RESTRICTION"
				+ "&00200682500000004918=%d"
				+ "&_APP=00200682500000001952&_EVENT=RESULT"
				+ "&00200682500000004875="
				+ "&00200682500000004932=%d"
				+ "&00200682500000004919=%d"
				+ "&00200682500000005464=FALSE"
				+ "&00200682500000005466=TRUE&00200682500000005465=TRUE&00200682500000005467=TRUE&00200682500000005468=TRUE"
				+ "&00200682500000005462=HEAD_FRGTS&00200682500000005463=0"
				+ "&00200682500000002804=&00200682500000002805=&00200682500000002076="
				+ "&00200682500000000719=OW2"
				+ "&00200682500000002070=&00200682500000004920="
				+ "&00200682500000004877=123456&00200682500000004878=0022"
				+ "&00200682500000004879=ABCDEFGHIJKLMNOPQRSTUVWXYZ"
				+ "&00200682500000005448=NO"
				+ "&00200682500000001280=USER"
				+ "&00200682500000001274=%s"	//dtto   YYYYMMDD
				+ "&00200682500000001276=%s"	//dtfrom YYYYMMDD
				+ "&01100107900000000030="
				+ "&00200682500000005063=NO_RES&TEMP_SEARCH=1&NAMESPACE_SEARCH=NO_SEA"
				+ "&DEFAULT_SEARCH=X";
		
		for (int q=1; q<=maxpages; q++) {
			ad.add(String.format(pt, "D", pagesize, q, (q-1)*pagesize, dtto, dtfrom));
			ae.add(String.format(pt, "E", pagesize, q, (q-1)*pagesize, dtto, dtfrom));
			aj.add(String.format(pt, "J", pagesize, q, (q-1)*pagesize, dtto, dtfrom));
		}

		Path dir = cache.getTmpDir();
		Path p2 = Cache.fs.getPath(dir.toString(), "00params.xml");
		Path xml = Cache.fs.getPath(dir.toString(), String.format("notes_%s--%s.xml", dtfrom, dtto));
		Path ar = Cache.fs.getPath(dir.toString(), String.format("notes_%s--%s.zip", dtfrom, dtto));

		BufferedWriter bw = Files.newBufferedWriter(p2, utf8);
		bw.write("DF=" + sDF + "\n");
		bw.write("TZ=" + sTZ + "\n");
		bw.write("BEGDA=" + dtfrom + "\n");
		bw.write("ENDDA=" + dtto + "\n");
		bw.write("\n");
		bw.flush();

		DateTimeFormatter dtf = null;
		if ("YYYY-MM-DD".equals(sDF))
			dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		else
			assert false;
		ParseIndexContext ctx = new ParseIndexContext(dtf, "E");

		ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(ar), utf8);
		ZipEntry ze;
		List<Path> todel = new ArrayList<Path>(100);

		for (Pair<String,List<String>> zz: aq) {
			String lang = zz.getKey();
			List<String> ax = zz.getValue();
			int q = 1;
			ctx.lang = lang;
			for (String x: ax) {
	        	HttpPost p = new HttpPost(getUrl(htx, EScheme.NoteListForm, lang));
	        	p.setEntity(new StringEntity(x));
	        	rsp = cl.execute(p);
	        	e = rsp.getEntity();
	        	Path fp = Cache.fs.getPath(dir.toString(), String.format("notelist_%s_%03d.html", lang, q));
	    		Utils.streamToFile(e.getContent(), Files.newOutputStream(fp), lang.equals("J") ? shift_jis : cp1252, utf8);
	        	ze = new ZipEntry(fp.getFileName().toString());
	        	zos.putNextEntry(ze);
	        	IOUtils.copy(Files.newInputStream(fp), zos);
	        	zos.closeEntry();
	        	zos.flush();

	        	parseList(pagesize, new Scanner(fp, utf8.name()), ctx);
	        	todel.add(fp);
		        S2 s2 = new S2(new RandomAccessFile(fp.toString(), "r"));
        		System.out.println(String.format("[%s] page #%d parsed (%d/%d)", lang, q, s2.pagecur, s2.pagesall));
	        	if (s2.latest()) 
	        		break;
	        	q++;
	    	}
		}
		bw.flush();
		bw.close();
		zos.putNextEntry(new ZipEntry(p2.getFileName().toString()));
		IOUtils.copy(Files.newInputStream(p2), zos);
		zos.closeEntry();
		todel.add(p2);

		cache.storeXml(ctx, xml);
		ctx.list.clear();
		zos.putNextEntry(new ZipEntry(xml.getFileName().toString()));
		IOUtils.copy(Files.newInputStream(xml), zos);
		zos.closeEntry();
		todel.add(xml);

		// store at archive
		zos.flush();
		zos.close();
		for (Path p: todel) Files.delete(p);
		
		// move archive
		Path p = Cache.fs.getPath(cache.supportSapComNotes.toString(), ar.getFileName().toString());
		if (Files.isRegularFile(p)) {
			Path pnew = Cache.fs.getPath(p.toString() + ".old." + Instant.now().getNano());
			Files.move(p, pnew);
//			throw new RuntimeException(p.toString());
		}
		Files.move(ar, p);
//		DirectoryStream<Path> qq = Files.newDirectoryStream(dir);
//		for (Path qx:qq) {
//			System.out.println("***"+qx);
//		}
//		Files.delete(dir); // tmpdir
	}
	String noteverscan(Cache cache, 
			String lang, 
			Path f, 
			PrintWriter pw, 
			List<Integer> vers) throws IOException {
		Scanner a = new Scanner(f, utf8.name());
		String p1 = "<span class=\"urImgCbgWhl1\" id=\"version";
		String p2 = "__keyNotChecked\" value=\"\">";
		String p3 = "<span ct=\"TextView\" class=\"urTxtStd urVt1\" style=\"white-space:nowrap;\">";
		String p4 = "</span>";
		String p5 = "<input type=\"hidden\" name=\"xsrftoken\" value=\"";
		String p6 = "\">";
		String pe = "</td></tr></table>";
		String xsrf = null;
		Pattern t1 = Pattern.compile("([0-9]+) .+");
		
		pw.print("<lang l='" + lang + "'");
		int j=0;
		while (a.hasNextLine()) {
			String x = a.nextLine().trim();
			if (x.startsWith(p1)) {
				x = ParseIndexContext.simpleSubstring("item", x, p1, p2, 0, null);
				Matcher m = t1.matcher(x);
				assert m.matches();
				pw.print("<version ver='" + m.group(1) + "'");
				vers.add(Integer.parseInt(m.group(1)));
			} else if (x.startsWith(p3)) {
				x = ParseIndexContext.simpleSubstring("item", x, p3, p4, 0, null);
				if (x.equals("&nbsp;")) { 
					j = 0; 
					continue;
				} else if (!x.equals("-")) {
					if (j==0) 
						pw.print(" begda='" + x + "'");
					else if (j==1)
						pw.print(" endda='" + x + "' />\n");
					assert j<2 : j;
					j++;
				}
			} else if (x.startsWith(p5)) {
				x = ParseIndexContext.simpleSubstring("xsfr", x, p5, p6, 0, null);
				xsrf = x;
				pw.print(" xsrftoken='" + xsrf + "'>\n");
			} else if (x.equals(pe)) break;
		}
		a.close();
		pw.println("</lang>");
    	pw.flush();
    	vers.sort(Comparator.comparing(x -> x));
    	assert xsrf!=null;
    	return xsrf;
	}
	private static boolean isObjectId(Object x) {
		return ((String)x).length()=="012006153200001160522015".length();
	}
	private static boolean isNNumber(Object x) {
		return (x instanceof Integer) && (int)x<9999999 & (int)x>0;
	}
	private static URI getUrl(HttpClientContext htx, EScheme e, String lang, Object ... args) throws URISyntaxException {
		String host = htx.getTargetHost().toString(), x, y;
		URI u = null;
		switch (e) {
		case CompareVersions:
			assert args.length == 1 && ((String)args[0]).length()=="012006153200001160522015".length();
			u = new URI(host + "/sap/bc/bsp/spn/sno_corr/compare_versions.htm?key=" + args[0] + "&language=" + lang);
			break;
		case NotePrintVersion:
			assert args.length == 1;
	    	x = "iv_mode=003&iv_sapnotes_number=" + args[0] + "&iv_language=" + lang;
	    	x = String.format("%060x", new BigInteger(1, (x.getBytes()))).toUpperCase();
	    	u = new URI(host + "/sap/bc/bsp/sno/ui_entry/entry.htm?param=" + x);
	    	break;
		case NoteLastFancyVersion:
			assert args.length == 1;
			x = "iv_mode=001&iv_sapnotes_number=" + args[0] + "&iv_language=" + lang;
//			x = "iv_mode=001&iv_sapnotes_number=" + args[0] + "&sap-language=" + lang;
			x = String.format("%060x", new BigInteger(1, (x.getBytes()))).toUpperCase();
			y = Base64.encode(x.getBytes(), 1000);
	    	u = new URI(host + "/sap/bc/bsp/sno/ui/main.do?param=" + x + "&bspapplicationffields=" + y);
//	    	u = new URI(host + "/sap/bc/bsp/sno/ui/main.do?param=" + x + "&param=" + x + "&bspapplicationffields=" + y);
			break;
		case NoteListForm:
	    	u = new URI(host + "/~form/handler");
	    	break;
		case NoteStatistic:
			assert args.length == 1 && isObjectId(args[0]) ;
			u = new URI(host + "/sap/support/notes/statistic/reads.htm?iv_key=" + args[0]);
			break;
		case NoteWUL:
			assert args.length==2 && isObjectId(args[0]);
			String sno_key = (String)args[0];
			String gr_type_key = (String)args[1];
			u = new URI(host + "/sap/bc/bsp/sno/ui/where_used_display.htm?iv_sapnotes_key="+sno_key+"&iv_gr_type_key="+gr_type_key);
			break;
		case NoteAttachment:
			assert args.length==3 && isObjectId(args[0]);
			sno_key = (String)args[0];
			String iv_version = (String)args[1];
			String guid = (String)args[2];
			u = new URI(host + "/sap/support/sapnotes/public/services/attachment.htm?iv_key="+sno_key+"&iv_version="+iv_version+"&iv_guid="+guid);
			break;
		case NoteChangeLog:
			assert args.length==1 && isObjectId(args[0]);
			u = new URI(host + "/sap/bc/bsp/sno/ui/change_log_display.htm?key=" + args[0]);
			break;
		case InternalMemo:
			assert args.length==1 && isObjectId(args[0]);
			u = new URI(host + "/sap/bc/bsp/sno/ui/memo_display.htm?key=" + args[0]);
			break;
		case SSCR:
			assert args.length==2 && isNNumber(args[0]);
			u = new URI(host + "/sap/bc/bsp/spn/sno_corr/note_sscr.htm?iv_nnum=" + args[0] + "&iv_vernr=" + args[1]);
			break;
		case Pilot:
			assert args.length==1 && isNNumber(args[0]);
			u = new URI(host + "/sap/bc/bsp/spn/sno_corr/pilot_customers.htm?iv_number=" + args[0]);
			break;
		case CWB:
			assert args.length==1 && isNNumber(args[0]);
			u = new URI(host + "/sap/bc/bsp/spn/sno_corr/cwb_status.ajax?lv_numm=" + args[0]);
			break;
		case Nit:
			assert args.length==1 && isNNumber(args[0]);
			u = new URI(host + "/sap/bc/bsp/spn/sno_corr/NNF_Gui_perform_nit.sap?pv_numm=" + args[0]);
			break;
		case DPA:
			assert args.length==2 && isNNumber(args[0]);
			u = new URI(host + "/sap/bc/bsp/spn/sno_corr/NNF_Gui_perform_dpa.sap?sts="+args[1] + "&pv_numm=" + args[0]);
			break;
		case PutDownloadBasket:
			assert args.length==1 && isObjectId(args[0]);
			u = new URI(host + "/sap/bc/bsp/spn/download_basket/download.htm?objid=" + args[0] +"&_APP=01100107900000000263&_EVENT=DL_ERROR&STATUS=OBJADD'" );
			break;
		case UserProfile:
			assert args.length==0;
			u = new URI(host + "/sap/bc/bsp/spn/smp_user_pref/userpref.htm?referrer=public%2fssphome");
			break;
		default:
			throw new RuntimeException("Unknonwn scheme: " + e);
		}
		return u;
	}

//	String getObjid(CloseableHttpClient cl, HttpClientContext htx
//			, Cache cache, int number, boolean httpEnable) 
//			throws IOException, URISyntaxException {
//		assert number > 0;
//		
//		String objid = null;
//		HttpGet get;
//		Scanner a;
//		Pattern pt;
//		String q;
//		int j="002007204200000280162010".length();
//		pt = Pattern.compile("/sap/support/notes/statistic/reads.htm\\?iv_key=([0-9]{"+j+"})");
//		Path dir = cache.getPathByScheme(EScheme.ZRoot, number, null);
//		DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*_fancy_?.html");
//		Iterator<Path> it = ds.iterator();
//		q = null;
//		while (it.hasNext()) {
//			a = new Scanner(it.next(), utf8.name());
//			q = a.findWithinHorizon(pt, 100000);
//			if (q!=null) objid = new String(q.substring(q.length()-j, q.length()));
//			a.close();
//		}
//		if (httpEnable&&(objid==null||"".equals(objid))) {
//			Path latestFancy = cache.getPathByScheme(EScheme.NoteLastFancyVersion, number, "E", 0);
//			get = new HttpGet(getUrl(htx, EScheme.NoteLastFancyVersion, "E", number));
//			entityToFile(cl.execute(get).getEntity(), latestFancy, cp1252, utf8);
//			a = new Scanner(latestFancy);
//			q = a.findWithinHorizon(pt, 100000);
//			if (q!=null) objid = new String(q.substring(q.length()-j, q.length()));
//			a.close();
//		}
//		return objid;
////    	Path dl = cache.getPathByScheme(EScheme.PutDownloadBasket, number, null);
////		if (!Files.isRegularFile(dl)) {
////			get = new HttpGet(getUrl(htx, EScheme.PutDownloadBasket, null, objid));
////			entityToFile(cl.execute(get).getEntity(), dl, fromCs, toCs);
////		}
//	}

	// <TD CLASS="numberofnotes"><H3 CLASS="note">%d SAP Notes found</H3></TD>
    // <TD CLASS="numberofpages">Page 1 of 1</TD>
	class S2 {
		static final String p1 = "<TD CLASS=\"numberofnotes\"><H3 CLASS=\"note\">";
		static final String p2 = "</H3></TD>";
		static final String p3 = "<TD CLASS=\"numberofpages\">Page ";
		static final String p4 = "</TD>";
		private final Pattern rx1 = Pattern.compile("([0-9]+) SAP Notes found");
		private final Pattern rx2 = Pattern.compile("([0-9]+) of ([0-9]+)");
		
		int pagecur, pagesall, notesfound;
		S2(RandomAccessFile f) throws IOException {
			byte[] b = new byte[(int) f.length()];
			f.readFully(b);
			String x = new String(b);
			int a1 = x.indexOf(p1);
			int a2 = x.indexOf(p2, a1);
			int a3 = x.indexOf(p3, a2);
			int a4 = x.indexOf(p4, a3);
			
			String y = x.substring(a1 + p1.length(), a2);
			String z = x.substring(a3 + p3.length(), a4);
//			System.out.println(y);	System.out.println(z);
			Matcher m = rx1.matcher(y);
			if (m.matches()) {
				notesfound = Integer.parseInt(m.group(1));
			}
			m = rx2.matcher(z);
			if (m.matches()) {
				pagecur = Integer.parseInt(m.group(1));
				pagesall = Integer.parseInt(m.group(2));
			}
			b = null;
			f.close();
		}
		boolean latest() {
			return pagecur >= pagesall;
		}
		public String toString() {
			return "" + notesfound + " notes found, " + pagecur + "/" + pagesall;
		}
		String newName(S2 s, String dtfrom, String dtto, String langu) {
			 String nn;
			 nn = "notes_" + dtfrom + "--" + dtto + "_" + String.format("%03d", pagecur) + "_" + langu + ".html";
			 return nn;
		}
	}

	void close() {
		//TODO close Http connections
	}
	static void cache(Cache cache, NotesDB db) throws IOException, SQLException {
		Iterator<Path> it = cache.getSupportSapComNotesZip();
		Map<String,String> cached = db.w44();  		

		while (it.hasNext()) {
			Path q = it.next();
			String x = q.getFileName().toString().replace(".zip", ".xml");
			ZipInputStream zis = new ZipInputStream(Files.newInputStream(q), utf8);
			ZipEntry ze = zis.getNextEntry();
			while(!ze.getName().equals(x)) {
				ze = zis.getNextEntry();
			}
			if (ze.getName().equals(x)) {
				String y = ze.getLastModifiedTime().toString();
				String yc = cached.get(x);
				if (yc==null || !yc.equals(y)) {
					System.out.println(x);
					Snotes sn = JAXB.unmarshal(zis, Snotes.class);
					assert sn!=null && sn.getNS()!=null && sn.getNS().size()>0;
					db.walk22(sn, yc==null, x, y);
				}
//				for (JaxbNote j: sn.n) {
//					if (j.l.equals("J") && !prj.containsKey(j.c)) {
//						for (JaxbNote e: sn.n) if (e.n==j.n) {
//							prj.put(j.c, e.c);
//							break;
//						}
//						System.out.println(String.format("(\"%s\", \"%s\", ), \\%d %s", StringEscapeUtils.escapeJava(j.c), prj.get(j.c), j.n, j.d));
//					}
//				}
//				System.out.println(x + "\t" + ft);
			}
			zis.close();
		}
		db.walk22(null, false, null, null); // clear the caches
	}
	
	public static void main(String a[]) throws Exception {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate ld = LocalDate.parse("2016-01-14", dtf);
		LocalDate prev = ld.minusMonths(1);
		System.out.println(ld + "\t" + prev);
//		ZonedDateTime zd = ZonedDateTime.of(ld, ZoneId.of("UTF"));
//		System.out.println(zd);
	}
	
	void zZz(Cache cache, String dtfrom, String dtto, boolean cmd) 
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException, URISyntaxException, ParseException {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
		LocalDate b = LocalDate.parse("19900101", dtf), t, tm = LocalDate.now();
		Iterator<Path> it = cache.getSupportSapComNotesZip();
		int ps = 5000, mx = 1000000;
		if (dtfrom!=null && dtto!=null && !cmd) { 
			probeUrl();
			getParseUserProfile();
			iter(cache, ps, mx/ps, dtfrom, dtto);
			return;
		} 
		List<String> z = new ArrayList<String>();
		while (it.hasNext()) {
			Path q = it.next();
			LocalDate from =  LocalDate.parse(q.getFileName().toString().substring(6,14), dtf);
			LocalDate to =  LocalDate.parse(q.getFileName().toString().substring(16,24), dtf);
			while (!b.isEqual(from) && b.isBefore(from)) {
				t = b.plusMonths(1);
				z.add(dtf.format(b));
				z.add(dtf.format(t));
				b = t;
			}
			b = to;
		}
		while (b.isBefore(tm)) {
			t = b.plusMonths(1);
			z.add(dtf.format(b));
			z.add(dtf.format(t));
			b = t;
		}
		if (z.size()==0) return;
		if (!cmd) {
			probeUrl();
			getParseUserProfile();
			for (int i=0; i<z.size(); i+=2) 
				iter(cache, ps, mx/ps, z.get(i), z.get(i+1));
		} else {
			for (int i=0; i<z.size(); i+=2)
				System.out.println(String.format("call online.bat GETLIST %s %s", z.get(i), z.get(i+1)));
		}
	}

//	private Path getPathByScheme(EScheme scheme, int number, String lang, Object ... args) throws IOException {
//	assert number > 0;
//	Path dir = rawver(number);
//	switch (scheme) {
//	case CompareVersions:
//		assert isLang(lang);
//		return fs.getPath(dir.toString(), "compare_" + lang + ".html");
//		
//	case ZVersions:
//		return fs.getPath(dir.toString(), "versions.xml");
//		
//	case NotePrintVersion:
//		assert isLang(lang) && args.length>0;
//		return fs.getPath(dir.toString(), number + "_" + args[0] + "_print_" + lang + ".html");
//		
//	case NoteLastFancyVersion:
//		assert isLang(lang) && args.length>0;
//		return fs.getPath(dir.toString(), number + "_" + args[0] + "_fancy_" + lang + ".html");
//
//	case NoteChangeLog:
//		assert lang==null;
//		return fs.getPath(dir.toString(), "changelog.html");
//
//	case InternalMemo:
//		assert lang==null;
//		return fs.getPath(dir.toString(), "intmemo.html");
//
//	case ZRoot:
//		assert lang==null;
//		return dir;
//
//	case PutDownloadBasket:
//		assert lang==null;
//		return fs.getPath(dir.toString(), "dl.html");
//		
//	default:
//		throw new RuntimeException("Unsupported scheme: " + scheme + " for note " + number);
//	}
//}

}

//TODO
//https://websmp110.sap-ag.de/~form/handler?_APP=00200682500000002095&_EVENT=DISPL_MAIN&_COMP=		application areas viewer
//https://smpdl.sap-ag.de/~swdc/012002523100020457252015E/CDLABEL.htm?_ACTION=CONTENT_INFO
//
//
//static URL getNoteURL(String number, String mode) throws MalformedURLException {
//// что-то вроде https://service.sap.com/sap/bc/bsp/sno/ui_entry/entry.htm?param=69765F6D6F64653D3030312669765F7361706E6F7465735F6E756D6265723D3232353338343926
//String c = "https://service.sap.com/sap/bc/bsp/sno/ui_entry/entry.htm";
//String p = "?param=";
//String v = "iv_mode=" + mode + "&iv_sapnotes_number=" + number;
//String w =  DatatypeConverter.printHexBinary(v.getBytes());
//URL u = new URL(c+p+w);
//return u;
//}
//static URL getNotePdfURL() {
//// https://websmp130.sap-ag.de/sap/support/notes/convert2pdf/0002253849?sap-language=EN
//return null;
//}

//static URL getNoteStatistic(String key) throws MalformedURLException {
//// key is like 012006153200001854902015
//String c = "https://service.sap.com/sap/support/notes/statistic/reads.htm?iv_key=" + key;
//return new URL(c);
//}
