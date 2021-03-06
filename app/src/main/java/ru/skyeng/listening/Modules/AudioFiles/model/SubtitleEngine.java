package ru.skyeng.listening.Modules.AudioFiles.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import ru.skyeng.listening.CommonComponents.FacadeCommon;

/**
 * ---------------------------------------------------
 * Created by Sermilion on 17/02/2017.
 * Project: Listening
 * ---------------------------------------------------
 * <a href="http://www.skyeng.ru">www.skyeng.ru</a>
 * <a href="http://www.github.com/sermilion>github</a>
 * ---------------------------------------------------
 */

public class SubtitleEngine implements Comparable<Long>, Parcelable {

    private int index = -1;
    private List<SubtitleFile> subtitleFileList;
    private SubtitleFile current;

    public SubtitleEngine(Parcel in) {
        index = in.readInt();
    }

    public static final Creator<SubtitleEngine> CREATOR = new Creator<SubtitleEngine>() {
        @Override
        public SubtitleEngine createFromParcel(Parcel in) {
            return new SubtitleEngine(in);
        }

        @Override
        public SubtitleEngine[] newArray(int size) {
            return new SubtitleEngine[size];
        }
    };

    public int size() {
        if (subtitleFileList == null) return 0;
        return subtitleFileList.size();
    }

    public SubtitleEngine() {
        subtitleFileList = new ArrayList<>();
    }

    public void clear() {
        subtitleFileList.clear();
    }

    public SubtitleFile updateSubtitles(long currentTime) {
        int result = this.compareTo(currentTime);
        if (result == -1 || result == 0) {
            index++;
            if (index < subtitleFileList.size())
                current = subtitleFileList.get(index);
        }
        return current;
    }

    public void setSubtitles(List<SubtitleFile> subtitleFileList) {
        this.subtitleFileList = subtitleFileList;
        if (subtitleFileList.size() > 0) {
            current = subtitleFileList.get(0);
        }
    }

    @Override
    public int compareTo(@NonNull Long newSubStart) {
        if (current == null) return -1;
        if (index > -1) {

            while (FacadeCommon.dateToMills(subtitleFileList.get(index).getEndTime()) * 1000 < newSubStart) {
                index++;
            }

            if (index > -1) {
                while (index > -1 && FacadeCommon.dateToMills(subtitleFileList.get(index).getEndTime()) * 1000 > newSubStart) {
                    index--;
                }

            }

            if (index > -1) {
                current = subtitleFileList.get(index);
            }
        }
        long currentStart = FacadeCommon.dateToMills(current.getEndTime()) * 1000;
        if (currentStart < newSubStart) {
            return -1;
        } else if (currentStart == 0) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(index);
    }
}
