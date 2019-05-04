package com.t0p47.faceidentification.db.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


@Entity
public class Face {

    @PrimaryKey
    @NonNull
    public String faceId;

    public String personId;

    public String faceUri;

}
