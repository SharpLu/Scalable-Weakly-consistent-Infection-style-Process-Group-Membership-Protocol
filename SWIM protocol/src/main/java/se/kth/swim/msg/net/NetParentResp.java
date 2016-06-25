package se.kth.swim.msg.net;

import se.kth.swim.msg.ParentResp;

import se.sics.kompics.network.Header;
import se.sics.p2ptoolbox.util.network.NatedAddress;

public class NetParentResp extends NetMsg<ParentResp>{

//	public NetParentResp(NatedAddress src, NatedAddress dst, ParentResp content) {
//		super(src, dst, content);
//		// TODO Auto-generated constructor stub
//	}
//
//	@Override
//	public NetMsg copyMessage(Header<NatedAddress> newHeader) {
//		// TODO Auto-generated method stub
//		return null;
//	}
	
	 public NetParentResp(NatedAddress src, NatedAddress dst, ParentResp parentResp) {
	        super(src, dst, parentResp);
	    }

	    private NetParentResp(Header<NatedAddress> header, ParentResp content) {
	        super(header, content);
	    }

	    @Override
	    public NetMsg copyMessage(Header<NatedAddress> newHeader) {
	        return new NetParentResp(newHeader, getContent());
	    }


}
