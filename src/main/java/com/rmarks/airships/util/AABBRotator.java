package com.rmarks.airships.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AABBRotator
{
    private static Vec3 vec00, vec01, vec10, vec11;
    private static Vec3	vec0h, vec1h, vech0, vech1;
    static
    {
        vec00 = new Vec3(0D, 0D, 0D);
        vec01 = new Vec3(0D, 0D, 0D);
        vec10 = new Vec3(0D, 0D, 0D);
        vec11 = new Vec3(0D, 0D, 0D);

        vec0h = new Vec3(0D, 0D, 0D);
        vec1h = new Vec3(0D, 0D, 0D);
        vech0 = new Vec3(0D, 0D, 0D);
        vech1 = new Vec3(0D, 0D, 0D);
    }

    public static void rotateAABBAroundY(AABB aabb, double xOff, double zOff, float ang)
    {
        double y0 = aabb.maxY;
        double y1 = aabb.maxY;

        vec00 = new Vec3(aabb.minX - xOff, vec00.y, aabb.minZ - zOff);
        vec01 = new Vec3(aabb.minX - xOff, vec00.y, aabb.maxZ - zOff);
        vec10 = new Vec3(aabb.maxX - xOff, vec00.y, aabb.minZ - zOff);
        vec11 = new Vec3(aabb.maxX - xOff, vec00.y, aabb.maxZ - zOff);

        vec00.yRot(ang);
        vec01.yRot(ang);
        vec10.yRot(ang);
        vec11.yRot(ang);

        vec0h = new Vec3((vec00.x + vec01.x) / 2D, vec0h.y, (vec00.z + vec01.z) / 2D);
        vec1h = new Vec3((vec10.x + vec11.x) / 2D, vec1h.y, (vec10.z + vec11.z) / 2D);
        vech0 = new Vec3((vec00.x + vec10.x) / 2D, vech0.y, (vec00.z + vec10.z) / 2D);
        vech1 = new Vec3((vec01.x + vec11.x) / 2D, vech1.y, (vec01.z + vec11.z) / 2D);

        aabb.setMinX(minX());
        aabb.setMinY(y0);
        aabb.setMinZ(minZ());
        aabb.setMaxX(maxX());
        aabb.setMaxY(y1);
        aabb.setMaxZ(maxZ());
        aabb.move(xOff, 0F, zOff);
    }

    private static double minX() {
        return Math.min(Math.min(Math.min(vec0h.x, vec1h.x), vech0.x), vech1.x);
    }

    private static double minZ() {
        return Math.min(Math.min(Math.min(vec0h.z, vec1h.z), vech0.z), vech1.z);
    }

    private static double maxX() {
        return Math.max(Math.max(Math.max(vec0h.x, vec1h.x), vech0.x), vech1.x);
    }

    private static double maxZ() {
        return Math.max(Math.max(Math.max(vec0h.z, vec1h.z), vech0.z), vech1.z);
    }
}
