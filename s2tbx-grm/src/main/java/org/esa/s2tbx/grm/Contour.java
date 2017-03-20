package org.esa.s2tbx.grm;

import com.sun.javafx.scene.shape.PathUtils;

/**
 * @author Jean Coravu
 */
public class Contour {
    public static final byte TOP_MOVE_INDEX = 0;
    public static final byte RIGHT_MOVE_INDEX = 1;
    public static final byte BOTTOM_MOVE_INDEX = 2;
    public static final byte LEFT_MOVE_INDEX = 3;

    private byte[] bits;
    private int size;

    public Contour() {
        this.size = 0;
        this.bits = new byte[] {0, 0};
    }

    public Contour(int size, byte[] bits) {
        this.size = size;
        this.bits = bits;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int i=0; i<size; i+=2) {
            if (getBitAt(i) == 1) {
                str.append("1");
            } else {
                str.append("0");
            }
            if (getBitAt(i+1) == 1) {
                str.append("1");
            } else {
                str.append("0");
            }
            if (i < size-2) {
                str.append(" "); // add an empty space
            }
        }
        return str.toString();
    }

    public void pushTop() { // Push0  contour.push_back(0); contour.push_back(0);
        pushTwoBits(TOP_MOVE_INDEX);
    }

    public void pushRight() { // Push1  contour.push_back(1); contour.push_back(0);
        pushTwoBits(RIGHT_MOVE_INDEX);
    }

    public void pushBottom() { // Push2  contour.push_back(0); contour.push_back(1);
        pushTwoBits(BOTTOM_MOVE_INDEX);
    }

    public void pushLeft() { // Push3  contour.push_back(1); contour.push_back(1);
        pushTwoBits(LEFT_MOVE_INDEX);
    }

    public byte getBitAt(int index) {
        int arrayIndex = index / 8;
        int positionsToMove = 7 - (index % 8);
        int value = (this.bits[arrayIndex] >> positionsToMove) & 0x01;
        return (byte)value;
    }

    public int getMove(int index) {
        int bitIndex = 2 * index;
        return (2 * getBitAt(bitIndex)) + getBitAt(bitIndex + 1);
    }

    public int size() {
        return this.size;
    }

    public byte[] getBits() {
        return bits;
    }

    private void pushTwoBits(byte bitValue) {
        int arrayLength = (this.size / 8) + 2;
        if (this.bits.length < arrayLength) {
            byte[] newBits = new byte[2 * this.bits.length]; // double the size
            for (int i=0; i<this.bits.length; i++) {
                newBits[i] = this.bits[i];
            }
            for (int i=this.bits.length; i<newBits.length; i++) {
                newBits[i] = 0; // fill with zero
            }
            this.bits = newBits;
        }
        int arrayIndex = this.size / 8;
        this.size += 2;
        int positionsToMove = 8 - (this.size % 8);
        if (positionsToMove == 8) {
            positionsToMove = 0; // no position to move
        }
        int x = this.bits[arrayIndex] >> positionsToMove;
        x = x | bitValue;
        x = x << positionsToMove;
        this.bits[arrayIndex] = (byte)x;
    }
}
