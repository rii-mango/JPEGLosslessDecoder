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


public class ScanHeader {

	private int ah;
	private int al;
	private int numComp; // Number of components in the scan
	private int selection; // Start of spectral or predictor selection
	private int spectralEnd; // End of spectral selection

	protected ScanComponent components[];



	public int getAh() {
		return ah;
	}



	public int getAl() {
		return al;
	}



	public int getNumComponents() {
		return numComp;
	}



	public int getSelection() {
		return selection;
	}



	public int getSpectralEnd() {
		return spectralEnd;
	}



	public void setAh(final int ah) {
		this.ah = ah;
	}



	public void setAl(final int al) {
		this.al = al;
	}



	public void setSelection(final int selection) {
		this.selection = selection;
	}



	public void setSpectralEnd(final int spectralEnd) {
		this.spectralEnd = spectralEnd;
	}



	protected int read(final DataStream data) throws IOException {
		int count = 0;
		final int length = data.get16();
		count += 2;

		numComp = data.get8();
		count++;

		components = new ScanComponent[numComp];

		for (int i = 0; i < numComp; i++) {
			components[i] = new ScanComponent();

			if (count > length) {
				throw new IOException("ERROR: scan header format error");
			}

			components[i].setScanCompSel(data.get8());
			count++;

			final int temp = data.get8();
			count++;

			components[i].setDcTabSel(temp >> 4);
			components[i].setAcTabSel(temp & 0x0F);
		}

		setSelection(data.get8());
		count++;

		setSpectralEnd(data.get8());
		count++;

		final int temp = data.get8();
		setAh(temp >> 4);
		setAl(temp & 0x0F);
		count++;

		if (count != length) {
			throw new IOException("ERROR: scan header format error [count!=Ns]");
		}

		return 1;
	}
}
