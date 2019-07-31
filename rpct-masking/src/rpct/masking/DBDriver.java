package rpct.masking;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;

import rpct.db.DataAccessException;
import rpct.db.domain.configuration.ConfigurationDAO;
import rpct.db.domain.configuration.ConfigurationDAOHibernate;
import rpct.db.domain.configuration.ConfigurationManager;
import rpct.db.domain.configuration.LocalConfigKey;
import rpct.db.domain.configuration.StaticConfiguration;
import rpct.db.domain.configuration.SynCoderConf;
import rpct.db.domain.equipment.Board;
import rpct.db.domain.equipment.BoardType;
import rpct.db.domain.equipment.Crate;
import rpct.db.domain.equipment.CrateType;
import rpct.db.domain.equipment.EquipmentDAO;
import rpct.db.domain.equipment.EquipmentDAOHibernate;
import rpct.db.domain.equipment.FebConnector;
import rpct.db.domain.equipment.LinkBoard;
import rpct.db.domain.equipment.chamberloaction.BarrelOrEndcap;
import rpct.db.domain.equipment.chamberstrip.ChamberStrip;
import rpct.db.domain.equipment.feblocation.FebLocation;
import rpct.db.domain.hibernate.HibernateContext;
import rpct.db.domain.hibernate.context.simple.SimpleHibernateContextImpl;
import rpct.xdaq.axis.Binary;



public class DBDriver {
	private static Logger logger = Logger.getLogger(DBDriver.class);

	private HibernateContext context = null;
	private EquipmentDAO equipmentDAO = null;
	private ConfigurationManager configurationManager = null;
	private ConfigurationDAO configurationDAO = null;
	private LocalConfigKey inputConfigKey = null;

	// ALL_LINKBOARDS with getAllLinkBoards() is a singleton
	private List<LinkBoardWithConfiguration> ALL_LINKBOARDS = null;
	
	// ALL_CHAMBERS with getAllChambers() is a singleton
	private List<ChamberWithMasks> ALL_CHAMBERS = null;
	
	public HibernateContext getContext() {
		return context;
	}
	
	public void configure() throws DataAccessException {
		// Following rpct-db/src/main/java/rpct/testAndUtils/PutStripMasksToDB.java
		if (context == null) {
			context = new SimpleHibernateContextImpl();
			equipmentDAO = new EquipmentDAOHibernate(context);
			configurationDAO = new ConfigurationDAOHibernate(
					context);
			configurationManager = new ConfigurationManager(
					context, equipmentDAO,
					configurationDAO);

			//the source of the configuration data
			inputConfigKey = configurationManager.getDefaultLocalConfigKey();
		}
	}
	
	public void free() {
		ALL_CHAMBERS = null;
		ALL_LINKBOARDS = null;
	}

	public List<LinkBoardWithConfiguration> getAllLinkBoards() throws DataAccessException {
		MaskingUtils.log("DBDriver.getAllLinkBoards() starts");

		if (ALL_LINKBOARDS != null)
			return ALL_LINKBOARDS;

		final List<LinkBoardWithConfiguration> result = new ArrayList<LinkBoardWithConfiguration>();
		List<? extends Crate> crates = null;
		
		final boolean useShortList = true;
		
		if (useShortList) {
			// report only W-2/RB1in/1Forward
			final Integer diskOrWheel = -2;
			final Integer layer = 1;
			final Integer sector = 1;
			final String subsector = "";
			final BarrelOrEndcap barrelOrEndcap = BarrelOrEndcap.Barrel;
			
			crates = equipmentDAO.getLinkBoxesByChamberLocation(diskOrWheel, layer, sector,
					subsector, barrelOrEndcap);
		}
		else {
			crates = equipmentDAO.getCratesByType(CrateType.LINKBOX, false);
		}
		
		for(Crate crate : crates) {
			for(Board board : crate.getBoards()) {
        		if(board.getType() == BoardType.LINKBOARD) {
        			final LinkBoard lb = (LinkBoard) board;

        			StaticConfiguration configuration = configurationManager
					.getConfiguration(lb.getSynCoder(), inputConfigKey);

        			if (configuration != null) {
						result.add(new LinkBoardWithConfiguration(lb, (SynCoderConf) configuration));
					}
					else {
						System.out.println("LB " + lb.getName() + " has null configuration!");
					}
        		}
        	}
        }

		// sort by name
		Collections.sort(result);

		ALL_LINKBOARDS = result;
		
		MaskingUtils.log("DBDriver.getAllLinkBoards() ends");
		
		return result;
	}

	public List<ChamberWithMasks> getAllChambers() throws DataAccessException {
		MaskingUtils.log("DBDriver.getAllChambers() starts");

		if (ALL_CHAMBERS != null)
			return ALL_CHAMBERS;

		final List<ChamberWithMasks> result =	new ArrayList<ChamberWithMasks>();

		final List<LinkBoardWithConfiguration> allLBoards = getAllLinkBoards();
		
		for (LinkBoardWithConfiguration lb: allLBoards) {
			final LinkBoard lbRaw = lb.getRawLinkBoard();
			
			final Set<ChamberWithMasks> lbChambers = new HashSet<ChamberWithMasks>();
			for (FebLocation fl: lbRaw.getFEBLocations()) {
				final String chamberName = DBDriver.chamberNameOf(fl);
				final String chamberID = fl.getChamberLocation().getId() +
				// Endcap chamber are merged over all etaPartition into one (see also  chamberNameOf())
				(fl.getChamberLocation().getBarrelOrEndcap() == BarrelOrEndcap.Barrel ? fl.getFebLocalEtaPartition() : "");

				final String chamberEnabledStripsMask = DBDriver.getMaskEnabledStrips(lb, chamberName);  
				
				lbChambers.add(new ChamberWithMasks(chamberID, chamberName, lb.getName(), chamberEnabledStripsMask));
			}
			
			result.addAll(lbChambers);
		}
		
		// sort by name
		Collections.sort(result);
		
		ALL_CHAMBERS = result;

		MaskingUtils.log("DBDriver.getAllChambers() ends");

		return result;
	}

	private static String getMaskEnabledStrips(LinkBoardWithConfiguration lb, String chamberName) {
		final SortedMap<Integer, Integer> stripToChannel =  getStripToChannelMap(lb, chamberName);
		final Binary enabledChannelsLB = new Binary(lb.getEnabledChannelsMask());

		final int stripBitNumberMax = stripToChannel.lastKey() - 1;
		final byte[] enabledStripsBytes = new byte[stripBitNumberMax / Byte.SIZE + 1];
		
		for (int stripNumber: stripToChannel.keySet()) {
			final int channelNumber = stripToChannel.get(stripNumber);
			final int stripBitNumber = stripNumber - 1;
			final int channelBitNumber = channelNumber - 1;
			final int enabledChannelBitValue = (enabledChannelsLB.getBits(channelBitNumber, 1) == 0 ? 0 : 1);

			Binary.setBit(enabledStripsBytes, stripBitNumber, enabledChannelBitValue);
		}
		
		return new Binary(enabledStripsBytes).toString().toUpperCase();
	}

	private static SortedMap<Integer, Integer> getStripToChannelMap(LinkBoardWithConfiguration lb, String chamberName) {
		String canonChamberName = DBDriver.canonicalChamberName(chamberName);

		// The mapping strip => channel
		final TreeMap<Integer, Integer> stripToChannel = new TreeMap<Integer, Integer>();
		// Map of etaPart to stripToChannel
		final Map<String, SortedMap<Integer, Integer>> etaPartStripToChanel = new TreeMap<String, SortedMap<Integer,Integer>>();


		for (FebLocation fl : lb.getRawLinkBoard().getFEBLocations()) {
			final String etaPartitionFL = fl.getFebLocalEtaPartition();
		
			//			System.out.format("Got fl: (%d, %s, %s)%n", fl.getId(), fl
			//					.getChamberLocation().getChamberLocationName(), etaPartitionFL);
			// Endcap doesn't have etaPart attached - all (A,B,C) are merged in one big chamber 
			final String canonChamberNameFL = DBDriver.canonicalChamberName(chamberNameOf(fl));
			
			if (! canonChamberNameFL.equals(canonChamberName))
				continue;
			
			//			System.out.format("This fl %d matches target chambername %s%n", fl.getId(), chamberName);
			
			if (! etaPartStripToChanel.containsKey(etaPartitionFL))
				etaPartStripToChanel.put(etaPartitionFL, new TreeMap<Integer, Integer>());
			final Map<Integer, Integer> thisEtaPartStripToChanel = etaPartStripToChanel.get(etaPartitionFL);
			
			for (FebConnector fc : fl.getFebConnectors()) {
				//				System.out.format("fc: (id = %d, lbInputNum = %d, cs_num = %d)%n",
				//						fc.getId(), fc.getLinkBoardInputNum(),
				//						fc.getChamberStrips().size());

				for (ChamberStrip cs : fc.getChamberStrips()) {
					final Integer channelNumber = DBDriver.getChannelNumber(cs);
					final Integer stripNumber = DBDriver.getStripNumber(cs);
					
					thisEtaPartStripToChanel.put(stripNumber, channelNumber);
				}
			}
		}
		
		// Merge etaPartitions of (the sorted)etaPartStripToChanel
		int stripOffset = 0;
		for (SortedMap<Integer, Integer> m: etaPartStripToChanel.values()) {
			//			System.out.println("Merging " + m.toString());
			for (Entry<Integer, Integer> e: m.entrySet()) {
				final Integer stripNumber = e.getKey() + stripOffset;
				final Integer channelNumber = e.getValue();
				
				stripToChannel.put(stripNumber, channelNumber);
			}
			stripOffset += m.lastKey();
		}
		
		return stripToChannel;
	}

	public static String canonicalChamberName(String chamberName) {
		return chamberName.replaceAll("[ ]+", "");
	}
	
	// In compliance with LinkBoard.getChamberNames() format
	public static String chamberNameOf(FebLocation fl) {
		String result =	fl.getChamberLocation().getChamberLocationName();
		
		if(fl.getChamberLocation().getBarrelOrEndcap() == BarrelOrEndcap.Barrel) {
			result += " " + fl.getFebLocalEtaPartition();
		}
		
		return result;
	}
	
	public static int getChannelNumber(ChamberStrip cs) {
		final int lbInputNum = cs.getFebConnector().getLinkBoardInputNum() - 1;
		final int cableChannelNum = cs.getCableChannelNum();
		final int channelsPerFEB = 16;
		final int channelNumber = lbInputNum * channelsPerFEB
		+ cableChannelNum;

		return channelNumber;
	}
	
	public static int getStripNumber(ChamberStrip cs) {
		return cs.getChamberStripNumber();
	}
	
	public static int getCMSStripNumber(ChamberStrip cs) {
		return cs.getCmsStripNumber();
	}
	
	public static class ParamModified {
		private final Object paramVal;
		private final Timestamp modified;
		private final String lckName;
		// TODO: add chipId to avoid ParamModified anonymousity
		
		public ParamModified(Object paramVal, Timestamp modified, String lckName) {
			this.paramVal = paramVal;
			this.modified = modified;
			this.lckName = lckName;
		}

		public Object getParamVal() {
			return paramVal;
		}
		public Timestamp getModified() {
			return modified;
		}
		public String getLckName() {
			return lckName;
		}

		private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		/**
		 * @return DATE_FORMAT formatted timestamp - fractional seconds are truncated!
		 */
		public String getFormattedModified() {
			return DATE_FORMAT.format(modified);
		}

		@Override
		public String toString() {
			return ParamModified.class + " (" + modified + ", " + paramVal + ", " + lckName + ")";
		}
	}

	public List<ParamModified> getMaskHistory(int chipId) throws HibernateException, DataAccessException {
		// TODO: remove the {lag,lead}_modified from the query string
		// TODO: switch to entity query, see http://docs.jboss.org/hibernate/core/3.3/reference/en/html/querysql.html#d0e13696
		final String query_string = "" + 
		"select chipid, paramval, modified, lckname" +
		"  from (" +
		"    select chipid, lag(paramval) over (partition by chipid order by modified desc) lag_paramval," +
		"           lead(paramval) over (partition by chipid order by modified desc) lead_paramval," +
		"           paramval, modified, lckname," + 
		"           lag(modified) over (partition by chipid order by modified desc) lag_modified," +
		"           lead(modified) over (partition by chipid order by modified desc) lead_modified" +
		"      from (" +
		"        select cca.chip_chipid chipid, cc.inchannelsena paramval, cca.creationdate modified, lck.name lckname" +
		"          from linkchipconf cc" +
		"          join chipconfassignment cca" +
		"               on cca.sc_staticconfigurationid = cc.linkchipstaticconfid" +
		"          join chip" +
		"               on chip.chipid = cca.chip_chipid" +
        "          join localconfigkey lck" +
        "               on lck.localconfigkeyid = cca.lck_localconfigkeyid" +
		"         where chipid = :id" +
		"      order by modified desc" +
		"        )" +
		"       ) " + 
		"where lead_paramval is null" +
		"      or lead_paramval <> paramval " +
		"order by 1, 3 desc";
		
		logger.debug("created query:\n" + query_string);
		final SQLQuery query = context.currentSession().createSQLQuery(query_string);
		query
		.addScalar("CHIPID", Hibernate.LONG)
		.addScalar("PARAMVAL", Hibernate.STRING)
		.addScalar("MODIFIED", Hibernate.TIMESTAMP)
		.addScalar("LCKNAME", Hibernate.STRING)
		.setParameter("id", chipId, Hibernate.INTEGER);

		List<ParamModified> result = new ArrayList<ParamModified>();

		for (Object every: query.list()) {
			Object[] row = (Object[]) every;

			final ParamModified pm = new ParamModified((String) row[1], (Timestamp) row[2], (String) row[3]); 
			logger.debug("chipId " + row[0] + ": " + pm);
			result.add(pm);
		}
		
		return result;
	}
}