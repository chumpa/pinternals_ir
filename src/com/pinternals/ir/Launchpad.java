package com.pinternals.ir;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.bind.DataBindingException;
import javax.xml.bind.JAXB;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SgmlPage;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.xml.XmlPage;
import com.sap2.CWBCIHEAD;
import com.sap2.CWBCIOBJ;
import com.sap2.CWBCIVALID;
import com.sap2.CWBCMTEXT;
import com.sap2.CWBNTCI;
import com.sap2.CWBNTFIXED;
import com.sap2.CWBNTGATTR;
import com.sap2.CWBNTHEAD;
import com.sap2.CWBNTSTXT;
import com.sap2.CWBNTVALID;

class NoteRetrException extends RuntimeException {
	private static final long serialVersionUID = 9041797099776264027L;
	final static String nry = "This note has not been released"
			, interrorCode1 = "HTTP/500/E/Internal Server Error"
			, interrorCode2 = "In the context of Data Services an unknown internal server error occured";
	java.util.Properties dispjson = null;
	com.sap.lpad.Entry en0=null, en1=null;
	boolean notreleased = false, internalerror;
	String errText = null, errCode;
	char lang;
	URL url = null;
	int rc=0, step=-1, mark=-1, number=0, version=-1;
	
	private List<String> facet2 = new ArrayList<String>();
	
	List<URL> makeURL() throws MalformedURLException {
		assert mark>NotesDB.SAP_UNKNOWNYET && mark<NotesDB.SAP_INVALID : mark;
		String b=null;
		List<URL> urls = new ArrayList<URL>();
		switch (mark) {
		case NotesDB.SAP_NOTE:
			b = "https://launchpad.support.sap.com/services/odata/svt/snogwscorr/TrunkSet(SapNotesNumber='%010d',Version='%d',Language='%s')%s";
			break;
		case NotesDB.SAP_KBA:
			b = "https://launchpad.support.sap.com/services/odata/svt/snogwskba/TrunkSet(SapNotesNumber='%010d',Version='%d',Language='%s')%s";
			break;
		case NotesDB.SAP_SECNOTE:
			b = "https://launchpad.support.sap.com/services/odata/svt/snogwssecurity/TrunkSet(SapNotesNumber='%010d',Version='%d',Language='%s')%s";
			break;
		default:
			assert false: String.format("Unsupported scheme: %d", mark); 
		}
		if (step==0) {
			urls.add(new URL(String.format(b, number, version, lang, "?$expand=Languages") ));
		} else if (step==1) {
			assert en0!=null;
			StringJoiner facets = new StringJoiner(",");
			// TODO for security notes VersionInfo causes error: Method 'VERSIONINFOSET_GET_ENTITYSET' not implemented in data provider class.
			// https://launchpad.support.sap.com/services/odata/svt/snogwssecurity/TrunkSet(SapNotesNumber='0001232259',Version='5',Language='E')?$expand=RefBy,RefTo,CorrIns,Patch,Sp,OtherCom,SoftCom,Attach,LongText,VersionInfo,Languages,SideCau,SideSol 
			for (com.sap.lpad.Link l: en0.getLink()) if (!"self".equals(l.getRel()) ) 
				if (!"VersionInfo".equals(l.getTitle())) { 
					facets.add(l.getTitle());
					facet2.add(l.getTitle());
				}
			urls.add(new URL(String.format(b, number, version, lang, "?$expand=" + facets)));
		} else {
			for (String s: facet2) urls.add(new URL(String.format(b, number, version, lang, "/" + s)));
		}
		return urls;
	}

	void parseAlias() throws NoteRetrException {
		assert dispjson != null : dispjson;
		String alias = dispjson.getProperty("alias");
		if (alias==null) {
			errText = String.format("No 'alias' found at %s", dispjson.toString());
			throw this;
		}
		switch (alias) {
		case "" : mark = NotesDB.SAP_INVALID; break;
		case "CORR" : mark = NotesDB.SAP_NOTE; break;
		case "SECURITY": mark = NotesDB.SAP_SECNOTE; break;
		case "KBA" : mark = NotesDB.SAP_KBA; break;
		case "OTHER" : mark = NotesDB.SAP_ONE; break;
		case "STAND" : mark = NotesDB.SAP_STAND; break;
		default:
			errText = String.format("Unknown alias: %s", alias);
			throw this;
		}
	}
	
	NoteRetrException(int number, int version, char lang, int mark) {
		assert lang!=0 : lang;
		assert number>0 : number;
		this.number = number;
		this.version = version;
		this.lang = lang;
		this.mark = mark;
		this.step = 0;
	}

	void raiseError(Path ph) throws IOException {
//		System.err.println(String.format("HTTP error %d when ask %s", rc, u));
		/* there are 4 error rezults, look at `ph':
		 1. Text asnwer: "An existing connection was forcibly closed by the remote host" or "An existing connection was forcibly closed by backend .."
		 2. html-answer:
	<html><head><title>Error report</title></head><body><h1>HTTP Status 500 - An internal application error occurred.Request: 137724313 supportportal:supportshell</h1></body></html>
		 3. xml-answer by Abap:
	<?xml..?><error><code>HTTP/500/E/Internal Server Error</code>
	  <message>The following error text was processed in system W72 : http://service.sap.com/sap/support/notes/821267 cannot be interpreted as a number</message></error>	 
		 4. xml-answer by OData:
	<?xml..?><error xmlns="http://schemas.microsoft.com/ado/2007/08/dataservices/metadata">....</error>	  
		*/
		// for OData-style errors
		try {
			com.sap.err.Error error = JAXB.unmarshal(Files.newInputStream(ph), com.sap.err.Error.class);
			// for internal errors
			com.sap.lpad.Error err2 = JAXB.unmarshal(Files.newInputStream(ph), com.sap.lpad.Error.class);
			assert error!=null&&err2!=null;
			
			if (err2.getCode()!=null) {
				this.errCode = err2.getCode();
				internalerror = interrorCode1.equals(errCode);
				this.errText = err2.getMessage().trim();
			} else if (error.getCode()!=null) {
				this.errText = error.getMessage().getContent().trim();
				notreleased = rc==400 & nry.equals(errText);
				internalerror = interrorCode2.equals(this.errText);
			} else {
				this.errText = Files.lines(ph).collect(Collectors.joining(""));  
			}
		} catch (DataBindingException e) {
			this.errText = Files.lines(ph).collect(Collectors.joining(""));  
		}
	}
	
	@Deprecated
	NoteRetrException(Path ph, URL u, int rc) throws IOException {
//		System.err.println(String.format("HTTP error %d when ask %s", rc, u));
		/* there are 4 error rezults, look at `ph':
		 1. Text asnwer: "An existing connection was forcibly closed by the remote host" or "An existing connection was forcibly closed by backend .."
		 2. html-answer:
	<html><head><title>Error report</title></head><body><h1>HTTP Status 500 - An internal application error occurred.Request: 137724313 supportportal:supportshell</h1></body></html>
		 3. xml-answer by Abap:
	<?xml..?><error><code>HTTP/500/E/Internal Server Error</code>
	  <message>The following error text was processed in system W72 : http://service.sap.com/sap/support/notes/821267 cannot be interpreted as a number</message></error>	 
		 4. xml-answer by OData:
	<?xml..?><error xmlns="http://schemas.microsoft.com/ado/2007/08/dataservices/metadata">....</error>	  
		*/
		// for OData-style errors
		try {
			com.sap.err.Error error = JAXB.unmarshal(Files.newInputStream(ph), com.sap.err.Error.class);
			// for internal errors
			com.sap.lpad.Error err2 = JAXB.unmarshal(Files.newInputStream(ph), com.sap.lpad.Error.class);
			assert error!=null&&err2!=null;
			
			if (err2.getCode()!=null) {
				this.errCode = err2.getCode();
				internalerror = interrorCode1.equals(errCode);
				this.errText = err2.getMessage().trim();
			} else if (error.getCode()!=null) {
				this.errText = error.getMessage().getContent().trim();
				notreleased = rc==400 & nry.equals(errText);
				internalerror = interrorCode2.equals(this.errText);
			} else {
				this.errText = Files.lines(ph).collect(Collectors.joining(""));  
			}
		} catch (DataBindingException e) {
			this.errText = Files.lines(ph).collect(Collectors.joining(""));  
		}
		this.url = u;
		this.rc = rc;
//		throw new RuntimeException(String.format("Failed (rc=%d) to get %s, look at %s%n%s - %s", rc, u, ph, error.getCode(), error.getMessage().getContent()));
	}
}

class NoteA {
	static final Charset utf8 = Charset.forName("UTF-8");
	int num, versionAsked, versionDownload;
	String langAsked;
	String hdTHEMK;	// application area

	Map<String,String> titles = new HashMap<String,String>();
	Map<String,String> ntg = new HashMap<String,String>();

	public NoteA(String zipname) {
		String sa[] = zipname.split("_");
		num = Integer.parseInt(sa[0]);
		versionAsked = Integer.parseInt(sa[1]);
		langAsked = sa[2].substring(0,1);
		assert num>0 && versionAsked>0 && "DEJ".contains(langAsked);
	}
	public String toString() {
		String s = String.format("%d.%d [%s %s]", num, versionAsked, hdTHEMK, titles.get("E"));
		return s;
	}
	/**
	 * Parse downloaded from launchpad.support.sap.com zip-file
	 * 
	 * Before <b>parseNoteDownloaded</b>, NoteA have to be created with <b>NoteA(String zipname)</b>
	 * @param zis
	 * @param debug
	 * @throws IOException
	 */
	public void parseNoteDownloaded(InputStream zis, Path debug) throws IOException {
		com.sap1.Abap ab = JAXB.unmarshal(new CloseShieldInputStream(zis), com.sap1.Abap.class);
//		for (com.sap1.Values.XMLOBJECTDATABIN.CWBCIOBJDELTA cw: ab.getValues().getXMLOBJECTDATABIN().getCWBCIOBJDELTA())
//			objectdata.add(new ObjectData(cw));
		byte xmlDataBin[] = ab.getValues().getXMLDATABIN();
		byte codeDeltaBin[] = ab.getValues().getXMLCODEDELTABINT();

		if (debug!=null) {
			Path t1 = debug.resolve(String.format("./%010d_%d_%s_databin.xml", num, versionAsked, langAsked));
			if (!Files.isRegularFile(t1) || Files.size(t1)==0)
				Files.newOutputStream(t1).write(xmlDataBin);
			Path t2 = debug.resolve(String.format("./%010d_%d_%s_codebin.xml", num, versionAsked, langAsked));
			if (!Files.isRegularFile(t2) || Files.size(t2)==0)
				Files.newOutputStream(t1).write(codeDeltaBin);
		}
		
		com.sap2.Abap ab2 = JAXB.unmarshal(new ByteArrayInputStream(xmlDataBin), com.sap2.Abap.class);
		com.sap3.Abap ab3 = JAXB.unmarshal(new ByteArrayInputStream(codeDeltaBin), com.sap3.Abap.class);
		ab = null;
		xmlDataBin = null;
		codeDeltaBin = null;

		assert ab2.getValues()!=null && ab2.getValues().getXMLCWBNTHEAD()!=null;
		CWBNTHEAD hd = ab2.getValues().getXMLCWBNTHEAD().getCWBNTHEAD();
		assert num==Integer.parseInt(hd.getNUMM()) : num + "==" + hd.getNUMM();
		versionDownload = Integer.parseInt(hd.getVERSNO());
		if (versionAsked!=versionDownload) {
			System.err.println(String.format("For %d asked version %d, got %d", num, versionAsked, versionDownload));
		}
		hdTHEMK = new String(hd.getTHEMK());
		for (CWBNTSTXT cw: ab2.getValues().getXMLCWBNTSTXT().getCWBNTSTXT()) {
			assert num==Integer.parseInt(cw.getNUMM());
			assert versionDownload==Integer.parseInt(cw.getVERSNO());
			titles.put(new String(cw.getLANGU()), new String(cw.getSTEXT()));
		}
		int c = 0;
		for (com.sap2.Htmltextitem it: ab2.getValues().getXMLHTMLTEXT().getItem()) {
			c++;
			assert num==Integer.parseInt(it.getNOTE().getNUMM());
			assert versionDownload==Integer.parseInt(it.getNOTE().getVERSNO());
//			String lang = new String(it.getNOTE().getLANGU());
//			String html = it.getTEXT();
//			System.out.println(html);
		}
		assert c==ab2.getValues().getXMLCWBNTDATA().getItem().size(); // right part is TDLINE form of getXMLHTMLTEXT
		// for java
		for (CWBNTVALID v: ab2.getValues().getXMLCWBNTVALID().getCWBNTVALID()) {
			assert num==Integer.parseInt(v.getNUMM());
			assert versionDownload==Integer.parseInt(v.getVERSNO());
			String vDEPAKID = v.getDEPAKIDV();	//4350
			String vDEALEID = v.getDEALEIDV();
			assert vDEALEID.length() == "0000000148".length();
			assert Integer.parseInt(vDEPAKID) > 0 && Integer.parseInt(vDEALEID)>0;
		}
		// valid for Java notes
		for (CWBNTGATTR a: ab2.getValues().getXMLCWBNTGATTR().getCWBNTGATTR()) {
			assert num==Integer.parseInt(a.getNUMM());
			assert versionDownload==Integer.parseInt(a.getVERSNO());
			String id = a.getID();
			String val = a.getVALUE();
			/* for java notes, id:val are: 
				SAP_STATUS: 00 
				SIDEEFFECT: 0001710679 0001539241 ... note number
				NT_STATUS: 00 
				MASTERLANG: D E 
				POST_PROC: X 
				NT_CATEG: A B C D E F G H I K M N O P R T U W X Y 
			 */
			ntg.put(id, val);
		}

		// ------------------------------------------------------------------------------------------------------------
		// following facets only for ABAP
		for (CWBNTCI ci: ab2.getValues().getXMLCWBNTCI().getCWBNTCI()) {
			assert num==Integer.parseInt(ci.getNUMM());
			assert versionDownload==Integer.parseInt(ci.getVERSNO());
//			<CIINSTA>0120061532</CIINSTA>
//			<CIPAKID>41</CIPAKID>
//			<CIALEID>0001358365</CIALEID>
//			<CIVERSNO>0002</CIVERSNO>
		}
		for (CWBNTFIXED fx: ab2.getValues().getXMLCWBNTFIXED().getCWBNTFIXED()) {
			assert num==Integer.parseInt(fx.getNUMM());
			assert versionDownload==Integer.parseInt(fx.getVERSNO());
//			<PAKID>41</PAKID>
//			<ALEID>0000000543</ALEID>
		}
		// X-ML_CWBCIHEAD и X-ML_CWBCIHEAD3 совпадают на вид
		for (CWBCIHEAD hx: ab2.getValues().getXMLCWBCIHEAD3().getCWBCIHEAD()) {
			assert hx.getINSTA().length() == "0120061532".length();
//			<PAKID>41</PAKID>
//			<ALEID>0001372663</ALEID>
//			<VERSNO>0001</VERSNO>
//			<FORMAT_ID>3</FORMAT_ID>
//			<INCOMPLETE/>
//			<MANUAL_ACTIVITY/>
//			<IS_TRANSPORTABLE/>
		}
		for (CWBCIVALID cv: ab2.getValues().getXMLCWBCIVALID().getCWBCIVALID()) {
			assert cv.getINSTA().length() == "0120061532".length();
//			<PAKID>41</PAKID>
//			<ALEID>0001358365</ALEID>
//			<VERSNO>0002</VERSNO>
//			<TRPAKID>41</TRPAKID>
//			<TRTYPEID>P</TRTYPEID>
//			<TRALSID>000027</TRALSID>
//			<DEPAKID_LO>41</DEPAKID_LO>
//			<DEALEID_LO>0000000460</DEALEID_LO>
//			<DEPAKID_HI>41</DEPAKID_HI>
//			<DEALEID_HI>0000000554</DEALEID_HI>
		}

		// CWBCIOBJ -- транспортные объекты или
		for (CWBCIOBJ tr: ab2.getValues().getXMLCWBCIOBJ().getCWBCIOBJ()) {
			assert tr.getINSTA().length() == "0120061532".length();
//			<PAKID>41</PAKID>
//			<ALEID>0001358365</ALEID>
//			<VERSNO>0002</VERSNO>
//			<PGMID>LIMU</PGMID>
//			<OBJECT>WAPP</OBJECT>
//			<OBJ_NAME>SXIDEMO_AGCY_UI CHECK_AVAIL_ERROR.HTM</OBJ_NAME>
//			<TRPGMID>R3TR</TRPGMID>
//			<TROBJECT>WAPA</TROBJECT>
//			<TROBJ_NAME>SXIDEMO_AGCY_UI</TROBJ_NAME>
		}
//		<CWBCMPNT>
//			<PAKID>41</PAKID>
//			<PAKTXT>SAP_BASIS</PAKTXT>
//		</CWBCMPNT>
		
		for (CWBCMTEXT tx: ab2.getValues().getXMLCWBCMTEXT().getCWBCMTEXT()) {
			assert "DEJ".contains(tx.getLANGU());
//			<PAKID>41</PAKID>
//			<DESCR>SAP Basis component</DESCR>
		}
		
//		for (CWBCMLAST ls: ab2.getValues().getXMLCWBCMLAST().getCWBCMLAST()) {
//			<PAKID>41</PAKID>
//			<TSTAMP>20160120152034</TSTAMP>
//		}
//		for (CWBDEHEAD hx: ab2.getValues().getXMLCWBDEHEAD().getCWBDEHEAD()) {
//			<PAKID>41</PAKID>
//			<ALEID>0000000001</ALEID>
//			<ALETXT>46A</ALETXT>
//			<TYPID>R</TYPID>
//			<ALSID>000004</ALSID>
//		}
//		for (CWBDEPRDC pr: ab2.getValues().getXMLCWBDEPRDC().getCWBDEPRDC()) {
//			<PAKID>41</PAKID>
//			<ALEID>0000000002</ALEID>
//			<PAKID_P>41</PAKID_P>
//			<ALEID_P>0000000027</ALEID_P>
//		}
		// трек поставки
//		for (CWBDETRACK tr: ab2.getValues().getXMLCWBDETRACK().getCWBDETRACK()) {
//			<PAKID>41</PAKID>
//			<TYPID>A</TYPID>
//			<ALSID>000010</ALSID>
//			<ALSTXT/>
//		}
		// совсем другой сегмент
		for (com.sap3.Item it: ab3.getValues().getXMLCWBCIDATA().getItem()) {
			String insta = it.getCIHEAD().getINSTA();
			assert insta.length() == "0120061532".length();
//			String pakid = it.getCIHEAD().getPAKID();
//			String aleid = it.getCIHEAD().getALEID();
//			String versno = it.getCIHEAD().getVERSNO();
//			for (com.sap3.Item d: it.getDELTAS().getItem()) {
//				//HEADER
//				//STATEMENT_PATTERNS
//				//CODE_BLOCK_DELTAS
//			}
		}
	}
	void parseKBA(InputStream zis) {
		
	}
}

class AZ{
	int area, num, mark, cat, prio, dled; //, longTexts=0, swcv=0, sp=0;
	String objid=null;//, askdate=null, langMaster=null;
	char lang;
	boolean error = false;
	com.sap.lpad.Properties mprop=null;
	
	@Override
	public String toString() {
		return String.valueOf(num);
	}
	
	boolean secnote() {
		assert mprop!=null;
		return mprop.getType().equals("SAP Security Note");
	}

	/**
	 * constructor from CDB, DBA
	 * @param num
	 * @param area
	 * @param obj
	 */
	AZ(int num, int area, int mark, int cat, int prio, boolean error) {
		assert num>0 : num;
		assert mark>=0 : mark;
		assert cat>0 && prio>0 : cat + " " + prio;
		this.num = num;
		this.mark = mark;
		this.cat = cat;
		this.prio = prio;
		this.error = error;
		this.area = area;
	}

	AZ(int num, int mark, char lang) {
		this.num = num;
		this.mark = mark;
		this.lang = lang;
	}
	AZ(String s) {
		num = Integer.parseInt(s.substring(0, 10));
		if (s.endsWith("_error.xml")) {
			error = true;
			lang = 0;
		} else {
			lang = s.charAt(11);
		}
	}
}

public class Launchpad {
	static final Charset utf8 = Charset.forName("UTF-8");
	private Cache cache = null;
	List<NotesDB> dbas = null;
	WebClient wc = null;
	Path inbox = null;
	private String uname; 
	private HttpHost prHost;
	private Credentials prCred;

	Launchpad (Cache cache) throws IOException {
		this.cache = cache;
		this.inbox = cache.launchpad.resolve("_inbox");
		if (!Files.isDirectory(this.inbox)) Files.createDirectory(this.inbox); 
	}
	
	/**
	 * Create central launchpad object
	 * @param cache	nonnull cache
	 * @param uname	nullable S-uname
	 * @param prHost	nullable http-proxy host
	 * @param prCred	nullable http-proxy credentials
	 * @throws IOException
	 */
	Launchpad (Cache cache, String uname, HttpHost prHost, Credentials prCred) throws IOException {
		this.cache = cache;
		if (!Files.isRegularFile(cache.getLaunchpadArealist())) Files.createFile(cache.getLaunchpadArealist());
		this.uname = uname;
		this.prHost = prHost;
		this.prCred = prCred;
		this.inbox = cache.launchpad.resolve("_inbox");
		if (!Files.isDirectory(this.inbox)) Files.createDirectory(this.inbox); 

	}

	Launchpad (Cache cache, WebClient wc) {
		assert wc!=null;
		this.cache = cache;
		this.wc = wc;
	}
	/**
	 * Parse arealist.txt
	 * @param	px	path to arealist.txt, cache.getLaunchpadArealist()
	 * @return	Map<String,List<Pattern>> 
	 * @throws	IOException
	 */
	private static Map<String,List<Pattern>> parseAreaList(Path px) throws IOException {
		Map<String,List<Pattern>> q = new HashMap<String,List<Pattern>>();
		Scanner a = new Scanner(Files.newBufferedReader(px, utf8));
		while (a.hasNextLine()) {
			String z[] = a.nextLine().split("\t");
			if (z==null || z.length<2) continue;
			List<Pattern> lp = new ArrayList<Pattern>();
			for (int i=1; i<z.length; i++) 
				lp.add(Pattern.compile(z[i]));
			q.put(z[0], lp);
		}
		a.close();
		return q;
	}

	/**
	 * 
	 * @param cdb	central notes.db
	 * @throws IOException
	 * @throws SQLException
	 */
	void areaList(NotesDB cdb) throws IOException, SQLException {
		assert cdb!=null && cache!=null;
		assert dbas==null;
		dbas = new ArrayList<NotesDB>();
		cdb.initArea();
		Area.reload();
		Map<String,List<Pattern>> lines = parseAreaList(cache.getLaunchpadArealist());

		for (Map.Entry<String, List<Pattern>> e: lines.entrySet()) {
			String nick = e.getKey();
			Path p, t, q, dx;
			p = cache.launchpad.resolve(nick);
			if (!Files.isDirectory(p)) Files.createDirectory(p);
			t = p.resolve("tmp");
			if (!Files.isDirectory(t)) Files.createDirectory(t);
			q = p.resolve("areas.txt");
			dx = p.resolve(nick+".db");

			NotesDB dba = null;
			if (Files.isRegularFile(dx))
				dba = new NotesDB(dx, false, true, true);
			else {
				System.out.println("To create: " + dx);
				dba = new NotesDB(dx, true, true, true);
			}
			BufferedWriter bw = Files.newBufferedWriter(q, utf8);
			for (Map.Entry<String, Integer> ea: Area.areaToCode.entrySet()) {
				String area = ea.getKey();
				boolean g = false;
				for (Pattern pt: e.getValue()) if (pt.matcher(area).matches()) {
					bw.write(area + "\t" + Area.areaToDescr.get(area) + "\n");
					g = true;
				}
				if (g) Area.addForNick(nick, area);
			}
			bw.close();
			dbas.add(dba);
		}
	}

	
//	static URL getBySchemeZip(String lang, int num, int ver, String value) throws MalformedURLException {
//		String s = String.format("https://launchpad.support.sap.com/services/odata/svt/snogwscorr"
//				+ "/Zip4SSet(SapNotesNumber='%010d',Version='%04d',Language='%s')%s", num, ver, lang, value);
//    	URL u = new URL(s);
//    	return u;
//	}
	
	//
	public static WebClient getLaunchpad(String uname, HttpHost prHost, Credentials prCred) throws IOException, UnknownServiceException {
		URL ln = new URL("https://launchpad.support.sap.com/services/odata/svt/snogwscorr");
		WebClient webClient = null;
		assert uname != null;
		
        if (prHost!=null)
        	webClient = new WebClient(BrowserVersion.CHROME, prHost.getHostName(), prHost.getPort());
        else
        	webClient = new WebClient(BrowserVersion.CHROME);

        String passwd = Utils.getPasswd(uname);
        CredentialsProvider cp = webClient.getCredentialsProvider();
        if (prHost!=null) 
        	cp.setCredentials(new AuthScope(prHost), prCred);
        
        UsernamePasswordCredentials suser = new UsernamePasswordCredentials(uname, passwd);
        cp.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), suser);

        com.gargoylesoftware.htmlunit.Cache zc = new com.gargoylesoftware.htmlunit.Cache();
        webClient.setCache(zc);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setAppletEnabled(false);
        webClient.getOptions().setGeolocationEnabled(false);
        webClient.getOptions().setPopupBlockerEnabled(true);
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getOptions().setUseInsecureSSL(true);
	    webClient.getOptions().setJavaScriptEnabled(true);
	    webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
	    webClient.getOptions().setPrintContentOnFailingStatusCode(false);
	    webClient.getOptions().setThrowExceptionOnScriptError(false);
	    webClient.getOptions().setMaxInMemory(64*1024*1024);
	    webClient.setAlertHandler(null);
	    webClient.setAjaxController(new NicelyResynchronizingAjaxController());
	    webClient.setCssErrorHandler(new SilentCssErrorHandler());
	    webClient.setIncorrectnessListener(new IncorrectnessListener() {
			@Override
			public void notify(String arg0, Object arg1) {}
	    });

	    HtmlPage x = webClient.getPage(ln);
	    HtmlForm form = x.getForms().get(0);
	    assert form.getAttribute("id").equals("logOnForm");
	    HtmlTextInput textField = form.getInputByName("j_username");
	    textField.setValueAttribute(uname);
	    HtmlPasswordInput textField2 = form.getInputByName("j_password");
	    textField2.setValueAttribute(passwd);
	    List<HtmlElement> buttons = form.getElementsByAttribute("button", "type", "submit");
	    HtmlButton button = (HtmlButton)buttons.get(0);
	    Page p = button.click();
	    if (p instanceof SgmlPage) {
		    SgmlPage o = (SgmlPage)p;
		    int rc = o.getWebResponse().getStatusCode();
		    webClient.getOptions().setJavaScriptEnabled(false);
		    webClient.getOptions().setCssEnabled(false);
		    if (rc>399) {
		    	IOUtils.copy(o.getWebResponse().getContentAsStream(), Files.newOutputStream(Cache.fs.getPath("Launchpad_login_error.html")));
			    throw new RuntimeException(String.format("Failed (rc=%d) when log on to %s, see Launchpad_login_error.html", rc, ln));
		    }
		    assert o instanceof XmlPage : o;
		    passwd = null;
		    return webClient;
	    } else { //if (p instanceof TextPage) {
	    	TextPage tp = (TextPage)p;
	    	throw new java.net.UnknownServiceException(tp.getContent()); 
	    }
	}
	public static java.util.Properties simpleJsonParse(String x) {
		if (x.charAt(0)=='{' && x.charAt(x.length()-1)=='}') x = x.substring(1, x.length()-1).trim();
		Pattern re = Pattern.compile("\"[^\"]*\"", Pattern.MULTILINE);
		Matcher m = re.matcher(x);
		java.util.Properties prop1 = new java.util.Properties();
		while (m.find()) {
			String name = x.substring(m.start()+1, m.end()-1).trim();
			m.find();
			prop1.put(name, x.substring(m.start()+1, m.end()-1).trim());
		}
		return prop1;
	}
	
	public static void main(String args[]) throws Exception {
		String x = new String(Files.readAllBytes(FileSystems.getDefault().getPath("dispatcher.json")), utf8).trim();
		System.out.println(simpleJsonParse(x));
		DirectoryStream<Path> ds = Files.newDirectoryStream(FileSystems.getDefault().getPath("."), "*_entryfacets.xml");
		Iterator<Path> it = ds.iterator();
		String s, ml;
		while (it.hasNext()) {
			Path p = it.next();
			com.sap.lpad.Entry en = JAXB.unmarshal(Files.newInputStream(p), com.sap.lpad.Entry.class);
			com.sap.lpad.Properties prop = en.getContent().getProperties();
			System.out.println(String.format("%s:\t%s%n%s", p, en.getId(), prop.getSapNotesKey()));
			ml = null;
			for (com.sap.lpad.Link l: en.getLink()) if (!l.getRel().equals("self")) {
				s = l.getTitle();
				System.out.println(String.format("%s %s  ", s, l.getInline().getFeed().getEntry()));
				for (com.sap.lpad.Entry en2: l.getInline().getFeed().getEntry()) {
					if ("Languages".equals(s)) ml = en2.getContent().getProperties().getLangMaster(); 
//					System.out.println(en2.getContent().getProperties());
				}
			}
			System.out.println(ml);
		}
	}
	void close() {
		if (wc!=null) wc.close();
	}

	/**
	 * {Online}
	 * @param cdb
	 * @param dba
	 * @throws IOException
	 * @throws SQLException
	 */
	boolean getNotes(NotesDB cdb, NotesDB dba, Path facets) throws IOException, SQLException {
		assert cache!=null && cdb!=null && dba!=null;
		assert !cdb.dba && dba.dba;
		assert !cdb.isClosed() && !dba.isClosed();
		System.out.println(dba.getNick());
		String nick = dba.getNick();
		Collection<String> areas = Area.nickToArea.get(nick);
		List<AZ> azs = cdb.getNotesCDB_byAreas(areas);
		List<AZ> ozs = dba.getNotesDBA(cdb.cat, cdb.prio);

		boolean needmore = false, e;
		for (AZ x: azs) { // every x.num occurs at `azs` once
			assert x.num>0 && x.mark>=0 && x.objid!=null && x.area!=0 && x.mprop==null;
			e = true;
//			com.sap.lpad.Entry en = null;
//			int av = 0, av2 = 0;
//			String al = "E", al2 = null;
			// many y.num may occurs at `ozs`. The unique key is num-version-language
			for (AZ y: ozs) if (x.num==y.num) {
				assert y.num>0 : y.num;
				assert y.mark>0 : y.mark;
				assert y.objid!=null : y.objid;
				assert y.area!=0 : y.area;
				assert y.mprop!=null : y.mprop;
				if (x.mark!=y.mark) { 	//TODO move to stat1
					assert x.mark==0 : x.num;
//					System.out.println(String.format("Note %s has to be turned to mark=%d", x.num, y.mark));
					cdb.setMark(x.num, y.mark);
					x.mark = y.mark;
				}
				if (!x.objid.equals(y.objid)) {	//TODO move to stat1
					assert x.objid.startsWith("Z") : x.num;
//					System.out.println(String.format("Note %s has to be turned to objid=%s", x.num, y.objid));
					cdb.setObjid(x.num, y.objid);
					x.objid = y.objid;
				}
				e = false;
			}
			if (!e) continue;
			System.out.println("Need to download note: " + x.num + " " + nick);
			if (wc==null) {
				int i = 10;
				while (i-->0 && wc==null) {
					try {
						wc = Launchpad.getLaunchpad(uname, prHost, prCred);
					} catch (UnknownServiceException se) {
				    	System.err.println("Unexpected answer: " + se.getMessage());
					}
				}
				if (wc==null) throw new RuntimeException("Cannot log on to LPAD");
			}
			Instant n = Instant.now();
			com.sap.lpad.Properties p = null;
			NoteRetrException nx = new NoteRetrException(x.num, 0, x.lang, x.mark);
			Path tmp = facets.resolve("tmp" + Instant.now().toEpochMilli());
			try {
				downloadEntry2(wc, nx, tmp, 3);
				downloadEntry2(wc, nx, tmp, 2);
//				dba.putA01(nx.en1);
				p = nx.en1.getContent().getProperties();
//				dba.putA02(Integer.parseInt(p.getSapNotesNumber()), p.getLanguage(), Integer.parseInt(p.getVersion()), n,  al, av, null, null);
//				dba.commit();
//				Path xd = facets.resolve(String.format("%010d_%s_%d.xml", x.num, al2, av2));
//				Files.move(tmp, xd);
			} catch (NoteRetrException nre) {
//				dba.putA02(x.num, nx.al, av, n, null, null, nre.rc, nre.errText);
//				dba.commit();
				Files.deleteIfExists(tmp);
//				return false;
//				if (nre.notreleased) {
//					dba.putA02(en, n, "E", 0, nre.rc, nre.errText);
//					System.out.println("\tNot released yet: " + x.num);
//				} else if (nre.internalerror) { 
//					dba.putA02(en, n, "E", 0, nre.rc, nre.errText);
//					System.out.println("\tInternal error: " + x.num);
//				} else {
//					dba.putA02(en, n, "E", 0, nre.rc, nre.errText);
//					System.out.println("\tUNKNOWN: " + x.num);
//				}
			}
		}
		return needmore;
	}

	void checkFS(List<AZ> azs, List<Integer> toDl) throws IOException {
		assert cache!=null;
		assert inbox!=null && Files.isDirectory(inbox);

		Iterator<Path> ds = Files.newDirectoryStream(inbox, "*.xml").iterator();
		System.out.println("scanned " + inbox);
		Set<Integer> toRem = new HashSet<Integer>();
		while (ds.hasNext()) {
			String s = ds.next().getFileName().toString().substring(0, 10);
			int i = Integer.parseInt(s);
			if (toDl.remove(new Integer(i))) toRem.add(i);
		}
		if (toRem.size()>0) {
			Iterator<AZ> it = azs.iterator();
			while (it.hasNext()) {
				if (toRem.contains(it.next().num)) it.remove();
			}
		}
	}

	List<AZ> getFS() throws IOException {
		List<AZ> azs = new ArrayList<AZ>(2000000);
		Iterator<Path> ds = Files.newDirectoryStream(this.cache.launchpad, "*.zip").iterator();
		while (ds.hasNext()) {
			Path p = ds.next();
			System.out.println(p.getFileName());
			ZipInputStream zis = new ZipInputStream(Files.newInputStream(p));
			ZipEntry ze = zis.getNextEntry();
			while (ze!=null) {
				AZ az = new AZ(ze.getName());
				azs.add(az);
				zis.closeEntry();
				ze = zis.getNextEntry();
			}
			zis.close();
		}
		return azs;
	}

	boolean dlNotes(List<AZ> azs, List<Integer> toDl, int j) throws IOException {
		System.out.println(String.format("%nMore last: %d/%d", toDl.size(), azs.size()));
		for (AZ x: azs) { // every x.num occurs at `azs` once
			if (!toDl.contains(x.num)) continue;
			if (--j==0) break;
			toDl.remove(new Integer(x.num));
			assert x.num>0 && x.mark>=0;
			if (wc==null) {
				int i = 10;
				while (i-->0 && wc==null) {
					try {
						wc = Launchpad.getLaunchpad(uname, prHost, prCred);
					} catch (UnknownServiceException se) {
				    	System.err.println("Unexpected answer: " + se.getMessage());
					}
				}
				if (wc==null) throw new RuntimeException("Cannot log on to LPAD");
			}
//			com.sap.lpad.Properties p = null;
//			com.sap.lpad.Entry en = null;
			Path tmp = Files.createTempFile(inbox, "tmp", "."+j);
			NoteRetrException nx = new NoteRetrException(x.num, 0, x.lang, x.mark);
			try {
				downloadEntry2(wc, nx, tmp, 2);
				downloadEntry2(wc, nx, tmp, 2);
				Path xd = inbox.resolve(String.format("%010d_%s_%d.xml", x.num, x.lang, nx.version));
				if (Files.isRegularFile(xd))
					break;
				else
					Files.move(tmp, xd);
				System.out.println(x.num + "\t" + x.area +"\tOK" + "\t(prio=" + x.prio + ")");
			} catch (NoteRetrException nre) {
				System.out.println(nx.step);
				System.out.println(nre.errText);
				if (nx.step==1 && 
					"In the context of Data Services an unknown internal server error occured".equals(nre.errText)) {
					nx.step = 2;
					downloadFeed(wc, nx, tmp, 3);
				}
				System.out.println(String.format("%010d %s%n%s", x.num, nre.errText, nre.url));
				Path xd = inbox.resolve(String.format("%010d_error.xml", x.num));
				if (Files.isRegularFile(xd)) 
					Files.deleteIfExists(tmp);
				else {
					try {
						Files.move(tmp, xd);
					} catch (java.nio.file.NoSuchFileException e) {
						System.err.println(e.getMessage());
					}
				}
			}
		}
		return toDl.size()>0;
	}

	/**
	 * There is "dispatcher.json" servlet which returns for given number: 
	 * {"alias": "CORR", "ui": "1", "number": "0000217675", "errorMessage": "Document is not released"}
	 * @param wc  		WebClient, successfully logged in
	 * @param number	Note number
	 * @param ma		Max attempts
	 * @return			Properties (alias, ui, number, errorMessage)
	 * @throws IOException
	 */
	static java.util.Properties getNoteJsonAttrs(WebClient wc, int number, int ma) throws IOException {
		assert wc!=null && number>0 && ma>0;
		URL u = new URL(String.format("https://launchpad.support.sap.com/applications/nnf/services/bsp/sap/support/lp/dispatcher.json?number=%d", number));
		int rc=-1, m = ma;
		WebResponse wr = null;
		java.util.Properties js = null;
		while (m-->0) {
			Page o = wc.getPage(u);
			wr = o.getWebResponse();
			assert wr!=null;
			rc = wr.getStatusCode();
			if (rc==200) {
				js = simpleJsonParse(wr.getContentAsString());
				break;
			}
		}
		if (rc!=200) {
			throw new IOException(String.format("Cannot retrieve %s: rc=%d (max attempts=%d)", u.toString(), rc, ma));
		}
		return js;
	}

	private static void downloadEntry2(WebClient wc, NoteRetrException nx, Path ph, int ma) throws IOException, NoteRetrException {
		assert nx.step==0 || (nx.step>0 && nx.mark!=0 && nx.version>0) : String.format("step%d mark%d version%d", nx.step, nx.mark, nx.version);
		WebResponse wr = null;
		if (nx.mark==0) {
    		nx.dispjson = getNoteJsonAttrs(wc, nx.number, 3); 
    		nx.parseAlias();
    		assert nx.mark!=0;
    	}
		if (nx.mark==NotesDB.SAP_ONE) {
			nx.errText = "SAPBusinessOne notes are not supported";
	    	throw nx;
		}
		if (nx.mark==NotesDB.SAP_INVALID) {
			nx.errText = "SAP_INVALID notes are not supported";
	    	throw nx;
		}
		List<URL> urls = nx.makeURL();
		nx.url = urls.remove(0);
		while (ma-->0) {
			Page o = wc.getPage(nx.url);
			wr = o.getWebResponse();
			nx.rc = wr.getStatusCode();
			if (nx.rc==200||nx.rc==400) break;
		}
		assert wr!=null;
		if (nx.rc>399) {
	    	IOUtils.copy(wr.getContentAsStream(), Files.newOutputStream(ph));
	    	nx.raiseError(ph);
	    	throw nx;
		} else {
			IOUtils.copy(wr.getContentAsStream(), Files.newOutputStream(ph));
			com.sap.lpad.Entry en = null;
			try {
				en = JAXB.unmarshal(Files.newInputStream(ph), com.sap.lpad.Entry.class);
			} catch (javax.xml.bind.DataBindingException e) {
				assert false : e.getMessage();
				return;
			}
			assert en!=null;
			switch (nx.step) {
				case 0: 
					nx.en0 = en; 
					nx.lang = en.getContent().getProperties().getLanguage().charAt(0);
					nx.version = Integer.parseInt(en.getContent().getProperties().getVersion());
					break;
				case 1: 
					nx.en1 = en; 
					break;
				default:
					assert false; 
			}
			nx.step++;
		}
	}

	private static void downloadFeed(WebClient wc, NoteRetrException nx, Path ph, int mb) throws IOException, NoteRetrException {
		assert nx.mark!=0 && nx.version>0 : String.format("step%d mark%d version%d", nx.step, nx.mark, nx.version);
		WebResponse wr = null;
		List<URL> urls = nx.makeURL();
		System.out.println(urls);
		for (URL u: urls) {
			nx.url = u;
			int ma = mb;
			while (ma-->0) {
				Page o = wc.getPage(nx.url);
				wr = o.getWebResponse();
				nx.rc = wr.getStatusCode();
				if (nx.rc==200||nx.rc==400) break;
			}
			assert wr!=null;
			if (nx.rc>399) {
		    	IOUtils.copy(wr.getContentAsStream(), Files.newOutputStream(ph));
			} else {
				IOUtils.copy(wr.getContentAsStream(), Files.newOutputStream(ph));
				com.sap.lpad.Feed f = null;
				try {
					f = JAXB.unmarshal(Files.newInputStream(ph), com.sap.lpad.Feed.class);
					System.out.println(f);
				} catch (javax.xml.bind.DataBindingException e) {
					assert false : e.getMessage();
					return;
				}
			}
		}
	}
	
//	private static com.sap.lpad.Entry downloadEntry2(WebClient wc, com.sap.lpad.Entry en0, int num, String lang, int ver, int mark, Path ph) 
//			throws IOException, NoteRetrException {
//    	assert wc!=null : wc;
//    	assert num>0 : num;
//    	assert ph!=null : ph;
//		URL u = null;
//		int ma=3, rc=0;
//		Page o = null;
//		WebResponse wr = null;
//
//		if (mark==0) {
//    		u = new URL(String.format("https://launchpad.support.sap.com/applications/nnf/services/bsp/sap/support/lp/dispatcher.json?number=%d", num));
//    		while (ma-->0) {
//    			o = wc.getPage(u);
//    			wr = o.getWebResponse();
//    			rc = wr.getStatusCode();
//    			if (rc==200||rc==400) break;
//    		}
//    		assert wr!=null;
//    		java.util.Properties js = simpleJsonParse(wr.getContentAsString());
//    		String alias = js.getProperty("alias");
//    		if (alias==null) {
//    			NoteRetrException ne = new NoteRetrException(wr.getContentAsString(), u, rc);
//    	    	throw ne;
//    		}
//    		assert alias!=null : num + "\n" + wr.getContentAsString() + "\n" + u;
//    		switch (alias) {
//    		case "CORR" : mark = NotesDB.SAP_NOTE; break;
//    		case "SECURITY": mark = NotesDB.SAP_SECNOTE; break;
//    		case "KBA" : mark = NotesDB.SAP_KBA; break;
//    		case "OTHER" : mark = NotesDB.SAP_ONE; break;
//    		case "STAND" : mark = NotesDB.SAP_STAND; break;
//    		default: 
//    			NoteRetrException ne = new NoteRetrException("Unknown alias:" + alias, u, 200);
//    	    	throw ne;
//    		}
//    	}
//		if (mark==NotesDB.SAP_ONE) {
//			NoteRetrException ne = new NoteRetrException("SAPBusinessOne", u, 200);
//	    	throw ne;
//		}
//    	//TODO 0002247644
////    	assert mark>=NotesDB.SAP_KBA && mark<NotesDB.SAP_STAND : mark;
//		String b = String.format("https://launchpad.support.sap.com/services/odata/svt/%s/TrunkSet(SapNotesNumber='%010d',Version='%d',Language='%s')",
//    		mark==NotesDB.SAP_KBA ? "snogwskba" : mark==NotesDB.SAP_SECNOTE ? "snogwssecurity" : "snogwscorr", num, ver, lang);
//		com.sap.lpad.Entry en;
//		if (en0==null) {
//			u = new URL(b + "?$expand=Languages");
//		} else {
//			StringJoiner facets = new StringJoiner(",");
//			// TODO for security notes VersionInfo causes error
//			// https://launchpad.support.sap.com/services/odata/svt/snogwssecurity/TrunkSet(SapNotesNumber='0001232259',Version='5',Language='E')?$expand=RefBy,RefTo,CorrIns,Patch,Sp,OtherCom,SoftCom,Attach,LongText,VersionInfo,Languages,SideCau,SideSol 
//			for (com.sap.lpad.Link l: en0.getLink()) if (!"self".equals(l.getRel()) ) 
//				if (!"VersionInfo".equals(l.getTitle())) 
//					facets.add(l.getTitle());
//			u = new URL(b + "?$expand=" + facets);
//		}
//		ma = 3;
//		while (ma-->0) {
//			o = wc.getPage(u);
//			wr = o.getWebResponse();
//			rc = wr.getStatusCode();
//			if (rc==200||rc==400) break;
//		}
//		assert wr!=null;
//		if (rc>399) {
//	    	IOUtils.copy(wr.getContentAsStream(), Files.newOutputStream(ph));
//	    	NoteRetrException ne = new NoteRetrException(ph, u, rc);
//	    	throw ne;
//		} else {
//			IOUtils.copy(wr.getContentAsStream(), Files.newOutputStream(ph));
//			try {
//				en = JAXB.unmarshal(Files.newInputStream(ph), com.sap.lpad.Entry.class);
//			} catch (javax.xml.bind.DataBindingException e) {
//				NoteRetrException ne = new NoteRetrException(e.getMessage(), u, rc);
//				throw ne;
//			}
//		}
//		return en;
//	}	

	private static com.sap.lpad.Entry downloadEntry3(WebClient wc, com.sap.lpad.Entry en0, int num, String lang, int ver, int mark, Path ph) 
			throws IOException, NoteRetrException {
    	assert wc!=null : wc;
    	assert num>0 : num;
    	assert ph!=null : ph;
    	assert mark!=0 : mark;
    	assert ver>0 : ver;
    	assert en0!=null : en0;
		URL u = null;
		int ma=3, rc=0;
		Page o = null;
		WebResponse wr = null;
		String b = String.format("https://launchpad.support.sap.com/services/odata/svt/%s/TrunkSet(SapNotesNumber='%010d',Version='%d',Language='%s')",
	    		mark==NotesDB.SAP_KBA ? "snogwskba" : mark==NotesDB.SAP_SECNOTE ? "snogwssecurity" : "snogwscorr", num, ver, lang);
		List<String> facets = new ArrayList<String>();
		for (com.sap.lpad.Link l: en0.getLink()) if (!"self".equals(l.getRel()) ) 
			if (!"VersionInfo".equals(l.getTitle())) 
				facets.add(l.getTitle());
		
		return null;
	}
	
	/**
	 * checks tmpdir for previously cached notes (zip-archives)
	 * @param dba <area>.db for one nick
	 * @param debug  true if to store intermediate parsing objects
	 * @throws IOException
	 * @throws SQLException
	 */
	void cacheTmpdir(NotesDB dba, boolean debug) throws IOException, SQLException {
		assert dbas!=null : "call areaList(cdb) first";
		Path tmpdir = dba.pathdb.resolve("../tmp/").normalize();
		Path dbgdir = debug ? dba.pathdb.resolve("../debug/").normalize() : null;
		System.out.println(String.format("%s, tmp at %s", dba.getNick(), tmpdir));
		DirectoryStream<Path> ex = Files.newDirectoryStream(tmpdir, "*.zip");
		Iterator<Path> it = ex.iterator();
		int prcOK = 0, prcER = 0;
		NoteA na;
		Map<String,Set<String>> nntg = new HashMap<String,Set<String>>();
		while (it.hasNext()) {
			Path p = it.next();	assert p.getFileName()!=null;
			
			na = new NoteA(p.getFileName().toString());
			ZipInputStream zis = new ZipInputStream(Files.newInputStream(p), utf8);
			ZipEntry ze = zis.getNextEntry();
			if (ze!=null) {
				System.out.print(p.getFileName().toString());
				na.parseNoteDownloaded(zis, dbgdir);
				for (Map.Entry<String, String> x: na.ntg.entrySet()) {
					Set<String> ss = nntg.get(x.getKey());
					if (ss==null) ss = new HashSet<String>();
					ss.add(x.getValue());
					nntg.put(x.getKey(), ss);
				}
				System.out.println("\t" + na);
				prcOK++;
			} else {
				System.err.println("Possibly KBA: " + na.num);
				prcER++;
			}
		}
		System.out.println(String.format("Processed OK: %d, ER1: %d", prcOK, prcER));
		for (Map.Entry<String, Set<String>> x: nntg.entrySet()) {
			System.out.println("*" + x.getKey());
			for (String s: x.getValue()) {
				System.out.print(s+" ");
			}
			System.out.println();
		}
	}
}

/*
CWBCICATTR           Атрибуты руководства по корректуре установленные кли
CWBCICONFIRMLOC      Correction Instruction: Confirmation Non-Transportab
CWBCIDATA            Сжатые данные к руководствам по корректуре
CWBCIDATAOBJ         Сжатые данные к руководствам по корректуре
CWBCIDPNDC           Зависимости руководств по корректуре
CWBCIFIXED           Руководство по корректуре, выполненное поср.события
CWBCIHEAD            Данные заголовка руководства по корректуре
CWBCIINVLD           Интервалы версии, для к. не действует рук-во по корр
CWBCIOBJ             Список объектов руководства по корректуре с TADIR-кл
CWBCIVALID           Таблица области действия для руководств по корректур
CWBCMDEV             События поставки
CWBCMLAST            Компонент ПО - последнее обновление
CWBCMPNT             Компоненты ПО
CWBCMTEXT            Компоненты программного обеспечения - краткий текст
CWBDEEQUIV           Эквивалентные результаты поставки (a содержится в b)
CWBDEHEAD            Событие поставки (версия/горячий пакет)
CWBDEPRDC            Предшественник события поставки
CWBDEPRDCV           Отношение последовательности с треками поставки
CWBDETRACK           Трек поставки
CWBMODILOG           Журнал модификаций клиента в объектах СР
CWBNTCI              Присвоение указания руководству по корректуре
CWBNTCOMP            Ракурс для встроенных указаний и компонента указания
CWBNTCONT            Контейнер данных для данных версии в SAP-ноте
CWBNTCUST            Атрибуты клиента к указанию 
CWBNTDATA            Сжатые данные к OSS-указаниям
CWBNTFIXED           Указание выполненное с помощью события поставки
CWBNTGATTR           Таблица для любых атрибутов указания
CWBNTHEAD            Таблица загоолвка для OSS-указаний в системе клиента
CWBNTLOG             Присвоение журнальному файлу
CWBNTMSG             SAP-указания, журнал сообщений
CWBNTSTATT           Тексты: статус обработки для указаний
CWBNTSTATV           Постоянные значения для статуса обработки
CWBNTSTXT            Краткий текст указания
CWBNTVALID           Таблица областей действия для указаний
CWBPRSTATT           Тексты статуса инсталляции
CWBPRSTATV           Статус инсталляции для указаний
CWBRFCCACH           Кэш для данных запроса из дистанционной системы
CWBRFCUSR            Overwrite RFC Connection for Certain Users
CWBVDBSCFG           Тест внед. ноты: конфиг. для системы БД версии

*/