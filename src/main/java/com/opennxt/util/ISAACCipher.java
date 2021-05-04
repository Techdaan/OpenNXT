package com.opennxt.util;

/**
 * @author Graham Edgecombe
 */
public class ISAACCipher {
	/**
	 * The golden ratio.
	 */
	public static final int RATIO = 0x9e3779b9;
	/**
	 * The log of the size of the results and memory arrays.
	 */
	public static final int SIZE_LOG = 8;
	/**
	 * The size of the results and memory arrays.
	 */
	public static final int SIZE = 1 << SIZE_LOG;
	/**
	 * For pseudorandom lookup.
	 */
	public static final int MASK = (SIZE - 1) << 2;
	/**
	 * The count through the results.
	 */
	@SuppressWarnings("unused")
	private int count = 0;
	/**
	 * The results.
	 */
	private int results[] = new int[SIZE];
	/**
	 * The internal memory state.
	 */
	private int memory[] = new int[SIZE];
	/**
	 * The accumulator.
	 */
	private int a;
	/**
	 * The last result.
	 */
	private int b;
	/**
	 * The counter.
	 */
	private int c;

	/**
	 * Creates the ISAAC cipher. * @param seed The generator seed.
	 */
	public ISAACCipher(int[] seed) {
		for (int i = 0; i < seed.length; i++) {
			results[i] = seed[i];
		}
		init(true);
	}

	/**
	 * Gets the next value. * @return The next value.
	 */
	public int getNextValue() {
		if (count-- == 0) {
			isaac();
			count = SIZE - 1;
		}
		return results[count];
	}

	public int getCurrentValue() {
		if (count == 0) {
			ISAACCipher two = new ISAACCipher(new int[]{0, 0, 0,0 });
			two.count = count;
			System.arraycopy(results, 0, two.results, 0, SIZE);
			System.arraycopy(memory, 0, two.memory, 0, SIZE);
			two.a = a;
			two.b = b;
			two.c = c;
			return two.getNextValue();
		}

		return results[count - 1];
	}

	/**
	 * Generates 256 results.
	 */
	public void isaac() {
		int i, j, x, y;
		b += ++c;
		for (i = 0, j = SIZE / 2; i < SIZE / 2; ) {
			x = memory[i];
			a ^= a << 13;
			a += memory[j++];
			memory[i] = y = memory[(x & MASK) >> 2] + a + b;
			results[i++] = b = memory[((y >> SIZE_LOG) & MASK) >> 2] + x;
			x = memory[i];
			a ^= a >>> 6;
			a += memory[j++];
			memory[i] = y = memory[(x & MASK) >> 2] + a + b;
			results[i++] = b = memory[((y >> SIZE_LOG) & MASK) >> 2] + x;
			x = memory[i];
			a ^= a << 2;
			a += memory[j++];
			memory[i] = y = memory[(x & MASK) >> 2] + a + b;
			results[i++] = b = memory[((y >> SIZE_LOG) & MASK) >> 2] + x;
			x = memory[i];
			a ^= a >>> 16;
			a += memory[j++];
			memory[i] = y = memory[(x & MASK) >> 2] + a + b;
			results[i++] = b = memory[((y >> SIZE_LOG) & MASK) >> 2] + x;
		}
		for (j = 0; j < SIZE / 2; ) {
			x = memory[i];
			a ^= a << 13;
			a += memory[j++];
			memory[i] = y = memory[(x & MASK) >> 2] + a + b;
			results[i++] = b = memory[((y >> SIZE_LOG) & MASK) >> 2] + x;
			x = memory[i];
			a ^= a >>> 6;
			a += memory[j++];
			memory[i] = y = memory[(x & MASK) >> 2] + a + b;
			results[i++] = b = memory[((y >> SIZE_LOG) & MASK) >> 2] + x;
			x = memory[i];
			a ^= a << 2;
			a += memory[j++];
			memory[i] = y = memory[(x & MASK) >> 2] + a + b;
			results[i++] = b = memory[((y >> SIZE_LOG) & MASK) >> 2] + x;
			x = memory[i];
			a ^= a >>> 16;
			a += memory[j++];
			memory[i] = y = memory[(x & MASK) >> 2] + a + b;
			results[i++] = b = memory[((y >> SIZE_LOG) & MASK) >> 2] + x;
		}
	}

	/**
	 * * Initializes the ISAAC. * * @param flag Indicating if we should perform a second pass.
	 */
	public void init(boolean flag) {
		int i;
		int a, b, c, d, e, f, g, h;
		a = b = c = d = e = f = g = h = RATIO;
		for (i = 0; i < 4; ++i) {
			a ^= b << 11;
			d += a;
			b += c;
			b ^= c >>> 2;
			e += b;
			c += d;
			c ^= d << 8;
			f += c;
			d += e;
			d ^= e >>> 16;
			g += d;
			e += f;
			e ^= f << 10;
			h += e;
			f += g;
			f ^= g >>> 4;
			a += f;
			g += h;
			g ^= h << 8;
			b += g;
			h += a;
			h ^= a >>> 9;
			c += h;
			a += b;
		}
		for (i = 0; i < SIZE; i += 8) {
			if (flag) {
				a += results[i];
				b += results[i + 1];
				c += results[i + 2];
				d += results[i + 3];
				e += results[i + 4];
				f += results[i + 5];
				g += results[i + 6];
				h += results[i + 7];
			}
			a ^= b << 11;
			d += a;
			b += c;
			b ^= c >>> 2;
			e += b;
			c += d;
			c ^= d << 8;
			f += c;
			d += e;
			d ^= e >>> 16;
			g += d;
			e += f;
			e ^= f << 10;
			h += e;
			f += g;
			f ^= g >>> 4;
			a += f;
			g += h;
			g ^= h << 8;
			b += g;
			h += a;
			h ^= a >>> 9;
			c += h;
			a += b;
			memory[i] = a;
			memory[i + 1] = b;
			memory[i + 2] = c;
			memory[i + 3] = d;
			memory[i + 4] = e;
			memory[i + 5] = f;
			memory[i + 6] = g;
			memory[i + 7] = h;
		}
		if (flag) {
			for (i = 0; i < SIZE; i += 8) {
				a += memory[i];
				b += memory[i + 1];
				c += memory[i + 2];
				d += memory[i + 3];
				e += memory[i + 4];
				f += memory[i + 5];
				g += memory[i + 6];
				h += memory[i + 7];
				a ^= b << 11;
				d += a;
				b += c;
				b ^= c >>> 2;
				e += b;
				c += d;
				c ^= d << 8;
				f += c;
				d += e;
				d ^= e >>> 16;
				g += d;
				e += f;
				e ^= f << 10;
				h += e;
				f += g;
				f ^= g >>> 4;
				a += f;
				g += h;
				g ^= h << 8;
				b += g;
				h += a;
				h ^= a >>> 9;
				c += h;
				a += b;
				memory[i] = a;
				memory[i + 1] = b;
				memory[i + 2] = c;
				memory[i + 3] = d;
				memory[i + 4] = e;
				memory[i + 5] = f;
				memory[i + 6] = g;
				memory[i + 7] = h;
			}
		}
		isaac();
		count = SIZE;
	}
}