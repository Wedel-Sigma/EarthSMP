package io.github.wedelsigma.earthsmp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SidebarTextTest {
    @Test void homeUnlimitedOnlyForFirstTwoDays() {
        assertEquals("-", SidebarText.homeLabel(0,true,0));
        assertEquals("∞", SidebarText.homeLabel(1,true,0));
        assertEquals("∞", SidebarText.homeLabel(2,true,3));
        assertEquals("4/4", SidebarText.homeLabel(3,true,0));
        assertEquals("2/4", SidebarText.homeLabel(3,true,2));
        assertEquals("0/4", SidebarText.homeLabel(5,true,5));
        assertEquals("-", SidebarText.homeLabel(5,false,0));
    }
    @Test void longNamesAreTrimmedWithoutBreakingUnicode() {
        var member = new NationDatabase.Membership("a", "BardzoDlugaNazwaKrajuPolskiego", "LEADER");
        assertTrue(SidebarText.nationLabel(member).codePointCount(0,SidebarText.nationLabel(member).length()) <= 19);
        assertEquals("Brak panstwa", SidebarText.nationLabel(null));
    }
    @Test void alwaysRendersEightLines() {
        assertEquals(8, SidebarText.lines(0,null,null,0,false,0).size());
        var m = new NationDatabase.Membership("a", "Alfa", "LEADER");
        var r = new NationDatabase.NationRank("a", "Alfa", 5, 3);
        assertEquals(8, SidebarText.lines(3,m,r,2,true,1).size());
    }
}
