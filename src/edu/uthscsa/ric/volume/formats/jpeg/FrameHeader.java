/*
 * Copyright (C) 2015 Michael Martinez
 * Changes: Added support for selection values 2-7, fixed minor bugs &
 * warnings, split into multiple class files, and general clean up.
 */

/*
 * Copyright (C) 2003-2009 JNode.org
 * Original source: http://webuser.fh-furtwangen.de/~dersch/
 * Changed License to LGPL with the friendly permission of Helmut Dersch.
 */

/*
 * Copyright (C) Helmut Dersch
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package edu.uthscsa.ric.volume.formats.jpeg;

import java.io.IOException;


public class FrameHeader {

	private ComponentSpec components[]; // Components 
	private int dimX; // Number of samples per line
	private int dimY; // Number of lines
	private int numComp; // Number of component in the frame
	private int precision; // Sample Precision (from the original image)



	public ComponentSpec[] getComponents() {
		return components.clone();
	}



	public int getDimX() {
		return dimX;
	}



	public int getDimY() {
		return dimY;
	}



	public int getNumComponents() {
		return numComp;
	}



	public int getPrecision() {
		return precision;
	}



	protected int read(final DataStream data) throws IOException {
		int count = 0;

		final int length = data.get16();
		count += 2;

		precision = data.get8();
		count++;

		dimY = data.get16();
		count += 2;

		dimX = data.get16();
		count += 2;

		numComp = data.get8();
		count++;

		//components = new ComponentSpec[numComp]; // some image exceed this range...
		components = new ComponentSpec[256]; // setting to 256 -- not sure what it should be.

		for (int i = 1; i <= numComp; i++) {
			if (count > length) {
				throw new IOException("ERROR: frame format error");
			}

			final int c = data.get8();
			count++;

			if (count >= length) {
				throw new IOException("ERROR: frame format error [c>=Lf]");
			}

			final int temp = data.get8();
			count++;

			if (components[c] == null) {
				components[c] = new ComponentSpec();
			}

			components[c].hSamp = temp >> 4;
			components[c].vSamp = temp & 0x0F;
			components[c].quantTableSel = data.get8();
			count++;
		}

		if (count != length) {
			throw new IOException("ERROR: frame format error [Lf!=count]");
		}

		return 1;
	}
}
