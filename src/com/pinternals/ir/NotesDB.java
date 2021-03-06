package com.pinternals.ir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.bind.JAXB;

import org.apache.commons.io.input.CloseShieldInputStream;
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
	private static final Charset utf8 = Charset.forName("UTF-8");
	private static ResourceBundle ddlrb = ResourceBundle.getBundle("com/pinternals/ir/ddl");
	private static ResourceBundle sqlrb = ResourceBundle.getBundle("com/pinternals/ir/sql");
	private static ResourceBundle sqlab = ResourceBundle.getBundle("com/pinternals/ir/sqla");
//	(0, 'SAP UnknownYet'), \
//	(1, 'SAP Knowledge Base Article'), \
//	(2, 'SAP Note'),\
//	(3, 'SAP Security Note');\
	public static final int SAP_UNKNOWNYET=0, SAP_KBA=1, SAP_NOTE=2, SAP_SECNOTE=3, SAP_ONE=4, SAP_STAND=5, SAP_INVALID=99;
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
		assert code!=null && !dba;
		return con.prepareStatement(sqlrb.getString(code), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
	}
	private PreparedStatement sqla(String code) throws SQLException {
		assert code!=null && dba;
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement(sqlab.getString(code), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		} catch (SQLException e) {
			System.err.println(String.format("SQL Error for '%s'%n%s%n%s", code, sqlab.getString(code), e.getMessage()));
			throw e;
		}
		return ps;
	}
	private PreparedStatement setPs(PreparedStatement ps, Object[] args) throws SQLException {
		assert ps!=null : ps;
		assert args!=null && args.length>0 : args;
		int i = 1;
		for (Object o: args) {
			if (o==null)
				ps.setString(i, (String)o);
			else if (o instanceof Integer) {
				assert o!=null;
				ps.setInt(i, (Integer)o);
			} else if (o instanceof String)
				ps.setString(i, (String)o);
			else 
				throw new SQLException("setPs, NIY: " + o.getClass() + " " + o);
			i++;
		}
		return ps;
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

//	static void initDB(Path db) throws SQLException {
//		NotesDB ndb = new NotesDB(db, true, false, false);
//
//		ndb.con.commit();
//
//		for (String s: sqlrb.keySet()) {
//			System.out.print(s + " ");
//			ndb.sql(s);	// is statement compilable ?
//		}
//	}
//	
//	static NotesDB initDBA(Path db) throws SQLException {
//		NotesDB ndb = new NotesDB(db, true, false, true);
//		ndb.con.commit();
//		ndb.close();
//		ndb = new NotesDB(db, true, true);
//		return ndb;
//	}	

	NotesDB(Path pat, boolean init, boolean checksql, boolean isdba) throws SQLException {
		this.pathdb = pat;
		this.dba = isdba;
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException c) {
			throw new SQLException("Cannot load sqlite JDBC driver");
		}
		con = java.sql.DriverManager.getConnection("jdbc:sqlite:" + pat.toFile());
		con.setAutoCommit(false);
		List<String> tm = new ArrayList<String>();
		PreparedStatement ps = null;
		if (init) {
			for (String s: ddlrb.keySet()) 
				if ((s.startsWith("0") && !dba) || (s.startsWith("a") && dba)) tm.add(s);
			tm.sort(Comparator.naturalOrder());
			for (String s: tm) {
				ps = con.prepareStatement(ddl(s));
				ps.execute();
				con.commit();
			}
			tm.clear();
		}
		ResourceBundle rb = dba ? sqlab : sqlrb;
		if (checksql) {
			tm.addAll(rb.keySet());
			tm.sort(Comparator.naturalOrder());
			for (String s: tm) {
				String sq = rb.getString(s);
				try {
					ps = con.prepareStatement(sq, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
				} catch (SQLException e) {
					System.err.println(String.format("database %s cannot compile '%s'%n%s%n%s", pat, s, sq, e.getMessage()));
					throw e;
				} finally {
					if (ps!=null) ps.close();
				}
			}
		}
		for (String s: rb.keySet()) if (s.startsWith("00open")) con.prepareStatement(rb.getString(s)).execute();
		if (!dba) {
			pairToMap3(sql("01types"), types);
			pairToMap3(sql("01prioget2"), prio);
			pairToMap3(sql("01catget2"), cat);
		}
		con.commit();
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
			AZ z = new AZ(rs.getInt(1), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), false);
			az.add(z);
		}
		cl.executeUpdate();
		commit();
		return az;
		
	}

	/**
	 * Ask central database for all notes by areas
	 * @param collection of areas (ex: BC-XI, BC-JAS-SEC-CON, ...)
	 * @return 
	 * @throws SQLException
	 */
	List<AZ> getNotesCDB_byNums(List<Integer> as) throws SQLException {
		PreparedStatement cl = sql("w08d"), ps = sql("w08e");
		cl.executeUpdate();
		int i=0;
		for (int a: as) {
			ps.setInt(1, a);
			ps.setInt(2, i++);
			ps.addBatch();
		}
		ps.executeBatch();
//		commit();
		ps = sql("w08n");
		ResultSet rs = ps.executeQuery();

		List<AZ> az2 = new ArrayList<AZ>(i);
		while (rs.next()) {
			String area = Area.codeToArea.get(rs.getInt(3));
			assert area!=null;
			AZ z = new AZ(rs.getInt(1), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), false);
			az2.add(z);
		}
		cl.executeUpdate();
		commit();
		return az2;
	}

	PreparedStatement ps3b = null;
	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	List<AZ> getNotesDBA(Map<String,Integer> cat, Map<String,Integer> prio) throws SQLException {
		assert dba && !isClosed();
		assert cat.size()>0 && prio.size()>0 : cat + " " + prio;
		List<AZ> azl = new ArrayList<AZ>(10000);
		if (ps3b==null) ps3b = sqla("trunkget");// con.prepareStatement(""
		assert types!=null && types.size()>0 : "note types aren't filled /" + types;
		ResultSet rs;
		rs = ps3b.executeQuery();
		while (rs.next()) {
			int num = rs.getInt(1), ver = rs.getInt(5);
			com.sap.lpad.Properties p = new com.sap.lpad.Properties();
			p.setSapNotesNumber(""+num);
			p.setSapNotesKey(rs.getString(2));
			p.setTitle(rs.getString(3));
			p.setType(rs.getString(4));
			p.setVersion(""+ver);
			p.setPriority(rs.getString(6));
			p.setCategory(rs.getString(7));
			p.setReleasedOn(rs.getString(8));
			p.setComponentKey(rs.getString(9));
			p.setComponentText(rs.getString(10));
			p.setLanguage(rs.getString(11));
//			String langMaster = rs.getString(12), askdate = rs.getString(13);
			Integer tp = types.get(p.getType());
			Objects.requireNonNull(tp, String.format("'%s' not mapped to mark", p.getType()));
			int cat1 = cat.get(rs.getString(7)), prio1 = prio.get(rs.getString(6));
//			AZ az = new AZ(num, p.getComponentKey(), p.getSapNotesKey(), tp, cat1, prio1);
//			az.mprop = p;
//			azl.add(az);
		}
		return azl;
	}

	PreparedStatement lttins = null;
	private void putA03(String notesType, String lang, int typeKey, String typeText) throws SQLException {
		assert dba && !isClosed();
		if (lttins==null) lttins = sqla("lttins");
		Object[] ax = new Object[]{notesType, typeKey, lang, typeText};
		setPs(lttins, ax).executeUpdate();
	}
	
	PreparedStatement qryins = null;
	void putA02(int notenumber, String language, int version, Instant n, String askLanguage, Integer askVersion, Integer rc, String message) throws SQLException {
		assert dba && !isClosed();
		if (qryins==null) qryins = sqla("queryins");
		Object[] qry = new Object[]{notenumber, language, version, n.toString(), askLanguage, askVersion, rc, message};
		setPs(qryins, qry).executeUpdate();
	}
	
	private Map<String,PreparedStatement> dbaPs = new HashMap<String,PreparedStatement>();
	void putA01(com.sap.lpad.Entry en) throws SQLException {
		assert dba && !isClosed();
		String pk[] = new String[]{"trunkins", 
				"longins", "softcomins", "corrinsins", "spins", "reftoins", "refbyins", "patchins", "attachins",
				"sidecauins", "sidesolins", "productins", "langins", "othcomins", "facetins", "facetupd"};
		if (dbaPs.size()==0) for (String s: pk) dbaPs.put(s, sqla(s));
		com.sap.lpad.Properties q, p = en.getContent().getProperties();

		List<Entry<String,Object[]>> todo = new ArrayList<Entry<String,Object[]>>();
		Object o[];
		Object[] facets = new Object[]{
			Integer.parseInt(p.getSapNotesNumber()), p.getLanguage(), Integer.parseInt(p.getVersion())   
			, null //Languages -- #4
			, null, null, null, null, null, null, null, null, null, null, null
			, null // OtherCom -- #16
			};

		Map<String,Integer> fn = new HashMap<String,Integer>();
		fn.put("Languages", 4);
		fn.put("LongText", 5);
		fn.put("SoftCom", 6);
		fn.put("Sp", 7);
		fn.put("Patch", 8);
		fn.put("CorrIns", 9);
		fn.put("RefTo", 10);
		fn.put("RefBy", 11);
		fn.put("SideSol", 12);
		fn.put("SideCau", 13);
		fn.put("Attach", 14);
		fn.put("Product", 15);
		fn.put("OtherCom", 16);
		fn.put("VersionInfo", 17); //for future

		for (com.sap.lpad.Link l: en.getLink()) {
			if ("self".equals(l.getRel()) || "VersionInfo".equals(l.getTitle()) ) continue;
			assert l.getInline()!=null && l.getInline().getFeed()!=null : l.getTitle();

			List<com.sap.lpad.Entry> ex = l.getInline().getFeed().getEntry();
			assert ex!=null;
//			System.out.println(l.getTitle());
			assert fn.containsKey(l.getTitle()) : "key " + l.getTitle() + " is unknown";
			int fx = fn.get(l.getTitle())-1; 
			facets[fx] = facets[fx]==null ? new Integer(0) : facets[fx]; 
			for (com.sap.lpad.Entry eo: ex) {
				q = eo.getContent().getProperties();
				facets[fx] = new Integer(  ((Integer)facets[fx]).intValue() + 1); 
				switch (l.getTitle()) {
				case "Languages":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),q.getLangMaster()};
						todo.add(new AbstractMap.SimpleEntry<String,Object[]>("langins", o));
					break;
				case "LongText":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),q.getLanguage(),Integer.parseInt(q.getVersion()),
						q.getTypeKey(), q.getText()};
					todo.add(new AbstractMap.SimpleEntry<String,Object[]>("longins", o));
					putA03(p.getType(), p.getLanguage(), Integer.parseInt(q.getTypeKey()), q.getTypeText());
					break;
				case "SoftCom":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),Integer.parseInt(q.getVersion()),
							Integer.parseInt(q.getPakId()), Integer.parseInt(q.getAleiKey()), 
							q.getName(), q.getVerFrom(), q.getVerTo()}; 
					todo.add(new AbstractMap.SimpleEntry<String,Object[]>("softcomins", o));
					break;
				case "Sp":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),Integer.parseInt(q.getVersion()),
						q.getName(), q.getSp(), q.getVersion()};
					todo.add(new AbstractMap.SimpleEntry<String,Object[]>("spins", o));
					break;
				case "Patch":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),Integer.parseInt(q.getVersion()),
							q.getName(), q.getSp(), Integer.parseInt(q.getLevel())};
					todo.add(new AbstractMap.SimpleEntry<String,Object[]>("patchins", o));
					break;
				case "CorrIns":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),Integer.parseInt(q.getVersion()),
							Integer.parseInt(q.getPakId()), q.getName(), Integer.parseInt(q.getCount()) };
					todo.add(new AbstractMap.SimpleEntry<String,Object[]>("corrinsins", o));
					break;
				case "RefTo":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),Integer.parseInt(q.getVersion()),
							q.getRefNum(), q.getRefType(), q.getRefTitle(), q.getRefUrl(), q.getRefKey()};
					todo.add(new AbstractMap.SimpleEntry<String,Object[]>("reftoins", o));
					break;
				case "RefBy":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),Integer.parseInt(q.getVersion()),
							q.getRefNum(), q.getRefType(), q.getRefTitle(), q.getRefUrl(), q.getRefKey()};
					todo.add(new AbstractMap.SimpleEntry<String,Object[]>("refbyins", o));
					break;
				case "SideSol":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),Integer.parseInt(q.getVersion()),
							q.getRefNum(), q.getRefType(), q.getRefTitle(), q.getRefUrl(), q.getRefKey()};
					todo.add(new AbstractMap.SimpleEntry<String,Object[]>("sidesolins", o));
					break;
				case "SideCau":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),Integer.parseInt(q.getVersion()),
							q.getRefNum(), q.getRefType(), q.getRefTitle(), q.getRefUrl(), q.getRefKey()};
					todo.add(new AbstractMap.SimpleEntry<String,Object[]>("sidecauins", o));
					break;
				case "Attach":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),Integer.parseInt(q.getVersion()),
							q.getFileName(), q.getFileSize(), q.getFileLink(), q.getMimeType()};
					todo.add(new AbstractMap.SimpleEntry<String,Object[]>("attachins", o));
					break;
				case "Product":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),Integer.parseInt(q.getVersion()),
							q.getProductKey(), q.getProductName(), q.getProductVersion() };
					todo.add(new AbstractMap.SimpleEntry<String,Object[]>("productins", o));
					break;
				case "OtherCom":
					o = new Object[]{Integer.parseInt(q.getSapNotesNumber()),Integer.parseInt(q.getVersion()),
							q.getKey(), q.getValue() };
					todo.add(new AbstractMap.SimpleEntry<String,Object[]>("othcomins", o));
					break;
				default:
					throw new RuntimeException("NIY " + l.getTitle() + " " + ex.size());
				}
			}
		}
		o = new Object[]{Integer.parseInt(p.getSapNotesNumber()), p.getSapNotesKey(), p.getTitle(), p.getType()
				, Integer.parseInt(p.getVersion()), p.getPriority(), p.getCategory(), p.getReleasedOn(), p.getComponentKey()
				, p.getComponentText(), p.getLanguage()};
		setPs(dbaPs.get("trunkins"), o).addBatch();
		setPs(dbaPs.get("facetins"), facets).addBatch();
		setPs(dbaPs.get("facetupd"), facets).addBatch();
		for (Entry<String,Object[]> e: todo) 
			setPs(dbaPs.get(e.getKey()), e.getValue()).addBatch();
		for (String s: pk) dbaPs.get(s).executeBatch();
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
	
//	List<Integer> arrrrrrrrrr(List<String> as, String pat) throws SQLException {
//		List<Integer> ar = new ArrayList<Integer>(1000);
//		PreparedStatement ps = sql("03areaunk");
//		ps.setString(1, pat);
//		ResultSet rs = ps.executeQuery();
//		String p = null;
//		while (as.size()>0 && rs.next()) {
//			int num = rs.getInt(1);
//			String t = rs.getString(2);
//			if (p!=t && as.contains(t)) {
//				System.out.println(num + "\t" + t);
//				ar.add(num);
//				as.remove(t);
//				p = t;
//			}
//		}
//		return ar;
//	}

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
	
	/**
	 * 
	 * @param cdb notes.db
	 * @param facets
	 * @throws IOException
	 * @throws SQLException
	 */
	void importFacets(NotesDB cdb, Path facets) throws IOException, SQLException {
		assert dba && !isClosed();
		StringJoiner sj = new StringJoiner("\n");
		Iterator<Path> it = Files.newDirectoryStream(facets, "*.zip").iterator();
		Pattern xp = Pattern.compile("(\\d+)_(.)_(.+)\\.xml");
		while (it.hasNext()) {
			Path p = it.next();
			int i=0;
			ZipInputStream zis = new ZipInputStream(Files.newInputStream(p), utf8);
			ZipEntry ze = zis.getNextEntry();
			while (ze!=null) {
				Matcher m = xp.matcher(ze.getName());
				assert m.matches() && m.groupCount()==3 : m;
				com.sap.lpad.Entry en = JAXB.unmarshal(new CloseShieldInputStream(zis), com.sap.lpad.Entry.class);
				com.sap.lpad.Properties pr = en.getContent().getProperties();
				putA01(en);
				putA02(Integer.parseInt(pr.getSapNotesNumber()), pr.getLanguage(),
						Integer.parseInt(pr.getVersion()), 
						ze.getLastModifiedTime().toInstant(), null, null, null, null);
				i++;
				zis.closeEntry();
				ze = zis.getNextEntry();
			}
			zis.close();
			commit();
			System.out.println(String.format("%s - %d commited", p.getFileName(), i));
		}
			
		it = Files.newDirectoryStream(facets, "000*.xml").iterator();
		int i = 0;
		while (it.hasNext()) {
			Path p = it.next();
			Matcher m = xp.matcher(p.getFileName().toString());
			assert m.matches() && m.groupCount()==3 : m;
			com.sap.lpad.Entry en = JAXB.unmarshal(Files.newInputStream(p), com.sap.lpad.Entry.class);
			com.sap.lpad.Properties pr = en.getContent().getProperties();
			int n2 = Integer.parseInt(m.group(1)), n3 = Integer.parseInt(pr.getSapNotesNumber()), v3 = Integer.parseInt(pr.getVersion());
			assert n2==n3 : "Note number error: " + n2 + "/" + n3 + "/" + p.getFileName().toString();
			String l2 = m.group(2), l22 = m.group(3);
			if (!pr.getLanguage().equals(l2) || !pr.getVersion().equals(l22)) {
				sj.add(String.format("ren %s %s", p.getFileName().toString(), String.format("%010d_%s_%d.xml", n2, pr.getLanguage(), v3)));
			} else {
				putA01(en);
				putA02(n3, pr.getLanguage(), v3, Files.getLastModifiedTime(p).toInstant(), null, null, null, null);
				if (++i>499) {
					System.out.println(i + " files commited");
					commit();
					i=0;
				}
			}
		}
		if (i>0) {
			System.out.println(i + " files commited");
			commit();
		}
		if (sj.length()>0) {
			BufferedWriter bw = Files.newBufferedWriter(facets.resolve("renamer.bat"), utf8); 
			bw.write(sj.toString());
			bw.close();
		}
		// fill values from DBA into CDB
		Collection<String> areas = Area.nickToArea.get(getNick());
		List<AZ> azs = cdb.getNotesCDB_byAreas(areas);
		List<AZ> ozs = getNotesDBA(cdb.cat, cdb.prio);
		for (AZ x: azs) { // every x.num occurs at `azs` once
			assert x.num>0 && x.mark>=0 && x.objid!=null && x.area!=0 && x.mprop==null;
			for (AZ y: ozs) if (x.num==y.num) {
				if (x.mark!=y.mark) { 
					assert x.mark==0 : x.num;
					System.out.println(String.format("Note %s has to be turned to mark=%d", x.num, y.mark));
					cdb.setMark(x.num, y.mark);
					x.mark = y.mark;
				}
				if (!x.objid.equals(y.objid)) {
					assert x.objid.startsWith("Z") : x.num;
					System.out.println(String.format("Note %s has to be turned to objid=%s", x.num, y.objid));
					cdb.setObjid(x.num, y.objid);
					x.objid = y.objid;
				}
			}
		}
	}

	PreparedStatement psSWC = null;
	Object getNotesBySWCV(String name, V f) throws SQLException {
		assert dba && !isClosed();
		if (psSWC==null) psSWC=sqla("swcget");
		psSWC.setString(1, name);
		psSWC.setString(2, String.format("SP%03d", f.sp));
		psSWC.setInt(3, f.patch);
		ResultSet rs = psSWC.executeQuery();
		while (rs.next()) {
			String num = rs.getString(1);
			String ver = rs.getString(2);
			String title = rs.getString(3);
			System.out.println(String.format("%s\t%s\t%s", name, num, title));
		}
		return null;
	}
	
	PreparedStatement sldswcv1 = null; 
	void putSWCV(List<java.util.Properties> prop) throws SQLException {
		assert !dba && !isClosed();
		if (sldswcv1==null) sldswcv1=sql("sldswcv1");
		sldswcv1.clearBatch();
		for (java.util.Properties p: prop) {
			String type = p.getProperty("Type");
			if (type==null) {
				System.err.println(p);
				continue;
			}
			sldswcv1.setString(1, p.getProperty("Name"));
			sldswcv1.setString(2, p.getProperty("Caption"));
			sldswcv1.setString(3, p.getProperty("Version"));
			sldswcv1.setString(4, type);
			sldswcv1.addBatch();
		}
		sldswcv1.executeBatch();
		commit();
	}
	
	PreparedStatement sldswcv2 = null; 
	void getSWCV(Collection<String> set, Object[][] arr, int j) throws SQLException {
		assert !dba && !isClosed();
		assert set!=null && arr!=null && j>0;
		if (sldswcv2==null) sldswcv2=sql("sldswcv2");
		ResultSet rs = sldswcv2.executeQuery();
		while (rs.next()) {
			String name = rs.getString(1);
			if (!set.contains(name)) continue;
			String caption = rs.getString(2);
			String version = rs.getString(3);
			for (int i=0; i<j; i++) if (name.equals( (String)arr[1][i] )) {
				V f = (V)arr[0][i];
				if (f.eq(version)) {
					arr[2][i] = caption;
					break;
				}
			}
		}
	}

	PreparedStatement sapk1=null, sapk2=null; 
	void getNotesBySAPK(Sapk pk) throws SQLException {
		assert dba && !isClosed();
		assert pk!=null;

		if (sapk1==null) sapk1=sqla("sapk1");
		if (sapk2==null) sapk2=sqla("sapk2");
		sapk1.setString(1, pk.name);
		sapk1.setString(2, pk.ver);
		sapk2.setString(1, pk.name);

		ResultSet rs = sapk1.executeQuery(), rs2;
		while (rs.next()) {
			int num = rs.getInt(1);
			int ver = rs.getInt(2);
			sapk2.setInt(2, num);
			sapk2.setInt(3, ver);
			rs2 = sapk2.executeQuery();
			System.out.println(String.format("%s-%s-%d\t%d-%d", pk.name, pk.ver, pk.patchlevel, num, ver));
			while (rs2.next()) {
				String sp = rs2.getString(1);
				int level = rs2.getInt(2);
				boolean b = false;
				if (pk.devk.length()=="SAPKB74099".length() && sp.length()=="SAPKB74099".length()) {
					String sver = sp.substring(5, 8);
					if (sver.equals(pk.ver)) {
						System.out.println(String.format(" %s==%s sp=%s level=%d", sver, pk.ver, sp, level));
						b = true;
					}
				} else {
					throw new RuntimeException(pk.devk + "\t" + sp);
				}
				
				if (!b) System.out.println(String.format("\t\tsp=%s level=%d", sp, level));
			}
		}
	}
	
	PreparedStatement stat1clr=null, stat1ins=null, stat1upd=null, stat1get=null, stat1gethottest=null; 
//	List<Integer> stat1(List<NotesDB> dbas, boolean update, Predicate<AZ> add) throws SQLException {
//		assert !dba && !isClosed();
//		assert add!=null;
//		if (stat1clr==null) stat1clr=sql("stat1clr");
//		if (stat1ins==null) stat1ins=sql("stat1ins");
//		if (stat1upd==null) stat1upd=sql("stat1upd");
//		if (stat1gethottest==null) stat1gethottest=sql("stat1gethottest");
//		List<Integer> numsToDload = new ArrayList<Integer>();
//		if (update) {
//			stat1clr.executeUpdate();
//			stat1ins.executeUpdate();
//			commit();
//			for (NotesDB dba: dbas) {
//				System.out.println(dba.getNick());
//				assert dba.dba && !dba.isClosed() : dba;
//				stat1get = dba.sqla("stat1get");
//				ResultSet rs = stat1get.executeQuery();
//				while (rs.next()) {
//					int num = rs.getInt(1);
//					String lang = rs.getString(2);
//				commit();
//			}
//		}
//		// make a lattice
//		int maxAreas = 15000; // get it from Areas
//		int zz[][] = new int[dbas.size()][maxAreas], j = 0;
//		for (NotesDB dba: dbas) {
//			String nick = dba.getNick();
//			Set<String> areas = Area.nickToArea.get(nick);
//			for (int i=0; i<maxAreas;i++) zz[j][i] = 0;
//			for (String s: areas) {
//				zz[j][Area.getCode(s)] += 1; 
//			}
//			j++;
//		}
//
//		ResultSet rs = stat1gethottest.executeQuery();
//		while (rs.next()) {
//			int number = rs.getInt(1);
//			int prio = rs.getInt(2);
//			int area = rs.getInt(3);
//			int cat = rs.getInt(4);
//			int mark = rs.getInt(5);
//			int qty = rs.getInt(6);
//			int q = 0;
//			for (int i=0; i<dbas.size(); i++) 
//				q += zz[i][area];
//			AZ z = new AZ(number, Area.codeToArea.get(area), "UNKNOWN", mark, cat, prio);
//			if (q==0 && qty==0 && add.test(z)) {
//				//System.out.println(String.format("%010d   %s\t%d\t%d", number, Area.codeToArea.get(area), prio, qty));
//				numsToDload.add(number);
//			}
//		}
//		return numsToDload;
//	}


	void stat1upd(List<AZ> fs) throws SQLException {
		assert !dba && !isClosed();
		if (stat1clr==null) stat1clr=sql("stat1clr");
		if (stat1ins==null) stat1ins=sql("stat1ins");
		if (stat1upd==null) stat1upd=sql("stat1upd");
		stat1clr.executeUpdate();
		stat1ins.executeUpdate();
		for (AZ az: fs) {
			stat1upd.setInt(1, az.num);
			if (az.lang=='E') 
				stat1upd.setInt(2, 1);
			else
				stat1upd.setNull(2, java.sql.Types.INTEGER);
			if (az.lang=='D') 
				stat1upd.setInt(3, 1);
			else
				stat1upd.setNull(3, java.sql.Types.INTEGER);
			if (az.lang=='J') 
				stat1upd.setInt(4, 1);
			else
				stat1upd.setNull(4, java.sql.Types.INTEGER);
			if (az.error)
				stat1upd.setInt(5, 1);
			else
				stat1upd.setNull(5, java.sql.Types.INTEGER);
			stat1upd.addBatch();
		}
		stat1upd.executeBatch();
		commit();
	}
	List<AZ> stat1get(Predicate<AZ> add) throws SQLException {
		if (stat1gethottest==null) stat1gethottest=sql("stat1gethottest");
		ResultSet rs = stat1gethottest.executeQuery();
		List<AZ> rez = new ArrayList<AZ>();
		while (rs.next()) {
			int number = rs.getInt(1);
			int prio = rs.getInt(2);
			int area = rs.getInt(3);
			int cat = rs.getInt(4);
			int mark = rs.getInt(5);
			int dled = rs.getInt(6);
			int errors = rs.getInt(7);
			AZ az = new AZ(number, area, mark, cat, prio, errors>0);
			az.dled = dled;
//			System.out.println(String.format("%d\t%d\t%d", az.num, dled, errors));
			if (add.test(az)) rez.add(az);
		}
		return rez;
	}

}
