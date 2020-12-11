@file:Suppress("unused")

package com.example.groove

import org.assertj.core.api.Assertions

infix fun Any?.shouldEqual(other: Any?) {
	Assertions.assertThat(this).isEqualTo(other)
}

infix fun Any?.shouldNotEqual(other: Any?) {
	Assertions.assertThat(this).isNotEqualTo(other)
}

infix fun Any?.shouldBe(other: Any?) {
	Assertions.assertThat(this).isSameAs(other)
}

infix fun Any?.shouldNotBe(other: Any?) {
	Assertions.assertThat(this).isNotSameAs(other)
}
