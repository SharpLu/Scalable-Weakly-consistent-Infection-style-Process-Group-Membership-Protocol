package se.kth.swim.msg.net;

import se.kth.swim.msg.PingReq;

import se.sics.kompics.network.Header;
import se.sics.p2ptoolbox.util.network.NatedAddress;

public class NetPingReq extends NetMsg<PingReq>{

	public NetPingReq(NatedAddress src, NatedAddress dst, PingReq content) {
		super(src, dst, content);
		// TODO Auto-generated constructor stub
	}

	@Override
	public NetMsg copyMessage(Header<NatedAddress> newHeader) {
		// TODO Auto-generated method stub
		return null;
	}
	 private NetPingReq(Header<NatedAddress> header, PingReq content) {
	        super(header, content);
	    }



}
