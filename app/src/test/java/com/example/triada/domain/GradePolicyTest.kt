package com.example.triada.domain

import com.example.triada.domain.model.GradePolicy
import org.junit.Assert.*
import org.junit.Test

class GradePolicyTest {
    @Test fun scoreIsProportionalAndRoundedToOneDecimal() {
        assertEquals(0.0,GradePolicy.score(0,3),0.001)
        assertEquals(13.3,GradePolicy.score(2,3),0.001)
        assertEquals(20.0,GradePolicy.score(3,3),0.001)
    }
    @Test fun invalidCountsAreRejected() {
        listOf(0 to 0, -1 to 3, 4 to 3).forEach { (a,b)-> assertTrue(runCatching { GradePolicy.score(a,b) }.isFailure) }
    }
}
