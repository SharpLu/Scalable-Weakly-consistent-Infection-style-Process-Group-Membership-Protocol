package se.kth.nattraversal;

import java.util.HashSet;
import java.util.Set;




import se.kth.swim.croupier.util.Container;
import se.sics.p2ptoolbox.util.network.NatedAddress;

public class Updateparent<C extends Object> implements NatTraversalMsg.OneWay{
	
	//public final int overlayId;
    public final Set<Container<NatedAddress, C>> publicSample;
  
    public Set<NatedAddress> parents;
    public Updateparent(Set<Container<NatedAddress, C>> publicSample,Set<NatedAddress> parents) {
        
        this.publicSample = publicSample;
       this.parents=parents;
    }
    

    @Override
    public String toString() {
        return "SAMPLE";
    }
	
	
}
