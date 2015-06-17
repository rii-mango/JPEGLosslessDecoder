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


public class QuantizationTable {

	private final int precision[] = new int[4]; // Quantization precision 8 or 16
	private final int[] tq = new int[4]; // 1: this table is presented

	protected final int quantTables[][] = new int[4][64]; // Tables



	public QuantizationTable() {
		tq[0] = 0;
		tq[1] = 0;
		tq[2] = 0;
		tq[3] = 0;
	}



	protected int read(final DataStream data, final int[] table) throws IOException {
		int count = 0;
		final int length = data.get16();
		count += 2;

		while (count < length) {
			final int temp = data.get8();
			count++;
			final int t = temp & 0x0F;

			if (t > 3) {
				throw new IOException("ERROR: Quantization table ID > 3");
			}

			precision[t] = temp >> 4;

			if (precision[t] == 0) {
				precision[t] = 8;
			} else if (precision[t] == 1) {
				precision[t] = 16;
			} else {
				throw new IOException("ERROR: Quantization table precision error");
			}

			tq[t] = 1;

			if (precision[t] == 8) {
				for (int i = 0; i < 64; i++) {
					if (count > length) {
						throw new IOException("ERROR: Quantization table format error");
					}

					quantTables[t][i] = data.get8();
					count++;
				}

				enhanceQuantizationTable(quantTables[t], table);
			} else {
				for (int i = 0; i < 64; i++) {
					if (count > length) {
						throw new IOException("ERROR: Quantization table format error");
					}

					quantTables[t][i] = data.get16();
					count += 2;
				}

				enhanceQuantizationTable(quantTables[t], table);
			}
		}

		if (count != length) {
			throw new IOException("ERROR: Quantization table error [count!=Lq]");
		}

		return 1;
	}



	private void enhanceQuantizationTable(final int qtab[], final int[] table) {
		for (int i = 0; i < 8; i++) {
			qtab[table[(0 * 8) + i]] *= 90;
			qtab[table[(4 * 8) + i]] *= 90;
			qtab[table[(2 * 8) + i]] *= 118;
			qtab[table[(6 * 8) + i]] *= 49;
			qtab[table[(5 * 8) + i]] *= 71;
			qtab[table[(1 * 8) + i]] *= 126;
			qtab[table[(7 * 8) + i]] *= 25;
			qtab[table[(3 * 8) + i]] *= 106;
		}

		for (int i = 0; i < 8; i++) {
			qtab[table[0 + (8 * i)]] *= 90;
			qtab[table[4 + (8 * i)]] *= 90;
			qtab[table[2 + (8 * i)]] *= 118;
			qtab[table[6 + (8 * i)]] *= 49;
			qtab[table[5 + (8 * i)]] *= 71;
			qtab[table[1 + (8 * i)]] *= 126;
			qtab[table[7 + (8 * i)]] *= 25;
			qtab[table[3 + (8 * i)]] *= 106;
		}

		for (int i = 0; i < 64; i++) {
			qtab[i] >>= 6;
		}
	}
}
