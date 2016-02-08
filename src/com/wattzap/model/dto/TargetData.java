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
package com.wattzap.model.dto;

/**
 *
 * Represents a target data from .imf file
 *
 * @author Jarek
 */
public class TargetData extends AxisPoint
{
	private int power = 0;
	private int hr = 0;
	private int cadence = 0;

    public TargetData(double distanceFromStart) {
        super(distanceFromStart);
    }

	public int getPower() {
		return power;
	}

	public void setPower(int power) {
		this.power = power;
	}

	public int getCadence() {
		return cadence;
	}

	public void setCadence(int cadence) {
		this.cadence = cadence;
	}

	public int getHr() {
		return hr;
	}

	public void setHr(int hr) {
		this.hr = hr;
	}

	@Override
	public String toString() {
		return "Point [distance=" + getDistance() +
                " power=" + getPower() + " cadence=" + getCadence() +
                " hr=" + getHr() + "]";
	}
}
