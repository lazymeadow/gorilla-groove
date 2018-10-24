package com.example.groove.db.dao

import com.example.groove.db.model.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@DataJpaTest
class RepositoriesTests(@Autowired val entityManager: TestEntityManager,
                        @Autowired val userRepository: UserRepository) {

    // This method --should-- be able to be named with backticks + spaces, as is standard Kotlin. But there is a problem
    // with Java 10 + @DataJpaTest issue. https://github.com/spring-guides/tut-spring-boot-kotlin/issues/8
    @Test
    fun when_findById_then_return_user() {
        val user = User(0, "Testy Test", "a@b.c", "password")
        entityManager.persist(user)
        entityManager.flush()

        val found = userRepository.findById(1)

        assertThat(found.get()).isEqualTo(user)
    }

}
