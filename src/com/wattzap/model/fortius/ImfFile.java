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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ja
 */
public class ImfFile {
    private Header header = null;

    List<DataVR> rideData;

    public ImfFile(String fileName) {
        rideData = new ArrayList<DataVR>();
        read(new TacxStream(fileName, true));
    }

    public final void read(TacxStream is) {
        String t = "     \t";
        header = new Header(is);
        if (header.getFileFingerprint() != Header.IMF_FINGERPRINT) {
            throw new Error("Wrong Tacx file type " + header.getFileFingerprint());
        }
        for (int i = 0; i < header.getBlockCount(); i++) {
            InfoBlock infoBlock = new InfoBlock(is);

            long before = is.getFilePos();
            switch (infoBlock.getBlockFingerprint()) {
                case InfoBlock.VR_GENERAL_INFO: // 4010-General information
                    is.skipBlocks(infoBlock);
                    break;
                case InfoBlock.VR_COURSE_DATA: // 4020-Course data dane przejazdu pilota
                    is.skipBlocks(infoBlock);
                    break;
                case InfoBlock.VR_RIDE_INFO: // 4030-Ride information (pilotowy rider informacje
                    is.skipBlocks(infoBlock);
                    break;
                case InfoBlock.RIDER_INFO: // 210-Rider information informacje wła¶ciwy rider
                    is.skipBlocks(infoBlock);
                    break;
                case InfoBlock.NOTES: // 120-Notes
                    is.skipBlocks(infoBlock);
                    break;
                case InfoBlock.LAP_DATA: // 110-Lap data
                    is.skipBlocks(infoBlock);
                    break;
                case InfoBlock.VR_RIDE_DATA: // 4040-Ride data wła¶ciwe dane przejazdu
                    System.out.println("Found " + infoBlock.getRecordCount() + "entries");
                    System.out.println(
                            "n\tdist" + t + "dt  " + t + "speed" + t + "power" + t +
                                    "cadence" + t + "hr" + t + "slope");
                    DataVR d = null;
                    for (int j = 0; j < infoBlock.getRecordCount(); j++) {
                        d = new DataVR(is, d);
                        if (d.dt() > 0.00001) {

                            System.out.println(j + "\t" + d.dist() + t + d.dt() + t + d.speed() + t + d.power() + t +
                                    d.cadence() + t + d.hr() + t + d.slope());

                            rideData.add(d);
                        }
                    }
                    break;
                default:
                    throw new Error(infoBlock + ":: not expected here");
            }
            long read = is.getFilePos() - before;
            if (read != infoBlock.getRecordCount() * infoBlock.getRecordSize()) {
                throw new Error(infoBlock + ":: read " + read + ", while should "
                        + (infoBlock.getRecordCount() * infoBlock.getRecordSize()));
            }
        }
        try {
            is.readByte();
            throw new Error("File contains more data");
        } catch (Error e) {
            // silence.. file is ok
        }
    }

    public List<DataVR> getRideData() {
        return rideData;
    }

    /*
    public static void main(String[] args) {
        ImfFile f = new ImfFile("C:\\Users\\ja\\Downloads\\PLV-14.etap.1 Krzysztof Bejner 8-11-2015.imf");
    }
    */
}
