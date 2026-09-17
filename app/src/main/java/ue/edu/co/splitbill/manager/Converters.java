package ue.edu.co.splitbill.manager;

import androidx.room.TypeConverter;

import java.util.Date;

import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.entity.SyncStatus;

/**
 * Traductores entre los tipos de Java y los tipos que entiende SQLite.
 *
 * SQLite solo guarda texto, enteros, reales y blobs. Las fechas se guardan como enteros
 * (milisegundos desde 1970) y los enum como texto con su nombre, que es legible al inspeccionar
 * la base de datos y no se rompe si mas adelante se agrega una constante nueva al enum.
 */
public final class Converters {

    @TypeConverter
    public static Long dateToLong(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Date longToDate(Long millis) {
        return millis == null ? null : new Date(millis);
    }

    @TypeConverter
    public static String splitTypeToString(SplitType splitType) {
        return splitType == null ? null : splitType.name();
    }

    @TypeConverter
    public static SplitType stringToSplitType(String name) {
        return name == null ? null : SplitType.valueOf(name);
    }

    @TypeConverter
    public static String syncStatusToString(SyncStatus syncStatus) {
        return syncStatus == null ? null : syncStatus.name();
    }

    @TypeConverter
    public static SyncStatus stringToSyncStatus(String name) {
        return name == null ? null : SyncStatus.valueOf(name);
    }
}
