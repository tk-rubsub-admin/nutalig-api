package com.nutalig.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ObjectToMapUtil {

    public static Map<String, Object> convertObjectToMap(Object obj) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<>();

        // Get the class of the object
        Class<?> clazz = obj.getClass();

        // Get all the fields of the object
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true); // allow access to private fields
            map.put(field.getName(), field.get(obj)); // put field name and value into map
        }

        return map;
    }
}
