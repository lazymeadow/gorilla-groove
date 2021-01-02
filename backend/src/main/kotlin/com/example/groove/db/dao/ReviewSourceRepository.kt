package com.example.groove.db.dao

import com.example.groove.db.model.ReviewSource
import com.example.groove.db.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.sql.Timestamp

interface ReviewSourceRepository : CrudRepository<ReviewSource, Long> {

}
