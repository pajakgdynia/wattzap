/*
 * This file is part of Wattzap Community Edition.
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

import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.UserPreferences;

/**
 *
 * @author Jarek
 */
public class PowerSensor extends AntSensor {
    private static final int POWER_CHANNEL_PERIOD = 8182; // ~4.0049Hz
	private static final int POWER_DEVICE_TYPE = 11; // 0x0B

    private int eventCount = -1;

    @Override
    public int getSensorType() {
        return POWER_DEVICE_TYPE;
    }

    @Override
    public int getSensorPeriod() {
        return POWER_CHANNEL_PERIOD;
    }

    @Override
    public int getTransmissionType() {
        return 0x05;
    }

    private final AntCumulativeComp speedComp = new AntCumulativeComp(
            4, 2, 4 * 2048, // revolution/4s => min speed ~2km/h
            2, 1, 16, // max wheel rotations per second, max speed ~120km/h
            6 // about 1.5s to get the average
    );
    // wheel circumference [m], taken from configuration
    double wheelSize = 1.496;

    @Override
    public void storeReceivedData(long time, int[] data) {
        double speed;
        // torque, smoothness, pedal balance, etc are not used.
        switch (data[0]) {
            // Standard Power-Only
            case 0x10:
                // cadence.. 255 means not valid
                if (data[3] != 255) {
                    setValue(SourceDataEnum.CADENCE, data[3]);
                }
                // proper power value is reported only when eventCount changed,
                // otherwise last one is reported (and might be not valid anymore)
                if (eventCount != data[1]) {
                    setValue(SourceDataEnum.POWER, data[6] + (data[7] << 8));
                    eventCount = data[1];
                }
                break;
            // Standard Wheel Torque
            case 0x11:
                // the only speed source
                speed = speedComp.compute(time, data);
                if (speed > 0.0) {
                    // speed => convert to km/h
                    setValue(SourceDataEnum.WHEEL_SPEED, 3.6 * wheelSize * speed);
                }
                // computed cadence
                if (data[3] != 255) {
                    setValue(SourceDataEnum.CADENCE, data[3]);
                }
                break;
            // Standard Crank Torque
            case 0x12:
                // power measured on crank, delivers only cadence
                if (data[3] != 255) {
                    setValue(SourceDataEnum.CADENCE, data[3]);
                }
                break;
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case POWER:
            case WHEEL_SPEED:
            case CADENCE:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void configChanged(UserPreferences property) {
        wheelSize = property.getWheelsize() / 1000.0;
    }
}
