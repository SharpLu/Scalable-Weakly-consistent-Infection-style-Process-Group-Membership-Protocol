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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.nattraversal.NatTraversalPort;
import se.kth.nattraversal.Updateparent;
import se.kth.swim.croupier.CroupierPort;
import se.kth.swim.msg.Ping;
import se.kth.swim.msg.PingReq;
import se.kth.swim.msg.Pong;
import se.kth.swim.msg.Status;
import se.kth.swim.msg.net.NetPing;
import se.kth.swim.msg.net.NetPingReq;
import se.kth.swim.msg.net.NetPong;
import se.kth.swim.msg.net.NetStatus;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.util.network.NatType;
import se.sics.p2ptoolbox.util.network.NatedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SwimComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(SwimComp.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Positive<NatTraversalPort> nat = requires(NatTraversalPort.class);

    private final NatedAddress selfAddress;
    private final Set<NatedAddress> bootstrapNodes;
    private  Set<NatedAddress> opens;
    
    private final NatedAddress aggregatorAddress;
    private Map<NatedAddress,Integer> newNodes;
    
    private Map<NatedAddress,Integer> confirm;
    private Map<NatedAddress,Integer> suspectedNodes;
    private Map<NatedAddress,Integer> aliveNodes;
    private Map<NatedAddress,Integer> confirmlimit;
    private Map<NatedAddress,Integer> suspectedNodeslimit;
    private Map<NatedAddress,Integer> aliveNodeslimit;
    private Map<NatedAddress,Set<NatedAddress>> newparents;
    
    
    
    private  Set<NatedAddress> ack;
 
    private List<Pair<NatedAddress,UUID>> pongtimeouts; 
    private List<Pair<NatedAddress,UUID>> pingReqtimeouts;
    private Map<NatedAddress,Integer> incSelf;
    private Map<NatedAddress,Integer> incTrack;
    private NatedAddress target ;
    
    private List<NatedAddress> Kmembers;
    private Set<NatedAddress> indierctPnodes;
    private UUID pingTimeoutId;
    private UUID pongTimeoutId;
    private UUID pingReqTimeoutId;
    private UUID statusTimeoutId;
    private Ping ping = new Ping();
    private Pong pong = new Pong();
    private PingReq pingR = new PingReq();
    private int inc;
   
    
    private Queue<NatedAddress> queue;
    

    private int receivedPings = 0;

    public SwimComp(SwimInit init) {
    	
    	
    	queue = new LinkedList<NatedAddress>();
			
    	
        this.selfAddress = init.selfAddress;
        log.info("{} initiating...", selfAddress);
        this.bootstrapNodes = init.bootstrapNodes;
        this.aggregatorAddress = init.aggregatorAddress;
        opens = new HashSet<NatedAddress>();
        
        
        newNodes = new HashMap<NatedAddress,Integer>();
        suspectedNodes = new HashMap<NatedAddress,Integer>();
        confirm = new HashMap<NatedAddress,Integer>();
        aliveNodes = new HashMap<NatedAddress,Integer>();
        
        
        newparents = new HashMap<NatedAddress, Set<NatedAddress>>();
        
        
        
        
        ack = new HashSet<NatedAddress>();
        indierctPnodes= new HashSet<NatedAddress>();
        incTrack = new HashMap<NatedAddress,Integer>();
        Kmembers = new ArrayList<NatedAddress>();
        incSelf = new HashMap<NatedAddress,Integer>();
        incTrack = new HashMap<NatedAddress,Integer>();
        
        pongtimeouts = new ArrayList<Pair<NatedAddress,UUID>>();
       // pongtimeouts = new HashMap<UUID, NatedAddress>();
        pingReqtimeouts = new ArrayList<Pair<NatedAddress,UUID>>();
        
        inc = 0;
        
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handlePing, network);
        subscribe(handlePong, network);
        subscribe(handlePingReq, network);
        subscribe(handlePingTimeout, timer);
        subscribe(handlePongTimeout, timer);
        subscribe(handlePingReqTimeout, timer);
       
        subscribe(handleStatusTimeout, timer);
        subscribe(handleUpdateparent, nat);
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} starting...", new Object[]{selfAddress.getId()});

            if (!bootstrapNodes.isEmpty()) {
                schedulePeriodicPing();
            }
            schedulePeriodicStatus();
        }

    };
    private Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", new Object[]{selfAddress.getId()});
            if (pingTimeoutId != null) {
                cancelPeriodicPing();
            }
            if (statusTimeoutId != null) {
                cancelPeriodicStatus();
            }
        }

    };
    
    private Handler<Updateparent> handleUpdateparent = new Handler<Updateparent>() {

        @Override
        public void handle(Updateparent event) {
        	//log.info("{} croupier public nodes:{}", selfAddress.getBaseAdr(), event.publicSample);
        	
        		if(newparents.containsKey(selfAddress)){
        			newparents.remove(selfAddress);
        			newparents.put(selfAddress,event.parents);
        		}
        		else{
        			newparents.put(selfAddress,event.parents);
        		}

        	
        	
        	ping.setUpdateParent(newparents);
        	
        }

    };

    

    private Handler<NetPing> handlePing = new Handler<NetPing>() {

        @Override
        public void handle(NetPing event) {
            log.info("{} received ping from:{}", new Object[]{selfAddress.getId(), event.getHeader().getSource()});
            receivedPings++;
            
          //detect alive or new nodes coming,and keeps everyone has the latest incarnation number of other alive members
            if(!event.getContent().getInc().isEmpty()){
            	for (Map.Entry<NatedAddress,Integer> entry : event.getContent().getInc().entrySet()) {
            		//new node 
            		if (!incTrack.containsKey(entry.getKey())) {
            			incTrack.put(entry.getKey(), entry.getValue());
            			bootstrapNodes.add(entry.getKey());
            			
                         
                    }
            		//update incarnation number
            		if(incTrack.containsKey(entry.getKey())&& incTrack.get(entry.getKey()) <entry.getValue()){
            			incTrack.remove(entry.getKey());
            			incTrack.put(entry.getKey(), entry.getValue());
            			
            		}
            	}
            	
            	
            	
            }
            
         // Improvement:  When receiving a ping meassage from a suspected node, the node should be declare alive
     	     if(suspectedNodes.containsKey(event.getSource())){
     	    	aliveNodes.put(event.getSource(), suspectedNodes.get(event.getSource()));
     	    	ping.setAlive(aliveNodes);
             	pong.setAlive(aliveNodes);
             	suspectedNodes.remove(event.getSource());
     	     }
            
            
            
            
            
            
            //update parents list
            if(!event.getContent().getUpdateParent().isEmpty()){
            	for (Map.Entry<NatedAddress, Set<NatedAddress>> entry : event.getContent().getUpdateParent().entrySet()) {
            	if(newparents.containsKey(entry.getKey())){
            		newparents.remove(entry.getKey());
            		newparents.put(entry.getKey(), entry.getValue());
        		}
        		
            	}
            }
            
            
            // Alive overrides suspect and alive
       
          if(!event.getContent().getAlive().isEmpty()){
          	for (Map.Entry<NatedAddress,Integer> entry : event.getContent().getAlive().entrySet()) {
          		if(suspectedNodes.containsKey(entry.getKey())&& suspectedNodes.get(entry.getKey())< entry.getValue()){
          			suspectedNodes.remove(entry.getKey());
          			aliveNodes.put(entry.getKey(),entry.getValue());
          			cancelPongTimeout(entry.getKey());
         			cancelpingReqTimeout(entry.getKey());
          			
          			
          		}
          		if(aliveNodes.containsKey(entry.getKey())&& aliveNodes.get(entry.getKey())< entry.getValue()){
          			aliveNodes.remove(entry.getKey());
          			aliveNodes.put(entry.getKey(),entry.getValue());
          			
          			
          		}
          		if(!aliveNodes.containsKey(entry.getKey())){
          			
          			aliveNodes.put(entry.getKey(), entry.getValue());
          			
          		}
          	
          		
          	}
         // Message Limitation
          	aliveNodeslimit = new HashMap<NatedAddress,Integer>();
        
          	for (int i = 0; i < 3; i++) {
        		NatedAddress[] keys = aliveNodes.keySet().toArray(new NatedAddress[0]);
            	Random random = new Random();
            	
            		NatedAddress randomKey = keys[random.nextInt(keys.length)];
                	Integer randomValue = aliveNodes.get(randomKey);
                	aliveNodeslimit.put(randomKey, randomValue);
                	
                	
            	
            	
        	}
         	
         	ping.setAlive(aliveNodeslimit);
         	pong.setAlive(aliveNodeslimit);
         	
          	
          }
//          
//          //Suspect overrides suspect and alive
          
          if(!(event.getContent().getSuspect().isEmpty())){
          	
          	for (Map.Entry<NatedAddress,Integer> entry : event.getContent().getSuspect().entrySet()) {
          		
         			 if(!suspectedNodes.containsKey(entry.getKey())){
         				 suspectedNodes.put(entry.getKey(), entry.getValue());
         				
         			 }
         			 if(suspectedNodes.containsKey(entry.getKey())){
         				 
         				 if(suspectedNodes.get(entry.getKey())< entry.getValue()){
         					suspectedNodes.remove(entry.getKey());
                      	suspectedNodes.put(entry.getKey(),entry.getValue());
         				 }
         				
         			 }
         			
         			 if( aliveNodes.containsKey(entry.getKey()) && aliveNodes.get(entry.getKey())< entry.getValue()){
         				 aliveNodes.remove(entry.getKey());
                  	 suspectedNodes.put(entry.getKey(),entry.getValue());
                   }
         			 if(entry.getKey().equals(selfAddress)){
         				 inc++;
         				aliveNodes.put(selfAddress, inc);
         				ping.setAlive(aliveNodes);
                  	pong.setAlive(aliveNodes);
                  	log.info("{} says I am alive:{}", new Object[]{selfAddress.getId()});
                  	suspectedNodes.remove(selfAddress);
         			 }
         			
         		 }
          	
          	
          	
          	
          	
          	
          	//Message limitation
          	suspectedNodeslimit = new HashMap<NatedAddress,Integer>();
          
            for (int i = 0; i < 3; i++) {
        		
        		NatedAddress[] keys = suspectedNodes.keySet().toArray(new NatedAddress[0]);
            	Random random = new Random();
            	if(keys.length!=0){
            		NatedAddress randomKey = keys[random.nextInt(keys.length)];
                	Integer randomValue = suspectedNodes.get(randomKey);
                	suspectedNodeslimit.put(randomKey, randomValue);
                	
                
            	}
            		
            	
            	
        	}
        	
          	ping.setSuspect(suspectedNodeslimit);
          	pong.setSuspect(suspectedNodeslimit);
          	
          	
          //	log.info("{} says suspected nodes are:{}", new Object[]{selfAddress.getId(),suspectedNodes});
          }
         
          //Confirmation overrides suspect and alive	
          if(!event.getContent().getConfirm().isEmpty()){
          	for (Map.Entry<NatedAddress,Integer> entry : event.getContent().getConfirm().entrySet()) {
          		if(!confirm.containsKey(entry.getKey()) &&suspectedNodes.containsKey(entry.getKey())){
          			suspectedNodes.remove(entry.getKey());
          			confirm.put(entry.getKey(), entry.getValue());
          		}
          		if(!confirm.containsKey(entry.getKey()) &&aliveNodes.containsKey(entry.getKey())){
          			aliveNodes.remove(entry.getKey());
          			confirm.put(entry.getKey(), entry.getValue());
          		}
          		if(!confirm.containsKey(entry.getKey())){
          			confirm.put(entry.getKey(), entry.getValue());
          		}
          		if(bootstrapNodes.contains(entry.getKey())){
          			bootstrapNodes.remove(entry.getKey());
          			
          		}
          		cancelPongTimeout(entry.getKey());
      			cancelpingReqTimeout(entry.getKey());
          		
          	}
         
        	// Message Limitation
          	confirmlimit = new HashMap<NatedAddress,Integer>();
          	
          	for (int i = 0; i < 3; i++) {
        		NatedAddress[] keys = confirm.keySet().toArray(new NatedAddress[0]);
            	Random random = new Random();
            	
            		NatedAddress randomKey = keys[random.nextInt(keys.length)];
                	Integer randomValue = confirm.get(randomKey);
                	confirmlimit.put(randomKey, randomValue);
                	
                	
            	
            	
        	}
        	

          	ping.setConfirm(confirm);
            pong.setConfirm(confirm);
           
          	log.info("{} says dead:{}", new Object[]{selfAddress.getId(),confirm});
              
          }
//         
          log.info("{} respond pong to:{}", new Object[]{selfAddress.getId(), event.getHeader().getSource()});
         
          pong.setInc(incSelf);
          pong.setJoin(newNodes);
        
            trigger(new NetPong(selfAddress, event.getHeader().getSource(), pong), network);
        }

    };
    
    private Handler<NetPong> handlePong = new Handler<NetPong>() {
        @Override
        public void handle(NetPong event) {
        	

        	 if (!(event.getHeader().getSource() == null)) {
             	
             	
             	
                 
             	log.info("{} received pong from:{}", new Object[]{selfAddress.getId(), event.getHeader().getSource()});
             	cancelPongTimeout(event.getSource());
             	
             	
             	//indirect ping`s ACK 
                 if(indierctPnodes.contains(event.getSource())){
                 	indierctPnodes.remove(event.getSource());
               
                 	if(!pong.getAck().contains(event.getSource())){
                 		pong.getAck().add(event.getSource());
                 		
                 		
                 	}
               
                 	
                 	
                 }
                 
                 if(bootstrapNodes.contains(event.getSource())){    
                	 ack.addAll(event.getContent().getAck());
                 	for (NatedAddress natedAddress : ack) {
                 		 if(suspectedNodes.containsKey(natedAddress)){
                          	suspectedNodes.remove(natedAddress);
                          	pong.getAck().remove(natedAddress);
                          	if(incTrack.get(target)==null){
                          		aliveNodes.put(natedAddress, 0);
                              	cancelpingReqTimeout(natedAddress);
                          	}
                          	else{
                          		aliveNodes.put(natedAddress, incTrack.get(target));
                              	cancelpingReqTimeout(natedAddress);
                          	}
                          }
 					}
                 	ack.clear();
                
                 	ping.setAlive(aliveNodes);
                 	pong.setAlive(aliveNodes);
                 	
                 	
                 	
                 }
                 
          
            	//detect alive or new nodes coming,and keeps everyone has the latest incarnation number of other alive members
                 if(!(event.getContent().getInc()==null)){
                 	for (Map.Entry<NatedAddress,Integer> entry : event.getContent().getInc().entrySet()) {
                 		//new node 
                 		if (!incTrack.containsKey(entry.getKey())) {
                 			incTrack.put(entry.getKey(), entry.getValue());
                 			
                         }
                 		//update incarnation number
                 		if(incTrack.containsKey(entry.getKey())&& incTrack.get(entry.getKey()) <entry.getValue()){
                 			incTrack.remove(entry.getKey());
                 			incTrack.put(entry.getKey(), entry.getValue());
                 		}
                 	}
                 	
                 }
            // Alive overrides suspect and alive    
                 if(!event.getContent().getAlive().isEmpty()){
                 	for (Map.Entry<NatedAddress,Integer> entry : event.getContent().getAlive().entrySet()) {
                 		
                 		if(suspectedNodes.containsKey(entry.getKey())&& suspectedNodes.get(entry.getKey())< entry.getValue()){
                 			suspectedNodes.remove(entry.getKey());
                 			aliveNodes.put(entry.getKey(),entry.getValue());
                 			cancelPongTimeout(entry.getKey());
                 			cancelpingReqTimeout(entry.getKey());
                 			
                 		}
                 		
                 		if(aliveNodes.containsKey(entry.getKey())&& aliveNodes.get(entry.getKey())< entry.getValue()){
                 			
                 			
                 			aliveNodes.remove(entry.getKey());
                 			aliveNodes.put(entry.getKey(),entry.getValue());

                 			
                 		}
                 		if(!aliveNodes.containsKey(entry.getKey())){
                 			
                 			aliveNodes.put(entry.getKey(), entry.getValue());

                 			
                 		}
                 		
                 		
                 	}
                 	//Message Limitation
                 	aliveNodeslimit = new HashMap<NatedAddress,Integer>();
                	
                	for (int i = 0; i < 3; i++) {
                		NatedAddress[] keys = aliveNodes.keySet().toArray(new NatedAddress[0]);
                    	Random random = new Random();
                    	
                    		NatedAddress randomKey = keys[random.nextInt(keys.length)];
                        	Integer randomValue = aliveNodes.get(randomKey);
                        	aliveNodeslimit.put(randomKey, randomValue);
                        	
                        	
                    	
                    	
                	}
                	
                 	ping.setAlive(aliveNodeslimit);
                 	pong.setAlive(aliveNodeslimit);
                 	
                 	
                 }
//                 
//                 //Suspect overrides suspect and alive
                 
                 if(!event.getContent().getSuspect().isEmpty()){
                 	for (Map.Entry<NatedAddress,Integer> entry : event.getContent().getSuspect().entrySet()) {
                 		
                			 if(!suspectedNodes.containsKey(entry.getKey())){
                				 suspectedNodes.put(entry.getKey(), entry.getValue());
                			 }
                			 if(suspectedNodes.containsKey(entry.getKey())){
                				 if(suspectedNodes.get(entry.getKey())< entry.getValue()){
                					suspectedNodes.remove(entry.getKey());
                             	suspectedNodes.put(entry.getKey(),entry.getValue());
                				 }
                				
                			 }
            				
            			 
                			 if( aliveNodes.containsKey(entry.getKey()) && aliveNodes.get(entry.getKey())<= entry.getValue()){
                				 aliveNodes.remove(entry.getKey());
                         	 suspectedNodes.put(entry.getKey(),entry.getValue());
                          }
                			 if(entry.getKey().equals(selfAddress)){
                				 inc++;
                				aliveNodes.put(selfAddress, inc);
                				aliveNodeslimit = new HashMap<NatedAddress,Integer>();
                				aliveNodeslimit.put(selfAddress, inc);
                				
                				
                				for (int i = 0; i < 1; i++) {
                            		NatedAddress[] keys = aliveNodes.keySet().toArray(new NatedAddress[0]);
                                	Random random = new Random();
                                	
                                		NatedAddress randomKey = keys[random.nextInt(keys.length)];
                                    	Integer randomValue = aliveNodes.get(randomKey);
                                    	aliveNodeslimit.put(randomKey, randomValue);
                                    	
                                   
                                	
                                	
                            	}
                            	
                             	ping.setAlive(aliveNodeslimit);
                             	pong.setAlive(aliveNodeslimit);
                             	
//                				ping.setAlive(aliveNodes);
//	                         	pong.setAlive(aliveNodes);
	                         	log.info("{} says I am alive:{}", new Object[]{selfAddress.getId()});
	                         	suspectedNodes.remove(selfAddress);
                			 }
                			
                		 }
                 	//Message Limitation
                 	suspectedNodeslimit = new HashMap<NatedAddress,Integer>();
                 	
                 	for (int i = 0; i < 3; i++) {
                		NatedAddress[] keys = suspectedNodes.keySet().toArray(new NatedAddress[0]);
                    	Random random = new Random();
                    	if(keys.length>0){
                    		NatedAddress randomKey = keys[random.nextInt(keys.length)];
                        	Integer randomValue = suspectedNodes.get(randomKey);
                        	suspectedNodeslimit.put(randomKey, randomValue);
                        	
                        	
                    	}
                    		
                    	
                    	
                	}
                	
                  	ping.setSuspect(suspectedNodeslimit);
                  	pong.setSuspect(suspectedNodeslimit);
                  	
                  	
                  

                 	
                 	
                 
                 	//log.info("{} says suspected nodes are:{}", new Object[]{selfAddress.getId(),suspectedNodes});
                 	
                 }
//                 
                 
                 //Confirmation overrides suspect and alive	
                 if(!event.getContent().getConfirm().isEmpty()){
                 	for (Map.Entry<NatedAddress,Integer> entry : event.getContent().getConfirm().entrySet()) {
                 		if(!confirm.containsKey(entry.getKey()) &&suspectedNodes.containsKey(entry.getKey())){
                 			suspectedNodes.remove(entry.getKey());
                 			confirm.put(entry.getKey(), entry.getValue());
                 			
                 		}
                 		if(!confirm.containsKey(entry.getKey()) &&aliveNodes.containsKey(entry.getKey())){
                 			aliveNodes.remove(entry.getKey());
                 			confirm.put(entry.getKey(), entry.getValue());
                 		}
                 		if(!confirm.containsKey(entry.getKey())){
                 			confirm.put(entry.getKey(), entry.getValue());
                 		}
                 		if(bootstrapNodes.contains(entry.getKey())){
                 			
                 			bootstrapNodes.remove(entry.getKey());
                 			
                 		}
                 		cancelPongTimeout(entry.getKey());
             			cancelpingReqTimeout(entry.getKey());
                 		
                 	}
                 	
                 	
                 // Message Limitation
                 	confirmlimit = new HashMap<NatedAddress,Integer>();
                 	
                 	for (int i = 0; i < 2; i++) {
                 		NatedAddress[] keys = confirm.keySet().toArray(new NatedAddress[0]);
                    	Random random = new Random();
                    	
                    		NatedAddress randomKey = keys[random.nextInt(keys.length)];
                        	Integer randomValue = confirm.get(randomKey);
                        	confirmlimit.put(randomKey, randomValue);
                        	
                        	
                    	
					}
                	
               
                 	ping.setConfirm(confirmlimit);
                     pong.setConfirm(confirmlimit);
                    
                 	log.info("{} says dead:{}", new Object[]{selfAddress.getId(),confirm});
                 	
                 	
                 }
                 
//                
               
             }
            
        }

    };
    private Handler<NetPingReq> handlePingReq = new Handler<NetPingReq>(){
   	 @Override
        public void handle(NetPingReq event) {
   		 
   		 
   		 log.info("{} received pingRequest from:{}", new Object[]{selfAddress.getId(), event.getHeader().getSource()});
   		 for (Map.Entry<NatedAddress,Integer> entry : event.getContent().getIndierctPnodes().entrySet()) {
   			 if(!indierctPnodes.contains(entry.getKey())){
   				 indierctPnodes.add(entry.getKey());
   			 }
   		 }
   		 	for (NatedAddress natedAddress : indierctPnodes) {
   		 		if(natedAddress.getNatType()==NatType.OPEN){
   		 		 trigger(new NetPing(selfAddress, natedAddress, ping), network);
   				 log.info("{} as regular ping:{}", new Object[]{selfAddress.getId(), natedAddress});
   		 		}
   		 	}
   		
   		 trigger(new NetPong(selfAddress, event.getSource(), pong), network);
   		 
   	 }
   	
   	 
   };
    
    

    private Handler<PingTimeout> handlePingTimeout = new Handler<PingTimeout>() {

        @Override
        public void handle(PingTimeout event) {
        	incSelf.put(selfAddress, inc);
            ping.setInc(incSelf);
                List<NatedAddress> randomlist= new ArrayList<NatedAddress>();
               
               for(NatedAddress open: bootstrapNodes){
            	   if(open.getNatType()==NatType.OPEN){
            		   opens.add(open);
            	   }
               }
            
                for (NatedAddress partnerAddress1 : opens) {
                	
                	randomlist.add(partnerAddress1);
                	
                }
               if(!(randomlist.size()==0)){
            	  
            	   int r = new Random().nextInt(randomlist.size());
            	   
            		   trigger(new NetPing(selfAddress, randomlist.get(r), ping), network);
            	 
                       log.info("{} sending ping to partner:{}", new Object[]{selfAddress.getId(), randomlist.get(r)});
                       schedulePong(randomlist.get(r));
           }
                
        }

    };
    
    private Handler<PongTimeout> handlePongTimeout = new Handler<PongTimeout>() {
        @Override
        public void handle(PongTimeout event) {
        	
        	for (int i = 0; i < pongtimeouts.size(); i++) {
        		if(event.getTimeoutId().equals(pongtimeouts.get(i).getValue1())){
        			target=pongtimeouts.get(i).getValue0();
        			cancelPongTimeout(target);
        			
        			if(incTrack.get(target)==null){
        				suspectedNodes.put(target, 0);
        				pingR.getIndierctPnodes().put(target, 0);
        				schedulePingReqTimeout(target);
        			}else{
        				suspectedNodes.put(target, incTrack.get(target));
        				pingR.getIndierctPnodes().put(target, incTrack.get(target));
        				schedulePingReqTimeout(target);
        			}
        			
        		}
    			
    		}
        	// Message Limitation
        	suspectedNodeslimit = new HashMap<NatedAddress,Integer>();
        	
        	int l=0;
        	for (int i = 0; i < 3; i++) {
        		NatedAddress[] keys = suspectedNodes.keySet().toArray(new NatedAddress[0]);
            	Random random = new Random();
            	
            		NatedAddress randomKey = keys[random.nextInt(keys.length)];
                	Integer randomValue = suspectedNodes.get(randomKey);
                	suspectedNodeslimit.put(randomKey, randomValue);
                	
                	l++;
            	
            	
        	}
        	ping.setSuspect(suspectedNodeslimit);
        	pong.setSuspect(suspectedNodeslimit);
        	
        	// random K indirect
        	
        	// start Suspect time.
        	//Kmembers (correct member)
        	List<NatedAddress> correctmember= new ArrayList<NatedAddress>();
        	for (Map.Entry<NatedAddress,Integer> entry : suspectedNodes.entrySet()) {
        		for(NatedAddress partnerAddress : bootstrapNodes){
        			if(!partnerAddress.equals(entry.getKey())){
        				if(partnerAddress.getNatType()==NatType.OPEN){
        					correctmember.add(partnerAddress);
        				}
        				
        			}
        			
        		}
        		
        	}

        		int n = (int)(Math.random()*correctmember.size()); 
                
            	for (int i = 0; i < n; i++) {
            		Integer random = (int) (correctmember.size() * Math.random());
            		
            			Kmembers.add(correctmember.get(random));
            		
    			}
            	correctmember.clear();
            	
            	
            	  for (NatedAddress partnerAddress : Kmembers) {
                  	

                      log.info("{} sending pingRequest to partner:{}", new Object[]{selfAddress.getId(), partnerAddress});

                      trigger(new NetPingReq(selfAddress, partnerAddress, pingR), network);

                  }
            	  Kmembers.clear();

        }

    };
    private Handler<PingReqTimeout> handlePingReqTimeout = new Handler<PingReqTimeout>() {

        @Override
        public void handle(PingReqTimeout event) {
        	for (int i = 0; i < pingReqtimeouts.size(); i++) {
	        		if(suspectedNodes.containsKey(pingReqtimeouts.get(i).getValue0())){
	        			suspectedNodes.remove(pingReqtimeouts.get(i).getValue0());
	        		}
        			if(!confirm.containsKey(pingReqtimeouts.get(i).getValue0())){
        				confirm.put(pingReqtimeouts.get(i).getValue0(), incTrack.get(pingReqtimeouts.get(i).getValue0()));
        			
        			}
        			
        			
        			if(aliveNodes.containsKey(pingReqtimeouts.get(i).getValue0())){
                		aliveNodes.remove(pingReqtimeouts.get(i).getValue0());
                	}
        			bootstrapNodes.remove(pingReqtimeouts.get(i).getValue0());
                	
                	cancelpingReqTimeout(pingReqtimeouts.get(i).getValue0());
                    
        	}
        	// Message Limitation
        	confirmlimit = new HashMap<NatedAddress,Integer>();
        	
        	for (int i = 0; i < 3; i++) {
        		NatedAddress[] keys = confirm.keySet().toArray(new NatedAddress[0]);
            	Random random = new Random();
            	
            		NatedAddress randomKey = keys[random.nextInt(keys.length)];
                	Integer randomValue = confirm.get(randomKey);
                	confirmlimit.put(randomKey, randomValue);
                	
                	
            	
            	
        	}
        	

        	
        	ping.setConfirm(confirmlimit);
        	pong.setConfirm(confirmlimit);
        	
        	log.info("{} says dead:{}", new Object[]{selfAddress.getId(),confirm});

        }

    };
    
    
    

    private Handler<StatusTimeout> handleStatusTimeout = new Handler<StatusTimeout>() {

        @Override
        public void handle(StatusTimeout event) {
        	 log.info("{} sending status to aggregator:{}", new Object[]{selfAddress.getId(), aggregatorAddress});
             trigger(new NetStatus(selfAddress, aggregatorAddress, new Status(receivedPings)), network);
             Set<NatedAddress> send = new HashSet<NatedAddress>();
             for (Map.Entry<NatedAddress,Integer> entry : confirm.entrySet()) {
             	send.add(entry.getKey());
             	  trigger(new NetStatus(selfAddress, aggregatorAddress, new Status(entry.getKey())), network);
             }
            
            // trigger(new NetStatus(selfAddress, aggregatorAddress, new Status(send)), network);
        }

    };

    private void schedulePeriodicPing() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(2000, 2000);
        PingTimeout sc = new PingTimeout(spt);
        spt.setTimeoutEvent(sc);
        pingTimeoutId = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelPeriodicPing() {
        CancelTimeout cpt = new CancelTimeout(pingTimeoutId);
        trigger(cpt, timer);
        pingTimeoutId = null;
    }

    private void schedulePeriodicStatus() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(1000,1000);
        StatusTimeout sc = new StatusTimeout(spt);
        spt.setTimeoutEvent(sc);
        statusTimeoutId = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelPeriodicStatus() {
        CancelTimeout cpt = new CancelTimeout(statusTimeoutId);
        trigger(cpt, timer);
        statusTimeoutId = null;
    }

    
    private void schedulePong(NatedAddress d) {
        ScheduleTimeout spt = new ScheduleTimeout(1000);
        PongTimeout sc = new PongTimeout(spt);
        spt.setTimeoutEvent(sc);
        pongTimeoutId = sc.getTimeoutId();
        pongtimeouts.add(Pair.with(d,pongTimeoutId)); 
       
        trigger(spt, timer);

    }

    
    private  void cancelPongTimeout(NatedAddress d) {
    	
    	
    	for (int i = 0; i < pongtimeouts.size(); i++) {
    		if( d.equals(pongtimeouts.get(i).getValue0())){
    			pongTimeoutId=pongtimeouts.get(i).getValue1();
    			CancelTimeout cpt = new CancelTimeout(pongTimeoutId);
    	        trigger(cpt, timer);
    	        pongtimeouts.remove(pongtimeouts.get(i));
    	        pongTimeoutId=null;
    	       // break;
    		}
			
		}

    }
    
    private void schedulePingReqTimeout(NatedAddress partnerAddress) {
        ScheduleTimeout spt = new ScheduleTimeout(5000);
        PingReqTimeout sc = new PingReqTimeout(spt);
        spt.setTimeoutEvent(sc);
        pingReqTimeoutId = sc.getTimeoutId();
        pingReqtimeouts.add(Pair.with(partnerAddress,pingReqTimeoutId));
        trigger(spt, timer);
    }
    private void cancelpingReqTimeout(NatedAddress partnerAddress) {
    	for (int i = 0; i < pingReqtimeouts.size(); i++) {
    		if(partnerAddress.equals(pingReqtimeouts.get(i).getValue0())){
    			pingReqTimeoutId=pingReqtimeouts.get(i).getValue1();
    			CancelTimeout cpt = new CancelTimeout(pingReqTimeoutId);
    	        trigger(cpt, timer);
    	        pingReqtimeouts.remove(pingReqtimeouts.get(i));
    	        pingReqTimeoutId=null;
    	        break;
    		}
			
		}


    }
    public static class SwimInit extends Init<SwimComp> {

        public final NatedAddress selfAddress;
        public final Set<NatedAddress> bootstrapNodes;
        public final NatedAddress aggregatorAddress;

        public SwimInit(NatedAddress selfAddress, Set<NatedAddress> bootstrapNodes, NatedAddress aggregatorAddress) {
            this.selfAddress = selfAddress;
            this.bootstrapNodes = bootstrapNodes;
            this.aggregatorAddress = aggregatorAddress;
        }
    }

    private static class StatusTimeout extends Timeout {

        public StatusTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }

    private static class PingTimeout extends Timeout {

        public PingTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }
    private static class PongTimeout extends Timeout {

        public PongTimeout(ScheduleTimeout request) {
            super(request);
        }
    }
    private static class PingReqTimeout extends Timeout {

        protected PingReqTimeout(ScheduleTimeout request) {
			super(request);
			// TODO Auto-generated constructor stub
		}

		
    }
}
