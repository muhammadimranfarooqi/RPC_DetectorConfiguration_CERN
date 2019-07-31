package rpct.masking;

import rpct.db.domain.configuration.SynCoderConf;
import rpct.db.domain.equipment.LinkBoard;
import rpct.xdaq.axis.Binary;


public class LinkBoardWithConfiguration implements Comparable {
	private final LinkBoard linkBoard;
//	private final SynCoderConf configuration;

	private final String name;
	private final int id;
	private final byte[] connectedMaskBytes;
	private final byte[] enabledMaskBytes;

	public LinkBoardWithConfiguration(LinkBoard linkBoard,
			SynCoderConf configuration) {
		this.linkBoard = linkBoard;
		
		name = linkBoard.getName();
		id = linkBoard.getId();

		connectedMaskBytes = linkBoard.getConnectedStripsMask();
		enabledMaskBytes = configuration.getInChannelsEna();
		
		// no unconnected channels can be enabled
		for (int i = 0; i < enabledMaskBytes.length; i++) {
			enabledMaskBytes[i] = (byte) (enabledMaskBytes[i] & connectedMaskBytes[i]);
		}
	}
	
	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public String getConnectedChannelsMask() {
		return new Binary(connectedMaskBytes).toString().toUpperCase();
	}

	public String getEnabledChannelsMask() {
		return new Binary(enabledMaskBytes).toString().toUpperCase();
	}

	public LinkBoard getRawLinkBoard() {
		return linkBoard;
	}

	public int compareTo(Object o) {
		if (! (o instanceof LinkBoardWithConfiguration))
			return -1;
		return this.name.compareTo(((LinkBoardWithConfiguration) o ).name);
	}
}
