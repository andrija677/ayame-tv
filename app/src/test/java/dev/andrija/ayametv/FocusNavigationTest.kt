package dev.andrija.ayametv

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusNavigationTest {
    @Test fun topRowUpReturnsToNavigation() {
        for (index in 0..4) assertEquals(-1, nextAppFocus(index, 12, 5, false, Arrow.Up))
    }
    @Test fun gridDoesNotWrapAcrossRows() {
        assertEquals(4, nextAppFocus(4, 12, 5, false, Arrow.Right))
        assertEquals(5, nextAppFocus(5, 12, 5, false, Arrow.Left))
        assertEquals(6, nextAppFocus(5, 12, 5, false, Arrow.Right))
    }
    @Test fun incompleteLastRowNeverTargetsAMissingApp() {
        assertEquals(1, nextAppFocus(6, 12, 5, false, Arrow.Up))
        assertEquals(11, nextAppFocus(6, 12, 5, false, Arrow.Down))
        assertEquals(7, nextAppFocus(7, 12, 5, false, Arrow.Down))
        assertEquals(11, nextAppFocus(11, 12, 5, false, Arrow.Right))
    }
    @Test fun horizontalFavoritesHaveOneNavigationRow() {
        assertEquals(-1, nextAppFocus(8, 10, 5, true, Arrow.Up))
        assertEquals(8, nextAppFocus(8, 10, 5, true, Arrow.Down))
        assertEquals(9, nextAppFocus(8, 10, 5, true, Arrow.Right))
    }
    @Test fun emptyListsReturnToNavigation() {
        assertEquals(-1, nextAppFocus(0, 0, 5, false, Arrow.Down))
    }
}
