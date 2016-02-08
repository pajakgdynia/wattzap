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
package com.wattzap.model;

import com.gpxcreator.gpxpanel.GPXFile;
import com.wattzap.model.dto.AxisPointsList;
import java.io.File;

import org.jfree.data.xy.XYSeries;

import com.wattzap.model.dto.Point;
import com.wattzap.model.dto.TargetData;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.fortius.DataVR;
import com.wattzap.model.fortius.ImfFile;
import com.wattzap.model.power.Power;
import java.io.IOException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/*
 * Wrapper class for IMF virtual data.
 *
 * @author Jarek
 */
@RouteAnnotation
public class IMFReader extends RouteReader {
    private final Logger logger = LogManager.getLogger("imf");

    private double totalWeight = 85.0;
    private Power power = null;
    private boolean metric = true;
    private boolean slope = true;

    private String routeName;

	private AxisPointsList<Point> points = null;
	private AxisPointsList<TargetData> targets = null;

    @Override
	public String getExtension() {
		return "imf";
	}

	@Override
	public String getName() {
		return routeName;
	}

    /**
	 * Load GPX data from file
	 *
	 * @param file file to load
	 */
    @Override
	public String load(File file) {
        ImfFile imfFile;
        try {
            imfFile = new ImfFile(file.getCanonicalPath());
        } catch (IOException ex) {
            return "Cannot read file, exception" + ex;
        }
        if (imfFile == null) {
            return "Cannot read file";
        }
        routeName = file.getName().substring(0, file.getName().length() - 4);

        points = new AxisPointsList<Point>();
        targets = new AxisPointsList<TargetData>();

        double distance = 0.0;
        double time = 0;
        for (DataVR vr : imfFile.getRideData()) {
            Point p = new Point(vr.dist());
            p.setLatitude(vr.x() / 1800.0);
            p.setLongitude(vr.z() / 1800.0);
            p.setElevation(vr.y());
            p.setGradient(vr.slope());
            p.setTime((long) (1000.0 * time));
            p.setSpeed(vr.speed() / 3.6);
            points.add(p);
            TargetData target = new TargetData(vr.dist());
            target.setCadence((int) vr.cadence());
            target.setPower((int) vr.power());
            target.setHr((int) vr.hr());
            targets.add(target);
            distance = vr.dist();
            time += vr.dt();
        }
        logger.debug("Distance " + distance + ", time " + time);

        if (points.size() < 2) {
            return "No track";
        }
        String ret = points.checkData();
        routeLen = distance;
        return ret;
	}

    @Override
	public void close() {
        points = null;
        targets = null;
        super.close();
	}

    public GPXFile createGpx() {
        return ReaderUtil.createGpx(getName(), points);
    }

    @Override
    public XYSeries createProfile() {
        // profile depends on settings: metric or imperial
        if (slope) {
            return ReaderUtil.createSlopeProfile(points, metric, routeLen);
        } else {
            return ReaderUtil.createAltitudeProfile(points, metric, routeLen);
        }
    }


    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            case PAUSE: // pause when end of route
            case SPEED: // compute speed from power

            case ROUTE_SPEED:
            case ROUTE_TIME:
            case ALTITUDE:
            case SLOPE:
            case LATITUDE:
            case LONGITUDE:

            case TARGET_CADENCE:
            case TARGET_HR:
            case TARGET_POWER:
                return true;

            default:
                return false;
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        TargetData d = targets.get(1000.0 * t.getDistance());
        setValue(SourceDataEnum.TARGET_CADENCE, d.getCadence());
        setValue(SourceDataEnum.TARGET_HR, d.getHr());
        setValue(SourceDataEnum.TARGET_POWER, d.getPower());

        Point p = points.get(1000.0 * t.getDistance());
        if (p != null) {
            double realSpeed = 3.6 * power.getRealSpeed(totalWeight,
                p.getGradient() / 100.0, t.getPower());
            setValue(SourceDataEnum.SPEED, realSpeed);

            // interpolate time on distance, the most important interpolation
            // other don't matter, are just for display purposes.
            // If time is not correctly interpolated, then video (speed and
            // position) are incorrectly computed and strange video effects
            // happens
            double time = p.getTime();
            Point pp = points.getNext();
            if (pp != null) {
                time = points.interpolate(1000.0 * t.getDistance(), time, pp.getTime());
            }
            setValue(SourceDataEnum.ROUTE_TIME, time);

            setValue(SourceDataEnum.ROUTE_SPEED, p.getSpeed());
            setValue(SourceDataEnum.ALTITUDE, p.getElevation());
            setValue(SourceDataEnum.SLOPE, p.getGradient());
            setValue(SourceDataEnum.LATITUDE, p.getLatitude());
            setValue(SourceDataEnum.LONGITUDE, p.getLongitude());
        }

        // set pause at end of route or when no running, otherwise unpause
        if (p == null) {
            setPause(PauseMsgEnum.END_OF_ROUTE);
            setValue(SourceDataEnum.SPEED, 0.0);
        } else if (getValue(SourceDataEnum.SPEED) < 0.01) {
            if (t.getTime() < 1000) {
                setPause(PauseMsgEnum.START);
            } else {
                setPause(PauseMsgEnum.NO_MOVEMENT);
            }
        } else {
            setPause(PauseMsgEnum.RUNNING);
        }
    }

    @Override
    public void configChanged(UserPreferences pref) {
        if ((pref == UserPreferences.INSTANCE) || (pref == UserPreferences.TURBO_TRAINER)) {
            power = pref.getTurboTrainerProfile();
        }
        // it can be updated every configChanged without checking the property..
        totalWeight = pref.getTotalWeight();

        if ((pref == UserPreferences.INSTANCE) ||
            (pref == UserPreferences.METRIC) ||
            (pref == UserPreferences.SHOW_SLOPE))
        {
            metric = pref.isMetric();
            slope = pref.slopeShown();
            // rebuild Profile panel
            rebuildProfile();
        }
    }
}
