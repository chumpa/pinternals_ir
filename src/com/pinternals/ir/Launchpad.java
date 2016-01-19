package com.pinternals.ir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import javafx.util.Pair;

import javax.xml.bind.JAXB;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.xml.XmlPage;
import com.sap.Entry;
import com.sap.Properties;

public class Launchpad {
	static final Charset utf8 = Charset.forName("UTF-8");
	Path path, arealist;
	Map<String,Set<Path>> mp = null;
	Map<Path,Set<String>> pm = null;
	Map<Path,NotesDB> pn = null;

	Launchpad (Path o) throws IOException {
		this.path = o;
		arealist = Cache.fs.getPath(path.toString(), "arealist.txt");
		if (!Files.isRegularFile(arealist)) Files.createFile(arealist);
	}
	void z2(Cache cache, NotesDB db) throws IOException, SQLException {
		List<Pair<String,String>> ap = db.getAreas();
		List<String> ar = new LinkedList<String>(), ax = new LinkedList<String>();
		Scanner a = new Scanner(Files.newBufferedReader(arealist, utf8));
		BufferedWriter bw;
		mp = new HashMap<String,Set<Path>>();
		pm = new HashMap<Path,Set<String>>();
		pn = new HashMap<Path,NotesDB>();
		
		while(a.hasNextLine()) {
			String z[] = a.nextLine().split("\t");
			Path p = Cache.fs.getPath(path.toString(), z[0]), q = Cache.fs.getPath(path.toString(), z[0], "areas.txt");
			Path dx = Cache.fs.getPath(path.toString(), z[0], z[0]+".db");
			ax.clear();
			if (!Files.isDirectory(p)) Files.createDirectory(p);
			if (!Files.isRegularFile(dx)) {
				pn.put(p, NotesDB.initDBA(dx));
			} else
				pn.put(p, NotesDB.openDBA(dx));
			List<Pattern> lp = new LinkedList<Pattern>();
			for (int i=1; i<z.length; i++) 
				lp.add(Pattern.compile(z[i]));

			for (Pair<String,String> xx: ap) {
				boolean g = false;
				String x = xx.getKey();
				for (Pattern pt: lp)
					g = g || pt.matcher(x).matches();
				if (g) {
					Set<Path> sp = mp.get(x);
					if (sp==null) sp = new HashSet<Path>();
					Set<String> ss = pm.get(p);
					if (ss==null) ss = new HashSet<String>();
					sp.add(p);
					ss.add(x);
					mp.put(x, sp);
					pm.put(p, ss);
					if (!ax.contains(x)) ax.add(x);
				}
			}
			ar.addAll(ax);
			bw = Files.newBufferedWriter(q, utf8);
			for (String x: ax) {
				bw.write(x + "\n");
			}
			bw.close();
		}
		a.close();

		Path p = Cache.fs.getPath(path.toString(), "Zzz");
		bw = Files.newBufferedWriter(p, utf8);
		for (Pair<String,String> xx: ap) {
			String x = xx.getKey(), d = xx.getValue();
			if (d!=null)
				bw.write(x+"\t" + d + "\t" + ar.contains(x) + "\n");
			else 
				bw.write(x+"\t" + ar.contains(x) + "\n");
		}
		bw.close();
	}
	
	void z3(Cache cache, NotesDB db, String uname, HttpHost prHost, Credentials prCred) throws SQLException, IOException, ParseException {
		assert pm!=null && mp!=null;
		WebClient wc = null;
		for (Path par: pm.keySet()) {
			System.out.println(par.getFileName().toString());
			NotesDB dba = pn.get(par);
			for (String area: pm.get(par)) {
				System.out.println(area);
				List<AZ> azl = db.getZ3(area);
				dba.getZ3a(azl);
				for (AZ az: azl) {
					if (!az.foundA) {
						if (wc==null) wc = Launchpad.getLaunchpad(uname, prHost, prCred);
						URL u = getByScheme("E", az.num, 0);
						Path tmp = Cache.fs.getPath(par.toString(), String.format("%010d.xml", az.num));
				    	BufferedWriter w = Files.newBufferedWriter(tmp, NotesRetriever.utf8);
				    	Page o = wc.getPage(u);
				    	WebResponse wr = o.getWebResponse();
				    	int rc = wr.getStatusCode();
				    	w.write(wr.getContentAsString());
				    	w.close();
				    	if (rc>=200 && rc<=299) {
				    		System.out.println(az.num);
				    		Entry en = JAXB.unmarshal(tmp.toFile(), Entry.class);
				    		System.out.println(en.getContent().getProperties().getSapNotesKey());
				    		dba.put(en.getContent().getProperties(), Instant.now());
				    	} else {
				    		System.err.println(rc);
				    	}
					}
				}
				System.out.println(azl);
			}
			dba.close();
		}
		if (wc!=null) wc.close();
		return;
	}
		

	static URL getByScheme(String lang, int num, int ver) throws MalformedURLException {
		String s = String.format("https://launchpad.support.sap.com/services/odata/svt/snogwscorr"
			+ "/TrunkSet(SapNotesNumber='%010d',Version='%d',Language='%s')", num, ver, lang);
//			+ "?$expand=LongText,RefBy,RefTo,Languages", num, ver, lang);
		//SoftCom -- плохо
//			+ "?$expand=LongText,SoftCom,RefBy,RefTo,Sp,Patch,Attach,CorrIns,SideSol,SideCau,Languages", num, ver, lang);
    	URL u = new URL(s);
    	return u;
	}

	public static WebClient getLaunchpad(String suname, HttpHost prHost, Credentials prCred) throws IOException {
		URL ln = new URL("https://launchpad.support.sap.com/services/odata/svt/snogwscorr/");
		WebClient webClient = null;
		assert suname != null;
		
        if (prHost!=null)
        	webClient = new WebClient(BrowserVersion.CHROME, prHost.getHostName(), prHost.getPort());
        else
        	webClient = new WebClient(BrowserVersion.CHROME);

        String passwd = Utils.getPasswd(suname);
        CredentialsProvider cp = webClient.getCredentialsProvider();
        if (prHost!=null) 
        	cp.setCredentials(new AuthScope(prHost), prCred);
        
        UsernamePasswordCredentials suser = new UsernamePasswordCredentials(suname, passwd);
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
	    textField.setValueAttribute(suname);
	    HtmlPasswordInput textField2 = form.getInputByName("j_password");
	    textField2.setValueAttribute(passwd);
	    List<HtmlElement> buttons = form.getElementsByAttribute("button", "type", "submit");
	    HtmlButton button = (HtmlButton)buttons.get(0);
	    Object o = button.click();
	    assert o instanceof XmlPage;
	    passwd = null;
	    webClient.getOptions().setJavaScriptEnabled(false);
	    webClient.getOptions().setCssEnabled(false);
	    return webClient;
	}
	
	public static void main(String args[]) throws Exception {
		WebClient wc = null;
	    Instant th = Instant.now(), t2 = th, t3;
	    int nums[] = new int[]{};
	    Path p;
	    for (int num: nums) {
	    	if (wc==null) wc = Launchpad.getLaunchpad("s0000000000", null, null);
	    	p = Cache.fs.getPath("tmp", String.format("%010d.xml", num));
	    	BufferedWriter w = Files.newBufferedWriter(p, NotesRetriever.utf8);
	    	URL u = getByScheme("E", num, 0);
	    	System.out.println(u);
	    	Page o = wc.getPage(u);
	    	WebResponse wr = o.getWebResponse();
	    	int rc = wr.getStatusCode();
	    	System.out.println(o);
	    	if (o instanceof XmlPage) {
//	    		XmlPage xm = (XmlPage)o; 
		    	w.write(wr.getContentAsString());
		    	w.flush();
		    	w.close();
		    	if (rc>=200 && rc<=299) {
		    		Entry en = JAXB.unmarshal(p.toFile(), Entry.class);
		    		System.out.println(en.getContent().getProperties().getType());
		    	} else {
		    		System.err.println(wr.getStatusMessage());
		    	}
	    	} else
	    		throw new RuntimeException(o.toString());
	    	t3 = Instant.now();
	    	t2 = t3;
	    }
	    System.out.println(String.format("\nAverage is %s / %d", Duration.between(t2, th), nums.length));
	    if (wc!=null) wc.close();
	    p = Cache.fs.getPath("tmp", "0001381198.xml");
	    Entry en = JAXB.unmarshal(p.toFile(), Entry.class);
	    Properties r = en.getContent().getProperties();
	    System.out.println(r.getVersion());
	    String x = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
	    System.out.println(x);
	    x = Instant.now().toString();
	    System.out.println(x);
	}

}


// https://smpdl.sap-ag.de/~swdc/012002523100020457252015E/CDLABEL.htm?_ACTION=CONTENT_INFO

