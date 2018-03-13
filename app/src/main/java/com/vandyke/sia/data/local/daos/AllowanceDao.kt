/*
 * Copyright (c) 2017 Nicholas van Dyke. All rights reserved.
 */

package com.vandyke.sia.data.local.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import com.vandyke.sia.data.models.renter.RenterSettingsAllowanceData
import io.reactivex.Flowable

@Dao
interface AllowanceDao : BaseDao<RenterSettingsAllowanceData> {
    @Query("SELECT * FROM allowance LIMIT 1")
    fun onlyEntry(): Flowable<RenterSettingsAllowanceData>

    @Query("DELETE FROM scValue")
    fun deleteAll()
}