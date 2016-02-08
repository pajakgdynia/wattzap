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
package com.wattzap.model.fortius;

/**
 *
 * @author ja
 */
public class DataVR {
    public static final int RECORD_SIZE = 38;

    /*
    typedef struct{
        FLOAT X; //x co-ordinate on 3D map
        FLOAT Y; //y co-ordinate on 3D map (and height in metres)
        FLOAT Z; //z co-ordinate on 3D map
        FLOAT Alpha;//Horizontal alignment of rider at each coordinate
        FLOAT Beta; //Declination of rider at each coordinate
        FLOAT Gamma;
        BYTE HeartRate;
        BYTE Cadence;
        INT16 PowerX10;
        INT16 SpeedX10;
        FLOAT TerrainAngle;
        FLOAT ForkAngle; // < -100 = crash
    } COURSE_DATA_VR;
    */
    private final float x;
    private final float y;
    private final float z;
    private final float alpha;
    private final float beta;
    private final float gamma;
    private final int heartRate;
    private final int cadence;
    private final float power;
    private final float speed;
    private final float terrainAngle;
    private final float forkAngle;

    private final float dist;
    private final float slope;
    private final float dt;

    public DataVR(TacxStream is, DataVR p) {
        x = is.readFloat();
        y = is.readFloat();
        z = is.readFloat();
        alpha = is.readFloat();
        beta = is.readFloat();
        gamma = is.readFloat();
        heartRate = is.readByte();
        cadence = is.readByte();
        power = (float) (is.readShort() / 10.0);
        speed = (float) (is.readShort() / 10.0);
        terrainAngle = is.readFloat();
        forkAngle = is.readFloat();
        is.checkData(RECORD_SIZE, this);

        float dist;
        if (p != null) {
            dist = (float) Math.sqrt((x - p.x())*(x - p.x()) + (y - p.y())*(y - p.y()) + (z - p.z())*(z - p.z()));
        } else {
            dist = (float) 0.0;
        }
        if (dist > 0.01) {
            slope = (float) 100.0 * (y - p.y()) / dist;
        } else if (p != null) {
            slope = p.slope();
        } else {
            slope = (float) 0.0;
        }
        if (speed > 0.01) {
            dt = (float) dist / (speed / (float) 3.6);
        } else {
            dt = (float) 0.0;
        }
        if (p != null) {
            dist += p.dist();
        }
        this.dist = dist;
    }

    public float x() {
        return x;
    }
    public float y() {
        return y;
    }
    public float z() {
        return z;
    }
    public float speed() {
        return speed;
    }
    public float dist() {
        return dist;
    }
    public float slope() {
        return slope;
    }
    public float dt() {
        return dt;
    }
    public float hr() {
        return heartRate;
    }
    public float power() {
        return power;
    }
    public float cadence() {
        return cadence;
    }
}
