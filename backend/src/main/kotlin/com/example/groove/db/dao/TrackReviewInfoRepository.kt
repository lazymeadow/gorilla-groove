package com.example.groove.db.dao

import com.example.groove.db.model.TrackReviewInfo
import org.springframework.data.repository.CrudRepository

interface TrackReviewInfoRepository : CrudRepository<TrackReviewInfo, Long>
