package example;

import top.skyeyefast.mcr.*;

public final class JavaConsumer {
    public static void main(String[] args) {
        if (!"wmo-2006-en".equals(McrMahjong.SCORING_PROFILE)) {
            throw new AssertionError("Unexpected public scoring contract");
        }
        // This return type belongs to kotlin-stdlib, which must be on the Java compile classpath.
        if (Tile.getEntries().size() != 34 || Fan.getEntries().size() != 81 || Fan.TWO_CONCEALED_KONGS.getPoints() != 8) {
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
        var mixedHand = new Hand(Tiles.parse("345pEE67s"), java.util.List.of(
                new Meld.Kong(Tile.M1), new Meld.Kong(Tile.S2, RelativePlayer.LEFT)));
        var mixed = (ScoreResult.Winning) McrMahjong.score(mixedHand, Tile.S8, new WinContext(WinMethod.SELF_DRAW));
        var mixedAward = mixed.getFans().stream().filter(FanCount::isMixedKongPair).findFirst().orElseThrow();
        if (mixed.getTotalFan() != 8 || !mixed.getMeetsMinimum() || mixedAward.getPoints() != 6) {
            throw new AssertionError("Public mixed-kong exception through Maven POM");
        }
        System.out.println("Java 17 / Maven POM consumer passed: " + result);
    }
}
