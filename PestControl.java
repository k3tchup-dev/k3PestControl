package com.k3.script;

import org.powerbot.script.*;
import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.rt6.GameObject;

import java.util.concurrent.Callable;

@Script.Manifest(name = "k3PestControl", description = "Trains at Pest Control without gaining Constitution XP.", properties = "")
public class PestControl extends PollingScript<ClientContext> {

    final Tile START_TILE = new Tile(2638, 2653, 0);
    final Tile BOAT_TILE = new Tile(2634, 2653, 0);
    private int barricadeIds[] = {14227, 14228, 14229, 14230, 14231, 14232};
    private boolean gotLogs = false;

    @Override
    public void poll() {
        switch (state()) {
            case IDLE:
                break;

            case START:
                gotLogs = false;
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.chat.queryContinue();
                    }
                }, 100, 10);
                if (ctx.chat.queryContinue()) {
                    ctx.chat.clickContinue();
                } else {
                    final GameObject plank = ctx.objects.select().id(25632).nearest().poll();
                    plank.interact("Cross");
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return ctx.players.local().tile().equals(BOAT_TILE);
                        }
                    });
                }
                break;

            case GET_LOGS:
                final GameObject pileOfLogs = ctx.objects.select().id(91455).nearest().poll();
                pileOfLogs.interact("Take 10");
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.backpack.select().count() == 10;
                    }
                });
                gotLogs = true;
                break;

            case REPAIR:
                final int LOGS = ctx.backpack.select().count();
                if (LOGS > 0) {
                    final GameObject barricade = ctx.objects.select().id(barricadeIds).nearest().poll();
                    if (barricade.inViewport()) {
                        barricade.interact("Repair");
                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ctx.backpack.select().count() == LOGS - 1;
                            }
                        }, 150, 10);
                    }
                    else {
                        if (canReach(barricade)) {
                            ctx.movement.step(barricade);
                            ctx.camera.turnTo(barricade);
                            Condition.wait(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {
                                    return barricade.inViewport();
                                }
                            });
                        }
                        else {
                            GameObject door = ctx.objects.select().id(25636).nearest().poll();
                            if (door.inViewport()) {
                                door.interact("Open");
                            } else {
                                ctx.movement.step(door);
                                ctx.camera.turnTo(door);
                            }
                        }
                    }
                }
                break;
        }
    }

    private State state() {
        // On RuneScape
        if (ctx.objects.select().id(barricadeIds).size() == 0) {
            // On the Pier
            if (ctx.players.local().tile().equals(START_TILE)) {
                gotLogs = false;
                return State.START;
            }
            // In the Boat
            else if (ctx.players.local().tile().equals(BOAT_TILE)) {
                return State.IDLE;
            }
        }
        // On Pest Control Island
        else {
            // Retrieved Logs
            if (gotLogs) {
                return State.REPAIR;
            }
            // Haven't Retrieved Logs
            else {
                return State.GET_LOGS;
            }
        }
        // Do Nothing
        return State.IDLE;
    }

    private boolean canReach(Locatable l) {
        // Array of orthogonal tiles
        final Tile[] tiles = {
                l.tile().derive(-1, 0), l.tile().derive(1, 0),
                l.tile().derive(0, -1), l.tile().derive(0, 1)
        };
        for (Tile t : tiles) {
            // Filters objects to only include those at 't'
            ctx.objects.select().at(t);
            // Filters to only include boundaries
            ctx.objects.select(new Filter<GameObject>() {
                @Override
                public boolean accept(GameObject gameObject) {
                    return gameObject.type() == GameObject.Type.BOUNDARY;
                }
            });
            // Checks there are no objects and t is reachable
            if (ctx.objects.isEmpty() && t.matrix(ctx).reachable()) {
                return true;
            }
        }
        return false;
    }

    private enum State {
        START, IDLE, GET_LOGS, REPAIR
    }
}