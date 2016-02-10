package com.pinternals.ir;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import javax.xml.bind.JAXB;

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
	String formatName1(String name) {
		assert thousand>0 && major>0 : src; 
		return String.format("%s %d.%d", name, major, minor);
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
	
	void check(NotesDB dba) throws SQLException {
		assert comp1!=null;
		for (SAPITSAMJ2EeClusterSoftwareComponentPartComponentElement x: comp1.getSAPITSAMJ2EeClusterSoftwareComponentPartComponentElement()) {
//			String version = x.getVersion(); // like 1000.7.50.1.3.20151202223900
			V f = new V(x.getVersion());
//			String vendor = x.getVendor(); // sap.com for standard
//			String name = x.getName();	// LMCFG
			if ("sap.com".equals(x.getVendor())) {
//				System.out.println(String.format("%s %s %s", version, vendor, name));
				Object o = dba.getNotesBySWCV(x.getName(), f);
			}
		}
	}
}
