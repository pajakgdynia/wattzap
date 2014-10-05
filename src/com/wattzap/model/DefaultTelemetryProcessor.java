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
package com.wattzap.model;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.Messages;
import com.wattzap.model.dto.Point;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.power.Power;

/**
 *
 * @author Jarek
 */
public class DefaultTelemetryProcessor extends TelemetryProcessor {
    private double totalWeight = 85.0;
    private int resistance;
    private boolean autoResistance;
    private boolean initialize;
    private VirtualPowerEnum virtualPower;
    private RouteReader routeData = null;
    private Power power = null;

    @Override
    public String getPrettyName() {
        return "DefaultTelemetryProcessor";
    }

    @Override
    public SourceDataProcessorIntf initialize() {
        initialize = true;
        super.initialize();
        MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
        // set default resistance..
        setValue(SourceDataEnum.RESISTANCE, resistance);
        initialize = false;
        return this;
    }

    @Override
    public void release() {
        MessageBus.INSTANCE.unregister(Messages.GPXLOAD, this);
        super.release();
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        if (initialize || (virtualPower != prefs.getVirtualPower())) {
            virtualPower = prefs.getVirtualPower();
            activate(virtualPower == VirtualPowerEnum.SPEED_TO_POWER);
        }
        if (initialize) {
            if (prefs.getResistance() == 0) {
                resistance = 1;
                autoResistance = true;
            } else {
                resistance = prefs.getResistance();
                autoResistance = false;
            }
        }

        totalWeight = prefs.getTotalWeight();
        power = prefs.getPowerProfile();
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            // main targets
            case SPEED:
            case POWER:

            // it shall be handled by TrainerHandler!
            case RESISTANCE:

            // these shall be made by RouteHandler!
            case ALTITUDE:
            case SLOPE:
            case LATITUDE:
            case LONGITUDE:
            case PAUSE:
                return true;

            default:
                return false;
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // We have a time value and rotation value, lets calculate the speed
        int powerWatts = power.getPower(t.getWheelSpeed(), t.getResistance());
        setValue(SourceDataEnum.POWER, powerWatts);

        // if we have GPX Data and Simulspeed is enabled calculate speed
        // based on power and gradient using magic sauce

        // these data shall be moved to routeHandler! Except simulated speed..
        boolean noMoreRoute = true;
        if (routeData != null) {
            // System.out.println("gettng point at distance " + distance);
            Point p = routeData.getPoint(t.getDistance());
            if (p != null) {
                // if slope is known
                if (routeData.routeType() == RouteReader.SLOPE) {
                    double realSpeed = 3.6 * power.getRealSpeed(totalWeight,
                        p.getGradient() / 100, powerWatts);
                    setValue(SourceDataEnum.SPEED, realSpeed);
                    noMoreRoute = false;
                } else {
                    System.out.println("Route type is " + routeData.routeType());
                }

                setValue(SourceDataEnum.ALTITUDE, p.getElevation());
                setValue(SourceDataEnum.SLOPE, p.getGradient());
                setValue(SourceDataEnum.LATITUDE, p.getLatitude());
                setValue(SourceDataEnum.LONGITUDE, p.getLongitude());
            } else {
                System.out.println("No point at " + t.getDistance());
            }
        } else {
            System.out.println("No route data at all!");
        }

        // default resistance taken from preferences
        if (autoResistance) {
            // Best matching (this is wheelSpeed best matches speed) shall be selected
            setValue(SourceDataEnum.RESISTANCE, 1);
        }

        // set pause at end of route or when no running, otherwise unpause
        if (noMoreRoute) {
            setValue(SourceDataEnum.PAUSE, 100.0);
            setValue(SourceDataEnum.SPEED, 0.0);
        } else if (getValue(SourceDataEnum.SPEED) < 0.01) {
            setValue(SourceDataEnum.PAUSE, 1.0);
        } else {
            setValue(SourceDataEnum.PAUSE, 0.0);
        }
    }

    @Override
    public void callback(Messages m, Object o) {
        switch (m) {
            case GPXLOAD:
                routeData = (RouteReader) o;
                /* no break */
            case STARTPOS:
                setValue(SourceDataEnum.PAUSE, 0);
                break;
        }
        super.callback(m, o);
    }
}
