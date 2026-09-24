package example;

import top.skyeyefast.mcr.*;

public final class JavaConsumer {
    public static void main(String[] args) {
        // This return type belongs to kotlin-stdlib, which must be on the Java compile classpath.
        if (Tile.getEntries().size() != 34 || Fan.getEntries().size() != 82) {
            throw new AssertionError("Published API dependency metadata");
        }
        var hand = new Hand(Tiles.parse("19m19s19pESWNCFP"));
        if (McrMahjong.shanten(hand) != 0 || McrMahjong.waitingTiles(hand).size() != 13) {
            throw new AssertionError("Thirteen-orphans waits");
        }
        if (McrMahjong.analyze(hand).getRemainingCount() != 39) {
            throw new AssertionError("Effective tiles");
        }
        var result = McrMahjong.score(hand, Tile.NORTH, new WinContext());
        if (!(result instanceof ScoreResult.Winning win) || win.getTotalFan() != 88
                || win.count(Fan.THIRTEEN_ORPHANS) != 1 || !win.getMeetsMinimum()) {
            throw new AssertionError("Scoring through the published Java API");
        }
        if (!McrMahjong.discards(hand, Tile.NORTH).get(0).getCompletesForms().contains(HandForm.THIRTEEN_ORPHANS)) {
            throw new AssertionError("Discard analysis");
        }
        System.out.println("Java 17 / Maven POM consumer passed: " + result);
    }
}
