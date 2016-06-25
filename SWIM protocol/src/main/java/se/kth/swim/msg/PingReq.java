package se.kth.swim.msg;

import java.util.HashMap;
import java.util.Map;

import se.sics.p2ptoolbox.util.network.NatedAddress;

public class PingReq {
	private Map<NatedAddress, Integer> indierctPnodes = new HashMap<NatedAddress, Integer>();

	public Map<NatedAddress, Integer> getIndierctPnodes() {
		return indierctPnodes;
	}

	public void setIndierctPnodes(Map<NatedAddress, Integer> indierctPnodes) {
		this.indierctPnodes = indierctPnodes;
	}

}
