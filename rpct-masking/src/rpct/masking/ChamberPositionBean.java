package rpct.masking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.apache.myfaces.trinidad.component.core.input.CoreInputText;
import org.apache.myfaces.trinidad.component.core.input.CoreSelectOneChoice;
import org.apache.myfaces.trinidad.component.UIXComponentBase;
import org.apache.myfaces.trinidad.component.UIXValue;
import org.apache.myfaces.trinidad.context.RequestContext;

import rpct.masking.utils.ChamberPosition;
import rpct.masking.utils.Division;
import rpct.masking.utils.EtaPartition;
import rpct.masking.utils.Layer;
import rpct.masking.utils.Section;
import rpct.masking.utils.Sector;
import rpct.masking.utils.Subsector;

public class ChamberPositionBean {
	private static Logger logger = Logger.getLogger(ChamberPositionBean.class);
	
	// The collection of all(String) values used in SelectItem
	private static final Map<Division, Map<ChamberPosition.Coordinate, List<String>>> SELECTIONS = new HashMap<Division, Map<ChamberPosition.Coordinate, List<String>>>();
	// SELECTIONS initialization
	static {
		// === BARREL ===
		final List<String> BARREL_SECTION_LIST = new ArrayList<String>();
		final List<String> BARREL_LAYER_LIST = Arrays.asList("1in", "1out",
				"2in", "2out", "3", "4");
		final List<String> BARREL_SECTOR_LIST = new ArrayList<String>();
		final List<String> BARREL_SUBSECTOR_LIST = Arrays.asList("", "-", "+",
				"--", "++");
		final List<String> BARREL_ETAPARTITION_LIST = Arrays.asList("Forward",
				"Central", "Backward");
		{
			// Sections
			final int MAX_SECTION = 2;
			final int MIN_SECTION = -MAX_SECTION;
			for (int i = MIN_SECTION; i <= MAX_SECTION; i++) {
				// "%+d" on 0 gives "+0" but "0" is required
				BARREL_SECTION_LIST.add(String.format((i != 0 ? "%+d" : "%d"),
						i));
			}

			// Sectors
			final int NUM_SECTORS = 12;
			for (int i = 1; i <= NUM_SECTORS; i++)
				BARREL_SECTOR_LIST.add(String.valueOf(i));
		}

		// === ENDCAP ===
		final List<String> ENDCAP_SECTION_LIST = new ArrayList<String>();
		final List<String> ENDCAP_LAYER_LIST = Arrays.asList("1", "2", "3");
		final List<String> ENDCAP_SECTOR_LIST = new ArrayList<String>();
		final List<String> ENDCAP_SUBSECTOR_LIST = Arrays.asList("");
		final List<String> ENDCAP_ETAPARTITION_LIST = Arrays.asList("");
		{
			// Sections
			final int MAX_SECTION = 4;
			final int MIN_SECTION = -MAX_SECTION;
			for (int i = MIN_SECTION; i <= MAX_SECTION; i++) {
				// no 0 section
				if (i == 0)
					continue;
				ENDCAP_SECTION_LIST.add(String.format("%+d", i));
			}

			// Sectors
			final int NUM_SECTORS = 36;
			for (int i = 1; i <= NUM_SECTORS; i++)
				ENDCAP_SECTOR_LIST.add(String.valueOf(i));
		}

		final Map<ChamberPosition.Coordinate, List<String>> BARREL_LIST = new HashMap<ChamberPosition.Coordinate, List<String>>();

		// TODO: loop over enum Coordinate: make BARREL_coordinate_LIST a map or directly use  SELECIONS
		BARREL_LIST.put(ChamberPosition.Coordinate.SECTION, BARREL_SECTION_LIST);
		BARREL_LIST.put(ChamberPosition.Coordinate.LAYER, BARREL_LAYER_LIST);
		BARREL_LIST.put(ChamberPosition.Coordinate.SECTOR, BARREL_SECTOR_LIST);
		BARREL_LIST.put(ChamberPosition.Coordinate.SUBSECTOR, BARREL_SUBSECTOR_LIST);
		BARREL_LIST.put(ChamberPosition.Coordinate.ETAPARTITION, BARREL_ETAPARTITION_LIST);

		final Map<ChamberPosition.Coordinate, List<String>> ENDCAP_LIST = new HashMap<ChamberPosition.Coordinate, List<String>>();
		ENDCAP_LIST.put(ChamberPosition.Coordinate.SECTION, ENDCAP_SECTION_LIST);
		ENDCAP_LIST.put(ChamberPosition.Coordinate.LAYER, ENDCAP_LAYER_LIST);
		ENDCAP_LIST.put(ChamberPosition.Coordinate.SECTOR, ENDCAP_SECTOR_LIST);
		ENDCAP_LIST.put(ChamberPosition.Coordinate.SUBSECTOR, ENDCAP_SUBSECTOR_LIST);
		ENDCAP_LIST.put(ChamberPosition.Coordinate.ETAPARTITION, ENDCAP_ETAPARTITION_LIST);

		SELECTIONS.put(Division.BARREL, BARREL_LIST);
		SELECTIONS.put(Division.ENDCAP, ENDCAP_LIST);
	}

	private Division division;

	private String layer;
	private CoreSelectOneChoice layerUI;

	private String section;
	private CoreSelectOneChoice sectionUI;

	private String sector;
	private CoreSelectOneChoice sectorUI;

	private String subsector;
	private CoreSelectOneChoice subsectorUI;

	private String etaPartition;
	private CoreSelectOneChoice etaPartitionUI;
	
	private String location;
	private CoreInputText locationUI;
	
	private Map<ChamberPosition.Coordinate, List<SelectItem>> coordinateList = new HashMap<ChamberPosition.Coordinate, List<SelectItem>>();

	private static final Map<Division, Map<ChamberPosition.Coordinate, String>> SELECTION_LABELS = new HashMap<Division, Map<ChamberPosition.Coordinate, String>>();
	static {
		final Map<ChamberPosition.Coordinate, String> BARREL_LABELS = new HashMap<ChamberPosition.Coordinate, String>() {
			private static final long serialVersionUID = -7254668338081110688L;
			{
				put(ChamberPosition.Coordinate.SECTION, "Wheel");
				put(ChamberPosition.Coordinate.LAYER, "RB");
				put(ChamberPosition.Coordinate.SECTOR, "Sector");
				put(ChamberPosition.Coordinate.SUBSECTOR, "Subsector");
				put(ChamberPosition.Coordinate.ETAPARTITION, "Eta part.");
			}
		};

		final Map<ChamberPosition.Coordinate, String> ENDCAP_LABELS = new HashMap<ChamberPosition.Coordinate, String>() {
			private static final long serialVersionUID = 362680990490002296L;
			{
				put(ChamberPosition.Coordinate.SECTION, "Disk");
				put(ChamberPosition.Coordinate.LAYER, "Ring");
				put(ChamberPosition.Coordinate.SECTOR, "Sector");
			}
		};

		SELECTION_LABELS.put(Division.BARREL, BARREL_LABELS);
		SELECTION_LABELS.put(Division.ENDCAP, ENDCAP_LABELS);
	}

	public ChamberPositionBean() {
		division = Division.BARREL;

		setDefaultCoordinates(division);
		syncCoordinatesToLocation();
	}

	private void setDefaultCoordinates(Division aDivision) {
		final Map<ChamberPosition.Coordinate, List<String>> selections = SELECTIONS
				.get(aDivision);
		// TODO: Division setting was missing, might broke something!
		division = aDivision;
		section = selections.get(ChamberPosition.Coordinate.SECTION).get(0);
		layer = selections.get(ChamberPosition.Coordinate.LAYER).get(0);
		sector = selections.get(ChamberPosition.Coordinate.SECTOR).get(0);
		subsector = selections.get(ChamberPosition.Coordinate.SUBSECTOR).get(0);
		etaPartition = selections.get(ChamberPosition.Coordinate.ETAPARTITION).get(0);
	}

	private void setLabels(Division aDivision) {
		final Map<ChamberPosition.Coordinate, String> labels = SELECTION_LABELS.get(aDivision);

		sectionUI.setLabel(labels.get(ChamberPosition.Coordinate.SECTION));
		layerUI.setLabel(labels.get(ChamberPosition.Coordinate.LAYER));
		sectorUI.setLabel(labels.get(ChamberPosition.Coordinate.SECTOR));
		subsectorUI.setLabel(labels.get(ChamberPosition.Coordinate.SUBSECTOR));
		etaPartitionUI.setLabel(labels.get(ChamberPosition.Coordinate.ETAPARTITION));
	}

	public List<String> getLabel() {
		final Division defaultDiv = Division.BARREL;
		final Map<ChamberPosition.Coordinate, String> labels = SELECTION_LABELS.get(defaultDiv);

		final List<String> result = Arrays.asList(labels
				.get(ChamberPosition.Coordinate.SECTION), labels.get(ChamberPosition.Coordinate.LAYER), labels
				.get(ChamberPosition.Coordinate.SECTOR), labels.get(ChamberPosition.Coordinate.SUBSECTOR),
				labels.get(ChamberPosition.Coordinate.ETAPARTITION));

		return result;
	}

	private void syncGUI() {
		sectionUI.setValue(section);
		layerUI.setValue(layer);
		sectorUI.setValue(sector);
		subsectorUI.setValue(subsector);
		//TODO: check why etaPart is missing

		locationUI.setValue(location);
	}

	public void divisionChanged(ValueChangeEvent event) {
		final Division newDiv = (Division) ((UIXValue) event.getSource())
				.getValue();

		setDefaultCoordinates(newDiv);
		syncCoordinatesToLocation();
		setLabels(newDiv);
		syncGUI();
	}

	public void componentChanged(ValueChangeEvent event) {
		logger.debug("sectionChanged() called on CPB '" + this.toString() + "', locationUI = '" + location + "'");

		String componentId = ((UIXComponentBase) (event.getSource())).getId();
		logger.debug("componentId: " + componentId);
		final String oldValue = (String) (event.getOldValue());
		final String newValue = (String) ((UIXValue) event.getSource())
		.getValue();
		logger.debug("oldValue= " + oldValue + ", newValue= " + newValue);

		if (componentId.equals("section"))
			section = newValue;
		else if (componentId.equals("layer"))
			layer = newValue;
		else if (componentId.equals("sector"))
			sector = newValue;
		else if (componentId.equals("subsector"))
			subsector = newValue;
		else if (componentId.equals("etapartition"))
			etaPartition = newValue;
		else if (componentId.equals("location")) {
			boolean locationIsGood = false;
			for (String test: new String[] {newValue, oldValue}) {
				logger.debug("Testing with location '" + test + "'");
				try {
					if (ChamberPosition.valueOf(test).isValid()) { 
						location = test;
						logger.debug("location set to new (validated) value '" + location + "'");
						locationIsGood = true;
						break;
					}
				} catch (IllegalArgumentException e) {
					logger.debug("While testing '" + test + "' caught " + e);
				}
			}
			if (! locationIsGood) {
				// fall back to default
				setDefaultCoordinates(Division.BARREL);
				syncAllowedSelections(ChamberPosition.valueOf(location));
				syncCoordinatesToLocation();
				syncGUI();
				return;
			}
			logger.debug(String.format("location '%s' parsed to %s", location, ChamberPosition.valueOf(location)));
		}
		else 
			throw new RuntimeException("Cannot handle unknown componentId '" + componentId + "'!");
			
		ChamberPosition position = null;
		
		if (componentId.equals("location")) {
			// TODO: extract to syncLocationToCoordinates()
			try {
				position = ChamberPosition.valueOf(location);
			
				division = position.getDivision();
				section = position.getSection().alias();
				layer = position.getLayer().alias();
				sector = position.getSector().alias();
				if (division == Division.BARREL) {
					subsector = position.getSubsector().alias();
					etaPartition = position.getEtaPartition().alias();
				}
				else {
					// No subsector & etapart in endcap
					subsector = "";
					etaPartition = "";
				}
			} catch (Exception e) {
				logger.warn("Caught " + e.toString());
			}
			
			logger.debug("Before syncAllowedSelections(): ChamberPositionBean is " + this);
			logger.debug("Before syncAllowedSelections(): ChamberPosition is " + position);
			syncAllowedSelections(position);
			logger.debug("After syncAllowedSelections(): ChamberPositionBean is " + this);
			logger.debug("After syncAllowedSelections(): ChamberPosition is " + position);

			
			setLabels(division);
			syncGUI();
			return;
		}
		else {
			// TODO: check why on hitting browser's "Back" coordinates are messed up, like "RE-2/1in/1"
			
			try {
				Division posDivision = division;
				Section posSection = Section.get(posDivision, section);
				Layer posLayer = Layer.get(posDivision, layer);
				Sector posSector = Sector.get(posDivision, Integer.parseInt(sector));
				Subsector posSubsector = Subsector.get(subsector);
				EtaPartition posEtaPartition = EtaPartition.get(etaPartition);

				position = new ChamberPosition(
						posDivision,
						posSection,
						posLayer,
						posSector,
						posSubsector,
						posEtaPartition);
			} catch (IllegalArgumentException e) {
				// Fall back to default
				setDefaultCoordinates(Division.BARREL);
				syncCoordinatesToLocation();
				position = ChamberPosition.valueOf(location);
				syncAllowedSelections(position);
				syncGUI();
				return;
			}
		}
		
		// selections should display only allowed sets
		logger.debug("Before syncAllowedSelections(): ChamberPositionBean is " + this);
		logger.debug("Before syncAllowedSelections(): ChamberPosition is " + position);
		syncAllowedSelections(position);
		logger.debug("After syncAllowedSelections(): ChamberPositionBean is " + this);
		logger.debug("After syncAllowedSelections(): ChamberPosition is " + position);
		
		syncCoordinatesToLocation();
		
		syncGUI();
	}
	private void syncCoordinatesToLocation() {
		location = this.toString();
	}
	private void syncAllowedSelections(ChamberPosition position) {
		// TODO: Division?
		
		// Section
		final List<String> allowedSections = new ArrayList<String>();
		for (Section each: position.getAllowedSections()) {
			allowedSections.add(each.alias());
		}
		SELECTIONS.get(division).put(ChamberPosition.Coordinate.SECTION, allowedSections);
		if (! position.isSectionValid())
			section = allowedSections.get(0);
		
		// Layer
		final List<String> allowedLayers = new ArrayList<String>();
		for (Layer each: position.getAllowedLayers()) {
			allowedLayers.add(each.alias());
		}
		SELECTIONS.get(division).put(ChamberPosition.Coordinate.LAYER, allowedLayers);
		if (! position.isLayerValid())
			layer = allowedLayers.get(0);

		// Sector
		final List<String> allowedSectors = new ArrayList<String>();
		for (Sector each: position.getAllowedSectors()) {
			allowedSectors.add(each.alias());
		}
		SELECTIONS.get(division).put(ChamberPosition.Coordinate.SECTOR, allowedSectors);
		if (! position.isSectorValid()) {
			sector = allowedSectors.get(0);
			position.setSector(Sector.get(division, Integer.parseInt(sector)));
		}

		// Subsector
		final List<String> allowedSubsectors = new ArrayList<String>();
		for (Subsector each: position.getAllowedSubsectors()) {
			allowedSubsectors.add(each.alias());
		}
		SELECTIONS.get(division).put(ChamberPosition.Coordinate.SUBSECTOR, allowedSubsectors);
		if (! position.isSubsectorValid()) {
			subsector = allowedSubsectors.get(0);
			position.setSubsector(Subsector.get(subsector));
		}

		// EtaPartition
		final List<String> allowedEtaPartitions = new ArrayList<String>();
		for (EtaPartition each: position.getAllowedEtaPartitions()) {
			allowedEtaPartitions.add(each.alias());
		}
		SELECTIONS.get(division).put(ChamberPosition.Coordinate.ETAPARTITION, allowedEtaPartitions);
		if (! position.isEtaPartitionValid()) {
			etaPartition = allowedEtaPartitions.get(0);
			position.setEtaPartition(EtaPartition.get(etaPartition));
		}
	}
	
	public boolean isSubsectorRendered() {
		return division != Division.ENDCAP;
	}

	public boolean isEtaPartitionRendered() {
		return division != Division.ENDCAP;
	}

	public Division getDivision() {
		return division;
	}

	public String getEtaPartition() {
		return etaPartition;
	}

	public List<SelectItem> getEtaPartitionList() {
		final ChamberPosition.Coordinate c = ChamberPosition.Coordinate.ETAPARTITION;

		populateList(c);
		return coordinateList.get(c);
	}

	public CoreSelectOneChoice getEtaPartitionUI() {
		return etaPartitionUI;
	}

	public String getLayer() {
		return layer;
	}

	public List<SelectItem> getLayerList() {
		final ChamberPosition.Coordinate c = ChamberPosition.Coordinate.LAYER;

		populateList(c);
		return coordinateList.get(c);
	}

	public CoreSelectOneChoice getLayerUI() {
		return layerUI;
	}

	/**
	 * @return layer number, starting from 1, according to the ordering in {BARREL,ENDCAP}_LAYER_LIST
	 */
	public int getLayerNumber() {
		final List<String> layers = SELECTIONS.get(division).get(
				ChamberPosition.Coordinate.LAYER);

		return Integer.valueOf(layers.indexOf(layer)) + 1;
	}

	public String getSection() {
		return section;
	}

	public List<SelectItem> getSectionList() {
		final ChamberPosition.Coordinate c = ChamberPosition.Coordinate.SECTION;

		populateList(c);
		return coordinateList.get(c);
	}

	public CoreSelectOneChoice getSectionUI() {
		return sectionUI;
	}

	public String getSector() {
		return sector;
	}

	public List<SelectItem> getSectorList() {
		final ChamberPosition.Coordinate c = ChamberPosition.Coordinate.SECTOR;

		populateList(c);
		return coordinateList.get(c);
	}

	public CoreSelectOneChoice getSectorUI() {
		return sectorUI;
	}

	public String getSubsector() {
		return subsector;
	}

	public List<SelectItem> getSubsectorList() {
		final ChamberPosition.Coordinate c = ChamberPosition.Coordinate.SUBSECTOR;

		populateList(c);
		return coordinateList.get(c);
	}

	public CoreSelectOneChoice getSubsectorUI() {
		return subsectorUI;
	}

	private void populateList(ChamberPosition.Coordinate c) {
		coordinateList.put(c, SelectItemsValueOf(SELECTIONS.get(division)
				.get(c)));
	}

	private static List<SelectItem> SelectItemsValueOf(
			List<String> listOfStrings) {
		List<SelectItem> result = new ArrayList<SelectItem>();

		for (String e : listOfStrings) {
			result.add(new SelectItem(e, e));
		}

		return result;
	}

	public void setDivision(Division division) {
		this.division = division;
	}

	public void setLayer(String layer) {
		this.layer = layer;
	}

	public void setLayerList(List<SelectItem> layerList) {
		coordinateList.put(ChamberPosition.Coordinate.LAYER, layerList);
	}

	public void setLayerUI(CoreSelectOneChoice layerUI) {
		this.layerUI = layerUI;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public void setSectionList(List<SelectItem> sectionList) {
		coordinateList.put(ChamberPosition.Coordinate.SECTION, sectionList);
	}

	public void setSectionUI(CoreSelectOneChoice sectionUI) {
		this.sectionUI = sectionUI;
	}

	public void setSector(String sector) {
		this.sector = sector;
	}

	public void setSectorList(List<SelectItem> sectorList) {
		coordinateList.put(ChamberPosition.Coordinate.SECTOR, sectorList);
	}

	public void setSectorUI(CoreSelectOneChoice sectorUI) {
		this.sectorUI = sectorUI;
	}

	public void setSubsector(String subsector) {
		this.subsector = subsector;
	}

	public void setSubsectorList(List<SelectItem> subsectorList) {
		coordinateList.put(ChamberPosition.Coordinate.SUBSECTOR, subsectorList);
	}

	public void setSubsectorUI(CoreSelectOneChoice subsectorUI) {
		this.subsectorUI = subsectorUI;
	}

	public void setEtaPartition(String etaPartition) {
		this.etaPartition = etaPartition;
	}

	public void setEtaPartitionList(List<SelectItem> etaPartitionList) {
		coordinateList.put(ChamberPosition.Coordinate.ETAPARTITION, etaPartitionList);
	}

	public void setEtaPartitionUI(CoreSelectOneChoice etaPartitionUI) {
		this.etaPartitionUI = etaPartitionUI;
	}

	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public CoreInputText getLocationUI() {
		return locationUI;
	}
	public void setLocationUI(CoreInputText locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	public String toString() {
		/**
		 * Example:
		 *    BARREL: 
		 *       "W+2/RB2out/7 Forward", "W0/RB3/1+ Backward"
		 *    ENDCAP:
		 *       "RE-1/2/29 B", "RE+3/2/12 C"
		 */
		String result = null;

		switch (division) {
		case BARREL:
			result = String.format("%s%s/%s%s/%s%s %s", division.sectionName(),
					section, division.alias(), layer, sector, subsector,
					etaPartition);
			break;
		case ENDCAP:
			result = String.format("%s%s/%s/%s", division.alias(), section,
					layer, sector);
		}

		return result;
	}

	private void fallBack() {
		setDefaultCoordinates(Division.BARREL);
		syncAllowedSelections(ChamberPosition.valueOf(location));
		syncCoordinatesToLocation();
	}
	public String onSubmit() {
		try {
			if (! ChamberPosition.valueOf(location).isValid()) { 
				fallBack();
				syncGUI();
			}
		} catch (IllegalArgumentException e) {
			logger.debug("on submit  '" + "' caught " + e);
			return "error";
		}

		MaskingUtils.removeBeanFromSession("mask");
		logger.debug("Removed bean 'mask'");

		RequestContext requestContext = RequestContext.getCurrentInstance();
		requestContext.getPageFlowScope().put("chamberPosition", this);
		logger.debug("Added '" + this + "' to pageFlowScope");

		return "select";
	}

	public void onReportAll(ActionEvent e) {
		final String beanName = "chambersReport";

		MaskingUtils.removeBeanFromSession(beanName);
	}
}