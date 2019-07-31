package rpct.masking.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public enum Subsector {
	NONE(""), MINUS("-"), PLUS("+"), MINUSMINUS("--"), PLUSPLUS("++");

	private final String alias;

	private Subsector(String alias) {
		this.alias = alias;
	}

	public String alias() {
		return alias;
	}
	
	private static final Map<String, Subsector> aliasToSubsector = new HashMap<String, Subsector>();
	static {
		for (Subsector each: Subsector.values()) {
			aliasToSubsector.put(each.alias, each);
		}
	}
	
	public static Subsector get(String alias) throws IllegalArgumentException {
		if (aliasToSubsector.containsKey(alias))
			return aliasToSubsector.get(alias);
		else
			throw new IllegalArgumentException("Subsector '" + alias + "' is not defined!");
	}

	public static final List<Subsector>  NONE_ONLY = Arrays.asList(NONE);
	public static final List<Subsector>  MINUS_PLUS = Arrays.asList(MINUS, PLUS);
	public static final List<Subsector>  MINUSMINUS_MINUS_PLUS_PLUSPLUS = Arrays.asList(MINUSMINUS, MINUS, PLUS, PLUSPLUS);
}
