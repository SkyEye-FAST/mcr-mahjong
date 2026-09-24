package top.skyeyefast.mcr;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JavaApiTest {
    @Test
    void ordinaryJava17Consumer() {
        var hand = new Hand(Tiles.parse("19m19s19pESWNCFP"));
        assertEquals(0, McrMahjong.shanten(hand));
        assertEquals(13, McrMahjong.waitingTiles(hand).size());
        var result = McrMahjong.score(hand, Tile.NORTH, new WinContext());
        assertInstanceOf(ScoreResult.Winning.class, result);
        var score = (ScoreResult.Winning) result;
        assertEquals(88, score.getTotalFan());
        assertEquals(1, score.count(Fan.THIRTEEN_ORPHANS));
        assertTrue(score.getMeetsMinimum());
        assertEquals(Tile.NORTH, McrMahjong.discards(hand, Tile.NORTH).get(0).getDiscard());
        assertEquals(39, McrMahjong.analyze(hand).getRemainingCount());
    }

    @Test
    void javaNullsAndOutputConstructionCannotBypassValidation() {
        assertThrows(NullPointerException.class, () -> new Hand(null));
        assertThrows(NullPointerException.class, () -> new Meld.Chow(null));
        var tiles = new ArrayList<>(Tiles.parse("19m19s19pESWNCFP"));
        tiles.set(0, null);
        assertThrows(IllegalArgumentException.class, () -> new Hand(tiles));
        assertThrows(IllegalArgumentException.class, () -> new Hand(
            Tiles.parse("123m456s789pE"), Collections.singletonList(null)));
        var hand = new Hand(Tiles.parse("19m19s19pESWNCFP"));
        assertThrows(IllegalArgumentException.class, () -> McrMahjong.analyze(hand, Collections.singletonList(null)));
        assertThrows(IllegalArgumentException.class, () -> McrMahjong.discards(hand, Tile.NORTH, Collections.singletonList(null)));
        assertThrows(NullPointerException.class, () -> McrMahjong.score(hand, null));
        assertThrows(IllegalArgumentException.class, () -> new FanCount(Fan.BIG_FOUR_WINDS, Integer.MAX_VALUE));
        for (var type : List.of(FormAnalysis.class, HandAnalysis.class, DiscardAnalysis.class, ScoreResult.Winning.class)) {
            assertTrue(Arrays.stream(type.getConstructors()).allMatch(c -> c.isSynthetic()), type.getName());
        }
    }

    @Test
    void snapshotsAndAllReturnedCollectionsAreImmutable() {
        var tiles = new ArrayList<>(Tiles.parse("258m1477s369p"));
        var melds = new ArrayList<Meld>(List.of(new Meld.Chow(Tile.M4, ChowPosition.HIGH)));
        var hand = new Hand(tiles, melds);
        tiles.clear();
        melds.clear();
        assertEquals(10, hand.getConcealedTiles().size());
        assertEquals(1, hand.getMelds().size());
        assertThrows(UnsupportedOperationException.class, () -> hand.getMelds().clear());
        assertThrows(UnsupportedOperationException.class, () -> Tiles.parse("1m").clear());
        assertThrows(UnsupportedOperationException.class, () -> McrMahjong.waitingTiles(hand).clear());

        var visible = new ArrayList<Tile>();
        var analysis = McrMahjong.analyze(hand, visible);
        int count = analysis.getRemainingCount();
        visible.add(Tile.S7);
        assertEquals(count, analysis.getRemainingCount());
        assertThrows(UnsupportedOperationException.class, () -> analysis.getEffectiveTiles().clear());
        for (var form : analysis.getForms()) {
            assertThrows(UnsupportedOperationException.class, () -> form.getEffectiveTiles().clear());
        }
        var discards = McrMahjong.discards(hand, Tile.S7);
        assertThrows(UnsupportedOperationException.class, discards::clear);
        assertThrows(UnsupportedOperationException.class, () -> discards.get(0).getCompletesForms().clear());
        var score = (ScoreResult.Winning) McrMahjong.score(hand, Tile.S7);
        assertThrows(UnsupportedOperationException.class, () -> score.getFans().clear());
        assertEquals(15, score.getTotalFan());
    }
}
