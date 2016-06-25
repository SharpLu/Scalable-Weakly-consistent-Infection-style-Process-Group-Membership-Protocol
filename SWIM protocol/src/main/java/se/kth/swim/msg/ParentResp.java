package se.kth.swim.msg;

import java.util.HashSet;
import java.util.Set;

import se.sics.p2ptoolbox.util.network.NatedAddress;

public class ParentResp {
	private Set<NatedAddress> updateParent = new HashSet<NatedAddress>();

	public Set<NatedAddress> getUpdateParent() {
		return updateParent;
	}

	public void setUpdateParent(Set<NatedAddress> updateParent) {
		this.updateParent = updateParent;
	}


}
