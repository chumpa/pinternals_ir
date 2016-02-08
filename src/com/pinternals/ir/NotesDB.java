package com.pinternals.ir;

import java.io.IOException;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
		if (init) {
			if (!dba) {
				for (String s: ddlrb.keySet()) if (s.startsWith("0")) {
					System.out.println(s);
					PreparedStatement ps = con.prepareStatement(ddl(s));
					ps.execute();
				}
			} else
				for (String s: ddlrb.keySet()) if (s.startsWith("a")) 
					con.prepareStatement(ddl(s)).execute();
		}
		con.commit();
		ResourceBundle rb = dba ? sqlab : sqlrb;
		if (checksql) {
			for (String s: rb.keySet()) {
				System.out.println(s);
				PreparedStatement ps = null;
				String sq = rb.getString(s);
				try {
					ps = con.prepareStatement(sq, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
				} catch (SQLException e) {
					System.err.println(String.format("database %s cannot compile '%s'%n%s%n%s", pat, s, sq, e.getMessage()));
				} finally {
					if (ps!=null) ps.close();
				}
			}
		}
		for (String s: rb.keySet()) if (s.startsWith("00open")) con.prepareStatement(rb.getString(s)).execute();
		if (!dba) pairToMap3(sql("01types"), types);
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
			AZ z = new AZ(rs.getInt(1), area, rs.getString(2), rs.getInt(4));
			az.add(z);
		}
		cl.executeUpdate();
		commit();
		return az;
		
	}
	
	PreparedStatement ps3b = null;
	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	List<AZ> getNotesDBA() throws SQLException {
		assert dba && !isClosed();
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
			AZ az = new AZ(num, p.getComponentKey(), p.getSapNotesKey(), tp);
			az.mprop = p;
//			az.langMaster = langMaster;
//			az.askdate = askdate;
			azl.add(az);
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
				"sidecauins", "sidesolins", "productins", "langins", "facetins", "facetupd"};
		if (dbaPs.size()==0) for (String s: pk) dbaPs.put(s, sqla(s));
		com.sap.lpad.Properties q, p = en.getContent().getProperties();

		List<Entry<String,Object[]>> todo = new ArrayList<Entry<String,Object[]>>();
		Object o[];
		Object[] facets = new Object[]{
			Integer.parseInt(p.getSapNotesNumber()), p.getLanguage(), Integer.parseInt(p.getVersion()),   
			null, null, null, null, null, null, null, null, null, null, null, null};

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

//		int a = fac[idxOneBased-1]==null ? 0 : (Integer)fac[idxOneBased-1];
//		fac[idxOneBased-1] = new Integer(a+1);

		for (com.sap.lpad.Link l: en.getLink()) {
			if ("self".equals(l.getRel())) continue;
			List<com.sap.lpad.Entry> ex = l.getInline().getFeed().getEntry();
			assert ex!=null;
//			System.out.println(l.getTitle());
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
							q.getName(), q.getSp(), q.getVersion()};
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
				default:
					throw new RuntimeException("NIY " + l.getTitle() + " " + ex.size());
				}
			}
		}
//		queryins = insert into Queries(NotesNumber,Language,Version,\
//				askdate,gotlanguage,gotversion,rc,answer) \
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
}
