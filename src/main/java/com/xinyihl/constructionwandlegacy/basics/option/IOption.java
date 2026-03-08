package com.xinyihl.constructionwandlegacy.basics.option;

import com.xinyihl.constructionwandlegacy.Tags;

public interface IOption<T> {
    String getKey();

    String getValueString();

    void setValueString(String val);

    default String getKeyTranslation() {
        return Tags.MOD_ID + ".option." + getKey();
    }

    default String getValueTranslation() {
        return Tags.MOD_ID + ".option." + getKey() + "." + getValueString();
    }

    default String getDescTranslation() {
        return Tags.MOD_ID + ".option." + getKey() + "." + getValueString() + ".desc";
    }

    boolean isEnabled();

    void set(T val);

    T get();

    T next(boolean dir);

    default T next() {
        return next(true);
    }
}
