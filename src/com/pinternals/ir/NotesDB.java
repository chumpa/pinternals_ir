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

import javafx.util.Pair;

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

//class AppArea {
//	String text, area, parent;
//	static Map<String,Integer> qq = new HashMap<String,Integer>();
//	static void put(String area, int db, int parent) {
//		qq.put(area, db);
//	}
//	static int find(String area) {
//		return qq.get(area);
//	}
//	AppArea getParent() {
//		return null;
//	}
//}

public class NotesDB {
//	private static Charset utf8 = Charset.forName("UTF-8");
	Connection con = null;
	private int eu;	// for asserts
	private static ResourceBundle ddlrb = ResourceBundle.getBundle("com/pinternals/ir/ddl");
	private static ResourceBundle sqlrb = ResourceBundle.getBundle("com/pinternals/ir/sql");

	private static String ddl(String code) {
		return ddlrb.getString(code);
	}

	protected PreparedStatement sql(String code) throws SQLException {
		return con.prepareStatement(sqlrb.getString(code));
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
	private PreparedStatement aa02get, aa02put, zzz2, zy2, zy3, fcupd, fcins;
	Map<String,Integer> areaToCode = new HashMap<String,Integer>();
	Map<String,Integer> prio = new HashMap<String,Integer>();
	Map<String,Integer> cat = new HashMap<String,Integer>();
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
			aa02get = sql("02appareaget");
			aa02put = sql("02appareaput");
			zzz2 = sql("zz2");
			zy3 = sql("zy3");
			zy2 = sql("zy2");
			fcupd = sql("009fcupd");
			fcins = sql("009fcins");
		} 
		pairToMap3(sql("01prioget2"), prio);
		pairToMap3(sql("01catget2"), cat);
		pairToMap3(aa02get, areaToCode);
		ResultSet rs = sql("zzz").executeQuery();
		while (rs.next()) bsnums.set(rs.getInt(1));
		
		for (String lang: bstitlang.keySet()) {
			zy2.setString(1, lang);
			rs = zy2.executeQuery();
			bs = bstitlang.get(lang);
			while (rs.next()) bs.set(rs.getInt(1));
		}
		for (JaxbNote x: sn.getNS()) {
			String xa = x.getA();
			if (!areaToCode.containsKey(xa) && !zz2.contains(xa)) {
				aa02put.setString(1, xa);
				aa02put.addBatch();
				zz2.add(xa);
			}
		}
		if (zz2.size()>0) {
			int ia[] = aa02put.executeBatch();
			assert zz2.size()==ia.length;
			con.commit();
			aa02put.clearBatch();
			pairToMap3(aa02get, areaToCode);
			zz2.clear();
		}
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
			Integer area = areaToCode.get(xa);
			assert area!=null : num + "\tArea unknown: " + xa;
			bs = bstitlang.get(xl);
			if (!bsnums.get(num)) {
				// note is completely new
				zzz2.setInt(1, num);
				zzz2.setInt(2, area);
				zzz2.setInt(3, ca);
				zzz2.setInt(4, pr);
				if (xm!=null && xm.equals("*"))
					zzz2.setInt(5, 2);
				else if (xm==null)
					zzz2.setInt(5, 1);
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

	List<Pair<String,String>> getAreas() throws SQLException {
		List<Pair<String,String>> a = new ArrayList<Pair<String,String>>(25000);
		ResultSet rs = sql("02apparea2").executeQuery();
		while (rs.next()) { 
			a.add(new Pair<String, String>(rs.getString(1),rs.getString(2)));
		}
		rs.close();
		return a;
	}
	
//	void walk(Iterator<Path> it, boolean force, Path d2) throws SQLException, IOException {
//		PreparedStatement aa02get = sql("02appareaget");
//		PreparedStatement aa02put = sql("02appareaput");
//		Map<String,Integer> zz = new HashMap<String,Integer>();
//		Map<String,Integer> prio = new HashMap<String,Integer>();
//		pairToMap3(sql("01prioget2"), prio);
//		Map<String,Integer> cat = new HashMap<String,Integer>();
//		pairToMap3(sql("01catget2"), cat);
//		Set<String> zz2 = new HashSet<String>();
//		pairToMap3(aa02get, zz);
//
//		Set<Integer> notes = new HashSet<Integer>(2000000);
//		ResultSet rs = sql("zzz").executeQuery();
//		while (rs.next()) notes.add(rs.getInt(1));
//		rs.close();
//
//		PreparedStatement zzz2 = sql("zz2");
//		PreparedStatement zy3 = sql("zy3");
//		PreparedStatement fcget = sql("009fcget");
//		PreparedStatement fcupd = sql("009fcupd");
//		PreparedStatement fcins = sql("009fcins");
//		BufferedWriter pw = Files.newBufferedWriter(d2, csUtf8, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND); 
//		while (it.hasNext()) {
//			long lastmodDb = 0;
//			Path p = it.next();
//			long lastmodFile = Files.getLastModifiedTime(p).toMillis();
//
//			String pn = p.getFileName().toString();
//			fcget.setString(1, pn);
//			rs = fcget.executeQuery();
//			while (rs.next()) lastmodDb = rs.getLong(1); 
//
//			if (!force && lastmodFile == lastmodDb) continue;
//			System.out.println(pn + " at db=" + lastmodDb + ", at fs=" + lastmodFile);
//			JaxbSnotes sn = JAXB.unmarshal(p.toFile(), JaxbSnotes.class);
//			assert sn!=null && sn.n!=null && sn.n.size()>0;
//
//			String lang = sn.n.get(0).l;
//			PreparedStatement zy2 = sql("zy2");
//			Set<Integer> nol = new HashSet<Integer>(2000000);
//
//			zy2.setString(1, lang);
//			rs = zy2.executeQuery();
//			while (rs.next()) nol.add(rs.getInt(1));
//			rs.close();
//
//			for (JaxbNote x: sn.n) {
//				if (!zz.containsKey(x.a) && !zz2.contains(x.a)) {
//					aa02put.setString(1, x.a);
//					aa02put.addBatch();
//					zz2.add(x.a);
//				}
//			}
//			if (zz2.size()>0) {
//				int ia[] = aa02put.executeBatch();
//				assert zz2.size()==ia.length;
//				con.commit();
//				aa02put.clearBatch();
//				pairToMap3(aa02get, zz);
//				zz2.clear();
//			}
//			boolean itz = false, it3 = false;
//			for (JaxbNote x: sn.n) {
//				Integer pr = prio.get(x.p);
//				Integer ca = cat.get(x.c);
//				if (pr==null) System.err.println(x.n + "\tPriority unknown: " + x.p);
//				if (ca==null) System.err.println(x.n + "\tCategory unknown: " + x.c);
//				assert (pr!=null && ca!=null);
//				if (x.o==null) {
//					x.o = "Z" + x.n;
//				}
//				assert x.o!=null;
//				Integer area = zz.get(x.a);
//				assert area!=null : x.n + "\tArea unknown: " + x.a;
//				String s = x.n + "\t" + x.a + "\t" + x.d + "\t" + x.l + "\n";
//				if (!notes.contains(x.n)) {
//					zzz2.setInt(1, x.n);
//					zzz2.setInt(2, area);
//					zzz2.setInt(3, ca);
//					zzz2.setInt(4, pr);
//					if (x.m!=null && x.m.equals("*"))
//						zzz2.setInt(5, 2);
//					else if (x.m==null)
//						zzz2.setInt(5, 1);
//					else
//						assert false : "Unknown mark: " + x.m;
//					if (x.o!=null)
//						zzz2.setString(6, x.o);
//					else
//						zzz2.setNull(6, Types.VARCHAR);
//					zzz2.addBatch();
//					notes.add(x.n);
//					itz = true;
//					zy3.setInt(1, x.n);
//					zy3.setString(2, x.l);
//					zy3.setString(3, x.content);
//					zy3.addBatch();
//					nol.add(x.n);
//					it3 = true;
//					pw.write(s);
//				} else if (!nol.contains(x.n)) {
//					zy3.setInt(1, x.n);
//					zy3.setString(2, x.l);
//					zy3.setString(3, x.content);
//					zy3.addBatch();
//					it3 = true;
//					nol.add(x.n);
//					pw.write(s);
//				} 
//			} // for (JaxbNote x: sn.n) 
//			if (itz) {
//				zzz2.executeBatch();
//				zzz2.clearBatch();
//			}
//			if (it3) {
//				zy3.executeBatch();
//				zy3.clearBatch();
//			}
//			con.commit();
//			if (lastmodDb==0) {
//				fcins.setString(1, pn);
//				fcins.setLong(2, lastmodFile);
//				fcins.executeUpdate();
//			} else {
//				fcupd.setString(1, pn);
//				fcupd.setLong(2, lastmodFile);
//				fcupd.executeUpdate();
//			}
//			con.commit();
//			pw.flush();
//		}
//		pw.close();
//	} // void walk(Iterator<Path> it);

//	void skywalk(String area, PrintWriter pw) throws SQLException {
//		PreparedStatement ps = sql("sw01");
//		ps.setString(1, area);
//		ResultSet rs = ps.executeQuery();
//		while (rs.next()) {
//			String x = rs.getString(2);
//			pw.println(String.format("%07d\t%s\t%s", rs.getInt(1), x!=null?x:"", rs.getString(3)));
//		}
//		pw.flush();
//		rs.close();
//	} // void skywalk() throws SQLException {
	
//	void skywalk2(String area, PrintWriter pw) throws SQLException {
//		PreparedStatement ps = sql("sw02");
//		ps.setString(1, area);
//		ResultSet rs = ps.executeQuery();
//		while (rs.next()) {
//			String x = rs.getString(2);
//			pw.println(String.format("%07d\t%s\t%s", rs.getInt(1), x!=null?x:"", rs.getString(3)));
//		}
//		pw.flush();
//		rs.close();
//	} // void skywalk2() throws SQLException {

//	List<Integer> getUnknownObjid(String o, int top) throws SQLException {
//		List<Integer> a = new ArrayList<Integer>(100);
//		PreparedStatement ps = sql("sw03");
//		ps.setString(1, o);
////		ps.setInt(2,  top);
//		ResultSet rs = ps.executeQuery();
//		while (rs.next()) a.add(rs.getInt(1));
//		rs.close();
//		ps.close();
//		return a;
//	}

//	int updateObjid(List<Integer> num, List<String> obj) throws SQLException {
//		int x = num.size(), i = 0, p=999, q = 0;
//		assert obj.size()==x;
//		PreparedStatement ps = sql("sw04");
//		for (String o: obj) {
//			int j = num.get(i++);
//			if (o!=null) {
//				ps.setInt(1, j);
//				ps.setString(2, o);
//				ps.addBatch();
//				q++;
//			}
//			if (p--==0) {
//				ps.executeBatch();
//				con.commit();
//				ps.clearBatch();
//				p=999;
//			}
//		}
//		ps.executeBatch();
//		con.commit();
//		ps.close();
//		return q;
//	}
	
//	void report(String b) throws SQLException {
//		ResultSet rs = sql(b).executeQuery();
//		while (rs.next()) {
//			String a = rs.getString(1);
//			String q = rs.getString(2);
//			int p = rs.getInt(3);
////			int r = rs.getInt(4);
////			int s = rs.getInt(5);
//			System.out.println(a + "\t" + q + "\t" + p);
//		}
//		rs.close();
//	}
	
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
//				az.foundA(rs.getString(1), rs.getString(2));
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
//	private PreparedStatement zy5 = null;
//	void setMissedTitle(int num, String lang) throws SQLException {
//		assert num > 0 && lang!=null;
//		assert "DEJ".contains(lang);
//		if (zy5==null) zy5 = sql("zy5");
//		zy5.setInt(1, num);
//		zy5.setString(2, lang);
//		zy5.executeUpdate();
//		con.commit();
//	}
	
//	private PreparedStatement zy6 = null, v02=null;
//	void updateVer(AZ az) throws SQLException {
//		assert !az.badLang;
//		assert az.objid!=null;
//		if (zy6==null) zy6 = sql("zy6");
//		// update objid
//		if (!az.objid.equals(az.objid2)) {
//			zy6.setInt(1, az.num);
//			zy6.setString(2, az.objid2);
//			eu = zy6.executeUpdate();
//			assert eu==1;
//			az.objid2 = az.objid;
//			con.commit();
//		}
//		if (v02==null) v02 = sql("v02");
//		v02.clearParameters();
//		v02.setInt(1, az.num);
//		v02.setString(2, az.lang);
//		v02.setInt(3, Integer.valueOf(az.newVers));
//		v02.setString(4, az.newReleasedOn);
//		eu = v02.executeUpdate();
//		assert eu==1;
//		con.commit();
//	}
}
