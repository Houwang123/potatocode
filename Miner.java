package potato;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import battlecode.common.*;

public strictfp class Miner extends Unit {

    Miner(RobotController rc) throws GameActionException {
        super(rc);
    }

    //General
    MapLocation moveTarget;

    //Constants
    final int REFINERY_DISTANCE = 250;
    final int BUILD_DISTANCE = 100;

    //Building stuff
    ArrayList<RobotType> buildTarget = new ArrayList<RobotType>();
    ArrayList<MapLocation> buildLocation = new ArrayList<MapLocation>();
    ArrayList<Integer> buildID = new ArrayList<Integer>();
    MapLocation currentBuildLocation;
    Integer currentBuildID = -1;
    RobotType currentBuildTarget;

    //Refinery stuff
    ArrayList<MapLocation> refinerySpots = new ArrayList<MapLocation>();
    boolean wantToBuildRefinery = false;
    MapLocation leftMineLocation;

    MapLocation findClosestRefinery(MapLocation loc) {
        if (refinerySpots.size() == 0) {
            return null;
        }
        int distance = Integer.MAX_VALUE;
        MapLocation res = null;
        for (MapLocation refinery : refinerySpots) {
            if (refinery.distanceSquaredTo(loc) < distance) {
                distance = refinery.distanceSquaredTo(loc);
                res = refinery; 
            }
        }
        return res;
    }

    //Refining stuff
    void goRefine() throws GameActionException {
        MapLocation closestRefinery = findClosestRefinery(myMapLocation);
        leftMineLocation = myMapLocation;
        if (closestRefinery == null || closestRefinery.distanceSquaredTo(myMapLocation)>REFINERY_DISTANCE) {
            wantToBuildRefinery = true;
        }
        state = 52;
    }

    boolean tryRefine() throws GameActionException {
        for (Direction dir : Direction.allDirections()){
            if (rc.isReady() && rc.canDepositSoup(dir)) {
                rc.depositSoup(dir, rc.getSoupCarrying());
                return true;
            }
        }
        return false;
    }

    //Mining Stuff
    boolean tryMine() throws GameActionException {
        for (Direction dir : Direction.allDirections()){
            if (rc.isReady() && rc.canMineSoup(dir)) {
                rc.mineSoup(dir);
                //TODO: Send message once sector finished mining
                return true;
            }
        }
        return false;
    }

    //General stuff
    void reset() {
        //Resets state to 0
        state = 0;
        moveTarget = null;
        currentBuildID = -1;
        currentBuildLocation = null;
        currentBuildTarget = null;
        leftMineLocation = null;
        wantToBuildRefinery = false;

    }

    void minerUpdate() throws GameActionException {
        update();
        for (MapLocation processingMapLocation : nearbySoup) {
            if (state == 0) {
                moveTarget = null;
            }
        }

        for (RobotInfo robot : nearbyRobots) {
            if (robot.getType() == RobotType.REFINERY && robot.getTeam() == team && !refinerySpots.contains(robot.getLocation())) {
                refinerySpots.add(robot.getLocation());
            }
        }
        minerCommunication();
    }

    void minerCommunication() {
        for (Transaction trans : latestCommunication) {
            int[] message = getInformation(trans);
            if (message[6] != KEY) {
                continue;
            }

            //Potential mining data
            if (message[0] == 0) {
                MapLocation loc = new MapLocation((message[1]%10000-message[1]%100)/100, message[1]%100);
                if (message[3] > 0 && state == 0) {
                    moveTarget = null;
                }
                //Someone built a refinery
                if (message[4] != 0 && message[4]/10==0 && message[4]%10 ==3) {
                    if (!refinerySpots.contains(loc)) {
                        refinerySpots.add(loc);
                    }
                }
            }

            //Building data
            if (message[0] == 1) {
                MapLocation loc = new MapLocation((message[1]%10000-message[1]%100)/100, message[1]%100);
                buildLocation.add(loc);
                buildTarget.add(getRobotTypeFromID(message[4]));
                buildID.add(message[5]);
            }
            if (message[0] == 5) {
                for (int i = 0; i < buildID.size(); i++) {
                    if (buildID.get(i) == message[5]) {
                        buildID.remove(i);
                        buildTarget.remove(i);
                        buildLocation.remove(i);
                        break;
                    }
                    if (currentBuildID == message[5]) {
                        reset();
                    }
                }
            }
            
        }
    }

    //Run function

    @Override
    public void run() throws GameActionException {
        minerUpdate();
        System.out.println(state);

        if (turn == 1) {
            for (MapLocation loc : getAdjacent()) {
                if (rc.isLocationOccupied(loc) && rc.senseRobotAtLocation(loc).getType() == RobotType.HQ && rc.senseRobotAtLocation(loc).getTeam() == team) { 
                    hqLoc = loc;
                    refinerySpots.add(loc);
                }
            }
        }

        //Building on orders
        if (state != 53) {
            for (int i = 0; i < buildLocation.size(); i ++) {
                if (buildLocation.get(i).distanceSquaredTo(myMapLocation) < BUILD_DISTANCE && rc.getTeamSoup()>buildTarget.get(i).cost+30) {
                    state = 53;
                    currentBuildID = buildID.get(i);
                    currentBuildTarget = buildTarget.get(i);
                    currentBuildLocation = buildLocation.get(i);
                    break;
                }
            }
        }

        if (state == 53) {

            System.out.println(currentBuildLocation);
            System.out.println(currentBuildTarget);

            moveTarget = currentBuildLocation;
            if (myMapLocation.equals(currentBuildLocation)) {
                for  (Direction dir : directions) {
                    if (canMoveWithoutSuicide(dir)) {
                        rc.move(dir);
                    }
                }
            }
            if (rc.canSenseLocation(currentBuildLocation) && rc.senseRobotAtLocation(currentBuildLocation) != null && rc.senseRobotAtLocation(currentBuildLocation).getTeam()==team && rc.senseRobotAtLocation(currentBuildLocation).getType()==currentBuildTarget) {
                //Potential bug
                for (int i = 0; i < buildID.size(); i++) {
                    if (buildID.get(i) == currentBuildID) {
                        buildID.remove(i);
                        buildTarget.remove(i);
                        buildLocation.remove(i);
                        break;
                    }
                }
                tryBroadcastSuccess(currentBuildID, cheapSend);
                reset();
            }
            if (getAdjacent().contains(currentBuildLocation)) {
                moveTarget = myMapLocation;
                if (tryBuild(currentBuildTarget, myMapLocation.directionTo(currentBuildLocation))) {
                    while(!tryBroadcastSuccess(currentBuildID, averageSend));
                    reset();
                }
            }
        }


        //Doing nothing
        if (state == 0) {
            if (moveTarget == null) {
                //Find a mining spot with BFS
                Queue<Integer> queue = new LinkedList<Integer>();
                HashMap<Integer,Boolean> visited = new HashMap<Integer,Boolean>();
                queue.add(getMiniMapLocation(myMapLocation));
                while (queue.size() > 0) {
                    Integer current = queue.poll();
                    if (visited.containsKey(current)) {
                        continue;
                    }
                    visited.put(current,true);
                    if (map[current][1] > 0) {
                        moveTarget = locationInMiniMap(current);
                        if (state != 53) {
                            state = 51;
                        }
                        state = 51;
                        break;
                    }
                    //Add nearby sectors
                    int x = current%miniMapWidth;
                    int y = current/miniMapWidth;
                    if (x > 0) {
                        queue.add((x-1)+y*miniMapWidth);
                    }
                    if (x < miniMapWidth-1) {
                        queue.add((x+1)+y*miniMapWidth);
                    }
                    if (y > 0) {
                        queue.add(x+(y-1)*miniMapWidth);
                    }
                    if (y < miniMapHeight-1) {
                        queue.add(x+(y+1)*miniMapWidth);
                    }
                }
            }
        }
            
        if (state == 0 && (moveTarget == null || myMapLocation.equals(moveTarget))) {
            //Nothing must have been found. Go to a random location and explore
            moveTarget = randomLocation(); 
        }

        //Mining
        if (state == 51) {
            //System.out.printf("Currently carrying %d soup",rc.getSoupCarrying());
            //So many edge cases I want to kill myself
            while(tryMine());
            if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
                goRefine();
                Clock.yield();
            }
            if (rc.canSenseLocation(moveTarget) && rc.senseSoup(moveTarget)==0 && nearbySoup.length > 0) {
                moveTarget = nearbySoup[0];
            }
            if (getMiniMapLocation(myMapLocation) == getMiniMapLocation(moveTarget) && nearbySoup.length==0) {
                if (rc.getSoupCarrying()>0) {
                    goRefine();
                } else {
                    state = 0;
                    moveTarget = null;
                }
                Clock.yield();
            }
        }

        //Refining
        if (state == 52) {
            moveTarget = findClosestRefinery(myMapLocation);
            if (moveTarget == null) {
                wantToBuildRefinery = true;
                leftMineLocation = myMapLocation;
            } else {
                if (myMapLocation.distanceSquaredTo(moveTarget)>400 && nearbySoup.length>0) {
                    wantToBuildRefinery = true;
                    leftMineLocation = myMapLocation;
                }
                if (leftMineLocation.distanceSquaredTo(findClosestRefinery(leftMineLocation)) < REFINERY_DISTANCE) {
                    wantToBuildRefinery = false;
                }
            }
            if (wantToBuildRefinery) {
                for (Direction dir : directions) {
                    if (tryBuild(RobotType.REFINERY, dir)) {
                        wantToBuildRefinery = false;
                        tryBroadcastLocation(rc.adjacentLocation(dir), fastSend);
                        refinerySpots.add(rc.adjacentLocation(dir));
                    }
                }
            }

            tryRefine();
            if (rc.getSoupCarrying() == 0) {
                reset();
            }
        }

        //Movement

        if (rc.isReady()) {
            if (moveTarget != null && bugNavigate(moveTarget)) {
                if (!myMapLocation.equals(moveTarget)) {
                    //Not sure if this should be added
                    map[getMiniMapLocation(moveTarget)][1] = 0;
                    //Can't get to the target
                    if (state == 53) {
                        for (int i = 0; i < buildID.size(); i++) {
                            if (buildID.get(i) == currentBuildID) {
                                buildID.remove(i);
                                buildTarget.remove(i);
                                buildLocation.remove(i);
                                break;
                            }
                        }
                    }
                    if (state == 52) {
                        for (int i = 0; i < refinerySpots.size(); i ++) {
                            if (refinerySpots.get(i) == moveTarget) {
                                refinerySpots.remove(i);
                                moveTarget = null;
                            }
                        }
                    }
                    System.out.println("Can't get there!");
                    reset();
                    
                }
            }
        }
    }
}