package com.t0p47.faceidentification.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.t0p47.faceidentification.db.entities.PersonGroup;

import io.reactivex.Single;

@Dao
public interface PersonGroupDao {

    @Query("SELECT personGroupId FROM persongroup")
    Single<String> getPersonGroupId();

    @Insert
    long insertPersonGroup(PersonGroup personGroup);

}
