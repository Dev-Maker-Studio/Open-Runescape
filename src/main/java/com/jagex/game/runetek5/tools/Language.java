package com.jagex.game.runetek5.tools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jagex.game.runetek5.config.Identifiable;

public enum Language implements Identifiable {
    ENGLISH("EN", "en", 0, "GB"),
    GERMAN("DE", "de", 1, "DE"),
    FRENCH("FR", "fr", 2, "FR"),
    PORTUGESE("PT", "pt", 3, "BR"),
    DUTCH("NL", "nl", 4, "NL"),
    SPANISH("ES", "es", 5, "ES");

    public static final Language[] LANGUAGES;
    public static Map<Language, ConcurrentLinkedQueue<SimpleDateFormat>> dateformatByLanguage;

    static {
        Language[] langs = values();
        LANGUAGES = new Language[langs.length];
        for (Language language : langs) {
            if (LANGUAGES[language.value] != null) {
                throw new IllegalStateException();
            }
            LANGUAGES[language.value] = language;
        }
    }

    public final String uppercaseAbbreviation;
    public final String lowercaseAbbreviation;
    public final int value;
    public final Locale locale;

    Language(String upperAbbreviation, String lowerAbbreviation, int value, String country) {
    	this.uppercaseAbbreviation = upperAbbreviation;
        this.lowercaseAbbreviation = lowerAbbreviation;
        this.value = value;
        Locale locale = null;
        if (country != null) {
            locale = new Locale(getLowercaseAbbreviation(), country);
        }
        if (locale != null) {
            this.locale = locale;
        } else {
        	this.locale = new Locale(getLowercaseAbbreviation());
        }
    }

    public static Language getLanguage(int index) {
        return index >= 0 && index < LANGUAGES.length ? LANGUAGES[index] : null;
    }

    public String getLowercaseAbbreviation() {
        return this.lowercaseAbbreviation;
    }

    @Override
    public int getValue() {
        return this.value;
    }

    public String toString() {
        return getLowercaseAbbreviation().toLowerCase(Locale.ENGLISH);
    }

    public Locale getLocale() {
        return this.locale;
    }

	public static String getFormattedDatestring(Date date, String format, TimeZone timezone) {
	    if (dateformatByLanguage == null) {
	        dateformatByLanguage = new HashMap<>(6);
	        Language[] languages = values();
	        for (int index = 0; index < languages.length; index++) {
	            Language language = languages[index];
	            dateformatByLanguage.put(language, new ConcurrentLinkedQueue<SimpleDateFormat>());
	        }
	    }
	    ConcurrentLinkedQueue<SimpleDateFormat> dateformats = dateformatByLanguage.get(ENGLISH);
	    SimpleDateFormat dateformat = dateformats.poll();
	    if (dateformat == null) {
	        dateformat = new SimpleDateFormat(format, ENGLISH.getLocale());
	    } else {
	        dateformat.applyPattern(format);
	    }
	    dateformat.setTimeZone(timezone);
	    String formattedDate = dateformat.format(date);
	    dateformats.add(dateformat);
	    return formattedDate;
	}
}
