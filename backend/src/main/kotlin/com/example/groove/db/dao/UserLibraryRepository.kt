package com.example.groove.db.dao

import com.example.groove.db.model.UserLibrary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface UserLibraryRepository : CrudRepository<UserLibrary, Long> {
	@Query("SELECT ul " +
			"FROM UserLibrary ul " +
			"WHERE ul.user.id = :userId " +
			"AND (:loadHidden IS TRUE OR ul.hidden = FALSE) " +
			"AND (:name IS NULL OR ul.name LIKE %:name%) " +
			"AND (:artist IS NULL OR ul.artist LIKE %:artist%) " +
			"AND (:album IS NULL OR ul.album LIKE %:album%)"
	)
	fun getLibrary(
			@Param("name") name: String? = null,
			@Param("artist") artist: String? = null,
			@Param("album") album: String? = null,
			@Param("userId") userId: Long,
			@Param("loadHidden") loadHidden: Boolean = false,
			pageable: Pageable = Pageable.unpaged()
	): Page<UserLibrary>

	@Modifying
	@Query("UPDATE UserLibrary ul " +
			"SET ul.hidden = :isHidden " +
			"WHERE ul.user.id = :userId " + // This user check is to prevent people hiding / showing
			"AND ul.id IN :userLibraryIds"  // other users' songs by guessing at IDs
	)
	fun setHiddenForUser(
			@Param("userLibraryIds") userLibraryIds: List<Long>,
			@Param("userId") userId: Long,
			@Param("isHidden") isHidden: Boolean
	)
}
