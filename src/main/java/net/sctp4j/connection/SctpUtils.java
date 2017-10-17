package net.sctp4j.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpMapper;
import net.sctp4j.core.SctpPorts;
import net.sctp4j.core.UdpServerLink;
import net.sctp4j.origin.Sctp;

public class SctpUtils {

	private static final int THREADPOOL_MULTIPLIER = 3;

	private static final Logger LOG = LoggerFactory.getLogger(SctpUtils.class);
	
	@Getter
	private static final SctpMapper mapper = new SctpMapper();
	@Getter
	private static final ExecutorService threadPoolExecutor = Executors
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * THREADPOOL_MULTIPLIER);

	@Getter
	@Setter
	private static UdpServerLink link;
	
	@Getter
	private static volatile boolean isInitialized = false;

	public static synchronized void init(final InetAddress serverAddress, final int sctpServerPort, SctpDataCallback cb)
			throws SocketException {

		if (isInitialized) {
			return; // we only need to init once
		} else {
			Sctp.init();
			isInitialized = true;
		}

		if (cb == null) {
			cb = new SctpDefaultConfig().getCb();
		}

		if (!checkFreePort(sctpServerPort) || !checkRange(sctpServerPort)) {
			link = new UdpServerLink(mapper, serverAddress, cb);
		} else {
			link = new UdpServerLink(mapper, serverAddress, sctpServerPort, cb);
		}
	}

	private static boolean checkFreePort(final int sctpServerPort) {
		return SctpPorts.getInstance().isFreePort(sctpServerPort);
	}

	private static boolean checkRange(final int sctpServerPort) {
		return sctpServerPort <= 65535 || sctpServerPort > 0;
	}

	public static Promise<Object, Exception, Object> shutdownSctp(UdpServerLink customLink, SctpMapper customMapper) {
 		Deferred<Object, Exception, Object> d = new DeferredObject<>();
		
 		threadPoolExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				LOG.debug("sctp4j shutdown initialized");
				
				if (customLink == null) {
					link.close();
				} else {
					customLink.close();
				}
				
				if (customMapper == null) {
					mapper.shutdown();
				} else {
					customMapper.shutdown();
				}
				
				SctpPorts.shutdown();
				
				try {
					Sctp.finish();
				} catch (IOException e) {
					d.reject(e);
				}
				
				LOG.debug("sctp4j shutdown done");
				d.resolve(null);
			}
		});
		
		return d.promise();
	}
}
