package rpct.masking;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.log4j.Logger;
import org.apache.myfaces.trinidad.component.UIXValue;
import org.apache.myfaces.trinidad.component.core.nav.CoreCommandButton;
import org.apache.myfaces.trinidad.context.RequestContext;

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
import rpct.db.domain.equipment.EquipmentDAO;
import rpct.db.domain.equipment.EquipmentDAOHibernate;
import rpct.db.domain.equipment.FebConnector;
import rpct.db.domain.equipment.LinkBoard;
import rpct.db.domain.equipment.chamberloaction.BarrelOrEndcap;
import rpct.db.domain.equipment.chamberstrip.ChamberStrip;
import rpct.db.domain.equipment.feblocation.FebLocation;
import rpct.db.domain.hibernate.HibernateContext;
import rpct.db.domain.hibernate.context.simple.SimpleHibernateContextImpl;
import rpct.masking.utils.Division;
import rpct.xdaq.axis.Binary;


public class MaskBean {
	private static Logger logger = Logger.getLogger(MaskBean.class);

	public static class Bit {
		private final String name;
		private boolean value;

		private Bit(String name, boolean value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public boolean getValue() {

			return value;
		}

		public void setValue(boolean value) {
			this.value = value;
		}

		// A bit tagged as dummy:
		private static final String DUMMY_BIT_NAME = "A.DUMMY.BIT";

		public static Bit dummy() {
			return new Bit(DUMMY_BIT_NAME, false);
		}

		public boolean isDummy() {
			return this.name.equals(DUMMY_BIT_NAME);
		}
	}

	/**
	 * The mask (1 bit per strip) of enabled strips
	 */
	private Binary hexMask;

	/**
	 * To keep track of the history Updated when mask changes (besides the ctor
	 * in setSelectedStrips() only)
	 */
	private Binary oldHexMask;

	/**
	 * The mask of connected strips
	 * Immutable once fetched from DB 
	 */
	private Binary hexMaskConnected;

	/**
	 * Tied to the hexMask
	 * Used by the presentation
	 */
	private List<Bit> stripBits = new ArrayList<Bit>();

	public List<Bit> getStripBitsConnected() {
		return stripBitsConnected;
	}

	/**
	 * Tied to the hexMaskConnected
	 * Used by the presentation
	 */
	private List<Bit> stripBitsConnected = new ArrayList<Bit>();

	private final static int NUMBER_OF_STRIP_COLUMNS = 10;

	public int getNumberOfColumns() {
		return NUMBER_OF_STRIP_COLUMNS;
	}

	/**
	 * @return range 0 (NumberOfRows - 1)
	 */
	public List<Integer> getTableRows() {
		final int numRows = getNumberOfStrips() / NUMBER_OF_STRIP_COLUMNS + 1;

		List<Integer> rowIndexes = new ArrayList<Integer>(numRows);
		for (int i = 0; i < numRows; i++)
			rowIndexes.add(i);

		return rowIndexes;
	}

	public MaskBean() throws DataAccessException {
		// load from DB

		loadMaskBytes();
	}

	public String getNewHexMask() {
		syncHexMask();
		return hexMask.toString();
	}

	public String getOldHexMask() {
		return oldHexMask.toString();
	}

	public List<Bit> getStripBits() {
		return stripBits;
	}

	public int getNumberOfStrips() {
		int result = 0;

		result = maskMapper.getNumberMax();

		// Ugly, but fast solution
		// TODO: make it pretty
		switch (result) {
		case 47:
		case 95:
			result ++;
			break;
		case 46:
		case 94:
			result += 2;
			break;
		}
		
		return result;
	}

	/**
	 * Load the mask, corresponding to the current chamber coordinates
	 * @throws DataAccessException 
	 * @throws IllegalArgumentException
	 */
	private void loadMaskBytes() throws DataAccessException,
			IllegalArgumentException {
//		ChamberPositionBean position = (ChamberPositionBean) FacesContext
//				.getCurrentInstance().getExternalContext().getSessionMap().get(
//						"chamberPosition");
	    RequestContext requestContext = RequestContext.getCurrentInstance();
	    ChamberPositionBean position = (ChamberPositionBean) requestContext.getPageFlowScope().get("chamberPosition");

		
		if (position != null) {
			logger.info("loadMaskBytes() of " + position.toString());

			final boolean useDB = true;

			if (useDB) {
				try {
					fetchMaskFromDB(position.getLocation());
				} catch (DataAccessException e) {
					e.printStackTrace();
					System.err.println("Error on canceling session!");
				}
			} else {
				// imitate fetching
				hexMask = new Binary("f10f00000000000000000000");
			}

			System.out.println("hexMask numberOfBits: " + hexMask.getBitNum());
			System.out.println("mapper  numberOfBits: " + getNumberOfStrips());
			oldHexMask = hexMask;
			System.out.println("loadMaskBytes(): oldHexMask set to "
					+ oldHexMask.toString());
		} else {
			throw new IllegalArgumentException("ChamberPositionBean is null!");
		}

		syncStripBits();
	}

	private StringWriter loggerWeb = new StringWriter();

	private void presentLog() {
		// TODO: consider removing 
		//		loggerUI.setValue(logger.toString());
	}

	/**
	 * @author dnikolay
	 *
	 * Keeps the state between loading from and saving to DB   
	 */
	private class DBState {
		private HibernateContext context = null;
		private EquipmentDAO equipmentDAO = null;
		private ConfigurationManager configurationManager = null;
		private SynCoderConf coderConf = null;
		private ConfigurationDAO configurationDAO = null;
		private LinkBoard lb = null;
	}

	private DBState dbState = new DBState();

	/**
	 * @param position Chamber position name
	 * @return 
	 * @return mask
	 * @throws DataAccessException
	 * @throws IllegalArgumentException
	 * 
	 * sets hexMask{,Connected};
	 */
	private void fetchMaskFromDB(String position) throws DataAccessException,
			IllegalArgumentException {
		byte[] dbMaskBytes = null;
		byte[] dbMaskBytesConnected = null;
		// Following rpct-db/src/main/java/rpct/testAndUtils/PutStripMasksToDB.java
		if (dbState.context == null) {
			dbState.context = new SimpleHibernateContextImpl();
			dbState.equipmentDAO = new EquipmentDAOHibernate(dbState.context);
			dbState.configurationDAO = new ConfigurationDAOHibernate(
					dbState.context);
			dbState.configurationManager = new ConfigurationManager(
					dbState.context, dbState.equipmentDAO,
					dbState.configurationDAO);
		}

		try {
			//the source of the configuration data, on which the correction will be applied
			LocalConfigKey inputConfigKey = dbState.configurationManager
					.getDefaultLocalConfigKey();

			Map<String, LinkBoard> chamberBoardsMap = new HashMap<String, LinkBoard>();

			// See CHAMBERLOCATION and FEBLOCATION
			// See http://cmslxr.fnal.gov/lxr/source/DataFormats/MuonDetId/interface/RPCDetId.h

			/* Convert from ChamberPositionBean to rpct-db coordinates (diskOrWheel, layer, sector, subsector, barrelOrEndcap)
			 * BARREL: (section, layer, sector, subsector) -> (diskOrWheel, layer, sector, subsector)
			 * ENDCAP:                                        (diskOrWheel, localetapartition, sector, layer), 
			 *        etaPartition -> rpct-db.subsector is undef
			 */
//			ChamberPositionBean cpBean = (ChamberPositionBean) FacesContext
//					.getCurrentInstance().getExternalContext().getSessionMap()
//					.get("chamberPosition");
		    RequestContext requestContext = RequestContext.getCurrentInstance();
		    ChamberPositionBean cpBean = (ChamberPositionBean) requestContext.getPageFlowScope().get("chamberPosition");

			Division division = cpBean.getDivision();

			Integer diskOrWheel = Integer.valueOf(cpBean.getSection()
					.replaceFirst("\\+", ""));

			Integer layer = null;
			Integer sector = null;
			String subsector = null;
			BarrelOrEndcap barrelOrEndcap = null;

			switch (division) {
			case BARREL:
				barrelOrEndcap = BarrelOrEndcap.Barrel;
				layer = cpBean.getLayerNumber();
				sector = Integer.valueOf(cpBean.getSector());
				subsector = cpBean.getSubsector();
				break;
			case ENDCAP:
				barrelOrEndcap = BarrelOrEndcap.Endcap;
				layer = cpBean.getLayerNumber();
				sector = Integer.valueOf(cpBean.getSector());
				subsector = null;
				break;
			}

			System.out.format("Coordinates of %s: (%d, %d, %d, '%s', %s)%n",
					cpBean.toString(), diskOrWheel, layer, sector, subsector,
					barrelOrEndcap);
			System.out.println("linkboxes size: "
					+ dbState.equipmentDAO.getLinkBoxesByChamberLocation(
							diskOrWheel, layer, sector, subsector,
							barrelOrEndcap).size());
			for (Crate crate : dbState.equipmentDAO
					.getLinkBoxesByChamberLocation(diskOrWheel, layer, sector,
							subsector, barrelOrEndcap)) {
				System.out.format("Got crate: <%s> with %d boards%n", crate
						.toString(), crate.getBoards().size());
				for (Board board : crate.getBoards()) {
					if (board.getType() == BoardType.LINKBOARD) {
						LinkBoard lb = (LinkBoard) board;
						System.out.println("Got lb: " + lb.toString());
						for (String chamberName : lb.getChamberNames()) {
							System.out.println("   Serving " + chamberName);
							if (chamberBoardsMap.containsKey(chamberName)) {
								throw new RuntimeException("Chambername <"
										+ chamberName + "> already inserted!");
							}
							chamberBoardsMap.put(chamberName.replaceFirst(
									"[ ]+", ""), lb);
						}
					}
				}
			}

			System.out.println("Chamber names: "
					+ new TreeMap<String, LinkBoard>(chamberBoardsMap));

			final String chamberName = position.replaceFirst("[ ]+", "");
			dbState.lb = chamberBoardsMap.get(chamberName);

			if (dbState.lb == null)
				throw new IllegalArgumentException("FEB location '"
						+ cpBean.toString() + "' not found in DB!");

			System.out.println("Selected lb: " + dbState.lb.toString());
			loggerWeb.write(String.format(
					"LB name: <b>%s</b> of chamber <b>%s</b>%n", dbState.lb
							.getName(), chamberName));

			// Used in MaskMapper initialization
			// TreeMap used for ordered indexes
			final TreeMap<Integer, Integer> conjugateToChannel = new TreeMap<Integer, Integer>();
			// Map of etaPart to conjugateToChannel
			final Map<String, SortedMap<Integer, Integer>> etaPartConjugateToChanel = new TreeMap<String, SortedMap<Integer,Integer>>();
			conjugateToEtaPart = new HashMap<Integer, String>();

			for (FebLocation fl : dbState.lb.getFEBLocations()) {
				final String etaPartitionFL = fl.getFebLocalEtaPartition();
				logger.debug("etaPartitionFL is '" + etaPartitionFL + "'");
				
				System.out.format("Got fl: (%d, %s, %s)%n", fl.getId(), fl
						.getChamberLocation().getChamberLocationName(), etaPartitionFL);
				// Endcap doesn't have etaPart attached - all (A,B,C) are merged in one big chamber 
				final String chamberNameFL = fl.getChamberLocation().getChamberLocationName() + 
				(cpBean.getDivision() == Division.BARREL ? etaPartitionFL : "");
				
				if (chamberNameFL.equals(chamberName)) {
					logger.debug(String.format(
							"This fl %d matches target chambername %s%n", fl
									.getId(), chamberName));
					logger.debug(String.format("fl %d has %d fcs%n", fl.getId(), fl
							.getFebConnectors().size()));
					
					if (! etaPartConjugateToChanel.containsKey(etaPartitionFL))
						etaPartConjugateToChanel.put(etaPartitionFL, new TreeMap<Integer, Integer>());
					final Map<Integer, Integer> thisEtaPartConjugateToChanel = etaPartConjugateToChanel.get(etaPartitionFL);
					
					for (FebConnector fc : fl.getFebConnectors()) {
						for (ChamberStrip cs : fc.getChamberStrips()) {
							final Integer channelNumber = MaskMapper.CHANNEL
									.getNumber(cs);
							final Integer conjugateNumber = maskMapper
									.getNumber(cs);
							thisEtaPartConjugateToChanel.put(conjugateNumber, channelNumber);
						}
					}
				}
			}

			// Merge etaPartitions of (the sorted)etaPartConjugateToChanel
			int conjugateOffset = 0;
			for (String etaPart: etaPartConjugateToChanel.keySet()) {
				final SortedMap<Integer, Integer> m = etaPartConjugateToChanel.get(etaPart);
				System.out.println("Merging " + m.toString());
				for (Entry<Integer, Integer> e: m.entrySet()) {
					final Integer conjugateNumber = e.getKey() + conjugateOffset;
					final Integer channelNumber = e.getValue();
					
					conjugateToChannel.put(conjugateNumber, channelNumber);
					conjugateToEtaPart.put(conjugateNumber, etaPart);
				}
				if (maskMapper != MaskMapper.CHANNEL)
					// Only non-CHANNEL mappers have to be rearanged
					// It is assumed that conjugateNumber returned by maskMapper starts (at least) from 1!
					//    (if from 0, overlapping of the last previous and the first next will occur)
					conjugateOffset += m.lastKey();
			}
			
			
			maskMapper.setMapConjugateToChannel(conjugateToChannel);

			System.out.println(maskMapper.longDescription());

			StaticConfiguration configuration = dbState.configurationManager
					.getConfiguration(dbState.lb.getSynCoder(), inputConfigKey);

			// TODO: move the verification upper
			if (configuration != null) {
				dbState.coderConf = (SynCoderConf) configuration;
				dbMaskBytes = dbState.coderConf.getInChannelsEna();
				dbMaskBytesConnected = dbState.lb.getConnectedStripsMask();

				hexMaskConnected = new Binary(dbMaskBytesConnected);

				loggerWeb
						.write(String
								.format(
										"Masks read:%n"
												+ "   * <tt>ConnectedStripsMask():</tt> <big>%s</big>%n"
												+ "   * <tt>InChannelsEna():&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</tt> <big>%s</big>%n",
										new Binary(dbMaskBytesConnected)
												.toString().toUpperCase(),
										new Binary(dbMaskBytes).toString()
												.toUpperCase()));
				// final mask is the connected AND enabled
				for (int i = 0; i < dbMaskBytes.length; i++) {
					dbMaskBytes[i] = (byte) (dbMaskBytes[i] & dbMaskBytesConnected[i]);
				}

				hexMask = new Binary(dbMaskBytes);
				hexMaskConnected = new Binary(dbMaskBytesConnected);
			} else {
				loggerWeb.write("no configuration found for the LB "
						+ dbState.lb.getName() + " !!!!!!!!!!!!!!!!!!\n");
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
			dbState.context.rollback();
			loggerWeb.write("context.rollback()\n");
		} finally {
			dbState.context.closeSession();
		}

		presentLog();
	}

	/**
	 * save mask to DB
	 * @throws DataAccessException 
	 */
	public void saveMask(ActionEvent ae) throws DataAccessException {
		syncHexMask();
		// Following rpct-db/src/main/java/rpct/testAndUtils/PutStripMasksToDB.java
		final boolean saveToDB = true;

		loggerWeb.write(String.format("[saveMask] old: %s, new: %s%n", oldHexMask
				.toString().toUpperCase(), hexMask.toString().toUpperCase()));
		presentLog();

		// TODO: Save if the new mask is different from the old one
		// oldHexMask doesn't change with hexMask - stays the same as initially read from DB?
		if (Arrays.equals(hexMask.getBytes(), oldHexMask.getBytes())) {
			loggerWeb
					.write(String
							.format("   * newMask = oldMask.%n   * <big>SAVING IGNORED!</big>%n"));
			presentLog();

			return;
		}

		if (saveToDB) {
			SynCoderConf newCoderConf = new SynCoderConf(dbState.coderConf);

			final byte[] channelBytes = hexMask.getBytes();

			newCoderConf.setInChannelsEna(channelBytes);
			loggerWeb.write(String.format("   * Trying to save %s ...%n",
					new Binary(newCoderConf.getInChannelsEna()).toString()
							.toUpperCase()));
			presentLog();

			try {
				dbState.configurationDAO.saveObject(newCoderConf);
				//the key, under which the corrected data will be stored                                                                       
				LocalConfigKey outputConfigKey = dbState.configurationManager
						.getDefaultLocalConfigKey();

				dbState.configurationManager
						.assignConfiguration(dbState.lb.getSynCoder(),
								newCoderConf, outputConfigKey.getName());
				loggerWeb.write(String.format("   * New mask saved%n"));
			} catch (DataAccessException e) {
				e.printStackTrace();
				loggerWeb.write(e.getMessage());
				loggerWeb.flush();
				dbState.context.rollback();
			} finally {
				dbState.context.closeSession();
				loggerWeb.write(String.format("   * DB Session closed%n"));
			}
		} else {
			loggerWeb.write(String.format("*Dummy* saving new mask %s done.",
					hexMask.toString().toUpperCase()));
		}

		presentLog();
	}

	/**
	 * Used to convert from/to channel number
	 *
	 * A variant of "State" Pattern
	 * A "Decorator" of a subset of Map
	 */
	private enum MaskMapper {
		CHANNEL {
			@Override
			public int getNumber(ChamberStrip cs) {
				final int lbInputNum = cs.getFebConnector()
						.getLinkBoardInputNum() - 1;
				final int cableChannelNum = cs.getCableChannelNum();
				final int channelsPerFEB = 16;
				final int channelNumber = lbInputNum * channelsPerFEB
						+ cableChannelNum;

				return channelNumber;
			}
		},
		STRIP {
			@Override
			public int getNumber(ChamberStrip cs) {
				return cs.getChamberStripNumber();
			}
		},
		STRIPCMS {
			@Override
			public int getNumber(ChamberStrip cs) {
				return cs.getCmsStripNumber();
			}
		};

		// TODO: TreeMap is good only for getNumberMax():
		//       consider replacing it with a HashMap for a better performance
		private TreeMap<Integer, Integer> mapConjugateToChannel;

		public void setMapConjugateToChannel(
				TreeMap<Integer, Integer> mapConjugateToChannel) {
			this.mapConjugateToChannel = mapConjugateToChannel;

		}

		// Delegate to Map
		public Set<Integer> keySet() {
			return mapConjugateToChannel.keySet();
		}

		public Integer get(Integer key) {
			return mapConjugateToChannel.get(key);
		}

		public boolean containsKey(Integer key) {
			return mapConjugateToChannel.containsKey(key);
		}

		public String longDescription() {
			return "MaskMapper."
					+ this.name()
					+ " map: "
					+ (mapConjugateToChannel == null ? null
							: mapConjugateToChannel.toString());
		}

		// Delegation ends

		// Specific methods
		public int getNumberMax() {
			// this is a TreeMap - the keys are ordered
			// Not good when in "Channel" view if the last channel is not connect
			return mapConjugateToChannel.lastKey();
		}

		abstract int getNumber(ChamberStrip cs);
	}

	private MaskMapper maskMapper = MaskMapper.STRIP;

	public MaskMapper getMaskMapper() {
		return maskMapper;
	}

	public void setMaskMapper(MaskMapper maskMapper) {
		System.out.format("setMaskMapper(%s)%n", maskMapper);
		this.maskMapper = maskMapper;
	}

	/**
	 * Synchronize hexMask to match stripBits
	 */
	private void syncHexMask() {
		final byte[] maskBytes = hexMask.getBytes().clone();

		for (Integer conjugate : maskMapper.keySet()) {
			final Bit bit = stripBits.get(conjugate);
			if (!bit.isDummy()) {
				final Integer channel = maskMapper.get(conjugate);
				if (channel == null)
					throw new RuntimeException("No mapping for conjChannel "
							+ conjugate);
				final int indexBit = channel - 1;
				Binary.setBit(maskBytes, indexBit, bit.getValue() ? 1 : 0);
			}
		}

		oldHexMask = hexMask;
		System.out.println("syncHexMask(): oldHexMask set to "
				+ oldHexMask.toString());
		hexMask = new Binary(maskBytes, oldHexMask.getBitNum());

		loggerWeb.write(String.format("<i>mask synced to</i> %s%n", hexMask
				.toString().toUpperCase()));
	}

	// TODO move to Utils class
	/**
	 * @param i argument to step
	 * @param width step width
	 * @return step height: 1 in [0, width], 2 in [width + 1, 2 * width] ...
	 */
	private static int step(int i, int width) {
		return i / width + ((i != 0 && i % width == 0) ? 0 : 1);
	}

	/**
	 * Synchronize stripBits to match hexMask
	 */
	private void syncStripBits() {
		stripBits.clear();
		stripBitsConnected.clear();

		// because of the presentation, strips are indexed starting from 1;
		stripBits.add(Bit.dummy());
		stripBitsConnected.add(Bit.dummy());

		final int conjugateMax = maskMapper.getNumberMax();
		final boolean appendEtaPart = getNumberOfEtaPartitions() > 1;
		for (int conjugate = 1; conjugate <= conjugateMax; conjugate++) {
			Bit bit = null;
			Bit bitConnected = null;
			if (maskMapper.containsKey(conjugate)) {
				final Integer channel = maskMapper.get(conjugate);
				if (channel == null)
					throw new RuntimeException("Conjugate channel " + conjugate
							+ " has corresponding null channel");

				final int bitIndex = channel - 1; //DB mask numbering starts from 1, see Linkboard.getConnectedStripsMask() 

				String bitName = String.valueOf(conjugate);
				if (appendEtaPart)
					bitName = bitName + " " + conjugateToEtaPart.get(conjugate);
				final boolean bitValue = hexMask.getBits(bitIndex, 1) != 0;
				bit = new Bit(bitName, bitValue);

				final boolean bitConnectedValue = hexMaskConnected.getBits(
						bitIndex, 1) != 0;
				bitConnected = new Bit(bitName, bitConnectedValue);
			} else { // conjugate is not mapped
				bit = Bit.dummy();
				bitConnected = Bit.dummy();
			}
			stripBits.add(bit);
			stripBitsConnected.add(bitConnected);
		}

		// Presentation requires stripBits.size() a multiple of  NUMBER_OF_STRIP_COLUMNS
		// Add more artificial strips that will not be rendered
		for (int conjugate = conjugateMax + 1; conjugate <= MaskBean.step(
				conjugateMax, NUMBER_OF_STRIP_COLUMNS)
				* NUMBER_OF_STRIP_COLUMNS; conjugate++) {
			stripBits.add(Bit.dummy());
			stripBitsConnected.add(Bit.dummy());
			System.out.println("syncStripBits(): Added Bit.dummy() for conj. "
					+ conjugate);
		}
	}

	public String[] getEtaPartitions() {
		return new HashSet<String>(conjugateToEtaPart.values()).toArray(new String[]{});
	}
	public int getNumberOfEtaPartitions() {
		return getEtaPartitions().length;
	}

	public void doAllOfEtaPartition(ActionEvent ae) {
		System.out.println(ae);
		final String btnId = ((CoreCommandButton) ae.getSource()).getId();
		// Hack the Id to get the button index
		final String btnIdIndex = btnId.substring(btnId.length() - 1);
		int etaPartIndex = 0;
		
		try {
			etaPartIndex = Integer.parseInt(btnIdIndex);
		} catch (NumberFormatException e) {
			//TODO: Warn?
		} 
		final String etaPartition = getEtaPartitions()[etaPartIndex];
		
		if (btnId.startsWith("btnEnableAll"))
			new EtaPartLooperEnabler().loop(etaPartition);
		else if (btnId.startsWith("btnDisableAll"))
			new EtaPartLooperDisabler().loop(etaPartition);
		else 
			throw new IllegalArgumentException("doAllOfEtaPartition(): Can't handle button id " + btnId);
	}
	/**
	 * Loop over eta partition and set stripBits to bitValueToSet(conjugate)
	 * Template Method
	 */
	private abstract class EtaPartLooper {
		public void loop(String etaPartition) {
			for (int conjugate = 1; conjugate <= stripBits.size(); conjugate++) {
				if (maskMapper.containsKey(conjugate) && 
						conjugateToEtaPart.get(conjugate).equals(etaPartition) && // is in the right etaPartition
						stripBitsConnected.get(conjugate).getValue() // strip is connected
						) {
					final Bit bit = stripBits.get(conjugate);
					bit.setValue(bitValueToSet(conjugate));
				}
			}
		}
		abstract boolean bitValueToSet(int conjugate);
	}
	private final class EtaPartLooperEnabler extends EtaPartLooper{
		@Override
		boolean bitValueToSet(int conjugate) {
			return true;
		}
	}
	private final class EtaPartLooperDisabler extends EtaPartLooper{
		@Override
		boolean bitValueToSet(int conjugate) {
			return false;
		}
	}
	
	public void maskMapperChanged(ValueChangeEvent event)
			throws DataAccessException, IllegalArgumentException {
		final MaskMapper newMaskMapper = (MaskMapper) ((UIXValue) event
				.getSource()).getValue();

		System.out.println("maskMapperChanged(): newMaskMapper = "
				+ newMaskMapper);
		maskMapper = newMaskMapper;
		loadMaskBytes();
	}

	/**
	 * @return String of hexadecimal digit characters (two per byte). The leading
	 * zeroes (if any) are presented.
	 */
	@Override
	public String toString() {
		final String s = hexMask.toString();

		return s;
	}

	private boolean numbersOn = false;

	// etaPartition of a conjugate
	private Map<Integer, String> conjugateToEtaPart;

	public void setNumbersOn(boolean numbersOn) {
		this.numbersOn = numbersOn;
	}

	public boolean isNumbersOn() {
		return numbersOn;
	}

	public void setLogtxt(String logtxt) {
		loggerWeb.write(logtxt);
	}

	public String getLogtxt() {
		return loggerWeb.toString();
	}

	public List<DBDriver.ParamModified> getMaskModifications() throws DataAccessException {
		// TODO: merge DbState to DBDriver
		DBDriver dbDriver = new DBDriver();
		// TODO: add configure(context, ...) to use already set configuration settings 
		dbDriver.configure();
		
		final int synCoderId = dbState.lb.getSynCoder().getId();
		return dbDriver.getMaskHistory(synCoderId);
	}
}