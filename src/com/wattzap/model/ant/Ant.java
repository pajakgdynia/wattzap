/* This file is part of Wattzap Community Edition.
 *
 * Wattzap Community Edtion is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wattzap Community Edition is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wattzap.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.wattzap.model.ant;

import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.cowboycoders.ant.Channel;
import org.cowboycoders.ant.NetworkKey;
import org.cowboycoders.ant.Node;
import org.cowboycoders.ant.events.MessageCondition;
import org.cowboycoders.ant.events.MessageConditionFactory;
import org.cowboycoders.ant.interfaces.AntTransceiver;
import org.cowboycoders.ant.messages.SlaveChannelType;
import org.cowboycoders.ant.messages.commands.ChannelRequestMessage;
import org.cowboycoders.ant.messages.commands.ChannelRequestMessage.Request;
import org.cowboycoders.ant.messages.data.BroadcastDataMessage;
import org.cowboycoders.ant.messages.responses.ChannelIdResponse;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.SensorSubsystemTypeEnum;
import com.wattzap.model.UserPreferences;
import java.util.ArrayList;
import java.util.List;

/**
 * Gets data from Ant device and calculates speed, distance, cadence etc.
 *
 * @author David George
 * @date 30 May 2013
 */
public class Ant implements MessageCallback, AntSensorSubsystemIntf {
	private static Logger logger = LogManager.getLogger("Ant");
	public static final Level LOG_LEVEL = Level.SEVERE;

    /*
	 * This should match the device you are connecting with. Some devices are
	 * put into pairing mode (which sets this bit).
	 *
	 * Note: Many ANT+ sport devices do not set this bit (eg. HRM strap).
	 *
	 * See ANT+ docs.
	 */
	private static final boolean PAIRING_FLAG = false;

	/*
	 * Should match device transmission id (0-255). Special rules apply for
	 * shared channels. See ANT+ protocol.
	 *
	 * 0: wildcard, matches any value (slave only)
	 */
	private static final int DEFAULT_TRANSMISSION_TYPE = 1;

	private static final int ANT_SPORT_FREQ = 57; // 0x39

    private final List<Channel> channels = new ArrayList<>();
    private boolean running;

	private Node node = null;
	private NetworkKey key = null;

    private UserPreferences userPrefs = UserPreferences.INSTANCE;


	public Ant() {
        running = false;
	}

	public void initialize() {
        MessageBus.INSTANCE.register(Messages.START, this);
		MessageBus.INSTANCE.register(Messages.STOP, this);
        MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);

		/*
		 * Choose driver: AndroidAntTransceiver or AntTransceiver
		 *
		 * AntTransceiver(int deviceNumber) deviceNumber : 0 ... number of usb
		 * sticks plugged in 0: first usb ant-stick
		 */
        AntTransceiver antchip = null;
        if (userPrefs.isANTUSB()) {
            antchip = new AntTransceiver(0, AntTransceiver.ANTUSBM_ID);
        } else {
            antchip = new AntTransceiver(0);
        }
        // initialises node with chosen driver
        node = new Node(antchip);

        // ANT+ key
        key = new NetworkKey(0xB9, 0xA5, 0x21, 0xFB, 0xBD, 0x72, 0xC3, 0x45);
        key.setName("N:ANT+");

        // new subsystem was added
        MessageBus.INSTANCE.send(Messages.SUBSYSTEM, this);

        // optional: enable console logging with Level = LOG_LEVEL
        setupLogging();
	}

	public static void setupLogging() {
		// set logging level
		AntTransceiver.LOGGER.setLevel(LOG_LEVEL);
		ConsoleHandler handler = new ConsoleHandler();
		// PUBLISH this level
		handler.setLevel(LOG_LEVEL);
		AntTransceiver.LOGGER.addHandler(handler);
	}

    public void release() {
        MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);
        MessageBus.INSTANCE.send(Messages.SUBSYSTEM, this);
        MessageBus.INSTANCE.send(Messages.SUBSYSTEM_REMOVED, this);
    }


	@Override
    public int getChannelId(Channel channel) {
		// build request
		ChannelRequestMessage msg = new ChannelRequestMessage(
				channel.getNumber(), Request.CHANNEL_ID);

		// response should be an instance of ChannelIdResponse
		MessageCondition condition =
                MessageConditionFactory.newInstanceOfCondition(ChannelIdResponse.class);

		try {

			// send request (blocks until reply received or timeout expired)
			ChannelIdResponse response = (ChannelIdResponse)
                    channel.sendAndWaitForMessage(msg, condition, 5L, TimeUnit.SECONDS, null);

			/*
			 * System.out.println();
			 * System.out.println("Device configuration: ");
			 * System.out.println("deviceID: " + response.getDeviceNumber());
			 * System.out.println("deviceType: " + response.getDeviceType());
			 * System.out.println("transmissionType: " + response.getTransmissionType());
			 * System.out.println("pairing flag set: " + response.isPairingFlagSet());
             * System.out.println();
			 */

            return response.getDeviceNumber();
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage());
		}

		return 0;
	}

	public void close() {
        if (!running) {
            return;
        }

        while (channels.size() != 0) {
            closeChannel(channels.get(0));
        }

        // cleans up : gives up control of usb device etc.
        node.stop();
        running = false;

        MessageBus.INSTANCE.send(Messages.SUBSYSTEM, this);
    }

	@Override
    public void open() {
		/* must be called before any configuration takes place */
		node.start();

		/* sends reset request : resets channels to default state */
		node.reset();

		// specs say wait 500ms after reset before sending any more host
		// commands
		try {
			Thread.sleep(500);
		} catch (InterruptedException ex) {

		}

		// sets network key of network zero
		node.setNetworkKey(0, key);
        running = true;
        MessageBus.INSTANCE.send(Messages.SUBSYSTEM, this);
	}

    @Override
	public void callback(Messages message, Object o) {
		switch (message) {
            case CONFIG_CHANGED:
                if (running) {
                    // close if disabled
                }
                break;
            case STOP:
                close();
                break;
            case START:
                open();
                break;
		}
	}

    @Override
    public SensorSubsystemTypeEnum getType() {
        return SensorSubsystemTypeEnum.ANT;
    }

    @Override
    public boolean isOpen() {
        return running;
    }

    @Override
    public Channel createChannel(int sensorId, AntSourceDataHandler sensorHandler) {
        // subsystem is closed.. cannot create new channel
        if (!isOpen()) {
            return null;
        }

        Channel channel = node.getFreeChannel();
		// Arbitrary name : useful for identifying channel
		channel.setName(sensorHandler.getSensorName());
		// use ant network key "N:ANT+"
		channel.assign("N:ANT+", new SlaveChannelType());
		// registers an instance of our callback with the channel
		channel.registerRxListener(sensorHandler, BroadcastDataMessage.class);
        // set channel configuration
		channel.setId(sensorId, sensorHandler.getSensorType(), DEFAULT_TRANSMISSION_TYPE, PAIRING_FLAG);
		channel.setFrequency(ANT_SPORT_FREQ);
		channel.setPeriod(sensorHandler.getSensorPeriod());
		// timeout before we give up looking for device
		channel.setSearchTimeout(Channel.SEARCH_TIMEOUT_NEVER);

		// start listening
		channel.open();
        // keep channel for close operation..
        channels.add(channel);
        return channel;
    }

    @Override
    public void closeChannel(Channel channel) {
        // if subsystem disabled, all channels were already freed
        if ((!isOpen()) || (channel == null)) {
            return;
        }

        if (channel.isFree()) {
            channel.close();
            channel.unassign();
            node.freeChannel(channel);
        }
        channels.remove(channel);
    }
}
