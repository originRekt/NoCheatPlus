package fr.neatmonster.nocheatplus.test;

import org.bukkit.Material;
import org.junit.Test;

import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.FakeBlockCache;
import fr.neatmonster.nocheatplus.utilities.PassableRayTracing;

public class TestPassableRayTracing {
    
    // TODO: Moving into a block, 
    // TODO: Moving out of a block
    // TODO: Moving horizontally on various kinds of ground (normal, half blocks)
    // TODO: Moving up stairs etc ?
    // TODO: From ground and onto ground moves, onto-edge moves (block before edge, into block, etc).
    // TODO: Randomized tests (Collide with inner sphere, not collide with outer sphere).
    
    public TestPassableRayTracing() {
        StaticLog.setUseLogManager(false);
        BlockTests.initBlockProperties();
        StaticLog.setUseLogManager(true);
    }
    
    @Test
    public void testAir() {
        FakeBlockCache bc = new FakeBlockCache();
        PassableRayTracing rt = new PassableRayTracing();
        rt.setBlockCache(bc);
        double[] coords = new double[]{0.5, 0.5, -0.5, 0.5, 0.5, 1.5};
        rt.set(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
        rt.loop();
        if (rt.collides()) {
            TestRayTracing.doFail("Expect not to collide when moving through a block.", coords);
        }
        if (rt.getStepsDone() > 4) {
            TestRayTracing.doFail("Expect less than 4 steps for moving straight through a block of air.", coords);
        }
        rt.cleanup();
        bc.cleanup();
    }
    
    @Test
    public void testThroughOneBlock() {
        FakeBlockCache bc = new FakeBlockCache();
        bc.set(0, 0, 0, Material.STONE);
        PassableRayTracing rt = new PassableRayTracing();
        rt.setBlockCache(bc);
        double[][] setups = new double[][] {
            // Through the middle of the block.
            {0.5, 0.5, -0.5, 0.5, 0.5, 1.5},
            {-0.5, 0.5, 0.5, 1.5, 0.5, 0.5},
            {0.5, -0.5, 0.5, 0.5, 1.5, 0.5},
            // Along the edges.
            {0.5, 0.0, -0.5, 0.5, 0.0, 1.5},
            {-0.5, 0.0, 0.5, 1.5, 0.0, 0.5},
            // Exactly diagonal.
            {-0.5, -0.5, -0.5, 1.5, 1.5, 1.5}, // 3d
            {-0.5, 0.0, -0.5, 1.5, 0.0, 1.5}, // 2d
            // Through a corner.
            {1.2, 0.5, 0.5, 0.5, 0.5, 1.2},
            
            // TODO: More of each and other... + generic set-ups?
        };
        TestRayTracing.runCoordinates(rt, setups, true, false, 3.0, true);
        rt.cleanup();
        bc.cleanup();
    }
    
    /**
     * Moving diagonally through an "empty corner", seen from above:<br>
     * ox<br>
     * xo
     */
    @Test
    public void testEmptyCorner() {
        FakeBlockCache bc = new FakeBlockCache();
        // The "empty corner" setup.
        bc.set(10, 70, 10, Material.STONE);
        bc.set(11, 70, 11, Material.STONE);
        // Ground.
        for (int x = 9; x < 13; x++) {
            for (int z = 9; z < 13; z++) {
                bc.set(x, 69, z, Material.STONE);
            }
        }
        PassableRayTracing rt = new PassableRayTracing();
        rt.setBlockCache(bc);
        // TODO: More Directions, over a corner, sides, etc.
        double[][] setups = new double[][] {
            // Slightly off the middle (11, y, 11)
            {11.4, 70.0, 10.4, 10.6, 70.0, 11.4},
            // Going exactly through the middle (11, y, 11)
            {11.4, 70.0, 10.6, 10.6, 70.0, 11.4},
            {11.5, 70.0, 10.5, 10.5, 70.0, 11.5},
        };
        TestRayTracing.runCoordinates(rt, setups, true, false, 3.0, true);
        rt.cleanup();
        bc.cleanup();
    }
    
    @Test
    public void testGround() {
        FakeBlockCache bc = new FakeBlockCache();
        // Ground using full blocks.
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                bc.set(x, 65, z, Material.STONE);
            }
        }
        PassableRayTracing rt = new PassableRayTracing();
        rt.setBlockCache(bc);
        // TODO: More Directions, also from air underneath to ground).
        double[][] noCollision = new double[][] {
            {1.3, 66.0, 2.43, 5.25, 66.0, 7.12},
        };
        TestRayTracing.runCoordinates(rt, noCollision, false, true, 3.0, true);
        double[][] shouldCollide = new double[][] {
            {1.3, 65.1, 2.43, 2.3, 65.1, 4.43},
            {1.3, 65.0, 2.43, 2.3, 65.0, 4.43},
            {1.3, 66.0, 2.43, 1.3, 65.9, 2.43},
            {1.3, 66.0, 2.43, 5.25, 65.9, 7.12},
            {1.3, 65.4, 2.43, 1.3, 65.4, 2.43}, // No distance.
        };
        TestRayTracing.runCoordinates(rt, shouldCollide, true, false, 3.0, true);
        rt.cleanup();
        bc.cleanup();
    }
    
    @Test
    public void testGroundSteps() {
        FakeBlockCache bc = new FakeBlockCache();
        // Ground using 0.5 high step blocks.
        final double[] stepBounds = new double[]{0.0, 0.0, 0.0, 1.0, 0.5, 1.0};
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // STONE = HACK (CompatBukkit block-flags problem).
                bc.set(x, 65, z, Material.STONE, stepBounds);
            }
        }
        PassableRayTracing rt = new PassableRayTracing();
        rt.setBlockCache(bc);
        // TODO: More Directions, also from air underneath to ground).
        double[][] noCollision = new double[][] {
            {1.3, 65.5, 2.43, 5.25, 65.5, 7.12},
        };
        TestRayTracing.runCoordinates(rt, noCollision, false, true, 3.0, true);
        double[][] shouldCollide = new double[][] {
            {1.3, 65.1, 2.43, 2.3, 65.1, 7.43},
            {1.3, 65.0, 2.43, 2.3, 65.0, 7.43},
            {1.3, 65.5, 2.43, 1.3, 65.4, 2.43},
            {1.3, 65.5, 2.43, 5.25, 65.4, 7.12},
            {1.3, 65.4, 2.43, 1.3, 65.4, 2.43}, // No distance.
        };
        TestRayTracing.runCoordinates(rt, shouldCollide, true, false, 3.0, true);
        rt.cleanup();
        bc.cleanup();
    }
    
}
