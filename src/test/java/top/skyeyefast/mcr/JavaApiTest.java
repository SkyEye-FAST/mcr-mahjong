package top.skyeyefast.mcr;

import org.junit.jupiter.api.Test;
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
}
