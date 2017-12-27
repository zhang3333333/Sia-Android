/*
 * Copyright (c) 2017 Nicholas van Dyke. All rights reserved.
 */

package vandyke.siamobile.data.local.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Flowable
import org.intellij.lang.annotations.Language
import vandyke.siamobile.data.remote.data.wallet.TransactionData

@Dao
abstract class TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(tx: TransactionData)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertAll(txs: List<TransactionData>)

    @Language("RoomSql")
    @Query("SELECT * FROM transactions ORDER BY confirmationTimestamp DESC")
    abstract fun getAllByMostRecent(): Flowable<List<TransactionData>>

    @Language("RoomSql")
    @Query("DELETE FROM transactions")
    abstract fun deleteAll()

    @Language("RoomSql")
    @android.arch.persistence.room.Transaction
    open fun deleteAllAndInsert(txs: List<TransactionData>) {
        deleteAll()
        insertAll(txs)
    }
}