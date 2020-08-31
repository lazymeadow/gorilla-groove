package com.example.groove.db.model

import com.example.groove.util.DateUtils.now
import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.sql.Timestamp
import javax.persistence.*

@Entity
class User(
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@Column
		val name: String,

		@Column
		val email: String,

		@JsonIgnore
		@Column(name = "password")
		var encryptedPassword: String, // Named encryptedPassword to not conflict with the parent method getPassword()

		// Doesn't necessarily require a "log in". If a user opens the site with a valid cookie that counts too
		@Column(name = "last_login")
		var lastLogin: Timestamp = now(),

		@Column(name = "created_at")
		val createdAt: Timestamp = now(),

		@Column(name = "updated_at")
		var updatedAt: Timestamp = now(),

		@JsonIgnore
		@Column(columnDefinition = "BIT")
		var deleted: Boolean = false,

		@JsonIgnore
		@ManyToMany(mappedBy = "partyUsers")
		var partyDevices: MutableList<Device> = mutableListOf(),

		@JsonIgnore
		@ManyToMany(mappedBy = "subscribedUsers")
		var reviewSources: MutableList<ReviewSource> = mutableListOf()

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

