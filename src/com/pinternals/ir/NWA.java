package com.pinternals.ir;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

public class NWA {
	com.sap.nwa.SAPITSAMJ2EeClusterSoftwareComponentPartComponent comp1 = null;
	com.sap.nwa.DevelopmentComponents comp2 = null;
	public NWA (List<String> files) {
		for (String s: files) {
			Path p = FileSystems.getDefault().getPath(s);
			com.sap.nwa.SAPITSAMJ2EeClusterSoftwareComponentPartComponent x = 
					JAXB.unmarshal(p.toFile(), com.sap.nwa.SAPITSAMJ2EeClusterSoftwareComponentPartComponent.class);
			com.sap.nwa.DevelopmentComponents y = JAXB.unmarshal(p.toFile(), com.sap.nwa.DevelopmentComponents.class);
			if (x!=null && x.getSAPITSAMJ2EeClusterSoftwareComponentPartComponentElement()!=null && x.getSAPITSAMJ2EeClusterSoftwareComponentPartComponentElement().size()>0) 
				comp1=x;
			else if (y!=null && y.getDevelopmentComponentsElement()!=null && y.getDevelopmentComponentsElement().size()>0) 
				comp2 = y;
		}
	}
	
	void check(NotesDB cdb, NotesDB dba) throws SQLException {
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
		// select SWCV from 
		cdb.getSWCV(zz, arr, j);
		
		for (i=0; i<j; i++) {
			V f = (V)arr[0][i];
			String name = (String)arr[1][i];
			String caption = (String)arr[2][i];
//			System.out.println(String.format("%s\t%s\t%s", name, f, caption));
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
