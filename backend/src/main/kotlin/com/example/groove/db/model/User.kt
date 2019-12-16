package com.example.groove.db.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.sql.Timestamp
import javax.persistence.*

@Entity
data class User(
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@Column
		val name: String,

		@Column
		val email: String,

		@Column(name = "password")
		var encryptedPassword: String, // Named encryptedPassword to not conflict with the parent method getPassword()

		// Doesn't necessarily require a "log in". If a user opens the site with a valid cookie that counts too
		@Column(name = "last_login")
		var lastLogin: Timestamp = Timestamp(System.currentTimeMillis())
) : UserDetails {
	override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
		return mutableListOf()
	}

	override fun isEnabled(): Boolean {
		return true
	}

	override fun getUsername(): String {
		return email
	}

	override fun isCredentialsNonExpired(): Boolean {
		return true
	}

	override fun getPassword(): String {
		return encryptedPassword
	}

	override fun isAccountNonExpired(): Boolean {
		return true
	}

	override fun isAccountNonLocked(): Boolean {
		return true
	}
}

