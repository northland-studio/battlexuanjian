package com.northland.battledxuanjian.capture;

/**
 * 据点长方体选区（纯逻辑，不依赖 Bukkit，便于单元测试）。
 *
 * <p>坐标在构造时自动归一化，保证 min ≤ max。</p>
 */
public record PointRegion(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public PointRegion {
        int x1 = Math.min(minX, maxX);
        int x2 = Math.max(minX, maxX);
        int y1 = Math.min(minY, maxY);
        int y2 = Math.max(minY, maxY);
        int z1 = Math.min(minZ, maxZ);
        int z2 = Math.max(minZ, maxZ);
        minX = x1;
        maxX = x2;
        minY = y1;
        maxY = y2;
        minZ = z1;
        maxZ = z2;
    }

    public boolean contains(String worldName, double x, double y, double z) {
        if (worldName == null || world == null || !world.equals(worldName)) {
            return false;
        }
        return x >= minX && x <= maxX + 1.0
                && y >= minY && y <= maxY + 1.0
                && z >= minZ && z <= maxZ + 1.0;
    }

    public boolean containsBlock(String worldName, int x, int y, int z) {
        return worldName != null && world != null && world.equals(worldName)
                && x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public String sizeLabel() {
        return (maxX - minX + 1) + "x" + (maxY - minY + 1) + "x" + (maxZ - minZ + 1);
    }

    public double centerX() {
        return (minX + maxX + 1) / 2.0;
    }

    public double centerY() {
        return minY;
    }

    public double centerZ() {
        return (minZ + maxZ + 1) / 2.0;
    }
}
