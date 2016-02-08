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
import com.wattzap.model.dto.Telemetry;

/**
 * Module that target power from the route (mostly TRN, IMF and TCX files)
 *
 * @author Jarek
 */
@SelectableDataSourceAnnotation
public class TargetPowerFromRoute extends TelemetryHandler {
    private RouteReader reader = null;

    @Override
    public String getPrettyName() {
        return "route_target_power";
    }

    @Override
    public SourceDataHandlerIntf initialize() {
        super.initialize();
		MessageBus.INSTANCE.register(Messages.CLOSE, this);
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
        return this;
    }

    @Override
    public void release() {
		MessageBus.INSTANCE.unregister(Messages.GPXLOAD, this);
		MessageBus.INSTANCE.unregister(Messages.CLOSE, this);
        super.release();
    }

    @Override
    public void callback(Messages m, Object o) {
        super.callback(m, o);
        switch (m) {
            case GPXLOAD:
                reader = (RouteReader) o;
                break;
            case CLOSE:
                reader = null;
                break;
        }
    }

   @Override
    public void configChanged(UserPreferences pref) {
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        return data == SourceDataEnum.TARGET_POWER;
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        if ((reader != null) && (reader.provides(SourceDataEnum.TARGET_POWER))) {
            setValue(SourceDataEnum.TARGET_POWER, reader.getValue(SourceDataEnum.TARGET_POWER));
        } else {
            setValue(SourceDataEnum.TARGET_POWER, -1);
        }
    }
}
