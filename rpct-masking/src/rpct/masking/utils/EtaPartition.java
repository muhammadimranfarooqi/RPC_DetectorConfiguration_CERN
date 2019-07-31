package rpct.masking.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public enum EtaPartition {
	BACKWARD("Backward"), CENTRAL("Central"), FORWARD("Forward"),
	A("A"), B("B"), C("C"),
	// Required for hiding Endcap partitions 
	NONE("");
	
	private final String alias;

	private EtaPartition(String alias) {
		this.alias = alias;
	}

	public String alias() {
		return alias;
	}

	private static final Map<String, EtaPartition> aliasToEtaPartition = new HashMap<String, EtaPartition>();
	static {
		for (EtaPartition each: EtaPartition.values()) {
			aliasToEtaPartition.put(each.alias, each);
			
			aliasToEtaPartition.put(each.name().toUpperCase(), each);
		}
	}
	
	public static EtaPartition get(String alias) throws IllegalArgumentException {
		final String key = alias.toUpperCase();

		if (aliasToEtaPartition.containsKey(key))
			return aliasToEtaPartition.get(key);
		else
			throw new IllegalArgumentException("EtaPartition '" + alias + "' is not defined!");
	}

	public static final List<EtaPartition> BACKWARD_FORWARD = Arrays.asList(BACKWARD, FORWARD);
	public static final List<EtaPartition> BACKWARD_CENTRAL_FORWARD = Arrays.asList(BACKWARD, CENTRAL, FORWARD);
	public static final List<EtaPartition> A_B_C = Arrays.asList(A, B, C);
	public static final List<EtaPartition> NONE_ONLY = Arrays.asList(NONE);
}
