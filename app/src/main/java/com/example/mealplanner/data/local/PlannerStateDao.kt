package com.example.mealplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlannerStateDao {
    @Query("SELECT * FROM planner_state WHERE id = :id")
    fun getById(id: Int = PlannerStateEntity.SINGLETON_ID): PlannerStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: PlannerStateEntity)
}
