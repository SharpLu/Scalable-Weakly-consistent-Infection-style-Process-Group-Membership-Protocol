package se.kth.swim.msg.net;

import se.kth.swim.msg.ParentReq;
import se.kth.swim.msg.ParentResp;
import se.sics.kompics.network.Header;
import se.sics.p2ptoolbox.util.network.NatedAddress;



public class NetParentReq extends NetMsg<ParentReq>{

	

//	public NetParentReq(NatedAddress src, NatedAddress dst, ParentReq content) {
//		super(src, dst, content);
//		// TODO Auto-generated constructor stub
//	}
//
//	@Override
//	public NetMsg copyMessage(Header<NatedAddress> newHeader) {
//		// TODO Auto-generated method stub
//		return null;
//	}
	 public NetParentReq(NatedAddress src, NatedAddress dst,ParentReq parentReq) {
	        super(src, dst,parentReq);
	    }

	    private NetParentReq(Header<NatedAddress> header, ParentReq content) {
	        super(header, content);
	    }

	    @Override
	    public NetMsg copyMessage(Header<NatedAddress> newHeader) {
	        return new NetParentReq(newHeader, getContent());
	    }




}
