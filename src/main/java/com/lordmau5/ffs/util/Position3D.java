package com.lordmau5.ffs.util;

/**
 * Created by Dustin on 28.06.2015.
 */
public class Position3D {

    private int x, y, z;

    public Position3D(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Position3D getDistance(Position3D otherPoint) {
        return new Position3D(getX() - otherPoint.getX(), getY() - otherPoint.getY(), getZ() - otherPoint.getZ());
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Position3D))
            return false;
        Position3D other = (Position3D) obj;
        return other.getX() == getX() && other.getY() == getY() && other.getZ() == getZ();
    }

    @Override
    public String toString() {
        return "X: " + getX() + " - Y: " + getY() + " - Z: " + getZ();
    }

    @Override
    public int hashCode() {
        int hash = 23;
        hash = hash * 31 + getX();
        hash = hash * 31 + getY();
        hash = hash * 31 + getZ();
        return hash;
    }
}
