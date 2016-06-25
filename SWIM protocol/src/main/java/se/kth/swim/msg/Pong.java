package se.kth.swim.msg;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import se.sics.p2ptoolbox.util.network.NatedAddress;

public class Pong {
	private Map<NatedAddress, Integer> inc = new HashMap<NatedAddress, Integer>();
	private Map<NatedAddress, Integer> join = new HashMap<NatedAddress, Integer>();
    private Set<NatedAddress> ack = new HashSet<NatedAddress>();
	public Set<NatedAddress> getAck() {
		return ack;
	}
	public void setAck(Set<NatedAddress> ack) {
		this.ack = ack;
	}

	
	public Map<NatedAddress, Integer> getJoin() {
		return join;
	}
	public void setJoin(Map<NatedAddress, Integer> join) {
		this.join = join;
	}

	
	
	public Map<NatedAddress, Integer> getInc() {
		return inc;
	}
	public void setInc(Map<NatedAddress, Integer> inc) {
		this.inc = inc;
	}
	
	private Map<NatedAddress, Integer> alive = new HashMap<NatedAddress, Integer>();
	private Map<NatedAddress, Integer> suspect = new HashMap<NatedAddress, Integer>();
	private Map<NatedAddress, Integer> confirm = new HashMap<NatedAddress, Integer>();
	public Map<NatedAddress, Integer> getAlive() {
		return alive;
	}
	public void setAlive(Map<NatedAddress, Integer> alive) {
		this.alive = alive;
	}
	public Map<NatedAddress, Integer> getSuspect() {
		return suspect;
	}
	public void setSuspect(Map<NatedAddress, Integer> suspect) {
		this.suspect = suspect;
	}
	public Map<NatedAddress, Integer> getConfirm() {
		return confirm;
	}
	public void setConfirm(Map<NatedAddress, Integer> confirm) {
		this.confirm = confirm;
	}


}
