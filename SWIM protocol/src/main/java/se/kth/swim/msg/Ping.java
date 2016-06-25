/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package se.kth.swim.msg;

import java.awt.Container;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import se.sics.p2ptoolbox.util.network.NatedAddress;



/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Ping {
	
	

	private Map<NatedAddress,Set<NatedAddress>> updateParent = new HashMap<NatedAddress,Set<NatedAddress>>();

	
	public Map<NatedAddress, Set<NatedAddress>> getUpdateParent() {
		return updateParent;
	}
	public void setUpdateParent(Map<NatedAddress, Set<NatedAddress>> updateParent) {
		this.updateParent = updateParent;
	}

	private Map<NatedAddress, Integer> join = new HashMap<NatedAddress, Integer>();
	public Map<NatedAddress, Integer> getJoin() {
		return join;
	}
	public void setJoin(Map<NatedAddress, Integer> join) {
		this.join = join;
	}
	
	private Map<NatedAddress, Integer> inc = new HashMap<NatedAddress, Integer>();
	
	
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
	public Map<NatedAddress, Integer> getInc() {
		return inc;
	}
	public void setInc(Map<NatedAddress, Integer> inc) {
		this.inc = inc;
	}
	
	

}
