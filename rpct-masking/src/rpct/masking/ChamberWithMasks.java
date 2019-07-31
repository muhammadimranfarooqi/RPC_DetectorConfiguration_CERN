package rpct.masking;



public class ChamberWithMasks implements Comparable{
	private final String id;
	private final String name;
	private final String lbName;
	private final String enabledStripsMask;


	public ChamberWithMasks(String id, String name, String lbName,
			String enabledStripsMask) {
		this.id = id;
		this.name = name;
		this.lbName = lbName;
		this.enabledStripsMask = enabledStripsMask;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getLBoardName() {
		return lbName;
	}

	public String getEnabledStripsMask() {
		return enabledStripsMask;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
	    if ( !(obj instanceof ChamberWithMasks) ) return false;
	    
	    ChamberWithMasks chm = (ChamberWithMasks) obj;
	    	    
		return this.name.equals(chm.name) && this.id.equals(chm.id);
	}

	@Override
	public int hashCode() {
		final int SEED = 23;
		final int ODD_PRIME_NUMBER = 37;

		int result = SEED;

		result = result *  ODD_PRIME_NUMBER + name.hashCode();
		result = result *  ODD_PRIME_NUMBER + id.hashCode();
		
		return result;
	}

	public int compareTo(Object o) {
		if (! (o instanceof ChamberWithMasks))
			return -1;
		return this.name.compareTo(((ChamberWithMasks) o ).name);
	}
}
