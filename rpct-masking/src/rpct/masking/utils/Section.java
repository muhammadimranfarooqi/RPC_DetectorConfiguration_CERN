package rpct.masking.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public enum Section {
	// TODO: consider the alternative of not having division dependence: -5 .. +5 sections
	WM2("-2", -2, Division.BARREL), WM1("-1", -1, Division.BARREL), W0("0", 0, Division.BARREL),
	WP2("+2", +2, Division.BARREL), WP1("+1", +1, Division.BARREL),
	DM4("-4", -4, Division.ENDCAP), DM3("-3", -3, Division.ENDCAP), DM2("-2", -2, Division.ENDCAP), DM1("-1", -1, Division.ENDCAP), 
	DP4("+4", +4, Division.ENDCAP), DP3("+3", +3, Division.ENDCAP), DP2("+2", +2, Division.ENDCAP), DP1("+1", +1, Division.ENDCAP);
	
	private final String alias;
	private final int number;
	private final Division division;
	
	private Section(String alias, int number, Division division) {
		this.alias = alias;
		this.number = number;
		this.division = division;
	}

	public String alias() {
		return alias;
	}

	public int number() {
		return number;
	}

	public Division division() {
		return division;
	}

	public List<Layer> getLayers() {
		switch (division) {
		case BARREL:
			return Layer.ALL_BARREL;
		case ENDCAP:
			return Layer.ALL_ENDCAP;
		default:
			throw new RuntimeException("Unknown division " + division());
		}
	}
	
	private static final Map<Division, Map<String, Section>> aliasToSection = new HashMap<Division, Map<String, Section>>();
	static {
		aliasToSection.put(Division.BARREL, new HashMap<String, Section>());
		aliasToSection.put(Division.ENDCAP, new HashMap<String, Section>());
		for (Section each: Section.values()) {
			aliasToSection.get(each.division).put(each.alias, each);
		}
	}
	
	public static Section get(Division division, String alias) throws IllegalArgumentException {
		if (aliasToSection.containsKey(division) && aliasToSection.get(division).containsKey(alias))
			return aliasToSection.get(division).get(alias);
		else
			throw new IllegalArgumentException("Division '" + division + "', Section '" + alias + "' is not defined!");
	}

	public static final List<Section> ALL_BARREL = new ArrayList<Section>();
	public static final List<Section> ALL_ENDCAP = new ArrayList<Section>();
	static {
		for (Section section: Section.values()) {
			if (section.division() == Division.BARREL)
				ALL_BARREL.add(section);
			else if (section.division() == Division.ENDCAP)
				ALL_ENDCAP.add(section);
			else
				throw new RuntimeException("Unknown division " + section.division());
		}
	}
}
