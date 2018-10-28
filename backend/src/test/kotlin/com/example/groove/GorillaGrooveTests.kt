package com.example.groove

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest()
class GorillaGrooveTests {

	@Test
	fun `context loads`() {
        assertThat(5).isEqualTo(5)
	}

}
