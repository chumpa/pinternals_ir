package com.pinternals.ir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.bind.JAXB;

import org.apache.commons.io.input.CloseShieldInputStream;

import com.sap.nwa.SAPITSAMJ2EeClusterSoftwareComponentPartComponentElement;

class V {
	int thousand=0,major=0,minor=0,sp=0,patch=0;
	long date=0;
	String src = null;
	
	//1000.1.0.2.8.20151217042700
	//1000.7.50.1.2.20151126203500".length()==longv.length()			// sap.com format for many components
	//1000.710.710.14.1.20150821064042".length()==longv.length()		// sap.com.format for MDM
	//1000.0.0.0.0.1411713900154".length()==longv.length()	// custom code
	V (String longv) {
		assert longv!=null;
		src = longv;
		String p[] = longv.split("\\.");
		if (p.length==6 && "1000".equals(p[0])) {
			thousand = Integer.parseInt(p[0]);
			major = Integer.parseInt(p[1]);
			minor = Integer.parseInt(p[2]);
			sp = Integer.parseInt(p[3]);
			patch = Integer.parseInt(p[4]);
			date = Long.parseLong(p[5]);
		} else
			throw new RuntimeException("Unknown version format: " + longv);
	}
	public String toString() { 
		return src;
	}
	String formatName1(String name) {
		assert thousand>0 && major>0 : src; 
		return String.format("%s %d.%d", name, major, minor);
	}
	boolean eq(String version) {
		// version = 7.50
		//LMCFG │ LM CONFIGURATION 7.50                          │ 7.50                 │ BASIS 
		assert thousand>0 && major>0 : src;
		assert version!=null && !version.equals("") : version;
		String p[] = version.split("\\.");
		assert p.length<3 : version;
		int mj = Integer.parseInt(p[0]);
		if (p.length==1) return mj==major;
		int mn = Integer.parseInt(p[1]);
		return mj==major && mn==minor;
	}
}

class Sapk {
	String name, ver, devk;
	int patchlevel;
	Sapk(String s) {
		String r[] = s.split("\t");
		assert r.length>=4 : r.length+s;
		name=r[0];
		ver=r[1];
		patchlevel=Integer.parseInt(r[2]);
		devk=r[3];
	}
}
public class NWA {
	String sid = null;
	private boolean abap=false, java=false, mdm=false;
	
	private com.sap.nwa.SAPITSAMJ2EeClusterSoftwareComponentPartComponent comp1 = null;
	private com.sap.nwa.DevelopmentComponents comp2 = null;
	private List<Sapk> sapk = new ArrayList<Sapk>();

	public NWA (String z) {
		assert z!=null;
		String p[] = z.split(":");
		sid = p[0];
		assert sid!=null && sid.length()==3 && sid.toUpperCase().equals(sid) : sid;
		for (int i=1; i<p.length; i++) {
			abap = abap || "ABAP".equals(p[i]);
			java = java || "JAVA".equals(p[i]);
			mdm = mdm || "MDM".equals(p[i]);
		}
		assert abap || java || mdm;
		assert (mdm && !(abap && java)) || (!mdm && (abap||java)) : String.format("abap=%s java=%s mdm=%s", abap, java, mdm);
	}
	public NWA (String sid, boolean abap, boolean java, boolean mdm) {
		assert sid!=null && sid.length()==3 && sid.toUpperCase().equals(sid) : sid;
		assert abap || java || mdm;
		assert (mdm && !(abap && java)) || (!mdm && (abap||java)) : String.format("abap=%s java=%s mdm=%s", abap, java, mdm);
		this.sid = sid;
		this.abap = abap;
		this.java = java;
		this.mdm = mdm;
	}

	public void addSysInfo(Path p) throws IOException {
		assert !mdm;
		String kind = "unk";
		if (abap&&!java) 
			kind = "ABAP";
		else if (!abap&&java)
			kind = "JAVA";
		else
			throw new RuntimeException("to implement soon: dual-stack");

		switch(kind){
		case "JAVA":
			com.sap.nwa.SAPITSAMJ2EeClusterSoftwareComponentPartComponent x;
			com.sap.nwa.DevelopmentComponents y;
			x =	JAXB.unmarshal(p.toFile(), com.sap.nwa.SAPITSAMJ2EeClusterSoftwareComponentPartComponent.class);
			y = JAXB.unmarshal(p.toFile(), com.sap.nwa.DevelopmentComponents.class);
			if (x!=null && x.getSAPITSAMJ2EeClusterSoftwareComponentPartComponentElement()!=null 
					&& x.getSAPITSAMJ2EeClusterSoftwareComponentPartComponentElement().size()>0) comp1=x;
			else if (y!=null && y.getDevelopmentComponentsElement()!=null 
					&& y.getDevelopmentComponentsElement().size()>0) comp2 = y;
			break;
		case "ABAP":
			Iterator<String> it = Files.lines(p).iterator();
			while (it.hasNext()) sapk.add(new Sapk(it.next()));
			break;
		}
	}
	/**
	 * 
	 * @param cdb
	 * @param dba
	 * @throws SQLException
	 */
	void check(NotesDB cdb, NotesDB dba) throws SQLException {
		if (java && comp1!=null) 
			checkSCA(comp1, cdb, dba);
		else if (abap && sapk.size()>0)
			checkABAP(sapk, cdb, dba);
	}
	
	private static void checkABAP(List<Sapk> ar, NotesDB cdb, NotesDB dba) throws SQLException {
		assert ar!=null && ar.size()>0;
		for (Sapk a: ar) dba.getNotesBySAPK(a);
	}
	
	private static void checkSCA(com.sap.nwa.SAPITSAMJ2EeClusterSoftwareComponentPartComponent comp1, NotesDB cdb, NotesDB dba) throws SQLException {
		assert comp1!=null;
		Set<String> zz = new HashSet<String>();
		int i=0, j = comp1.getSAPITSAMJ2EeClusterSoftwareComponentPartComponentElement().size();
		Object[][] arr = new Object[3][j];
		for (SAPITSAMJ2EeClusterSoftwareComponentPartComponentElement x: comp1.getSAPITSAMJ2EeClusterSoftwareComponentPartComponentElement()) {
			V f = new V(x.getVersion()); // 1000.7.50.1.3.20151202223900
			String name = x.getName();	// LMCFG or SAP_BASIS
			if ("sap.com".equals(x.getVendor())) {
				zz.add(name);
				arr[0][i] = f;
				arr[1][i] = name;
				i++;
				assert i<=j;
			}
		}
		j = i;
		cdb.getSWCV(zz, arr, j);
		
		for (i=0; i<j; i++) {
			V f = (V)arr[0][i];
			String caption = (String)arr[2][i];
			Object o = dba.getNotesBySWCV(caption, f);
		}
	}

	/**
	 * Parses export content from SLD
	 * @param p incoming zip-file
	 * @return 
	 * @throws IOException
	 */
	static List<java.util.Properties> parseSLD(Path p) throws IOException {
		ZipInputStream zis = new ZipInputStream(Files.newInputStream(p));
		ZipEntry ze = zis.getNextEntry();
		List<java.util.Properties> swcvs = new ArrayList<java.util.Properties>(); 
		while (ze!=null) {
			if (!"manifest".equals(ze.getName())) {
				com.sap.sld.CIM cim = JAXB.unmarshal(new CloseShieldInputStream(zis), com.sap.sld.CIM.class);
//				System.out.println(ze.getName() + "\t" + cim);
				for (com.sap.sld.VALUEOBJECTWITHPATH v: cim.getDECLARATION().getDECLGROUPWITHPATH().getVALUEOBJECTWITHPATH()) {
					if (!"SAP_SoftwareComponent".equals(v.getINSTANCE().getCLASSNAME())) continue;
					java.util.Properties prop = new java.util.Properties();
					for (Object o: v.getINSTANCE().getQUALIFIEROrPROPERTYOrPROPERTYARRAY()) {
						assert o!=null && (o instanceof com.sap.sld.QUALIFIER 
							|| o instanceof com.sap.sld.PROPERTY
							|| o instanceof com.sap.sld.PROPERTYARRAY) : o;
						if (o instanceof com.sap.sld.PROPERTY) {
							String s, val;
							s = ((com.sap.sld.PROPERTY)o).getNAME();
							val = ((com.sap.sld.PROPERTY)o).getVALUE();
							assert s!=null : s;
							if ("Vendor,Name,Version,TechnologyType,Caption".contains(s) && val!=null)
								prop.put(s, val);
						}
					}
					swcvs.add(prop);
				}
			}
			zis.closeEntry();
			ze = zis.getNextEntry();
		}
		return swcvs;
	}
}
