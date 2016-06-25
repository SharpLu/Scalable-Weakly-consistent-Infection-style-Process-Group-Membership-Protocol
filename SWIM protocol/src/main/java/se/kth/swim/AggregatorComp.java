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
package se.kth.swim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.swim.msg.net.NetStatus;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.util.network.NatedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AggregatorComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(AggregatorComp.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private final NatedAddress selfAddress;
//    private Map<NatedAddress,Set<NatedAddress>> deadnodes = new HashMap<NatedAddress,Set<NatedAddress>>();
//    private Set<NatedAddress> repoter = new HashSet<NatedAddress>();;
    private Set<Pair<Set<NatedAddress>,NatedAddress>> deadnodes;
    private Set<NatedAddress> count;
    private Map<NatedAddress,Set<NatedAddress>> p;
    private Set<NatedAddress> source;
    
    
    public AggregatorComp(AggregatorInit init) {
        this.selfAddress = init.selfAddress;
        log.info("{} initiating...", new Object[]{selfAddress.getId()});
        deadnodes =new HashSet<Pair<Set<NatedAddress>,NatedAddress>>();
        p= new HashMap<NatedAddress,Set<NatedAddress>>();
        count = new HashSet<NatedAddress>();
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleStatus, network);
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} starting...", new Object[]{selfAddress});
        }

    };
    private Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", new Object[]{selfAddress});
        }

    };

    private Handler<NetStatus> handleStatus = new Handler<NetStatus>() {

        @Override
        public void handle(NetStatus status) {

//		    if(!(status.getContent().getNatedAddress().isEmpty())){
//		    	
//		    	deadnodes.add(Pair.with(status.getContent().getNatedAddress(), status.getSource()));
//		  	
//		    		for(Pair<Set<NatedAddress>,NatedAddress> p : deadnodes){
//		        		
//		        		if(status.getContent().getNatedAddress().equals(p.getValue0())&&!status.getSource().equals(p.getValue1())){
//		        			count.add(p.getValue1());
//		        		}
//		        	}
//		        	
//		            log.info("{} has been detected dead by:{} nodes", 
//		                    new Object[]{status.getContent().getNatedAddress(),count.size()});
//		    	
//		    	
//		    }
        	if(!((status.getContent().getNa())==null)){
        		
       		
        		if(p.containsKey(status.getContent().getNa())){
        			p.get(status.getContent().getNa()).add(status.getSource());
        		}
        		else{
        			source= new HashSet<NatedAddress>();
               		source.add(status.getSource());
               		p.put(status.getContent().getNa(), source);	
        		}
        		
        		
        		
        	}
        	for (Map.Entry<NatedAddress,Set<NatedAddress>> entry : p.entrySet()) {
        		
        		log.info("{} has been detected dead by:{} nodes", 
	                    new Object[]{entry.getKey(),entry.getValue().size()});
//	    	
        	}
        	
        
        }
    };

    public static class AggregatorInit extends Init<AggregatorComp> {

        public final NatedAddress selfAddress;

        public AggregatorInit(NatedAddress selfAddress) {
            this.selfAddress = selfAddress;
        }
    }
}
