/*
 * (c) the authors
 * Licensed under the Apache License, Version 2.0.
 */

package org.roaringbitmap;

/**
 * Various useful methods for roaring bitmaps.
 */
public final class Util {

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private Util() {
    }

    /**
     * Find the smallest integer larger than pos such that array[pos]&gt;= min.
     * If none can be found, return length. Based on code by O. Kaser.
     *
     * @param array
     * @param pos
     * @param min
     * @return x greater than pos such that array[pos] is at least as large
     * as min, pos is is equal to length if it is not possible.
     */
    private static int advanceUntil(short[] array, int pos, int length, short min) {
        int lower = pos + 1;

        // special handling for a possibly common sequential case
        if (lower >= length || toIntUnsigned(array[lower]) >= toIntUnsigned(min)) {
            return lower;
        }

        int spansize = 1; // could set larger
        // bootstrap an upper limit

        while (lower + spansize < length && toIntUnsigned(array[lower + spansize]) < toIntUnsigned(min))
            spansize *= 2; // hoping for compiler will reduce to
        // shift
        int upper = (lower + spansize < length) ? lower + spansize : length - 1;

        // maybe we are lucky (could be common case when the seek ahead
        // expected
        // to be small and sequential will otherwise make us look bad)
        if (array[upper] == min) {
            return upper;
        }

        if (toIntUnsigned(array[upper]) < toIntUnsigned(min)) {// means
            // array
            // has no
            // item
            // >= min
            // pos = array.length;
            return length;
        }

        // we know that the next-smallest span was too small
        lower += (spansize / 2);

        // else begin binary search
        // invariant: array[lower]<min && array[upper]>min
        while (lower + 1 != upper) {
            int mid = (lower + upper) / 2;
            if (array[mid] == min) {
                return mid;
            } else if (toIntUnsigned(array[mid]) < toIntUnsigned(min))
                lower = mid;
            else
                upper = mid;
        }
        return upper;

    }

    /**
     * Compute the bitwise AND between two long arrays and write
     * the set bits in the container.
     *
     * @param container where we write
     * @param bitmap1   first bitmap
     * @param bitmap2   second bitmap
     */
    public static void fillArrayAND(final short[] container, final long[] bitmap1,
                                    final long[] bitmap2) {
        int pos = 0;
        if (bitmap1.length != bitmap2.length)
            throw new IllegalArgumentException("not supported");
        for (int k = 0; k < bitmap1.length; ++k) {
            long bitset = bitmap1[k] & bitmap2[k];
            while (bitset != 0) {
                long t = bitset & -bitset;
                container[pos++] = (short) (k * 64 + Long.bitCount(t - 1));
                bitset ^= t;
            }
        }
    }

    /**
     * Compute the bitwise ANDNOT between two long arrays and write
     * the set bits in the container.
     *
     * @param container where we write
     * @param bitmap1   first bitmap
     * @param bitmap2   second bitmap
     */
    public static void fillArrayANDNOT(final short[] container,
                                       final long[] bitmap1, final long[] bitmap2) {
        int pos = 0;
        if (bitmap1.length != bitmap2.length)
            throw new IllegalArgumentException("not supported");
        for (int k = 0; k < bitmap1.length; ++k) {
            long bitset = bitmap1[k] & (~bitmap2[k]);
            while (bitset != 0) {
                long t = bitset & -bitset;
                container[pos++] = (short) (k * 64 + Long.bitCount(t - 1));
                bitset ^= t;
            }
        }
    }

    /**
     * Compute the bitwise XOR between two long arrays and write
     * the set bits in the container.
     *
     * @param container where we write
     * @param bitmap1   first bitmap
     * @param bitmap2   second bitmap
     */
    public static void fillArrayXOR(final short[] container, final long[] bitmap1,
                                    final long[] bitmap2) {
        int pos = 0;
        if (bitmap1.length != bitmap2.length)
            throw new IllegalArgumentException("not supported");
        for (int k = 0; k < bitmap1.length; ++k) {
            long bitset = bitmap1[k] ^ bitmap2[k];
            while (bitset != 0) {
                long t = bitset & -bitset;
                container[pos++] = (short) (k * 64 + Long.bitCount(t - 1));
                bitset ^= t;
            }
        }
    }

    protected static short highbits(int x) {
        return (short) (x >>> 16);
    }

    protected static short lowbits(int x) {
        return (short) (x & 0xFFFF);
    }

    protected static short maxLowBit() {
        return (short) 0xFFFF;
    }

    protected static int toIntUnsigned(short x) {
        return x & 0xFFFF;
    }

    protected static int unsignedBinarySearch(final short[] array, final int begin,
                                              final int end, final short k) {
        int ikey = toIntUnsigned(k);
        // next line accelerates the possibly common case where the value would be inserted at the end
        if((end>0) && (toIntUnsigned(array[end-1]) < ikey)) return - end - 1;
        int low = begin;
        int high = end - 1;
        while (low <= high) {
            final int middleIndex = (low + high) >>> 1;
            final int middleValue = toIntUnsigned(array[middleIndex]);

            if (middleValue < ikey)
                low = middleIndex + 1;
            else if (middleValue > ikey)
                high = middleIndex - 1;
            else
                return middleIndex;
        }
        return -(low + 1);
    }

    /**
     * Compute the difference between two sorted lists and write the result to the provided
     * output array
     *
     * @param set1    first array
     * @param length1 length of first array
     * @param set2    second array
     * @param length2 length of second array
     * @param buffer  output array
     * @return cardinality of the difference
     */
    public static int unsignedDifference(final short[] set1,
                                         final int length1, final short[] set2, final int length2,
                                         final short[] buffer) {
        int pos = 0;
        int k1 = 0, k2 = 0;
        if (0 == length2) {
            System.arraycopy(set1, 0, buffer, 0, length1);
            return length1;
        }
        if (0 == length1) {
            return 0;
        }
        while (true) {
            if (toIntUnsigned(set1[k1]) < toIntUnsigned(set2[k2])) {
                buffer[pos++] = set1[k1];
                ++k1;
                if (k1 >= length1) {
                    break;
                }
            } else if (toIntUnsigned(set1[k1]) == toIntUnsigned(set2[k2])) {
                ++k1;
                ++k2;
                if (k1 >= length1) {

                    break;
                }
                if (k2 >= length2) {
                    for (; k1 < length1; ++k1)
                        buffer[pos++] = set1[k1];
                    break;
                }
            } else {// if (val1>val2)
                ++k2;
                if (k2 >= length2) {
                    for (; k1 < length1; ++k1)
                        buffer[pos++] = set1[k1];
                    break;
                }
            }
        }
        return pos;
    }

    /**
     * Compute the exclusive union of two sorted lists and write the result to the provided
     * output array
     *
     * @param set1    first array
     * @param length1 length of first array
     * @param set2    second array
     * @param length2 length of second array
     * @param buffer  output array
     * @return cardinality of the exclusive union
     */
    public static int unsignedExclusiveUnion2by2(final short[] set1,
                                                 final int length1, final short[] set2, final int length2,
                                                 final short[] buffer) {
        int pos = 0;
        int k1 = 0, k2 = 0;
        if (0 == length2) {
            System.arraycopy(set1, 0, buffer, 0, length1);
            return length1;
        }
        if (0 == length1) {
            System.arraycopy(set2, 0, buffer, 0, length2);
            return length2;
        }
        while (true) {
            if (toIntUnsigned(set1[k1]) < toIntUnsigned(set2[k2])) {
                buffer[pos++] = set1[k1];
                ++k1;
                if (k1 >= length1) {
                    for (; k2 < length2; ++k2)
                        buffer[pos++] = set2[k2];
                    break;
                }
            } else if (toIntUnsigned(set1[k1]) == toIntUnsigned(set2[k2])) {
                ++k1;
                ++k2;
                if (k1 >= length1) {
                    for (; k2 < length2; ++k2)
                        buffer[pos++] = set2[k2];
                    break;
                }
                if (k2 >= length2) {
                    for (; k1 < length1; ++k1)
                        buffer[pos++] = set1[k1];
                    break;
                }
            } else {// if (val1>val2)
                buffer[pos++] = set2[k2];
                ++k2;
                if (k2 >= length2) {
                    for (; k1 < length1; ++k1)
                        buffer[pos++] = set1[k1];
                    break;
                }
            }
        }
        return pos;
    }

    /**
     * Intersect two sorted lists and write the result to the provided
     * output array
     *
     * @param set1    first array
     * @param length1 length of first array
     * @param set2    second array
     * @param length2 length of second array
     * @param buffer  output array
     * @return cardinality of the intersection
     */
    public static int unsignedIntersect2by2(final short[] set1,
                                            final int length1, final short[] set2, final int length2,
                                            final short[] buffer) {
        if (set1.length * 64 < set2.length) {
            return unsignedOneSidedGallopingIntersect2by2(set1, length1, set2, length2, buffer);
        } else if (set2.length * 64 < set1.length) {
            return unsignedOneSidedGallopingIntersect2by2(set2, length2, set1, length1, buffer);
        } else {
            return unsignedLocalIntersect2by2(set1, length1, set2, length2, buffer);
        }
    }

    protected static int unsignedLocalIntersect2by2(final short[] set1,
                                                    final int length1, final short[] set2, final int length2,
                                                    final short[] buffer) {
        if ((0 == length1) || (0 == length2))
            return 0;
        int k1 = 0;
        int k2 = 0;
        int pos = 0;

        mainwhile:
        while (true) {
            if (toIntUnsigned(set2[k2]) < toIntUnsigned(set1[k1])) {
                do {
                    ++k2;
                    if (k2 == length2)
                        break mainwhile;
                } while (toIntUnsigned(set2[k2]) < toIntUnsigned(set1[k1]));
            }
            if (toIntUnsigned(set1[k1]) < toIntUnsigned(set2[k2])) {
                do {
                    ++k1;
                    if (k1 == length1)
                        break mainwhile;
                } while (toIntUnsigned(set1[k1]) < toIntUnsigned(set2[k2]));
            } else {
                // (set2[k2] == set1[k1])
                buffer[pos++] = set1[k1];
                ++k1;
                if (k1 == length1)
                    break;
                ++k2;
                if (k2 == length2)
                    break;
            }
        }
        return pos;
    }

    protected static int unsignedOneSidedGallopingIntersect2by2(
            final short[] smallSet, final int smallLength,
            final short[] largeSet, final int largeLength,
            final short[] buffer) {
        if (0 == smallLength)
            return 0;
        int k1 = 0;
        int k2 = 0;
        int pos = 0;
        while (true) {
            if (toIntUnsigned(largeSet[k1]) < toIntUnsigned(smallSet[k2])) {
                k1 = advanceUntil(largeSet, k1, largeLength, smallSet[k2]);
                if (k1 == largeLength)
                    break;
            }
            if (toIntUnsigned(smallSet[k2]) < toIntUnsigned(largeSet[k1])) {
                ++k2;
                if (k2 == smallLength)
                    break;
            } else {
                // (set2[k2] == set1[k1])
                buffer[pos++] = smallSet[k2];
                ++k2;
                if (k2 == smallLength)
                    break;
                k1 = advanceUntil(largeSet, k1, largeLength, smallSet[k2]);
                if (k1 == largeLength)
                    break;
            }

        }
        return pos;

    }

    /**
     * Unite two sorted lists and write the result to the provided
     * output array
     *
     * @param set1    first array
     * @param length1 length of first array
     * @param set2    second array
     * @param length2 length of second array
     * @param buffer  output array
     * @return cardinality of the union
     */
    public static int unsignedUnion2by2(final short[] set1,
                                        final int length1, final short[] set2, final int length2,
                                        final short[] buffer) {
        int pos = 0;
        int k1 = 0, k2 = 0;
        if (0 == length2) {
            System.arraycopy(set1, 0, buffer, 0, length1);
            return length1;
        }
        if (0 == length1) {
            System.arraycopy(set2, 0, buffer, 0, length2);
            return length2;
        }
        while (true) {
            if (toIntUnsigned(set1[k1]) < toIntUnsigned(set2[k2])) {
                buffer[pos++] = set1[k1];
                ++k1;
                if (k1 >= length1) {
                    for (; k2 < length2; ++k2)
                        buffer[pos++] = set2[k2];
                    break;
                }
            } else if (toIntUnsigned(set1[k1]) == toIntUnsigned(set2[k2])) {
                buffer[pos++] = set1[k1];
                ++k1;
                ++k2;
                if (k1 >= length1) {
                    for (; k2 < length2; ++k2)
                        buffer[pos++] = set2[k2];
                    break;
                }
                if (k2 >= length2) {
                    for (; k1 < length1; ++k1)
                        buffer[pos++] = set1[k1];
                    break;
                }
            } else {// if (set1[k1]>set2[k2])
                buffer[pos++] = set2[k2];
                ++k2;
                if (k2 >= length2) {
                    for (; k1 < length1; ++k1)
                        buffer[pos++] = set1[k1];
                    break;
                }
            }
        }
        return pos;
    }
    
    
    
    
    /**
     * Given a word w, return the position of the jth true bit.
     * 
     * @param w word
     * @param j index
     * @return position of jth true bit in w
     */
    public static int select(long w, int j) {
        int part1 = (int)( w & 0xFFFFFFFF);
        int wfirsthalf = Integer.bitCount(part1);
        if(wfirsthalf > j ) {
            return select(part1,j); 
        } else {
            return select((int)(w>>>32),j - wfirsthalf) + 32;
        }
    }

    /**
     * Given a word w, return the position of the jth true bit.
     * 
     * @param w word
     * @param j index
     * @return position of jth true bit in w
     */
    public static int select(int w, int j) {
        int part1 =  w & 0xFFFF;
        int wfirsthalf = Integer.bitCount(part1);
        if(wfirsthalf > j ) {
            return select((short)part1,j); 
        } else {
            return select((short)(w>>>16),j-wfirsthalf) + 16;
        }
    }
    

    /**
     * Given a word w, return the position of the jth true bit.
     * 
     * @param w word
     * @param j index
     * @return position of jth true bit in w
     */
    public static int select(short w, int j) {
        int sumtotal = 0;
        for(int counter = 0;  counter < 16 ; ++counter) {
            sumtotal += (w >> counter) & 1;
            if(sumtotal > j) return counter;
        }
        throw new IllegalArgumentException("cannot local "+j+"th bit in "+w+" weight is "+Integer.bitCount(w));
    }
    
    

    // flip bits start, start+1,..., end-1
    protected static void flipBitmapRange(long[] bitmap, int start, int end) {
            if (start == end) return;
            int firstword = start / 64;
            int endword   = (end - 1 ) / 64;
            bitmap[firstword] ^= ~(~0L << start);;
            for (int i = firstword; i < endword; i++)
                bitmap[i] = ~bitmap[i];
            bitmap[endword] ^= ~0L >>> -end;
    }

    // clear bits start, start+1,..., end-1
    protected static void resetBitmapRange(long[] bitmap, int start, int end) {
        if (start == end) return;
        int firstword = start / 64;
        int endword   = (end - 1 ) / 64;
        if(firstword == endword) {
          bitmap[firstword] &= ~((~0L << start) & (~0L >>> -end));
          return;       
        }
        bitmap[firstword] &= ~(~0L << start);
        for (int i = firstword+1; i < endword; i++)
            bitmap[i] = 0;
        bitmap[endword] &= ~(~0L >>> -end);
        
    }

    // set to true bits start, start+1,..., end-1
    protected static void setBitmapRange(long[] bitmap, int start, int end) {
        if (start == end) return;
        int firstword = start / 64;
        int endword   = (end - 1 ) / 64;
        if(firstword == endword) {
          bitmap[firstword] |= (~0L << start) & (~0L >>> -end);
          return;       
        }
        bitmap[firstword] |= ~0L << start;
        for (int i = firstword+1; i < endword; i++)
            bitmap[i] = ~0;
        bitmap[endword] |= ~0L >>> -end;
    }
        

    
}
