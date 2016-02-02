package com.pinternals.ir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

import com.sap.JaxbNote;
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
	static Map<String,Integer> areaToCode = new HashMap<String,Integer>();
	static Map<Integer,String> codeToArea = new HashMap<Integer,String>();
	static Map<String,String> areaToDescr = new HashMap<String,String>();
	static Map<String,Set<String>> nickToArea = new HashMap<String,Set<String>>();
	private static Set<String> zz2 = new HashSet<String>();
	static PreparedStatement aa02get, aa02put, aa02upd;
	private static boolean aa02 = false;

	static void reload() throws SQLException {
		ResultSet rs = aa02get.executeQuery();
		areaToCode.clear();
		areaToDescr.clear();
		zz2.clear();
		while (rs.next()) {
			areaToCode.put(rs.getString(1), rs.getInt(2));
			areaToDescr.put(rs.getString(1), rs.getString(3));
			codeToArea.put(rs.getInt(2), rs.getString(1));
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
	static void addForNick(String nick, String area) {
		assert nick!=null && !"".equals(nick);
		assert area!=null && !"".equals(area);
		Set<String> s = nickToArea.get(nick);
		if (s==null) s = new HashSet<String>();
		s.add(area);
		nickToArea.put(nick, s);
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
	private static ResourceBundle ddlrb = ResourceBundle.getBundle("com/pinternals/ir/ddl");
	private static ResourceBundle sqlrb = ResourceBundle.getBundle("com/pinternals/ir/sql");
	private static ResourceBundle sqlab = ResourceBundle.getBundle("com/pinternals/ir/sqla");
//	(0, 'SAP UnknownYet'), \
//	(1, 'SAP Knowledge Base Article'), \
//	(2, 'SAP Note'),\
//	(3, 'SAP Security Note');\
	public static final int SAP_UNKNOWNYET=0, SAP_KBA=1, SAP_NOTE=2, SAP_SECNOTE=3;
	static Map<String,Integer> types = new HashMap<String,Integer>();

	private Connection con = null;
	boolean dba = false;
	private int eu, ia[];	// for asserts
	Path pathdb = null;

	private static String ddl(String code) {
		assert code!=null;
		return ddlrb.getString(code);
	}
	private PreparedStatement sql(String code) throws SQLException {
		assert code!=null;
		return con.prepareStatement(sqlrb.getString(code), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
	}
	private PreparedStatement sqla(String code) throws SQLException {
		assert code!=null;
		return con.prepareStatement(sqlab.getString(code), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
	}
	/**
	 * TODO: rewrite to truly updatable prepared statements
	 * @param code
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement sqlUpd(String code) throws SQLException {
		assert code!=null;
		return con.prepareStatement(sqlrb.getString(code), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
	}

	static void initDB(Path db) throws SQLException {
		NotesDB ndb = new NotesDB(db, false, false);

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
		NotesDB ndb = new NotesDB(db, false, true);
		for (String s: ddlrb.keySet()) if (s.startsWith("a")) 
			ndb.con.prepareStatement(ddl(s)).execute();
		ndb.con.commit();
		ndb.close();
		ndb = new NotesDB(db, true, true);
		return ndb;
	}	

	NotesDB(Path pat, boolean checksql, boolean isdba) throws SQLException {
		this.pathdb = pat;
		this.dba = isdba;
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException c) {
			throw new SQLException("Cannot load sqlite JDBC driver");
		}
		con = java.sql.DriverManager.getConnection("jdbc:sqlite:" + pat.toFile());
		con.setAutoCommit(false);
		if (checksql && !dba) 
			for (String s: sqlrb.keySet()) sql(s);	// is statement compilable ?
		if (checksql && dba) 
			for (String s: sqlab.keySet()) {
				try {
					sqla(s);	// is statement compilable ?
				} catch (SQLException e) {
					System.err.println(pat + "\t" + s + "\t" + sqlab.getString(s));
					System.err.println(e.getMessage());
					System.err.println(e.getSQLState());
					throw e;
				}
			}
		if (!dba) pairToMap3(sql("01types"), types);
		if (!dba) for (String s: sqlrb.keySet()) if (s.startsWith("00open")) sql(s).execute();
		if (dba) for (String s: sqlab.keySet()) if (s.startsWith("00open")) sqla(s).execute();
	}

	boolean isClosed() throws SQLException {
		return con.isClosed();
	}
	void commit() throws SQLException {
		con.commit();
	}
	void close() throws SQLException {
		con.commit();
		con.close();
	}

	void initArea() throws SQLException {
		assert !dba && !isClosed();
		Area.aa02get = sql("02appareaget");
		Area.aa02put = sqlUpd("02appareaput");
		Area.aa02upd = sqlUpd("02appareaupd");
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

			initArea();
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
			assert num<bsmaxnotes : String.format("Note number %d have to be lt %d", num, bsmaxnotes);
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

	/**
	 * Ask central database for all notes by areas
	 * @param collection of areas (ex: BC-XI, BC-JAS-SEC-CON, ...)
	 * @return 
	 * @throws SQLException
	 */
	List<AZ> getNotesCDB_byAreas(Collection<String> as) throws SQLException {
		PreparedStatement cl = sql("w08a"), ps = sql("w08b");
		cl.executeUpdate();
		for (String a: as) {
			ps.setInt(1, Area.getCode(a));
			ps.addBatch();
		}
		ps.executeBatch();
//		commit();
		ps = sql("w08c");
		ResultSet rs = ps.executeQuery();
		List<AZ> az = new ArrayList<AZ>(1000);
		while (rs.next()) {
			String area = Area.codeToArea.get(rs.getInt(3));
			assert area!=null && as.contains(area);
			AZ z = new AZ(rs.getInt(1), area, rs.getString(2), rs.getInt(4));
			az.add(z);
		}
		cl.executeUpdate();
		commit();
		return az;
		
	}
	
//	List<AZ> getZ3(String area) throws SQLException {
//		List<AZ> az = new ArrayList<AZ>(1000);
//		PreparedStatement ps = sql("w07");
//		ps.setString(1, area);
//		ResultSet rs = ps.executeQuery();
//		while (rs.next()) {
//			int num = rs.getInt(1);
//			String j = rs.getString(2);
//			az.add(new AZ(num, j, area));
//		}
//		ps.close();
//		return az;
//	}
	
//	@Deprecated
//	void getZ3a(List<AZ> azl) throws SQLException {
//		PreparedStatement ps = con.prepareStatement("select askdate,NotesKey,"
//				+ "Title,Type,Version,Priority,Category,ReleasedOn,ComponentKey,ComponentText,Language from a01 where NotesNumber=?;");
//		ResultSet rs;
//		for (AZ az: azl) {
//			ps.setInt(1, az.num);
//			rs = ps.executeQuery();
//			while (rs.next()) {
//				com.sap.Properties p = new Properties();
//				p.setSapNotesNumber(String.valueOf(az.num));
//				p.setSapNotesKey(rs.getString(2));
//				p.setTitle(rs.getString(3));
//				p.setType(rs.getString(4));
//				p.setVersion(rs.getString(5));
//				p.setPriority(rs.getString(6));
//				p.setCategory(rs.getString(7));
//				p.setReleasedOn(rs.getString(8));
//				p.setComponentKey(rs.getString(9));
//				p.setComponentText(rs.getString(10));
//				p.setLanguage(rs.getString(11));
//				az.foundA(rs.getString(1), p);
//			}
//		}
//	}

	PreparedStatement ps3b = null;
	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	List<AZ> getNotesDBA() throws SQLException {
		assert dba && !isClosed();
		List<AZ> azl = new ArrayList<AZ>(10000);
		if (ps3b==null) ps3b = sqla("a01get2");// con.prepareStatement(""
		assert types!=null && types.size()>0 : "note types aren't filled /" + types;
		ResultSet rs;
		rs = ps3b.executeQuery();
		while (rs.next()) {
			Integer tp = types.get(rs.getString(4));
			Objects.requireNonNull(tp, String.format("'%s' not mapped to mark", rs.getString(4)));
			AZ az = new AZ(rs.getInt(12), rs.getString(9), rs.getString(2), tp);
			com.sap.lpad.Properties p = new com.sap.lpad.Properties();
			p.setSapNotesNumber(String.valueOf(rs.getInt(12)));
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
			az.mprop = p;
			az.longTexts = rs.getInt(13);
			az.swcv = rs.getInt(14);
			az.sp = rs.getInt(15);
			azl.add(az);
		}
		return azl;
	}

	PreparedStatement psPut = null;
	void putA01(com.sap.lpad.Properties p, Instant n) throws SQLException {
		assert dba && !isClosed();
		if (psPut==null) psPut = sqla("a01put");//con.prepareStatement("");
		String askdate = n.toString();
		psPut.setString(1, askdate);
		psPut.setInt(2, Integer.parseInt(p.getSapNotesNumber()));
		psPut.setString(3, p.getSapNotesKey());
		psPut.setString(4, p.getTitle());
		psPut.setString(5, p.getType());
		psPut.setInt(6, Integer.parseInt(p.getVersion()));	// version
		psPut.setString(7, p.getPriority()); // priority
		psPut.setString(8, p.getCategory());
		psPut.setString(9, p.getReleasedOn());
		psPut.setString(10, p.getComponentKey());
		psPut.setString(11, p.getComponentText());
		psPut.setString(12, p.getLanguage());
		eu = psPut.executeUpdate();
		assert eu == 1;
		con.commit();
	}

	PreparedStatement psPutf = null, psPutSW=null, psPutSp=null;
	void putFeeds(com.sap.lpad.Properties prop, com.sap.lpad.Feed f, Instant n) throws SQLException {
		assert dba && !isClosed();
		if (psPutf==null) psPutf = sqla("long01put");
		if (psPutSW==null) psPutSW = sqla("sw01put");
		if (psPutSp==null) psPutSp = sqla("sp01put");
		String askdate = n.toString();
		int num = Integer.parseInt(prop.getSapNotesNumber());
		int ver = Integer.parseInt(prop.getVersion());
		String lang = prop.getLanguage();
		
		psPutf.clearBatch();
		psPutSW.clearBatch();
		for (com.sap.lpad.Entry ch: f.getEntry()) {
			String cas = null;
//			System.out.println(num + "\t" + ch.getContent().getType());
			for (Object o: ch.getIdOrLinkOrTitle()) if (o instanceof com.sap.lpad.Link) {
				cas = ((com.sap.lpad.Link)o).getTitle();
			}
/*				if (o instanceof JAXBElement) {
//					JAXBElement j = (JAXBElement)o;
//					System.out.println(j.getName() + " " + j.getValue());
//				} else if (o instanceof com.sap.lpad.Title) {
//					com.sap.lpad.Title tit = (com.sap.lpad.Title)o;
//					System.out.println(tit.getContent());
//				} else if (o instanceof com.sap.lpad.Category) {
//					com.sap.lpad.Category cat;
//					cat = (com.sap.lpad.Category)o;
					System.out.println(cat.getTerm());*/
			com.sap.lpad.Properties pch = ch.getContent().getProperties();
			if ("LongText".equals(cas)) {
				int typekey = Integer.parseInt(pch.getTypeKey());
				assert typekey > 0;
				assert lang.equals(pch.getLanguage());
//				System.out.println(pch.getTypeText());
//				System.out.println();
				psPutf.setString(1, askdate);
				psPutf.setInt(2, num);
				psPutf.setInt(3, ver);
				psPutf.setString(4, pch.getLanguage());
				psPutf.setInt(5, typekey);
				psPutf.setString(6, pch.getText());
				psPutf.addBatch();
			} else if ("SoftCom".equals(cas)) {
				psPutSW.setString(1, askdate);
				psPutSW.setInt(2, num);
				psPutSW.setInt(3, ver);
				psPutSW.setInt(4, Integer.parseInt(pch.getPakId()));
				psPutSW.setInt(5, Integer.parseInt(pch.getAleiKey()));
				psPutSW.setString(6, pch.getName());
				psPutSW.setString(7, pch.getVerFrom());
				psPutSW.setString(8, pch.getVerTo());
				psPutSW.addBatch();
			} else if ("SpPatch".equals(cas)) {
				psPutSp.setString(1, askdate);
				psPutSp.setInt(2, num);
				psPutSp.setInt(3, ver);
				psPutSp.setString(4, pch.getName());
				psPutSp.setString(5, pch.getSp());
				psPutSp.setString(6, pch.getLevel());
				psPutSp.addBatch();
			} else {
				System.err.println("CAS=" + cas);
				System.exit(-1);
			}
		}
		ia = psPutf.executeBatch();
		ia = psPutSW.executeBatch();
		ia = psPutSp.executeBatch();
		commit();
	}
	
	// area-db into notes.db
	void fromDBAtoCDB(NotesDB dba) throws SQLException {
		// dba == area db
		assert dba!=null && dba.con!=null && !dba.con.isClosed();
		PreparedStatement px = dba.sqla("a01get");
		PreparedStatement mr01 = sql("mr01"), mr02 = sqlUpd("mr02"), zzz2 = sqlUpd("zz2"), zy3 = sqlUpd("zy3");
		PreparedStatement mr05 = sqlUpd("mr05"), mr08 = sql("mr08"), mr09 = sqlUpd("mr09");
		ResultSet rx = px.executeQuery(), rs;
		pairToMap3(sql("01prioget2"), prio);
		pairToMap3(sql("01catget2"), cat);
		initArea();
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
		ia = zzz2.executeBatch();
		eu = ia.length;
		ia = mr02.executeBatch();
		eu += ia.length;
		ia = zy3.executeBatch();
		eu += ia.length;
		ia = mr05.executeBatch();
		eu += ia.length;
		ia = mr09.executeBatch();
		eu += ia.length;
		con.commit();
		assert eu>0;
	}
	
	List<Integer> arrrrrrrrrr(List<String> as, String pat) throws SQLException {
		List<Integer> ar = new ArrayList<Integer>(1000);
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

	/**
	 * called from launchpad for <area>.db
	 * @param area	like BC-XI-CON-AFW-SEC or FI-AA
	 * @param code	dynamic integer
	 */
//	void addArea(String area, int code) {
//		assert dba;
//		if (areaAreas==null) areaAreas = new HashMap<String,Integer>();
//		// TODO assert for code differences for one area (look into Area)  
//		areaAreas.put(area, code);
//	}

	/**
	 * Mutual synchronization of central notes.db and many of <area>.db
	 * @param colldba collection of <area>.db objects
	 * @throws SQLException
	 */
	void sync(NotesDB dba) throws SQLException {
		assert dba.dba && !dba.isClosed();
		System.out.println("sync " + pathdb + " with " + dba.pathdb);
		fromDBAtoCDB(dba);	// one way
//		fromCDBtoDBA(dba);
		bsnums = new BitSet(bsmaxnotes);
		
	}
	/**
	 * for C:\sap\notes-cache\launchpad\<u>xi</u>\xi.db returns xi
	 * @return nickname
	 */
	String getNick() {
		assert dba;
		assert pathdb.getNameCount()>2;
		String nick = pathdb.getName(pathdb.getNameCount()-2).toString();
		return nick;
	}
	
	PreparedStatement psSetMark=null, psSetObjID=null;
	void setMark(int num, int mark) throws SQLException {
		assert !dba && !isClosed();
		if (psSetMark==null) psSetMark = sql("w09");
		psSetMark.setInt(1, num);
		psSetMark.setInt(2, mark);
		eu = psSetMark.executeUpdate();
		assert eu==1;
		commit();
	}
	void setObjid(int num, String objid) throws SQLException {
		assert !dba && !isClosed();
		if (psSetObjID==null) psSetObjID = sql("w10");
		psSetObjID.setInt(1, num);
		psSetObjID.setString(2, objid);
		eu = psSetObjID.executeUpdate();
		assert eu==1;
		commit();
	}
	
}
