package com.t0p47.faceidentification.db.dao.subsets;

import android.arch.persistence.room.ColumnInfo;

public class Name {

    @ColumnInfo(name = "firstName")
    public String firstName;

    @ColumnInfo(name = "lastName")
    public String lastName;

}
