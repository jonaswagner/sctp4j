/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sctp4nat.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.NotFoundException;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.util.SctpUtils;

/**
 * This class is heavily inspired by sctp4j's UdpLink class.
 * 
 * @author Jonas Wagner
 * 
 *         This class holds the {@link DatagramSocket} the outgoing SCTP
 *         connection attempts. It also forwards incoming SCPT packets, which
 *         need to be encoded first, because they contain a UDP header.
 *
 */
public class UdpClientLink implements NetworkLink {

	private final static Logger LOG = LoggerFactory.getLogger(UdpClientLink.class);

	/**
	 * Udp socket used for transport.
	 */
	private final DatagramSocket udpSocket;

	/**
	 * Destination <tt>InetSocketAddress</tt>.
	 */
	private final InetSocketAddress local;

	/**
	 * Destination <tt>InetSocketAddress</tt>.
	 */
	private final InetSocketAddress remote;

	/**
	 * Trigger to end the wrapper thread.
	 */
	private boolean isShutdown = false;

	/**
	 * Creates new instance of <tt>UdpClientLink</tt>.
	 */
	public UdpClientLink(final InetSocketAddress local, final InetSocketAddress remote, final SctpChannel so)
			throws IOException {
		so.setLink(this);
		this.local = local;
		this.remote = remote;
		this.udpSocket = new DatagramSocket(local.getPort(), local.getAddress());

		// Listening thread
		receive(remote, so);
	}

	/**
	 * Do not use this constructor without a valid {@link DatagramSocket}! Creates
	 * new instance of <tt>UdpClientLink</tt>.
	 */
	public UdpClientLink(final InetSocketAddress local, final InetSocketAddress remote, final SctpChannel so,
			final DatagramSocket udpSocket) {
		so.setLink(this);
		this.local = local;
		this.remote = remote;
		this.udpSocket = udpSocket;

		// Listening thread
		receive(remote, so);
	}

	/**
	 * Forwards the UDP packets to the native counterpart.
	 * 
	 * @param remote
	 *            {@link InetSocketAddress} of the remote endpoint
	 * @param so
	 *            The assigned {@link SctpChannel}
	 */
	private void receive(final InetSocketAddress remote, final SctpChannel so) {
		SctpUtils.getThreadPoolExecutor().execute(new Runnable() {
			public void run() {

				try {
					byte[] buff = new byte[UDP_DEFAULT_BUFFER_SIZE];
					DatagramPacket p = new DatagramPacket(buff, UDP_DEFAULT_BUFFER_SIZE);
					while (!isShutdown) {
						udpSocket.receive(p);
						so.onConnIn(p.getData(), p.getOffset(), p.getLength());
					}
				} catch (IOException e) {
					LOG.error(e.getMessage());
				}
				LOG.debug(
						"Link shutdown, closing udp connection to " + remote.getHostString() + ":" + remote.getPort());
			}
		});
	}

	@Override
	public void onConnOut(final SctpChannelFacade facade, final byte[] data, final int tos)
			throws IOException, NotFoundException {
		LOG.info("sending sctp packet from {}/{} to {}/{}, ", local.getAddress().getHostAddress(), local.getPort(),
				remote.getAddress().getHostAddress(), remote.getPort());
		DatagramPacket packet = new DatagramPacket(data, data.length, this.remote.getAddress(), this.remote.getPort());
		udpSocket.send(packet);
	}

	@Override
	public void close() {
		this.isShutdown = true;
		this.udpSocket.close();
	}

	@Override
	public String toString() {
		InetSocketAddress local = this.local;
		InetSocketAddress remote = this.remote;

		return "UdpClientLink(" + "Local(" + local.getAddress().getHostAddress() + ":" + local.getPort() + ")"
				+ ", Remote(" + remote.getAddress().getHostAddress() + ":" + remote.getPort() + "), shutdown is "
				+ isShutdown + ")";

	}

}
