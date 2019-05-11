package com.t0p47.faceidentification.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.t0p47.faceidentification.db.entities.Face;

import java.util.List;

import io.reactivex.Single;

@Dao
public interface FaceDao {

    @Query("SELECT faceUri FROM Face WHERE faceId = :faceId")
    Single<String> getFaceUri(String faceId);

    @Insert
    long insert(Face face);

    @Query("DELETE FROM person WHERE faceIdList IN (:idsToDeleteList)")
    int deleteFaces(List<String> idsToDeleteList);

}
