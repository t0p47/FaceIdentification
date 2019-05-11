package com.t0p47.faceidentification.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.t0p47.faceidentification.db.dao.subsets.Name;
import com.t0p47.faceidentification.db.entities.Person;

import java.util.List;

import io.reactivex.Single;

@Dao
public interface PersonDao {

    @Query("SELECT * FROM person")
    Single<List<Person>> getAll();

    @Query("SELECT firstName FROM person WHERE personId= :personId")
    Single<Name> getNameById(String personId);

    @Query("SELECT personId FROM person")
    Single<List<String>> getAllPersonIds();

    @Query("SELECT faceIdList FROM Person WHERE personId = :personId")
    Single<List<String>> getAllFaceIds(String personId);

    @Query("UPDATE person SET faceIdList = :newFaceIds WHERE personId = :personId")
    int updateFaceIds(String personId, List<String> newFaceIds);

    @Insert
    long insertPerson(Person person);

    @Update
    int updatePerson(Person person);

    /*
    * @Query("DELETE FROM person WHERE faceIdList IN (:idsToDeleteList)")
    int deleteFaces(List<String> idsToDeleteList);*/
    @Query("DELETE FROM person WHERE personId IN (:idToDelete)")
    int deletePersons(List<String> idToDelete);


}
