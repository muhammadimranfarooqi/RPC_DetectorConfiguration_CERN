package rpct.masking.utils;

import java.util.ArrayList;
import java.util.List;


public enum Sector {
	// Barrel
	SB1, SB2, SB3, SB4, SB5, SB6, SB7, SB8, SB9, SB10, 
	SB11, SB12, 
	// Endcap
	SE1, SE2, SE3, SE4, SE5, SE6, SE7, SE8, SE9, SE10, 
	SE11, SE12, SE13, SE14, SE15, SE16, SE17, SE18, SE19, SE20, 
	SE21, SE22, SE23, SE24, SE25, SE26, SE27, SE28, SE29, SE30, 
	SE31, SE32, SE33, SE34, SE35, SE36;
	
	public String alias() {
		return String.valueOf(number());
	}

	private static final int NUM_BARREL_SECTORS = 12;
	// Numbering starts from 1
	public int number() {
		return division() == Division.BARREL ? ordinal() + 1 : ordinal() - NUM_BARREL_SECTORS + 1;
	}
	
	public Division division() {
		return ordinal() < NUM_BARREL_SECTORS ? Division.BARREL : Division.ENDCAP;
	}
	
	public static Sector get(Division division, int number) throws IllegalArgumentException {
		String prefix = "";
		switch (division) {
		case BARREL:
			prefix = "SB";
			break;
		case ENDCAP:
			prefix = "SE";
			break;
		default: throw new IllegalArgumentException("Unknown division " + division + " !");
		}
		
		Sector result = Sector.valueOf(prefix + number);
		
		return result ;
	}
	
	public static final List<Sector> ALL_BARREL = new ArrayList<Sector>();
	public static final List<Sector> ALL_ENDCAP = new ArrayList<Sector>();
	public static final List<Sector> FIRST_HALF_ENDCAP = new ArrayList<Sector>();
	static {
		final int HALF_ENDCAP_NUMBER = 18;
		for (Sector sector: Sector.values()) {
			if (sector.division().equals(Division.BARREL))
				ALL_BARREL.add(sector);
			else if (sector.division().equals(Division.ENDCAP)) {
				ALL_ENDCAP.add(sector);
				if (sector.number() <= HALF_ENDCAP_NUMBER)
					FIRST_HALF_ENDCAP.add(sector);
			}
		}
	}
}
