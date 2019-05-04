package com.t0p47.faceidentification.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.t0p47.faceidentification.db.dao.FaceDao;
import com.t0p47.faceidentification.db.dao.PersonDao;
import com.t0p47.faceidentification.db.dao.PersonGroupDao;
import com.t0p47.faceidentification.db.entities.Face;
import com.t0p47.faceidentification.db.entities.Person;
import com.t0p47.faceidentification.db.entities.PersonGroup;

@Database(entities = {Person.class, Face.class, PersonGroup.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract FaceDao faceDao();
    public abstract PersonDao personDao();
    public abstract PersonGroupDao personGroupDao();

}
