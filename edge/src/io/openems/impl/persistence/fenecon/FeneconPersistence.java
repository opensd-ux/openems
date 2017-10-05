/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.persistence.fenecon;

import java.net.Inet4Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.HashMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.persistence.Persistence;
import io.openems.api.thing.Thing;
import io.openems.common.types.FieldValue;
import io.openems.common.types.NullFieldValue;
import io.openems.common.types.NumberFieldValue;
import io.openems.common.types.StringFieldValue;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.core.Config;
import io.openems.core.ConfigFormat;
import io.openems.core.Databus;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.websocket.EdgeWebsocketHandler;

// TODO make sure this is registered as ChannelChangeListener also to ConfigChannels
@ThingInfo(title = "FENECON Persistence", description = "Establishes the connection to FENECON Cloud.")
public class FeneconPersistence extends Persistence implements ChannelChangeListener {

	private final static String DEFAULT_CONFIG_LANGUAGE = "en";

	/*
	 * Config
	 */
	@ChannelInfo(title = "Apikey", description = "Sets the apikey for FENECON Cloud.", type = String.class)
	public final ConfigChannel<String> apikey = new ConfigChannel<String>("apikey", this).doNotPersist();

	@ChannelInfo(title = "Uri", description = "Sets the connection Uri to FENECON Cloud.", type = String.class, defaultValue = "\"wss://fenecon.de:443/openems-backend\"")
	public final ConfigChannel<String> uri = new ConfigChannel<String>("uri", this).doNotPersist();

	@ChannelInfo(title = "Sets the duration of each cycle in milliseconds", type = Integer.class)
	public ConfigChannel<Integer> cycleTime = new ConfigChannel<Integer>("cycleTime", this)
			.defaultValue(DEFAULT_CYCLETIME);

	/*
	 * Constructor
	 */
	public FeneconPersistence() {
		this.websocketHandler = new EdgeWebsocketHandler();
		this.reconnectingWebsocket = new ReconnectingWebsocket(this.websocketHandler, (websocket) -> {
			/*
			 * onOpen
			 */
			log.info("FENECON persistence connected [" + uri.valueOptional().orElse("") + "]");
			// Add current status of all channels to queue
			this.addCurrentValueOfAllChannelsToQueue();
			// Send current config
			try {
				WebSocketUtils.send( //
						websocket, //
						DefaultMessages.configQueryReply(
								Config.getInstance().getJson(ConfigFormat.OPENEMS_UI, DEFAULT_CONFIG_LANGUAGE)));
				log.info("Sent config to FENECON persistence.");
			} catch (NotImplementedException | ConfigException e) {
				log.error("Unable to send config: " + e.getMessage());
			}
		}, () -> {
			/*
			 * onClose
			 */
			log.error("FENECON persistence closed connection to uri [" + uri.valueOptional().orElse("") + "]");
		});
	}

	@Override
	public void init() {
		this.updateWebsocketParams();
	}

	/*
	 * Fields
	 */
	private static final int DEFAULT_CYCLETIME = 2000;
	private final EdgeWebsocketHandler websocketHandler;
	private final ReconnectingWebsocket reconnectingWebsocket;

	// Queue of data for the next cycle
	private HashMultimap<Long, FieldValue<?>> queue = HashMultimap.create();
	// Unsent queue (FIFO)
	private EvictingQueue<JsonObject> unsentCache = EvictingQueue.create(1000);
	private volatile Integer configuredCycleTime = DEFAULT_CYCLETIME;

	/*
	 * Methods
	 */
	private void updateWebsocketParams() {
		// TODO call on channel update URI + apikey
		Optional<String> apikeyOpt = this.apikey.valueOptional();
		if (apikeyOpt.isPresent()) {
			// set apikey header
			this.reconnectingWebsocket.addHttpHeader("apikey", apikeyOpt.get());

			Optional<String> uriOpt = this.uri.valueOptional();
			if (uriOpt.isPresent()) {
				try {
					URI uri = new URI(uriOpt.get());
					this.reconnectingWebsocket.setUri(Optional.of(uri));
				} catch (URISyntaxException e) {
					log.error("URI [" + uriOpt.get() + "] is invalid: " + e.getMessage());
					this.reconnectingWebsocket.setUri(Optional.empty());
					return;
				}
			} else {
				// URI is not present
				this.reconnectingWebsocket.setUri(Optional.empty());
			}
		}
	}

	/**
	 * Receives update events for all {@link ReadChannel}s, excluding {@link ConfigChannel}s via the {@link Databus}.
	 */
	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		// Update cycleTime of FENECON Persistence
		if (channel == cycleTime) {
			this.configuredCycleTime = cycleTime.valueOptional().orElse(DEFAULT_CYCLETIME);
		}
		this.addChannelValueToQueue(channel, newValue);
	}

	@Override
	protected void forever() {
		// Convert FieldVales in queue to JsonObject
		JsonObject j;
		synchronized (queue) {
			j = DefaultMessages.timestampedData(queue);
			queue.clear();
		}

		// Send data to Server
		if (this.send(j)) {
			// Successful

			// reset cycleTime
			resetCycleTime();

			// resend from cache
			for (Iterator<JsonObject> iterator = unsentCache.iterator(); iterator.hasNext();) {
				JsonObject jCached = iterator.next();
				boolean cacheWasSent = this.send(jCached);
				if (cacheWasSent) {
					iterator.remove();
				}
			}
		} else {
			// Failed to send

			// increase cycleTime
			increaseCycleTime();

			// cache data for later
			unsentCache.add(j);
		}
	}

	@Override
	protected void dispose() {
		this.reconnectingWebsocket.dispose();
	}

	/**
	 * Send message to websocket
	 *
	 * @param j
	 * @return
	 */
	private boolean send(JsonObject j) {
		return this.websocketHandler.send(j);
	}

	/**
	 * Gets the websocket handler
	 *
	 * @return
	 */
	public EdgeWebsocketHandler getWebsocketHandler() {
		return this.websocketHandler;
	}

	private void increaseCycleTime() {
		int currentCycleTime = this.getCycleTime();
		int newCycleTime;
		if (currentCycleTime < 30000 /* 30 seconds */) {
			newCycleTime = currentCycleTime * 2;
		} else {
			newCycleTime = currentCycleTime;
		}
		if (currentCycleTime != newCycleTime) {
			this.cycleTime.updateValue(newCycleTime, false);
		}
	}

	/**
	 * Cycletime is adjusted if connection to Backend fails. This method resets it to configured or default value.
	 */
	private void resetCycleTime() {
		int currentCycleTime = this.getCycleTime();
		int newCycleTime = this.configuredCycleTime;
		this.cycleTime.updateValue(newCycleTime, false);
		if (currentCycleTime != newCycleTime) {
			this.cycleTime.updateValue(newCycleTime, false);
		}
	}

	/**
	 * Add a channel value to the send queue
	 *
	 * @param channel
	 * @param valueOpt
	 */
	private void addChannelValueToQueue(Channel channel) {
		if (!(channel instanceof ReadChannel<?>)) {
			// TODO check for more types - see other addChannelValueToQueue method
			return;
		}
		ReadChannel<?> readChannel = (ReadChannel<?>) channel;
		this.addChannelValueToQueue(channel, readChannel.valueOptional());
	}

	/**
	 * Add a channel value to the send queue
	 *
	 * @param channel
	 * @param valueOpt
	 */
	private void addChannelValueToQueue(Channel channel, Optional<?> valueOpt) {
		// Ignore anything that is not a ReadChannel
		if (!(channel instanceof ReadChannel<?>)) {
			return;
		}
		ReadChannel<?> readChannel = (ReadChannel<?>) channel;
		// Ignore channels that shall not be persisted
		if (readChannel.isDoNotPersist()) {
			return;
		}

		// Get timestamp and round to seconds
		Long timestamp = System.currentTimeMillis() / 1000 * 1000;

		// Read and format value from channel
		String field = readChannel.address();
		FieldValue<?> fieldValue;
		if (!valueOpt.isPresent()) {
			fieldValue = new NullFieldValue(field);
		} else {
			Object value = valueOpt.get();
			if (value instanceof Number) {
				fieldValue = new NumberFieldValue(field, (Number) value);
			} else if (value instanceof String) {
				fieldValue = new StringFieldValue(field, (String) value);
			} else if (value instanceof Inet4Address) {
				fieldValue = new StringFieldValue(field, ((Inet4Address) value).getHostAddress());
			} else if (value instanceof Boolean) {
				fieldValue = new NumberFieldValue(field, ((Boolean) value) ? 1 : 0);
			} else if (value instanceof DeviceNature || value instanceof JsonElement || value instanceof Map
					|| value instanceof Set || value instanceof List || value instanceof ThingMap) {
				// ignore
				return;
			} else {
				log.warn("FENECON Persistence for value type [" + value.getClass().getName() + "] of channel ["
						+ channel.address() + "] is not implemented.");
				return;
			}
		}

		// Add timestamp + value to queue
		synchronized (queue) {
			queue.put(timestamp, fieldValue);
		}
	}

	/**
	 * On websocket open, add current values of all channels to queue. This is to prepare upcoming "channelChanged"
	 * events, where only changes are sent
	 */
	private void addCurrentValueOfAllChannelsToQueue() {
		ThingRepository thingRepository = ThingRepository.getInstance();
		for (Thing thing : thingRepository.getThings()) {
			for (Channel channel : thingRepository.getChannels(thing)) {
				this.addChannelValueToQueue(channel);
			}
		}
	}

	@Override
	protected int getCycleTime() {
		return cycleTime.valueOptional().orElse(DEFAULT_CYCLETIME);
	}

	@Override
	protected boolean initialize() {
		return this.reconnectingWebsocket.websocketIsOpen();
	}
}