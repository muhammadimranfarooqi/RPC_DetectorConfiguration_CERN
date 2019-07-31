package rpct.masking.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public enum Layer {
	RB1IN("1in", 1, Division.BARREL), RB1OUT("1out", 2, Division.BARREL), RB2IN("2in", 3, Division.BARREL), RB2OUT("2out", 4, Division.BARREL), RB3("3", 5, Division.BARREL), RB4("4", 6, Division.BARREL),
	RING1("1", 1, Division.ENDCAP), RING2("2", 2, Division.ENDCAP), RING3("3", 3, Division.ENDCAP);
	
	private final String alias;
	private final int number;
	private final Division division;


	private Layer(String alias, int number, Division division) {
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

	private static final Map<Division, Map<String, Layer>> aliasToLayer = new HashMap<Division, Map<String, Layer>>();
	static {
		aliasToLayer.put(Division.BARREL, new HashMap<String, Layer>());
		aliasToLayer.put(Division.ENDCAP, new HashMap<String, Layer>());
		for (Layer each: Layer.values()) {
			final String key = each.alias.toUpperCase();
			aliasToLayer.get(each.division).put(key, each);
			
			aliasToLayer.get(each.division).put(each.name().toUpperCase(), each);
		}
	}
	
	public static Layer get(Division division, String alias) throws IllegalArgumentException {
		final String key = alias.toUpperCase();
		
		if (aliasToLayer.containsKey(division) && aliasToLayer.get(division).containsKey(key))
			return aliasToLayer.get(division).get(key);
		else
			throw new IllegalArgumentException("Division '" + division + "', Layer '" + alias + "' is not defined!");
	}

	public static final List<Layer> ALL_BARREL = new ArrayList<Layer>();
	public static final List<Layer> ALL_ENDCAP = new ArrayList<Layer>();
	static {
		for (Layer layer: Layer.values()) {
			if (layer.division() == Division.BARREL)
				ALL_BARREL.add(layer);
			else if (layer.division() == Division.ENDCAP)
				ALL_ENDCAP.add(layer);
			else
				throw new RuntimeException("Unknown division " + layer.division());
		}
	}
}
