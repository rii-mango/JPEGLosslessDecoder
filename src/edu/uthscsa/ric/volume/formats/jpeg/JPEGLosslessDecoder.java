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
import java.nio.ByteBuffer;


public class JPEGLosslessDecoder implements DataStream {

	private final ByteBuffer buffer;
	private final FrameHeader frame;
	private final HuffmanTable huffTable;
	private final QuantizationTable quantTable;
	private final ScanHeader scan;
	private final int DU[][][] = new int[10][4][64]; // at most 10 data units in a MCU, at most 4 data units in one component
	private final int HuffTab[][][] = new int[4][2][MAX_HUFFMAN_SUBTREE * 256];
	private final int IDCT_Source[] = new int[64];
	private final int nBlock[] = new int[10]; // number of blocks in the i-th Comp in a scan
	private final int[] acTab[] = new int[10][]; // ac HuffTab for the i-th Comp in a scan
	private final int[] dcTab[] = new int[10][]; // dc HuffTab for the i-th Comp in a scan
	private final int[] qTab[] = new int[10][]; // quantization table for the i-th Comp in a scan

	private int dataBufferIndex;
	private int marker;
	private int markerIndex;
	private int numComp;
	private int restartInterval;
	private int selection;
	private int xDim, yDim;
	private int xLoc;
	private int yLoc;
	private int[] outputData;

	private static final int IDCT_P[] = { 0, 5, 40, 16, 45, 2, 7, 42, 21, 56, 8, 61, 18, 47, 1, 4, 41, 23, 58, 13, 32, 24, 37, 10, 63, 17, 44, 3, 6, 43, 20,
		57, 15, 34, 29, 48, 53, 26, 39, 9, 60, 19, 46, 22, 59, 12, 33, 31, 50, 55, 25, 36, 11, 62, 14, 35, 28, 49, 52, 27, 38, 30, 51, 54 };
	private static final int TABLE[] = { 0, 1, 5, 6, 14, 15, 27, 28, 2, 4, 7, 13, 16, 26, 29, 42, 3, 8, 12, 17, 25, 30, 41, 43, 9, 11, 18, 24, 31, 40, 44, 53,
		10, 19, 23, 32, 39, 45, 52, 54, 20, 22, 33, 38, 46, 51, 55, 60, 21, 34, 37, 47, 50, 56, 59, 61, 35, 36, 48, 49, 57, 58, 62, 63 };

	public static final int MAX_HUFFMAN_SUBTREE = 50;
	public static final int MSB = 0x80000000;



	public JPEGLosslessDecoder(final byte[] data) {
		buffer = ByteBuffer.wrap(data);
		frame = new FrameHeader();
		scan = new ScanHeader();
		quantTable = new QuantizationTable();
		huffTable = new HuffmanTable();
	}



	public int[] decode() throws IOException {
		int current, scanNum = 0;
		final int pred[] = new int[10];
		int[] outputRef = null;

		xLoc = 0;
		yLoc = 0;
		current = get16();

		if (current != 0xFFD8) { // SOI
			throw new IOException("Not a JPEG file");
		}

		current = get16();

		while (((current >> 4) != 0x0FFC) || (current == 0xFFC4)) { // SOF 0~15
			switch (current) {
				case 0xFFC4: // DHT
					huffTable.read(this, HuffTab);
					break;
				case 0xFFCC: // DAC
					throw new IOException("Program doesn't support arithmetic coding. (format throw new IOException)");
				case 0xFFDB:
					quantTable.read(this, TABLE);
					break;
				case 0xFFDD:
					restartInterval = readNumber();
					break;
				case 0xFFE0:
				case 0xFFE1:
				case 0xFFE2:
				case 0xFFE3:
				case 0xFFE4:
				case 0xFFE5:
				case 0xFFE6:
				case 0xFFE7:
				case 0xFFE8:
				case 0xFFE9:
				case 0xFFEA:
				case 0xFFEB:
				case 0xFFEC:
				case 0xFFED:
				case 0xFFEE:
				case 0xFFEF:
					readApp();
					break;
				case 0xFFFE:
					readComment();
					break;
				default:
					if ((current >> 8) != 0xFF) {
						throw new IOException("ERROR: format throw new IOException! (decode)");
					}
			}

			current = get16();
		}

		if ((current < 0xFFC0) || (current > 0xFFC7)) {
			throw new IOException("ERROR: could not handle arithmetic code!");
		}

		frame.read(this);
		current = get16();

		do {
			while (current != 0x0FFDA) { //SOS
				switch (current) {
					case 0xFFC4: //DHT
						huffTable.read(this, HuffTab);
						break;
					case 0xFFCC: //DAC
						throw new IOException("Program doesn't support arithmetic coding. (format throw new IOException)");
					case 0xFFDB:
						quantTable.read(this, TABLE);
						break;
					case 0xFFDD:
						restartInterval = readNumber();
						break;
					case 0xFFE0:
					case 0xFFE1:
					case 0xFFE2:
					case 0xFFE3:
					case 0xFFE4:
					case 0xFFE5:
					case 0xFFE6:
					case 0xFFE7:
					case 0xFFE8:
					case 0xFFE9:
					case 0xFFEA:
					case 0xFFEB:
					case 0xFFEC:
					case 0xFFED:
					case 0xFFEE:
					case 0xFFEF:
						readApp();
						break;
					case 0xFFFE:
						readComment();
						break;
					default:
						if ((current >> 8) != 0xFF) {
							throw new IOException("ERROR: format throw new IOException! (Parser.decode)");
						}
				}

				current = get16();
			}

			final int precision = frame.getPrecision();
			final ComponentSpec[] components = frame.getComponents();

			scan.read(this);
			numComp = scan.getNumComponents();
			selection = scan.getSelection();

			final ScanComponent[] scanComps = scan.components;
			final int[][] quantTables = quantTable.quantTables;

			for (int i = 0; i < numComp; i++) {
				final int compN = scanComps[i].getScanCompSel();
				qTab[i] = quantTables[components[compN].quantTableSel];
				nBlock[i] = components[compN].vSamp * components[compN].hSamp;
				dcTab[i] = HuffTab[scanComps[i].getDcTabSel()][0];
				acTab[i] = HuffTab[scanComps[i].getAcTabSel()][1];
			}

			xDim = frame.getDimX();
			yDim = frame.getDimY();
			outputData = new int[xDim * yDim];
			outputRef = outputData;

			scanNum++;

			while (true) { // Decode one scan
				final int temp[] = new int[1]; // to store remainder bits
				final int index[] = new int[1];
				temp[0] = 0;
				index[0] = 0;

				for (int i = 0; i < 10; i++) {
					pred[i] = (1 << (precision - 1));
				}

				if (restartInterval == 0) {
					current = decode(pred, temp, index);

					while ((current == 0) && ((xLoc < xDim) && (yLoc < yDim))) {
						output(pred);
						current = decode(pred, temp, index);
					}

					break; //current=MARKER
				}

				for (int mcuNum = 0; mcuNum < restartInterval; mcuNum++) {
					current = decode(pred, temp, index);
					output(pred);

					if (current != 0) {
						break;
					}
				}

				if (current == 0) {
					if (markerIndex != 0) {
						current = (0xFF00 | marker);
						markerIndex = 0;
					} else {
						current = get16();
					}
				}

				if ((current >= 0xFFD0) && (current <= 0xFFD7)) {
					//empty
				} else {
					break; //current=MARKER
				}
			}

			if ((current == 0xFFDC) && (scanNum == 1)) { //DNL
				readNumber();
				current = get16();
			}
		} while ((current != 0xFFD9) && ((xLoc < xDim) && (yLoc < yDim)) && (scanNum == 0));

		return outputRef;
	}



	@Override
	public final int get16() {
		final int value = (buffer.getShort(dataBufferIndex) & 0xFFFF);
		dataBufferIndex += 2;
		return value;
	}



	@Override
	public final int get8() {
		return buffer.get(dataBufferIndex++) & 0xFF;
	}



	private int decode(final int prev[], final int temp[], final int index[]) throws IOException {
		switch (selection) {
			case 2:
				prev[0] = getPreviousY();
				break;
			case 3:
				prev[0] = getPreviousXY();
				break;
			case 4:
				prev[0] = (getPreviousX() + getPreviousY()) - getPreviousXY();
				break;
			case 5:
				prev[0] = getPreviousX() + ((getPreviousY() - getPreviousXY()) >> 1);
				break;
			case 6:
				prev[0] = getPreviousY() + ((getPreviousX() - getPreviousXY()) >> 1);
				break;
			case 7:
				prev[0] = (int) (((long) getPreviousX() + getPreviousY()) / 2);
				break;
			default:
				prev[0] = getPreviousX();
				break;
		}

		if (numComp > 1) {
			int value, actab[], dctab[];
			int qtab[];

			for (int ctrC = 0; ctrC < numComp; ctrC++) {
				qtab = qTab[ctrC];
				actab = acTab[ctrC];
				dctab = dcTab[ctrC];
				for (int i = 0; i < nBlock[ctrC]; i++) {
					for (int k = 0; k < IDCT_Source.length; k++) {
						IDCT_Source[k] = 0;
					}

					value = getHuffmanValue(dctab, temp, index);

					if (value >= 0xFF00) {
						return value;
					}

					prev[ctrC] = IDCT_Source[0] = prev[ctrC] + getn(index, value, temp, index);
					IDCT_Source[0] *= qtab[0];

					for (int j = 1; j < 64; j++) {
						value = getHuffmanValue(actab, temp, index);

						if (value >= 0xFF00) {
							return value;
						}

						j += (value >> 4);

						if ((value & 0x0F) == 0) {
							if ((value >> 4) == 0) {
								break;
							}
						} else {
							IDCT_Source[IDCT_P[j]] = getn(index, value & 0x0F, temp, index) * qtab[j];
						}
					}

					scaleIDCT(DU[ctrC][i]);
				}
			}

			return 0;
		} else {
			for (int i = 0; i < nBlock[0]; i++) {
				final int value = getHuffmanValue(dcTab[0], temp, index);
				if (value >= 0xFF00) {
					return value;
				}

				prev[0] += getn(prev, value, temp, index);
			}

			return 0;
		}
	}



	//	Huffman table for fast search: (HuffTab) 8-bit Look up table 2-layer search architecture, 1st-layer represent 256 node (8 bits) if codeword-length > 8
	//	bits, then the entry of 1st-layer = (# of 2nd-layer table) | MSB and it is stored in the 2nd-layer Size of tables in each layer are 256.
	//	HuffTab[*][*][0-256] is always the only 1st-layer table.
	//	 
	//	An entry can be: (1) (# of 2nd-layer table) | MSB , for code length > 8 in 1st-layer (2) (Code length) << 8 | HuffVal
	//	 
	//	HuffmanValue(table   HuffTab[x][y] (ex) HuffmanValue(HuffTab[1][0],...)
	//	                ):
	//	    return: Huffman Value of table
	//	            0xFF?? if it receives a MARKER
	//	    Parameter:  table   HuffTab[x][y] (ex) HuffmanValue(HuffTab[1][0],...)
	//	                temp    temp storage for remainded bits
	//	                index   index to bit of temp
	//	                in      FILE pointer
	//	    Effect:
	//	        temp  store new remainded bits
	//	        index change to new index
	//	        in    change to new position
	//	    NOTE:
	//	      Initial by   temp=0; index=0;
	//	    NOTE: (explain temp and index)
	//	      temp: is always in the form at calling time or returning time
	//	       |  byte 4  |  byte 3  |  byte 2  |  byte 1  |
	//	       |     0    |     0    | 00000000 | 00000??? |  if not a MARKER
	//	                                               ^index=3 (from 0 to 15)
	//	                                               321
	//	    NOTE (marker and marker_index):
	//	      If get a MARKER from 'in', marker=the low-byte of the MARKER
	//	        and marker_index=9
	//	      If marker_index=9 then index is always > 8, or HuffmanValue()
	//	        will not be called
	private int getHuffmanValue(final int table[], final int temp[], final int index[]) throws IOException {
		int code, input;
		final int mask = 0xFFFF;

		if (index[0] < 8) {
			temp[0] <<= 8;
			input = get8();
			if (input == 0xFF) {
				marker = get8();
				if (marker != 0) {
					markerIndex = 9;
				}
			}
			temp[0] |= input;
		} else {
			index[0] -= 8;
		}

		code = table[temp[0] >> index[0]];

		if ((code & MSB) != 0) {
			if (markerIndex != 0) {
				markerIndex = 0;
				return 0xFF00 | marker;
			}

			temp[0] &= (mask >> (16 - index[0]));
			temp[0] <<= 8;
			input = get8();

			if (input == 0xFF) {
				marker = get8();
				if (marker != 0) {
					markerIndex = 9;
				}
			}

			temp[0] |= input;
			code = table[((code & 0xFF) * 256) + (temp[0] >> index[0])];
			index[0] += 8;
		}

		index[0] += 8 - (code >> 8);

		if (index[0] < 0) {
			throw new IOException("index=" + index[0] + " temp=" + temp[0] + " code=" + code + " in HuffmanValue()");
		}

		if (index[0] < markerIndex) {
			markerIndex = 0;
			return 0xFF00 | marker;
		}

		temp[0] &= (mask >> (16 - index[0]));
		return code & 0xFF;
	}



	private int getn(final int[] PRED, final int n, final int temp[], final int index[]) throws IOException {
		int result;
		final int one = 1;
		final int n_one = -1;
		final int mask = 0xFFFF;
		int input;

		if (n == 0) {
			return 0;
		}

		if (n == 16) {
			if (PRED[0] >= 0) {
				return -32768;
			} else {
				return 32768;
			}
		}

		index[0] -= n;

		if (index[0] >= 0) {
			if ((index[0] < markerIndex) && !isLastPixel()) { // this was corrupting the last pixel in some cases
				markerIndex = 0;
				return (0xFF00 | marker) << 8;
			}

			result = temp[0] >> index[0];
				temp[0] &= (mask >> (16 - index[0]));
		} else {
			temp[0] <<= 8;
			input = get8();

			if (input == 0xFF) {
				marker = get8();
				if (marker != 0) {
					markerIndex = 9;
				}
			}

			temp[0] |= input;
			index[0] += 8;

			if (index[0] < 0) {
				if (markerIndex != 0) {
					markerIndex = 0;
					return (0xFF00 | marker) << 8;
				}

				temp[0] <<= 8;
				input = get8();

				if (input == 0xFF) {
					marker = get8();
					if (marker != 0) {
						markerIndex = 9;
					}
				}

				temp[0] |= input;
				index[0] += 8;
			}

			if (index[0] < 0) {
				throw new IOException("index=" + index[0] + " in getn()");
			}

			if (index[0] < markerIndex) {
				markerIndex = 0;
				return (0xFF00 | marker) << 8;
			}

			result = temp[0] >> index[0];
				temp[0] &= (mask >> (16 - index[0]));
		}

		if (result < (one << (n - 1))) {
			result += (n_one << n) + 1;
		}

		return result;
	}



	private int getPreviousX() {
		if (xLoc > 0) {
			return outputData[((yLoc * xDim) + xLoc) - 1];
		} else if (yLoc > 0) {
			return getPreviousY();
		} else {
			return (1 << (frame.getPrecision() - 1));
		}
	}



	private int getPreviousXY() {
		if ((xLoc > 0) && (yLoc > 0)) {
			return outputData[(((yLoc - 1) * xDim) + xLoc) - 1];
		} else {
			return getPreviousY();
		}
	}



	private int getPreviousY() {
		if (yLoc > 0) {
			return outputData[((yLoc - 1) * xDim) + xLoc];
		} else {
			return getPreviousX();
		}
	}



	private boolean isLastPixel() {
		return (xLoc == (xDim - 1)) && (yLoc == (yDim - 1));
	}



	private void output(final int PRED[]) {
		if ((xLoc < xDim) && (yLoc < yDim)) {
			outputData[(yLoc * xDim) + xLoc] = PRED[0];

			xLoc++;

			if (xLoc >= xDim) {
				yLoc++;
				xLoc = 0;
			}
		}
	}



	private int readApp() throws IOException {
		int count = 0;
		final int length = get16();
		count += 2;

		while (count < length) {
			get8();
			count++;
		}

		return length;
	}



	private String readComment() throws IOException {
		final StringBuffer sb = new StringBuffer();
		int count = 0;

		final int length = get16();
		count += 2;

		while (count < length) {
			sb.append((char) get8());
			count++;
		}

		return sb.toString();
	}



	private int readNumber() throws IOException {
		final int Ld = get16();

		if (Ld != 4) {
			throw new IOException("ERROR: Define number format throw new IOException [Ld!=4]");
		}

		return get16();
	}



	private void scaleIDCT(final int matrix[]) {
		final int p[][] = new int[8][8];
		int t0, t1, t2, t3, i;
		int src0, src1, src2, src3, src4, src5, src6, src7;
		int det0, det1, det2, det3, det4, det5, det6, det7;
		int mindex = 0;

		for (i = 0; i < 8; i++) {
			src0 = IDCT_Source[(0 * 8) + i];
			src1 = IDCT_Source[(1 * 8) + i];
			src2 = IDCT_Source[(2 * 8) + i] - IDCT_Source[(3 * 8) + i];
			src3 = IDCT_Source[(3 * 8) + i] + IDCT_Source[(2 * 8) + i];
			src4 = IDCT_Source[(4 * 8) + i] - IDCT_Source[(7 * 8) + i];
			src6 = IDCT_Source[(5 * 8) + i] - IDCT_Source[(6 * 8) + i];
			t0 = IDCT_Source[(5 * 8) + i] + IDCT_Source[(6 * 8) + i];
			t1 = IDCT_Source[(4 * 8) + i] + IDCT_Source[(7 * 8) + i];
			src5 = t0 - t1;
			src7 = t0 + t1;

			det4 = (-src4 * 480) - (src6 * 192);
			det5 = src5 * 384;
			det6 = (src6 * 480) - (src4 * 192);
			det7 = src7 * 256;
			t0 = src0 * 256;
			t1 = src1 * 256;
			t2 = src2 * 384;
			t3 = src3 * 256;
			det3 = t3;
			det0 = t0 + t1;
			det1 = t0 - t1;
			det2 = t2 - t3;

			src0 = det0 + det3;
			src1 = det1 + det2;
			src2 = det1 - det2;
			src3 = det0 - det3;
			src4 = det6 - det4 - det5 - det7;
			src5 = (det5 - det6) + det7;
			src6 = det6 - det7;
			src7 = det7;

			p[0][i] = (src0 + src7 + (1 << 12)) >> 13;
			p[1][i] = (src1 + src6 + (1 << 12)) >> 13;
			p[2][i] = (src2 + src5 + (1 << 12)) >> 13;
			p[3][i] = (src3 + src4 + (1 << 12)) >> 13;
			p[4][i] = ((src3 - src4) + (1 << 12)) >> 13;
			p[5][i] = ((src2 - src5) + (1 << 12)) >> 13;
			p[6][i] = ((src1 - src6) + (1 << 12)) >> 13;
			p[7][i] = ((src0 - src7) + (1 << 12)) >> 13;
		}

		for (i = 0; i < 8; i++) {
			src0 = p[i][0];
			src1 = p[i][1];
			src2 = p[i][2] - p[i][3];
			src3 = p[i][3] + p[i][2];
			src4 = p[i][4] - p[i][7];
			src6 = p[i][5] - p[i][6];
			t0 = p[i][5] + p[i][6];
			t1 = p[i][4] + p[i][7];
			src5 = t0 - t1;
			src7 = t0 + t1;

			det4 = (-src4 * 480) - (src6 * 192);
			det5 = src5 * 384;
			det6 = (src6 * 480) - (src4 * 192);
			det7 = src7 * 256;
			t0 = src0 * 256;
			t1 = src1 * 256;
			t2 = src2 * 384;
			t3 = src3 * 256;
			det3 = t3;
			det0 = t0 + t1;
			det1 = t0 - t1;
			det2 = t2 - t3;

			src0 = det0 + det3;
			src1 = det1 + det2;
			src2 = det1 - det2;
			src3 = det0 - det3;
			src4 = det6 - det4 - det5 - det7;
			src5 = (det5 - det6) + det7;
			src6 = det6 - det7;
			src7 = det7;

			matrix[mindex++] = (src0 + src7 + (1 << 12)) >> 13;
			matrix[mindex++] = (src1 + src6 + (1 << 12)) >> 13;
			matrix[mindex++] = (src2 + src5 + (1 << 12)) >> 13;
			matrix[mindex++] = (src3 + src4 + (1 << 12)) >> 13;
			matrix[mindex++] = ((src3 - src4) + (1 << 12)) >> 13;
			matrix[mindex++] = ((src2 - src5) + (1 << 12)) >> 13;
			matrix[mindex++] = ((src1 - src6) + (1 << 12)) >> 13;
			matrix[mindex++] = ((src0 - src7) + (1 << 12)) >> 13;
		}
	}
}
