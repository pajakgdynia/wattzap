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
    private int zeroOffset = 500;
    private boolean ctfAutoZero;

    /*
    private int torqueTime = 0;
    private int ctfEvents = -1;
    private int torqueTicks;
    */

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
    private final AntCumulativeComp torqueComp = new AntCumulativeComp(
            2000, // ticks per second
            16, 8000, // 16 bits for ticks, but 8000 ticks allowed
            16, 2500, // 16 bits for torque impulses, but only power ~4kW allowe (offset 500 + slope 25 * 80)
            2 // 3 events to get proper value..
    );
    private final AntCumulativeComp cadenceComp = new AntCumulativeComp(
            2000, // ticks per second
            16, 8000, // 16 bits for ticks, but 8000 ticks allowed
            8, 5, // 8 bits for rotations, 300RPM is the max
            2 // 3 events to get proper value..
    );

    // wheel circumference [m], taken from configuration
    double wheelSize = 1.496;

    @Override
    public void storeReceivedData(long time, int[] data) {
        double speed;
        double frequency;
        double cadence;

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
                // It is not computed via computations, just reports this value
                if (data[3] != 255) {
                    setValue(SourceDataEnum.CADENCE, data[3]);
                }
                break;

            // CTF calibration data, changed automatically when autoZero is set.
            case 0x01:
                if ((data[1] == 0x10) && (data[2] ==0x01) && ctfAutoZero) {
                    UserPreferences.CTF_ZERO_OFFSET.setInt((data[6] << 8) + data[7]);
                }
                break;
            case 0x20:
                frequency = torqueComp.compute(time, (data[4] << 8) + data[5], (data[6] << 8) + data[7]);
                if (frequency > 0.0) {
                    setValue(SourceDataEnum.CTF_TORQUE_FREQUENCY, frequency);
                }

                cadence = cadenceComp.compute(time, (data[4] << 8) + data[5], data[1]);
                if (cadence > 0.0) {
                    setValue(SourceDataEnum.CADENCE, cadence * 60);
                }
                if ((frequency > 0.0) && (cadence > 0.0)) {
                    int slope = (data[2] << 8) + data[3];
                    double torque = (frequency - zeroOffset) / (slope / 10.0);
                    double power = torque * cadence * 2.0 * 3.141519;
                    setValue(SourceDataEnum.POWER, power);
                    setValue(SourceDataEnum.TARGET_POWER, power);
                }
                break;
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case WHEEL_SPEED:
            case POWER:
            case CADENCE:
            case CTF_TORQUE_FREQUENCY:
            case TARGET_POWER:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void configChanged(UserPreferences property) {
        if ((property == UserPreferences.WHEEL_SIZE) ||
            (property == UserPreferences.INSTANCE))
        {
            wheelSize = property.getWheelsize() / 1000.0;
        }
        if ((property == UserPreferences.CTF_OFFSET_AUTO_ZERO) ||
            (property == UserPreferences.INSTANCE))
        {
            ctfAutoZero = property.ctfOffsetAutoZero();
        }
        if ((property == UserPreferences.CTF_ZERO_OFFSET) ||
            (property == UserPreferences.INSTANCE))
        {
            zeroOffset = property.ctfZeroOffset();
        }
    }
}
