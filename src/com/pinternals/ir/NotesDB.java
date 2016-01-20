package com.pinternals.ir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

import com.sap.JaxbNote;
import com.sap.Properties;
import com.sap.Snotes;

enum EPrio {
	Recommend, Normal, HotNews, CorrLow, CorrMedium, CorrHigh;
	long db = 0;
	static Map<String,EPrio> qq = new HashMap<String,EPrio>();
	static void put(String human, EPrio e, long db) {
		assert e.db==0 || e.db==db : "Already has db: " + db + "/" + e.db;
		e.db = db;
		qq.put(human, e);
	}
	static EPrio findHuman(String human) {
		return qq.get(human);
	}
}

enum ECat {
	Default,
	InfoFAQ, InfoAnnounceLegal, InfoConsulting, InfoHOWTO, InfoInstallation,
	InfoReleasePlanning,
	InfoUpgrade,
	InfoHelpErrorAnalysis,
	InfoWorkaround,
	InfoCorrectionLegal,
	BugFiled,
	Customizing,
	LegalChange,
	Modification,
	Performance,
	DevelopmentAdvance,
	DevelopmentSpecial,
	Problem,
	ProductEnhancement,
	ErrorExternal,
	ErrorProgram,
	ErrorTranslation,
	ErrorDocumentation;
	long db = 0;
	static Map<String,ECat> qq = new HashMap<String,ECat>();
	static void put(String human, ECat c, long db) {
		assert c.db==0 || c.db==db : "Already has db: " + db + "/" + c.db;
		c.db = db;
		qq.put(human, c);
	}
	static ECat findHuman(String human) {
		return qq.get(human);
	}
}

class Area {
	protected static Map<String,Integer> areaToCode = new HashMap<String,Integer>();
	protected static Map<String,String> areaToDescr = new HashMap<String,String>();
	private static Set<String> zz2 = new HashSet<String>();
	private static PreparedStatement aa02get, aa02put, aa02upd;
	private static boolean aa02 = false;
	static void init(NotesDB db) throws SQLException {//PreparedStatement aa02ge, PreparedStatement aa02pu, PreparedStatement aa02up) {
		aa02get = db.sql("02appareaget");
		aa02put = db.sqlUpd("02appareaput");
		aa02upd = db.sqlUpd("02appareaupd");
	}
	static void reload() throws SQLException {
		ResultSet rs = aa02get.executeQuery();
		areaToCode.clear();
		areaToDescr.clear();
		zz2.clear();
		while (rs.next()) {
			areaToCode.put(rs.getString(1), rs.getInt(2));
			areaToDescr.put(rs.getString(1), rs.getString(3));
		}
		rs.close();
	}
	static void handleSupport(String xa) throws SQLException {
		if (!areaToCode.containsKey(xa) && !zz2.contains(xa)) {
			aa02put.setString(1, xa);
			aa02put.setString(2, null);	// descr is not here
			aa02put.addBatch();
			zz2.add(xa);
		}
	}
	static void updateSupport() throws SQLException {
		if (zz2.size()>0) {
			int ia[] = aa02put.executeBatch();
			assert zz2.size()==ia.length;
			aa02put.getConnection().commit();
			aa02put.clearBatch();
			reload();
		}
		assert zz2.size()==0;
	}
	static Integer getCode(String a) {
		return areaToCode.get(a);
	}
	static void handleLaunchpad(String xa, String descr) throws SQLException {
		boolean b = areaToCode.containsKey(xa);
		String sd = areaToDescr.get(xa); 
		if (!b && !zz2.contains(xa)) {
			aa02put.setString(1, xa);
			aa02put.setString(2, descr);
			aa02put.addBatch();
			zz2.add(xa);
		} else if (b && !descr.equals(sd)) {
			aa02upd.setString(1,xa);
			aa02upd.setString(2,descr);
			aa02upd.addBatch();
			areaToDescr.put(xa, descr);
			aa02 = true;
		}
	}
	static void updateLaunchpad() throws SQLException {
		boolean x = aa02;
		int ia[];
		if (zz2.size()>0) {
			ia = aa02put.executeBatch();
			assert zz2.size()==ia.length;
			x = true;
		}
		if (aa02) {
			ia = aa02upd.executeBatch();
			assert ia.length>0;
			aa02 = false;
		}
		if (x) {
			aa02put.getConnection().commit();
			aa02put.clearBatch();
			aa02upd.clearBatch();
			reload();
		}
		assert zz2.size()==0 && !aa02;
	}
}

public class NotesDB {
	private static final int SAP_KBA=1, SAP_UNKNOWNYET=0;
	private Connection con = null;
	private int eu;	// for asserts
	private static ResourceBundle ddlrb = ResourceBundle.getBundle("com/pinternals/ir/ddl");
	private static ResourceBundle sqlrb = ResourceBundle.getBundle("com/pinternals/ir/sql");

	private static String ddl(String code) {
		return ddlrb.getString(code);
	}
	protected PreparedStatement sql(String code) throws SQLException {
		return con.prepareStatement(sqlrb.getString(code), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
	}
	protected PreparedStatement sqlUpd(String code) throws SQLException {
		return con.prepareStatement(sqlrb.getString(code), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
	}

	static void initDB(Path db) throws SQLException {
		NotesDB ndb = new NotesDB(db, false);

		for (String s: ddlrb.keySet()) if (s.startsWith("0")) {
			System.out.println(s);
			PreparedStatement ps = ndb.con.prepareStatement(ddl(s));
			ps.execute();
		}
		ndb.con.commit();

		for (String s: sqlrb.keySet()) {
			System.out.print(s + " ");
			ndb.sql(s);	// is statement compilable ?
		}
	}
	
	static NotesDB initDBA(Path db) throws SQLException {
		NotesDB ndb = new NotesDB(db, false);
		for (String s: ddlrb.keySet()) if (s.startsWith("a")) 
			ndb.con.prepareStatement(ddl(s)).execute();
		ndb.con.commit();
		return ndb;
	}	

	static NotesDB openDBA(Path db) throws SQLException {
		NotesDB ndb = new NotesDB(db, false);
		ndb.con.commit();
		return ndb;
	}	
	
	NotesDB(Path file, boolean checksql) throws SQLException {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException c) {
			throw new SQLException("Cannot load sqlite JDBC driver");
		}
		con = java.sql.DriverManager.getConnection("jdbc:sqlite:" + file.toFile());
		con.setAutoCommit(false);
		if (checksql) for (String s: sqlrb.keySet()) sql(s);	// is statement compilable ?
	}

	void close() throws SQLException {
		con.commit();
		con.close();
	}

	private static void pairToMap3(PreparedStatement ps, Map<String,Integer> m) throws SQLException {
		ResultSet r = ps.executeQuery();
		m.clear();
		while (r.next()) {
			m.put(r.getString(1), r.getInt(2));
		}
		r.close();
	}	

	Map<String,String> w44() throws SQLException {
		PreparedStatement fcget = sql("009a");
		ResultSet rs = fcget.executeQuery();
		Map<String,String> a = new HashMap<String,String>(500);
		while (rs.next()) {
			a.put(rs.getString(1), rs.getString(2));
		}
		rs.close();
		return a;
	}

	private static final int bsmaxnotes = 3*1024*1024;
	private BitSet bsnums = null, bs; // note numbers: 0 for absence, 1 for presence
	Map<String,BitSet> bstitlang = null; //;
	private PreparedStatement zzz2=null, zy2=null, zy3=null, fcupd=null, fcins=null;
	Map<String,Integer> prio = new HashMap<String,Integer>();
	Map<String,Integer> cat = new HashMap<String,Integer>();
	Map<String,Integer> types = new HashMap<String,Integer>();
	Set<String> zz2 = new HashSet<String>();
	void walk22(Snotes sn, boolean isnew, String fname, String lastmod) throws SQLException, IOException {
		if (sn==null) {
			bsnums = null;
			bstitlang = null;
			return;
		} else if (bsnums==null) {
			bsnums = new BitSet(bsmaxnotes);
			bstitlang = new HashMap<String,BitSet>();
			bstitlang.put("E", new BitSet(bsmaxnotes));
			bstitlang.put("D", new BitSet(bsmaxnotes));
			bstitlang.put("J", new BitSet(bsmaxnotes));

			Area.init(this);
			Area.reload();

			zzz2 = sql("zz2");
			zy3 = sql("zy3");
			zy2 = sql("zy2");
			fcupd = sqlUpd("009fcupd");
			fcins = sqlUpd("009fcins");
		} 
		pairToMap3(sql("01prioget2"), prio);
		pairToMap3(sql("01catget2"), cat);
		ResultSet rs = sql("zzz").executeQuery();
		while (rs.next()) bsnums.set(rs.getInt(1));
		
		for (String lang: bstitlang.keySet()) {
			zy2.setString(1, lang);
			rs = zy2.executeQuery();
			bs = bstitlang.get(lang);
			while (rs.next()) bs.set(rs.getInt(1));
		}
		for (JaxbNote x: sn.getNS()) 
			Area.handleSupport(x.getA());
		Area.updateSupport();
		for (JaxbNote x: sn.getNS()) {
			String xa = x.getA(), xp = x.getP(), xc = x.getC(), xo = x.getO(), xl = x.getL(), xm = x.getM(), xcon = x.getContent();
			int num = x.getN();
			assert num<bsmaxnotes : String.format("Notes number %d have to be lt %d, num, bsmaxnotes");
			Integer pr = prio.get(xp);
			Integer ca = cat.get(xc);
			if (pr==null) System.err.println(String.format("Note #%d, priority unknown: %s %s", num, xp, StringEscapeUtils.escapeJava(xp) ));
			if (ca==null) System.err.println(String.format("Note #%d, category unknown: %s %s", num, xc, StringEscapeUtils.escapeJava(xc) )); //x.n + "\tCategory unknown: " + x.c);
			assert (pr!=null && ca!=null);
			if (xo==null) {
				xo = "Z" + num;
			}
			assert xo!=null;
			Integer area = Area.getCode(xa);
			assert area!=null : num + "\tArea unknown: " + xa;
			bs = bstitlang.get(xl);
			if (!bsnums.get(num)) {
				// note is completely new
				zzz2.setInt(1, num);
				zzz2.setInt(2, area);
				zzz2.setInt(3, ca);
				zzz2.setInt(4, pr);
				if (xm!=null && xm.equals("*"))
					zzz2.setInt(5, SAP_KBA);
				else if (xm==null)
					zzz2.setInt(5, SAP_UNKNOWNYET);
				else
					assert false : "Unknown mark: " + xm;
				zzz2.setString(6, xo);
				zzz2.addBatch();
				bsnums.set(num);
			}
			if (!bs.get(num)) {
				// for given language there is no title 
				zy3.setInt(1, num);
				zy3.setString(2, xl);
				zy3.setString(3, xcon);
				zy3.addBatch();
				bs.set(num);
			}
		} // for (JaxbNote x: sn.n) 
		zzz2.executeBatch();
		zzz2.clearBatch();
		zy3.executeBatch();
		zy3.clearBatch();
		con.commit();
		if (isnew) {
			fcins.setString(1, fname);
			fcins.setString(2, lastmod);
			eu = fcins.executeUpdate();
		} else {
			fcupd.setString(1, fname);
			fcupd.setString(2, lastmod);
			eu = fcupd.executeUpdate();
		}
		assert eu == 1;
		con.commit();
	} // void walk22(Iterator<Path> it);

	List<AZ> getZ3(String area) throws SQLException, ParseException {
		List<AZ> az = new ArrayList<AZ>(1000);
		PreparedStatement ps = sql("w07");
		ps.setString(1, area);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			int num = rs.getInt(1);
			String j = rs.getString(2);
			az.add(new AZ(num, j, area));
		}
		ps.close();
		return az;
	}
	void getZ3a(List<AZ> azl) throws SQLException {
		PreparedStatement ps = con.prepareStatement("select askdate,NotesKey,"
				+ "Title,Type,Version,Priority,Category,ReleasedOn,ComponentKey,ComponentText,Language from a01 where NotesNumber=?;");
		ResultSet rs;
		for (AZ az: azl) {
			ps.setInt(1, az.num);
			rs = ps.executeQuery();
			while (rs.next()) {
				com.sap.Properties p = new Properties();
				p.setSapNotesNumber(String.valueOf(az.num));
				p.setSapNotesKey(rs.getString(2));
				p.setTitle(rs.getString(3));
				p.setType(rs.getString(4));
				p.setVersion(rs.getString(5));
				p.setPriority(rs.getString(6));
				p.setCategory(rs.getString(7));
				p.setReleasedOn(rs.getString(8));
				p.setComponentKey(rs.getString(9));
				p.setComponentText(rs.getString(10));
				p.setLanguage(rs.getString(11));
				az.foundA(rs.getString(1), p);
			}
		}
	}

	void put(Properties p, Instant n) throws SQLException {
		PreparedStatement ps = con.prepareStatement("insert into a01(askdate,NotesNumber,NotesKey,"
			+ "Title,Type,Version,Priority,Category,ReleasedOn,ComponentKey,ComponentText,Language) values (?,?,?,?,?,?,?,?,?,?,?,?);");
		String askdate = n.toString();
		ps.setString(1, askdate);
		ps.setInt(2, Integer.valueOf(p.getSapNotesNumber()));
		ps.setString(3, p.getSapNotesKey());
		ps.setString(4, p.getTitle());
		ps.setString(5, p.getType());
		ps.setInt(6, Integer.valueOf(p.getVersion()));	// version
		ps.setString(7, p.getPriority()); // priority
		ps.setString(8, p.getCategory());
		ps.setString(9, p.getReleasedOn());
		ps.setString(10, p.getComponentKey());
		ps.setString(11, p.getComponentText());
		ps.setString(12, p.getLanguage());
		eu = ps.executeUpdate();
		assert eu == 1;
		con.commit();
	}

	void merge(NotesDB dba) throws SQLException {
		// dba == area db
		assert dba!=null && dba.con!=null && !dba.con.isClosed();
		PreparedStatement px = dba.con.prepareStatement("select NotesNumber,Version,NotesKey,"
				+ "Title,Type,Priority,Category,ReleasedOn,ComponentKey,ComponentText,Language,askdate from a01");
		PreparedStatement mr01 = sql("mr01"), mr02 = sqlUpd("mr02"), zzz2 = sqlUpd("zz2"), zy3 = sqlUpd("zy3");
		PreparedStatement mr05 = sqlUpd("mr05"), mr08 = sql("mr08"), mr09 = sqlUpd("mr09");
		ResultSet rx = px.executeQuery(), rs;
		pairToMap3(sql("01prioget2"), prio);
		pairToMap3(sql("01catget2"), cat);
		pairToMap3(sql("01types"), types);
		Area.init(this);
		Area.reload();
		
		// 1st -- areas handling and preparation   TODO rewrite to distinct
		while (rx.next()) 
			Area.handleLaunchpad(rx.getString(9), rx.getString(10));
		Area.updateLaunchpad();
		rx = px.executeQuery();

		bsnums = new BitSet(bsmaxnotes);
		bstitlang = new HashMap<String,BitSet>();
		bstitlang.put("E", new BitSet(bsmaxnotes));
		bstitlang.put("D", new BitSet(bsmaxnotes));
		bstitlang.put("J", new BitSet(bsmaxnotes));
		zy2 = sql("zy2");
		for (String lang: bstitlang.keySet()) {
			zy2.setString(1, lang);
			rs = zy2.executeQuery();
			bs = bstitlang.get(lang);
			while (rs.next()) bs.set(rs.getInt(1));
		}

		// 2nd -- notes handling
		while (rx.next()) {
			String ad, nk, title, type, pris, cas, relon, lang; 
			int num = rx.getInt(1), ver = rx.getInt(2), ac = Area.getCode(rx.getString(9)).intValue();
			nk = rx.getString(3);
			title = rx.getString(4);
			type = rx.getString(5);
			pris = rx.getString(6);
			cas = rx.getString(7);
			relon = rx.getString(8);
			lang = rx.getString(11);
			ad = rx.getString(12);
			
			mr01.setInt(1, num);
			rs = mr01.executeQuery();
			while (rs.next()) {
				bsnums.set(num);
				String objid = rs.getString(1);
				int ar = rs.getInt(2), ca = rs.getInt(3), pr = rs.getInt(4), m = rs.getInt(5);
				boolean b = ac!=ar;
				b = b || cat.get(cas)!=ca;
				b = b || prio.get(pris)!=pr;
				b = b || types.get(type)!=m;
				b = b || !objid.equals(nk);
				if (b) {
					mr02.setInt(1, num);
					mr02.setString(2, nk);
					mr02.setInt(3, ac);
					mr02.setInt(4, cat.get(cas));
					mr02.setInt(5, prio.get(pris));
					mr02.setInt(6, types.get(type));
					mr02.addBatch();
				}
			}
			rs.close();
			if (!bsnums.get(num)) {
				zzz2.setInt(1, num);
				zzz2.setInt(2, ac);
				zzz2.setInt(3, cat.get(cas));
				zzz2.setInt(4, prio.get(pris));
				zzz2.setInt(5, types.get(type));
				zzz2.setString(6, nk);
				zzz2.addBatch();
				bsnums.set(num);
			}
			if (!bstitlang.get(lang).get(num)) {
				zy3.setInt(1, num);
				zy3.setString(2, lang);
				zy3.setString(3, title);
				zy3.addBatch();
				bstitlang.get(lang).set(num);
				mr05.setInt(1,num);
				mr05.setString(2, lang);
				mr05.setInt(3, ver);
				mr05.setString(4, relon);
				mr05.setString(5, ad);
				mr05.addBatch();
			} else {
				mr08.setInt(1, num);
				mr08.setString(2, lang);
				rs = mr08.executeQuery();
				boolean b = false;
				while (rs.next()) {
					int ver2 = rs.getInt(1);
					String relon2 = rs.getString(2), ad2 = rs.getString(3);
					if (ver==ver2) {
						if (!ad2.equals(ad) || relon2.equals(relon)) {
							mr09.setInt(1, num);
							mr09.setString(2, lang);
							mr09.setInt(3, ver);
							mr09.setString(4, relon);
							mr09.setString(5, ad);
							mr09.addBatch();
						}
						b = true;
					}
				}
				rs.close();
				if (!b) { 
					mr05.setInt(1,num);
					mr05.setString(2, lang);
					mr05.setInt(3, ver);
					mr05.setString(4, relon);
					mr05.setString(5, ad);
					mr05.addBatch();
				}				
			}
		}
		px.close();
		int ia[] = zzz2.executeBatch();
		ia = mr02.executeBatch();
		ia = zy3.executeBatch();
		ia = mr05.executeBatch();
		ia = mr09.executeBatch();
		con.commit();
		System.out.println(ia);
	}
	
	List<Integer> arrrrrrrrrr(List<String> as, String pat) throws SQLException {
		List<Integer> ar = new ArrayList<Integer>(10000);
		PreparedStatement ps = sql("03areaunk");
		ps.setString(1, pat);
		ResultSet rs = ps.executeQuery();
		String p = null;
		while (as.size()>0 && rs.next()) {
			int num = rs.getInt(1);
			String t = rs.getString(2);
			if (p!=t && as.contains(t)) {
				System.out.println(num + "\t" + t);
				ar.add(num);
				as.remove(t);
				p = t;
			}
		}
		return ar;
	}
}
