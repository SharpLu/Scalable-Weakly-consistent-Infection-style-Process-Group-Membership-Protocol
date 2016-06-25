package se.kth.nattraversal;

import se.kth.swim.croupier.msg.CroupierSample;
import se.kth.swim.croupier.msg.CroupierUpdate;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.PortType;

public class NatTraversalPort extends PortType{
	 {
	        
	        request(CroupierSample.class);
	        indication( Updateparent.class);
	    }

}
