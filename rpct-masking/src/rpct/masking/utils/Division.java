package rpct.masking.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public enum Division {
	BARREL("RB") {
		@Override
		public String sectionName() {
			return BARREL_SECTION_NAME;
		}
		@Override
		public List<Section> getSections() {
			return Section.ALL_BARREL;
		}
	},
	ENDCAP("RE") {
		@Override
		public String sectionName() {
			return ENDCAP_SECTION_NAME;
		}
		@Override
		public List<Section> getSections() {
			return Section.ALL_ENDCAP;
		}
	};

	private final String alias;

	private Division(String alias) {
		this.alias = alias;
	}

	public String alias() {
		return alias;
	}

	private static final String BARREL_SECTION_NAME = "W";
	private static final String ENDCAP_SECTION_NAME = "";

	public abstract String sectionName();
	public abstract List<Section> getSections();

	private static final Map<String, Division> aliasToDivision = new HashMap<String, Division>();
	static {
		for (Division each: Division.values()) {
			aliasToDivision.put(each.alias.toUpperCase(), each);

			aliasToDivision.put(each.name().toUpperCase(), each);
		}
	}
	
	public static Division get(String alias) {
		final String key = alias.toUpperCase();

		if (aliasToDivision.containsKey(key))
			return aliasToDivision.get(key);
		else
			throw new IllegalArgumentException("Division '" + alias + "' is not defined!");
	}


	
	public static final List<Division> ALL = Arrays.asList(Division.values());
}