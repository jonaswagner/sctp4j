package net.sctp4nat.connection;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Builder;
import net.sctp4nat.core.NetworkLink;
import net.sctp4nat.core.SctpDataCallback;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.exception.SctpInitException;
import net.sctp4nat.util.SctpUtils;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelBuilder;
import net.sctp4nat.core.SctpChannelFacade;

@Builder
public class SctpConnection {

	private static final Logger LOG = LoggerFactory.getLogger(SctpConnection.class);

	private SctpDefaultConfig config;
	private InetSocketAddress local;
	private InetSocketAddress remote;
	private SctpDataCallback cb;
	private int localSctpPort;

	public Promise<SctpChannelFacade, Exception, Object> connect(final NetworkLink link) throws Exception {
		
		if (remote == null) {
			LOG.error("Remote InetSocketAddress was null. We can't connect to null!");
			throw new NullPointerException("Remote InetSocketAddress was null. We can't connect to null!");
		}

		if (local == null) {
			LOG.error("Local InetSocketAddress was null. We can't connect to null!");
			throw new NullPointerException("Local InetSocketAddress was null. We can't connect to null!");
		}

		if (config == null) {
			config = new SctpDefaultConfig();
		}

		if (cb == null) {
			cb = config.getCb();
		}

		if (localSctpPort == -1) {
			localSctpPort = remote.getPort();
		}

			
		SctpChannel socket = null;
		try {
		socket = new SctpChannelBuilder().remoteAddress(remote.getAddress()).remotePort(remote.getPort())
				.sctpDataCallBack(cb).mapper(SctpUtils.getMapper()).localSctpPort(localSctpPort).build();
		} catch (SctpInitException e) {
			LOG.error("Could not create SctpChannel, because Sctp is not initialized! Try SctpUtils.init()");
			throw new SctpInitException(e.getMessage());
		}

		if (socket == null) {
			throw new NullPointerException("Could not create SctpSocketAdapter!");
		}

		NetworkLink link2 = link;
		if (link == null) {
			link2 = new UdpClientLink(local, remote, socket);
		}

		final SctpChannel so = socket;

		if (link2 == null) {
			LOG.error("Could not create NetworkLink");
			releaseAssignedParams(so, new IOException("Could not create UdpClientLink"));
			throw new NullPointerException("NetworkLink was null!");
		}

		so.setLink(link2);

		Promise<SctpChannelFacade, Exception, Object> p = so.connect(remote);

		p.fail(new FailCallback<Exception>() {

			@Override
			public void onFail(Exception e) {
				LOG.error("Could not connect to remote host", e);
				releaseAssignedParams(so, e);
			}
		});

		return p;

	}

	private void releaseAssignedParams(SctpChannel so, Exception e) {
		SctpPorts.getInstance().removePort(so);
		so.close();
	}

}