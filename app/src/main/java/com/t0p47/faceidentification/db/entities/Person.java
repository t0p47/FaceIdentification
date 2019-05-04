package com.t0p47.faceidentification.db.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.t0p47.faceidentification.db.FaceIdConverter;

import java.util.List;

//import io.reactivex.annotations.NonNull;

@Entity
public class Person {

    @PrimaryKey
    @NonNull
    public String personId;

    public String firstName;
    public String lastName;
    public String patronymicName;
    public String PassportNumber;
    @TypeConverters({FaceIdConverter.class})
    public List<String> faceIdList;
    public int counter;

}
