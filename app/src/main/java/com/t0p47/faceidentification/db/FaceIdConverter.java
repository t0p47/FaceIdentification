package com.t0p47.faceidentification.db;

import android.arch.persistence.room.TypeConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FaceIdConverter {

    @TypeConverter
    public String fromFaceIds(List<String> faceIds){
        return faceIds.stream().collect(Collectors.joining(","));
    }

    @TypeConverter
    public List<String> toFaceIds(String faceId){
        return Arrays.asList(faceId.split(","));
    }

}
