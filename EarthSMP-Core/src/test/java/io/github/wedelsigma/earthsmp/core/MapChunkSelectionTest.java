package io.github.wedelsigma.earthsmp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapChunkSelectionTest {
    @Test void parsesNegativeCoordinatesAndDisjointChunks() {
        var chunks = MapChunkSelection.parse("-10,20;1,2;10,-3");
        assertEquals(3, chunks.size());
        assertEquals(new NationDatabase.TerritoryChunk(-10,20),chunks.getFirst());
        assertEquals(new NationDatabase.TerritoryChunk(10,-3),chunks.getLast());
    }
    @Test void rejectsDuplicatesMalformedAndEmpty() {
        assertThrows(IllegalArgumentException.class,()->MapChunkSelection.parse("1,2;1,2"));
        assertThrows(IllegalArgumentException.class,()->MapChunkSelection.parse("1,2;"));
        assertThrows(IllegalArgumentException.class,()->MapChunkSelection.parse("1, 2"));
        assertThrows(IllegalArgumentException.class,()->MapChunkSelection.parse(""));
        assertThrows(IllegalArgumentException.class,()->MapChunkSelection.parse("1000001,1"));
    }
    @Test void rejectsTooManyChunks() {
        StringBuilder b=new StringBuilder();
        for(int i=0;i<1025;i++) {if(i>0)b.append(';');b.append(i).append(",0");}
        assertThrows(IllegalArgumentException.class,()->MapChunkSelection.parse(b.toString()));
    }
}
