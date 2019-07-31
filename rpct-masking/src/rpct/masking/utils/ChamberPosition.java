package rpct.masking.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;



public class ChamberPosition {
	private Division division;
	private Section section;
	private Layer layer;
	private Sector sector;
	private Subsector subsector;
	private EtaPartition etaPartition;

	private static Logger logger = Logger.getLogger(ChamberPosition.class);

	public ChamberPosition(Division division, Section section, Layer layer,
			Sector sector, Subsector subsector, EtaPartition etaPartition) {
		super();
		this.division = division;
		this.section = section;
		this.layer = layer;
		this.sector = sector;
		this.subsector = subsector;
		this.etaPartition = etaPartition;
	}

	public ChamberPosition() {
		this.division = null;
		this.section = null;
		this.layer = null;
		this.sector = null;
		this.subsector = null;
		this.etaPartition = null;
	}

	public enum Coordinate {
		// TODO: change to a more general names,
		// see http://cmslxr.fnal.gov/lxr/source/DataFormats/MuonDetId/interface/RPCDetId.h
		DIVISION, LAYER, SECTION, SECTOR, SUBSECTOR, ETAPARTITION;
	}

	public Division getDivision() {
		return division;
	}

	public void setDivision(Division division) {
		this.division = division;
	}

	public Section getSection() {
		return section;
	}

	public void setSection(Section section) {
		this.section = section;
	}

	public Layer getLayer() {
		return layer;
	}

	public void setLayer(Layer layer) {
		this.layer = layer;
	}

	public Sector getSector() {
		return sector;
	}

	public void setSector(Sector sector) {
		this.sector = sector;
	}

	public Subsector getSubsector() {
		return subsector;
	}

	public void setSubsector(Subsector subsector) {
		this.subsector = subsector;
	}

	public EtaPartition getEtaPartition() {
		return etaPartition;
	}

	public void setEtaPartition(EtaPartition etaPartition) {
		this.etaPartition = etaPartition;
	}
	
	public List<Division> getAllowedDivisions() {
		return Division.ALL;
	}
	
	public List<Section> getAllowedSections() {
		return division.getSections();
	}
	
	public List<Layer> getAllowedLayers() {
		return section.getLayers();
	}
	
	public List<Sector> getAllowedSectors() {
		List<Sector> result = null;

		switch (division) {
		case BARREL:
			result = Sector.ALL_BARREL;
			break;
		case ENDCAP:
			switch (section) {
			case DM4:
			case DM3:
			case DM2:
			case DP2:
			case DP3:
			case DP4:
				switch (layer) {
				case RING1:
					result = Sector.FIRST_HALF_ENDCAP;
					break;
				case RING2:
				case RING3:
					result = Sector.ALL_ENDCAP;
					break;
				default: throw new RuntimeException(division + ":" + section + ":" + layer + " not handled!");
				}
				break;
			case DM1:
			case DP1:
				result = Sector.ALL_ENDCAP;
				break;
			default: throw new RuntimeException(division + ":" + section + " not handled!");
			}
			break;
		default: throw new RuntimeException(division + " not handled!");
		}
		
		return result;
	}

	public List<Subsector> getAllowedSubsectors() {
		List<Subsector> result = null;

		switch (division) {
		case BARREL:
			switch (layer) {
			// No subsector
			case RB1IN:
			case RB1OUT:
			case RB2IN:
			case RB2OUT:
				result = Subsector.NONE_ONLY;
				break;
			case RB3:
				result = Subsector.MINUS_PLUS;
				break;
			case RB4:
				if (sector == Sector.SB4)
					result = Subsector.MINUSMINUS_MINUS_PLUS_PLUSPLUS;
				else if (sector == Sector.SB9 || sector == Sector.SB11)
					result = Subsector.NONE_ONLY;
				else 
					result = Subsector.MINUS_PLUS;
				break;
			default: throw new RuntimeException(division + ":" + section + ":" + layer + " not handled!");
			}
			break;
		case ENDCAP:
			// No subsectors in Endcap
			result = Subsector.NONE_ONLY;
			break;
		default: throw new RuntimeException(division + " not handled!");
		}
		
		return result;
	}
	
	public List<EtaPartition> getAllowedEtaPartitions() {
		List<EtaPartition> result = null;

		switch (division) {
		case BARREL:
			switch (section) {
			case WM2:
			case WP2:
				switch (layer) {
				case RB1IN:
				case RB1OUT:
				case RB2IN:
				case RB3:
				case RB4:
					result = EtaPartition.BACKWARD_FORWARD;
					break;
				case RB2OUT:
					result = EtaPartition.BACKWARD_CENTRAL_FORWARD;
					break;
				default: throw new RuntimeException(division + ":" + section + ":" + layer + " not handled!");
				}
				break;
			case WM1:
			case W0:
			case WP1:
				switch (layer) {
				case RB1IN:
				case RB1OUT:
				case RB2OUT:
				case RB3:
				case RB4:
					result = EtaPartition.BACKWARD_FORWARD;
					break;
				case RB2IN:
					result = EtaPartition.BACKWARD_CENTRAL_FORWARD;
					break;
				default: throw new RuntimeException(division + ":" + section + ":" + layer + " not handled!");
				}
				break;
			default: throw new RuntimeException(division + ":" + section + " not handled!");
			}
			break;
		case ENDCAP:
			/* Endcap Etapartitions are combined in one "non-partition" 
			 * result = EtaPartition.A_B_C;
			 */
			result = EtaPartition.NONE_ONLY;
			break;
		default: throw new RuntimeException(division + " not handled!");
		}

		return result;
	}
	
	public boolean isDivisionValid() {
		return getAllowedDivisions().contains(division);
	}

	public boolean isSectionValid() {
		return getAllowedSections().contains(section);
	}

	public boolean isLayerValid() {
		return getAllowedLayers().contains(layer);
	}

	public boolean isSectorValid() {
		return getAllowedSectors().contains(sector);
	}

	public boolean isSubsectorValid() {
		return getAllowedSubsectors().contains(subsector);
	}

	public boolean isEtaPartitionValid() {
		return getAllowedEtaPartitions().contains(etaPartition);
	}
	
	public boolean isValid() {
		logger.debug(this + " isDivisionValid(): " + isDivisionValid());
		logger.debug(this + " isSectionValid(): " + isSectionValid());
		logger.debug(this + " isLayerValid(): " + isLayerValid());
		logger.debug(this + " isSectorValid(): " + isSectorValid());
		logger.debug(this + " isSubsectorValid(): " + isSubsectorValid());
		logger.debug(this + " isEtaPartitionValid(): " + isEtaPartitionValid());
		return 
		isDivisionValid() &&
		isSectionValid() &&
		isLayerValid() &&
		isSectorValid() &&
		isSubsectorValid() &&
		isEtaPartitionValid();
	}

	public static ChamberPosition valueOf(String location) throws IllegalArgumentException {
		/* Example locations: 
		 *    "W-2/RB3/1- Backward"
		 *    "RE-3/2/13"
		 */
		ChamberPosition result = new ChamberPosition();
		
		final String delim = "/";
		final String[] parts = location.trim().toUpperCase().split(delim);
		

		if (parts[0].startsWith("W")) {
			// W{wheel}
			result.division = Division.BARREL;
			result.section = Section.get(result.division, parts[0].substring(1));
			
			result.layer = Layer.get(result.division, parts[1]);

			// Sector, Subsector & EtaPartition
			Pattern pattern = Pattern.compile("\\D*(\\d+)(\\S*)\\s*(\\S+)");
			Matcher matcher = pattern.matcher(parts[2]);
			
			if (matcher.matches()) {
				result.sector = Sector.get(result.division, Integer.parseInt(matcher.group(1)));
				result.subsector = Subsector.get(matcher.group(2));
				if (result.subsector == null)
					// change with Null-object
					result.subsector = Subsector.NONE;
				result.etaPartition = EtaPartition.get(matcher.group(3).toUpperCase());
			}
			else 
				throw new IllegalArgumentException("Cannot parse location '" + location + "'!");
		}
		else if (parts[0].startsWith("RE")) {
			// RE{disk}
			result.division = Division.ENDCAP;
			result.section = Section.get(result.division, parts[0].substring(2));
			
			result.layer = Layer.get(result.division, parts[1]);
			
			try {
				result.sector = Sector.get(result.division, Integer.parseInt(parts[2]));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Cannot parse location '" + location + "''s sector!");
			}
			// No subsector & etaPartition in Endcap - move to the Null-objects
			result.subsector = Subsector.NONE;
			result.etaPartition = EtaPartition.NONE;
		}
		else
			throw new IllegalArgumentException("Cannot parse location '" + location + "'!");
		
		return result;
	}
	
	private static ChamberPosition DEFAULT_CHAMBER_POSITION = null;
	public static ChamberPosition getDefault() {
		if (DEFAULT_CHAMBER_POSITION == null)
			DEFAULT_CHAMBER_POSITION = valueOf("W-2/RB1in/1 Backward");

		return DEFAULT_CHAMBER_POSITION;
	}
	
	@Override
	public String toString() {
		String result = 
			String.format("%s [division=%s, section=%s, layer=%s, sector=%s, subsector=%s, etaPart=%s", 
					getClass(), division, section, layer, sector, subsector, etaPartition);
		
		return result;
	}
}
