package com.pinternals.ir;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;

import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

public class Swdc {
	static final Charset utf8 = Charset.forName("UTF-8");
	private Cache cache = null;
	private WebClient wc = null;
	private String uname; 
	private HttpHost prHost;
	private Credentials prCred;
	
	Swdc (Cache cache, String uname, HttpHost prHost, Credentials prCred) throws IOException {
		this.cache = cache;
		if (!Files.isRegularFile(cache.getLaunchpadArealist())) Files.createFile(cache.getLaunchpadArealist());
		this.uname = uname;
		this.prHost = prHost;
		this.prCred = prCred;
	}
	Swdc (Cache cache, WebClient wc) {
		assert wc!=null;
		this.cache = cache;
		this.wc = wc;
	}
	void test() throws IOException {
		assert cache!=null;
		if (wc==null) wc = getSwdc(uname, prHost, prCred);
		wc.setAjaxController(new AjaxController(){
			private static final long serialVersionUID = -864491138480223433L;
			@Override
		    public boolean processSynchron(HtmlPage page, WebRequest request, boolean async)
		    {
		        return true;
		    }
		});
	    wc.getOptions().setJavaScriptEnabled(true);
		URL u = new URL("https://service.sap.com/~form/handler?_APP=00200682500000001943&_EVENT=DISPHIER&HEADER=N&FUNCTIONBAR=Y&EVENT=TREE&TMPL=INTRO_SWDC_SP_N&V=MAINT&REFERER=MAINT_A-Z");
    	HtmlPage p = wc.getPage(u);
    	WebResponse wr = p.getWebResponse();
    	PrintWriter pw = null;
    	assert wr!=null;
    	HtmlAnchor a = p.getAnchorByText("SAP NETWEAVER");
    	HtmlPage q = a.click();
    	System.out.println("\n\n1>"+q);
    	a = q.getAnchorByText("SAP NETWEAVER 7.5");
    	assert a!=null;
    	p = a.click();
    	System.out.println("\n\n2>"+q);
    	pw = new PrintWriter(new File("zz/indexP.html"));
    	pw.println(p.getWebResponse().getContentAsString());
    	pw.flush();
    	pw = new PrintWriter(new File("zz/indexQ.html"));
    	pw.println(q.getWebResponse().getContentAsString());
    	pw.flush();
    	
//    	for (HtmlAnchor b: p.getAnchors()) {
//    		System.out.print("\n"+b+"\t" + b.getTextContent());
//    	}
//    	List<HtmlAnchor> aa = new ArrayList<HtmlAnchor>();
//    	System.out.println("\n\n3>"+p);
//    	for (HtmlAnchor b: p.getAnchors()) {
//    		if (b.getAttribute("href").startsWith("javascript:nextStep(")) aa.add(b);
////    		System.out.print("\n"+b+"\t" + b.getTextContent());
//    	}
    	HtmlAnchor b = q.getAnchorByText("Application Server Java");
    	HtmlPage hp = b.click();
		pw = new PrintWriter(new File("zz/Application Server Java.html"));
		pw.println(hp.getWebResponse().getContentAsString());
		pw.flush();
		pw = new PrintWriter(new File("zz/Application Server Java2.html"));
		pw.println(p.getWebResponse().getContentAsString());
		pw.flush();
//		HtmlElement t = hp.getFirstByXPath("//table[@summary='table content summary']/tbody");
//		if (t!=null) {
//			Iterator<DomElement> it = t.getChildElements().iterator();
//			while (it.hasNext()) {
//				DomElement de = it.next();
//				System.out.println(de);
//			} 
//		}
    	if (pw!=null) pw.close();
//    	System.out.println(p.getWebResponse().getContentAsString());
	}
	
	public static WebClient getSwdc(String suname, HttpHost prHost, Credentials prCred) throws IOException {
		URL ln = new URL("https://support.sap.com/swdc");	//bin/ids/login.smp.html?_=nc
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
	    webClient.getOptions().setThrowExceptionOnScriptError(false);
	    webClient.getOptions().setPrintContentOnFailingStatusCode(false);
	    webClient.getOptions().setMaxInMemory(64*1024*1024);
	    webClient.setAlertHandler(null);
	    webClient.setAjaxController(new NicelyResynchronizingAjaxController());
	    webClient.setCssErrorHandler(new SilentCssErrorHandler());
	    webClient.setIncorrectnessListener(new IncorrectnessListener() {
			@Override
			public void notify(String arg0, Object arg1) {}
	    });

	    webClient.getOptions().setJavaScriptEnabled(true);
	    HtmlPage x = webClient.getPage(ln);
	    HtmlAnchor href = x.getAnchorByText("Login");
	    assert href!=null;
	    x = href.click();
//	    System.out.println(x);
	    HtmlForm form = x.getForms().get(0);
	    assert form.getAttribute("id").equals("logOnForm");
	    HtmlTextInput textField = form.getInputByName("j_username");
	    textField.setValueAttribute(suname);
	    HtmlPasswordInput textField2 = form.getInputByName("j_password");
	    textField2.setValueAttribute(passwd);
	    List<HtmlElement> buttons = form.getElementsByAttribute("button", "type", "submit");
	    HtmlButton button = (HtmlButton)buttons.get(0);
	    Object o = button.click();
	    System.out.println(o);
//	    assert o instanceof XmlPage;
//	    passwd = null;
//	    webClient.getOptions().setJavaScriptEnabled(false);
//	    webClient.getOptions().setCssEnabled(false);
	    return webClient;
	}
	void close() {
		if (wc!=null) wc.close();
	}

}
