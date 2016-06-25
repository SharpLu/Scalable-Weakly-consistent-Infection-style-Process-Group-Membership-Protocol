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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.nattraversal.NatTraversalPort;
import se.kth.nattraversal.Updateparent;
import se.kth.swim.croupier.CroupierPort;
import se.kth.swim.croupier.msg.CroupierSample;
import se.kth.swim.croupier.util.Container;
import se.kth.swim.msg.ParentReq;
import se.kth.swim.msg.ParentResp;
import se.kth.swim.msg.Ping;
import se.kth.swim.msg.net.NetMsg;
import se.kth.swim.msg.net.NetParentReq;
import se.kth.swim.msg.net.NetParentResp;
import se.kth.swim.msg.net.NetPing;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.util.network.NatedAddress;
import se.sics.p2ptoolbox.util.network.impl.RelayHeader;
import se.sics.p2ptoolbox.util.network.impl.SourceHeader;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NatTraversalComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(NatTraversalComp.class);
    private Negative<Network> local = provides(Network.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<CroupierPort> croupier = requires(CroupierPort.class);
    private Negative<NatTraversalPort> nat = provides(NatTraversalPort.class);
    private Positive<Timer> timer = requires(Timer.class);

    private final NatedAddress selfAddress;
    private final Random rand;
    private Updateparent updateparent;
    //private final int overlayId;
    private Set<NatedAddress> deadpublic;
    private NatedAddress target;
    private ParentReq parentReq;
    private ParentResp parentResp;
    private Ping ping = new Ping();;
    private List<Pair<NatedAddress,UUID>> parentResptimeouts;
    //private Set<NatedAddress> possible;
    
    private UUID parentReqTimeoutId;
    private UUID parentRespTimeoutId;
    
    private UUID pingTimeoutId;
    private UUID pongTimeoutId;
    
  
    
    public NatTraversalComp(NatTraversalInit init) {
        this.selfAddress = init.selfAddress;
        //this.overlayId = init.overlayId;
        parentResptimeouts = new ArrayList<Pair<NatedAddress,UUID>>();
        deadpublic = new HashSet<NatedAddress>();
       // possible = new HashSet<NatedAddress>();
        log.info("{} {} initiating...", new Object[]{selfAddress.getId(), (selfAddress.isOpen() ? "OPEN" : "NATED")});

        this.rand = new Random(init.seed);
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleIncomingMsg, network);
       
       subscribe(handleparentRequest, network);
     
        subscribe(handleparentResponse,network);
        subscribe(handleOutgoingMsg, local);
        subscribe(handleParentReqTimeout,timer);
        subscribe(handleParentRespTimeout,timer);
        subscribe(handleCroupierSample, croupier);
        
        
        
        
        
        
        
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
           // log.info("{} starting...", new Object[]{selfAddress.getId()});
            schedulePeriodicParentReq();
           // schedulePeriodicPing();
        }

    };
    private Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
           // log.info("{} stopping...", new Object[]{selfAddress.getId()});
            cancelPeriodicParentReq();
           
        
        }

    };

    private Handler<NetMsg<Object>> handleIncomingMsg = new Handler<NetMsg<Object>>() {

        @Override
        public void handle(NetMsg<Object> msg) {
        	
            //log.trace("{} received msg:{}", new Object[]{selfAddress.getId(), msg});
            Header<NatedAddress> header = msg.getHeader();
            if (header instanceof SourceHeader) {
                if (!selfAddress.isOpen()) {
                    throw new RuntimeException("source header msg received on nated node - nat traversal logic error");
                }
                SourceHeader<NatedAddress> sourceHeader = (SourceHeader<NatedAddress>) header;
                if (sourceHeader.getActualDestination().getParents().contains(selfAddress)) {
                  //  log.info("{} relaying message for:{}", new Object[]{selfAddress.getId(), sourceHeader.getSource()});
                    RelayHeader<NatedAddress> relayHeader = sourceHeader.getRelayHeader();
                    trigger(msg.copyMessage(relayHeader), network);
                    return;
                } else {
                  //  log.warn("{} received weird relay message:{} - dropping it", new Object[]{selfAddress.getId(), msg});
                    return;
                }
            } else if (header instanceof RelayHeader) {
                if (selfAddress.isOpen()) {
                    throw new RuntimeException("relay header msg received on open node - nat traversal logic error");
                }
                RelayHeader<NatedAddress> relayHeader = (RelayHeader<NatedAddress>) header;
               // log.info("{} delivering relayed message:{} from:{}", new Object[]{selfAddress.getId(), msg, relayHeader.getActualSource()});
                Header<NatedAddress> originalHeader = relayHeader.getActualHeader();
                trigger(msg.copyMessage(originalHeader), local);
                return;
            } else {
              //  log.info("{} delivering direct message:{} from:{}", new Object[]{selfAddress.getId(), msg, header.getSource()});
                trigger(msg, local);
                return;
            }
        }

    };

    private Handler<NetMsg<Object>> handleOutgoingMsg = new Handler<NetMsg<Object>>() {

        @Override
        public void handle(NetMsg<Object> msg) {
           // log.trace("{} sending msg:{}", new Object[]{selfAddress.getId(), msg});
            Header<NatedAddress> header = msg.getHeader();
            if(header.getDestination().isOpen()) {
               // log.info("{} sending direct message:{} to:{}", new Object[]{selfAddress.getId(), msg, header.getDestination()});
                trigger(msg, network);
                return;
            } else {
                if(header.getDestination().getParents().isEmpty()) {
                    throw new RuntimeException("nated node with no parents");
                }
                NatedAddress parent = randomNode(header.getDestination().getParents());
                
                SourceHeader<NatedAddress> sourceHeader = new SourceHeader(header, parent);
               // log.info("{} sending message:{} to relay:{}", new Object[]{selfAddress.getId(), msg, parent});
                trigger(msg.copyMessage(sourceHeader), network);
                return;
            }
        }

    };
 
    
  
//(handleparentRequest, network);
//NatTraverser layer periodically send heart beats to the relay servers.
  
  private Handler<NetParentReq> handleparentRequest = new Handler<NetParentReq>() {
      @Override
      public void handle(NetParentReq event) {
      	 log.info("{} receive parent request from {}", selfAddress.getBaseAdr(), event.getSource());
          
          trigger(new NetParentResp(selfAddress, event.getSource(), parentResp),network);
        
      }
  };
  private Handler<NetParentResp> handleparentResponse = new Handler<NetParentResp>() {
      @Override
      public void handle(NetParentResp event) {
      	// log.info("{} receive parent response from {}", selfAddress.getBaseAdr(), event.getSource());
      	cancelParentRespTimeout(event.getSource());
          
      }
  };
  
  
  
  
  
private Handler<ParentReqTimeout> handleParentReqTimeout = new Handler<ParentReqTimeout>() {

    @Override
    public void handle(ParentReqTimeout event) {
        for (NatedAddress parentAddress : selfAddress.getParents()) {
            //log.info("{} sending ParentReq to partner:{}", new Object[]{selfAddress.getId(), parentAddress});
            trigger(new NetParentReq(selfAddress, parentAddress, parentReq), network);
            scheduleParentResp(parentAddress);
        }
    }

};

private Handler<ParentRespTimeout> handleParentRespTimeout = new Handler<ParentRespTimeout>() {

    @Override
    public void handle(ParentRespTimeout event) {
    	
       
            
    	for (int i = 0; i < parentResptimeouts.size(); i++) {
    		if(event.getTimeoutId().equals(parentResptimeouts.get(i).getValue1())){
    			target=parentResptimeouts.get(i).getValue0();
    			parentResptimeouts.remove(parentResptimeouts.get(i));
    			
    			deadpublic.add(target);
    			
    		
    		}
			
		}
    	//log.info("{} says public dead nodes are:{}", new Object[]{selfAddress.getId(), deadpublic});
    	for (NatedAddress dp : deadpublic) {
			if(selfAddress.getParents().contains(dp)){
				selfAddress.getParents().remove(dp);
				
			}
		}
    	
    	
    }

};
  
  
  
  
  
 
  

private static class ParentReqTimeout extends Timeout {

    protected ParentReqTimeout(SchedulePeriodicTimeout request) {
		super(request);
		// TODO Auto-generated constructor stub
	}

	
}


private static class ParentRespTimeout extends Timeout {

	protected ParentRespTimeout(ScheduleTimeout request) {
		super(request);
		// TODO Auto-generated constructor stub
	}
	
}
 
private void schedulePeriodicParentReq() {
SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(1000, 1000);
ParentReqTimeout sc = new ParentReqTimeout(spt);
spt.setTimeoutEvent(sc);
parentReqTimeoutId = sc.getTimeoutId();
trigger(spt, timer);
}

private void cancelPeriodicParentReq() {
CancelTimeout cpt = new CancelTimeout(parentReqTimeoutId);
trigger(cpt, timer);
parentReqTimeoutId = null;
}
  

private void scheduleParentResp(NatedAddress partnerAddress) {
ScheduleTimeout spt = new ScheduleTimeout(1000);
ParentRespTimeout sc = new ParentRespTimeout(spt);
spt.setTimeoutEvent(sc);
parentRespTimeoutId = sc.getTimeoutId();
parentResptimeouts.add(Pair.with(partnerAddress,parentRespTimeoutId)); 
trigger(spt, timer);

}
private  void cancelParentRespTimeout(NatedAddress natedaddress) {
	
	for (int i = 0; i < parentResptimeouts.size(); i++) {
		if(natedaddress.equals(parentResptimeouts.get(i).getValue0())){
			parentRespTimeoutId=parentResptimeouts.get(i).getValue1();
			CancelTimeout cpt = new CancelTimeout(parentRespTimeoutId);
	        trigger(cpt, timer);
	        parentResptimeouts.remove(parentResptimeouts.get(i));
	        parentRespTimeoutId=null;
	        break;
		}
		
	}

  
}
    
    
    
    
    
    
    
    
    private Handler<CroupierSample> handleCroupierSample = new Handler<CroupierSample>() {
        @Override
        public void handle(CroupierSample event) {
          //  log.info("{} croupier public nodes:{}", selfAddress.getBaseAdr(), event.publicSample);
          
        	if(selfAddress.getParents().size()<=2){
        		 Iterator<Container<NatedAddress, Object>> it =null;
            	 if(selfAddress.getParents().isEmpty()&&! selfAddress.isOpen()){
            		 it=event.publicSample.iterator();
            		 while(it.hasNext()){
            			 NatedAddress n= it.next().getSource();
            			 selfAddress.getParents().add(n);
            		 }
            		 
            	 }
        		
        	}
        	//System.out.println(selfAddress.getParents());
        	 
            Updateparent up = new Updateparent(event.publicSample, selfAddress.getParents());
            trigger(up, nat);

          
            
        }
    };
    
    private NatedAddress randomNode(Set<NatedAddress> nodes) {
        int index = rand.nextInt(nodes.size());
        Iterator<NatedAddress> it = nodes.iterator();
        while(index > 0) {
            it.next();
            index--;
        }
        return it.next();
    }

    public static class NatTraversalInit extends Init<NatTraversalComp> {

        public final NatedAddress selfAddress;
        public final long seed;
       // public final int overlayId;

        public NatTraversalInit(NatedAddress selfAddress, long seed) {
            this.selfAddress = selfAddress;
            this.seed = seed;
          
        }
    }
}
