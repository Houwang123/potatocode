package potato;
import battlecode.common.*;

public strictfp class Drone extends Unit {

    Drone(RobotController rc) throws GameActionException{
        super(rc);
    }

    @Override
    public void run() throws GameActionException {
        /*Team enemy = rc.getTeam().opponent();
        MapLocation water = null;
        RobotInfo[] robots;
        // if drone is not holding unit or found water
        if (!rc.isCurrentlyHoldingUnit() || water==null) {
            // moves in a random direction
            for (int i=0;i<8;i++) {
                if (rc.isReady() && rc.canMove(directions[i])) {
                    rc.move(directions[i]);
                    System.out.println("I moved!");
                    // searches for water
                    if (rc.senseFlooding(rc.getLocation()))
                    {
                        water = rc.getLocation();
                    }
                    break;
                }
            }
            robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }                  
        }
        else {
            // if drone is one step away from water
            if (rc.getLocation().distanceSquaredTo(water)==1){
                // drone drops robot into water
                rc.dropUnit(rc.getLocation().directionTo(water));
                System.out.println("I dropped " + robots[0].getID() + "!");
            }
            else
            {
                // if not near water, drone moves towards water
                rc.move(rc.getLocation().directionTo(water));
                System.out.println("I moved!");
            } 
        } */       
    }        
}